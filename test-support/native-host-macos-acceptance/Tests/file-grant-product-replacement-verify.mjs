import fs from 'node:fs';
import assert from 'node:assert/strict';
import {pathToFileURL} from 'node:url';

export function verifyFileReplacement(text, mode) {
  assert.ok(['read', 'write'].includes(mode));
  const rows = text.split('\n').filter(s => s.startsWith('HOST_PROTOCOL '))
    .map(s => JSON.parse(s.slice(14))).map((r, i) => ({...r, i}));
  const all = (type, predicate = () => true) => rows.filter(r => r.type === type && predicate(r));
  const one = (type, predicate) => {
    const found = all(type, predicate); assert.equal(found.length, 1, `one ${type}`); return found[0];
  };
  const yes = (payload, names) => names.forEach(n => assert.equal(payload[n], true, n));
  assert.equal(all('error').length, 0);
  for (const type of ['acceptance.sidecarFailureInjected', 'sidecar.recoveryScheduled',
    'sidecar.replacementFailed', 'sidecar.rollbackReady', 'sidecar.replacementEscalated'])
    assert.equal(all(type).length, 0, `unexpected ${type}`);
  const owner = `file-${mode}-grant-replay-secondary`;
  const commands = all('command');
  const envelope = r => r.payload.envelope;
  const commandName = r => envelope(r)?.payload?.command;
  const result = command => {
    const matched = one('capabilityResult', r => envelope(r)?.correlationId === envelope(command).correlationId
      && r.payload.sidecarGeneration === command.payload.sidecarGeneration);
    assert.ok(command.i < matched.i);
    for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(envelope(matched)[field], envelope(command)[field]);
    assert.equal(envelope(matched).payload.permissionDecision, 'allowed');
    return matched;
  };
  const request = one('command', r => commandName(r) === 'runtime.replacement.request');
  assert.equal(envelope(request).windowId, 'main');
  assert.equal(envelope(request).payload.args.requestId, 'file-grant-replacement');
  assert.equal(envelope(request).payload.args.candidateId, 'next');
  const admitted = envelope(result(request)).payload;
  yes(admitted, ['success']); assert.equal(admitted.status, 'pending');
  const requested = one('sidecar.productReplacementRequested');
  const terminal = one('sidecar.productReplacementResult');
  assert.equal(requested.payload.operationId, admitted.operationId);
  assert.equal(terminal.payload.operationId, admitted.operationId);
  assert.equal(terminal.payload.status, 'replaced');
  assert.ok(request.i < requested.i && requested.i < terminal.i);
  const old = one('sidecar.started', r => r.payload.generation === 1);
  const fresh = one('sidecar.started', r => r.payload.generation === 2);
  assert.equal(all('sidecar.started').length, 2);
  assert.ok(old.payload.processIdentifier > 0 && fresh.payload.processIdentifier > 0);
  assert.notEqual(old.payload.processIdentifier, fresh.payload.processIdentifier);
  const oldExit = one('sidecar.exit', r => r.payload.generation === 1);
  const finalExit = one('sidecar.exit', r => r.payload.generation === 2);
  assert.equal(oldExit.payload.processIdentifier, old.payload.processIdentifier);
  assert.equal(finalExit.payload.processIdentifier, fresh.payload.processIdentifier);
  const launch = one('launch.sidecar', r => r.payload.generation === 2);
  assert.ok(launch.payload.command.endsWith(' -jar $APP/Contents/Resources/RuntimeCandidates/next/application.jar'));
  assert.ok(requested.i < oldExit.i && oldExit.i < fresh.i && fresh.i < terminal.i);
  assert.equal(all('sidecar.exit').length, 2);
  const drained = one('host.nativeResourceDrainCompleted');
  yes(drained.payload, ['drained']);
  assert.equal(drained.payload.pendingCount, 0);
  assert.equal(drained.payload.failureCount, 0);
  const prefix = mode === 'read' ? 'flowFileReadGrantReplay' : 'flowFileWriteGrantReplaySandbox';
  const accepted = one(`${prefix}.frameworkAcceptance`);
  yes(accepted.payload, ['complete', 'standardBundleMode', 'flowClientOwned', 'secondaryContextRemoved',
    'processReplaced', 'recoveredStateResetObserved', 'balancedDrainObserved', 'resultStableAfterDrain',
    'panelURLsVerified', 'externalPanelSelection', 'nativePointerObserved', 'nativePointerTrustedObserved',
    'errorFree', 'gracefulCloseRequested', 'sandboxEnforced']);
  assert.equal(accepted.payload.physicalInput, false);
  assert.equal(accepted.payload.panelCount, 2);
  const pointers = all(`${prefix}.nativePointerDispatch`);
  const fileActions = commands.filter(c => commandName(c) !== 'runtime.replacement.request');
  assert.equal(pointers.length, mode === 'read' ? 8 : 14);
  assert.equal(fileActions.length, pointers.length);
  pointers.forEach((p, i) => {
    assert.equal(p.payload.inputMode, 'appkit-window-pointer-event');
    yes(p.payload, ['activeApplication', 'keyWindow']);
    for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(p.payload[field], envelope(fileActions[i])[field]);
    assert.ok(p.i < fileActions[i].i && (!pointers[i+1] || result(fileActions[i]).i < pointers[i+1].i));
  });
  assert.ok(terminal.i < accepted.i && accepted.i < finalExit.i);
  assert.ok(accepted.i < drained.i && drained.i < finalExit.i);
  const action = one('fileGrant.productReplacementAction');
  yes(action.payload, ['dispatched']); assert.equal(action.payload.inputMode, 'browser-programmatic');
  const panels = all(`${prefix}.panelObserved`);
  assert.equal(panels.length, 2); panels.forEach(p => yes(p.payload, ['complete', 'panelURLVerified', 'externalPanelSelection']));
  assert.ok(panels[0].i < request.i && panels[1].i > terminal.i);
  const grantPrefix = mode === 'read' ? 'fileReadGrant' : 'fileWriteGrant';
  const created = all(`${grantPrefix}.created`);
  assert.equal(created.length, 2);
  assert.notEqual(created[0].payload.diagnosticId, created[1].payload.diagnosticId);
  created.forEach((r, i) => {
    assert.equal(r.payload.windowId, owner); assert.equal(r.payload.sidecarGeneration, i + 1);
    assert.equal(r.payload.securityScoped, true);
  });
  const invalidated = one(`${grantPrefix}.invalidated`);
  assert.equal(invalidated.payload.diagnosticId, created[0].payload.diagnosticId);
  assert.equal(invalidated.payload.reason, 'sidecar-replacement');
  assert.equal(invalidated.payload[mode + 'InFlight'], false, 'settled-grant profile, not in-flight proof');
  const closes = all(`${grantPrefix}.closed`);
  assert.equal(closes.length, 2);
  closes.forEach((r, i) => {
    assert.equal(r.payload.diagnosticId, created[i].payload.diagnosticId);
    yes(r.payload, ['closeSucceeded', 'securityScopeStopped']);
    assert.equal(r.payload.reason, i === 0 ? 'sidecar-replacement' : 'released');
  });
  assert.ok(created[0].i < request.i && request.i < invalidated.i && invalidated.i < closes[0].i
    && closes[0].i < created[1].i && created[1].i < closes[1].i && closes[1].i < accepted.i);
  const staleCommands = commands.filter(r => r.payload.sidecarGeneration === 2
    && r.i < created[1].i && commandName(r)?.startsWith('fileGrant.'));
  assert.equal(staleCommands.length, 2);
  for (const c of staleCommands) {
    assert.equal(envelope(c).windowId, owner);
    const p = envelope(result(c)).payload;
    assert.equal(p.success, false); assert.equal(p.grantUnavailable, true);
    assert.equal(p.errorCode, 'grant-unavailable');
    assert.equal(p.bodyBase64, undefined); assert.equal(p.bytesWritten, undefined);
  }
  for (const c of commands) {
    assert.equal(c.payload.transport, 'flow-ipc'); assert.equal(c.payload.source, 'java-sidecar');
    assert.equal(envelope(c).version, 1);
    if (commandName(c)?.startsWith('file')) assert.equal(envelope(c).windowId, owner);
    result(c);
  }
  if (mode === 'read') {
    yes(accepted.payload, ['sameOldTokenUsedObserved', 'oldTokenUnchangedObserved', 'oldNoContentObserved',
      'freshTokenDistinctObserved', 'contentMatches', 'handoffRemovedObserved', 'oldSecurityScoped', 'freshSecurityScoped',
      'mainUidlRoundTripObserved', 'secondaryUidlRoundTripObserved']);
    assert.equal(accepted.payload.freshReadBytes, 19);
    const probes = all('fileGrant.readSandboxProbe');
    assert.deepEqual(probes.map(p => p.payload.stage), ['old-target-before', 'old-sibling-before', 'fresh-sibling-before', 'released-sibling']);
    probes.forEach(p => yes(p.payload, ['denied', 'exactEntitlements']));
    const reads = commands.filter(c => commandName(c) === 'fileGrant.read');
    assert.equal(reads.length, 2);
    const readResult = envelope(result(reads[1])).payload;
    assert.equal(readResult.success, true); assert.equal(readResult.bytesRead, 19);
    assert.equal(readResult.offset, 0);
  } else {
    yes(accepted.payload, ['replayDenied', 'handoffRemoved', 'exactEntitlementsObserved',
      'oldPreselectionDenied', 'freshPreselectionDenied', 'oldSiblingDeniedAfterWrite', 'freshSiblingDeniedAfterWrite']);
    assert.equal(accepted.payload.writtenByteCount, 65542);
    const probes = all(`${prefix}.accessProbe`);
    assert.equal(probes.length, 6); probes.forEach(p => yes(p.payload, ['complete', 'denied']));
    const writes = all('fileWriteGrant.write');
    assert.equal(writes.length, 4);
    writes.forEach((w, i) => {
      assert.equal(w.payload.success, true);
      assert.equal(w.payload.sidecarGeneration, i < 2 ? 1 : 2);
      assert.equal(w.payload.bytesWritten, i % 2 === 0 ? 65536 : 6);
      assert.equal(w.payload.nextOffset, i % 2 === 0 ? 65536 : 65542);
    });
    closes.forEach(c => assert.equal(c.payload.nextOffset, 65542));
  }
  return {mode, status:'PASS', oldPid:old.payload.processIdentifier, newPid:fresh.payload.processIdentifier,
    evidence:'real Sandbox settled grants; public product replacement; in-flight races require separate deterministic evidence'};
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  console.log(JSON.stringify(verifyFileReplacement(fs.readFileSync(process.argv[2], 'utf8'), process.argv[3]), null, 2));
}
