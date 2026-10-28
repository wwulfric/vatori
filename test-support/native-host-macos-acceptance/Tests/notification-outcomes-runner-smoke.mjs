import assert from 'node:assert/strict';
import { appendFileSync, copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, dirname, join, resolve } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
if (process.platform !== 'darwin') {
  console.log('Notification outcomes runner smoke skipped: macOS temporary-clone layout required');
  process.exit(0);
}
const work = mkdtempSync(join(tmpdir(), 'vatori-notification-runner-smoke-'));
const support = join(work, 'test-support/native-host-macos-acceptance');
const bin = join(support, 'bin');
const build = join(work, '.build/native-hosts/macos');
const app = join(build, 'VaadinDesktopHost.app');
const stubs = join(work, 'stubs');
const jdk = join(work, 'jdk');
const receiptPath = join(work, 'launch.json');
const inspectPath = join(work, 'inspected');
const actionsPath = join(work, 'actions');
const lock = join(build, '.acceptance-package.lock');
const runner = join(bin, 'run-flow-notification-outcomes');
const delay = (milliseconds) => new Promise(resolveDelay => setTimeout(resolveDelay, milliseconds));
let liveChild;
let liveExit;
let lastReceipt;
let smokePassed = false;
const executable = (path, body) => writeFileSync(path, body, { mode: 0o700 });
try {
  for (const path of [bin, join(support, 'Fixtures'), join(work, 'bin'), stubs,
    join(app, 'Contents/MacOS'), join(app, 'Contents/Resources/Sidecar'), join(jdk, 'bin')]) {
    mkdirSync(path, { recursive: true });
  }
  for (const name of ['run-flow-notification-outcomes', 'run-flow-notification-delivery', 'with-package-lock']) {
    copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin', name), join(bin, name));
  }
  for (const name of ['notification-outcomes', 'notification-delivery']) {
    copyFileSync(join(root, `test-support/native-host-macos-acceptance/Fixtures/${name}-capabilities.json`),
      join(support, `Fixtures/${name}-capabilities.json`));
  }
  assert.deepEqual(JSON.parse(readFileSync(join(support, 'Fixtures/notification-delivery-capabilities.json'), 'utf8')),
    { schemaVersion: 1, permissionMode: 'manifest-enforced',
      windows: [{ label: 'main', commands: ['notification.show'] }] },
    'foreground delivery fixture grants exactly the one public capability under test');
  writeFileSync(join(app, 'Contents/Info.plist'), '{}');
  writeFileSync(join(app, 'Contents/MacOS/VaadinDesktopHost'), 'inert host fixture');
  writeFileSync(join(app, 'Contents/Resources/Sidecar/application.jar'), 'inert sidecar fixture');
  executable(join(jdk, 'bin/java'), 'inert user JDK fixture');
  const sentinel = join(build, 'unrelated-sentinel');
  writeFileSync(sentinel, 'preserve unrelated material');
  const neighbor = join(build, 'notification-outcomes.KEEP42.neighbor');
  mkdirSync(neighbor);
  const neighborSentinel = join(neighbor, 'sentinel');
  writeFileSync(neighborSentinel, 'preserve neighboring clone lookalike');

  // Validate ownership with synthetic rows. Never discover or signal a real
  // host/JVM while testing matching, including prefix lookalikes.
  const source = readFileSync(runner, 'utf8');
  const matcher = source.match(/owned_pids\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  assert.ok(matcher);
  const ownedHost = '/private/tmp/vatori-notification-outcomes.ABC123/VaadinDesktopHost.app/Contents/MacOS/VaadinDesktopHost';
  const ownedJar = '/private/tmp/vatori-notification-outcomes.ABC123/VaadinDesktopHost.app/Contents/Resources/Sidecar/application.jar';
  const aliasHost = ownedHost.replace('/private/tmp/', '/tmp/');
  const aliasJar = ownedJar.replace('/private/tmp/', '/tmp/');
  const rows = [
    `201 ${ownedHost}`, `202 ${aliasHost}`,
    `203 /fixture/jdk/bin/java -jar ${ownedJar}`,
    `204 /fixture/jdk/bin/java -jar ${aliasJar}`,
    `205 ${ownedHost}.neighbor`, `206 ${aliasHost}.neighbor`,
    `207 /fixture/jdk/bin/java -jar ${ownedJar}.neighbor`,
    `208 /fixture/jdk/bin/java -jar ${aliasJar}.neighbor --argument`,
    `209 /fixture/jdk/bin/java -jar ${ownedJar.replace('ABC123', 'DEF456')}`,
    `210 /bin/bash -c echo /fixture/jdk/bin/java -jar ${ownedJar} --argument`,
    `211 /other/jdk/bin/java -jar ${ownedJar}`,
    `212 /fixture/jdk/bin/java.neighbor -jar ${ownedJar}`,
    `213 /usr/bin/env /fixture/jdk/bin/java -jar ${ownedJar}`,
    `214 /fixture/wrapper /fixture/jdk/bin/java -jar ${aliasJar}`,
    `215 /bin/bash -c /fixture/jdk/bin/java -jar ${aliasJar} --argument`,
    `216 java -jar ${ownedJar}`,
    `217 /fixture/jdk/bin/java/child -jar ${ownedJar}`,
    `218 /fixture/jdk/bin/java -jar /unrelated.jar --note -jar ${ownedJar}`,
    `219 /fixture/jdk/bin/java -jar ${ownedJar} --argument`,
    `220 /fixture/jdk/bin/java -Xmx1g -jar ${ownedJar}`,
  ].join('\n');
  const matching = spawnSync('/bin/bash', ['-c',
    `set -euo pipefail\nps() { printf '%s\\n' "$VATORI_NOTIFICATION_PS_ROWS"; }\n${matcher}\nowned_pids\n`], {
    env: { ...process.env, JAVA_HOME: '/fixture/jdk', HOST_EXE: ownedHost,
      SIDECAR_JAR: ownedJar, VATORI_NOTIFICATION_PS_ROWS: rows },
    encoding: 'utf8', timeout: 5_000,
  });
  assert.ifError(matching.error);
  assert.equal(matching.status, 0, matching.stderr);
  assert.equal(matching.stdout, '201\n202\n203\n204\n');

  const buildHost = join(build, 'notification-outcomes.ABC123/VaadinDesktopHost.app/Contents/MacOS/VaadinDesktopHost');
  const buildJar = join(build, 'notification-outcomes.ABC123/VaadinDesktopHost.app/Contents/Resources/Sidecar/application.jar');
  const buildRows = [
    `301 ${buildHost}`, `302 /fixture/jdk/bin/java -jar ${buildJar}`,
    `303 ${buildHost}.neighbor`, `304 /fixture/jdk/bin/java -jar ${buildJar}.neighbor`,
    `305 /fixture/jdk/bin/java -jar ${buildJar.replace('ABC123', 'DEF456')}`,
    `306 ${ownedHost}`, `307 /fixture/jdk/bin/java -jar ${aliasJar}`,
    `308 /bin/bash -c echo /fixture/jdk/bin/java -jar ${buildJar} --argument`,
    `309 /other/jdk/bin/java -jar ${buildJar}`,
    `310 /fixture/jdk/bin/java.neighbor -jar ${buildJar}`,
    `311 /usr/bin/env /fixture/jdk/bin/java -jar ${buildJar}`,
    `312 /fixture/wrapper /fixture/jdk/bin/java -jar ${buildJar}`,
    `313 /fixture/jdk/bin/java -jar /unrelated.jar --note -jar ${buildJar}`,
    `314 /fixture/jdk/bin/java -jar ${buildJar} --argument`,
    `315 /fixture/jdk/bin/java -Dfixture=true -jar ${buildJar}`,
  ].join('\n');
  const buildMatching = spawnSync('/bin/bash', ['-c',
    `set -euo pipefail\nps() { printf '%s\\n' "$VATORI_NOTIFICATION_PS_ROWS"; }\n${matcher}\nowned_pids\n`], {
    env: { ...process.env, JAVA_HOME: '/fixture/jdk', HOST_EXE: buildHost,
      SIDECAR_JAR: buildJar, VATORI_NOTIFICATION_PS_ROWS: buildRows },
    encoding: 'utf8', timeout: 5_000,
  });
  assert.ifError(buildMatching.error);
  assert.equal(buildMatching.status, 0, buildMatching.stderr);
  assert.equal(buildMatching.stdout, '301\n302\n');

  // The configured JDK can itself use macOS's /private alias. Both exact Java
  // spellings are allowed, but shell/env/wrapper references are never owners.
  const javaAliasRows = [
    `401 /private/fixture/jdk/bin/java -jar ${ownedJar}`,
    `402 /fixture/jdk/bin/java -jar ${aliasJar}`,
    `403 /private/fixture/jdk/bin/java.neighbor -jar ${ownedJar}`,
    `404 /bin/bash -c /private/fixture/jdk/bin/java -jar ${ownedJar} --argument`,
    `405 /usr/bin/env /fixture/jdk/bin/java -jar ${aliasJar}`,
    `406 /private/other/jdk/bin/java -jar ${ownedJar}`,
    `407 /private/fixture/jdk/bin/java -jar ${ownedJar}.neighbor`,
    `408 /fixture/wrapper /private/fixture/jdk/bin/java -jar ${ownedJar}`,
    `409 /private/fixture/jdk/bin/java -jar /unrelated.jar --note -jar ${ownedJar}`,
    `410 /fixture/jdk/bin/java -jar /unrelated.jar --note -jar ${aliasJar}`,
  ].join('\n');
  const javaAliasMatching = spawnSync('/bin/bash', ['-c',
    `set -euo pipefail\nps() { printf '%s\\n' "$VATORI_NOTIFICATION_PS_ROWS"; }\n${matcher}\nowned_pids\n`], {
    env: { ...process.env, JAVA_HOME: '/private/fixture/jdk', HOST_EXE: ownedHost,
      SIDECAR_JAR: ownedJar, VATORI_NOTIFICATION_PS_ROWS: javaAliasRows },
    encoding: 'utf8', timeout: 5_000,
  });
  assert.ifError(javaAliasMatching.error);
  assert.equal(javaAliasMatching.status, 0, javaAliasMatching.stderr);
  assert.equal(javaAliasMatching.stdout, '401\n402\n');

  // Exercise the real cleanup guard without discovering/signaling processes or
  // deleting rejected paths. rm is a recording stub; end-to-end cases below
  // separately prove that the exact owned clone is actually removed.
  const cleanup = source.match(/cleanup\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  assert.ok(cleanup);
  const cleanupTraps = source.match(/^trap .* EXIT\ntrap .* INT\ntrap .* TERM\ntrap .* HUP$/m)?.[0];
  assert.ok(cleanupTraps);
  const guardBuild = join(work, 'cleanup-guard/build.with[regex]+chars');
  const guardNeighbor = `${guardBuild}-neighbor`;
  mkdirSync(guardBuild, { recursive: true });
  mkdirSync(guardNeighbor);
  const guardSentinel = join(guardBuild, 'unrelated-sentinel');
  const guardTargetSentinel = join(guardNeighbor, 'target-sentinel');
  writeFileSync(guardSentinel, 'preserve guard build');
  writeFileSync(guardTargetSentinel, 'preserve guard neighbor');
  const link = join(guardBuild, 'notification-outcomes.LINK12');
  symlinkSync(guardNeighbor, link);
  const guardCalls = join(work, 'cleanup-rm-calls');
  const cleanupCases = [
    [join(guardBuild, 'notification-outcomes.ABC123'), true],
    [guardBuild, false],
    [join(guardNeighbor, 'notification-outcomes.ABC123'), false],
    [join(work, 'notification-outcomes.ABC123'), false],
    [join(guardBuild, 'notification-outcomes.ABC123.extra'), false],
    [join(guardBuild, 'notification-outcomes.ABC123/child'), false],
    [join(guardBuild, 'notification-outcomes.ABC12'), false],
    [join(guardBuild, 'notification-outcomesx.ABC123'), false],
    [link, false],
  ];
  for (const [candidate, allowed] of cleanupCases) {
    if (candidate !== link) mkdirSync(candidate, { recursive: true });
    rmSync(guardCalls, { force: true });
    const guarded = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { :; }
kill() { exit 97; }
rm() { printf '%s\\n' "$@" > "$VATORI_NOTIFICATION_GUARD_CALLS"; }
${cleanup}
cleanup 0
`], {
      env: { ...process.env, WORK: candidate, BUILD_ROOT: guardBuild, OPEN_WAIT_PID: '',
        VATORI_NOTIFICATION_GUARD_CALLS: guardCalls },
      encoding: 'utf8', timeout: 5_000,
    });
    assert.ifError(guarded.error);
    assert.equal(guarded.status, allowed ? 0 : 1, `${candidate}\n${guarded.stderr}`);
    assert.equal(existsSync(guardCalls), allowed, `cleanup authorization for ${candidate}`);
    if (allowed) assert.equal(readFileSync(guardCalls, 'utf8'), `-rf\n--\n${candidate}\n`);
    else assert.match(guarded.stderr, /Refusing cleanup of unexpected clone path/);
    assert(existsSync(candidate), 'guard probe never actually deletes material');
    assert.equal(readFileSync(guardSentinel, 'utf8'), 'preserve guard build');
    assert.equal(readFileSync(guardTargetSentinel, 'utf8'), 'preserve guard neighbor');
  }

  // Deterministically deliver INT at EXIT-cleanup entry, before its first mask
  // command. Both the cleanup body and trap registrations come from production;
  // the wrapper only creates the interleaving observed in the plain-Bash probe.
  // The historical exit-only INT handler is a negative control: it skips all
  // ownership checks and removal, even though the outer status is still 130.
  const performCleanup = cleanup.replace(/^cleanup\(\)/, 'perform_cleanup()');
  for (const [variant, traps, completes] of [
    ['unified', cleanupTraps, true],
    ['exit-only-int', cleanupTraps.replace(/^trap .* INT$/m, "trap 'exit 130' INT"), false],
  ]) {
    const events = join(work, `cleanup-entry-${variant}-events`);
    const removals = join(work, `cleanup-entry-${variant}-removals`);
    const candidate = join(guardBuild, 'notification-outcomes.ABC123');
    const reentrant = spawnSync('/bin/bash', ['-c', `set -euo pipefail
owned_pids() { printf 'scan\\n' >> "$VATORI_NOTIFICATION_ENTRY_EVENTS"; }
kill() { exit 97; }
rm() {
  printf 'remove\\n' >> "$VATORI_NOTIFICATION_ENTRY_EVENTS"
  printf '%s\\n' "$@" > "$VATORI_NOTIFICATION_ENTRY_REMOVALS"
}
${performCleanup}
ENTRY_SIGNAL_SENT=0
cleanup() {
  printf 'entry:%s\\n' "$1" >> "$VATORI_NOTIFICATION_ENTRY_EVENTS"
  if [[ "$ENTRY_SIGNAL_SENT" == 0 ]]; then
    ENTRY_SIGNAL_SENT=1
    builtin kill -INT "$$"
  fi
  perform_cleanup "$@"
}
${traps}
exit 42
`], {
      env: { ...process.env, WORK: candidate, BUILD_ROOT: guardBuild, OPEN_WAIT_PID: '',
        VATORI_NOTIFICATION_ENTRY_EVENTS: events, VATORI_NOTIFICATION_ENTRY_REMOVALS: removals },
      encoding: 'utf8', timeout: 5_000,
    });
    assert.ifError(reentrant.error);
    assert.equal(reentrant.status, 130, `${variant}\n${reentrant.stderr}`);
    assert.equal(reentrant.signal, null);
    assert.equal(readFileSync(events, 'utf8'), completes
      ? 'entry:42\nentry:130\nscan\nscan\nremove\n' : 'entry:42\n');
    assert.equal(existsSync(removals), completes);
    if (completes) assert.equal(readFileSync(removals, 'utf8'), `-rf\n--\n${candidate}\n`);
    assert(existsSync(candidate), 'entry probe never actually deletes material');
    assert.equal(readFileSync(guardSentinel, 'utf8'), 'preserve guard build');
    assert.equal(readFileSync(guardTargetSentinel, 'utf8'), 'preserve guard neighbor');
  }

  // A deterministic second signal during cleanup must not skip the final
  // ownership scan. This probes signal resilience, not the cause of any
  // intermittent single-SIGINT failure in the full runner. All functions that
  // discover, terminate or delete material are inert recording stubs here.
  for (const signal of ['SIGINT', 'SIGTERM', 'SIGHUP']) {
    const scans = join(work, `cleanup-${signal}-scans`);
    const removals = join(work, `cleanup-${signal}-removals`);
    const candidate = join(guardBuild, 'notification-outcomes.ABC123');
    const cleanupProbe = spawn('/bin/bash', ['-c', `set -euo pipefail
owned_pids() {
  if [[ ! -e "$VATORI_NOTIFICATION_SIGNAL_SCANS" ]]; then
    printf 'scan\\n' > "$VATORI_NOTIFICATION_SIGNAL_SCANS"
    printf 'CLEANUP_SCAN_READY\\n' >&2
    IFS= read -r release
    [[ "$release" == resume ]]
  else
    printf 'scan\\n' >> "$VATORI_NOTIFICATION_SIGNAL_SCANS"
  fi
}
kill() { exit 97; }
rm() { printf '%s\\n' "$@" > "$VATORI_NOTIFICATION_SIGNAL_REMOVALS"; }
${cleanup}
${cleanupTraps}
exit 130
`], {
      env: { ...process.env, WORK: candidate, BUILD_ROOT: guardBuild, OPEN_WAIT_PID: '',
        VATORI_NOTIFICATION_SIGNAL_SCANS: scans, VATORI_NOTIFICATION_SIGNAL_REMOVALS: removals },
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    let output = '', probeClosed = false;
    const ready = new Promise(resolveReady => cleanupProbe.stderr.on('data', data => {
      output += data;
      if (output.includes('CLEANUP_SCAN_READY\n')) resolveReady();
    }));
    cleanupProbe.stdout.on('data', data => { output += data; });
    // A failed assertion must still release the blocked, test-owned subshell.
    cleanupProbe.stdin.on('error', () => {});
    const probeExit = new Promise(resolveExit => cleanupProbe.once('close', (code, exitSignal) => {
      probeClosed = true;
      resolveExit({ code, signal: exitSignal });
    }));
    try {
      await Promise.race([ready, probeExit.then(() => { throw new Error(`cleanup probe exited before scan: ${output}`); }),
        delay(5_000).then(() => { throw new Error(`cleanup scan handshake timed out: ${output}`); })]);
      assert.equal(readFileSync(scans, 'utf8'), 'scan\n');
      assert.equal(cleanupProbe.kill(signal), true, `send ${signal} only to the test-owned cleanup shell`);
      cleanupProbe.stdin.end('resume\n');
      const stopped = await Promise.race([probeExit,
        delay(5_000).then(() => { throw new Error(`cleanup ${signal} probe timed out: ${output}`); })]);
      assert.deepEqual(stopped, { code: 130, signal: null }, output);
      assert.equal(readFileSync(scans, 'utf8'), 'scan\nscan\n', `${signal} must not interrupt the second ownership scan`);
      assert.equal(readFileSync(removals, 'utf8'), `-rf\n--\n${candidate}\n`);
      assert(existsSync(candidate), 'signal probe records deletion without deleting material');
      assert.equal(readFileSync(guardSentinel, 'utf8'), 'preserve guard build');
      assert.equal(readFileSync(guardTargetSentinel, 'utf8'), 'preserve guard neighbor');
    } finally {
      if (!probeClosed) {
        cleanupProbe.stdin.end();
        cleanupProbe.kill('SIGKILL');
        await Promise.race([probeExit, delay(5_000)]);
      }
    }
  }

  // Canceling the wrapper's watchdog must stop after sleep fails, including
  // when its parent has disabled errexit. Exercise the actual subshell body
  // with inert sleep/kill functions; no process group can be signaled here.
  const lockSource = readFileSync(join(bin, 'with-package-lock'), 'utf8');
  const watchdog = lockSource.match(/    \(\n([\s\S]*?)\n    \) &\n    escalation_pid=\$!/)?.[1];
  assert.ok(watchdog);
  for (const [variant, body, sleepStatus, escalates] of [
    ['cancelled', watchdog, 143, false],
    ['expired', watchdog, 0, true],
    ['missing-cancel-fence', watchdog.replace(/(sleep "\$CHILD_PROCESS_GROUP_TERM_GRACE_SECONDS") \|\| exit 0/, '$1'), 143, true],
  ]) {
    const events = join(work, `watchdog-${variant}-events`);
    const probe = spawnSync('/bin/bash', ['-c', `set -uo pipefail
sleep() {
  printf 'sleep:%s\\n' "$*" >> "$VATORI_NOTIFICATION_WATCHDOG_EVENTS"
  return "$VATORI_NOTIFICATION_WATCHDOG_SLEEP_STATUS"
}
kill() { printf 'kill:%s\\n' "$*" >> "$VATORI_NOTIFICATION_WATCHDOG_EVENTS"; }
(
${body}
)
`], {
      env: { ...process.env, CHILD_PROCESS_GROUP: '424242', CHILD_PROCESS_GROUP_TERM_GRACE_SECONDS: '5',
        VATORI_NOTIFICATION_WATCHDOG_EVENTS: events, VATORI_NOTIFICATION_WATCHDOG_SLEEP_STATUS: String(sleepStatus) },
      encoding: 'utf8', timeout: 5_000,
    });
    assert.ifError(probe.error);
    assert.equal(probe.status, 0, `${variant}\n${probe.stderr}`);
    assert.equal(probe.signal, null);
    assert.equal(readFileSync(events, 'utf8'), escalates ? 'sleep:5\nkill:-KILL -- -424242\n' : 'sleep:5\n');
  }

  executable(join(stubs, 'uname'), '#!/usr/bin/env bash\nprintf "Darwin\\n"\n');
  executable(join(stubs, 'ps'), `#!/usr/bin/env bash
set -euo pipefail
count=0
if [[ -f "$VATORI_NOTIFICATION_PS_CALLS" ]]; then read -r count < "$VATORI_NOTIFICATION_PS_CALLS"; fi
count=$((count + 1))
printf '%s\\n' "$count" > "$VATORI_NOTIFICATION_PS_CALLS"
if [[ "$count" == "\${VATORI_NOTIFICATION_PS_FAIL_AT:-0}" ]]; then exit 23; fi
exit "\${VATORI_NOTIFICATION_PS_STATUS:-0}"
`);
  executable(join(stubs, 'ditto'), `#!/usr/bin/env node
require('node:fs').cpSync(process.argv[2], process.argv[3], {recursive:true});
`);
  executable(join(stubs, 'plutil'), `#!/usr/bin/env node
const fs=require('node:fs'), args=process.argv.slice(2), path=args.at(-1);
const value=JSON.parse(fs.readFileSync(path,'utf8'));
value[args[1]]=args[3];
fs.writeFileSync(path,JSON.stringify(value));
`);
  executable(join(stubs, 'codesign'), `#!/usr/bin/env bash
printf 'codesign %s\\n' "$*" >> "$VATORI_NOTIFICATION_ACTIONS"
exit 0
`);
  const checkLock = `[[ -f "$VATORI_NOTIFICATION_LOCK/owner" ]]
[[ "\${VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD:-}" == 1 ]]
`;
  executable(join(work, 'bin/vaadin-desktop'), `#!/usr/bin/env bash
set -euo pipefail
${checkLock}
[[ "$*" == "package test-apps/flow-over-ipc-framework-app" ]]
[[ ! -s "$VATORI_NOTIFICATION_HOST_LOG" && ! -s "$VATORI_NOTIFICATION_HOST_ERR" ]]
printf 'package\\n' >> "$VATORI_NOTIFICATION_ACTIONS"
exit "$VATORI_NOTIFICATION_PACKAGE_STATUS"
`);
  executable(join(bin, 'build-host'), `#!/usr/bin/env bash
set -euo pipefail
${checkLock}
printf 'build-host\\n' >> "$VATORI_NOTIFICATION_ACTIONS"
exit "$VATORI_NOTIFICATION_BUILD_STATUS"
`);
  executable(join(stubs, 'open'), `#!/usr/bin/env node
const fs=require('node:fs'), assert=require('node:assert/strict'), args=process.argv.slice(2);
const envArg=name=>args.find(arg=>arg.startsWith(name+'='))?.slice(name.length+1);
const app=args.at(-1), log=args[args.indexOf('-o')+1], err=args[args.indexOf('--stderr')+1];
assert(fs.existsSync(process.env.VATORI_NOTIFICATION_LOCK+'/owner'));
assert.equal(fs.readFileSync(log,'utf8'),'');
assert.equal(fs.readFileSync(err,'utf8'),'');
const record={pid:process.pid,app,args,log,err,
  info:JSON.parse(fs.readFileSync(app+'/Contents/Info.plist','utf8')),
  mode:envArg('VAADIN_DESKTOP_FLOW_IPC_NOTIFICATION_EXPECTED_OUTCOME'),
  noActivate:envArg('VAADIN_DESKTOP_MACOS_NO_ACTIVATE'),
  driver:envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_OUTCOMES'),
  deliveryDriver:envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_DELIVERY'),
  shutdown:envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN'),
  timeout:envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS'),
  trace:envArg('VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL'),
  jdk:envArg('JAVA_HOME'),
  inheritedDrivers:Object.keys(process.env).filter(key=>key.startsWith('VAADIN_DESKTOP_FLOW_IPC_AUTO_')),
  stop:process.env.VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION??'',
  omitManifest:process.env.VAADIN_DESKTOP_ACCEPTANCE_OMIT_CAPABILITY_MANIFEST??'',
  manifest:process.env.VAADIN_DESKTOP_ACCEPTANCE_CAPABILITY_MANIFEST};
const behavior=process.env.VATORI_NOTIFICATION_OPEN;
if(behavior==='hold') {
  for(const signal of ['SIGINT','SIGTERM']) process.on(signal,()=>process.exit(signal==='SIGINT'?130:143));
  setInterval(()=>{},1000);
}
// The receipt is a readiness handshake, not merely evidence that open started.
fs.writeFileSync(process.env.VATORI_NOTIFICATION_RECEIPT,JSON.stringify(record));
if(behavior==='fail') process.exit(9);
else if(behavior!=='hold') {
  if(behavior==='pass') fs.writeFileSync(log,'CURRENT_NOTIFICATION_TRACE\\n');
  process.exit(0);
}
`);
  executable(join(work, 'bin/flow-ipc-trace-inspect'), `#!/usr/bin/env bash
set -euo pipefail
${checkLock}
printf '%s\\n' "$@" > "$VATORI_NOTIFICATION_INSPECT"
[[ "$1" == "$VATORI_NOTIFICATION_EXPECTED_GATE" ]]
[[ "$(< "$2")" == CURRENT_NOTIFICATION_TRACE ]]
exit "$VATORI_NOTIFICATION_INSPECT_STATUS"
`);

  const prepare = (mode, extra = {}) => {
    const hostLog = join(build, `host-notification-${mode}.out.log`);
    const hostErr = join(build, `host-notification-${mode}.err.log`);
    writeFileSync(hostLog, 'PREVIOUS_VALID_TRACE');
    writeFileSync(hostErr, 'PREVIOUS_ERRORS');
    for (const path of [receiptPath, inspectPath, actionsPath, join(work, 'ps-calls')]) rmSync(path, { force: true });
    const env = { ...process.env, PATH: `${stubs}:${process.env.PATH}`, JAVA_HOME: jdk,
      VATORI_NOTIFICATION_LOCK: lock, VATORI_NOTIFICATION_ACTIONS: actionsPath,
      VATORI_NOTIFICATION_HOST_LOG: hostLog, VATORI_NOTIFICATION_HOST_ERR: hostErr,
      VATORI_NOTIFICATION_RECEIPT: receiptPath, VATORI_NOTIFICATION_INSPECT: inspectPath,
      VATORI_NOTIFICATION_PACKAGE_STATUS: '0', VATORI_NOTIFICATION_BUILD_STATUS: '0',
      VATORI_NOTIFICATION_PS_STATUS: '0',
      VATORI_NOTIFICATION_PS_CALLS: join(work, 'ps-calls'), VATORI_NOTIFICATION_PS_FAIL_AT: '0',
      VATORI_NOTIFICATION_INSPECT_STATUS: '0', VATORI_NOTIFICATION_OPEN: 'pass',
      VATORI_NOTIFICATION_EXPECTED_GATE: mode === 'foreground-delivery'
        ? '--require-flow-ipc-notification-delivery' : '--require-flow-ipc-notification-outcomes',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS: '1',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE: '1',
      VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_CANCEL_SANDBOX: '1',
      VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION: '1',
      VAADIN_DESKTOP_ACCEPTANCE_OMIT_CAPABILITY_MANIFEST: '1',
      VAADIN_DESKTOP_ACCEPTANCE_PACKAGE_LOCK_TIMEOUT_SECONDS: '0',
      ...extra };
    delete env.VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD;
    return { env, hostLog, hostErr };
  };
  const verifyReceipt = (mode) => {
    const receipt = JSON.parse(readFileSync(receiptPath, 'utf8'));
    lastReceipt = receipt;
    assert.equal(receipt.mode, mode);
    assert.equal(receipt.noActivate, mode === 'authorization-required' ? '1' : '0');
    assert.equal(receipt.args.includes('-g'), false);
    assert.ok(receipt.args.includes('-n'));
    assert.ok(receipt.args.includes('-W'));
    assert.equal(receipt.driver, mode === 'foreground-delivery' ? undefined : '1');
    assert.equal(receipt.deliveryDriver, mode === 'foreground-delivery' ? '1' : undefined);
    assert.equal(receipt.shutdown, '1');
    assert.equal(receipt.trace, 'full');
    assert.equal(receipt.jdk, jdk);
    assert.deepEqual(receipt.inheritedDrivers, []);
    assert.equal(receipt.stop, '');
    assert.equal(receipt.omitManifest, '');
    assert.equal(receipt.manifest, join(support, mode === 'foreground-delivery'
      ? 'Fixtures/notification-delivery-capabilities.json' : 'Fixtures/notification-outcomes-capabilities.json'));
    assert.match(receipt.info.CFBundleIdentifier,
      new RegExp(`^dev\\.vatori\\.desktop\\.acceptance\\.notification\\.${mode}\\.[0-9a-f-]{36}$`));
    assert.equal(basename(receipt.app), 'VaadinDesktopHost.app');
    assert.match(basename(dirname(receipt.app)), /^notification-outcomes\.[A-Za-z0-9]{6}$/);
    assert.equal(realpathSync(dirname(dirname(receipt.app))), realpathSync(build),
      'clone must be a direct child of the runner build directory, not /tmp');
    const actions = readFileSync(actionsPath, 'utf8');
    assert(actions.includes(`codesign --force --sign - ${receipt.app}\n`));
    assert(actions.includes(`codesign --verify --strict --verbose=2 ${receipt.app}\n`));
    return receipt;
  };
  const identities = new Set();
  for (const mode of ['authorization-required', 'os-denied', 'scheduled', 'foreground-delivery']) {
    for (const scenario of ['pass', 'package-fail', 'build-fail', 'open-fail', 'empty-trace', 'inspect-fail', 'ps-fail', 'ps-late-fail']) {
      const fixture = prepare(mode, {
        VATORI_NOTIFICATION_PACKAGE_STATUS: scenario === 'package-fail' ? '6' : '0',
        VATORI_NOTIFICATION_BUILD_STATUS: scenario === 'build-fail' ? '7' : '0',
        VATORI_NOTIFICATION_OPEN: scenario === 'open-fail' ? 'fail' : scenario === 'empty-trace' ? 'empty' : 'pass',
        VATORI_NOTIFICATION_INSPECT_STATUS: scenario === 'inspect-fail' ? '8' : '0',
        VATORI_NOTIFICATION_PS_STATUS: scenario === 'ps-fail' ? '23' : '0',
        VATORI_NOTIFICATION_PS_FAIL_AT: scenario === 'ps-late-fail' ? '2' : '0',
      });
      const command = mode === 'foreground-delivery'
        ? [join(bin, 'run-flow-notification-delivery')] : [runner, mode];
      const result = spawnSync('/bin/bash', command, {
        env: fixture.env, encoding: 'utf8', timeout: 15_000,
      });
      assert.ifError(result.error);
      const expected = { pass: 0, 'package-fail': 6, 'build-fail': 7, 'open-fail': 9, 'empty-trace': 1, 'inspect-fail': 8 }[scenario];
      const psFailed = scenario === 'ps-fail' || scenario === 'ps-late-fail';
      if (psFailed) assert.notEqual(result.status, 0, `${scenario}: failed process discovery must not pass\n${result.stdout}\n${result.stderr}`);
      else assert.equal(result.status, expected, `${mode}/${scenario}\n${result.stdout}\n${result.stderr}`);
      assert.equal(result.signal, null);
      assert.equal(existsSync(lock), false, 'package lock must outlive verification and then release');
      assert.equal(readFileSync(fixture.hostErr, 'utf8'), '');
      assert(!readFileSync(fixture.hostLog, 'utf8').includes('PREVIOUS_VALID_TRACE'));
      assert.equal(existsSync(inspectPath), ['pass', 'empty-trace', 'inspect-fail', 'ps-fail', 'ps-late-fail'].includes(scenario));
      if (!['package-fail', 'build-fail'].includes(scenario)) {
        const receipt = verifyReceipt(mode);
        assert(!identities.has(receipt.info.CFBundleIdentifier), 'each clone must have a fresh OS identity');
        identities.add(receipt.info.CFBundleIdentifier);
        assert.equal(existsSync(dirname(receipt.app)), psFailed,
          'cleanup must retain the clone when process discovery fails');
        if (psFailed) {
          // ps is deliberately failed, but this test never starts a host/JVM and
          // open already exited. Remove only this proven inert, owned test clone.
          assert.equal(readFileSync(join(receipt.app, 'Contents/MacOS/VaadinDesktopHost'), 'utf8'), 'inert host fixture');
          assert.throws(() => process.kill(receipt.pid, 0), /ESRCH/);
          rmSync(dirname(receipt.app), { recursive: true, force: true });
        }
        if (mode === 'authorization-required') assert.match(result.stdout, /No OS authorization prompt is expected or permitted/);
        else assert.match(result.stdout, mode === 'os-denied' ? /External operator: choose Don't Allow/ : /External operator: choose Allow/);
      } else assert.equal(existsSync(receiptPath), false);
      assert.equal(readFileSync(sentinel, 'utf8'), 'preserve unrelated material');
      assert.equal(readFileSync(neighborSentinel, 'utf8'), 'preserve neighboring clone lookalike');
      assert.equal(readFileSync(join(app, 'Contents/MacOS/VaadinDesktopHost'), 'utf8'), 'inert host fixture');
      assert.equal(readFileSync(join(jdk, 'bin/java'), 'utf8'), 'inert user JDK fixture');
    }
  }

  // Hold only the fake open process. A second runner must not cross the real
  // package lock, and SIGINT must clean the first runner's clone and child.
  const signalXtrace = process.env.VATORI_NOTIFICATION_RUNNER_SMOKE_TRACE === '1';
  for (let attempt = 0; attempt < 3; attempt++) {
    // The normal regression is untraced so xtrace cannot hide its timing. Opt
    // into cross-exec Bash diagnostics only when investigating a new failure.
    // Outer status 130 alone does not prove that inner EXIT cleanup completed.
    const signalTrace = join(work, `signal-attempt-${attempt + 1}.bash.trace`);
    const shellOptions = (process.env.SHELLOPTS ?? '').split(':').filter(option => option && option !== 'xtrace');
    if (signalXtrace) shellOptions.push('xtrace');
    const held = prepare('authorization-required', { VATORI_NOTIFICATION_OPEN: 'hold',
      SHELLOPTS: [...new Set(shellOptions)].join(':'),
      ...(signalXtrace ? { PS4: '+ pid=$$ ppid=$PPID sub=$BASH_SUBSHELL sec=$SECONDS src=${BASH_SOURCE##*/}:$LINENO fn=${FUNCNAME[0]:-main} status=$? ' } : {}),
    });
    writeFileSync(signalTrace, `HARNESS attempt=${attempt + 1} xtrace=${signalXtrace} started=${Date.now()}\n`);
    liveChild = spawn('/bin/bash', [runner, 'authorization-required'], { env: held.env, stdio: ['ignore', 'pipe', 'pipe'] });
    let heldOutput = '';
    const recordHeldOutput = data => {
      heldOutput += data;
      appendFileSync(signalTrace, data);
    };
    liveChild.stdout.on('data', recordHeldOutput);
    liveChild.stderr.on('data', recordHeldOutput);
    const heldExit = new Promise(resolveExit => liveChild.once('close', (code, signal) => {
      appendFileSync(signalTrace, `\nHARNESS closed=${Date.now()} code=${code} signal=${signal}\n`);
      resolveExit({ code, signal });
    }));
    liveExit = heldExit;
    const receiptDeadline = Date.now() + 10_000;
    while (!existsSync(receiptPath) && Date.now() < receiptDeadline) await delay(25);
    assert(existsSync(receiptPath), heldOutput);
    const heldReceipt = verifyReceipt('authorization-required');
    appendFileSync(signalTrace, `\nHARNESS ready=${Date.now()} outer=${liveChild.pid} fakeOpen=${heldReceipt.pid}\n`);
    const blocked = spawnSync('/bin/bash', [runner, 'scheduled'], { env: held.env, encoding: 'utf8', timeout: 5_000 });
    assert.ifError(blocked.error);
    assert.equal(blocked.status, 75, blocked.stderr);
    assert(existsSync(lock));
    appendFileSync(signalTrace, `\nHARNESS signal=SIGINT sent=${Date.now()} blockedStatus=${blocked.status}\n`);
    liveChild.kill('SIGINT');
    const stopped = await Promise.race([heldExit, delay(15_000).then(() => { throw new Error(`SIGINT cleanup timed out: ${heldOutput}`); })]);
    liveChild = undefined;
    const psCalls = existsSync(join(work, 'ps-calls')) ? readFileSync(join(work, 'ps-calls'), 'utf8').trim() : 'none';
    const signalDiagnostics = `SIGINT attempt ${attempt + 1}, ps calls=${psCalls}, trace=${signalTrace}\n${heldOutput}`;
    assert.equal(stopped.code, 130, heldOutput);
    assert.equal(stopped.signal, null);
    assert.equal(existsSync(lock), false);
    assert.equal(existsSync(dirname(heldReceipt.app)), false, signalDiagnostics);
    assert.throws(() => process.kill(heldReceipt.pid, 0), /ESRCH/);
    assert.equal(existsSync(inspectPath), false);
  }

  const timeoutFixture = prepare('authorization-required', { VATORI_NOTIFICATION_OPEN: 'hold' });
  const timedOut = spawnSync('/bin/bash', [runner, 'authorization-required'], {
    env: timeoutFixture.env, encoding: 'utf8', timeout: 45_000,
  });
  assert.ifError(timedOut.error);
  assert.equal(timedOut.status, 124, `${timedOut.stdout}\n${timedOut.stderr}`);
  assert.match(timedOut.stderr, /external runtime deadline/);
  const timeoutReceipt = verifyReceipt('authorization-required');
  assert.equal(existsSync(dirname(timeoutReceipt.app)), false);
  assert.equal(existsSync(lock), false);
  assert.throws(() => process.kill(timeoutReceipt.pid, 0), /ESRCH/);
  assert.equal(existsSync(inspectPath), false);
  assert.equal(readFileSync(sentinel, 'utf8'), 'preserve unrelated material');
  assert.equal(readFileSync(neighborSentinel, 'utf8'), 'preserve neighboring clone lookalike');
  smokePassed = true;
  console.log('Notification outcomes runner freshness, identity, ownership, lock, signal and timeout smoke passed');
} finally {
  let childStopped = true;
  if (liveChild) {
    liveChild.kill('SIGTERM');
    childStopped = await Promise.race([liveExit.then(() => true), delay(15_000).then(() => false)]);
  }
  // Clones now live inside this fixture root. Preserve the entire root on any
  // assertion failure, including failures before a launch receipt is inspected.
  if (lastReceipt && existsSync(dirname(lastReceipt.app))) {
    console.error(`Notification stub clone retained for inspection: ${dirname(lastReceipt.app)}`);
  }
  if (smokePassed && childStopped) rmSync(work, { recursive: true, force: true });
  else console.error(`Notification runner smoke fixture retained for inspection: ${work}`);
}
