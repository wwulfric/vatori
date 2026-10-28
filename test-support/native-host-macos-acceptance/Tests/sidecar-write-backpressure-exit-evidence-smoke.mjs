import assert from 'node:assert/strict';
import { chmodSync, linkSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { once } from 'node:events';
import { fileURLToPath } from 'node:url';
import { verifyExitEvidence } from './sidecar-write-backpressure-exit-evidence.mjs';

// Only test-owned harmless Node children are probed. No GUI/JVM or SIGSTOP.
const work = realpathSync(mkdtempSync(join(tmpdir(), 'vatori-backpressure-evidence-smoke-')));
const verifier = fileURLToPath(new URL('./sidecar-write-backpressure-exit-evidence.mjs', import.meta.url));
const prefix = 'flowSidecarWriteBackpressure.', runId = '18ba55f3-7015-40b3-9a2a-fbbcb1191b52';
let serial = 0, cases = 0;
const alive = spawn(process.execPath, ['-e', 'setInterval(()=>{},1000)'], { stdio: 'ignore' });
const aliveExit = once(alive, 'exit');
const absentPID = async () => {
  const child = spawn(process.execPath, ['-e', ''], { stdio: 'ignore' }), pid = child.pid;
  await once(child, 'exit'); assert.throws(() => process.kill(pid, 0), error => error.code === 'ESRCH'); return pid;
};
const deadJava = await absentPID(), deadHost = await absentPID();
function fixture() {
  const dir = join(work, `vatori-host-shutdown.${String(++serial).padStart(6, '0')}`);
  mkdirSync(dir, { mode: 0o700 });
  const marker = { schemaVersion: 1, runId, processIdentifier: deadJava, mode: 'cooperative' };
  writeFileSync(join(dir, 'startup.json'), JSON.stringify(marker), { mode: 0o600 });
  const common = { windowId: 'main', sessionId: 'flow-ipc-session', uiId: 'main', runId,
    processIdentifier: deadJava, hostProcessIdentifier: deadHost, recoveryGeneration: 0 };
  const e = (type, payload) => ({ at: Date.now(), direction: 'recv', type, payload });
  const c = (type, payload = {}) => e(prefix + type, { ...common, ...payload });
  const events = [e('sidecar.started', { generation: 1, processIdentifier: deadJava }), c('prepared'),
    c('sidecarSuspended', { signal: 'SIGSTOP', processState: 4, stopped: true }),
    c('burstRequested', { requestCount: 16, headerBytes: 16384, browserProgrammatic: true }),
    e('sidecar.writeBackpressure', { generation: 1, processIdentifier: deadJava, pendingBytes: 200000, pendingFrames: 24, partialFrameBytes: 8192 }),
    c('browserDispatchCompleted', { requestCount: 16 }),
    c('mainLoopHeartbeat', { mainThread: true, sidecarStopped: true, windowRetained: true, webViewRetained: true, browserDispatchCompleted: true }),
    c('quitDispatched', { source: 'product-application-menu', action: 'terminate:', keyEquivalent: 'q', commandModifier: true,
      menuMatched: true, activeApplication: true, keyWindow: true }),
    e('sidecar.exit', { generation: 1, processIdentifier: deadJava, expected: true, status: 9, reason: 2 }),
    e('shutdown.sidecar', { mode: 'killed' }), e('host.sidecarShutdownCompleted', { succeeded: true }),
    c('applicationWillTerminate', { complete: true, preparedObserved: true, sidecarSuspendedObserved: true, burstDispatched: true,
      heartbeatObserved: true, quitDispatched: true, terminationSource: 'applicationWillTerminate', currentProcessIdentifier: 0,
      originalSidecarAbsent: true, originalWindowRetained: true, originalWebViewRetained: true, startupMarkerMatched: true })];
  const trace = join(work, `trace-${serial}.log`);
  const save = () => writeFileSync(trace, events.map(event => `HOST_PROTOCOL ${JSON.stringify(event)}\n`).join(''), { mode: 0o600 });
  save(); return { dir, trace, marker, common, events, save };
}
function reject(label, mutate, pattern = /./) {
  const f = fixture(); mutate(f); const before = readFileSync(f.trace, 'utf8');
  assert.throws(() => verifyExitEvidence(f.dir, f.trace, true), pattern, label);
  assert.equal(readFileSync(f.trace, 'utf8'), before, `${label}: no receipt on failure`); cases++;
}
try {
  const f = fixture(), receipt = verifyExitEvidence(f.dir, f.trace);
  assert.equal(receipt.type, prefix + 'externalExitObserved');
  assert.deepEqual(receipt.payload, { ...f.common, schemaVersion: 1, hostAbsent: true, sidecarAbsent: true, markersMatched: true, cleanupPerformed: false }); cases++;
  const retry = fixture(); retry.events.splice(5, 0, { ...retry.events[4] }); retry.save(); verifyExitEvidence(retry.dir, retry.trace); cases++;
  const earlier = fixture(); earlier.events.splice(1, 0, { ...earlier.events[4] }); earlier.save();
  verifyExitEvidence(earlier.dir, earlier.trace); cases++;
  reject('preparation backpressure alone', f => { const [event] = f.events.splice(4, 1); f.events.splice(1, 0, event); f.save(); });
  reject('post-stop but before burst alone', f => { [f.events[3], f.events[4]] = [f.events[4], f.events[3]]; f.save(); });
  reject('backpressure after Quit despite valid earlier evidence', f => { f.events.splice(8, 0, { ...f.events[4] }); f.save(); });
  reject('fully written maximum frame is not partial backpressure', f => {
    f.events[4].payload.pendingBytes = 16 * 1024 * 1024;
    f.events[4].payload.partialFrameBytes = 16 * 1024 * 1024; f.save();
  });
  reject('missing marker', f => rmSync(join(f.dir, 'startup.json')));
  for (const name of ['hook-entered.json', 'launcher-returned.json', 'extra']) reject(`unexpected ${name}`, f => writeFileSync(join(f.dir, name), '{}'));
  for (const field of ['schemaVersion', 'runId', 'processIdentifier', 'mode']) reject(`marker ${field}`, f => writeFileSync(join(f.dir, 'startup.json'), JSON.stringify({ ...f.marker, [field]: 'wrong' })));
  reject('extra marker field', f => writeFileSync(join(f.dir, 'startup.json'), JSON.stringify({ ...f.marker, extra: true })));
  reject('marker malformed', f => writeFileSync(join(f.dir, 'startup.json'), '{'));
  reject('marker oversized', f => writeFileSync(join(f.dir, 'startup.json'), 'x'.repeat(2049)));
  reject('marker permissions', f => chmodSync(join(f.dir, 'startup.json'), 0o644));
  reject('marker symlink', f => { const original = join(f.dir, 'startup.json'), target = join(work, `marker-${serial}`); writeFileSync(target, JSON.stringify(f.marker)); rmSync(original); symlinkSync(target, original); });
  reject('marker hardlink', f => linkSync(join(f.dir, 'startup.json'), join(work, `marker-${serial}`)));
  reject('directory mode', f => chmodSync(f.dir, 0o755));
  reject('directory symlink', f => { const link = `${f.dir}-link`; symlinkSync(f.dir, link); f.dir = link; });
  reject('empty trace', f => writeFileSync(f.trace, ''));
  reject('trace malformed', f => writeFileSync(f.trace, 'HOST_PROTOCOL {\n'));
  reject('trace hardlink', f => linkSync(f.trace, `${f.trace}-link`));
  reject('trace symlink', f => { const link = `${f.trace}-link`; symlinkSync(f.trace, link); f.trace = link; });
  for (let i = 0; i < 12; i++) {
    reject(`missing ${i}`, f => { f.events.splice(i, 1); f.save(); });
    if (i !== 4) reject(`duplicate ${i}`, f => { f.events.push(f.events[i]); f.save(); });
    reject(`direction ${i}`, f => { f.events[i].direction = 'send'; f.save(); });
    reject(`timestamp ${i}`, f => { f.events[i].at = '1'; f.save(); });
  }
  for (const i of [1, 2, 3, 6, 7, 8, 9, 10, 11]) reject(`out of order ${i}`, f => { [f.events[i - 1], f.events[i]] = [f.events[i], f.events[i - 1]]; f.save(); });
  for (const [index, key, value] of [[2, 'processState', 2], [2, 'signal', 'SIGCONT'], [2, 'stopped', false],
    [3, 'requestCount', 17], [3, 'headerBytes', 1], [5, 'requestCount', 15], [6, 'mainThread', false],
    [6, 'sidecarStopped', false], [6, 'browserDispatchCompleted', false], [7, 'menuMatched', false], [7, 'action', 'close:'],
    [7, 'activeApplication', false], [7, 'keyWindow', false], [8, 'expected', false], [8, 'status', 15], [8, 'reason', 1],
    [8, 'generation', 2], [9, 'mode', 'graceful'], [10, 'succeeded', false], [11, 'complete', false],
    [11, 'currentProcessIdentifier', deadJava], [11, 'originalWindowRetained', false], [11, 'startupMarkerMatched', false],
    [4, 'generation', 2], [4, 'pendingBytes', 0], [4, 'pendingFrames', 0], [4, 'partialFrameBytes', -1],
    [4, 'pendingBytes', 16 * 1024 * 1024 + 1], [4, 'pendingFrames', 257], [4, 'partialFrameBytes', 200001],
    [4, 'unknown', true]]) {
    reject(`payload ${index}/${key}`, f => { f.events[index].payload[key] = value; f.save(); });
  }
  reject('backpressure after heartbeat', f => { const [event] = f.events.splice(4, 1); f.events.splice(6, 0, event); f.save(); });
  reject('wrong scope', f => { f.events[6].payload.windowId = 'secondary'; f.save(); });
  reject('prior external receipt', f => { f.events.push({ ...f.events[11], type: prefix + 'externalExitObserved' }); f.save(); });
  reject('live host', f => { for (const e of f.events) if ('hostProcessIdentifier' in e.payload) e.payload.hostProcessIdentifier = alive.pid; f.save(); }, /still alive/);
  reject('live JVM', f => {
    writeFileSync(join(f.dir, 'startup.json'), JSON.stringify({ ...f.marker, processIdentifier: alive.pid }));
    for (const e of f.events) if ('processIdentifier' in e.payload) e.payload.processIdentifier = alive.pid; f.save();
  }, /still alive/);
  for (const code of ['EPERM', 'EINVAL']) {
    const f = fixture(), original = process.kill;
    try { process.kill = () => { const error = new Error('test-only probe failure'); error.code = code; throw error; };
      assert.throws(() => verifyExitEvidence(f.dir, f.trace), new RegExp(code)); cases++;
    } finally { process.kill = original; }
  }
  const cli = fixture();
  for (const status of [0, 1]) {
    const r = spawnSync(process.execPath, [verifier, cli.dir, cli.trace], { encoding: 'utf8', timeout: 5000 });
    assert.ifError(r.error); assert.equal(r.status, status, r.stderr);
    assert.equal(readFileSync(cli.trace, 'utf8').split(prefix + 'externalExitObserved').length, 2); cases++;
  }
  console.log(`Sidecar write-backpressure evidence smoke passed: ${cases} process/evidence cases`);
} finally { alive.kill('SIGKILL'); await aliveExit; rmSync(work, { recursive: true, force: true }); }
