import fs from 'node:fs';
import assert from 'node:assert/strict';
import {pathToFileURL} from 'node:url';
import {verifyRecoverySequence} from './recovery-sequence-process-verify.mjs';

export function verifyRecoveryInteractions(text, {passiveActivation = false} = {}) {
  verifyRecoverySequence(text);
  const rows = text.split('\n').filter(l => l.startsWith('HOST_PROTOCOL '))
    .map(l => JSON.parse(l.slice(14)));
  const all = type => rows.filter(r => r.type === type);
  const starts = all('sidecar.started'), exits = all('sidecar.exit');
  const pointers = all('flowRecoveryTermination.nativePointerDispatch');
  const stages = all('flowRecoveryTermination.stageObserved');
  const faults = all('flowRecoverySequence.failureInjected');
  const finals = all('flowRecoverySequence.frameworkAcceptance');
  assert.equal(pointers.length, 4); assert.equal(stages.length, 4);
  assert.equal(faults.length, 3); assert.equal(finals.length, 1);
  assert.equal(all('error').length, 0);
  const final = finals[0];
  assert.equal(final.payload.complete, true); assert.equal(final.payload.recoveries, 3);
  assert.equal(final.payload.settledStages, 4);
  const before = (a, b) => assert.ok(rows.indexOf(a) < rows.indexOf(b), 'ordered interaction');
  const identity = stages[0].payload;
  for (const key of ['runId', 'documentMarker', 'browserDocumentMarker']) assert.ok(identity[key]);
  for (const key of ['documentMarker', 'browserDocumentMarker'])
    assert.equal(new Set(stages.map(r => r.payload[key])).size, 4);
  for (let i = 0; i < 4; i++) {
    const pointer = pointers[i], stage = stages[i], end = faults[i] ?? final;
    const p = pointer.payload, s = stage.payload;
    for (const event of [pointer, stage, end]) {
      for (const key of ['runId', 'windowId', 'sessionId', 'uiId', 'mainWindowNumber'])
        assert.equal(event.payload[key], identity[key]);
      for (const key of ['mainContextRetained', 'mainWindowRetained', 'mainWebViewRetained', 'browserDocumentRetained'])
        assert.equal(event.payload[key], true);
      assert.equal(event.payload.recoveryGeneration, i);
      assert.equal(event.payload.processIdentifier, starts[i].payload.processIdentifier);
      for (const key of ['documentMarker', 'browserDocumentMarker']) assert.equal(event.payload[key], s[key]);
    }
    assert.equal(p.inputMode, 'appkit-window-pointer-event');
    assert.equal(p.activeApplication, true); assert.equal(p.keyWindow, true);
    if (passiveActivation) {
      assert.equal(p.activationMode, 'passive', 'D01 cannot use activation assistance');
      assert.equal(p.webViewOwnsResponder, true, 'product must own responder before input');
    }
    for (const key of ['counter', 'pushCount', 'nativePointerReceiptCount', 'nativePointerTrustedCount']) assert.equal(s[key], 1);
    for (const key of ['client', 'backgroundSessionAbsent', 'balancedDrainObserved']) assert.equal(s[key], true);
    before(starts[i], pointer); before(pointer, stage); before(stage, end); before(end, exits[i]);
    if (i < 3) {
      assert.equal(end.payload.signal, 9);
      assert.equal(end.payload.targetProcessIdentifier, s.processIdentifier);
      assert.equal(end.payload.interactionSettled, true);
    }
    const epoch = rows.slice(rows.indexOf(starts[i]), rows.indexOf(end));
    const wire = type => epoch.filter(r => r.type === type);
    const opens = wire('opaqueHttpStreamOpen'), responses = wire('opaqueHttpStreamResponse'), ends = wire('opaqueHttpStreamResponseEnd');
    const pushes = wire('opaquePush'), acks = wire('opaquePushAck');
    for (const event of [...opens, ...responses, ...ends, ...pushes, ...acks]) {
      const send = ['opaqueHttpStreamOpen', 'opaquePushAck'].includes(event.type);
      assert.equal(event.direction, send ? 'send' : 'recv');
      assert.equal(event.payload.transport, 'flow-ipc');
      assert.equal(event.payload[send ? 'target' : 'source'], 'java-sidecar');
      if (!send) assert.equal(event.payload.sidecarGeneration, i + 1);
      const e = event.payload.envelope;
      assert.equal(e.version, 1); assert.equal(e.type, event.type);
      for (const key of ['windowId', 'sessionId', 'uiId']) assert.equal(e[key], s[key]);
    }
    assert.ok(opens.length > 0); assert.equal(opens.length, responses.length); assert.equal(opens.length, ends.length);
    assert.equal(s.requestCount, opens.length); assert.equal(s.responseCount, ends.length);
    assert.equal(p.requestBaseline, opens.filter(r => rows.indexOf(r) < rows.indexOf(pointer)).length);
    assert.equal(p.responseBaseline, ends.filter(r => rows.indexOf(r) < rows.indexOf(pointer)).length);
    assert.ok(opens.some(r => rows.indexOf(r) > rows.indexOf(pointer) && r.payload.envelope.payload.method === 'POST'));
    const body = r => r.payload.envelope.payload;
    assert.equal(new Set(opens.map(r => body(r).requestId)).size, opens.length);
    for (const open of opens) {
      const id = body(open).requestId; assert.ok(id);
      const rs = responses.filter(r => body(r).requestId === id), es = ends.filter(r => body(r).requestId === id);
      assert.equal(rs.length, 1); assert.equal(es.length, 1);
      assert.ok(body(rs[0]).status >= 200 && body(rs[0]).status < 400);
      for (const r of [open, rs[0], es[0]]) assert.equal(r.payload.envelope.correlationId, id);
      before(open, rs[0]); before(rs[0], es[0]); before(es[0], stage);
    }
    const key = r => `${body(r).connectionId}/${body(r).pushSequence}`;
    assert.equal(s.pushMessageCount, new Set(pushes.map(key)).size);
    assert.ok(pushes.some(r => rows.indexOf(r) > rows.indexOf(pointer)));
    assert.equal(pushes.length, acks.length);
    for (const pushKey of new Set(pushes.map(key))) {
      const attempts = pushes.filter(r => key(r) === pushKey);
      const receipts = acks.filter(r => key(r) === pushKey);
      assert.equal(receipts.length, attempts.length);
      attempts.forEach((attempt, n) => before(attempt, receipts[n]));
    }
    for (const push of pushes) {
      const matches = acks.filter(r => key(r) === key(push) && rows.indexOf(r) > rows.indexOf(push));
      assert.ok(matches.some(r => Number.isInteger(body(r).lastSeenServerSyncId)
        && body(r).lastSeenServerSyncId >= body(push).flowServerSyncId));
      assert.ok(body(push).flowServerSyncId >= 0 && body(push).uidlBytes > 0);
      before(push, stage);
      assert.ok(matches.some(r => rows.indexOf(r) < rows.indexOf(stage)), 'consuming ACK precedes settled receipt');
    }
  }
  return {recoveries: 3, settledNativeInteractions: 4, physicalInput: false};
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  assert(process.argv.length === 3 || (process.argv.length === 4 && process.argv[3] === '--passive-activation'));
  console.log('PASS recovery sequence interactions', verifyRecoveryInteractions(fs.readFileSync(process.argv[2], 'utf8'),
    {passiveActivation: process.argv[3] === '--passive-activation'}));
}
