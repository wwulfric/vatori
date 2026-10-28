import assert from 'node:assert/strict';
import { copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, realpathSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, dirname, join, resolve } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { once } from 'node:events';
import { fileURLToPath } from 'node:url';

// The actual runner is copied into an owned temporary tree. All builds,
// signing, process scans and host execution are inert stubs; no GUI/JVM runs.
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
if (process.platform !== 'darwin') { console.log('Ready-timeout runner smoke skipped: macOS clone layout required'); process.exit(0); }
const work = realpathSync(mkdtempSync(join(tmpdir(), 'vatori-ready-runner-smoke-')));
const support = join(work, 'test-support/native-host-macos-acceptance'), bin = join(support, 'bin');
const build = join(work, '.build/native-hosts/macos'), app = join(build, 'VaadinDesktopHost.app');
const stubs = join(work, 'stubs'), jdk = join(work, 'jdk'), runner = join(bin, 'run-host-ready-timeout');
const lock = join(build, '.acceptance-package.lock'), receipt = join(work, 'launch.json');
const inspected = join(work, 'inspected'), verified = join(work, 'verified'), psCalls = join(work, 'ps-calls');
const executable = (path, source) => writeFileSync(path, source, { mode: 0o700 });
let cases = 0;
try {
  for (const path of [bin, join(support, 'Tests'), join(support, 'Fixtures'), join(work, 'bin'), stubs,
    join(jdk, 'bin'), join(app, 'Contents/MacOS'), join(app, 'Contents/Resources/Sidecar')]) mkdirSync(path, { recursive: true });
  for (const name of ['run-host-ready-timeout', 'with-package-lock']) copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin', name), join(bin, name));
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/Fixtures/NeverReadySidecar.java'), join(support, 'Fixtures/NeverReadySidecar.java'));
  const originalJar = join(app, 'Contents/Resources/Sidecar/application.jar');
  writeFileSync(originalJar, 'original production staged JAR'); writeFileSync(join(app, 'Contents/Info.plist'), '{}');
  const sentinel = join(build, 'unrelated-sentinel'); writeFileSync(sentinel, 'preserve');
  const neighbor = join(build, 'host-ready-timeout.KEEP42.neighbor'); mkdirSync(neighbor); writeFileSync(join(neighbor, 'sentinel'), 'preserve');

  const source = readFileSync(runner, 'utf8');
  const matcher = source.match(/owned_pids\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  const cleanup = source.match(/cleanup\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  assert.ok(matcher); assert.ok(cleanup);
  const target = '/private/tmp/owned/host-ready-timeout.ABC123/VaadinDesktopHost.app';
  const host = `${target}/Contents/MacOS/VaadinDesktopHost`, jar = `${target}/Contents/Resources/Sidecar/application.jar`;
  const aliasHost = host.replace('/private/tmp/', '/tmp/'), aliasJar = jar.replace('/private/tmp/', '/tmp/');
  const rows = [`201 ${host}`, `202 ${aliasHost}`, `203 /private/fixture/jdk/bin/java -jar ${jar}`,
    `204 /fixture/jdk/bin/java -jar ${aliasJar}`, `205 ${host}.neighbor`, `206 /private/fixture/jdk/bin/java -jar ${jar}.neighbor`,
    `207 /private/fixture/jdk/bin/java -jar ${jar.replace('ABC123', 'XYZ987')}`, `208 /bin/bash -c echo /private/fixture/jdk/bin/java -jar ${jar}`,
    `209 /other/jdk/bin/java -jar ${jar}`, `210 java -jar ${jar}`, `211 /private/fixture/jdk/bin/java -jar /unrelated.jar --note -jar ${jar}`,
    `212 /private/fixture/jdk/bin/java -jar ${jar} --argument`, `213 /private/fixture/jdk/bin/java -Xmx1g -jar ${jar}`,
    `214 /usr/bin/env /private/fixture/jdk/bin/java -jar ${jar}`].join('\n');
  const match = spawnSync('/bin/bash', ['-c', `set -euo pipefail
ps() { printf '%s\\n' "$SMOKE_ROWS"; }
${matcher}
owned_pids
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, SMOKE_ROWS: rows, JAVA_HOME: '/private/fixture/jdk', HOST_EXE: host, SIDECAR_JAR: jar } });
  assert.ifError(match.error); assert.equal(match.status, 0, match.stderr); assert.equal(match.stdout, '201\n202\n203\n204\n'); cases += 14;
  const guardRoot = join(work, 'guard/build.with[regex]+chars'), guardNeighbor = `${guardRoot}-neighbor`, calls = join(work, 'rm-calls');
  mkdirSync(guardRoot, { recursive: true }); mkdirSync(guardNeighbor);
  const link = join(guardRoot, 'host-ready-timeout.LINK12'); symlinkSync(guardNeighbor, link);
  for (const [candidate, allowed] of [[join(guardRoot, 'host-ready-timeout.ABC123'), true], [guardRoot, false],
    [join(guardNeighbor, 'host-ready-timeout.ABC123'), false], [join(guardRoot, 'host-ready-timeout.ABC123.extra'), false],
    [join(guardRoot, 'host-ready-timeout.ABC123/child'), false], [link, false]]) {
    if (candidate !== link) mkdirSync(candidate, { recursive: true }); rmSync(calls, { force: true });
    const r = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { :; }
kill() { exit 97; }
rm() { printf '%s\\n' "$@" > "$SMOKE_RM_CALLS"; }
${cleanup}
cleanup 0
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, WORK: candidate, BUILD_ROOT: guardRoot, HOST_WAIT_PID: '', SMOKE_RM_CALLS: calls } });
    assert.ifError(r.error); assert.equal(r.status, allowed ? 0 : 1, r.stderr); assert.equal(existsSync(calls), allowed);
    if (allowed) assert.equal(readFileSync(calls, 'utf8'), `-rf\n--\n${candidate}\n`); assert(existsSync(candidate)); cases++;
  }
  const trapLines = source.split('\n').filter(line => /^trap 'cleanup /.test(line)).join('\n');
  assert.equal(trapLines.split('\n').length, 4);
  const cleanupTarget = join(guardRoot, 'host-ready-timeout.SIG001'); mkdirSync(cleanupTarget);
  for (const [signal, status] of [['INT', 130], ['TERM', 143], ['HUP', 129]]) {
    rmSync(calls, { force: true });
    const r = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { :; }
rm() { printf '%s\\n' "$@" >> "$SMOKE_RM_CALLS"; /bin/kill -INT "$$"; /bin/kill -TERM "$$"; }
${cleanup}
${trapLines}
/bin/kill -${signal} "$$"
exit 98
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, WORK: cleanupTarget, BUILD_ROOT: guardRoot, HOST_WAIT_PID: '', SMOKE_RM_CALLS: calls } });
    assert.ifError(r.error); assert.equal(r.status, status, r.stderr); assert.equal(readFileSync(calls, 'utf8'), `-rf\n--\n${cleanupTarget}\n`); cases++;
  }
  const killed = join(work, 'mock-killed'), killCalls = join(work, 'mock-kills');
  for (const escalation of ['TERM', 'KILL']) {
    for (const p of [killed, killCalls, calls]) rmSync(p, { force: true });
    const r = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { if [[ ! -f "$SMOKE_KILLED" ]]; then printf '424242\\n'; fi; }
kill() { printf '%s\\n' "$*" >> "$SMOKE_KILL_CALLS"; if [[ "$1" == "$SMOKE_EXIT_SIGNAL" ]]; then printf done > "$SMOKE_KILLED"; fi; return 0; }
sleep() { :; }
rm() { printf '%s\\n' "$@" > "$SMOKE_RM_CALLS"; }
${cleanup}
cleanup 0
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, WORK: cleanupTarget, BUILD_ROOT: guardRoot, HOST_WAIT_PID: '',
      SMOKE_RM_CALLS: calls, SMOKE_KILLED: killed, SMOKE_KILL_CALLS: killCalls, SMOKE_EXIT_SIGNAL: `-${escalation}` } });
    assert.ifError(r.error); assert.equal(r.status, 1, 'cleanup must not rescue PASS');
    assert.equal(readFileSync(killCalls, 'utf8'), escalation === 'TERM' ? '-TERM 424242\n' : '-TERM 424242\n-KILL 424242\n'); cases++;
  }
  // A stale/empty process scan must not hide a still-live captured child and
  // make cleanup block forever in wait. Only this test's harmless child is used.
  const waitCalls = join(work, 'wait-calls'), nodeCalls = join(work, 'node-calls');
  const live = spawn(process.execPath, ['-e', 'setInterval(()=>{},1000)'], { stdio: 'ignore' });
  const liveExit = once(live, 'exit');
  const dead = spawnSync(process.execPath, ['-e', ''], { stdio: 'ignore', timeout: 5000 });
  assert.ifError(dead.error); assert.equal(dead.status, 0);
  assert.throws(() => process.kill(dead.pid, 0), error => error.code === 'ESRCH');
  const cleanupWithCapturedPID = (pid, overrides = '', extraEnv = {}) => {
    for (const p of [calls, waitCalls, nodeCalls]) rmSync(p, { force: true });
    return spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { :; }
kill() { exit 97; }
wait() { printf '%s\\n' "$*" > "$SMOKE_WAIT_CALLS"; }
rm() { printf '%s\\n' "$@" > "$SMOKE_RM_CALLS"; }
${overrides}
${cleanup}
cleanup 0
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, WORK: cleanupTarget, BUILD_ROOT: guardRoot,
      HOST_WAIT_PID: String(pid), SMOKE_WAIT_CALLS: waitCalls, SMOKE_RM_CALLS: calls,
      SMOKE_NODE_CALLS: nodeCalls, SMOKE_NODE: process.execPath, ...extraEnv } });
  };
  try {
    const r = cleanupWithCapturedPID(live.pid);
    assert.ifError(r.error); assert.equal(r.status, 1, r.stderr); assert.match(r.stderr, /Captured host PID.*retaining clone/);
    assert(!existsSync(waitCalls)); assert(!existsSync(calls)); assert(existsSync(cleanupTarget));
    process.kill(live.pid, 0); cases++;
    const absent = cleanupWithCapturedPID(dead.pid);
    assert.ifError(absent.error); assert.equal(absent.status, 0, absent.stderr);
    assert.equal(readFileSync(waitCalls, 'utf8'), `${dead.pid}\n`); assert(existsSync(calls)); cases++;
    for (const code of ['EPERM', 'EINVAL']) {
      const failed = cleanupWithCapturedPID(dead.pid, `node() {
  "$SMOKE_NODE" -e 'const source = process.argv[1]; process.argv.splice(1, 1);
    process.kill = () => { const error = new Error("test-only signal-zero error"); error.code = process.env.SMOKE_KILL_ERROR; throw error; };
    eval(source);' "$2" "$3"
}`, { SMOKE_KILL_ERROR: code });
      assert.ifError(failed.error); assert.equal(failed.status, 1, failed.stderr);
      assert.match(failed.stderr, /Captured host PID.*retaining clone/);
      assert(!existsSync(waitCalls)); assert(!existsSync(calls)); cases++;
    }
    const scanCount = join(work, 'cleanup-scan-count');
    for (const finalScan of ['remaining', 'error']) {
      rmSync(scanCount, { force: true });
      const failed = cleanupWithCapturedPID(dead.pid, `owned_pids() {
  if [[ -f "$SMOKE_SCAN_COUNT" ]]; then
    if [[ "$SMOKE_FINAL_SCAN" == error ]]; then return 23; fi
    printf '424242\\n'
  else printf first > "$SMOKE_SCAN_COUNT"; fi
}
node() { printf called > "$SMOKE_NODE_CALLS"; exit 97; }`, { SMOKE_SCAN_COUNT: scanCount, SMOKE_FINAL_SCAN: finalScan });
      assert.ifError(failed.error); assert.equal(failed.status, 1, failed.stderr);
      assert.match(failed.stderr, /Owned ready-timeout processes.*retaining clone/);
      assert(!existsSync(waitCalls)); assert(!existsSync(nodeCalls)); assert(!existsSync(calls)); cases++;
    }
  } finally { live.kill('SIGKILL'); await liveExit; }
  const timeoutLoop = source.match(/run_started=\$SECONDS\nwhile kill -0 [\s\S]*?\ndone\n/)?.[0]; assert.ok(timeoutLoop);
  const timed = spawnSync('/bin/bash', ['-c', `set -euo pipefail
kill() { return 0; }
${timeoutLoop.replace('run_started=$SECONDS', 'run_started=$((SECONDS - 21))')}
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, HOST_WAIT_PID: '424242' } });
  assert.ifError(timed.error); assert.equal(timed.status, 124); assert.match(timed.stderr, /external runtime deadline/); cases++;

  executable(join(stubs, 'uname'), '#!/bin/bash\nprintf "Darwin\\n"\n');
  executable(join(stubs, 'ps'), `#!/bin/bash
count=0
if [[ -f "$SMOKE_PS_CALLS" ]]; then read -r count < "$SMOKE_PS_CALLS"; fi
count=$((count + 1)); printf '%s\\n' "$count" > "$SMOKE_PS_CALLS"
if [[ "$count" == "$SMOKE_PS_FAIL_AT" ]]; then exit 23; fi
`);
  executable(join(stubs, 'mktemp'), `#!/bin/bash
if [[ "$SMOKE_STATE_FAIL" == 1 && "$*" == *vatori-ready-timeout.* ]]; then exit 17; fi
exec /usr/bin/mktemp "$@"
`);
  executable(join(stubs, 'ditto'), `#!/usr/bin/env node
require('node:fs').cpSync(process.argv[2],process.argv[3],{recursive:true});
`);
  executable(join(stubs, 'plutil'), `#!/usr/bin/env node
const fs=require('node:fs'),a=process.argv.slice(2),p=a.at(-1),v=JSON.parse(fs.readFileSync(p,'utf8'));v[a[1]]=a[3];fs.writeFileSync(p,JSON.stringify(v));
`);
  executable(join(stubs, 'codesign'), '#!/bin/bash\nexit "$SMOKE_SIGN_STATUS"\n');
  executable(join(jdk, 'bin/java'), '#!/bin/bash\nexit 97\n');
  executable(join(jdk, 'bin/javac'), '#!/bin/bash\nexit "$SMOKE_JAVAC_STATUS"\n');
  executable(join(jdk, 'bin/jar'), `#!/usr/bin/env node
require('node:assert/strict').equal(process.argv[2],'cfe');require('node:fs').writeFileSync(process.argv[3],'fixture JAR');
process.exit(Number(process.env.SMOKE_JAR_STATUS));
`);
  const locked = `[[ -f "$SMOKE_LOCK/owner" && "$VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD" == 1 ]]`;
  executable(join(work, 'bin/vaadin-desktop'), `#!/bin/bash
set -euo pipefail
${locked}
[[ "$*" == "package test-apps/flow-over-ipc-framework-app" ]]
[[ "$VAADIN_DESKTOP_SIDECAR_INITIAL_READY_TIMEOUT_SECONDS" == 1 && "$VAADIN_DESKTOP_FLOW_IPC_SIDECAR_RESTART_LIMIT" == 0 ]]
[[ -z "\${VAADIN_DESKTOP_TEST_READY_TIMEOUT_DIR:-}" && -z "\${VAADIN_DESKTOP_TEST_READY_TIMEOUT_MODE:-}" ]]
[[ -z "\${VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR:-}" && -z "\${VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_DIR:-}" ]]
[[ -z "\${VAADIN_DESKTOP_ACCEPTANCE_CAPABILITY_MANIFEST:-}" && -z "\${VAADIN_DESKTOP_ACCEPTANCE_OMIT_CAPABILITY_MANIFEST:-}" ]]
exit "$SMOKE_PACKAGE_STATUS"
`);
  executable(join(app, 'Contents/MacOS/VaadinDesktopHost'), `#!/usr/bin/env node
const fs=require('node:fs'),p=require('node:path'),assert=require('node:assert/strict'),state=process.env.VAADIN_DESKTOP_TEST_READY_TIMEOUT_DIR;
const app=p.resolve(p.dirname(process.argv[1]),'../..');assert(fs.existsSync(process.env.SMOKE_LOCK+'/owner'));
fs.writeFileSync(process.env.SMOKE_RECEIPT,JSON.stringify({app,state,pid:process.pid,mode:process.env.VAADIN_DESKTOP_TEST_READY_TIMEOUT_MODE,
  stateMode:fs.statSync(state).mode&0o777,workMode:fs.statSync(p.dirname(app)).mode&0o777,
  info:JSON.parse(fs.readFileSync(app+'/Contents/Info.plist','utf8')),
  drivers:Object.keys(process.env).filter(k=>k.startsWith('VAADIN_DESKTOP_FLOW_IPC_AUTO_')),
  stop:process.env.VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION??'',noActivate:process.env.VAADIN_DESKTOP_MACOS_NO_ACTIVATE,
  detail:process.env.VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL}));
if(process.env.SMOKE_EMPTY_TRACE!=='1')console.log('CURRENT_READY_TIMEOUT_TRACE');
process.exit(Number(process.env.SMOKE_HOST_STATUS));
`);
  executable(join(support, 'Tests/host-ready-timeout-exit-evidence.mjs'), `
import fs from 'node:fs';import assert from 'node:assert/strict';
assert(fs.existsSync(process.env.SMOKE_LOCK+'/owner'));assert.equal(process.argv.length,6);
const launched=JSON.parse(fs.readFileSync(process.env.SMOKE_RECEIPT,'utf8'));
assert.equal(process.argv[2],launched.state);assert.equal(process.argv[4],launched.mode);assert.equal(Number(process.argv[5]),launched.pid);
fs.writeFileSync(process.env.SMOKE_VERIFIED,'called');
if(fs.readFileSync(process.argv[3],'utf8')!=='CURRENT_READY_TIMEOUT_TRACE\\n')process.exit(1);
process.exit(Number(process.env.SMOKE_VERIFY_STATUS));
`);
  executable(join(work, 'bin/flow-ipc-trace-inspect'), `#!/bin/bash
set -euo pipefail
${locked}
[[ -f "$SMOKE_VERIFIED" && "$1" == --require-flow-ipc-host-ready-timeout ]]
[[ "$(< "$2")" == CURRENT_READY_TIMEOUT_TRACE ]]
printf '%s\\n' "$@" > "$SMOKE_INSPECTED"
exit "$SMOKE_INSPECT_STATUS"
`);
  for (const args of [['wrong'], ['blocked', 'extra']]) {
    const r = spawnSync('/bin/bash', [runner, ...args], { encoding: 'utf8', timeout: 5000 });
    assert.ifError(r.error); assert.equal(r.status, 2); assert.match(r.stderr, /Usage:/); cases++;
  }
  const scenarios = { pass: 0, 'default-pass': 0, 'package-fail': 6, 'state-fail': 17, 'javac-fail': 7, 'jar-fail': 8,
    'sign-fail': 9, 'host-zero': 1, 'host-two': 1, 'empty-trace': 1, 'verify-fail': 11, 'inspect-fail': 12,
    'ps-fail': 23, 'ps-late-fail': 1, 'ps-final-fail': 1 };
  const identities = new Set();
  for (const [scenario, expected] of Object.entries(scenarios)) {
    const mode = scenario === 'default-pass' ? 'cooperative' : 'blocked';
    const log = join(build, `host-ready-timeout-${mode}.out.log`), err = join(build, `host-ready-timeout-${mode}.err.log`);
    writeFileSync(log, 'PREVIOUS_VALID_TRACE'); writeFileSync(err, 'PREVIOUS_ERROR');
    for (const p of [receipt, inspected, verified, psCalls]) rmSync(p, { force: true });
    const env = { ...process.env, PATH: `${stubs}:${process.env.PATH}`, JAVA_HOME: jdk,
      SMOKE_LOCK: lock, SMOKE_RECEIPT: receipt, SMOKE_INSPECTED: inspected, SMOKE_VERIFIED: verified, SMOKE_PS_CALLS: psCalls,
      SMOKE_PACKAGE_STATUS: scenario === 'package-fail' ? '6' : '0', SMOKE_STATE_FAIL: scenario === 'state-fail' ? '1' : '0',
      SMOKE_JAVAC_STATUS: scenario === 'javac-fail' ? '7' : '0', SMOKE_JAR_STATUS: scenario === 'jar-fail' ? '8' : '0',
      SMOKE_SIGN_STATUS: scenario === 'sign-fail' ? '9' : '0', SMOKE_HOST_STATUS: scenario === 'host-zero' ? '0' : scenario === 'host-two' ? '2' : '1',
      SMOKE_EMPTY_TRACE: scenario === 'empty-trace' ? '1' : '0', SMOKE_VERIFY_STATUS: scenario === 'verify-fail' ? '11' : '0',
      SMOKE_INSPECT_STATUS: scenario === 'inspect-fail' ? '12' : '0',
      SMOKE_PS_FAIL_AT: scenario === 'ps-fail' ? '1' : scenario === 'ps-late-fail' ? '2' : scenario === 'ps-final-fail' ? '3' : '0',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_DELIVERY: '1', VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS: '999',
      VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION: '1', VAADIN_DESKTOP_ACCEPTANCE_CAPABILITY_MANIFEST: '/untrusted/manifest',
      VAADIN_DESKTOP_ACCEPTANCE_OMIT_CAPABILITY_MANIFEST: '1', VAADIN_DESKTOP_TEST_READY_TIMEOUT_DIR: '/untrusted/state',
      VAADIN_DESKTOP_TEST_READY_TIMEOUT_MODE: 'untrusted', VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR: '/untrusted/recovery',
      VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_DIR: '/untrusted/shutdown', VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_MODE: 'untrusted',
      VAADIN_DESKTOP_ACCEPTANCE_PACKAGE_LOCK_TIMEOUT_SECONDS: '0' };
    delete env.VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD;
    const r = spawnSync('/bin/bash', [runner, ...(scenario === 'default-pass' ? [] : [mode])], { env, encoding: 'utf8', timeout: 10000 });
    assert.ifError(r.error); assert.equal(r.status, expected, `${scenario}\n${r.stdout}\n${r.stderr}`); assert.equal(r.signal, null);
    assert(!existsSync(lock)); assert(!readFileSync(log, 'utf8').includes('PREVIOUS_VALID_TRACE')); assert.equal(readFileSync(err, 'utf8'), '');
    assert.equal(existsSync(inspected), ['pass', 'default-pass', 'inspect-fail', 'ps-late-fail', 'ps-final-fail'].includes(scenario));
    assert.equal(existsSync(verified), ['pass', 'default-pass', 'empty-trace', 'verify-fail', 'inspect-fail', 'ps-late-fail', 'ps-final-fail'].includes(scenario));
    const launched = !['package-fail', 'state-fail', 'javac-fail', 'jar-fail', 'sign-fail'].includes(scenario);
    assert.equal(existsSync(receipt), launched);
    if (launched) {
      const data = JSON.parse(readFileSync(receipt, 'utf8'));
      assert.equal(data.mode, mode); assert.deepEqual(data.drivers, []); assert.equal(data.stop, '');
      assert.equal(data.noActivate, '1'); assert.equal(data.detail, 'full');
      assert.equal(data.stateMode, 0o700); assert.equal(data.workMode, 0o700); assert.equal(dirname(data.state), dirname(data.app));
      assert.match(basename(data.state), /^vatori-ready-timeout\.[A-Za-z0-9]{6}$/);
      assert.match(basename(dirname(data.app)), /^host-ready-timeout\.[A-Za-z0-9]{6}$/);
      assert.equal(realpathSync(dirname(dirname(data.app))), realpathSync(build));
      assert.match(data.info.CFBundleIdentifier, /^dev\.vatori\.desktop\.acceptance\.readytimeout\.[0-9a-f-]{36}$/);
      assert(!identities.has(data.info.CFBundleIdentifier)); identities.add(data.info.CFBundleIdentifier);
      const retained = ['ps-late-fail', 'ps-final-fail'].includes(scenario);
      assert.equal(existsSync(dirname(data.app)), retained);
      if (retained) { assert.match(r.stderr, /retaining clone/); rmSync(dirname(data.app), { recursive: true }); }
    }
    assert.deepEqual(readdirSync(build).filter(name => /^host-ready-timeout\.[A-Za-z0-9]{6}$/.test(name)), []);
    assert.equal(readFileSync(originalJar, 'utf8'), 'original production staged JAR');
    assert.equal(readFileSync(sentinel, 'utf8'), 'preserve'); assert.equal(readFileSync(join(neighbor, 'sentinel'), 'utf8'), 'preserve'); cases++;
  }
  console.log(`Host ready timeout runner smoke passed: ${cases} matcher/guard/end-to-end stub cases (no real GUI/build)`);
} finally { rmSync(work, { recursive: true, force: true }); }
