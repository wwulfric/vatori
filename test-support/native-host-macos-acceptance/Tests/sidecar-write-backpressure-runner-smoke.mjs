import assert from 'node:assert/strict';
import { copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync,
  realpathSync, rmSync, statSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, dirname, join, resolve } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { once } from 'node:events';
import { fileURLToPath } from 'node:url';

// All build, signing, LaunchServices and process-discovery commands are inert
// stubs inside one owned temporary workspace. This never launches a GUI/JVM or
// discovers/signals a user's host process.
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
if (process.platform !== 'darwin') {
  console.log('Sidecar write-backpressure runner smoke skipped: macOS clone layout required');
  process.exit(0);
}
const work = mkdtempSync(join(tmpdir(), 'vatori-write-backpressure-runner-smoke-'));
const support = join(work, 'test-support/native-host-macos-acceptance');
const bin = join(support, 'bin');
const build = join(work, '.build/native-hosts/macos');
const app = join(build, 'VaadinDesktopHost.app');
const stubs = join(work, 'stubs');
const jdk = join(work, 'jdk');
const runner = join(bin, 'run-sidecar-write-backpressure');
const lock = join(build, '.acceptance-package.lock');
const receipt = join(work, 'launch.json');
const inspected = join(work, 'inspected');
const verified = join(work, 'verified');
const manifest = join(support, 'Fixtures/host-shutdown-termination-capabilities.json');
const psCalls = join(work, 'ps-calls');
let hostLog, hostErr;
const executable = (path, source) => writeFileSync(path, source, { mode: 0o700 });
let cases = 0;
try {
  for (const path of [bin, join(support, 'Tests'), join(support, 'Fixtures'), join(work, 'bin'), stubs, join(jdk, 'bin'),
    join(app, 'Contents/MacOS'), join(app, 'Contents/Resources/Sidecar')]) mkdirSync(path, { recursive: true });
  for (const name of ['run-sidecar-write-backpressure', 'with-package-lock']) {
    copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin', name), join(bin, name));
  }
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/Fixtures/host-shutdown-termination-capabilities.json'), manifest);
  assert.deepEqual(JSON.parse(readFileSync(manifest, 'utf8')), { schemaVersion: 1,
    permissionMode: 'manifest-enforced', windows: [{ label: 'main', commands: ['window.close', 'window.setTitle'] }] });
  writeFileSync(join(app, 'Contents/Info.plist'), '{}');
  executable(join(app, 'Contents/MacOS/VaadinDesktopHost'), '#!/bin/bash\nexit 97\n');
  executable(join(jdk, 'bin/java'), '#!/bin/bash\nexit 97\n');
  writeFileSync(join(app, 'Contents/Resources/Sidecar/application.jar'), 'inert JAR');
  const sentinel = join(build, 'unrelated-sentinel');
  writeFileSync(sentinel, 'preserve unrelated material');
  const neighbor = join(build, 'sidecar-write-backpressure.KEEP42.neighbor');
  mkdirSync(neighbor);
  writeFileSync(join(neighbor, 'sentinel'), 'preserve neighboring clone');

  const source = readFileSync(runner, 'utf8');
  const matcher = source.match(/owned_pids\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  const cleanup = source.match(/cleanup\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  assert.ok(matcher); assert.ok(cleanup);
  const absence = source.match(/captured_pid_absent\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  assert.ok(absence);
  const target = '/private/tmp/owned/sidecar-write-backpressure.ABC123/VaadinDesktopHost.app';
  const host = `${target}/Contents/MacOS/VaadinDesktopHost`;
  const jar = `${target}/Contents/Resources/Sidecar/application.jar`;
  const aliasHost = host.replace('/private/tmp/', '/tmp/');
  const aliasJar = jar.replace('/private/tmp/', '/tmp/');
  const rows = [
    `201 ${host}`, `202 ${aliasHost}`,
    `203 /private/fixture/jdk/bin/java -jar ${jar}`, `204 /fixture/jdk/bin/java -jar ${aliasJar}`,
    `205 ${host}.neighbor`, `206 /private/fixture/jdk/bin/java -jar ${jar}.neighbor`,
    `207 /private/fixture/jdk/bin/java -jar ${jar.replace('ABC123', 'XYZ987')}`,
    `208 /bin/bash -c echo /private/fixture/jdk/bin/java -jar ${jar}`,
    `209 /other/jdk/bin/java -jar ${jar}`, `210 java -jar ${jar}`,
    `211 /private/fixture/jdk/bin/java -jar /unrelated.jar --note -jar ${jar}`,
    `212 /private/fixture/jdk/bin/java -jar ${jar} --argument`,
    `213 /private/fixture/jdk/bin/java -Xmx1g -jar ${jar}`,
    `214 /usr/bin/env /private/fixture/jdk/bin/java -jar ${jar}`,
  ].join('\n');
  const match = spawnSync('/bin/bash', ['-c', `set -euo pipefail
ps() { printf '%s\\n' "$SMOKE_ROWS"; }
${matcher}
owned_pids
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, SMOKE_ROWS: rows,
    JAVA_HOME: '/private/fixture/jdk', HOST_EXE: host, SIDECAR_JAR: jar } });
  assert.ifError(match.error); assert.equal(match.status, 0, match.stderr);
  assert.equal(match.stdout, '201\n202\n203\n204\n');
  cases += rows.split('\n').length;

  const guardRoot = join(work, 'guard/build.with[regex]+chars');
  const guardNeighbor = `${guardRoot}-neighbor`;
  mkdirSync(guardRoot, { recursive: true }); mkdirSync(guardNeighbor);
  const link = join(guardRoot, 'sidecar-write-backpressure.LINK12');
  symlinkSync(guardNeighbor, link);
  const calls = join(work, 'rm-calls');
  for (const [candidate, allowed] of [
    [join(guardRoot, 'sidecar-write-backpressure.ABC123'), true], [guardRoot, false],
    [join(guardNeighbor, 'sidecar-write-backpressure.ABC123'), false],
    [join(guardRoot, 'sidecar-write-backpressure.ABC123.extra'), false],
    [join(guardRoot, 'sidecar-write-backpressure.ABC123/child'), false], [link, false],
  ]) {
    if (candidate !== link) mkdirSync(candidate, { recursive: true });
    rmSync(calls, { force: true });
    const result = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { :; }
kill() { exit 97; }
rm() { printf '%s\\n' "$@" > "$SMOKE_RM_CALLS"; }
${cleanup}
cleanup 0
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, WORK: candidate,
      BUILD_ROOT: guardRoot, OPEN_WAIT_PID: '', SMOKE_RM_CALLS: calls } });
    assert.ifError(result.error); assert.equal(result.status, allowed ? 0 : 1, result.stderr);
    assert.equal(existsSync(calls), allowed);
    if (allowed) assert.equal(readFileSync(calls, 'utf8'), `-rf\n--\n${candidate}\n`);
    assert(existsSync(candidate), 'guard checks only record deletion');
    cases++;
  }

  const trapLines = source.split('\n').filter(line => /^trap 'cleanup /.test(line)).join('\n');
  assert.equal(trapLines.split('\n').length, 4);
  const cleanupTarget = join(guardRoot, 'sidecar-write-backpressure.SIG001');
  mkdirSync(cleanupTarget);
  for (const [trigger, status] of [['INT', 130], ['TERM', 143], ['HUP', 129]]) {
    rmSync(calls, { force: true });
    const result = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { :; }
kill() { exit 97; }
rm() {
  printf '%s\\n' "$@" >> "$SMOKE_RM_CALLS"
  /bin/kill -INT "$$"
  /bin/kill -TERM "$$"
}
${cleanup}
${trapLines}
/bin/kill -${trigger} "$$"
exit 98
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, WORK: cleanupTarget,
      BUILD_ROOT: guardRoot, OPEN_WAIT_PID: '', SMOKE_RM_CALLS: calls } });
    assert.ifError(result.error); assert.equal(result.status, status, result.stderr);
    assert.equal(readFileSync(calls, 'utf8'), `-rf\n--\n${cleanupTarget}\n`, 'reentrant signals do not skip or duplicate cleanup');
    cases++;
  }
  const killed = join(work, 'mock-killed'), killCalls = join(work, 'mock-kill-calls');
  for (const escalation of ['KILL']) {
    for (const path of [killed, killCalls, calls]) rmSync(path, { force: true });
    const result = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { if [[ ! -f "$SMOKE_KILLED" ]]; then printf '424242\\n'; fi; }
kill() {
  printf '%s\\n' "$*" >> "$SMOKE_KILL_CALLS"
  if [[ "$1" == "$SMOKE_EXIT_SIGNAL" ]]; then printf done > "$SMOKE_KILLED"; fi
  return 0
}
sleep() { :; }
rm() { printf '%s\\n' "$@" > "$SMOKE_RM_CALLS"; }
${cleanup}
cleanup 0
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, WORK: cleanupTarget,
      BUILD_ROOT: guardRoot, OPEN_WAIT_PID: '', SMOKE_RM_CALLS: calls,
      SMOKE_KILLED: killed, SMOKE_KILL_CALLS: killCalls, SMOKE_EXIT_SIGNAL: `-${escalation}` } });
    assert.ifError(result.error); assert.equal(result.status, 1, 'cleanup signals cannot rescue a PASS');
    assert.equal(readFileSync(killCalls, 'utf8'), '-KILL 424242\n');
    assert.equal(readFileSync(calls, 'utf8'), `-rf\n--\n${cleanupTarget}\n`);
    cases++;
  }
  const waitCalls = join(work, 'wait-calls');
  const live = spawn(process.execPath, ['-e', 'setInterval(()=>{},1000)'], { stdio: 'ignore' });
  const liveExit = once(live, 'exit');
  const dead = spawnSync(process.execPath, ['-e', ''], { stdio: 'ignore', timeout: 5000 });
  assert.ifError(dead.error); assert.equal(dead.status, 0);
  assert.throws(() => process.kill(dead.pid, 0), error => error.code === 'ESRCH');
  try {
    for (const [pid, code, expected] of [[live.pid, '', 1], [dead.pid, '', 0], [dead.pid, 'EPERM', 1], [dead.pid, 'EINVAL', 1]]) {
      for (const p of [calls, waitCalls, killCalls]) rmSync(p, { force: true });
      const fakeLookup = code ? `node() {
  "$SMOKE_NODE" -e 'const source=process.argv[1];process.argv.splice(1,1);
    process.kill=()=>{const error=new Error("test-only lookup failure");error.code=process.env.SMOKE_ERROR;throw error;};eval(source);' "$2" "$3"
}` : '';
      const result = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { :; }
sleep() { :; }
kill() { printf '%s\\n' "$*" >> "$SMOKE_KILL_CALLS"; return 1; }
wait() { printf '%s\\n' "$*" > "$SMOKE_WAIT_CALLS"; }
rm() { printf '%s\\n' "$@" > "$SMOKE_RM_CALLS"; }
${fakeLookup}
${absence}
${cleanup}
cleanup 0
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, WORK: cleanupTarget, BUILD_ROOT: guardRoot,
        OPEN_WAIT_PID: String(pid), SMOKE_WAIT_CALLS: waitCalls, SMOKE_RM_CALLS: calls,
        SMOKE_KILL_CALLS: killCalls, SMOKE_NODE: process.execPath, SMOKE_ERROR: code } });
      assert.ifError(result.error); assert.equal(result.status, expected, result.stderr);
      assert.equal(existsSync(waitCalls), expected === 0); assert.equal(existsSync(calls), expected === 0);
      if (expected) { assert.match(result.stderr, /waiter.*retaining clone/); assert.equal(readFileSync(killCalls, 'utf8'), `-TERM ${pid}\n-KILL ${pid}\n`); }
      else assert.equal(readFileSync(waitCalls, 'utf8'), `${pid}\n`);
      assert(existsSync(cleanupTarget)); cases++;
    }
    // Failure cleanup above only records signals; it must not touch this child.
    process.kill(live.pid, 0);
  } finally { live.kill('SIGKILL'); await liveExit; }
  const timeoutLoop = source.match(/run_started=\$SECONDS\nwhile kill -0 [\s\S]*?\ndone\n/)?.[0];
  assert.ok(timeoutLoop);
  const timedOut = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { :; }
kill() { return 0; }
rm() { printf '%s\\n' "$@" > "$SMOKE_RM_CALLS"; }
${cleanup}
${trapLines}
${timeoutLoop.replace('run_started=$SECONDS', 'run_started=$((SECONDS - 121))')}
exit 98
`], { encoding: 'utf8', timeout: 5000, env: { ...process.env, WORK: cleanupTarget,
    BUILD_ROOT: guardRoot, OPEN_WAIT_PID: '', SMOKE_RM_CALLS: calls } });
  assert.ifError(timedOut.error); assert.equal(timedOut.status, 124, timedOut.stderr);
  assert.match(timedOut.stderr, /external runtime deadline/); cases++;

  executable(join(stubs, 'uname'), '#!/bin/bash\nprintf "Darwin\\n"\n');
  executable(join(stubs, 'ps'), `#!/bin/bash
set -euo pipefail
count=0
if [[ -f "$SMOKE_PS_CALLS" ]]; then read -r count < "$SMOKE_PS_CALLS"; fi
count=$((count + 1))
printf '%s\\n' "$count" > "$SMOKE_PS_CALLS"
if [[ "$count" == "$SMOKE_PS_FAIL_AT" ]]; then exit 23; fi
`);
  executable(join(stubs, 'mktemp'), `#!/bin/bash
if [[ "$SMOKE_STATE_FAIL" == 1 && "$*" == *vatori-host-shutdown.* ]]; then exit 17; fi
exec /usr/bin/mktemp "$@"
`);
  executable(join(stubs, 'ditto'), `#!/usr/bin/env node
require('node:fs').cpSync(process.argv[2],process.argv[3],{recursive:true});
`);
  executable(join(stubs, 'plutil'), `#!/usr/bin/env node
const fs=require('node:fs'),a=process.argv.slice(2),p=a.at(-1),v=JSON.parse(fs.readFileSync(p,'utf8'));
v[a[1]]=a[3];fs.writeFileSync(p,JSON.stringify(v));
`);
  executable(join(stubs, 'codesign'), '#!/bin/bash\nexit 0\n');
  const locked = `[[ -f "$SMOKE_LOCK/owner" && "$VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD" == 1 ]]`;
  executable(join(work, 'bin/vaadin-desktop'), `#!/bin/bash
set -euo pipefail
${locked}
[[ "$*" == "package test-apps/flow-over-ipc-framework-app" ]]
[[ "$VAADIN_DESKTOP_FLOW_IPC_SIDECAR_RESTART_LIMIT" == 0 ]]
[[ "\${VAADIN_DESKTOP_ACCEPTANCE_CAPABILITY_MANIFEST:-}" == "$SMOKE_MANIFEST" ]]
[[ -z "\${VAADIN_DESKTOP_ACCEPTANCE_OMIT_CAPABILITY_MANIFEST:-}" ]]
[[ -z "\${VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR:-}" ]]
[[ -z "\${VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_DIR:-}" ]]
[[ -z "\${VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_MODE:-}" ]]
exit "$SMOKE_PACKAGE_STATUS"
`);
  executable(join(bin, 'build-host'), `#!/bin/bash
set -euo pipefail
${locked}
exit "$SMOKE_BUILD_STATUS"
`);
  executable(join(stubs, 'open'), `#!/usr/bin/env node
const fs=require('node:fs'),assert=require('node:assert/strict'),a=process.argv.slice(2);
const envArg=k=>a.find(x=>x.startsWith(k+'='))?.slice(k.length+1),app=a.at(-1),state=envArg('VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_DIR');
const log=a[a.indexOf('-o')+1],err=a[a.indexOf('--stderr')+1];
assert(fs.existsSync(process.env.SMOKE_LOCK+'/owner'));
assert.equal(fs.readFileSync(log,'utf8'),'');assert.equal(fs.readFileSync(err,'utf8'),'');
const record={app,state,args:a,info:JSON.parse(fs.readFileSync(app+'/Contents/Info.plist','utf8')),
  stateMode:fs.statSync(state).mode&0o777,workMode:fs.statSync(require('node:path').dirname(app)).mode&0o777,
  inheritedDrivers:Object.keys(process.env).filter(k=>k.startsWith('VAADIN_DESKTOP_FLOW_IPC_AUTO_')),
  stop:process.env.VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION??'',
  mode:envArg('VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_MODE'),
  driver:envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDECAR_WRITE_BACKPRESSURE'),shutdown:envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN'),
  timeout:envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS'),noActivate:envArg('VAADIN_DESKTOP_MACOS_NO_ACTIVATE'),
  jdk:envArg('JAVA_HOME'),detail:envArg('VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL')};
fs.writeFileSync(process.env.SMOKE_RECEIPT,JSON.stringify(record));
if(process.env.SMOKE_OPEN==='fail')process.exit(9);
if(process.env.SMOKE_OPEN==='pass')fs.writeFileSync(log,'CURRENT_BACKPRESSURE_TRACE\\n');
`);
  executable(join(support, 'Tests/sidecar-write-backpressure-exit-evidence.mjs'), `
import fs from 'node:fs';import assert from 'node:assert/strict';
assert(fs.existsSync(process.env.SMOKE_LOCK+'/owner'));
assert.equal(process.argv.length,4);
assert.equal(fs.statSync(process.argv[2]).mode&0o777,0o700);
fs.writeFileSync(process.env.SMOKE_VERIFIED,'called');
if(fs.readFileSync(process.argv[3],'utf8')!=='CURRENT_BACKPRESSURE_TRACE\\n')process.exit(1);
process.exit(Number(process.env.SMOKE_VERIFY_STATUS));
`);
  executable(join(work, 'bin/flow-ipc-trace-inspect'), `#!/bin/bash
set -euo pipefail
${locked}
[[ -f "$SMOKE_VERIFIED" ]]
printf '%s\\n' "$@" > "$SMOKE_INSPECTED"
[[ "$1" == --require-flow-ipc-sidecar-write-backpressure ]]
[[ "$(< "$2")" == CURRENT_BACKPRESSURE_TRACE ]]
exit "$SMOKE_INSPECT_STATUS"
`);
  const identities = new Set();
  for (const args of [['wrong'], ['blocked', 'extra']]) {
    const result = spawnSync('/bin/bash', [runner, ...args], { encoding: 'utf8', timeout: 5000 });
    assert.ifError(result.error); assert.equal(result.status, 2); assert.match(result.stderr, /Usage:/); cases++;
  }
  for (const scenario of ['pass', 'cooperative-pass', 'package-fail', 'build-fail', 'state-fail', 'open-fail',
    'empty-trace', 'verify-fail', 'inspect-fail', 'ps-fail', 'ps-late-fail', 'ps-final-fail']) {
    const mode = 'cooperative';
    hostLog = join(build, 'host-sidecar-write-backpressure.out.log');
    hostErr = join(build, 'host-sidecar-write-backpressure.err.log');
    writeFileSync(hostLog, 'PREVIOUS_VALID_TRACE'); writeFileSync(hostErr, 'PREVIOUS_ERROR');
    for (const path of [receipt, inspected, verified, psCalls]) rmSync(path, { force: true });
    const env = { ...process.env, PATH: `${stubs}:${process.env.PATH}`, JAVA_HOME: jdk,
      SMOKE_LOCK: lock, SMOKE_RECEIPT: receipt, SMOKE_INSPECTED: inspected, SMOKE_PS_CALLS: psCalls,
      SMOKE_MANIFEST: manifest, SMOKE_VERIFIED: verified,
      SMOKE_PACKAGE_STATUS: scenario === 'package-fail' ? '6' : '0',
      SMOKE_BUILD_STATUS: scenario === 'build-fail' ? '7' : '0',
      SMOKE_STATE_FAIL: scenario === 'state-fail' ? '1' : '0',
      SMOKE_INSPECT_STATUS: scenario === 'inspect-fail' ? '8' : '0',
      SMOKE_VERIFY_STATUS: scenario === 'verify-fail' ? '11' : '0',
      SMOKE_OPEN: scenario === 'open-fail' ? 'fail' : scenario === 'empty-trace' ? 'empty' : 'pass',
      SMOKE_PS_FAIL_AT: scenario === 'ps-fail' ? '1' : scenario === 'ps-late-fail' ? '2' : scenario === 'ps-final-fail' ? '3' : '0',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_DELIVERY: '1',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_OUTCOMES: '1',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_CANCEL_SANDBOX: '1',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS: '999',
      VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION: '1',
      VAADIN_DESKTOP_ACCEPTANCE_CAPABILITY_MANIFEST: '/untrusted/manifest',
      VAADIN_DESKTOP_ACCEPTANCE_OMIT_CAPABILITY_MANIFEST: '1',
      VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_DIR: '/untrusted/state',
      VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_MODE: 'untrusted',
      VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR: '/untrusted/recovery',
      VAADIN_DESKTOP_ACCEPTANCE_PACKAGE_LOCK_TIMEOUT_SECONDS: '0' };
    delete env.VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD;
    const result = spawnSync('/bin/bash', [runner], { env, encoding: 'utf8', timeout: 15000 });
    assert.ifError(result.error);
    assert.equal(result.status, { pass: 0, 'cooperative-pass': 0, 'package-fail': 6, 'build-fail': 7, 'state-fail': 17,
      'open-fail': 9, 'empty-trace': 1, 'verify-fail': 11, 'inspect-fail': 8, 'ps-fail': 23, 'ps-late-fail': 1, 'ps-final-fail': 1 }[scenario],
    `${scenario}\n${result.stdout}\n${result.stderr}`);
    assert.equal(result.signal, null);
    assert(!existsSync(lock), 'package lock is released after inspection and cleanup');
    assert(!readFileSync(hostLog, 'utf8').includes('PREVIOUS_VALID_TRACE'), `${scenario}: stale trace must be cleared before build`);
    assert.equal(readFileSync(hostErr, 'utf8'), '');
    assert.equal(existsSync(inspected), ['pass', 'cooperative-pass', 'inspect-fail', 'ps-late-fail', 'ps-final-fail'].includes(scenario));
    assert.equal(existsSync(verified), ['pass', 'cooperative-pass', 'empty-trace', 'verify-fail', 'inspect-fail', 'ps-late-fail', 'ps-final-fail'].includes(scenario));
    const shouldLaunch = !['package-fail', 'build-fail', 'state-fail'].includes(scenario);
    assert.equal(existsSync(receipt), shouldLaunch);
    if (shouldLaunch) {
      const r = JSON.parse(readFileSync(receipt, 'utf8'));
      assert.deepEqual(r.inheritedDrivers, []); assert.equal(r.stop, '');
      assert.equal(r.driver, '1'); assert.equal(r.shutdown, '1'); assert.equal(r.timeout, '90');
      assert.equal(r.noActivate, '0'); assert.equal(r.detail, 'full'); assert.equal(r.jdk, jdk);
      assert.equal(r.mode, mode);
      assert(r.args.includes('-n') && r.args.includes('-W') && !r.args.includes('-g'));
      assert.equal(r.stateMode, 0o700); assert.equal(r.workMode, 0o700);
      assert.equal(dirname(r.state), dirname(r.app));
      assert.match(basename(r.state), /^vatori-host-shutdown\.[A-Za-z0-9]{6}$/);
      assert.match(basename(dirname(r.app)), /^sidecar-write-backpressure\.[A-Za-z0-9]{6}$/);
      assert.equal(realpathSync(dirname(dirname(r.app))), realpathSync(build));
      assert.match(r.info.CFBundleIdentifier, /^dev\.vatori\.desktop\.acceptance\.backpressure\.[0-9a-f-]{36}$/);
      assert(!identities.has(r.info.CFBundleIdentifier)); identities.add(r.info.CFBundleIdentifier);
      const retained = ['ps-late-fail', 'ps-final-fail'].includes(scenario);
      assert.equal(existsSync(dirname(r.app)), retained, `${scenario}: exact owned clone cleanup policy`);
      if (retained) {
        assert.match(result.stderr, /retaining clone/);
        rmSync(dirname(r.app), { recursive: true }); // Entirely inert test-owned clone.
      }
    }
    assert.deepEqual(readdirSync(build).filter(name => /^sidecar-write-backpressure\.[A-Za-z0-9]{6}$/.test(name)), [],
      `${scenario}: no clone remains after confirmed no-process cleanup`);
    assert.equal(readFileSync(sentinel, 'utf8'), 'preserve unrelated material');
    assert.equal(readFileSync(join(neighbor, 'sentinel'), 'utf8'), 'preserve neighboring clone');
    cases++;
  }
  console.log(`Sidecar write-backpressure runner smoke passed: ${cases} matcher/guard/end-to-end stub cases (no real GUI/build)`);
} finally {
  rmSync(work, { recursive: true, force: true });
}
