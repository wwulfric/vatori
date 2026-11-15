import fs from 'node:fs';
import assert from 'node:assert/strict';
import {pathToFileURL} from 'node:url';

// Process-chain subgate only. A ready lifecycle event does not prove that
// WKWebView rendered, consumed push, or accepted any input after recovery.
export function verifyRecoverySequence(text) {
  const rows = text.split('\n').filter(l => l.startsWith('HOST_PROTOCOL '))
    .map(l => JSON.parse(l.slice(14)));
  const events = type => rows.filter(r => r.type === type);
  const starts = events('sidecar.started');
  const exits = events('sidecar.exit');
  const scheduled = events('sidecar.recoveryScheduled');
  const restarting = events('sidecar.recoveryStarted');
  const ready = events('sidecar.recoveryReady');
  assert.equal(starts.length, 4, 'three recoveries require four actual JVM starts');
  assert.equal(exits.length, 4, 'all four actual exits required');
  for (const list of [scheduled, restarting, ready]) assert.equal(list.length, 3);
  assert.equal(new Set(starts.map(r => r.payload.processIdentifier)).size, 4);
  assert.equal(rows.some(r => /sidecar\.(productReplacement|replacement|rollback)/.test(r.type)
    || ['sidecar.recoveryReadyTimeout', 'host.failureShutdownCompleted'].includes(r.type)), false);
  const ordered = sequence => {
    for (let i = 1; i < sequence.length; i++)
      assert.ok(rows.indexOf(sequence[i]) > rows.indexOf(sequence[i - 1]), 'causal lifecycle order');
  };
  for (let i = 0; i < 4; i++) {
    const start = starts[i], exit = exits[i];
    const pid = start.payload.processIdentifier;
    assert.ok(Number.isInteger(pid) && pid > 0);
    assert.equal(start.payload.generation, i + 1);
    assert.equal(exit.payload.generation, i + 1);
    assert.equal(exit.payload.processIdentifier, pid);
    assert.equal(exit.payload.expected, i === 3);
    assert.ok(Number.isInteger(exit.payload.status));
    ordered([start, exit]);
    if (i === 3) {
      assert.equal(exit.payload.status, 0);
      ordered([ready[2], exit]);
      continue;
    }
    const nextPid = starts[i + 1].payload.processIdentifier;
    for (const event of [scheduled[i], restarting[i], ready[i]]) {
      assert.equal(event.payload.previousProcessIdentifier, pid);
      assert.ok(Number.isInteger(event.payload.attempt) && event.payload.attempt > 0);
      assert.equal(event.payload.attempt, scheduled[i].payload.attempt);
    }
    assert.equal(ready[i].payload.processIdentifier, nextPid);
    assert.equal(ready[i].payload.processReplaced, true);
    assert.equal(ready[i].payload.recoveryGeneration, i + 1);
    assert.ok(Number.isInteger(ready[i].payload.windowsReloaded) && ready[i].payload.windowsReloaded > 0);
    ordered([exit, scheduled[i], restarting[i], starts[i + 1], ready[i], exits[i + 1]]);
  }
  return {recoveries: 3, processes: 4, processChainOnly: true, uiBaselineProven: false};
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  assert.equal(process.argv.length, 3, 'usage: recovery-sequence-process-verify.mjs TRACE');
  console.log('PASS recovery process chain only', verifyRecoverySequence(fs.readFileSync(process.argv[2], 'utf8')));
}
