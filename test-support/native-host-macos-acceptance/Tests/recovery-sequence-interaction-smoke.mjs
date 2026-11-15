import assert from 'node:assert/strict';
import {verifyRecoveryInteractions} from './recovery-sequence-interaction-verify.mjs';
const rows = [], scope = {windowId: 'main', sessionId: 'session', uiId: 'main'};
const add = (type, payload) => rows.push({type, payload});
for (let i = 0; i < 4; i++) {
  if (i) {
    const p = {attempt: i, previousProcessIdentifier: 100 + i - 1};
    add('sidecar.recoveryScheduled', p); add('sidecar.recoveryStarted', p);
  }
  add('sidecar.started', {generation: i + 1, processIdentifier: 100 + i});
  if (i) add('sidecar.recoveryReady', {attempt: i, previousProcessIdentifier: 99 + i,
    processIdentifier: 100 + i, processReplaced: true, recoveryGeneration: i, windowsReloaded: 1});
  const frame = {...scope, runId: 'run', mainWindowNumber: 5, mainContextRetained: true,
    mainWindowRetained: true, mainWebViewRetained: true, browserDocumentRetained: true,
    recoveryGeneration: i, processIdentifier: 100 + i, documentMarker: `java-${i}`, browserDocumentMarker: `browser-${i}`};
  add('flowRecoveryTermination.nativePointerDispatch', {...frame, inputMode: 'appkit-window-pointer-event',
    activeApplication: true, keyWindow: true, requestBaseline: 0, responseBaseline: 0});
  function wire(type, send, body) {
    rows.push({type, direction: send ? 'send' : 'recv', payload: {transport: 'flow-ipc',
      [send ? 'target' : 'source']: 'java-sidecar', sidecarGeneration: i + 1,
      envelope: {...scope, version: 1, type, correlationId: 'request', payload: body}}});
  }
  wire('opaqueHttpStreamOpen', true, {requestId: 'request', method: 'POST'});
  wire('opaqueHttpStreamResponse', false, {requestId: 'request', status: 200});
  wire('opaqueHttpStreamResponseEnd', false, {requestId: 'request'});
  wire('opaquePush', false, {connectionId: 'connection', pushSequence: 1, flowServerSyncId: 2, uidlBytes: 20});
  wire('opaquePushAck', true, {connectionId: 'connection', pushSequence: 1, lastSeenServerSyncId: 2});
  add('flowRecoveryTermination.stageObserved', {...frame, counter: 1, pushCount: 1,
    nativePointerReceiptCount: 1, nativePointerTrustedCount: 1, client: true,
    backgroundSessionAbsent: true, balancedDrainObserved: true, requestCount: 1, responseCount: 1, pushMessageCount: 1});
  if (i < 3) add('flowRecoverySequence.failureInjected', {...frame, signal: 9, targetProcessIdentifier: 100 + i, interactionSettled: true});
  else add('flowRecoverySequence.frameworkAcceptance', {...frame, complete: true, recoveries: 3, settledStages: 4});
  add('sidecar.exit', {generation: i + 1, processIdentifier: 100 + i, expected: i === 3, status: i === 3 ? 0 : 9});
}
const text = r => r.map(x => 'HOST_PROTOCOL ' + JSON.stringify(x)).join('\n');
assert.equal(verifyRecoveryInteractions(text(rows)).settledNativeInteractions, 4);
assert.throws(() => verifyRecoveryInteractions(text(rows), {passiveActivation: true}));
const passive = structuredClone(rows);
for (const row of passive.filter(r => r.type === 'flowRecoveryTermination.nativePointerDispatch')) {
  row.payload.activationMode = 'passive'; row.payload.webViewOwnsResponder = true;
}
assert.equal(verifyRecoveryInteractions(text(passive), {passiveActivation: true}).settledNativeInteractions, 4);
for (const [field, value] of [['activationMode', 'assisted'], ['webViewOwnsResponder', false]]) {
  const changed = structuredClone(passive);
  changed.find(r => r.type === 'flowRecoveryTermination.nativePointerDispatch').payload[field] = value;
  assert.throws(() => verifyRecoveryInteractions(text(changed), {passiveActivation: true}));
}
let negatives = 0;
for (let i = 0; i < rows.length; i++) {
  const changed = structuredClone(rows); changed.splice(i, 1);
  assert.throws(() => verifyRecoveryInteractions(text(changed))); negatives++;
}
for (const [type, mutate] of [
  ['opaquePushAck', p => { p.envelope.payload.lastSeenServerSyncId = 1; }],
  ['opaquePush', p => { p.sidecarGeneration = 4; }],
  ['opaqueHttpStreamResponse', p => { p.envelope.sessionId = 'foreign'; }],
  ['flowRecoveryTermination.nativePointerDispatch', p => { p.keyWindow = false; }],
  ['flowRecoveryTermination.stageObserved', p => { p.nativePointerTrustedCount = 0; }],
  ['flowRecoverySequence.failureInjected', p => { p.targetProcessIdentifier = 999; }],
  ['flowRecoverySequence.frameworkAcceptance', p => { p.complete = false; }]
]) {
  const changed = structuredClone(rows); mutate(changed.find(r => r.type === type).payload);
  assert.throws(() => verifyRecoveryInteractions(text(changed))); negatives++;
}
console.log(`Recovery sequence interaction verifier passed (synthetic, ${negatives} negatives)`);
