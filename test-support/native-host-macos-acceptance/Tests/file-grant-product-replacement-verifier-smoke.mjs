import fs from 'node:fs';
import assert from 'node:assert/strict';
import {verifyFileReplacement} from './file-grant-product-replacement-verify.mjs';

// Mutation tests replay a retained real run, never rewrite its original trace.
const [file, mode] = process.argv.slice(2);
if (!file || !['read', 'write'].includes(mode)) throw Error('Usage: node verifier-smoke.mjs trace read|write');
const text = fs.readFileSync(file, 'utf8');
verifyFileReplacement(text, mode);
const rows = text.split('\n').filter(s => s.startsWith('HOST_PROTOCOL ')).map(s => JSON.parse(s.slice(14)));
const prefix = mode === 'read' ? 'fileReadGrant' : 'fileWriteGrant';
const acceptance = mode === 'read' ? 'flowFileReadGrantReplay' : 'flowFileWriteGrantReplaySandbox';
const serialize = r => r.map(row => 'HOST_PROTOCOL ' + JSON.stringify(row)).join('\n');
const mutations = [
  r => r.filter(x => x.type !== 'sidecar.productReplacementRequested'),
  r => r.filter(x => x.type !== 'sidecar.productReplacementResult'),
  r => {r.find(x => x.type === 'sidecar.productReplacementResult').payload.status = 'rolled-back'; return r;},
  r => {r.find(x => x.type === `${prefix}.invalidated`).payload.reason = 'sidecar-exit'; return r;},
  r => {r.find(x => x.type === `${prefix}.closed`).payload.closeSucceeded = false; return r;},
  r => {r.find(x => x.type === `${prefix}.closed`).payload.securityScopeStopped = false; return r;},
  r => {r.find(x => x.type === `${prefix}.created`).payload.securityScoped = false; return r;},
  r => {r.find(x => x.type === `${acceptance}.frameworkAcceptance`).payload.complete = false; return r;},
  r => {r.find(x => x.type === 'host.nativeResourceDrainCompleted').payload.pendingCount = 1; return r;},
  r => r.filter(x => !(x.type === 'sidecar.exit' && x.payload.generation === 2)),
  r => {r.push(structuredClone(r.find(x => x.type === 'capabilityResult'))); return r;},
  r => {r.find(x => x.type === 'capabilityResult').payload.envelope.sessionId = 'foreign'; return r;},
  r => {r.find(x => x.type === 'command').payload.transport = 'compatibility-http'; return r;},
  r => {r.push({type:'acceptance.sidecarFailureInjected',payload:{}}); return r;},
];
mutations.forEach((mutate, i) => assert.throws(() => verifyFileReplacement(serialize(mutate(structuredClone(rows))), mode), undefined, `mutation ${i}`));
console.log(`File replacement ${mode} verifier: real positive and ${mutations.length} negative mutations passed`);
