import assert from 'node:assert/strict';
import {readFileSync,mkdtempSync,writeFileSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join,resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
import {verifySnapshots} from './lazy-data-baseline-verify.mjs';
import {verifyWire} from './lazy-data-baseline-wire-verify.mjs';
import {withWire} from './lazy-data-baseline-wire-fixture.mjs';

// Synthetic contract tests, not retained evidence from a product run.
const scope={processIdentifier:123,windowId:'main',uiId:'main',sessionId:'flow-ipc-session',inputMode:'browser-programmatic'};
const events=Array.from({length:50},(_,index)=>{
  const local=index%25,cycle=Math.min(10,Math.floor(local/2));
  const mode=index<25?'exact':'unknown',removed=local>=23;
  const stage=local<22?(local%2?'tail':'top'):['selected','removed','survivor'][local-22];
  const ids=[];
  for(let id=1;id<=1000;id++) if((!removed||id!==1)&&(!cycle||id%11!==cycle)) ids.push(id);
  if(cycle%2) ids.reverse();
  const offset=stage==='tail'?Math.floor((ids.length-1)/50)*50:0;
  const queryIds=ids.slice(offset,offset+50);
  return {type:'lazyDataBaseline.stageObserved',direction:'recv',payload:{...scope,index,mode,stage,
    requestCount:index+3,responseCount:index+3,size:ids.length,removedId:removed?1:0,
    visible:stage==='tail'?ids.slice(-8):ids.slice(0,8),
    selectionClient:local<22?null:stage==='removed'?'false':'true',
    status:{cycle:String(cycle),total:removed?'999':'1000',matches:String(ids.length),
      removals:removed?'1':'0',selections:String(local<22?0:local-21),
      selected:JSON.stringify(stage==='selected'?[1]:stage==='survivor'?[2]:[]),
      client:cycle===0?'false':'true',fetches:String(local+1),fetchBaseline:String(local),
      counts:mode==='exact'?String(local+1):'0',countBaseline:mode==='exact'?String(local):'0'},
    query:{cycle:String(cycle),direction:cycle%2?'desc':'asc',offset:String(offset),limit:'50',
      ids:JSON.stringify(queryIds),returned:String(queryIds.length)}}};
});
events.push({type:'lazyDataBaseline.browserRunCompleted',direction:'recv',payload:{...scope,receiptCount:50,independentGatePassed:false}});
assert.match(verifySnapshots(events),/^PASS:/);
let negatives=0;
function rejects(edit) { const copy=structuredClone(events);edit(copy);assert.throws(()=>verifySnapshots(copy));negatives++; }
rejects(e=>e.splice(42,8)); rejects(e=>e.pop()); rejects(e=>e.push(e.at(-1)));
rejects(e=>e.unshift(e.pop())); rejects(e=>e.push({type:'error'}));
rejects(e=>{e[2].payload.status.fetchBaseline='0';});
rejects(e=>{e[23].payload.status.countBaseline='0';});
rejects(e=>{e[2].payload.responseCount=e[2].payload.requestCount=e[1].payload.responseCount;});
rejects(e=>{e[0].payload.visible=[1,1];});
rejects(e=>{e[0].payload.visible=[1,3];});
rejects(e=>{e[0].payload.visible=[2,1];});
rejects(e=>{e[1].payload.visible=[990,991];});
rejects(e=>{e[0].payload.query.offset='5000';e[0].payload.query.ids='[]';e[0].payload.query.returned='0';});
for(let i=0;i<50;i++) {
  for(const [key,value] of Object.entries({index:-1,mode:'wrong',stage:'wrong',processIdentifier:124,
    inputMode:'native',windowId:'other',sessionId:'other',uiId:'other',size:0,requestCount:0,
    responseCount:-1,visible:[1001],removedId:9,selectionClient:'invalid'})) {
    rejects(e=>{e[i].payload[key]=value;});
  }
  for(const key of ['cycle','total','matches','removals','selections','selected','client','fetches','fetchBaseline','counts','countBaseline']) {
    rejects(e=>{e[i].payload.status[key]='invalid';});
  }
  for(const key of ['cycle','direction','offset','limit','ids','returned']) {
    rejects(e=>{e[i].payload.query[key]='invalid';});
  }
}
console.log(`PASS: lazy-data snapshot verifier, 1 synthetic positive and ${negatives} negatives`);
const protocol=withWire(events);
assert.match(verifyWire(protocol),/^PASS:/);
let wireNegatives=0;
function rejectsWire(edit) { const copy=structuredClone(protocol);edit(copy);assert.throws(()=>verifyWire(copy));wireNegatives++; }
for(let i=0;i<protocol.length;i++) {
  const type=protocol[i].type;
  if(type!=='lazyDataBaseline.stageObserved') rejectsWire(e=>e.splice(i,1));
  if(type.startsWith('opaqueHttpStream')) {
    rejectsWire(e=>{e[i].payload.envelope.correlationId='wrong';});
    rejectsWire(e=>{e[i].payload.envelope.sessionId='wrong';});
    rejectsWire(e=>{e[i].payload.envelope.version=2;});
    rejectsWire(e=>{e[i].direction='wrong';});
    if(type!=='opaqueHttpStreamOpen') rejectsWire(e=>{e[i].payload.sidecarGeneration=2;});
    rejectsWire(e=>e.splice(i,0,structuredClone(e[i])));
  }
  if(type==='opaqueHttpStreamResponse') rejectsWire(e=>{e[i].payload.envelope.payload.status=500;});
  if(type==='opaqueHttpStreamResponseChunk') {
    rejectsWire(e=>{e[i].payload.envelope.payload.chunkSequence=1;});
    rejectsWire(e=>{e[i].payload.envelope.payload.bodyBytes=9;});
    rejectsWire(e=>{e[i].payload.envelope.payload.bodyRedacted=false;});
  }
  if(type==='opaqueHttpStreamResponseEnd') rejectsWire(e=>{e[i].payload.envelope.payload.chunkCount=2;});
}
rejectsWire(e=>{e.find(e=>e.type==='sidecar.exit').payload.status=1;});
rejectsWire(e=>{e.find(e=>e.type==='sidecar.exit').payload.processIdentifier=124;});
rejectsWire(e=>{e.find(e=>e.type==='lazyDataBaseline.stageObserved').payload.responseCount=100000;});
rejectsWire(e=>e.push({type:'opaqueHttpStreamError'}));
rejectsWire(e=>{
  const a=e.findIndex(e=>e.type==='opaqueHttpStreamResponse'),b=e.findIndex(e=>e.type==='opaqueHttpStreamResponseEnd');
  [e[a],e[b]]=[e[b],e[a]];
});
rejectsWire(e=>{
  const a=e.findIndex(e=>e.type==='sidecar.exit'),b=e.findIndex(e=>e.type==='lazyDataBaseline.browserRunCompleted');
  [e[a],e[b]]=[e[b],e[a]];
});
console.log(`PASS: wire verifier, 1 synthetic positive and ${wireNegatives} negatives`);

// Execute the actual runner branch against generated traces, including its exit handling.
const root=resolve(fileURLToPath(new URL('../../../',import.meta.url)));
const runner=readFileSync(join(root,'test-support/native-host-macos-acceptance/bin/run'),'utf8');
const branch=runner.match(/elif \[\[ "\$\{VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA_BASELINE:-\}" == "1" \]\]; then\n([\s\S]*?)\n  elif/);
assert(branch,'lazy-data runner branch');
const temp=mkdtempSync(join(tmpdir(),'vatori-lazy-verifier-'));
try {
  const trace=join(temp,'trace.log');
  for(const [source,expected] of [[protocol,0],[withWire(events.slice(0,42)),1],[events,1]]) {
    writeFileSync(trace,source.map(e=>'HOST_PROTOCOL '+JSON.stringify(e)).join('\n'));
    const result=spawnSync('bash',['-c',`status=0\n${branch[1]}\nexit "$status"`],
      {env:{...process.env,ROOT:root,host_log:trace},encoding:'utf8'});
    assert.equal(result.status,expected,result.stderr);
  }
} finally { rmSync(temp,{recursive:true,force:true}); }
console.log('PASS: runner branch accepts complete snapshots and rejects an incomplete trace');
