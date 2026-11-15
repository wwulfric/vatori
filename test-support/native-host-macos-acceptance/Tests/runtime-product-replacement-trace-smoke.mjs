import assert from 'node:assert/strict';
import { verifyProductReplacement } from './runtime-product-replacement-trace-verify.mjs';
const scope = {windowId: 'main', sessionId: 'session', uiId: 'main', version: 1};
const operationId = 'operation';
const result = {success: true, mode: 'host-runtime-replacement', permissionDecision: 'allowed',
  permissionReason: 'manifest-allow', operationId};
const rows = [
  {type: 'sidecar.started', payload: {generation: 1, processIdentifier: 10}},
  {type: 'command', direction: 'recv', payload: {transport: 'flow-ipc', source: 'java-sidecar', sidecarGeneration: 1,
    envelope: {...scope, correlationId: 'request', payload: {command: 'runtime.replacement.request',
      args: {candidateId: 'next', requestId: 'request'}}}}},
  {type: 'capabilityResult', payload: {sidecarGeneration: 1,
    envelope: {...scope, correlationId: 'request', payload: {...result, status: 'pending'}}}},
  {type: 'sidecar.productReplacementRequested', payload: {operationId, requestId: 'request', candidateId: 'next',
    requestingGeneration: 1, processIdentifier: 10}},
  {type: 'sidecar.exit', payload: {generation: 1, processIdentifier: 10, expected: false, status: 143}},
  {type: 'launch.sidecar', payload: {generation: 2,
    command: 'java -jar $APP/Contents/Resources/RuntimeCandidates/next/application.jar'}},
  {type: 'sidecar.started', payload: {generation: 2, processIdentifier: 20}},
  {type: 'sidecar.productReplacementResult', payload: {operationId, status: 'replaced'}},
  {type: 'command', payload: {sidecarGeneration: 2, envelope: {...scope, correlationId: 'query',
    payload: {command: 'runtime.replacement.status'}}}},
  {type: 'capabilityResult', payload: {sidecarGeneration: 2,
    envelope: {...scope, correlationId: 'query', payload: {...result, status: 'replaced', requestingGeneration: 1}}}},
  {type: 'standardBundle.productReplacement', payload: {status: 'replaced', observedInDom: true, recoveryGeneration: 1}},
  {type: 'sidecar.exit', payload: {generation: 2, processIdentifier: 20, expected: true, status: 0}}
];
const text = r => r.map(row => 'HOST_PROTOCOL ' + JSON.stringify(row)).join('\n');
verifyProductReplacement(text(rows));
let negatives = 0;
for (let index = 0; index < rows.length; index++) {
  const changed = structuredClone(rows); changed.splice(index, 1);
  assert.throws(() => verifyProductReplacement(text(changed))); negatives++;
}
const mutations = [
  r => { r[4].payload.processIdentifier = 99; },
  r => { r[6].payload.processIdentifier = 10; },
  r => { [r[4], r[6]] = [r[6], r[4]]; },
  r => { r[5].payload.command = 'java -jar old.jar'; },
  r => { r[2].payload.envelope.payload.permissionDecision = 'denied'; },
  r => { r[9].payload.envelope.payload.operationId = 'other'; },
  r => { r[9].payload.envelope.sessionId = 'other'; },
  r => { r[9].payload.envelope.payload.requestingGeneration = 2; },
  r => { r[10].payload.observedInDom = false; },
  r => { r[11].payload.status = 1; }
];
for (const mutate of mutations) {
  const changed = structuredClone(rows); mutate(changed);
  assert.throws(() => verifyProductReplacement(text(changed))); negatives++;
}
console.log(`Product replacement trace verifier smoke passed: valid fixture and ${negatives} negatives (synthetic)`);
const rollbackRows = [
  ...structuredClone(rows.slice(0, 7)),
  {type: 'sidecar.exit', payload: {generation: 2, processIdentifier: 20, expected: false, status: 42}},
  {type: 'sidecar.replacementFailed', payload: {requestId: operationId, cause: 'candidate-exited-before-ready'}},
  {type: 'launch.sidecar', payload: {generation: 3, command: 'java -jar $APP/Contents/Resources/Sidecar/application.jar'}},
  {type: 'sidecar.started', payload: {generation: 3, processIdentifier: 30}},
  {type: 'sidecar.rollbackReady', payload: {requestId: operationId, processIdentifier: 30}},
  ...structuredClone(rows.slice(7))
];
for (const row of rollbackRows) {
  if (row.type === 'sidecar.productReplacementResult' || row.type === 'standardBundle.productReplacement') row.payload.status = 'rolled-back';
  if (['command', 'capabilityResult'].includes(row.type) && row.payload.sidecarGeneration === 2) {
    row.payload.sidecarGeneration = 3;
    if (row.type === 'capabilityResult') row.payload.envelope.payload.status = 'rolled-back';
  }
}
rollbackRows.at(-1).payload.generation = 3;
rollbackRows.at(-1).payload.processIdentifier = 30;
verifyProductReplacement(text(rollbackRows), 'rollback');
assert.throws(() => verifyProductReplacement(text(rows), 'rollback'));
assert.throws(() => verifyProductReplacement(text(rollbackRows), 'success'));
let rollbackNegatives = 2;
for (let index = 0; index < rollbackRows.length; index++) {
  const changed = structuredClone(rollbackRows); changed.splice(index, 1);
  assert.throws(() => verifyProductReplacement(text(changed), 'rollback')); rollbackNegatives++;
}
for (const mutate of [
  r => { r[7].payload.status = 0; },
  r => { [r[7], r[9]] = [r[9], r[7]]; },
  r => { r[9].payload.command = 'java -jar $APP/Contents/Resources/RuntimeCandidates/next/application.jar'; },
  r => { r[10].payload.processIdentifier = 20; },
  r => { r[11].payload.requestId = 'other'; },
  r => { r.push({type: 'sidecar.recoveryScheduled', payload: {attempt: 1}}); }
]) {
  const changed = structuredClone(rollbackRows); mutate(changed);
  assert.throws(() => verifyProductReplacement(text(changed), 'rollback')); rollbackNegatives++;
}
console.log(`Product rollback trace verifier smoke passed: valid fixture and ${rollbackNegatives} negatives (synthetic)`);

