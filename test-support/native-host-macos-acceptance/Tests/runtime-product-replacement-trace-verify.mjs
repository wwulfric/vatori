import fs from 'node:fs';
import assert from 'node:assert/strict';
import { pathToFileURL } from 'node:url';

export function verifyProductReplacement(text, mode = 'success') {
  assert.ok(['success', 'rollback', 'rejection', 'blocked-retirement', 'blocked-candidate', 'burst', 'generation-conflict', 'owner-isolation', 'owner-reopen', 'owner-active-close'].includes(mode), 'known product profile required');
  const activeOwnerClose = mode === 'owner-active-close';
  const blockedCandidate = mode === 'blocked-candidate';
  const rollback = mode === 'rollback' || blockedCandidate;
  const finalGeneration = rollback ? 3 : 2;
  const expectedOutcome = rollback ? 'rolled-back' : 'replaced';
  const rows = text.split('\n').filter(line => line.startsWith('HOST_PROTOCOL '))
    .map(line => JSON.parse(line.slice(14)));
  if (mode === 'burst') return verifyRequestBurst(rows);
  if (mode === 'generation-conflict') return verifyGenerationConflict(rows);
  if (mode === 'owner-isolation') return verifyOwnerIsolation(rows);
  if (mode === 'owner-reopen') return verifyOwnerReopen(rows);
  const one = (type, predicate = () => true) => {
    const found = rows.map((row, index) => ({row, index})).filter(({row}) => row.type === type && predicate(row));
    assert.equal(found.length, 1, `one ${type} expected`); return found[0];
  };
  const request = one('command', row => row.payload?.envelope?.payload?.command === 'runtime.replacement.request');
  assert.equal(request.row.payload.transport, 'flow-ipc');
  assert.equal(request.row.direction, 'recv');
  assert.equal(request.row.payload.source, 'java-sidecar');
  assert.equal(request.row.payload.sidecarGeneration, 1);
  const envelope = request.row.payload.envelope;
  assert.equal(envelope.version, 1);
  for (const field of ['correlationId', 'windowId', 'sessionId', 'uiId']) assert.ok(envelope[field]);
  assert.equal(envelope.payload.args.candidateId, 'next');
  if (mode === 'rejection') {
    const initial = one('sidecar.started');
    assert.equal(initial.row.payload.generation, 1);
    assert.ok(initial.row.payload.processIdentifier > 0 && initial.index < request.index);
    const denied = one('capabilityResult', row => row.payload?.envelope?.correlationId === envelope.correlationId);
    assert.equal(denied.row.payload.sidecarGeneration, 1);
    const payload = denied.row.payload.envelope.payload;
    assert.equal(payload.success, false);
    assert.equal(payload.errorCode, 'invalid-candidate');
    assert.equal(payload.permissionDecision, 'allowed');
    assert.equal(payload.permissionReason, 'manifest-allow');
    for (const field of ['windowId', 'sessionId', 'uiId'])
      assert.equal(denied.row.payload.envelope[field], envelope[field]);
    const dom = one('standardBundle.productReplacementRejected');
    assert.equal(dom.row.payload.errorCode, 'invalid-candidate');
    assert.equal(dom.row.payload.observedInDom, true);
    assert.equal(dom.row.payload.recoveryGeneration, 0);
    assert.ok(request.index < denied.index && denied.index < dom.index);
    const query = one('command', row => rows.indexOf(row) > dom.index
      && row.payload?.envelope?.payload?.command === 'runtime.replacement.status');
    assert.equal(query.row.payload.sidecarGeneration, 1);
    const result = one('capabilityResult', row => row.payload?.envelope?.correlationId === query.row.payload.envelope.correlationId);
    assert.equal(result.row.payload.sidecarGeneration, 1);
    for (const field of ['windowId', 'sessionId', 'uiId']) {
      assert.equal(query.row.payload.envelope[field], envelope[field]);
      assert.equal(result.row.payload.envelope[field], envelope[field]);
    }
    assert.equal(result.row.payload.envelope.payload.success, true);
    assert.equal(result.row.payload.envelope.payload.status, 'none');
    assert.equal(result.row.payload.envelope.payload.permissionDecision, 'allowed');
    assert.equal(result.row.payload.envelope.payload.operationId, undefined);
    const statusDom = one('standardBundle.productRejectionStatus');
    assert.equal(statusDom.row.payload.status, 'none');
    assert.equal(statusDom.row.payload.observedInDom, true);
    assert.ok(query.index < result.index && result.index < statusDom.index);
    assert.equal(rows.some(row => /^sidecar\.(productReplacement|replacement|rollback|recoveryScheduled)/.test(row.type)), false,
      'rejected candidate must not admit, retire, replace or recover');
    const exit = one('sidecar.exit');
    assert.equal(exit.row.payload.generation, 1);
    assert.equal(exit.row.payload.processIdentifier, initial.row.payload.processIdentifier);
    assert.equal(exit.row.payload.expected, true);
    assert.equal(exit.row.payload.status, 0);
    assert.ok(exit.index > statusDom.index);
    return {oldPid: initial.row.payload.processIdentifier, outcome: 'invalid-candidate', processUnchanged: true};
  }
  const accepted = one('sidecar.productReplacementRequested');
  assert.equal(accepted.row.payload.requestId, envelope.payload.args.requestId);
  assert.equal(accepted.row.payload.candidateId, 'next');
  assert.equal(accepted.row.payload.requestingGeneration, 1);
  assert.ok(accepted.index > request.index);
  const receipt = one('capabilityResult', row => row.payload?.envelope?.correlationId === envelope.correlationId
    && row.payload?.sidecarGeneration === 1);
  assert.equal(receipt.row.payload.envelope.payload.permissionDecision, 'allowed');
  assert.equal(receipt.row.payload.envelope.payload.permissionReason, 'manifest-allow');
  assert.equal(receipt.row.payload.envelope.payload.status, 'pending');
  assert.equal(receipt.row.payload.envelope.payload.operationId, accepted.row.payload.operationId);
  for (const field of ['windowId', 'sessionId', 'uiId'])
    assert.equal(receipt.row.payload.envelope[field], envelope[field]);
  const initial = one('sidecar.started', row => row.payload?.generation === 1);
  const oldExit = one('sidecar.exit', row => row.payload?.generation === 1);
  const next = one('sidecar.started', row => row.payload?.generation === 2);
  const launch = one('launch.sidecar', row => row.payload?.generation === 2);
  assert.ok(launch.row.payload.command.endsWith(' -jar $APP/Contents/Resources/RuntimeCandidates/next/application.jar'));
  assert.equal(oldExit.row.payload.processIdentifier, initial.row.payload.processIdentifier);
  assert.equal(oldExit.row.payload.expected, false);
  assert.equal(accepted.row.payload.processIdentifier, initial.row.payload.processIdentifier);
  assert.ok(next.row.payload.processIdentifier > 0);
  assert.notEqual(next.row.payload.processIdentifier, initial.row.payload.processIdentifier);
  assert.ok(accepted.index < oldExit.index && oldExit.index < next.index);
  assert.ok(initial.index < request.index && oldExit.index < launch.index && launch.index < next.index);
  assert.ok(receipt.index > request.index && receipt.index < oldExit.index);
  if (mode === 'blocked-retirement' || activeOwnerClose) {
    assert.ok(text.includes('Test initial JVM shutdown hook blocked'), 'real shutdown hook entered');
    assert.equal(rows.filter(row => row.type === 'sidecar.started').length, 2);
    assert.equal(rows.filter(row => row.type === 'sidecar.exit').length, 2);
    const terminated = one('sidecar.replacementTerminated');
    const escalated = one('sidecar.replacementEscalated');
    for (const event of [terminated, escalated]) {
      assert.equal(event.row.payload.generation, 1);
      assert.equal(event.row.payload.processIdentifier, initial.row.payload.processIdentifier);
      assert.equal(event.row.payload.reason, 'product-replacement');
    }
    assert.equal(escalated.row.payload.graceSeconds, 1);
    assert.equal(oldExit.row.payload.status, 9);
    assert.ok(accepted.index < terminated.index && terminated.index < escalated.index && escalated.index < oldExit.index);
    const elapsed = escalated.row.at - terminated.row.at;
    assert.ok(Number.isFinite(elapsed) && elapsed >= 900 && elapsed < 5000, 'bounded one-second escalation');
    const exitDelay = oldExit.row.at - escalated.row.at;
    assert.ok(Number.isFinite(exitDelay) && exitDelay >= 0 && exitDelay < 2000, 'actual exit promptly confirmed');
    assert.equal(rows.some(row => ['sidecar.recoveryScheduled', 'sidecar.replacementEscalationFailed',
      'sidecar.replacementRetirementFailed', 'host.failureShutdownCompleted'].includes(row.type)), false);
  }
  let restored = next;
  if (rollback) {
    const candidateExit = one('sidecar.exit', row => row.payload?.generation === 2);
    assert.equal(candidateExit.row.payload.processIdentifier, next.row.payload.processIdentifier);
    assert.equal(candidateExit.row.payload.status, blockedCandidate ? 9 : 42);
    assert.equal(candidateExit.row.payload.expected, false);
    const failed = one('sidecar.replacementFailed');
    assert.equal(failed.row.payload.requestId, accepted.row.payload.operationId);
    assert.equal(failed.row.payload.cause, blockedCandidate ? 'candidate-ready-timeout' : 'candidate-exited-before-ready');
    if (blockedCandidate) {
      assert.ok(text.includes('Test candidate deliberately never ready'));
      assert.ok(text.includes('Test candidate shutdown hook blocked'));
      assert.equal(rows.filter(row => row.type === 'sidecar.started').length, 3);
      assert.equal(rows.filter(row => row.type === 'sidecar.exit').length, 3);
      const timeout = one('sidecar.replacementReadyTimeout');
      assert.equal(timeout.row.payload.requestId, accepted.row.payload.operationId);
      assert.equal(timeout.row.payload.processIdentifier, next.row.payload.processIdentifier);
      assert.equal(timeout.row.payload.timeoutSeconds, 15);
      const readyWait = timeout.row.at - next.row.at;
      assert.ok(Number.isFinite(readyWait) && readyWait >= 14500 && readyWait < 20000);
      const terminated = one('sidecar.replacementTerminated', row => row.payload.generation === 2);
      const escalated = one('sidecar.replacementEscalated');
      for (const event of [terminated, escalated]) {
        assert.equal(event.row.payload.generation, 2);
        assert.equal(event.row.payload.processIdentifier, next.row.payload.processIdentifier);
        assert.equal(event.row.payload.reason, 'candidate-ready-timeout');
      }
      assert.equal(escalated.row.payload.graceSeconds, 1);
      assert.ok(next.index < timeout.index && timeout.index < failed.index && failed.index < terminated.index
        && terminated.index < escalated.index && escalated.index < candidateExit.index);
      const escalationWait = escalated.row.at - terminated.row.at;
      assert.ok(Number.isFinite(escalationWait) && escalationWait >= 900 && escalationWait < 5000);
      const exitWait = candidateExit.row.at - escalated.row.at;
      assert.ok(Number.isFinite(exitWait) && exitWait >= 0 && exitWait < 2000);
    }
    const stableLaunch = one('launch.sidecar', row => row.payload?.generation === 3);
    assert.ok(stableLaunch.row.payload.command.endsWith(' -jar $APP/Contents/Resources/Sidecar/application.jar'));
    restored = one('sidecar.started', row => row.payload?.generation === 3);
    assert.ok(next.index < candidateExit.index && candidateExit.index < stableLaunch.index
      && stableLaunch.index < restored.index);
    assert.notEqual(restored.row.payload.processIdentifier, next.row.payload.processIdentifier);
    assert.notEqual(restored.row.payload.processIdentifier, initial.row.payload.processIdentifier);
    const ready = one('sidecar.rollbackReady');
    assert.equal(ready.row.payload.requestId, accepted.row.payload.operationId);
    assert.equal(ready.row.payload.processIdentifier, restored.row.payload.processIdentifier);
    assert.ok(ready.index > restored.index);
    assert.equal(rows.some(row => row.type === 'sidecar.recoveryScheduled'), false,
      'product rollback must not consume unexpected recovery budget');
  }
  const terminal = one('sidecar.productReplacementResult');
  assert.equal(terminal.row.payload.operationId, accepted.row.payload.operationId);
  assert.equal(terminal.row.payload.status, expectedOutcome);
  assert.ok(terminal.index > restored.index);
  if (activeOwnerClose) {
    assert.equal(envelope.windowId, 'runtime-observer');
    const created = one('window.create', r => r.payload.windowId === envelope.windowId);
    const closing = one('standardBundle.activeRuntimeOwnerClose');
    // window.closeRequested is emitted by the actual NSWindow will-close
    // delegate, not by the automation trigger. The retiring sidecar's fence
    // deliberately refuses delivery of windowClosed at this point.
    const closed = one('window.closeRequested', r => r.payload.windowId === envelope.windowId);
    const dropped = one('windowClosedDropped', r => r.payload.windowId === envelope.windowId);
    assert.equal(dropped.row.payload.reason, 'sidecar-lifecycle-unavailable');
    assert.equal(rows.some(r => r.type === 'windowClosed' && r.payload.windowId === envelope.windowId), false);
    for (const event of [created, closing, closed, dropped]) {
      for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(event.row.payload[field], envelope[field]);
    }
    assert.ok(created.index < request.index && accepted.index < closing.index
      && closing.index < closed.index && closed.index < dropped.index && dropped.index < oldExit.index,
      'requester must really close during old-child retirement, not after completion');
  }
  const query = one('command', row => row.payload?.envelope?.payload?.command === 'runtime.replacement.status'
    && row.payload?.sidecarGeneration === finalGeneration);
  const result = one('capabilityResult', row => row.payload?.sidecarGeneration === finalGeneration
    && row.payload?.envelope?.correlationId === query.row.payload.envelope.correlationId);
  const payload = result.row.payload.envelope.payload;
  assert.equal(payload.status, activeOwnerClose ? 'none' : expectedOutcome);
  assert.equal(payload.success, true);
  assert.equal(payload.mode, 'host-runtime-replacement');
  assert.equal(payload.permissionDecision, 'allowed');
  assert.equal(payload.permissionReason, 'manifest-allow');
  if (activeOwnerClose) {
    assert.equal(query.row.payload.envelope.windowId, 'main');
    for (const key of ['operationId', 'requestId', 'candidateId', 'requestingGeneration']) assert.equal(payload[key], undefined);
  } else {
    assert.equal(payload.operationId, accepted.row.payload.operationId);
    assert.equal(payload.requestingGeneration, 1);
  }
  for (const field of ['windowId', 'sessionId', 'uiId']) {
    if (activeOwnerClose) assert.notEqual(query.row.payload.envelope[field], envelope[field]);
    else assert.equal(query.row.payload.envelope[field], envelope[field]);
    assert.equal(result.row.payload.envelope[field], query.row.payload.envelope[field]);
  }
  assert.ok(query.index > terminal.index && result.index > query.index);
  const dom = one('standardBundle.productReplacement');
  assert.equal(dom.row.payload.status, activeOwnerClose ? 'none' : expectedOutcome);
  assert.equal(dom.row.payload.observedInDom, true);
  assert.ok(dom.row.payload.recoveryGeneration > 0 && dom.index > result.index);
  const finalExit = one('sidecar.exit', row => row.payload?.generation === finalGeneration);
  assert.equal(finalExit.row.payload.processIdentifier, restored.row.payload.processIdentifier);
  assert.equal(finalExit.row.payload.status, 0);
  assert.equal(finalExit.row.payload.expected, true);
  assert.ok(finalExit.index > dom.index);
  return { operationId: accepted.row.payload.operationId, oldPid: initial.row.payload.processIdentifier,
    newPid: restored.row.payload.processIdentifier, outcome: expectedOutcome };
}

