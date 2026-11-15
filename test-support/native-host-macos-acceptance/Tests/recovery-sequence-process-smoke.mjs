import assert from 'node:assert/strict';
import {verifyRecoverySequence} from './recovery-sequence-process-verify.mjs';
const rows = [];
const add = (type, payload) => rows.push({type, payload});
add('sidecar.started', {generation: 1, processIdentifier: 101});
for (let i = 1; i <= 3; i++) {
  add('sidecar.exit', {generation: i, processIdentifier: 100 + i, expected: false, status: 9});
  const p = {attempt: i, previousProcessIdentifier: 100 + i};
  add('sidecar.recoveryScheduled', p);
  add('sidecar.recoveryStarted', p);
  add('sidecar.started', {generation: i + 1, processIdentifier: 101 + i});
  add('sidecar.recoveryReady', {...p, processIdentifier: 101 + i,
    processReplaced: true, recoveryGeneration: i, windowsReloaded: 1});
}
add('sidecar.exit', {generation: 4, processIdentifier: 104, expected: true, status: 0});
const text = r => r.map(x => 'HOST_PROTOCOL ' + JSON.stringify(x)).join('\n');
assert.deepEqual(verifyRecoverySequence(text(rows)), {
  recoveries: 3, processes: 4, processChainOnly: true, uiBaselineProven: false});
let negatives = 0;
for (let i = 0; i < rows.length; i++) {
  const changed = structuredClone(rows); changed.splice(i, 1);
  assert.throws(() => verifyRecoverySequence(text(changed))); negatives++;
}
for (const mutate of [
  r => { r[3].payload.previousProcessIdentifier = 999; },
  r => { r[5].payload.processIdentifier = 999; },
  r => { r[5].payload.windowsReloaded = 0; },
  r => { r[5].payload.processReplaced = false; },
  r => { r[5].payload.recoveryGeneration = 3; },
  r => { r[5].payload.attempt = 9; },
  r => { r[1].payload.expected = true; },
  r => { r.at(-1).payload.status = 9; },
  r => { [r[5], r[6]] = [r[6], r[5]]; },
  r => { r[4].payload.processIdentifier = 101; },
  r => { r.push({type: 'sidecar.recoveryReadyTimeout'}); },
  r => { r.push({type: 'sidecar.productReplacementRequested'}); },
  r => { r.push({type: 'sidecar.started', payload: {generation: 5, processIdentifier: 105}}); }
]) {
  const changed = structuredClone(rows); mutate(changed);
  assert.throws(() => verifyRecoverySequence(text(changed))); negatives++;
}
console.log(`Recovery sequence process verifier passed (synthetic, ${negatives} negatives; not UI evidence)`);