const rejectionRows = [
  structuredClone(rows[0]), structuredClone(rows[1]),
  {type: 'capabilityResult', payload: {sidecarGeneration: 1, envelope: {...scope, correlationId: 'request',
    payload: {success: false, errorCode: 'invalid-candidate', permissionDecision: 'allowed', permissionReason: 'manifest-allow'}}}},
  {type: 'standardBundle.productReplacementRejected', payload: {errorCode: 'invalid-candidate', observedInDom: true, recoveryGeneration: 0}},
  {type: 'command', payload: {sidecarGeneration: 1, envelope: {...scope, correlationId: 'query-after',
    payload: {command: 'runtime.replacement.status'}}}},
  {type: 'capabilityResult', payload: {sidecarGeneration: 1, envelope: {...scope, correlationId: 'query-after',
    payload: {success: true, status: 'none', permissionDecision: 'allowed'}}}},
  {type: 'standardBundle.productRejectionStatus', payload: {status: 'none', observedInDom: true}},
  {type: 'sidecar.exit', payload: {generation: 1, processIdentifier: 10, expected: true, status: 0}}
];
verifyProductReplacement(text(rejectionRows), 'rejection');
let rejectionNegatives = 0;
for (let index = 0; index < rejectionRows.length; index++) {
  const changed = structuredClone(rejectionRows); changed.splice(index, 1);
  assert.throws(() => verifyProductReplacement(text(changed), 'rejection')); rejectionNegatives++;
}
for (const mutate of [
  r => { r[2].payload.envelope.payload.success = true; },
  r => { r[2].payload.envelope.payload.errorCode = 'permission-denied'; },
  r => { r[2].payload.envelope.payload.permissionDecision = 'denied'; },
  r => { r[2].payload.envelope.windowId = 'other'; },
  r => { r[3].payload.recoveryGeneration = 1; },
  r => { r[3].payload.observedInDom = false; },
  r => { r[5].payload.envelope.payload.status = 'pending'; },
  r => { r[5].payload.envelope.payload.operationId = 'unexpected-record'; },
  r => { r[5].payload.sidecarGeneration = 2; },
  r => { r[5].payload.envelope.sessionId = 'other'; },
  r => { [r[5], r[6]] = [r[6], r[5]]; },
  r => { r[7].payload.processIdentifier = 99; },
  r => { r[7].payload.expected = false; },
  r => { r.push({type: 'sidecar.productReplacementRequested', payload: {}}); },
  r => { r.push({type: 'sidecar.replacementTerminated', payload: {}}); },
  r => { r.push({type: 'sidecar.recoveryScheduled', payload: {}}); },
  r => { r.push({type: 'sidecar.started', payload: {generation: 2, processIdentifier: 20}}); }
]) {
  const changed = structuredClone(rejectionRows); mutate(changed);
  assert.throws(() => verifyProductReplacement(text(changed), 'rejection')); rejectionNegatives++;
}
console.log(`Product rejection trace verifier smoke passed: valid fixture and ${rejectionNegatives} negatives (synthetic)`);

