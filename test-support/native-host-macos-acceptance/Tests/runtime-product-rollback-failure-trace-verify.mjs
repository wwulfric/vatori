import assert from 'node:assert/strict';
import fs from 'node:fs';
import {pathToFileURL} from 'node:url';

export function verifyRollbackFailure(text, mode = 'rollback-failure') {
  assert.ok(['rollback-failure', 'blocked-rollback'].includes(mode));
  const blocked = mode === 'blocked-rollback';
  const rows = text.split('\n').filter(l => l.startsWith('HOST_PROTOCOL ')).map(l => JSON.parse(l.slice(14)));
  const one = (type, test = () => true) => {
    const matches = rows.filter(r => r.type === type && test(r));
    assert.equal(matches.length, 1, `exactly one ${type}`);
    return matches[0];
  };
  const before = (a, b) => assert.ok(rows.indexOf(a) < rows.indexOf(b), `${a.type} before ${b.type}`);
  assert.equal(one('launch').payload.transport, 'flow-ipc');
  assert.equal(one('standardBundleReady').payload.requestTransport, 'opaque-http-stream-ipc');
  const dom = one('standardBundle.runtimeReplacementStatus');
  assert.equal(dom.payload.status, 'none');
  assert.equal(dom.payload.observedInDom, true);
  const request = one('command', r => r.payload?.envelope?.payload?.command === 'runtime.replacement.request');
  assert.equal(request.direction, 'recv');
  assert.equal(request.payload.source, 'java-sidecar');
  assert.equal(request.payload.transport, 'flow-ipc');
  assert.equal(request.payload.sidecarGeneration, 1);
  const envelope = request.payload.envelope;
  assert.equal(envelope.version, 1);
  for (const key of ['windowId', 'sessionId', 'uiId', 'correlationId']) assert.ok(envelope[key]);
  assert.equal(envelope.payload.args.candidateId, 'next');
  before(dom, request);
  const admitted = one('sidecar.productReplacementRequested');
  assert.ok(admitted.payload.operationId);
  assert.equal(admitted.payload.requestId, envelope.payload.args.requestId);
  assert.equal(admitted.payload.candidateId, 'next');
  assert.equal(admitted.payload.requestingGeneration, 1);
  before(request, admitted);
  const receipt = one('capabilityResult', r => r.payload?.envelope?.correlationId === envelope.correlationId);
  assert.equal(receipt.payload.sidecarGeneration, 1);
  for (const key of ['windowId', 'sessionId', 'uiId']) assert.equal(receipt.payload.envelope[key], envelope[key]);
  const result = receipt.payload.envelope.payload;
  assert.equal(result.success, true);
  assert.equal(result.status, 'pending');
  assert.equal(result.operationId, admitted.payload.operationId);
  assert.equal(result.permissionDecision, 'allowed');
  assert.equal(result.permissionReason, 'manifest-allow');
  before(request, receipt);
  assert.equal(rows.filter(r => r.type === 'sidecar.started').length, 3);
  assert.equal(rows.filter(r => r.type === 'sidecar.exit').length, 3);
  assert.equal(rows.filter(r => r.type === 'launch.sidecar').length, 3);
  const starts = [], exits = [], launches = [];
  for (const generation of [1, 2, 3]) {
    const start = one('sidecar.started', r => r.payload.generation === generation);
    const exit = one('sidecar.exit', r => r.payload.generation === generation);
    const launch = one('launch.sidecar', r => r.payload.generation === generation);
    const path = generation === 2 ? 'RuntimeCandidates/next/application.jar' : 'Sidecar/application.jar';
    assert.ok(launch.payload.command.endsWith(` -jar $APP/Contents/Resources/${path}`));
    assert.ok(start.payload.processIdentifier > 0);
    assert.equal(exit.payload.processIdentifier, start.payload.processIdentifier);
    assert.equal(exit.payload.expected, blocked && generation === 3);
    if (generation > 1) assert.equal(exit.payload.status, generation === 2 ? 42 : (blocked ? 9 : 43));
    before(launch, start); before(start, exit);
    if (generation > 1) before(exits[generation - 2], launch);
    starts.push(start); exits.push(exit); launches.push(launch);
  }
  assert.equal(new Set(starts.map(r => r.payload.processIdentifier)).size, 3);
  assert.equal(admitted.payload.processIdentifier, starts[0].payload.processIdentifier);
  before(starts[0], request); before(admitted, exits[0]); before(receipt, exits[0]);
  const failed = one('sidecar.replacementFailed');
  assert.equal(failed.payload.requestId, admitted.payload.operationId);
  assert.equal(failed.payload.cause, 'candidate-exited-before-ready');
  before(exits[1], failed);
  const rollback = one('sidecar.rollbackStarted');
  assert.equal(rollback.payload.requestId, admitted.payload.operationId);
  before(failed, rollback); before(rollback, launches[2]);
  const rollbackFailed = one('sidecar.rollbackFailed');
  assert.equal(rollbackFailed.payload.requestId, admitted.payload.operationId);
  assert.equal(rollbackFailed.payload.cause, blocked ? 'rollback-ready-timeout' : 'rollback-exited-before-ready');
  assert.equal(rollbackFailed.payload.processIdentifier, starts[2].payload.processIdentifier);
  if (!blocked) {
    assert.equal(rollbackFailed.payload.status, 43);
    before(exits[2], rollbackFailed);
  } else {
    assert.equal(rollbackFailed.payload.timeoutSeconds, 15);
    const wait = rollbackFailed.at - starts[2].at;
    assert.ok(Number.isFinite(wait) && wait >= 14500 && wait < 20000);
    before(starts[2], rollbackFailed); before(rollbackFailed, exits[2]);
  }
  const terminal = one('sidecar.productReplacementResult');
  assert.equal(terminal.payload.operationId, admitted.payload.operationId);
  assert.equal(terminal.payload.status, 'rollback-failed');
  before(rollbackFailed, terminal);
  const drain = one('host.nativeResourceDrainCompleted');
  assert.equal(drain.payload.drained, true);
  assert.equal(drain.payload.pendingCount, 0);
  assert.equal(drain.payload.failureCount, 0);
  const stopped = one('host.sidecarShutdownCompleted');
  assert.equal(stopped.payload.succeeded, true);
  const fatal = one('host.failureShutdownCompleted');
  assert.equal(fatal.payload.exitStatus, 1);
  before(terminal, drain); before(drain, stopped); before(stopped, fatal);
  before(exits[2], stopped);
  if (blocked) {
    assert.ok(text.includes('Test rollback deliberately never ready'));
    assert.ok(text.includes('Test rollback shutdown hook blocked'));
    const term = one('sidecar.shutdownTerminated');
    const kill = one('sidecar.shutdownEscalated');
    for (const event of [term, kill]) {
      assert.equal(event.payload.generation, 3);
      assert.equal(event.payload.processIdentifier, starts[2].payload.processIdentifier);
      assert.equal(event.payload.graceSeconds, 1);
    }
    before(drain, term); before(term, kill); before(kill, exits[2]);
    const grace = kill.at - term.at;
    assert.ok(Number.isFinite(grace) && grace >= 900 && grace < 5000);
    const exitWait = exits[2].at - kill.at;
    assert.ok(Number.isFinite(exitWait) && exitWait >= 0 && exitWait < 2000);
  }
  assert.ok(Number.isFinite(request.at) && Number.isFinite(fatal.at));
  assert.ok(fatal.at >= request.at && fatal.at - request.at < 30000, 'failure settlement within 30 seconds');
  assert.equal(rows.some(r => ['sidecar.recoveryScheduled', 'sidecar.rollbackReady',
    'standardBundle.productReplacement', 'host.sidecarShutdownFailed'].includes(r.type)), false);
  return {operationId: admitted.payload.operationId, pids: starts.map(r => r.payload.processIdentifier),
    outcome: 'rollback-failed', hostExitStatus: 1};
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  console.log('PASS bounded product rollback failure', verifyRollbackFailure(fs.readFileSync(process.argv[2], 'utf8'), process.argv[3]));
}
