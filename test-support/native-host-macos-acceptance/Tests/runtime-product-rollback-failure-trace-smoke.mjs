import assert from 'node:assert/strict';
import {verifyRollbackFailure} from './runtime-product-rollback-failure-trace-verify.mjs';
const scope = {version: 1, windowId: 'main', sessionId: 's', uiId: 'u', correlationId: 'r'};
const rows = [];
const add = (type, payload, direction = 'recv') => rows.push({type, payload, direction, at: rows.length * 10});
const launch = (generation, pid) => {
  add('launch.sidecar', {generation, command: `java -jar $APP/Contents/Resources/${generation === 2 ? 'RuntimeCandidates/next' : 'Sidecar'}/application.jar`});
  add('sidecar.started', {generation, processIdentifier: pid});
};
const exit = (generation, pid, status) => add('sidecar.exit', {generation, processIdentifier: pid, status, expected: false});
add('launch', {transport: 'flow-ipc'}); launch(1, 10);
add('standardBundleReady', {requestTransport: 'opaque-http-stream-ipc'});
add('standardBundle.runtimeReplacementStatus', {status: 'none', observedInDom: true});
add('command', {transport: 'flow-ipc', source: 'java-sidecar', sidecarGeneration: 1,
  envelope: {...scope, payload: {command: 'runtime.replacement.request', args: {candidateId: 'next', requestId: 'req'}}}});
add('sidecar.productReplacementRequested', {operationId: 'op', requestId: 'req', candidateId: 'next', requestingGeneration: 1, processIdentifier: 10});
add('capabilityResult', {sidecarGeneration: 1, envelope: {...scope, payload: {success: true,
  status: 'pending', operationId: 'op', permissionDecision: 'allowed', permissionReason: 'manifest-allow'}}});
exit(1, 10, 143); launch(2, 20); exit(2, 20, 42);
add('sidecar.replacementFailed', {requestId: 'op', cause: 'candidate-exited-before-ready'});
add('sidecar.rollbackStarted', {requestId: 'op'});
launch(3, 30); exit(3, 30, 43);
add('sidecar.rollbackFailed', {requestId: 'op', cause: 'rollback-exited-before-ready', processIdentifier: 30, status: 43});
add('sidecar.productReplacementResult', {operationId: 'op', status: 'rollback-failed'});
add('host.nativeResourceDrainCompleted', {drained: true, pendingCount: 0, failureCount: 0});
add('host.sidecarShutdownCompleted', {succeeded: true});
add('host.failureShutdownCompleted', {exitStatus: 1});
const text = r => r.map(x => 'HOST_PROTOCOL ' + JSON.stringify(x)).join('\n');
verifyRollbackFailure(text(rows));
let negatives = 0;
for (let i = 0; i < rows.length; i++) {
  const r = structuredClone(rows); r.splice(i, 1);
  assert.throws(() => verifyRollbackFailure(text(r))); negatives++;
}
const row = (r, type) => r.find(x => x.type === type);
for (const mutate of [
  r => { row(r, 'capabilityResult').payload.envelope.payload.permissionDecision = 'denied'; },
  r => { row(r, 'capabilityResult').payload.envelope.sessionId = 'other'; },
  r => { row(r, 'sidecar.rollbackFailed').payload.status = 0; },
  r => { row(r, 'sidecar.rollbackFailed').payload.processIdentifier = 99; },
  r => { row(r, 'sidecar.rollbackFailed').payload.requestId = 'other'; },
  r => { row(r, 'sidecar.productReplacementResult').payload.status = 'rolled-back'; },
  r => { row(r, 'sidecar.productReplacementResult').payload.operationId = 'other'; },
  r => { row(r, 'host.nativeResourceDrainCompleted').payload.pendingCount = 1; },
  r => { row(r, 'host.failureShutdownCompleted').payload.exitStatus = 0; },
  r => { row(r, 'host.failureShutdownCompleted').at = 40000; },
  r => { r.push({type: 'sidecar.recoveryScheduled', payload: {}}); },
  r => { r.push({type: 'sidecar.rollbackReady', payload: {}}); },
  r => { r.push({type: 'sidecar.started', payload: {generation: 4, processIdentifier: 40}}); },
  r => { const i = r.findIndex(x => x.type === 'sidecar.exit' && x.payload.generation === 2); [r[i], r[i + 4]] = [r[i + 4], r[i]]; }
]) {
  const r = structuredClone(rows); mutate(r);
  assert.throws(() => verifyRollbackFailure(text(r))); negatives++;
}
console.log(`Rollback failure verifier passed: valid synthetic fixture and ${negatives} negatives`);