const blockedRows = structuredClone(rows);
blockedRows[4].payload.status = 9;
blockedRows[4].at = 1200;
blockedRows.splice(4, 0,
  {type: 'sidecar.replacementTerminated', at: 100, payload: {generation: 1, processIdentifier: 10, reason: 'product-replacement'}},
  {type: 'sidecar.replacementEscalated', at: 1100, payload: {generation: 1, processIdentifier: 10, reason: 'product-replacement', graceSeconds: 1}});
const blockedText = r => 'Test initial JVM shutdown hook blocked\n' + text(r);
verifyProductReplacement(blockedText(blockedRows), 'blocked-retirement');
assert.throws(() => verifyProductReplacement(text(blockedRows), 'blocked-retirement'));
let blockedNegatives = 1;
for (let i = 0; i < blockedRows.length; i++) {
  const r = structuredClone(blockedRows); r.splice(i, 1);
  assert.throws(() => verifyProductReplacement(blockedText(r), 'blocked-retirement')); blockedNegatives++;
}
for (const mutate of [
  r => { r[5].payload.processIdentifier = 99; },
  r => { r[5].payload.generation = 2; },
  r => { r[5].payload.reason = 'recovery'; },
  r => { r[5].payload.graceSeconds = 2; },
  r => { r[5].at = 150; },
  r => { r[5].at = 10000; },
  r => { r[6].payload.status = 143; },
  r => { r[6].at = 6000; },
  r => { [r[5], r[6]] = [r[6], r[5]]; },
  r => { r.push({type: 'sidecar.recoveryScheduled', payload: {}}); },
  r => { r.push({type: 'sidecar.started', payload: {generation: 3, processIdentifier: 30}}); }
]) {
  const r = structuredClone(blockedRows); mutate(r);
  assert.throws(() => verifyProductReplacement(blockedText(r), 'blocked-retirement')); blockedNegatives++;
}
console.log(`Blocked retirement verifier passed: valid synthetic fixture and ${blockedNegatives} negatives`);

const hungRows = structuredClone(rollbackRows);
hungRows[6].at = 0;
const hungExit = hungRows[7]; hungExit.at = 16010; hungExit.payload.status = 9;
const hungFailed = hungRows[8]; hungFailed.payload.cause = 'candidate-ready-timeout';
hungRows.splice(7, 2,
  {type: 'sidecar.replacementReadyTimeout', at: 15000, payload: {requestId: operationId, processIdentifier: 20, timeoutSeconds: 15}},
  hungFailed,
  {type: 'sidecar.replacementTerminated', at: 15001, payload: {generation: 2, processIdentifier: 20, reason: 'candidate-ready-timeout'}},
  {type: 'sidecar.replacementEscalated', at: 16001, payload: {generation: 2, processIdentifier: 20, reason: 'candidate-ready-timeout', graceSeconds: 1}},
  hungExit);
