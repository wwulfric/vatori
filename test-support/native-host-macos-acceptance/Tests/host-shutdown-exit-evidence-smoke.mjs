import assert from 'node:assert/strict';
import { chmodSync, linkSync, mkdirSync, mkdtempSync, readFileSync, realpathSync,
  rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { once } from 'node:events';
import { fileURLToPath } from 'node:url';
import { verifyExitEvidence } from './host-shutdown-exit-evidence.mjs';

// Only harmless children started by this test are probed or terminated. The
// EPERM case replaces signal-zero locally for one verifier invocation, not OS
// permissions or the production runner. No GUI, JVM, build or process scan.
const work = realpathSync(mkdtempSync(join(tmpdir(), 'vatori-shutdown-evidence-smoke-')));
const verifier = fileURLToPath(new URL('./host-shutdown-exit-evidence.mjs', import.meta.url));
const runId = '18ba55f3-7015-40b3-9a2a-fbbcb1191b52';
const names = ['startup.json', 'launcher-returned.json', 'hook-entered.json'];
const prefix = 'flowHostShutdownTermination.';
let cases = 0, serial = 0;
const alive = spawn(process.execPath, ['-e', 'setInterval(()=>{},1000)'], { stdio: 'ignore' });
const absentPID = async () => {
  const child = spawn(process.execPath, ['-e', ''], { stdio: 'ignore' });
  const pid = child.pid;
  await once(child, 'exit');
  assert.throws(() => process.kill(pid, 0), error => error.code === 'ESRCH');
  return pid;
};
const deadJava = await absentPID(), deadHost = await absentPID();

function fixture(mode = 'blocked') {
  const dir = join(work, `vatori-host-shutdown.${String(++serial).padStart(6, '0')}`);
  mkdirSync(dir, { mode: 0o700 });
  const marker = { schemaVersion: 1, runId, processIdentifier: deadJava, mode };
  for (const name of names) writeFileSync(join(dir, name), JSON.stringify(marker), { mode: 0o600 });
  const event = (type, payload) => ({ at: Date.now(), direction: 'recv', type, payload });
  const events = [event('sidecar.started', { generation: 1, processIdentifier: deadJava }),
    event('sidecar.exit', { generation: 1, processIdentifier: deadJava, expected: true,
      status: mode === 'blocked' ? 9 : 0, reason: mode === 'blocked' ? 2 : 1 }),
    event('host.sidecarShutdownCompleted', { succeeded: true }),
    event(prefix + 'applicationWillTerminate', {
      windowId: 'main', sessionId: 'flow-ipc-session', uiId: 'main', runId, mode,
      processIdentifier: deadJava, hostProcessIdentifier: deadHost, recoveryGeneration: 0,
      complete: true, preparedObserved: true, finalPointerDispatched: true,
      terminationSource: 'applicationWillTerminate', currentProcessIdentifier: 0,
      originalSidecarAbsent: true, originalWindowRetained: false,
      launcherReturnedMarkerMatched: true, hookEnteredMarkerMatched: true,
    })];
  const trace = join(work, `trace-${serial}.log`);
  const save = () => writeFileSync(trace, events.map(e => `HOST_PROTOCOL ${JSON.stringify(e)}\n`).join(''), { mode: 0o600 });
  save();
  return { dir, trace, mode, events, marker, save };
}
function reject(label, mutate, pattern = /./) {
  const f = fixture(); mutate(f);
  const before = readFileSync(f.trace, 'utf8');
  assert.throws(() => verifyExitEvidence(f.dir, f.trace, f.mode), pattern, label);
  assert.equal(readFileSync(f.trace, 'utf8'), before, `${label}: failure never appends a success receipt`);
  cases++;
}
try {
  for (const mode of ['blocked', 'cooperative']) {
    const f = fixture(mode);
    const receipt = verifyExitEvidence(f.dir, f.trace, mode);
    assert.equal(receipt.type, prefix + 'externalExitObserved');
    assert.equal(receipt.payload.hostAbsent, true); assert.equal(receipt.payload.sidecarAbsent, true);
    assert.equal(receipt.payload.cleanupPerformed, false);
    assert.equal(receipt.payload.mode, mode); assert.equal(receipt.payload.runId, runId);
    cases++;
  }
  reject('missing marker', f => rmSync(join(f.dir, names[1])));
  reject('extra marker', f => writeFileSync(join(f.dir, 'extra.json'), '{}'));
  for (const field of ['schemaVersion', 'runId', 'processIdentifier', 'mode']) {
    reject(`marker ${field} mismatch`, f => writeFileSync(join(f.dir, names[1]),
      JSON.stringify({ ...f.marker, [field]: field === 'processIdentifier' ? deadHost : 'wrong' })));
  }
  reject('unknown marker key', f => writeFileSync(join(f.dir, names[1]), JSON.stringify({ ...f.marker, extra: true })));
  reject('marker invalid JSON', f => writeFileSync(join(f.dir, names[1]), '{'));
  reject('marker oversized', f => writeFileSync(join(f.dir, names[1]), 'x'.repeat(2049)));
  reject('marker mode', f => chmodSync(join(f.dir, names[1]), 0o644));
  reject('marker symlink', f => { rmSync(join(f.dir, names[1])); symlinkSync(join(f.dir, names[0]), join(f.dir, names[1])); });
  reject('marker hardlink', f => { rmSync(join(f.dir, names[1])); linkSync(join(f.dir, names[0]), join(f.dir, names[1])); });
  reject('directory permissions', f => chmodSync(f.dir, 0o755));
  reject('directory symlink', f => { const link = `${f.dir}-link`; symlinkSync(f.dir, link); f.dir = link; });
  reject('invalid mode', f => { f.mode = 'allowed'; });
  reject('empty trace', f => writeFileSync(f.trace, ''));
  reject('malformed trace', f => writeFileSync(f.trace, 'HOST_PROTOCOL {\n'));
  reject('trace symlink', f => { const link = `${f.trace}-link`; symlinkSync(f.trace, link); f.trace = link; });
  reject('trace hardlink', f => linkSync(f.trace, `${f.trace}-link`));
  for (const index of [0, 1, 2, 3]) {
    reject(`missing event ${index}`, f => { f.events.splice(index, 1); f.save(); });
    reject(`duplicate event ${index}`, f => { f.events.push(f.events[index]); f.save(); });
    reject(`wrong direction ${index}`, f => { f.events[index].direction = 'send'; f.save(); });
    reject(`timestamp type ${index}`, f => { f.events[index].at = '1'; f.save(); });
  }
  reject('old external receipt', f => { f.events.push({ ...f.events[0], type: prefix + 'externalExitObserved' }); f.save(); });
  reject('exit before startup', f => { [f.events[0], f.events[1]] = [f.events[1], f.events[0]]; f.save(); });
  reject('completion before exit', f => { [f.events[1], f.events[2]] = [f.events[2], f.events[1]]; f.save(); });
  reject('willTerminate before completion', f => { [f.events[2], f.events[3]] = [f.events[3], f.events[2]]; f.save(); });
  for (const [field, value] of [['expected', false], ['status', 15], ['reason', 1], ['generation', 2], ['processIdentifier', deadHost]]) {
    reject(`exit ${field}`, f => { f.events[1].payload[field] = value; f.save(); });
  }
  for (const [field, value] of [['complete', 'true'], ['originalSidecarAbsent', false], ['originalWindowRetained', true],
    ['launcherReturnedMarkerMatched', false], ['hookEnteredMarkerMatched', false], ['runId', 'stale'],
    ['currentProcessIdentifier', deadJava], ['hostProcessIdentifier', deadJava], ['processIdentifier', true],
    ['windowId', 'secondary'], ['recoveryGeneration', 1], ['terminationSource', 'driver']]) {
    reject(`final ${field}`, f => { f.events[3].payload[field] = value; f.save(); });
  }
  reject('live host', f => { f.events[3].payload.hostProcessIdentifier = alive.pid; f.save(); }, /still alive/);
  reject('live JVM', f => {
    for (const name of names) writeFileSync(join(f.dir, name), JSON.stringify({ ...f.marker, processIdentifier: alive.pid }));
    for (const index of [0, 1, 3]) f.events[index].payload.processIdentifier = alive.pid;
    f.save();
  }, /still alive/);
  const f = fixture();
  const realKill = process.kill;
  try {
    process.kill = (pid, signal) => {
      assert.equal(signal, 0);
      if (pid === deadJava) { const error = new Error('simulated signal-zero permission failure'); error.code = 'EPERM'; throw error; }
      return realKill(pid, signal);
    };
    assert.throws(() => verifyExitEvidence(f.dir, f.trace, f.mode), /EPERM/);
    cases++;
  } finally { process.kill = realKill; }
  const cli = fixture();
  let result = spawnSync(process.execPath, [verifier, cli.dir, cli.trace, cli.mode], { encoding: 'utf8', timeout: 5000 });
  assert.ifError(result.error); assert.equal(result.status, 0, result.stderr);
  assert.equal(readFileSync(cli.trace, 'utf8').split(prefix + 'externalExitObserved').length, 2);
  result = spawnSync(process.execPath, [verifier, cli.dir, cli.trace, cli.mode], { encoding: 'utf8', timeout: 5000 });
  assert.equal(result.status, 1); assert.match(result.stderr, /already exists/);
  cases += 2;
  console.log(`Host shutdown exit evidence smoke passed: ${cases} real-owned PID and malformed-evidence cases`);
} finally {
  alive.kill('SIGKILL');
  await once(alive, 'exit');
  rmSync(work, { recursive: true, force: true });
}