const blockedRows = structuredClone(rows);
const exitIndex = blockedRows.findIndex(r => r.type === 'sidecar.exit' && r.payload.generation === 3);
const [blockedExit] = blockedRows.splice(exitIndex, 1);
blockedExit.payload.expected = true; blockedExit.payload.status = 9;
const failure = row(blockedRows, 'sidecar.rollbackFailed');
failure.payload.cause = 'rollback-ready-timeout';
delete failure.payload.status; failure.payload.timeoutSeconds = 15;
failure.at = blockedRows.find(r => r.type === 'sidecar.started' && r.payload.generation === 3).at + 15000;
row(blockedRows, 'sidecar.productReplacementResult').at = failure.at + 1;
row(blockedRows, 'host.nativeResourceDrainCompleted').at = failure.at + 2;
blockedExit.at = failure.at + 2010;
blockedRows.splice(blockedRows.findIndex(r => r.type === 'host.sidecarShutdownCompleted'), 0,
  {type: 'sidecar.shutdownTerminated', at: failure.at + 1000, payload: {generation: 3, processIdentifier: 30, graceSeconds: 1}},
  {type: 'sidecar.shutdownEscalated', at: failure.at + 2000, payload: {generation: 3, processIdentifier: 30, graceSeconds: 1}},
  blockedExit);
row(blockedRows, 'host.sidecarShutdownCompleted').at = failure.at + 2020;
row(blockedRows, 'host.failureShutdownCompleted').at = failure.at + 2030;
const blockedText = r => 'Test rollback deliberately never ready\nTest rollback shutdown hook blocked\n' + text(r);
verifyRollbackFailure(blockedText(blockedRows), 'blocked-rollback');
assert.throws(() => verifyRollbackFailure(text(blockedRows), 'blocked-rollback'));
assert.throws(() => verifyRollbackFailure(blockedText(blockedRows)));
let blockedNegatives = 2;
for (let i = 0; i < blockedRows.length; i++) {
  const r = structuredClone(blockedRows); r.splice(i, 1);
  assert.throws(() => verifyRollbackFailure(blockedText(r), 'blocked-rollback')); blockedNegatives++;
}
for (const mutate of [
  r => { row(r, 'sidecar.rollbackFailed').at = 100; },
  r => { row(r, 'sidecar.rollbackFailed').payload.timeoutSeconds = 1; },
  r => { row(r, 'sidecar.shutdownEscalated').payload.processIdentifier = 20; },
  r => { row(r, 'sidecar.shutdownEscalated').payload.generation = 2; },
  r => { row(r, 'sidecar.shutdownEscalated').at = row(r, 'sidecar.shutdownTerminated').at; },
  r => { r.find(x => x.type === 'sidecar.exit' && x.payload.generation === 3).payload.expected = false; },
  r => { const i = r.findIndex(x => x.type === 'sidecar.exit' && x.payload.generation === 3); [r[i], r[i + 1]] = [r[i + 1], r[i]]; },
  r => { row(r, 'host.failureShutdownCompleted').payload.exitStatus = 0; }
]) {
  const r = structuredClone(blockedRows); mutate(r);
  assert.throws(() => verifyRollbackFailure(blockedText(r), 'blocked-rollback')); blockedNegatives++;
}
console.log(`Blocked rollback verifier passed: valid synthetic fixture and ${blockedNegatives} negatives`);
