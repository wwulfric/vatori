import assert from 'node:assert/strict';
import {verifyWindowChurnLifecycle} from './window-churn-lifecycle-verify.mjs';

const rows = [];
const add = (type, payload, direction = 'send') => rows.push({type,payload,direction});
const scope = (windowId, n = 0) => ({windowId,uiId:windowId,sessionId:`${windowId}-${n}`});
const create = p => add('window.create', {...p,mode:'flow-ipc'});
const close = p => {
  add('window.closeRequested', p, 'recv');
  add('windowClosed', {...p,target:'java-sidecar',transport:'flow-ipc'});
};
add('sidecar.started', {processIdentifier:100,generation:1});
create(scope('main')); create(scope('sibling'));
for (let i = 0; i <= 10; i++) { create(scope('churn',i)); close(scope('churn',i)); }
close(scope('sibling')); close(scope('main'));
add('sidecar.exit', {processIdentifier:100,generation:1,expected:true,status:0});
const encode = events => events.map(row => 'HOST_PROTOCOL '+JSON.stringify(row)).join('\n');
assert.deepEqual(verifyWindowChurnLifecycle(encode(rows)),
  {peakWindows:3,reopenCycles:10,lifecycleOnly:true,uiIsolationProven:false});
let negatives = 0;
const reject = events => { assert.throws(() => verifyWindowChurnLifecycle(encode(events))); negatives++; };
for (let i = 0; i < rows.length; i++) reject(rows.filter((_,index) => index !== i));
for (const [type,field,value] of [
  ['window.create','mode','compatibility-http'], ['window.create','sessionId',''],
  ['windowClosed','sessionId','other'], ['windowClosed','transport','compatibility-http'],
  ['windowClosed','target','other'], ['sidecar.exit','status',1],
  ['sidecar.exit','expected',false], ['sidecar.exit','processIdentifier',101],
]) {
  const copy = structuredClone(rows); copy.find(row => row.type === type).payload[field] = value; reject(copy);
}
const reused = structuredClone(rows);
reused.filter(row => row.payload.sessionId === 'churn-1').forEach(row => row.payload.sessionId = 'churn-0');
reject(reused);
const early = structuredClone(rows); const closeIndex = early.findIndex(row => row.type === 'window.closeRequested');
[early[closeIndex],early[closeIndex+1]] = [early[closeIndex+1],early[closeIndex]]; reject(early);
reject([...rows,{type:'sidecar.recoveryReady',payload:{}}]);
reject([...rows,{type:'error',payload:{}}]);
console.log(`PASS window churn lifecycle subgate: synthetic positive and ${negatives} negatives; no real UI claim`);