function verifyOwnerReopen(rows) {
  const one = predicate => {
    const found = rows.filter(predicate); assert.equal(found.length, 1); return found[0];
  };
  const request = one(r => r.type === 'command' && r.payload?.envelope?.payload?.command === 'runtime.replacement.request');
  const old = request.payload.envelope;
  assert.equal(old.windowId, 'runtime-observer');
  const creates = rows.filter(r => r.type === 'window.create' && r.payload.windowId === old.windowId);
  const closes = rows.filter(r => r.type === 'windowClosed' && r.payload.windowId === old.windowId);
  assert.equal(creates.length, 2); assert.equal(closes.length, 2);
  const fresh = creates[1].payload;
  assert.notEqual(fresh.sessionId, old.sessionId);
  for (const field of ['windowId', 'sessionId', 'uiId']) {
    assert.equal(creates[0].payload[field], old[field]);
    assert.equal(closes[0].payload[field], old[field]);
    assert.equal(closes[1].payload[field], fresh[field]);
  }
  const retainedDom = one(r => r.type === 'standardBundle.productReplacement');
  for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(retainedDom.payload[field], old[field]);
  const captured = one(r => r.type === 'standardBundle.runtimeContextCaptured');
  for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(captured.payload[field], old[field]);
  const stale = one(r => r.type === 'standardBundle.runtimeStaleContext');
  assert.equal(stale.payload.result, 'query=true request=true background=true client=true');
  assert.equal(stale.payload.observedInDom, true);
  assert.equal(stale.payload.retiredSessionId, old.sessionId);
  assert.equal(rows.filter(r => r.type === 'command'
    && r.payload?.envelope?.payload?.requestId === 'stale-owner-probe').length, 0);
  const excluded = new Set();
  const emptyReceipts = [];
  for (const scope of [fresh, {windowId: 'main'}]) {
    const query = one(r => r.type === 'command' && r.payload?.sidecarGeneration === 2
      && r.payload.envelope?.windowId === scope.windowId
      && (!scope.sessionId || r.payload.envelope.sessionId === scope.sessionId)
      && r.payload.envelope.payload.command === 'runtime.replacement.status');
    assert.equal(query.direction, 'recv'); assert.equal(query.payload.source, 'java-sidecar');
    const reply = one(r => r.type === 'capabilityResult' && r.payload?.sidecarGeneration === 2
      && r.payload.envelope?.correlationId === query.payload.envelope.correlationId);
    const p = reply.payload.envelope.payload;
    assert.equal(p.success, true); assert.equal(p.status, 'none');
    assert.equal(p.permissionDecision, 'allowed'); assert.equal(p.permissionReason, 'manifest-allow');
    for (const key of ['operationId', 'requestId', 'candidateId', 'requestingGeneration']) assert.equal(p[key], undefined);
    for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(reply.payload.envelope[field], query.payload.envelope[field]);
    assert.ok(rows.indexOf(reply) > rows.indexOf(query));
    excluded.add(query); excluded.add(reply); emptyReceipts.push(query, reply);
  }
  const dom = one(r => r.type === 'standardBundle.runtimeObserverIsolation');
  assert.equal(dom.payload.status, 'none'); assert.equal(dom.payload.observedInDom, true);
  for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(dom.payload[field], fresh[field]);
  const mainDom = one(r => r.type === 'standardBundle.runtimeOwnerPreserved');
  assert.equal(mainDom.payload.status, 'none'); assert.equal(mainDom.payload.observedInDom, true);
  const ordered = [creates[0], request, retainedDom, captured, closes[0], creates[1],
    ...emptyReceipts.slice(0, 2), dom, ...emptyReceipts.slice(2), mainDom, stale, closes[1]];
  for (let i = 1; i < ordered.length; i++) assert.ok(rows.indexOf(ordered[i]) > rows.indexOf(ordered[i - 1]));
  assert.equal(rows.filter(r => r.type === 'sidecar.started').length, 2);
  assert.equal(rows.filter(r => r.type === 'sidecar.exit').length, 2);
  const base = verifyProductReplacement(rows.filter(r => !excluded.has(r)).map(r => 'HOST_PROTOCOL ' + JSON.stringify(r)).join('\n'));
  return {...base, reopenedOwnerStatus: 'none', freshSession: true, staleRuntimeInvalidated: true};
}

