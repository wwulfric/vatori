import assert from 'node:assert/strict';
import { chmodSync, copyFileSync, existsSync, linkSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { homedir, tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
if (process.platform !== 'darwin') {
  console.log('Sandbox file-write-grant runner smoke skipped: macOS filesystem tools required');
  process.exit(0);
}
const work = mkdtempSync(join(tmpdir(), 'vatori-sandbox-runner-smoke-'));
try {
  const support = join(work, 'test-support/native-host-macos-acceptance');
  const bin = join(support, 'bin');
  const build = join(work, '.build/native-hosts/macos');
  const app = join(build, 'VaadinDesktopHost.app');
  const jdk = join(work, 'jdk');
  const stubs = join(work, 'stubs');
  for (const path of [bin, join(support, 'Fixtures'), join(support, 'Tests'), join(work, 'bin'),
    join(app, 'Contents'), join(jdk, 'bin'), join(jdk, 'lib/server'), stubs]) {
    mkdirSync(path, { recursive: true });
  }
  const runner = join(bin, 'run-flow-file-write-grant-sandbox');
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-sandbox'), runner);
  // Exercise the actual matcher with synthetic ps rows only: no real process
  // discovery, kill, or cleanup is permitted in this scope test.
  const matcher = readFileSync(runner, 'utf8').match(/owned_pids\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  assert.ok(matcher, 'runner owned_pids helper must be discoverable');
  const ownedApp = '/private/tmp/vatori-sandbox-file-write-grant.matcher/VaadinDesktopHost.app';
  const ownedHost = `${ownedApp}/Contents/MacOS/VaadinDesktopHost`;
  const ownedJava = `${ownedApp}/Contents/JavaRuntime/bin/java`;
  const aliasHost = ownedHost.replace(/^\/private\/tmp\//, '/tmp/');
  const aliasJava = ownedJava.replace(/^\/private\/tmp\//, '/tmp/');
  const syntheticProcesses = [
    `201 ${ownedHost}`, `202 ${aliasHost}`,
    `203 ${ownedJava} -jar ${ownedApp}/Contents/Resources/Sidecar/application.jar`,
    `204 ${aliasJava} -jar /tmp/selected-sidecar.jar`,
    `205 ${ownedHost.replace('.matcher/', '.neighbor/')}`,
    `206 ${aliasJava.replace('.matcher/', '.neighbor/')} -jar /tmp/other.jar`,
    `207 ${ownedHost}-lookalike`, `208 ${aliasHost}-lookalike`,
    `209 ${ownedJava}-lookalike -jar /tmp/other.jar`,
    `210 ${aliasJava}-lookalike -jar /tmp/other.jar`,
    `211 ${ownedJava} -version`, `212 ${aliasJava} -version`,
  ].join('\n');
  const matched = spawnSync('bash', ['-c',
    `set -euo pipefail\nps() { printf '%s\\n' "$VATORI_MATCHER_ROWS"; }\n${matcher}\nowned_pids\n`], {
    env: { ...process.env, HOST_EXE: ownedHost, JAVA_EXE: ownedJava, VATORI_MATCHER_ROWS: syntheticProcesses },
    encoding: 'utf8',
  });
  assert.ifError(matched.error);
  assert.equal(matched.status, 0, matched.stderr);
  assert.equal(matched.stdout, '201\n202\n203\n204\n', 'only four exact owned executable paths may match');
  const handoffCleanup = readFileSync(runner, 'utf8').match(/cleanup_handoff\(\) \{\n[\s\S]*?\n\}\n/)?.[0];
  assert.ok(handoffCleanup, 'bounded private-handoff cleanup must be discoverable');
  const fakeContainerTmp = join(work, 'fake-container-tmp');
  mkdirSync(fakeContainerTmp);
  const privateRoot = join(fakeContainerTmp, 'vatori-file-write-grant-replay.abc123');
  const privateRecord = join(privateRoot, 'old-grant-token.bin');
  const neighbor = join(work, 'private-neighbor');
  writeFileSync(neighbor, 'never remove adjacent material', { mode: 0o600 });
  const cleanHandoff = (overrides = {}) => spawnSync('bash', ['-c',
    `set -euo pipefail\n${handoffCleanup}\ncleanup_handoff\n`], {
    env: { ...process.env, HANDOFF_CONTAINER_TMP: fakeContainerTmp,
      HANDOFF_ROOT: privateRoot, REPLAY_HANDOFF: privateRecord, ...overrides }, encoding: 'utf8',
  });
  assert.equal(cleanHandoff().status, 0, 'an uncreated root is harmless');
  for (const kind of ['record', 'empty', 'file-symlink', 'root-symlink', 'hard-link', 'wide-mode', 'wide-file-mode', 'extra-child']) {
    if (kind === 'root-symlink') symlinkSync(fakeContainerTmp, privateRoot);
    else {
      mkdirSync(privateRoot, { mode: 0o700 });
      if (kind === 'file-symlink') symlinkSync(neighbor, privateRecord);
      else if (kind === 'hard-link') linkSync(neighbor, privateRecord);
      else if (kind !== 'empty') writeFileSync(privateRecord, 'private fixture, no live token', { mode: 0o600 });
      if (kind === 'wide-mode') chmodSync(privateRoot, 0o755);
      if (kind === 'wide-file-mode') chmodSync(privateRecord, 0o644);
      if (kind === 'extra-child') writeFileSync(join(privateRoot, 'unexpected'), 'must remain');
    }
    const result = cleanHandoff();
    assert.ifError(result.error);
    assert.equal(result.status === 0, ['record', 'empty'].includes(kind), result.stderr);
    if (['record', 'empty'].includes(kind)) assert.equal(existsSync(privateRoot), false);
    else assert.equal(existsSync(privateRoot), true, 'unsafe or nonempty root must remain');
    assert.equal(readFileSync(neighbor, 'utf8'), 'never remove adjacent material');
    rmSync(privateRoot, { recursive: true, force: true });
  }
  assert.notEqual(cleanHandoff({ HANDOFF_ROOT: fakeContainerTmp,
    REPLAY_HANDOFF: join(fakeContainerTmp, 'old-grant-token.bin') }).status, 0, 'container root is never cleanup material');
  assert.equal(existsSync(fakeContainerTmp), true);
  assert.notEqual(cleanHandoff({ REPLAY_HANDOFF: neighbor }).status, 0, 'arbitrary record paths are never cleanup material');
  assert.equal(readFileSync(neighbor, 'utf8'), 'never remove adjacent material');
  const ownerCloseRunner = join(bin, 'run-flow-file-write-grant-owner-close-sandbox');
  const cancelRunner = join(bin, 'run-flow-file-write-grant-cancel-sandbox');
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-cancel-sandbox'),
    cancelRunner);
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-owner-close-sandbox'),
    ownerCloseRunner);
  const isolationRunner = join(bin, 'run-flow-file-write-grant-isolation-sandbox');
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-isolation-sandbox'),
    isolationRunner);
  const sidecarExitRunner = join(bin, 'run-flow-file-write-grant-sidecar-exit-sandbox');
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-sidecar-exit-sandbox'),
    sidecarExitRunner);
  const hostShutdownRunner = join(bin, 'run-flow-file-write-grant-host-shutdown-sandbox');
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-host-shutdown-sandbox'),
    hostShutdownRunner);
  const replayRunner = join(bin, 'run-flow-file-write-grant-replay-sandbox');
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-replay-sandbox'), replayRunner);
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/Tests/verify-file-write-grant-output.mjs'),
    join(support, 'Tests/verify-file-write-grant-output.mjs'));
  writeFileSync(join(work, 'bin/vaadin-desktop'), 'exit 0\n');
  writeFileSync(join(bin, 'build-host'), 'exit 0\n');
  writeFileSync(join(bin, 'check-file-write-grant-replay-handoff'), 'exit 0\n');
  writeFileSync(join(app, 'Contents/Info.plist'), 'test app, not executable');
  writeFileSync(join(jdk, 'bin/java'), 'unchanged JVM launcher', { mode: 0o700 });
  writeFileSync(join(jdk, 'lib/server/libjvm.dylib'), 'unchanged JVM library');
  const sentinel = join(build, 'neighbor-sentinel');
  writeFileSync(sentinel, 'preserve unrelated files');
  for (const name of ['codesign', 'plutil', 'ps']) {
    writeFileSync(join(stubs, name), '#!/usr/bin/env bash\nexit 0\n', { mode: 0o700 });
  }
  // Inspect provenance from the external launcher process, before any host runs.
  // No real UI or JVM is launched. The production inspector is not invoked.
  writeFileSync(join(stubs, 'open'), `#!/usr/bin/env node
const fs = require('node:fs');
const args = process.argv.slice(2);
const envArg = (name) => args.find((arg) => arg.startsWith(name + '='))?.slice(name.length + 1);
const selected = envArg('VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SELECTED_PATH');
const sibling = envArg('VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SIBLING_PATH');
const recoveredSelected = envArg('VAADIN_DESKTOP_FLOW_IPC_SANDBOX_RECOVERED_SELECTED_PATH') || '';
const recoveredSibling = envArg('VAADIN_DESKTOP_FLOW_IPC_SANDBOX_RECOVERED_SIBLING_PATH') || '';
const app = args.at(-1);
const receipt = {
  selected, sibling, app,
  recoveredSelected, recoveredSibling,
  recoveredSelectedExists: recoveredSelected ? fs.existsSync(recoveredSelected) : false,
  recoveredSiblingContents: recoveredSibling ? fs.readFileSync(recoveredSibling, 'utf8') : '',
  recoveredSiblingLinks: recoveredSibling ? fs.statSync(recoveredSibling).nlink : 0,
  selectedExists: fs.existsSync(selected),
  siblingContents: fs.readFileSync(sibling, 'utf8'),
  siblingLinks: fs.statSync(sibling).nlink,
  competingDriver: process.env.VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE ?? '',
  injectedStop: process.env.VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION ?? '',
  writeDriver: envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_SANDBOX') ?? '',
  cancelDriver: envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_CANCEL_SANDBOX') ?? '',
  ownerCloseDriver: envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_OWNER_CLOSE_SANDBOX') ?? '',
  isolationDriver: envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_ISOLATION_SANDBOX') ?? '',
  sidecarExitDriver: envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_SIDECAR_EXIT_SANDBOX') ?? '',
  hostShutdownDriver: envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_HOST_SHUTDOWN_SANDBOX') ?? '',
  replayDriver: envArg('VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_REPLAY_SANDBOX') ?? '',
  replayHandoff: envArg('VATORI_FILE_WRITE_GRANT_REPLAY_HANDOFF') ?? '',
  inheritedWriteDrivers: Object.keys(process.env).filter((key) => key.startsWith('VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT')),
  capabilityManifest: process.env.VAADIN_DESKTOP_ACCEPTANCE_CAPABILITY_MANIFEST,
};
fs.writeFileSync(process.env.VATORI_SANDBOX_SMOKE_RECEIPT, JSON.stringify(receipt));
if (['pass', 'corrupt', 'corrupt-recovered', 'missing-recovered', 'corrupt-recovered-sibling'].includes(process.env.VATORI_SANDBOX_SMOKE_OPEN_STATUS)) {
  const bytes = Buffer.alloc(65542);
  for (let i = 0; i < 65536; i++) bytes[i] = i % 251;
  bytes.set([0, 255, 1, 254, 42, 10], 65536);
  if (process.env.VATORI_SANDBOX_SMOKE_OPEN_STATUS === 'corrupt') bytes[10] ^= 1;
  fs.writeFileSync(selected, bytes, { flag: 'wx', mode: 0o600 });
  if (recoveredSelected && process.env.VATORI_SANDBOX_SMOKE_OPEN_STATUS !== 'missing-recovered') {
    if (process.env.VATORI_SANDBOX_SMOKE_OPEN_STATUS === 'corrupt-recovered') bytes[11] ^= 1;
    fs.writeFileSync(recoveredSelected, bytes, { flag: 'wx', mode: 0o600 });
  }
  if (recoveredSibling && process.env.VATORI_SANDBOX_SMOKE_OPEN_STATUS === 'corrupt-recovered-sibling') {
    fs.writeFileSync(recoveredSibling, 'changed');
  }
  process.exit(0);
}
if (process.env.VATORI_SANDBOX_SMOKE_OPEN_STATUS === 'hang') setInterval(() => {}, 1000);
else process.exit(Number(process.env.VATORI_SANDBOX_SMOKE_OPEN_STATUS));
`, { mode: 0o700 });
  const inspectReceipt = join(work, 'inspected');
  writeFileSync(join(work, 'bin/flow-ipc-trace-inspect'),
    '#!/usr/bin/env bash\nprintf "%s\\n" "$@" > "$VATORI_SANDBOX_SMOKE_INSPECTED"\nexit "$VATORI_SANDBOX_SMOKE_INSPECT_STATUS"\n');
  const launchReceipt = join(work, 'launched.json');
  for (const mode of [
    { name: 'sandbox-file-write-grant', entry: runner, driver: 'writeDriver' },
    { name: 'sandbox-file-write-grant-cancel', entry: cancelRunner, driver: 'cancelDriver' },
    { name: 'sandbox-file-write-grant-owner-close', entry: ownerCloseRunner, driver: 'ownerCloseDriver' },
    { name: 'sandbox-file-write-grant-isolation', entry: isolationRunner, driver: 'isolationDriver' },
    { name: 'sandbox-file-write-grant-sidecar-exit', entry: sidecarExitRunner, driver: 'sidecarExitDriver' },
    { name: 'sandbox-file-write-grant-host-shutdown', entry: hostShutdownRunner, driver: 'hostShutdownDriver' },
    { name: 'sandbox-file-write-grant-replay', entry: replayRunner, driver: 'replayDriver' },
  ]) {
    const { name } = mode;
    const hostLog = join(build, `host-${name}.out.log`);
    const hostErr = join(build, `host-${name}.err.log`);
    const recovered = ['sidecarExitDriver', 'replayDriver'].includes(mode.driver);
    const extraChecks = recovered
      ? ['corrupt-recovered', 'missing-recovered', 'corrupt-recovered-sibling'] : [];
    for (const openStatus of [1, 0, 'pass', 'corrupt', ...extraChecks, 'hang']) {
      rmSync(inspectReceipt, { force: true });
      writeFileSync(hostLog, 'OLD_PASS_TRACE');
      writeFileSync(hostErr, 'OLD_ERRORS');
      const result = spawnSync('bash', [mode.entry], {
        env: {
          ...process.env,
          PATH: `${stubs}:${process.env.PATH}`,
          VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD: '1',
          VAADIN_DESKTOP_ACCEPTANCE_JAVA_HOME: jdk,
          VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS: openStatus === 'hang' ? '1' : '10',
          VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE: '1',
          VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_SANDBOX: '1',
          VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_CANCEL_SANDBOX: '1',
          VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_OWNER_CLOSE_SANDBOX: '1',
          VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_ISOLATION_SANDBOX: '1',
          VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_SIDECAR_EXIT_SANDBOX: '1',
          VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_HOST_SHUTDOWN_SANDBOX: '1',
          VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_REPLAY_SANDBOX: '1',
          VATORI_FILE_WRITE_GRANT_REPLAY_HANDOFF: '/not-this-run/stale-handoff',
          VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION: '1',
          VATORI_SANDBOX_SMOKE_RECEIPT: launchReceipt,
          VATORI_SANDBOX_SMOKE_INSPECTED: inspectReceipt,
          VATORI_SANDBOX_SMOKE_OPEN_STATUS: String(openStatus),
          VATORI_SANDBOX_SMOKE_INSPECT_STATUS: ['pass', 'corrupt', ...extraChecks].includes(openStatus) ? '0' : '7',
        },
        encoding: 'utf8', timeout: openStatus === 'hang' ? 45_000 : 15_000,
      });
      if (result.error) {
        throw new Error(`Runner smoke failed: mode=${name}, openStatus=${openStatus}, `
          + `status=${result.status}, signal=${result.signal}\n${result.stdout}\n${result.stderr}`,
        { cause: result.error });
      }
      assert.equal(result.signal, null);
      assert.equal(result.status, openStatus === 'hang' ? 124
        : openStatus === 'pass' ? 0 : openStatus === 0 ? 7 : 1,
        `${result.stdout}\n${result.stderr}`);
      assert.equal(existsSync(inspectReceipt), [0, 'pass', 'corrupt', ...extraChecks].includes(openStatus));
      if (existsSync(inspectReceipt)) {
        assert.equal(readFileSync(inspectReceipt, 'utf8'),
          `--require-flow-ipc-${name}\n${hostLog}\n`);
      }
      const receipt = JSON.parse(readFileSync(launchReceipt, 'utf8'));
      assert.equal(receipt.selectedExists, false);
      assert.equal(receipt.siblingContents, 'unselected-sandbox-sibling-must-stay-unchanged');
      assert.equal(receipt.siblingLinks, 1);
      assert.equal(receipt.competingDriver, '');
      assert.equal(receipt.injectedStop, '');
      for (const driver of ['writeDriver', 'cancelDriver', 'ownerCloseDriver', 'isolationDriver', 'sidecarExitDriver', 'hostShutdownDriver', 'replayDriver']) {
        assert.equal(receipt[driver], mode.driver === driver ? '1' : '');
      }
      assert.deepEqual(receipt.inheritedWriteDrivers, []);
      assert.equal(receipt.capabilityManifest, join(support, `Fixtures/${name}-capabilities.json`));
      assert.equal(dirname(receipt.selected), dirname(receipt.sibling));
      if (recovered) {
        assert.equal(receipt.recoveredSelectedExists, false);
        assert.equal(receipt.recoveredSiblingContents, 'unselected-sandbox-sibling-must-stay-unchanged');
        assert.equal(receipt.recoveredSiblingLinks, 1);
        assert.notEqual(dirname(receipt.selected), dirname(receipt.recoveredSelected));
        assert.equal(dirname(receipt.recoveredSelected), dirname(receipt.recoveredSibling));
        assert.equal(dirname(dirname(receipt.selected)), dirname(dirname(receipt.recoveredSelected)));
        assert.equal(existsSync(dirname(receipt.recoveredSelected)), false, 'recovered fixture pair must be removed');
      } else {
        assert.equal(receipt.recoveredSelected, '');
        assert.equal(receipt.recoveredSibling, '');
      }
      if (mode.driver === 'replayDriver') {
        const containerTmp = join(homedir(), 'Library/Containers/dev.vatori.desktop.host.macos.sandbox-probe/Data/tmp');
        assert.equal(dirname(dirname(receipt.replayHandoff)), containerTmp);
        assert.match(receipt.replayHandoff.slice(containerTmp.length), /^\/vatori-file-write-grant-replay\.[a-zA-Z0-9]{6}\/old-grant-token\.bin$/);
        assert.equal(existsSync(dirname(receipt.replayHandoff)), false, 'runner must not create a container leaf');
        assert.ok(!receipt.replayHandoff.startsWith(dirname(receipt.selected)));
        assert.ok(!receipt.replayHandoff.startsWith(dirname(receipt.app)));
      } else assert.equal(receipt.replayHandoff, '', 'non-replay modes clear any inherited handoff');
      assert.ok(receipt.selected.startsWith(`${build}/sandbox-write-fixtures.`));
      assert.ok(!receipt.selected.startsWith(dirname(receipt.app)));
      assert.equal(existsSync(dirname(receipt.app)), false, 'temporary app/JVM clone must be removed');
      assert.equal(existsSync(dirname(receipt.selected)), false, 'external fixture pair must be removed');
      assert.equal(readFileSync(hostLog, 'utf8'), '');
      assert.equal(readFileSync(hostErr, 'utf8'), '');
      assert.equal(readFileSync(sentinel, 'utf8'), 'preserve unrelated files');
      assert.equal(readFileSync(join(jdk, 'bin/java'), 'utf8'), 'unchanged JVM launcher');
      assert.equal(readFileSync(join(jdk, 'lib/server/libjvm.dylib'), 'utf8'), 'unchanged JVM library');
      assert.equal(readdirSync(build).filter((name) => name.startsWith('sandbox-write-fixtures.')).length, 0);
    }
  }
  for (const [entry, args] of [[runner, ['--unknown']], [runner, ['--cancel', 'extra']],
    [runner, ['--cancel', '--owner-close']], [cancelRunner, ['extra']], [runner, ['--owner-close', 'extra']],
    [runner, ['--isolation', 'extra']], [runner, ['--owner-close', '--isolation']],
    [runner, ['--sidecar-exit', 'extra']], [runner, ['--isolation', '--sidecar-exit']],
    [runner, ['--host-shutdown', 'extra']], [runner, ['--sidecar-exit', '--host-shutdown']],
    [runner, ['--replay', 'extra']], [runner, ['--replay', '--sidecar-exit']],
    [ownerCloseRunner, ['extra']], [isolationRunner, ['extra']], [sidecarExitRunner, ['extra']],
    [hostShutdownRunner, ['extra']], [replayRunner, ['extra']]]) {
    const result = spawnSync('bash', [entry, ...args], { encoding: 'utf8' });
    assert.equal(result.status, 2);
    assert.match(result.stderr, /Usage:/);
  }
  console.log('Sandbox write-grant seven-mode runner provenance, routing, freshness and bounded cleanup smoke passed');
} finally {
  rmSync(work, { recursive: true, force: true });
}