const hungText = r => 'Test candidate deliberately never ready\nTest candidate shutdown hook blocked\n' + text(r);
verifyProductReplacement(hungText(hungRows), 'blocked-candidate');
assert.throws(() => verifyProductReplacement(text(hungRows), 'blocked-candidate'));
let hungNegatives = 1;
for (let i = 0; i < hungRows.length; i++) {
  const r = structuredClone(hungRows); r.splice(i, 1);
  assert.throws(() => verifyProductReplacement(hungText(r), 'blocked-candidate')); hungNegatives++;
}
for (const mutate of [
  r => { r[7].payload.requestId = 'other'; },
  r => { r[7].payload.timeoutSeconds = 1; },
  r => { r[7].at = 100; },
  r => { r[7].at = 30000; },
  r => { r[10].payload.processIdentifier = 10; },
  r => { r[10].payload.generation = 1; },
  r => { r[10].at = 15002; },
  r => { r[11].payload.status = 42; },
  r => { [r[10], r[11]] = [r[11], r[10]]; },
  r => { r.push({type: 'sidecar.started', payload: {generation: 4, processIdentifier: 40}}); }
]) {
  const r = structuredClone(hungRows); mutate(r);
  assert.throws(() => verifyProductReplacement(hungText(r), 'blocked-candidate')); hungNegatives++;
}
console.log(`Blocked candidate verifier passed: valid synthetic fixture and ${hungNegatives} negatives`);

const burstRows = structuredClone(rows);
burstRows[1].payload.envelope.payload.args.requestId = 'framework-burst-0';
burstRows[3].payload.requestId = 'framework-burst-0';
burstRows[2].payload.envelope.payload.duplicate = false;
const extras = [];
for (let i = 0; i < 3; i++) {
  const request = structuredClone(burstRows[1]);
  request.payload.envelope.correlationId = `extra-${i}`;
  request.payload.envelope.payload.args.requestId = i === 0 ? 'framework-burst-0' : 'framework-burst-1';
  const receipt = structuredClone(burstRows[2]);
  receipt.payload.envelope.correlationId = `extra-${i}`;
  receipt.payload.envelope.payload = i === 0
    ? {...receipt.payload.envelope.payload, duplicate: true}
    : {success: false, errorCode: 'busy', permissionDecision: 'allowed', permissionReason: 'manifest-allow'};
  extras.push(request, receipt);
}
burstRows.splice(4, 0, ...extras);
assert.deepEqual(verifyProductReplacement(text(burstRows), 'burst').requestDispositions,
  {admitted: 1, duplicate: 1, busy: 2, fenced: 0});
// Correlation counters restart in a new JVM; generation is part of identity.
const reusedRows = structuredClone(burstRows);
for (const row of reusedRows) {
  if (row.payload?.sidecarGeneration === 2 && row.payload.envelope?.correlationId === 'query') {
    row.payload.envelope.correlationId = 'extra-0';
  }
}
assert.deepEqual(verifyProductReplacement(text(reusedRows), 'burst').requestDispositions,
  {admitted: 1, duplicate: 1, busy: 2, fenced: 0});
const duplicateReceiptRows = structuredClone(reusedRows);
duplicateReceiptRows.push(structuredClone(reusedRows.find(row => row.type === 'capabilityResult'
  && row.payload.sidecarGeneration === 1 && row.payload.envelope.correlationId === 'extra-0')));
assert.throws(() => verifyProductReplacement(text(duplicateReceiptRows), 'burst'),
  'two dispositions within the same generation remain invalid');
