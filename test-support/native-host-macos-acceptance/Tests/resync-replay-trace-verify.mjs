import assert from 'node:assert/strict';
import fs from 'node:fs';
import {pathToFileURL} from 'node:url';

export function verifyResyncReplay(text) {
  const rows = text.split('\n').filter(line => line.startsWith('HOST_PROTOCOL '))
    .map(line => JSON.parse(line.slice(14)));
  const finals = rows.filter(row => row.type === 'standardBundle.frameworkAcceptance');
  assert.equal(finals.length, 1);
  const final = finals[0];
  assert.equal(final.payload.complete, true);
  assert.equal(final.payload.errorFree, true);
  const replay = final.payload.resyncReplay;
  assert.ok(replay);
  assert.equal(replay.source, 'test-replay-of-server-envelope');
  assert.equal(replay.observed, true);
  assert.equal(replay.messagesDelta, 0);
  assert.equal(replay.resyncsDelta, 0);
  assert.equal(replay.duplicatesDelta, 1);
  assert.equal(replay.acksDelta, 1);
  assert.ok(Number.isSafeInteger(replay.sequence) && replay.sequence > 0);
  assert.ok(typeof replay.connectionId === 'string' && replay.connectionId.length > 0);
  const end = rows.indexOf(final);
  const start = rows.slice(0, end).findLastIndex(row => row.type === 'sidecar.started');
  assert.ok(start >= 0);
  const generation = rows.slice(start, end);
  const snapshot = generation.findIndex(row => row.type === 'opaquePush'
    && row.direction === 'recv' && row.payload?.envelope?.payload?.resync === true
    && row.payload.envelope.payload.connectionId === replay.connectionId
    && row.payload.envelope.payload.pushSequence === replay.sequence);
  assert.ok(snapshot >= 0, 'actual server snapshot in the final JVM');
  const subsequent = generation.slice(snapshot + 1);
  const acks = subsequent.filter(row => row.type === 'opaquePushAck' && row.direction === 'send'
    && row.payload?.envelope?.payload?.connectionId === replay.connectionId
    && row.payload.envelope.payload.pushSequence === replay.sequence);
  assert.ok(acks.length >= 2, 'both original and replay acknowledged through host');
  assert.ok(subsequent.some(row => row.type === 'opaquePush' && row.direction === 'recv'
    && row.payload?.envelope?.payload?.connectionId === replay.connectionId
    && row.payload.envelope.payload.resync === false
    && row.payload.envelope.payload.pushSequence > replay.sequence), 'later normal push');
  return {sequence: replay.sequence, acknowledgements: acks.length, source: replay.source};
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  console.log('PASS controlled resync replay', verifyResyncReplay(fs.readFileSync(process.argv[2], 'utf8')));
}
