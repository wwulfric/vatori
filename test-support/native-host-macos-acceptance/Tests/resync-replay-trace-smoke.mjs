import assert from 'node:assert/strict';
import {verifyResyncReplay} from './resync-replay-trace-verify.mjs';
const event = (type, direction, payload) => ({type, direction, payload: {envelope: {payload}}});
const rows = [
  {type: 'sidecar.started'},
  event('opaquePush', 'recv', {connectionId: 'c', pushSequence: 2, resync: true}),
  event('opaquePushAck', 'send', {connectionId: 'c', pushSequence: 2}),
  event('opaquePushAck', 'send', {connectionId: 'c', pushSequence: 2}),
  event('opaquePush', 'recv', {connectionId: 'c', pushSequence: 3, resync: false}),
  {type: 'standardBundle.frameworkAcceptance', payload: {complete: true, errorFree: true,
    resyncReplay: {source: 'test-replay-of-server-envelope', observed: true, sequence: 2,
      connectionId: 'c', messagesDelta: 0, resyncsDelta: 0, duplicatesDelta: 1, acksDelta: 1}}}
];
const text = r => r.map(row => 'HOST_PROTOCOL ' + JSON.stringify(row)).join('\n');
verifyResyncReplay(text(rows));
for (let i = 0; i < rows.length; i++) {
  const changed = structuredClone(rows); changed.splice(i, 1);
  assert.throws(() => verifyResyncReplay(text(changed)));
}
for (const field of ['messagesDelta', 'resyncsDelta', 'duplicatesDelta', 'acksDelta']) {
  const changed = structuredClone(rows); changed.at(-1).payload.resyncReplay[field] += 1;
  assert.throws(() => verifyResyncReplay(text(changed)));
}
const stale = structuredClone(rows); stale.splice(4, 0, {type: 'sidecar.started'});
assert.throws(() => verifyResyncReplay(text(stale)), 'old JVM evidence cannot satisfy new JVM');
console.log('Controlled resync replay verifier passed (synthetic, 11 negatives)');