const fencedRows = structuredClone(burstRows);
for (let i = 0; i < 3; i++) {
  const index = fencedRows.findIndex(r => r.type === 'capabilityResult' && r.payload.envelope.correlationId === `extra-${i}`);
  fencedRows[index] = {type: 'staleCapabilityCommandDropped', payload: {...scope,
    correlationId: `extra-${i}`, expectedGeneration: 1, reason: 'sidecar-generation-unavailable', command: 'runtime.replacement.request'}};
}
assert.equal(verifyProductReplacement(text(fencedRows), 'burst').requestDispositions.fenced, 3);
let burstNegatives = 0;
for (let i = 0; i < burstRows.length; i++) {
  const r = structuredClone(burstRows); r.splice(i, 1);
  assert.throws(() => verifyProductReplacement(text(r), 'burst')); burstNegatives++;
}
for (const mutate of [
  r => { r[5].payload.envelope.payload.duplicate = false; },
  r => { r[5].payload.envelope.payload.operationId = 'other'; },
  r => { r[7].payload.envelope.payload.errorCode = 'invalid-candidate'; },
  r => { r[4].payload.envelope.sessionId = 'other'; },
  r => { r[7].payload.sidecarGeneration = 2; },
  r => { r.push(structuredClone(r[3])); }
]) {
  const r = structuredClone(burstRows); mutate(r);
  assert.throws(() => verifyProductReplacement(text(r), 'burst')); burstNegatives++;
}
console.log(`Request burst verifier passed: two valid synthetic schedules and ${burstNegatives} negatives`);
const conflictRows = structuredClone(rows);
const conflictRequest = structuredClone(rows[1]);
conflictRequest.payload.sidecarGeneration = 2;
conflictRequest.payload.envelope.correlationId = 'conflict';
const conflictReceipt = {type: 'capabilityResult', payload: {sidecarGeneration: 2,
  envelope: {...scope, correlationId: 'conflict', payload: {success: false,
    errorCode: 'request-conflict', permissionDecision: 'allowed', permissionReason: 'manifest-allow'}}}};
const conflictQuery = structuredClone(rows[8]);
conflictQuery.payload.envelope.correlationId = 'after-conflict';
const conflictStatus = structuredClone(rows[9]);
conflictStatus.payload.envelope.correlationId = 'after-conflict';
conflictRows.splice(11, 0, conflictRequest, conflictReceipt,
  {type: 'standardBundle.productGenerationConflict', payload: {errorCode: 'request-conflict',
    observedInDom: true, recoveryGeneration: 1}}, conflictQuery, conflictStatus,
  {type: 'standardBundle.productConflictStatus', payload: {status: 'replaced', observedInDom: true}});
assert.equal(verifyProductReplacement(text(conflictRows), 'generation-conflict').terminalPreserved, true);
let conflictNegatives = 0;
for (let i = 0; i < conflictRows.length; i++) {
  const changed = structuredClone(conflictRows); changed.splice(i, 1);
  assert.throws(() => verifyProductReplacement(text(changed), 'generation-conflict')); conflictNegatives++;
}
for (const mutate of [
  r => { r[12].payload.envelope.payload.permissionDecision = 'denied'; },
  r => { r[12].payload.envelope.payload.errorCode = 'busy'; },
  r => { r[11].payload.envelope.payload.args.requestId = 'fresh'; },
  r => { r[15].payload.envelope.payload.operationId = 'changed'; },
  r => { r[15].payload.envelope.windowId = 'foreign'; },
  r => { r.push({type: 'sidecar.started', payload: {generation: 3}}); }
]) {
  const changed = structuredClone(conflictRows); mutate(changed);
  assert.throws(() => verifyProductReplacement(text(changed), 'generation-conflict')); conflictNegatives++;
}
console.log(`Cross-generation request conflict verifier passed (synthetic, ${conflictNegatives} negatives)`);
const isolationRows = structuredClone(rows);
const foreignScope = {windowId: 'runtime-observer', sessionId: 'foreign-session', uiId: 'foreign-ui'};
const foreignQuery = structuredClone(rows[8]);
Object.assign(foreignQuery.payload.envelope, foreignScope, {correlationId: 'foreign-query'});
Object.assign(foreignQuery, {direction: 'recv'});
Object.assign(foreignQuery.payload, {source: 'java-sidecar', transport: 'flow-ipc'});
const foreignReply = {type: 'capabilityResult', payload: {sidecarGeneration: 2,
  envelope: {...scope, ...foreignScope, correlationId: 'foreign-query', payload: {
    success: true, status: 'none', permissionDecision: 'allowed', permissionReason: 'manifest-allow'}}}};
const ownerQuery = structuredClone(rows[8]); ownerQuery.payload.envelope.correlationId = 'owner-requery';
const ownerReply = structuredClone(rows[9]); ownerReply.payload.envelope.correlationId = 'owner-requery';
isolationRows.splice(11, 0,
  {type: 'window.create', payload: foreignScope}, foreignQuery, foreignReply,
  {type: 'standardBundle.runtimeObserverIsolation', payload: {...foreignScope, status: 'none', observedInDom: true}},
  ownerQuery, ownerReply,
  {type: 'standardBundle.runtimeOwnerPreserved', payload: {status: 'replaced', observedInDom: true}},
  {type: 'windowClosed', payload: foreignScope});