function verifyOwnerIsolation(rows) {
  const one = predicate => {
    const matches = rows.filter(predicate); assert.equal(matches.length, 1); return matches[0];
  };
  const query = one(r => r.type === 'command' && r.payload?.sidecarGeneration === 2
    && r.payload.envelope?.windowId === 'runtime-observer'
    && r.payload.envelope.payload.command === 'runtime.replacement.status');
  const foreign = query.payload.envelope;
  assert.equal(query.direction, 'recv');
  assert.equal(query.payload.source, 'java-sidecar');
  const reply = one(r => r.type === 'capabilityResult' && r.payload?.sidecarGeneration === 2
    && r.payload.envelope?.correlationId === foreign.correlationId);
  const p = reply.payload.envelope.payload;
  assert.equal(p.success, true);
  assert.equal(p.permissionDecision, 'allowed');
  assert.equal(p.permissionReason, 'manifest-allow');
  assert.equal(p.status, 'none');
  for (const key of ['operationId', 'requestId', 'candidateId', 'requestingGeneration']) assert.equal(p[key], undefined);
  const dom = one(r => r.type === 'standardBundle.runtimeObserverIsolation');
  assert.equal(dom.payload.status, 'none'); assert.equal(dom.payload.observedInDom, true);
  const created = one(r => r.type === 'window.create' && r.payload.windowId === foreign.windowId);
  const closed = one(r => r.type === 'windowClosed' && r.payload.windowId === foreign.windowId);
  for (const field of ['windowId', 'sessionId', 'uiId']) {
    assert.ok(foreign[field]);
    assert.equal(reply.payload.envelope[field], foreign[field]);
    assert.equal(dom.payload[field], foreign[field]);
    assert.equal(created.payload[field], foreign[field]);
    assert.equal(closed.payload[field], foreign[field]);
  }
  const ownerQuery = one(r => rows.indexOf(r) > rows.indexOf(dom) && r.type === 'command'
    && r.payload?.sidecarGeneration === 2 && r.payload.envelope?.windowId === 'main'
    && r.payload.envelope.payload.command === 'runtime.replacement.status');
  for (const field of ['windowId', 'sessionId', 'uiId']) assert.notEqual(ownerQuery.payload.envelope[field], foreign[field]);
  const ownerReply = one(r => r.type === 'capabilityResult' && r.payload?.sidecarGeneration === 2
    && r.payload.envelope?.correlationId === ownerQuery.payload.envelope.correlationId);
  const op = one(r => r.type === 'sidecar.productReplacementRequested');
  const result = ownerReply.payload.envelope.payload;
  assert.equal(result.success, true); assert.equal(result.status, 'replaced');
  assert.equal(result.operationId, op.payload.operationId); assert.equal(result.requestingGeneration, 1);
  assert.equal(result.permissionDecision, 'allowed'); assert.equal(result.permissionReason, 'manifest-allow');
  for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(ownerReply.payload.envelope[field], ownerQuery.payload.envelope[field]);
  const ownerDom = one(r => r.type === 'standardBundle.runtimeOwnerPreserved');
  assert.equal(ownerDom.payload.status, 'replaced'); assert.equal(ownerDom.payload.observedInDom, true);
  const originalDom = one(r => r.type === 'standardBundle.productReplacement');
  const ordered = [originalDom, created, query, reply, dom, ownerQuery, ownerReply, ownerDom, closed];
  for (let i = 1; i < ordered.length; i++) assert.ok(rows.indexOf(ordered[i]) > rows.indexOf(ordered[i - 1]));
  assert.equal(rows.filter(r => r.type === 'sidecar.started').length, 2);
  assert.equal(rows.filter(r => r.type === 'sidecar.exit').length, 2);
  const excluded = new Set([query, reply, ownerQuery, ownerReply]);
  const base = verifyProductReplacement(rows.filter(r => !excluded.has(r)).map(r => 'HOST_PROTOCOL ' + JSON.stringify(r)).join('\n'));
  return {...base, foreignOwnerStatus: 'none', originalOwnerStatus: 'replaced'};
}

