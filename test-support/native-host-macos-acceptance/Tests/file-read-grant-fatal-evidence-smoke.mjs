import assert from 'node:assert/strict';
import { chmodSync, existsSync, linkSync, mkdirSync, mkdtempSync, readFileSync, realpathSync,
  rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { armFatalTrigger, requireAbsent, verifyFatalExit } from './file-read-grant-fatal-evidence.mjs';

const work = realpathSync(mkdtempSync(join(tmpdir(), 'vatori-fatal-evidence-smoke-')));
const exitedPID = () => {
  const child = spawnSync(process.execPath, ['-e', ''], { timeout: 5000 });
  assert.ifError(child.error); assert.equal(child.status, 0);
  requireAbsent(child.pid); return child.pid;
};
const java = exitedPID(), host = exitedPID();
let serial = 0, cases = 0;
function fixture(arming = false) {
  const dir = join(work, `vatori-fatal-protocol.${String(++serial).padStart(6, '0')}`);
  mkdirSync(dir, { mode: 0o700 });
  const pid = arming ? process.pid : java;
  writeFileSync(join(dir, 'startup.pid'), `${pid}\n`, { mode: 0o600 });
  if (!arming) writeFileSync(join(dir, 'trigger'), '', { mode: 0o600 });
  const event = (type, payload, direction = 'recv') => ({ at: Date.now(), type, payload, direction });
  const prepared = { initialProcessIdentifier: pid, recoveryGeneration: 0, firstSelectionOutcome: 'SELECTED' };
  for (const field of ['complete', 'standardBundleMode', 'flowClientOwned', 'externalPanelSelection',
    'ownerWindowsOpen', 'operationsPending', 'windowRetained', 'webViewRetained', 'secondaryWindowRetained',
    'secondaryWebViewRetained', 'balancedDrainObserved', 'errorFree']) prepared[field] = true;
  const events = [event('sidecar.started', { generation: 1, processIdentifier: pid }),
    event('fileReadGrant.created', { diagnosticId: 1, sidecarGeneration: 1,
      windowId: 'file-read-grant-secondary', securityScoped: true }),
    event('flowFileReadGrant.hostShutdownPrepared', prepared)];
  if (!arming) events.push(
    event('sidecar.protocolRejected', { version: 2, cause: 'unsupported-sidecar-version' }, 'send'),
    event('error', { code: 'unsupportedSidecarVersion', cause: 'unsupported-sidecar-version' }, 'send'),
    event('host.failureShutdownStarted', { processIdentifier: pid, hostProcessIdentifier: host }),
    event('fileReadGrant.invalidated', {}), event('fileReadGrant.closed', {}),
    event('host.nativeResourceDrainCompleted', {}), event('shutdown', {}, 'send'),
    event('sidecar.exit', { generation: 1, processIdentifier: pid, expected: true, status: 0, reason: 1 }),
    event('shutdown.sidecar', {}), event('host.sidecarShutdownCompleted', { succeeded: true }),
    event('host.failureShutdownCompleted', { exitStatus: 1 }));
  const trace = join(work, `trace-${serial}.log`);
  const f = { dir, trace, events, save() {
    writeFileSync(this.trace, this.events.map(e => `HOST_PROTOCOL ${JSON.stringify(e)}\n`).join(''), { mode: 0o600 });
  } };
  f.save(); return f;
}
function reject(label, mutate, arming = false) {
  const f = fixture(arming); mutate(f);
  const before = readFileSync(f.trace, 'utf8');
  assert.throws(() => arming ? armFatalTrigger(f.dir, f.trace) : verifyFatalExit(f.dir, f.trace, true), /./, label);
  assert.equal(readFileSync(f.trace, 'utf8'), before, `${label}: no success append`);
  if (arming) assert.equal(existsSync(join(f.dir, 'trigger')), false, `${label}: no fault armed`);
  cases++;
}
try {
  const valid = fixture();
  const receipt = verifyFatalExit(valid.dir, valid.trace, true);
  assert.deepEqual(receipt.payload, { schemaVersion: 1, hostProcessIdentifier: host, processIdentifier: java,
    hostAbsent: true, sidecarAbsent: true, startupMatched: true, cleanupPerformed: false }); cases++;
  assert.throws(() => verifyFatalExit(valid.dir, valid.trace, true), /already exists/); cases++;
  const ready = fixture(true);
  armFatalTrigger(ready.dir, ready.trace);
  assert.equal(readFileSync(join(ready.dir, 'trigger'), 'utf8'), ''); cases++;
  assert.throws(() => armFatalTrigger(ready.dir, ready.trace), /Unexpected private state/); cases++;
  for (const arming of [false, true]) {
    reject('marker missing', f => rmSync(join(f.dir, 'startup.pid')), arming);
    reject('extra entry', f => writeFileSync(join(f.dir, 'extra'), ''), arming);
    reject('directory permissions', f => chmodSync(f.dir, 0o755), arming);
    reject('marker permissions', f => chmodSync(join(f.dir, 'startup.pid'), 0o644), arming);
    reject('marker hardlink', f => linkSync(join(f.dir, 'startup.pid'), join(work, `link-${serial}`)), arming);
    reject('marker symlink', f => {
      rmSync(join(f.dir, 'startup.pid')); symlinkSync(f.trace, join(f.dir, 'startup.pid'));
    }, arming);
    reject('trace permissions', f => chmodSync(f.trace, 0o644), arming);
    reject('trace hardlink', f => linkSync(f.trace, `${f.trace}-link`), arming);
    reject('trace symlink', f => { symlinkSync(f.trace, `${f.trace}-link`); f.trace += '-link'; }, arming);
    reject('wrong marker', f => writeFileSync(join(f.dir, 'startup.pid'), '1\n'), arming);
    reject('oversized marker', f => writeFileSync(join(f.dir, 'startup.pid'), '1'.repeat(33)), arming);
    reject('malformed trace', f => writeFileSync(f.trace, 'HOST_PROTOCOL {\n'), arming);
    reject('recovery', f => { f.events.unshift({ ...f.events[0], type: 'sidecar.recoveryStarted' }); f.save(); }, arming);
    for (const field of Object.keys(fixture(arming).events[2].payload)) {
      reject(`preparation ${field}`, f => { delete f.events[2].payload[field]; f.save(); }, arming);
    }
    for (let i = 0; i < (arming ? 3 : 14); i++) {
      reject(`missing ${i}`, f => { f.events.splice(i, 1); f.save(); }, arming);
      reject(`duplicate ${i}`, f => { f.events.push(f.events[i]); f.save(); }, arming);
      reject(`direction ${i}`, f => { f.events[i].direction = f.events[i].direction === 'recv' ? 'send' : 'recv'; f.save(); }, arming);
      if (i > 0) reject(`order ${i}`, f => {
        [f.events[i - 1], f.events[i]] = [f.events[i], f.events[i - 1]]; f.save();
      }, arming);
    }
  }
  for (const type of ['error', 'shutdown', 'sidecar.exit', 'fileReadGrant.invalidated', 'fileReadGrant.closed', 'fileReadGrant.read']) {
    reject(`cannot arm after ${type}`, f => { f.events.push({ ...f.events[0], type }); f.save(); }, true);
  }
  reject('live host', f => { f.events[5].payload.hostProcessIdentifier = process.pid; f.save(); });
  reject('live JVM', f => {
    writeFileSync(join(f.dir, 'startup.pid'), `${process.pid}\n`);
    for (const i of [0, 5, 10]) f.events[i].payload.processIdentifier = process.pid;
    f.events[2].payload.initialProcessIdentifier = process.pid; f.save();
  });
  reject('wrong fatal status', f => { f.events[13].payload.exitStatus = 0; f.save(); });
  reject('wrong exit status', f => { f.events[10].payload.status = 9; f.save(); });
  reject('nonempty trigger', f => writeFileSync(join(f.dir, 'trigger'), 'x'));
  reject('partial final trace', f => writeFileSync(f.trace, readFileSync(f.trace, 'utf8').trimEnd()));
  for (const code of ['EPERM', 'EINVAL']) {
    assert.throws(() => requireAbsent(java, () => { throw Object.assign(new Error('probe'), { code }); }), new RegExp(code)); cases++;
  }
  const executable = fileURLToPath(new URL('./file-read-grant-fatal-evidence.mjs', import.meta.url));
  for (const mode of ['watch', 'verify']) {
    const f = fixture(mode === 'watch');
    for (const expected of [0, 1]) {
      const result = spawnSync(process.execPath, [executable, mode, f.dir, f.trace], { encoding: 'utf8', timeout: 5000 });
      assert.ifError(result.error); assert.equal(result.status, expected, result.stderr); cases++;
    }
    if (mode === 'watch') assert.equal(readFileSync(join(f.dir, 'trigger'), 'utf8'), '');
    else assert.equal(readFileSync(f.trace, 'utf8').split('flowFileReadGrant.fatalExitObserved').length, 2);
  }
  console.log(`Fatal file-grant evidence smoke passed: ${cases} cases (synthetic trace, owned exited PIDs)`);
} finally { rmSync(work, { recursive: true, force: true }); }