assert.equal(verifyProductReplacement(text(isolationRows), 'owner-isolation').foreignOwnerStatus, 'none');
let isolationNegatives = 0;
for (let i = 0; i < isolationRows.length; i++) {
  const changed = structuredClone(isolationRows); changed.splice(i, 1);
  assert.throws(() => verifyProductReplacement(text(changed), 'owner-isolation')); isolationNegatives++;
}
for (const mutate of [
  r => { r[13].payload.envelope.payload.operationId = operationId; },
  r => { r[13].payload.envelope.payload.permissionDecision = 'denied'; },
  r => { r[13].payload.envelope.sessionId = scope.sessionId; },
  r => { r[16].payload.envelope.payload.operationId = 'changed'; },
  r => { r[18].payload.sessionId = 'different-owner'; },
  r => { r.push({type: 'sidecar.started'}); }
]) {
  const changed = structuredClone(isolationRows); mutate(changed);
  assert.throws(() => verifyProductReplacement(text(changed), 'owner-isolation')); isolationNegatives++;
}
console.log(`Runtime owner isolation verifier passed (synthetic, ${isolationNegatives} negatives)`);
const reopenRows = structuredClone(rows);
for (const index of [1, 2, 8, 9]) Object.assign(reopenRows[index].payload.envelope, foreignScope);
Object.assign(reopenRows[10].payload, foreignScope);
reopenRows.splice(1, 0, {type: 'window.create', payload: foreignScope});
const freshScope = {...foreignScope, sessionId: 'reopened-session'};
const freshQuery = structuredClone(foreignQuery);
Object.assign(freshQuery.payload.envelope, freshScope, {correlationId: 'fresh-query'});
const freshReply = structuredClone(foreignReply);
Object.assign(freshReply.payload.envelope, freshScope, {correlationId: 'fresh-query'});
const mainQuery = structuredClone(ownerQuery);
Object.assign(mainQuery, {direction: 'recv'}); mainQuery.payload.source = 'java-sidecar';
const mainReply = structuredClone(ownerReply);
mainReply.payload.envelope.payload = structuredClone(foreignReply.payload.envelope.payload);
reopenRows.splice(reopenRows.length - 1, 0,
  {type: 'standardBundle.runtimeContextCaptured', payload: foreignScope},
  {type: 'windowClosed', payload: foreignScope},
  {type: 'window.create', payload: freshScope}, freshQuery, freshReply,
  {type: 'standardBundle.runtimeObserverIsolation', payload: {...freshScope, status: 'none', observedInDom: true}},
  mainQuery, mainReply,
  {type: 'standardBundle.runtimeOwnerPreserved', payload: {status: 'none', observedInDom: true}},
  {type: 'standardBundle.runtimeStaleContext', payload: {
    result: 'query=true request=true background=true client=true', observedInDom: true,
    retiredSessionId: foreignScope.sessionId}},
  {type: 'windowClosed', payload: freshScope});
