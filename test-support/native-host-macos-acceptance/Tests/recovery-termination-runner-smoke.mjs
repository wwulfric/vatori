import assert from 'node:assert/strict';
import { copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync,
  realpathSync, rmSync, statSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, dirname, join, resolve } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

// All build, signing, LaunchServices and process-discovery commands are inert
// stubs inside one owned temporary workspace. This never launches a GUI/JVM or
// discovers/signals a user's host process.
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
if (process.platform !== 'darwin') {
  console.log('Recovery termination runner smoke skipped: macOS clone layout required');
  process.exit(0);
}
const work = mkdtempSync(join(tmpdir(), 'vatori-recovery-runner-smoke-'));
const support = join(work, 'test-support/native-host-macos-acceptance');
const bin = join(support, 'bin');
const build = join(work, '.build/native-hosts/macos');
const app = join(build, 'VaadinDesktopHost.app');
const stubs = join(work, 'stubs');
const jdk = join(work, 'jdk');
const runner = join(bin, 'run-recovery-termination');
const lock = join(build, '.acceptance-package.lock');
const receipt = join(work, 'launch.json');
const inspected = join(work, 'inspected');
const psCalls = join(work, 'ps-calls');
const hostLog = join(build, 'host-recovery-termination.out.log');
const hostErr = join(build, 'host-recovery-termination.err.log');
const executable = (path, source) => writeFileSync(path, source, { mode: 0o700 });
let cases = 0;
try {
  for (const path of [bin, join(work, 'bin'), stubs, join(jdk, 'bin'),
    join(app, 'Contents/MacOS'), join(app, 'Contents/Resources/Sidecar')]) mkdirSync(path, { recursive: true });
  for (const name of ['run-recovery-termination', 'with-package-lock']) {
    copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin', name), join(bin, name));
  }
  writeFileSync(join(app, 'Contents/Info.plist'), '{}');
  executable(join(app, 'Contents/MacOS/VaadinDesktopHost'), '#!/bin/bash\nexit 97\n');
  executable(join(jdk, 'bin/java'), '#!/bin/bash\n[[ "$*" == "-version" ]] || exit 97\nprintf "stub JDK version\\n" >&2\nexit "${SMOKE_JDK_STATUS:-0}"\n');
  writeFileSync(join(app, 'Contents/Resources/Sidecar/application.jar'), 'inert JAR');
  const sentinel = join(build, 'unrelated-sentinel');
  writeFileSync(sentinel, 'preserve unrelated material');
  const neighbor = join(build, 'recovery-termination.KEEP42.neighbor');
  mkdirSync(neighbor);
  writeFileSync(join(neighbor, 'sentinel'), 'preserve neighboring clone');

  const source = readFileSync(runner, 'utf8');
  const matcher = source.match(/owned_pids\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  const cleanup = source.match(/cleanup\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  assert.ok(matcher); assert.ok(cleanup);
  const target = '/private/tmp/owned/recovery-termination.ABC123/VaadinDesktopHost.app';
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
  const link = join(guardRoot, 'recovery-termination.LINK12');
  symlinkSync(guardNeighbor, link);
  const calls = join(work, 'rm-calls');
  for (const [candidate, allowed] of [
    [join(guardRoot, 'recovery-termination.ABC123'), true], [guardRoot, false],
    [join(guardNeighbor, 'recovery-termination.ABC123'), false],
    [join(guardRoot, 'recovery-termination.ABC123.extra'), false],
    [join(guardRoot, 'recovery-termination.ABC123/child'), false], [link, false],
  ]) {
    if (candidate !== link) mkdirSync(candidate, { recursive: true });
    rmSync(calls, { force: true });
    const result = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { :; }
record_exit() { exit "$1"; }
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

  executable(join(stubs, 'uname'), '#!/bin/bash\nif [[ "$*" == "-m" ]]; then printf "arm64\\n"; else printf "Darwin\\n"; fi\n');
  executable(join(stubs, 'sw_vers'), '#!/bin/bash\nprintf "stub macOS build\\n"\nexit "${SMOKE_OS_STATUS:-0}"\n');
  executable(join(stubs, 'ps'), `#!/bin/bash
set -euo pipefail
count=0
if [[ -f "$SMOKE_PS_CALLS" ]]; then read -r count < "$SMOKE_PS_CALLS"; fi
count=$((count + 1))
printf '%s\\n' "$count" > "$SMOKE_PS_CALLS"
if [[ "$count" == "$SMOKE_PS_FAIL_AT" ]]; then exit 23; fi
`);
  executable(join(stubs, 'mktemp'), `#!/bin/bash
if [[ "$SMOKE_STATE_FAIL" == 1 && "$*" == *vatori-recovery-termination.* ]]; then exit 17; fi
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
  executable(join(stubs, 'rm'), `#!/bin/bash
if [[ "\${SMOKE_RM_FAIL:-0}" == 1 && "$*" == *'/recovery-termination.'* ]]; then exit 24; fi
exec /bin/rm "$@"
`);
  const locked = `[[ -f "$SMOKE_LOCK/owner" && "$VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD" == 1 ]]`;
  executable(join(work, 'bin/vaadin-desktop'), `#!/bin/bash
set -euo pipefail
${locked}
[[ "$*" == "package test-apps/flow-over-ipc-framework-app" ]]
[[ "$VAADIN_DESKTOP_FLOW_IPC_SIDECAR_RESTART_LIMIT" == 2 ]]
[[ "$VAADIN_DESKTOP_FLOW_IPC_SIDECAR_RECOVERY_READY_TIMEOUT_SECONDS" == 15 ]]
[[ "$VAADIN_DESKTOP_FLOW_IPC_SIDECAR_RECOVERY_STABLE_RUN_SECONDS" == 30 ]]
[[ -z "\${VAADIN_DESKTOP_ACCEPTANCE_CAPABILITY_MANIFEST:-}" ]]
[[ -z "\${VAADIN_DESKTOP_ACCEPTANCE_OMIT_CAPABILITY_MANIFEST:-}" ]]
exit "$SMOKE_PACKAGE_STATUS"
`);
  executable(join(bin, 'build-host'), `#!/bin/bash
set -euo pipefail
${locked}
exit "$SMOKE_BUILD_STATUS"
`);
  executable(join(stubs, 'open'), `#!/usr/bin/env node
const fs=require('node:fs'),assert=require('node:assert/strict'),a=process.argv.slice(2);
const envArg=k=>a.find(x=>x.startsWith(k+'='))?.slice(k.length+1),app=a.at(-1),state=envArg('VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR');
const log=a[a.indexOf('-o')+1],err=a[a.indexOf('--stderr')+1];
assert(fs.existsSync(process.env.SMOKE_LOCK+'/owner'));
assert.equal(fs.readFileSync(log,'utf8'),'');assert.equal(fs.readFileSync(err,'utf8'),'');
const record={app,state,log,err,args:a,info:JSON.parse(fs.readFileSync(app+'/Contents/Info.plist','utf8')),
  stateMode:fs.statSync(state).mode&0o777,workMode:fs.statSync(require('node:path').dirname(app)).mode&0o777,
  inheritedDrivers:Object.keys(process.env).filter(k=>k.startsWith('VAADIN_DESKTOP_FLOW_IPC_AUTO_')),
  stop:process.env.VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION??'',
  driver:envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_RECOVERY_TERMINATION'),shutdown:envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN'),
  timeout:envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS'),noActivate:envArg('VAADIN_DESKTOP_MACOS_NO_ACTIVATE'),
  jdk:envArg('JAVA_HOME'),detail:envArg('VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL')};
fs.writeFileSync(process.env.SMOKE_RECEIPT,JSON.stringify(record));
if(process.env.SMOKE_OPEN==='fail')process.exit(9);
if(process.env.SMOKE_OPEN==='pass')fs.writeFileSync(log,'CURRENT_RECOVERY_TRACE\\n');
`);
  executable(join(work, 'bin/flow-ipc-trace-inspect'), `#!/bin/bash
set -euo pipefail
${locked}
printf '%s\\n' "$@" > "$SMOKE_INSPECTED"
[[ "$1" == --require-flow-ipc-recovery-termination ]]
[[ "$(< "$2")" == CURRENT_RECOVERY_TRACE ]]
exit "$SMOKE_INSPECT_STATUS"
`);
  const identities = new Set();
  const evidenceSnapshots = new Map();
  for (const scenario of ['pass', 'os-fail', 'jdk-fail', 'package-fail', 'build-fail', 'state-fail', 'open-fail',
    'empty-trace', 'inspect-fail', 'ps-fail', 'ps-late-fail', 'cleanup-fail',
    'manual-start', 'manual-cancel', 'manual-eof']) {
    writeFileSync(hostLog, 'PREVIOUS_VALID_TRACE'); writeFileSync(hostErr, 'PREVIOUS_ERROR');
    for (const path of [receipt, inspected, psCalls]) rmSync(path, { force: true });
    const env = { ...process.env, PATH: `${stubs}:${process.env.PATH}`, JAVA_HOME: jdk,
      SMOKE_LOCK: lock, SMOKE_RECEIPT: receipt, SMOKE_INSPECTED: inspected, SMOKE_PS_CALLS: psCalls,
      SMOKE_OS_STATUS: scenario === 'os-fail' ? '18' : '0',
      SMOKE_JDK_STATUS: scenario === 'jdk-fail' ? '19' : '0',
      SMOKE_PACKAGE_STATUS: scenario === 'package-fail' ? '6' : '0',
      SMOKE_BUILD_STATUS: scenario === 'build-fail' ? '7' : '0',
      SMOKE_STATE_FAIL: scenario === 'state-fail' ? '1' : '0',
      SMOKE_RM_FAIL: scenario === 'cleanup-fail' ? '1' : '0',
      SMOKE_INSPECT_STATUS: scenario === 'inspect-fail' ? '8' : '0',
      SMOKE_OPEN: scenario === 'open-fail' ? 'fail' : scenario === 'empty-trace' ? 'empty' : 'pass',
      SMOKE_PS_FAIL_AT: scenario === 'ps-fail' ? '1' : scenario === 'ps-late-fail' ? '2' : '0',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_DELIVERY: '1',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_OUTCOMES: '1',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_CANCEL_SANDBOX: '1',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS: '999',
      VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION: '1',
      VAADIN_DESKTOP_ACCEPTANCE_CAPABILITY_MANIFEST: '/untrusted/manifest',
      VAADIN_DESKTOP_ACCEPTANCE_OMIT_CAPABILITY_MANIFEST: '1',
      VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR: '/untrusted/state',
      VAADIN_DESKTOP_ACCEPTANCE_PACKAGE_LOCK_TIMEOUT_SECONDS: '0' };
    delete env.VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD;
    const manual = scenario.startsWith('manual-');
    const result = spawnSync('/bin/bash', [runner, ...(manual ? ['--wait-for-start'] : [])], {
      env, encoding: 'utf8', timeout: 15000,
      input: scenario === 'manual-start' ? 'start\n' : scenario === 'manual-cancel' ? 'cancel\n' : '' });
    assert.ifError(result.error);
    assert.equal(result.status, { pass: 0, 'os-fail': 18, 'jdk-fail': 19, 'package-fail': 6, 'build-fail': 7, 'state-fail': 17,
      'open-fail': 9, 'empty-trace': 1, 'inspect-fail': 8, 'ps-fail': 1, 'ps-late-fail': 1, 'cleanup-fail': 1,
      'manual-start': 0, 'manual-cancel': 2, 'manual-eof': 124 }[scenario],
    `${scenario}\n${result.stdout}\n${result.stderr}`);
    assert.equal(result.signal, null);
    assert(!existsSync(lock), 'package lock is released after inspection and cleanup');
    assert.equal(readFileSync(hostLog, 'utf8'), 'PREVIOUS_VALID_TRACE', 'legacy trace is preserved, never reused');
    assert.equal(readFileSync(hostErr, 'utf8'), 'PREVIOUS_ERROR');
    const evidence = result.stdout.match(/^Evidence directory: (.+)$/m)?.[1];
    assert.ok(evidence, `${scenario}: evidence location printed before build`);
    assert.equal(dirname(evidence), build);
    assert.match(basename(evidence), /^recovery-evidence\.[A-Za-z0-9]{6}$/);
    assert.equal(statSync(evidence).mode & 0o777, 0o700);
    assert.equal(readFileSync(join(evidence, 'runner-exit-status.txt'), 'utf8'), `${result.status}\n`,
      `${scenario}: saved final outcome includes environment, build, inspector and cleanup failures`);
    assert(!evidenceSnapshots.has(evidence), 'every retry gets a distinct directory');
    for (const [previous, files] of evidenceSnapshots) {
      for (const [name, content] of files) assert.equal(readFileSync(join(previous, name), 'utf8'), content,
        `${scenario}: earlier evidence ${name} was changed`);
    }
    assert.equal(readFileSync(join(evidence, 'host.err.log'), 'utf8'), '');
    assert.equal(existsSync(join(evidence, 'package.log')), !['os-fail', 'jdk-fail'].includes(scenario));
    assert.equal(readFileSync(join(evidence, 'macos-version.txt'), 'utf8'), 'stub macOS build\n');
    if (scenario !== 'os-fail') {
      assert.equal(readFileSync(join(evidence, 'architecture.txt'), 'utf8'), 'arm64\n');
      assert.equal(readFileSync(join(evidence, 'jdk-version.txt'), 'utf8'), 'stub JDK version\n');
    }
    assert(existsSync(join(evidence, 'source-revision.txt')));
    assert(existsSync(join(evidence, 'source-status.txt')));
    assert.equal(existsSync(inspected), ['pass', 'empty-trace', 'inspect-fail', 'ps-fail', 'ps-late-fail', 'cleanup-fail', 'manual-start'].includes(scenario));
    const shouldLaunch = !['os-fail', 'jdk-fail', 'package-fail', 'build-fail', 'state-fail', 'manual-cancel', 'manual-eof'].includes(scenario);
    assert.equal(existsSync(join(evidence, 'start-handoff.txt')), scenario === 'manual-start');
    if (manual) assert.match(result.stdout, /READY: app built/);
    assert.equal(existsSync(receipt), shouldLaunch);
    if (shouldLaunch) {
      const r = JSON.parse(readFileSync(receipt, 'utf8'));
      assert.equal(r.log, join(evidence, 'host.out.log'));
      assert.equal(r.err, join(evidence, 'host.err.log'));
      assert(existsSync(join(evidence, 'bundle-input-sha256.txt')));
      assert.deepEqual(r.inheritedDrivers, []); assert.equal(r.stop, '');
      assert.equal(r.driver, '1'); assert.equal(r.shutdown, '1'); assert.equal(r.timeout, '120');
      assert.equal(r.noActivate, '0'); assert.equal(r.detail, 'full'); assert.equal(r.jdk, jdk);
      assert(r.args.includes('-n') && r.args.includes('-W') && !r.args.includes('-g'));
      assert.equal(r.stateMode, 0o700); assert.equal(r.workMode, 0o700);
      assert.equal(dirname(r.state), dirname(r.app));
      assert.match(basename(r.state), /^vatori-recovery-termination\.[A-Za-z0-9]{6}$/);
      assert.match(basename(dirname(r.app)), /^recovery-termination\.[A-Za-z0-9]{6}$/);
      assert.equal(realpathSync(dirname(dirname(r.app))), realpathSync(build));
      assert.match(r.info.CFBundleIdentifier, /^dev\.vatori\.desktop\.acceptance\.recovery\.[0-9a-f-]{36}$/);
      assert(!identities.has(r.info.CFBundleIdentifier)); identities.add(r.info.CFBundleIdentifier);
      const retained = scenario.startsWith('ps-') || scenario === 'cleanup-fail';
      assert.equal(existsSync(dirname(r.app)), retained, `${scenario}: exact owned clone cleanup policy`);
      if (retained) {
        if (scenario.startsWith('ps-')) assert.match(result.stderr, /retaining clone/);
        rmSync(dirname(r.app), { recursive: true }); // Entirely inert test-owned clone.
      }
    }
    assert.deepEqual(readdirSync(build).filter(name => /^recovery-termination\.[A-Za-z0-9]{6}$/.test(name)), [],
      `${scenario}: no clone remains after confirmed no-process cleanup`);
    assert.equal(readFileSync(sentinel, 'utf8'), 'preserve unrelated material');
    assert.equal(readFileSync(join(neighbor, 'sentinel'), 'utf8'), 'preserve neighboring clone');
    evidenceSnapshots.set(evidence, readdirSync(evidence).map(name => [name, readFileSync(join(evidence, name), 'utf8')]));
    cases++;
  }
  const handoff=source.slice(source.indexOf('if [[ "$WAIT_FOR_START" != 0 ]]; then'), source.indexOf('for acceptance_flag in'));
  assert(handoff.startsWith('if [['));
  for (const scenario of ['start','cancel','directory','symlink','dangling-link']) {
    const evidence=mkdtempSync(join(work,'handoff-'));
    const child=spawn('/bin/bash',['-c',`set -euo pipefail\n${handoff}\necho LAUNCH`],
      {env:{...process.env,EVIDENCE_DIR:evidence,WAIT_FOR_START:'2'},stdio:['ignore','pipe','pipe']});
    let output='',sent=false,expired=false;
    const timer=setTimeout(()=>{expired=true;child.kill('SIGKILL');},5000);
    child.stdout.on('data',data=>{
      output+=data;
      if(output.includes('READY:')&&!sent) {
        sent=true; assert(!output.includes('LAUNCH'));
        const request=join(evidence,'start.request');
        if(scenario==='directory') mkdirSync(request);
        else if(scenario==='symlink') symlinkSync('/dev/null',request);
        else if(scenario==='dangling-link') symlinkSync('missing',request);
        else writeFileSync(request,scenario+'\n');
      }
    });
    const status=await new Promise((resolve,reject)=>{child.on('error',reject);child.on('close',resolve);});
    clearTimeout(timer); assert(!expired); assert(sent);
    assert.equal(status,scenario==='start'?0:2);
    assert.equal(output.includes('LAUNCH'),scenario==='start');
    cases++;
  }
  const retentionStart = source.indexOf('if [[ "$PASSIVE_ACTIVATION" == 1 ]]; then\n  cp -cRp');
  const retentionEnd = source.indexOf('echo "Only this clone', retentionStart);
  assert(retentionStart >= 0 && retentionEnd > retentionStart);
  const retentionApp = join(work, 'retention-input.app');
  const retentionSupport = join(work, 'retention-support');
  const retentionEvidence = mkdtempSync(join(work, 'retention-evidence-'));
  mkdirSync(retentionApp); mkdirSync(join(retentionSupport, 'Tests'), {recursive:true});
  writeFileSync(join(retentionApp, 'payload'), 'immutable test bundle');
  for (const name of ['recovery-sequence-process-verify.mjs', 'recovery-sequence-interaction-verify.mjs'])
    writeFileSync(join(retentionSupport, 'Tests', name), 'frozen verifier');
  const retention = spawnSync('/bin/bash', ['-c', 'set -euo pipefail\n' + source.slice(retentionStart, retentionEnd)],
    {encoding:'utf8', env:{...process.env, PATH:`${stubs}:${process.env.PATH}`, PASSIVE_ACTIVATION:'1',
      HOST_APP:retentionApp, ACCEPTANCE_ROOT:retentionSupport, EVIDENCE_DIR:retentionEvidence}});
  assert.equal(retention.status, 0, retention.stderr);
  assert.equal(readFileSync(join(retentionEvidence, 'Launched.app/payload'), 'utf8'), 'immutable test bundle');
  assert.equal(readFileSync(join(retentionEvidence, 'recovery-sequence-interaction-verify.mjs'), 'utf8'), 'frozen verifier');
  assert.equal(spawnSync('shasum', ['-a','256','-c',join(retentionEvidence,'retained-bundle-sha256.txt')]).status, 0);
  cases++;
  console.log(`Recovery termination runner smoke passed: ${cases} matcher/guard/end-to-end, retention and file-handoff cases (no real GUI/build)`);
} finally {
  rmSync(work, { recursive: true, force: true });
}
