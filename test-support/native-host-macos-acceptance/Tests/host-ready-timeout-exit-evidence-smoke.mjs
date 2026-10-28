import assert from 'node:assert/strict';
import { chmodSync, linkSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { once } from 'node:events';
import { fileURLToPath } from 'node:url';
import { verifyExitEvidence } from './host-ready-timeout-exit-evidence.mjs';

// Probes only children owned by this test. No GUI/JVM, host scan or OS permission changes.
const work = realpathSync(mkdtempSync(join(tmpdir(), 'vatori-ready-evidence-smoke-')));
const verifier = fileURLToPath(new URL('./host-ready-timeout-exit-evidence.mjs', import.meta.url));
const runId = '18ba55f3-7015-40b3-9a2a-fbbcb1191b52';
const names = ['startup.json', 'hook-entered.json'];
let cases = 0, serial = 0;
const alive = spawn(process.execPath, ['-e', 'setInterval(()=>{},1000)'], { stdio: 'ignore' });
const absentPID = async () => {
  const child = spawn(process.execPath, ['-e', ''], { stdio: 'ignore' });
  const pid = child.pid; await once(child, 'exit');
  assert.throws(() => process.kill(pid, 0), error => error.code === 'ESRCH');
  return pid;
};
const deadJava = await absentPID(), deadHost = await absentPID();
function fixture(mode = 'blocked') {
  const dir = join(work, `vatori-ready-timeout.${String(++serial).padStart(6, '0')}`);
  mkdirSync(dir, { mode: 0o700 });
  const marker = { schemaVersion: 1, runId, processIdentifier: deadJava, mode };
  for (const name of names) writeFileSync(join(dir, name), JSON.stringify(marker), { mode: 0o600 });
  const event = (type, payload) => ({ at: Date.now(), direction: 'recv', type, payload });
  const events = [event('sidecar.started', { generation: 1, processIdentifier: deadJava, transport: 'flow-ipc' }),
    event('host.failureShutdownStarted', { processIdentifier: deadJava, hostProcessIdentifier: deadHost }),
    event('sidecar.exit', { generation: 1, processIdentifier: deadJava, expected: true,
      status: mode === 'blocked' ? 9 : 0, reason: mode === 'blocked' ? 2 : 1 }),
    event('shutdown.sidecar', { mode: mode === 'blocked' ? 'killed' : 'graceful' }),
    event('host.sidecarShutdownCompleted', { succeeded: true }),
    event('host.failureShutdownCompleted', { exitStatus: 1 })];
  const trace = join(work, `trace-${serial}.log`);
  const save = () => writeFileSync(trace, events.map(e => `HOST_PROTOCOL ${JSON.stringify(e)}\n`).join(''), { mode: 0o600 });
  save(); return { dir, trace, mode, events, marker, save, hostPID: deadHost };
}
function reject(label, mutate, pattern = /./) {
  const f = fixture(); mutate(f);
  const before = readFileSync(f.trace, 'utf8');
  assert.throws(() => verifyExitEvidence(f.dir, f.trace, f.mode, f.hostPID, true), pattern, label);
  assert.equal(readFileSync(f.trace, 'utf8'), before, `${label}: failure never appends success`); cases++;
}
try {
  for (const mode of ['blocked', 'cooperative']) {
    const f = fixture(mode), receipt = verifyExitEvidence(f.dir, f.trace, mode, f.hostPID);
    assert.equal(receipt.type, 'hostReadyTimeout.externalExitObserved');
    assert.deepEqual(receipt.payload, { schemaVersion: 1, runId, mode, processIdentifier: deadJava,
      hostProcessIdentifier: deadHost, hostAbsent: true, sidecarAbsent: true, markersMatched: true, cleanupPerformed: false });
    cases++;
  }
  reject('missing marker', f => rmSync(join(f.dir, names[1])));
  reject('extra marker', f => writeFileSync(join(f.dir, 'extra.json'), '{}'));
  for (const field of ['schemaVersion', 'runId', 'processIdentifier', 'mode']) {
    reject(`marker ${field}`, f => writeFileSync(join(f.dir, names[1]), JSON.stringify({ ...f.marker, [field]: 'wrong' })));
  }
  reject('unknown marker key', f => writeFileSync(join(f.dir, names[1]), JSON.stringify({ ...f.marker, extra: true })));
  reject('invalid marker JSON', f => writeFileSync(join(f.dir, names[1]), '{'));
  reject('oversized marker', f => writeFileSync(join(f.dir, names[1]), 'x'.repeat(2049)));
  reject('marker permissions', f => chmodSync(join(f.dir, names[1]), 0o644));
  reject('marker symlink', f => { rmSync(join(f.dir, names[1])); symlinkSync(join(f.dir, names[0]), join(f.dir, names[1])); });
  reject('marker hardlink', f => { rmSync(join(f.dir, names[1])); linkSync(join(f.dir, names[0]), join(f.dir, names[1])); });
  reject('directory permissions', f => chmodSync(f.dir, 0o755));
  reject('directory symlink', f => { const link = `${f.dir}-link`; symlinkSync(f.dir, link); f.dir = link; });
  reject('invalid mode', f => { f.mode = 'allowed'; });
  for (const pid of [0, -1, '1', 1.5, true, deadJava]) reject(`host PID ${pid}`, f => { f.hostPID = pid; });
  reject('empty trace', f => writeFileSync(f.trace, ''));
  reject('invalid trace JSON', f => writeFileSync(f.trace, 'HOST_PROTOCOL {\n'));
  reject('trace symlink', f => { const link = `${f.trace}-link`; symlinkSync(f.trace, link); f.trace = link; });
  reject('trace hardlink', f => linkSync(f.trace, `${f.trace}-link`));
  for (let index = 0; index < 6; index++) {
    reject(`missing event ${index}`, f => { f.events.splice(index, 1); f.save(); });
    reject(`duplicate event ${index}`, f => { f.events.push(f.events[index]); f.save(); });
    reject(`wrong direction ${index}`, f => { f.events[index].direction = 'send'; f.save(); });
    reject(`timestamp type ${index}`, f => { f.events[index].at = '1'; f.save(); });
    if (index > 0) reject(`wrong order ${index}`, f => {
      [f.events[index - 1], f.events[index]] = [f.events[index], f.events[index - 1]]; f.save();
    });
  }
  for (const [field, value] of [['expected', false], ['status', 15], ['reason', 1], ['generation', 2], ['processIdentifier', deadHost]]) {
    reject(`exit ${field}`, f => { f.events[2].payload[field] = value; f.save(); });
  }
  reject('wrong original generation', f => { f.events[0].payload.generation = 2; f.save(); });
  reject('wrong original PID', f => { f.events[0].payload.processIdentifier = deadHost; f.save(); });
  reject('wrong transport', f => { f.events[0].payload.transport = 'compatibility-http'; f.save(); });
  reject('wrong fatal sidecar PID', f => { f.events[1].payload.processIdentifier = deadHost; f.save(); });
  reject('wrong fatal host PID', f => { f.events[1].payload.hostProcessIdentifier = deadJava; f.save(); });
  reject('unknown fatal key', f => { f.events[1].payload.extra = true; f.save(); });
  reject('wrong shutdown mode', f => { f.events[3].payload.mode = 'graceful'; f.save(); });
  reject('failed completion', f => { f.events[4].payload.succeeded = false; f.save(); });
  reject('wrong fatal exit', f => { f.events[5].payload.exitStatus = 0; f.save(); });
  reject('extra host event', f => { f.events.push({ ...f.events[0], type: 'unexpected' }); f.save(); });
  reject('old external receipt', f => { f.events.push({ ...f.events[0], type: 'hostReadyTimeout.externalExitObserved' }); f.save(); });
  reject('live host', f => { f.events[1].payload.hostProcessIdentifier = alive.pid; f.hostPID = alive.pid; f.save(); }, /still alive/);
  reject('live JVM', f => {
    for (const name of names) writeFileSync(join(f.dir, name), JSON.stringify({ ...f.marker, processIdentifier: alive.pid }));
    for (const index of [0, 1, 2]) f.events[index].payload.processIdentifier = alive.pid; f.save();
  }, /still alive/);
  for (const code of ['EPERM', 'EINVAL']) {
    const f = fixture(), realKill = process.kill;
    try {
      process.kill = (pid, signal) => { assert.equal(signal, 0); const e = new Error('test-only signal-zero failure'); e.code = code; throw e; };
      assert.throws(() => verifyExitEvidence(f.dir, f.trace, f.mode, f.hostPID), new RegExp(code)); cases++;
    } finally { process.kill = realKill; }
  }
  const cli = fixture();
  for (const expected of [0, 1]) {
    const result = spawnSync(process.execPath, [verifier, cli.dir, cli.trace, cli.mode, String(cli.hostPID)], { encoding: 'utf8', timeout: 5000 });
    assert.ifError(result.error); assert.equal(result.status, expected, result.stderr);
    assert.equal(readFileSync(cli.trace, 'utf8').split('hostReadyTimeout.externalExitObserved').length, 2); cases++;
  }
  console.log(`Host ready timeout exit evidence smoke passed: ${cases} real-owned PID and malformed-evidence cases`);
} finally {
  alive.kill('SIGKILL'); await once(alive, 'exit'); rmSync(work, { recursive: true, force: true });
}