assert.equal(verifyProductReplacement(text(reopenRows), 'owner-reopen').freshSession, true);
let reopenNegatives = 0;
for (let i = 0; i < reopenRows.length; i++) {
  const changed = structuredClone(reopenRows); changed.splice(i, 1);
  assert.throws(() => verifyProductReplacement(text(changed), 'owner-reopen')); reopenNegatives++;
}
for (const mutate of [
  r => { r.find(x => x.type === 'window.create' && x.payload.sessionId === freshScope.sessionId).payload.sessionId = foreignScope.sessionId; },
  r => { r.find(x => x.type === 'capabilityResult' && x.payload.envelope.correlationId === 'fresh-query').payload.envelope.payload.operationId = operationId; },
  r => { r.find(x => x.type === 'capabilityResult' && x.payload.envelope.correlationId === 'fresh-query').payload.envelope.payload.permissionDecision = 'denied'; },
  r => { r.push({type: 'sidecar.started'}); }
]) {
  const changed = structuredClone(reopenRows); mutate(changed);
  assert.throws(() => verifyProductReplacement(text(changed), 'owner-reopen')); reopenNegatives++;
}
console.log(`Runtime record-owner reopen verifier passed (synthetic, ${reopenNegatives} negatives)`);
for (const mutate of [
  r => { r.find(x => x.type === 'standardBundle.runtimeContextCaptured').payload = freshScope; },
  r => { r.find(x => x.type === 'standardBundle.runtimeStaleContext').payload.retiredSessionId = freshScope.sessionId; },
  r => { r.find(x => x.type === 'standardBundle.runtimeStaleContext').payload.observedInDom = false; },
  ...['query', 'request', 'background', 'client'].map(field => r => {
    const p = r.find(x => x.type === 'standardBundle.runtimeStaleContext').payload;
    p.result = p.result.replace(`${field}=true`, `${field}=false`);
  }),
  r => { const i = r.findIndex(x => x.type === 'standardBundle.runtimeStaleContext');
    const [event] = r.splice(i, 1); r.splice(1, 0, event); },
  r => { r.push({type: 'command', payload: {envelope: {payload: {requestId: 'stale-owner-probe'}}}}); }
]) {
  const changed = structuredClone(reopenRows); mutate(changed);
  assert.throws(() => verifyProductReplacement(text(changed), 'owner-reopen'));
}
console.log('Stale runtime context verifier passed (synthetic, 9 additional negatives)');

const activeCloseRows = structuredClone(blockedRows);
for (const row of activeCloseRows) {
  if (['command', 'capabilityResult'].includes(row.type) && row.payload.sidecarGeneration === 1)
    Object.assign(row.payload.envelope, foreignScope);
  if (row.type === 'capabilityResult' && row.payload.sidecarGeneration === 2) {
    const p = row.payload.envelope.payload;
    p.status = 'none';
    for (const key of ['operationId', 'requestId', 'candidateId', 'requestingGeneration']) delete p[key];
  }
  if (row.type === 'standardBundle.productReplacement') row.payload.status = 'none';
}
activeCloseRows.splice(1, 0, {type: 'window.create', payload: foreignScope});
activeCloseRows.splice(activeCloseRows.findIndex(r => r.type === 'sidecar.replacementEscalated'), 0,
  {type: 'standardBundle.activeRuntimeOwnerClose', payload: foreignScope},
  {type: 'window.closeRequested', payload: foreignScope},
  {type: 'windowClosedDropped', payload: {...foreignScope, reason: 'sidecar-lifecycle-unavailable'}});
verifyProductReplacement(blockedText(activeCloseRows), 'owner-active-close');
let activeCloseNegatives = 0;
for (let i = 0; i < activeCloseRows.length; i++) {
  const changed = structuredClone(activeCloseRows); changed.splice(i, 1);
  assert.throws(() => verifyProductReplacement(blockedText(changed), 'owner-active-close'));
  activeCloseNegatives++;
}
for (const mutate of [
  r => { const i = r.findIndex(x => x.type === 'window.closeRequested'); const [closed] = r.splice(i, 1); r.push(closed); },
  r => { r.find(x => x.type === 'window.closeRequested').payload.sessionId = 'wrong-owner'; },
  r => { r.find(x => x.type === 'windowClosedDropped').payload.reason = 'unknown'; },
  r => { r.push({type: 'windowClosed', payload: foreignScope}); },
  r => { r.find(x => x.type === 'capabilityResult' && x.payload.sidecarGeneration === 2).payload.envelope.payload.operationId = operationId; },
  r => { r.find(x => x.type === 'standardBundle.productReplacement').payload.status = 'replaced'; },
  r => { r.push({type: 'sidecar.started', payload: {generation: 3}}); }
]) {
  const changed = structuredClone(activeCloseRows); mutate(changed);
  assert.throws(() => verifyProductReplacement(blockedText(changed), 'owner-active-close'));
  activeCloseNegatives++;
}
console.log(`Active owner close verifier passed (synthetic, ${activeCloseNegatives} negatives)`);
