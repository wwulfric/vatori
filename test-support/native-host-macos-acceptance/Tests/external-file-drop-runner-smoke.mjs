import assert from 'node:assert/strict';
import { copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

// The real entry point and metadata preparer run in an isolated mirror. Package,
// AppKit launch, inspector and lock boundary are instrumented stubs; no GUI,
// Java sidecar, package build, process discovery or real runtime trace is used.
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const work = realpathSync(mkdtempSync(join(tmpdir(), 'vatori-external-drop-runner-smoke-')));
try {
  const support = join(work, 'test-support/native-host-macos-acceptance');
  const bin = join(support, 'bin');
  const tests = join(support, 'Tests');
  const build = join(work, '.build/native-hosts/macos');
  const stubs = join(work, 'stubs');
  for (const path of [bin, tests, build, stubs, join(work, 'bin')]) mkdirSync(path, { recursive: true });
  const runner = join(bin, 'run-flow-external-file-drop');
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin/run-flow-external-file-drop'), runner);
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/Tests/external-file-drop-evidence.mjs'),
    join(tests, 'external-file-drop-evidence.mjs'));
  const eventsPath = join(work, 'events.jsonl');
  const lockPath = join(work, 'test-package-lock-active');
  const runtimeReceipt = join(work, 'runtime.json');
  const inspectReceipt = join(work, 'inspector.json');
  const out = join(build, 'host.out.log'), err = join(build, 'host.err.log');
  const sentinel = join(build, 'unrelated-sentinel');
  writeFileSync(sentinel, 'preserve neighboring generated files');
  const emit = `const emit = (stage, details = {}) => fs.appendFileSync(process.env.VATORI_DROP_SMOKE_EVENTS,
    JSON.stringify({stage, locked: fs.existsSync(process.env.VATORI_DROP_SMOKE_LOCK), ...details}) + '\\n');`;
  writeFileSync(join(work, 'node-observer.cjs'), `const fs = require('node:fs');
${emit}
const args = process.argv.slice(2);
if (!fs.existsSync(process.env.VATORI_DROP_SMOKE_LOCK)) process.exit(91);
emit(args[1] === 'prepare' ? 'prepare' : args[0] === '-p' ? 'metadata' : 'node');
`);
  writeFileSync(join(stubs, 'node'), `#!/usr/bin/env bash
set -euo pipefail
"$VATORI_DROP_SMOKE_NODE" "$VATORI_DROP_SMOKE_ROOT/node-observer.cjs" "$@"
exec "$VATORI_DROP_SMOKE_NODE" "$@"
`, { mode: 0o700 });
  writeFileSync(join(bin, 'with-package-lock'), `#!/usr/bin/env bash
set -euo pipefail
[[ "$1" == -- && "$VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD" == 1 ]]
shift
mkdir "$VATORI_DROP_SMOKE_LOCK"
trap 'rmdir "$VATORI_DROP_SMOKE_LOCK"' EXIT
"$VATORI_DROP_SMOKE_NODE" -e 'require("node:fs").appendFileSync(process.env.VATORI_DROP_SMOKE_EVENTS, JSON.stringify({stage:"lock-enter",locked:true})+"\\n")'
status=0
"$@" || status=$?
"$VATORI_DROP_SMOKE_NODE" -e 'require("node:fs").appendFileSync(process.env.VATORI_DROP_SMOKE_EVENTS, JSON.stringify({stage:"lock-exit",locked:true})+"\\n")'
exit "$status"
`);
  writeFileSync(join(stubs, 'cp'), `#!/usr/bin/env bash
set -euo pipefail
[[ -d "$VATORI_DROP_SMOKE_LOCK" ]]
"$VATORI_DROP_SMOKE_NODE" -e 'require("node:fs").appendFileSync(process.env.VATORI_DROP_SMOKE_EVENTS, JSON.stringify({stage:"copy",locked:true})+"\\n")'
exec /bin/cp "$@"
`, { mode: 0o700 });
  writeFileSync(join(work, 'run-stub.cjs'), `const fs = require('node:fs');
const path = require('node:path');
${emit}
emit('runtime');
if (!fs.existsSync(process.env.VATORI_DROP_SMOKE_LOCK)) process.exit(92);
fs.writeFileSync(process.env.VATORI_DROP_SMOKE_RUNTIME, JSON.stringify({args:process.argv.slice(2), env:process.env}));
const dir = path.join(process.env.VATORI_DROP_SMOKE_ROOT, '.build/native-hosts/macos');
const out = path.join(dir,'host.out.log'), err = path.join(dir,'host.err.log');
const mode = process.env.VATORI_DROP_SMOKE_MODE;
if (mode === 'package-fail') process.exit(23);
if (mode === 'replace-identical') {
  for (const file of [out, err]) { const body = fs.readFileSync(file); fs.unlinkSync(file); fs.writeFileSync(file, body); }
} else if (mode === 'same-size-preserved-mtime') {
  for (const file of [out, err]) { const before = fs.statSync(file); fs.writeFileSync(file, file === out ? 'NEW_OUT\\n' : 'NEW_ERR\\n'); fs.utimesSync(file, before.atime, before.mtime); }
} else if (mode === 'stderr-only-fail') {
  fs.writeFileSync(err, 'NEW_ERR\\n');
} else {
  fs.writeFileSync(out, 'NEW_OUT\\n'); fs.writeFileSync(err, 'NEW_ERR\\n');
}
process.exit(['runtime-fail', 'stderr-only-fail'].includes(mode) ? 7 : 0);
`);
  writeFileSync(join(bin, 'run'), `#!/usr/bin/env bash
set -euo pipefail
exec "$VATORI_DROP_SMOKE_NODE" "$VATORI_DROP_SMOKE_ROOT/run-stub.cjs" "$@"
`);
  writeFileSync(join(work, 'inspect-stub.cjs'), `const fs = require('node:fs');
${emit}
emit('inspect');
if (!fs.existsSync(process.env.VATORI_DROP_SMOKE_LOCK)) process.exit(93);
const args = process.argv.slice(2);
fs.writeFileSync(process.env.VATORI_DROP_SMOKE_INSPECTOR, JSON.stringify({args, body:fs.readFileSync(args[1],'utf8')}));
process.exit(process.env.VATORI_DROP_SMOKE_MODE === 'gate-fail' ? 2 : 0);
`);
  writeFileSync(join(work, 'bin/flow-ipc-trace-inspect'), `#!/usr/bin/env bash
set -euo pipefail
exec "$VATORI_DROP_SMOKE_NODE" "$VATORI_DROP_SMOKE_ROOT/inspect-stub.cjs" "$@"
`);
  const cleanEnv = { ...process.env };
  delete cleanEnv.VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD;
  for (const key of Object.keys(cleanEnv)) if (key.startsWith('VAADIN_DESKTOP_FLOW_IPC_EXTERNAL_DROP_')) delete cleanEnv[key];
  const env = { ...cleanEnv, PATH: `${stubs}:${process.env.PATH}`, VATORI_DROP_SMOKE_ROOT: work,
    VATORI_DROP_SMOKE_NODE: process.execPath, VATORI_DROP_SMOKE_EVENTS: eventsPath, VATORI_DROP_SMOKE_LOCK: lockPath,
    VATORI_DROP_SMOKE_RUNTIME: runtimeReceipt, VATORI_DROP_SMOKE_INSPECTOR: inspectReceipt };
  const invoke = (args, mode = 'pass') => {
    for (const file of [eventsPath, runtimeReceipt, inspectReceipt]) rmSync(file, { force: true });
    const result = spawnSync('bash', [runner, ...args], { env: { ...env, VATORI_DROP_SMOKE_MODE: mode }, encoding: 'utf8', timeout: 20_000 });
    assert.ifError(result.error); assert.equal(result.signal, null);
    assert.equal(existsSync(lockPath), false, 'test lock must be released on every return path');
    return result;
  };
  const events = () => existsSync(eventsPath) ? readFileSync(eventsPath, 'utf8').trim().split('\n').map(JSON.parse) : [];
  for (const args of [[], ['relative-run'], [join(work, 'unused-extra'), 'extra']]) {
    const result = invoke(args);
    assert.equal(result.status, 2, `${result.stdout}\n${result.stderr}`);
    assert.deepEqual(events(), [], 'invalid arguments must not acquire lock, prepare or launch');
    assert.equal(existsSync(join(work, 'unused-extra')), false);
  }
  const existing = join(work, 'existing-run'); mkdirSync(existing);
  writeFileSync(join(existing, 'sentinel'), 'owned existing content');
  const existsResult = invoke([existing]);
  assert.notEqual(existsResult.status, 0);
  assert.equal(readFileSync(join(existing, 'sentinel'), 'utf8'), 'owned existing content');
  assert.equal(existsSync(join(existing, 'manifest.json')), false);
  assert.equal(existsSync(runtimeReceipt), false, 'existing directory must fail before launch');
  assert.deepEqual(events().map(e => e.stage), ['lock-enter', 'prepare', 'lock-exit']);

  for (const mode of ['package-fail', 'runtime-fail', 'stderr-only-fail', 'pass', 'gate-fail', 'replace-identical', 'same-size-preserved-mtime']) {
    writeFileSync(out, 'OLD_OUT\n'); writeFileSync(err, 'OLD_ERR\n');
    const runDir = join(work, `run-${mode}`);
    const result = invoke([runDir], mode);
    const expectedStatus = mode === 'package-fail' ? 23 : mode.endsWith('-fail') ? mode === 'gate-fail' ? 2 : 7 : 0;
    assert.equal(result.status, expectedStatus, `${mode}: ${result.stdout}\n${result.stderr}`);
    const manifest = JSON.parse(readFileSync(join(runDir, 'manifest.json'), 'utf8'));
    assert.equal(manifest.fileBytes, 65573);
    const runtime = JSON.parse(readFileSync(runtimeReceipt, 'utf8'));
    assert.deepEqual(runtime.args, ['dev', 'test-apps/flow-over-ipc-framework-app']);
    for (const [key, value] of Object.entries({ RUN_ID: manifest.runId, FILE_NAME: manifest.fileName,
      FILE_BYTES: String(manifest.fileBytes), FILE_SHA256: manifest.fileSha256 })) {
      assert.equal(runtime.env[`VAADIN_DESKTOP_FLOW_IPC_EXTERNAL_DROP_${key}`], value);
    }
    assert.equal(runtime.env.VAADIN_DESKTOP_FLOW_IPC_AUTO_EXTERNAL_FILE_DROP, '1');
    assert.equal(runtime.env.VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN, '1');
    assert.equal(runtime.env.VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS, '300');
    assert.equal(runtime.env.VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL, 'full');
    assert.equal(runtime.env.VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD, '1');
    assert.equal(Object.values(runtime.env).some(value => value.includes(manifest.sourcePath)), false,
      'independent source path must not enter the host launch environment');
    assert.deepEqual(Object.keys(runtime.env).filter(k => k.startsWith('VAADIN_DESKTOP_FLOW_IPC_EXTERNAL_DROP_')).sort(),
      ['FILE_BYTES', 'FILE_NAME', 'FILE_SHA256', 'RUN_ID'].map(k => `VAADIN_DESKTOP_FLOW_IPC_EXTERNAL_DROP_${k}`).sort());
    const order = events();
    assert.equal(order[0].stage, 'lock-enter'); assert.equal(order.at(-1).stage, 'lock-exit');
    assert.ok(order.every(e => e.locked), 'prepare, metadata, runtime, copies and inspector must stay under one package lock');
    assert.ok(order.findIndex(e => e.stage === 'prepare') < order.findIndex(e => e.stage === 'runtime'));
    if (mode === 'package-fail') {
      assert.equal(existsSync(join(runDir, 'host.out.log')), false, 'old passing stdout must never become this failed run trace');
      assert.equal(existsSync(join(runDir, 'host.err.log')), false, 'old errors must never become this failed run stderr');
      assert.equal(readFileSync(out, 'utf8'), 'OLD_OUT\n'); assert.equal(readFileSync(err, 'utf8'), 'OLD_ERR\n');
      assert.equal(order.some(e => e.stage === 'copy' || e.stage === 'inspect'), false);
    } else {
      assert.equal(readFileSync(join(runDir, 'host.err.log'), 'utf8'), mode === 'replace-identical' ? 'OLD_ERR\n' : 'NEW_ERR\n');
      if (mode === 'stderr-only-fail') assert.equal(existsSync(join(runDir, 'host.out.log')), false);
      else assert.equal(readFileSync(join(runDir, 'host.out.log'), 'utf8'), mode === 'replace-identical' ? 'OLD_OUT\n' : 'NEW_OUT\n');
      if (mode === 'runtime-fail' || mode === 'stderr-only-fail') assert.equal(existsSync(inspectReceipt), false, 'nonzero runtime must remain failure without gate laundering');
      else {
        const inspection = JSON.parse(readFileSync(inspectReceipt, 'utf8'));
        assert.deepEqual(inspection.args, ['--require-flow-ipc-external-file-drop', join(runDir, 'host.out.log')]);
        assert.ok(order.findIndex(e => e.stage === 'copy') < order.findIndex(e => e.stage === 'inspect'));
      }
    }
    assert.equal(readFileSync(sentinel, 'utf8'), 'preserve neighboring generated files');
    if (expectedStatus === 0) assert.match(result.stdout, /Finder provenance still requires an independently observed/);
    else assert.doesNotMatch(result.stdout, /Runtime gate passed/);
  }
  console.log('External file-drop runner smoke passed (isolated stubs; no GUI or package build)');
} finally {
  rmSync(work, { recursive: true, force: true });
}
