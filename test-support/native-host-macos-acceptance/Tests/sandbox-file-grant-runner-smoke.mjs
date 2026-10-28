import assert from 'node:assert/strict';
import { copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
if (process.platform !== 'darwin') {
  console.log('Sandbox file-grant runner smoke skipped: macOS filesystem tools required');
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
  for (const path of [bin, join(support, 'Fixtures'), join(work, 'bin'),
    join(app, 'Contents'), join(jdk, 'bin'), join(jdk, 'lib/server'), stubs]) {
    mkdirSync(path, { recursive: true });
  }
  const runner = join(bin, 'run-flow-file-read-grant-sandbox');
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-sandbox'), runner);
  writeFileSync(join(work, 'bin/vaadin-desktop'), 'exit 0\n');
  writeFileSync(join(bin, 'build-host'), 'exit 0\n');
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
const app = args.at(-1);
const receipt = {
  selected, sibling, app,
  selectedContents: fs.readFileSync(selected, 'utf8'),
  siblingContents: fs.readFileSync(sibling, 'utf8'),
  selectedLinks: fs.statSync(selected).nlink,
  siblingLinks: fs.statSync(sibling).nlink,
  competingDriver: process.env.VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE ?? '',
  injectedStop: process.env.VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION ?? '',
};
fs.writeFileSync(process.env.VATORI_SANDBOX_SMOKE_RECEIPT, JSON.stringify(receipt));
if (process.env.VATORI_SANDBOX_SMOKE_OPEN_STATUS === 'hang') setInterval(() => {}, 1000);
else process.exit(Number(process.env.VATORI_SANDBOX_SMOKE_OPEN_STATUS));
`, { mode: 0o700 });
  const inspectReceipt = join(work, 'inspected');
  writeFileSync(join(work, 'bin/flow-ipc-trace-inspect'),
    '#!/usr/bin/env bash\nprintf inspected > "$VATORI_SANDBOX_SMOKE_INSPECTED"\nexit 7\n');
  const hostLog = join(build, 'host-sandbox-file-read-grant.out.log');
  const hostErr = join(build, 'host-sandbox-file-read-grant.err.log');
  const launchReceipt = join(work, 'launched.json');
  for (const openStatus of [1, 0, 'hang']) {
    rmSync(inspectReceipt, { force: true });
    writeFileSync(hostLog, 'OLD_PASS_TRACE');
    writeFileSync(hostErr, 'OLD_ERRORS');
    const result = spawnSync('bash', [runner], {
      env: {
        ...process.env,
        PATH: `${stubs}:${process.env.PATH}`,
        VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD: '1',
        VAADIN_DESKTOP_ACCEPTANCE_JAVA_HOME: jdk,
        VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS: openStatus === 'hang' ? '1' : '10',
        VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE: '1',
        VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION: '1',
        VATORI_SANDBOX_SMOKE_RECEIPT: launchReceipt,
        VATORI_SANDBOX_SMOKE_INSPECTED: inspectReceipt,
        VATORI_SANDBOX_SMOKE_OPEN_STATUS: String(openStatus),
      },
      encoding: 'utf8', timeout: openStatus === 'hang' ? 45_000 : 15_000,
    });
    assert.ifError(result.error);
    assert.equal(result.signal, null);
    assert.equal(result.status, openStatus === 'hang' ? 124 : openStatus === 1 ? 1 : 7,
      `${result.stdout}\n${result.stderr}`);
    assert.equal(existsSync(inspectReceipt), openStatus === 0);
    const receipt = JSON.parse(readFileSync(launchReceipt, 'utf8'));
    assert.equal(receipt.selectedContents, 'vatori-file-read-grant-fixture::0123456789::tail');
    assert.notEqual(receipt.selectedContents, receipt.siblingContents);
    assert.equal(receipt.selectedLinks, 1);
    assert.equal(receipt.siblingLinks, 1);
    assert.equal(receipt.competingDriver, '');
    assert.equal(receipt.injectedStop, '');
    assert.equal(dirname(receipt.selected), dirname(receipt.sibling));
    assert.ok(receipt.selected.startsWith(`${build}/sandbox-file-fixtures.`));
    assert.ok(!receipt.selected.startsWith(dirname(receipt.app)));
    assert.equal(existsSync(dirname(receipt.app)), false, 'temporary app/JVM clone must be removed');
    assert.equal(existsSync(dirname(receipt.selected)), false, 'external fixture pair must be removed');
    assert.equal(readFileSync(hostLog, 'utf8'), '');
    assert.equal(readFileSync(hostErr, 'utf8'), '');
    assert.equal(readFileSync(sentinel, 'utf8'), 'preserve unrelated files');
    assert.equal(readFileSync(join(jdk, 'bin/java'), 'utf8'), 'unchanged JVM launcher');
    assert.equal(readFileSync(join(jdk, 'lib/server/libjvm.dylib'), 'utf8'), 'unchanged JVM library');
    assert.equal(readdirSync(build).filter((name) => name.startsWith('sandbox-file-fixtures.')).length, 0);
  }
  console.log('Sandbox file-grant runner provenance, freshness and cleanup smoke passed');
} finally {
  rmSync(work, { recursive: true, force: true });
}