function verifyGenerationConflict(rows) {
  const one = predicate => {
    const matches = rows.filter(predicate); assert.equal(matches.length, 1); return matches[0];
  };
  const request = one(r => r.type === 'command' && r.payload?.sidecarGeneration === 2
    && r.payload.envelope?.payload?.command === 'runtime.replacement.request');
  const initial = one(r => r.type === 'command' && r.payload?.sidecarGeneration === 1
    && r.payload.envelope?.payload?.command === 'runtime.replacement.request');
  const envelope = request.payload.envelope;
  assert.equal(request.direction, 'recv');
  assert.equal(request.payload.source, 'java-sidecar');
  assert.equal(request.payload.transport, 'flow-ipc');
  assert.deepEqual(envelope.payload.args, initial.payload.envelope.payload.args);
  for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(envelope[field], initial.payload.envelope[field]);
  const denied = one(r => r.type === 'capabilityResult' && r.payload?.sidecarGeneration === 2
    && r.payload.envelope?.correlationId === envelope.correlationId);
  const denial = denied.payload.envelope.payload;
  assert.equal(denial.success, false);
  assert.equal(denial.errorCode, 'request-conflict');
  assert.equal(denial.permissionDecision, 'allowed');
  assert.equal(denial.permissionReason, 'manifest-allow');
  assert.equal(denial.operationId, undefined);
  for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(denied.payload.envelope[field], envelope[field]);
  const dom = one(r => r.type === 'standardBundle.productGenerationConflict');
  assert.equal(dom.payload.errorCode, 'request-conflict');
  assert.equal(dom.payload.observedInDom, true);
  assert.equal(dom.payload.recoveryGeneration, 1);
  const query = one(r => rows.indexOf(r) > rows.indexOf(dom) && r.type === 'command'
    && r.payload?.sidecarGeneration === 2 && r.payload.envelope?.payload?.command === 'runtime.replacement.status');
  const result = one(r => r.type === 'capabilityResult' && r.payload?.sidecarGeneration === 2
    && r.payload.envelope?.correlationId === query.payload.envelope.correlationId);
  const operation = one(r => r.type === 'sidecar.productReplacementRequested');
  const payload = result.payload.envelope.payload;
  assert.equal(payload.success, true);
  assert.equal(payload.status, 'replaced');
  assert.equal(payload.operationId, operation.payload.operationId);
  assert.equal(payload.requestingGeneration, 1);
  assert.equal(payload.permissionDecision, 'allowed');
  assert.equal(payload.permissionReason, 'manifest-allow');
  for (const field of ['windowId', 'sessionId', 'uiId']) {
    assert.equal(query.payload.envelope[field], envelope[field]);
    assert.equal(result.payload.envelope[field], envelope[field]);
  }
  const status = one(r => r.type === 'standardBundle.productConflictStatus');
  assert.equal(status.payload.status, 'replaced');
  assert.equal(status.payload.observedInDom, true);
  const originalDom = one(r => r.type === 'standardBundle.productReplacement');
  const ordered = [originalDom, request, denied, dom, query, result, status];
  for (let i = 1; i < ordered.length; i++) assert.ok(rows.indexOf(ordered[i]) > rows.indexOf(ordered[i - 1]));
  assert.equal(rows.filter(r => r.type === 'sidecar.started').length, 2);
  assert.equal(rows.filter(r => r.type === 'sidecar.exit').length, 2);
  const excluded = new Set([request, denied, query, result]);
  const base = verifyProductReplacement(rows.filter(r => !excluded.has(r))
    .map(r => 'HOST_PROTOCOL ' + JSON.stringify(r)).join('\n'));
  return {...base, crossGenerationReuse: 'request-conflict', terminalPreserved: true};
}

