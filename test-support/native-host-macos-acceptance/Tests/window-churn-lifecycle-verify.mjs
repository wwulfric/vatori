import assert from 'node:assert/strict';
import fs from 'node:fs';
import {pathToFileURL} from 'node:url';

// Structural prerequisite, not an R02 pass: creation does not prove rendered
// Flow state, consumed push, stale callback rejection, or physical interaction.
export function verifyWindowChurnLifecycle(text) {
  const rows = text.split('\n').filter(line => line.startsWith('HOST_PROTOCOL '))
    .map(line => JSON.parse(line.slice(14)));
  const events = type => rows.filter(row => row.type === type);
  const starts = events('sidecar.started'), exits = events('sidecar.exit');
  assert.equal(starts.length, 1, 'one continuous JVM required');
  assert.equal(exits.length, 1, 'actual final JVM exit required');
  const start = starts[0], exit = exits[0];
  assert.ok(Number.isInteger(start.payload.processIdentifier) && start.payload.processIdentifier > 0);
  assert.equal(start.payload.generation, 1);
  assert.equal(exit.payload.processIdentifier, start.payload.processIdentifier);
  assert.equal(exit.payload.generation, 1);
  assert.equal(exit.payload.expected, true);
  assert.equal(exit.payload.status, 0);
  assert.ok(!rows.some(row => row.type === 'error' || /sidecar\.(recovery|productReplacement|replacement|rollback)/.test(row.type)
    || row.type === 'windowClosedDropped'), 'uninterrupted normal lifecycle required');
  const active = new Map(), sessions = new Set(), incarnations = new Map();
  const closeRequested = new Set();
  let peak = 0;
  const scopeKey = p => JSON.stringify([p.windowId, p.sessionId, p.uiId]);
  for (const [index, row] of rows.entries()) {
    const p = row.payload;
    if (!['window.create', 'window.closeRequested', 'windowClosed'].includes(row.type)) continue;
    // Some acceptance drivers emit an intent-only close trace. Only the native
    // windowWillClose event carries the full scope and counts as actual close.
    if (row.type === 'window.closeRequested' && p.sessionId === undefined) continue;
    assert.ok(index > rows.indexOf(start) && index < rows.indexOf(exit), 'window lifetime outside JVM lifetime');
    for (const key of ['windowId', 'sessionId', 'uiId']) assert.ok(typeof p[key] === 'string' && p[key].length > 0);
    const key = scopeKey(p);
    if (row.type === 'window.create') {
      assert.equal(row.direction, 'send');
      assert.equal(p.mode, 'flow-ipc');
      assert.ok(!active.has(p.windowId), 'reopen before previous owner retired');
      assert.ok(!sessions.has(p.sessionId), 'session reused across owners');
      sessions.add(p.sessionId);
      const history = incarnations.get(p.windowId) || [];
      if (history.length) {
        assert.equal(active.size, 2, 'both sibling owners must survive every reopen');
        assert.equal(p.uiId, history[0].uiId, 'same public UI identity required');
      }
      history.push(p); incarnations.set(p.windowId, history);
      active.set(p.windowId, key);
      peak = Math.max(peak, active.size);
      assert.ok(active.size <= 3, 'bounded three-window profile');
    } else {
      assert.equal(active.get(p.windowId), key, 'close must match exact live owner');
      if (row.type === 'window.closeRequested') {
        assert.equal(row.direction, 'recv');
        assert.ok(!closeRequested.has(key), 'duplicate native close');
        closeRequested.add(key);
      } else {
        assert.equal(row.direction, 'send');
        assert.equal(p.target, 'java-sidecar');
        assert.equal(p.transport, 'flow-ipc');
        assert.ok(closeRequested.has(key), 'sidecar notification cannot substitute for native close');
        active.delete(p.windowId);
      }
    }
  }
  assert.equal(peak, 3, 'three simultaneous owners required');
  assert.equal(active.size, 0, 'all owners must retire');
  assert.equal(incarnations.size, 3, 'three public window identities required');
  assert.deepEqual([...incarnations.values()].map(history => history.length).sort((a,b) => a-b), [1,1,11],
    'one owner must complete ten reopen cycles while two siblings survive');
  return {peakWindows: 3, reopenCycles: 10, lifecycleOnly: true, uiIsolationProven: false};
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  assert.equal(process.argv.length, 3, 'usage: window-churn-lifecycle-verify.mjs TRACE');
  console.log('PASS window churn lifecycle only', verifyWindowChurnLifecycle(fs.readFileSync(process.argv[2], 'utf8')));
}
