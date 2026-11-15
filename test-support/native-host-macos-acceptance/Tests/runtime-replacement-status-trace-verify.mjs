import fs from 'node:fs';
import { pathToFileURL } from 'node:url';
import assert from 'node:assert/strict';

export function verifyRuntimeStatus(text) {
  const rows = text.split('\n').filter(line => line.startsWith('HOST_PROTOCOL '))
    .map(line => JSON.parse(line.slice('HOST_PROTOCOL '.length)));
  const command = 'runtime.replacement.status';
  const requests = rows.map((row, index) => ({ row, index })).filter(({row}) =>
    row.type === 'command' && row.direction === 'recv'
      && row.payload?.envelope?.payload?.command === command);
  assert.equal(requests.length, 1, 'one Java query expected');
  const { row: request, index: requestIndex } = requests[0];
  const envelope = request.payload.envelope;
  assert.equal(request.payload.transport, 'flow-ipc');
  assert.equal(request.payload.source, 'java-sidecar');
  assert.equal(envelope.version, 1);
  assert.deepEqual(envelope.payload.args, {});
  assert.ok(Number.isInteger(request.payload.sidecarGeneration) && request.payload.sidecarGeneration > 0);
  for (const field of ['correlationId', 'windowId', 'sessionId', 'uiId']) assert.ok(envelope[field]);
  const results = rows.map((row, index) => ({row, index})).filter(({row}) =>
    row.type === 'capabilityResult' && row.direction === 'send'
      && row.payload?.envelope?.correlationId === envelope.correlationId);
  assert.equal(results.length, 1, 'one correlated native result expected');
  const {row: response, index: resultIndex} = results[0];
  assert.ok(resultIndex > requestIndex);
  assert.equal(response.payload.transport, 'flow-ipc');
  assert.equal(response.payload.sidecarGeneration, request.payload.sidecarGeneration);
  const resultEnvelope = response.payload.envelope;
  assert.equal(resultEnvelope.version, 1);
  assert.equal(resultEnvelope.error ?? null, null);
  for (const field of ['windowId', 'sessionId', 'uiId']) assert.equal(resultEnvelope[field], envelope[field]);
  const result = resultEnvelope.payload;
  assert.equal(result.command, command);
  assert.equal(result.success, true);
  assert.equal(result.mode, 'host-runtime-replacement');
  assert.equal(result.permissionDecision, 'allowed');
  assert.equal(result.permissionReason, 'manifest-allow');
  assert.equal(result.status, 'none');
  for (const field of ['operationId', 'candidateId', 'requestId', 'error', 'operationInvalidated'])
    assert.equal(Object.hasOwn(result, field), false);
  const observed = rows.findIndex(row => row.type === 'standardBundle.runtimeReplacementStatus'
    && row.payload?.status === 'none' && row.payload?.observedInDom === true);
  assert.ok(observed > resultIndex, 'Java result must be reflected in DOM after native result');
  return { status: 'none', generation: request.payload.sidecarGeneration, windowId: envelope.windowId };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  assert.ok(process.argv[2], 'trace path required');
  console.log('PASS runtime replacement status query', verifyRuntimeStatus(fs.readFileSync(process.argv[2], 'utf8')));
}