function verifyRequestBurst(rows) {
  const requests = rows.filter(r => r.type === 'command' && r.payload?.envelope?.payload?.command === 'runtime.replacement.request');
  assert.equal(requests.length, 4, 'four public commands must reach host');
  const operations = rows.filter(r => r.type === 'sidecar.productReplacementRequested');
  assert.equal(operations.length, 1, 'one admitted replacement');
  const operation = operations[0].payload;
  const correlations = new Set(requests.map(r => r.payload.envelope.correlationId));
  assert.equal(correlations.size, 4);
  const labels = requests.map(r => r.payload.envelope.payload.args.requestId).sort();
  assert.deepEqual(labels, ['framework-burst-0', 'framework-burst-0', 'framework-burst-1', 'framework-burst-1']);
  let admittedCorrelation;
  const counts = {admitted: 0, duplicate: 0, busy: 0, fenced: 0};
  const scope = requests[0].payload.envelope;
  for (const request of requests) {
    const env = request.payload.envelope;
    assert.equal(request.direction, 'recv');
    assert.equal(request.payload.transport, 'flow-ipc');
    assert.equal(request.payload.source, 'java-sidecar');
    assert.equal(request.payload.sidecarGeneration, 1);
    assert.equal(env.version, 1);
    assert.equal(env.payload.args.candidateId, 'next');
    for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(env[field], scope[field]);
    const results = rows.filter(r => r.type === 'capabilityResult'
      && r.payload?.sidecarGeneration === request.payload.sidecarGeneration
      && r.payload?.envelope?.correlationId === env.correlationId);
    const drops = rows.filter(r => r.type === 'staleCapabilityCommandDropped' && r.payload.correlationId === env.correlationId);
    assert.equal(results.length + drops.length, 1, 'each request has exactly one disposition');
    const event = results[0] ?? drops[0];
    assert.ok(rows.indexOf(event) > rows.indexOf(request));
    if (drops.length) {
      assert.equal(event.payload.expectedGeneration, 1);
      assert.equal(event.payload.reason, 'sidecar-generation-unavailable');
      assert.equal(event.payload.command, 'runtime.replacement.request');
      for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(event.payload[field], scope[field]);
      assert.ok(rows.indexOf(event) > rows.indexOf(operations[0]));
      counts.fenced++;
      continue;
    }
    assert.equal(event.payload.sidecarGeneration, 1);
    for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(event.payload.envelope[field], scope[field]);
    const result = event.payload.envelope.payload;
    assert.equal(result.permissionDecision, 'allowed');
    assert.equal(result.permissionReason, 'manifest-allow');
    if (result.success) {
      assert.equal(result.operationId, operation.operationId);
      assert.equal(result.status, 'pending');
      assert.equal(env.payload.args.requestId, operation.requestId);
      assert.equal(typeof result.duplicate, 'boolean');
      if (result.duplicate) counts.duplicate++;
      else { counts.admitted++; admittedCorrelation = env.correlationId; }
    } else {
      assert.equal(result.errorCode, 'busy');
      assert.notEqual(env.payload.args.requestId, operation.requestId);
      counts.busy++;
    }
  }
  assert.equal(counts.admitted, 1);
  assert.equal(rows.filter(r => r.type === 'sidecar.started').length, 2);
  assert.equal(rows.filter(r => r.type === 'sidecar.exit').length, 2);
  // Reuse the complete single-operation lifecycle gate after independently
  // proving that all extra public requests were duplicate, busy or fenced.
  const lifecycle = rows.filter(r => {
    const id = r.payload?.envelope?.correlationId;
    return !(['command', 'capabilityResult'].includes(r.type)
      && r.payload?.sidecarGeneration === 1
      && correlations.has(id) && id !== admittedCorrelation);
  });
  const result = verifyProductReplacement(lifecycle.map(r => 'HOST_PROTOCOL ' + JSON.stringify(r)).join('\n'));
  return {...result, requestDispositions: counts};
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  console.log('PASS product replacement and cross-JVM result', verifyProductReplacement(fs.readFileSync(process.argv[2], 'utf8'), process.argv[3] ?? 'success'));
}
