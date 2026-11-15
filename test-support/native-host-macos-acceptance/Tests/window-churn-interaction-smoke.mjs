import assert from 'node:assert/strict';
import {verifyWindowChurnInteractions} from './window-churn-interaction-verify.mjs';
const rows = [];
const add = (type,payload,direction='send') => { const row={type,payload,direction}; rows.push(row); return row; };
const runId='synthetic-run', processIdentifier=100;
const scope=(windowId,n=0)=>({windowId,uiId:windowId,sessionId:`${windowId}-${n}`});
const main=scope('main'), sibling=scope('churn-sibling');
const counts=new Map();
const create=s=>add('window.create',{...s,mode:'flow-ipc'});
const close=s=>{ add('window.closeRequested',s,'recv'); add('windowClosed',{...s,target:'java-sidecar',transport:'flow-ipc'}); };
const wire=(type,s,payload,id,direction='recv')=>add(type,{transport:'flow-ipc',sidecarGeneration:1,
  [direction==='recv'?'source':'target']:'java-sidecar',envelope:{...s,version:1,type,id,correlationId:id,payload}},direction);
const request=s=>{
  const n=(counts.get(s.sessionId)||0)+1; counts.set(s.sessionId,n); const id=`request-${n}`;
  wire('opaqueHttpStreamOpen',s,{requestId:id,method:'POST'},id,'send');
  wire('opaqueHttpStreamResponse',s,{requestId:id,status:200},id);
  wire('opaqueHttpStreamResponseEnd',s,{requestId:id},id);
};
const action=(s,element,cycle)=>{add('windowChurn.action',{...s,runId,processIdentifier,inputMode:'browser-programmatic',cycle,element});request(s);};
const windowAction=(s,opening,cycle)=>{
  action(main,`window-churn-${opening?'open':'close'}-${s.windowId}`,cycle);
  const command=opening?'window.open':'window.close', id=`${command}-${s.sessionId}`;
  wire('command',main,{command,args:{id:s.windowId}},id);
  if(opening) create(s); else close(s);
  wire('capabilityResult',main,{command,success:true,permissionDecision:'allowed'},id,'send');
};
const identity=n=>`00000000-0000-4000-8000-${String(n).padStart(12,'0')}`;
const stage=(s,cycle,n)=>add('windowChurn.stageObserved',{...s,runId,processIdentifier,inputMode:'browser-programmatic',
  cycle,settled:true,error:'',state:'change=1 client=true',document:identity(n),windowNumber:n,
  requestCount:counts.get(s.sessionId),responseCount:counts.get(s.sessionId),pushMessageCount:s.windowId==='churn-secondary'?1:0,
  probe:s.windowId==='churn-secondary'?`cycle=${cycle} before=${cycle} after=${cycle} fresh=true background=true client=true`:'pending'},'recv');
add('sidecar.started',{processIdentifier,generation:1});
create(main); action(main,'window-churn-mutate',0); windowAction(sibling,true,0); action(sibling,'window-churn-mutate',0);
for(let cycle=0;cycle<=10;cycle++) {
  const s=scope('churn-secondary',cycle); windowAction(s,true,cycle);
  action(s,'window-churn-mutate',cycle); action(s,'window-churn-probe',cycle);
  wire('command',s,{command:'window.setTitle',args:{title:`Churn owner ${cycle}`}},'title');
  wire('capabilityResult',s,{command:'window.setTitle',success:true,permissionDecision:'allowed'},'title','send');
  const push={connectionId:'push',pushSequence:1,flowUiId:0,flowServerSyncId:2,uidlBytes:100};
  wire('opaquePush',s,push,'push');
  wire('opaquePushAck',s,{...push,lastSeenServerSyncId:2},'ack','send');
  stage(main,cycle,1); stage(sibling,cycle,2); stage(s,cycle,cycle+3); windowAction(s,false,cycle);
}
windowAction(sibling,false,10);
add('windowChurn.browserRunCompleted',{...main,runId,processIdentifier,inputMode:'browser-programmatic',cycles:10,incarnations:11,independentGatePassed:false},'recv');
close(main); add('sidecar.exit',{processIdentifier,generation:1,expected:true,status:0});
const encode=r=>r.map(row=>'HOST_PROTOCOL '+JSON.stringify(row)).join('\n');
assert.equal(verifyWindowChurnInteractions(encode(rows)).reopenCycles,10);
let negatives=0;
const reject=r=>{ assert.throws(()=>verifyWindowChurnInteractions(encode(r))); negatives++; };
for(let i=0;i<rows.length;i++) reject(rows.filter((_,j)=>i!==j));
for(const mutate of [
  r=>r.find(x=>x.type==='opaquePushAck').payload.envelope.payload.lastSeenServerSyncId=1,
  r=>r.find(x=>x.type==='opaquePushAck').payload.envelope.sessionId='other',
  r=>r.find(x=>x.type==='command' && x.payload.envelope.payload.command==='window.setTitle').payload.envelope.payload.args.title='stale',
  r=>r.find(x=>x.type==='command').payload.envelope.payload.args.id='wrong-owner',
  r=>r.find(x=>x.type==='capabilityResult').payload.envelope.correlationId='wrong-correlation',
  r=>r.find(x=>x.type==='command').direction='send',
  r=>r.find(x=>x.type==='opaquePushAck').payload.target='other-process',
  r=>r.find(x=>x.type==='capabilityResult').payload.envelope.payload.success=false,
  r=>r.find(x=>x.type==='windowChurn.stageObserved').payload.state='change=2 client=true',
  r=>r.find(x=>x.type==='windowChurn.stageObserved').payload.inputMode='physical',
  r=>r.find(x=>x.type==='opaqueHttpStreamResponse').payload.envelope.payload.status=500,
]) { const copy=structuredClone(rows); mutate(copy); reject(copy); }
console.log(`PASS window churn interaction verifier: synthetic positive and ${negatives} negatives`);
