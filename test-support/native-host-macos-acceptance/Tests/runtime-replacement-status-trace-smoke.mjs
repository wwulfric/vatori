import assert from 'node:assert/strict';
import { verifyRuntimeStatus } from './runtime-replacement-status-trace-verify.mjs';

const scope = { windowId: 'main', sessionId: 'session', uiId: 'ui', correlationId: 'query', version: 1 };
const rows = [
  { type: 'command', direction: 'recv', payload: { transport: 'flow-ipc', source: 'java-sidecar', sidecarGeneration: 1,
    envelope: { ...scope, payload: {command: 'runtime.replacement.status', args: {}} } } },
  { type: 'capabilityResult', direction: 'send', payload: { transport: 'flow-ipc', sidecarGeneration: 1,
    envelope: { ...scope, payload: {command: 'runtime.replacement.status', mode: 'host-runtime-replacement',
      permissionDecision: 'allowed', permissionReason: 'manifest-allow', success: true, status: 'none'} } } },
  { type: 'standardBundle.runtimeReplacementStatus', payload: {status: 'none', observedInDom: true} }
];
const text = input => input.map(row => 'HOST_PROTOCOL ' + JSON.stringify(row)).join('\n');
verifyRuntimeStatus(text(rows));
const mutations = [
  r => r.splice(0, 1), r => r.splice(1, 1), r => r.splice(2, 1),
  r => r.push(structuredClone(r[0])), r => r.push(structuredClone(r[1])),
  r => r.reverse(),
  r => { r[0].payload.transport = 'compatibility-http'; },
  r => { r[0].payload.envelope.version = '1'; },
  r => { r[0].payload.envelope.payload.args = { owner: 'other' }; },
  r => { r[1].payload.sidecarGeneration = 2; },
  r => { r[1].payload.envelope.correlationId = 'other'; },
  ...['windowId', 'sessionId', 'uiId'].map(field => r => { r[1].payload.envelope[field] = 'other'; }),
  r => { r[1].payload.envelope.payload.permissionDecision = 'denied'; },
  r => { r[1].payload.envelope.payload.permissionReason = 'development-default'; },
  r => { r[1].payload.envelope.error = 'failed'; },
  r => { r[1].payload.envelope.payload.success = false; },
  r => { r[1].payload.envelope.payload.status = 'replaced'; },
  r => { r[1].payload.envelope.payload.operationId = 'unexpected'; },
  r => { r[1].payload.envelope.payload.operationInvalidated = true; },
  r => { r[2].payload.observedInDom = false; }
];
for (const mutate of mutations) {
  const altered = structuredClone(rows); mutate(altered);
  assert.throws(() => verifyRuntimeStatus(text(altered)));
}
console.log(`Runtime status trace verifier smoke passed: valid fixture and ${mutations.length} negatives (synthetic)`);
