import assert from 'node:assert/strict';
import {mkdtempSync,writeFileSync,readFileSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import vm from 'node:vm';
import {verify} from './transfer-endurance-verify.mjs';

const script=readFileSync(new URL('../Sources/VaadinDesktopHostAcceptanceSupport/FlowIpcTransferEnduranceScript.swift',import.meta.url),'utf8').split('#"""')[1].split('"""#')[0];
new vm.Script(script);
const root=mkdtempSync(join(tmpdir(),'vatori-transfer-verifier-'));
const events=[];
const add=(type,payload,at=0)=>events.push({type,payload,at});
const scopes=[{windowId:'main',sessionId:'a',uiId:'main'},{windowId:'transfer-secondary',sessionId:'b',uiId:'secondary'}];
add('launch',{host:'swift-appkit-wkwebview',transport:'flow-ipc'});
add('standardBundleReady',{requestTransport:'opaque-http-stream-ipc'});add('sidecar.started',{processIdentifier:12});
const wire=(type,id,scope,payload)=>add(type,{envelope:{version:1,type,correlationId:id,...scope,payload:{requestId:id,...payload}}});
for(let batch=0;batch<31;batch++){
  const size=batch===0?268435456:4194304;
  const open=(i,scope,kind)=>{
    const id=`${batch}-${kind}`;
    const raw=kind==='raw0'||kind==='raw1',multi=kind==='multi',resume=kind==='resume';
    wire('opaqueHttpStreamOpen',id,scope,{contentLength:raw?size:multi?-1:0,url:raw||multi?'https://app/upload':`https://app/download?bytes=${size}`,
      headers:raw?{'X-Filename':`owner${i}-${batch}-raw-${kind==='raw0'?0:1}-${size}.bin`}:multi?{'Content-Type':'multipart/form-data; boundary=x'}:resume?{Range:'bytes=262144-'}:{}});
  };
  const finish=(i,scope,kind)=>{
    const id=`${batch}-${kind}`,cancel=kind==='raw0'||kind==='download';
    const upload=kind.startsWith('raw')||kind==='multi';
    wire('opaqueHttpStreamRequestCredit',id,scope,{credit:4});
    wire('opaqueHttpStreamResponseCredit',id,scope,{credit:4});
    if(upload){const n=kind==='raw0'?262144:kind==='multi'?size+256:size;wire('opaqueHttpStreamChunk',id,scope,{chunkSequence:0,bodyBytes:n});if(!cancel)wire('opaqueHttpStreamEnd',id,scope,{chunkCount:1,bodyBytes:n});}
    if(kind!=='raw0')wire('opaqueHttpStreamResponse',id,scope,{status:kind==='resume'?206:200,headers:kind==='resume'?{'Content-Range':[`bytes 262144-${size-1}/${size}`]}:{}});
    if(!upload)wire('opaqueHttpStreamResponseChunk',id,scope,{chunkSequence:0,bodyBytes:kind==='download'?262144:size-262144});
    if(cancel)wire('opaqueHttpStreamCancel',id,scope,{});
    else wire('opaqueHttpStreamResponseEnd',id,scope,{chunkCount:upload?0:1,bodyBytes:upload?0:size-262144});
  };
  for(const [i,scope] of scopes.entries())for(const kind of ['raw0','multi','download'])open(i,scope,kind);
  for(const [i,scope] of scopes.entries()){
    for(const kind of ['raw0','multi','download'])finish(i,scope,kind);
    for(const kind of ['raw1','resume']){open(i,scope,kind);finish(i,scope,kind);}
  }
}
for(const [i,scope] of scopes.entries()){
  let bytes=0;
  for(let batch=0;batch<31;batch++){
    const size=batch===0?268435456:4194304;bytes+=size;
    add('transferEndurance.batch',{...scope,runId:'run',processIdentifier:12,batch,size,owner:`owner${i}`,seed:i,peak:3,started:batch*60000,ended:batch*60000+1000,
      results:[{bytes:size,retry:true,firstProgress:262144},{bytes:size,retry:false},{bytes:size,prefix:262144,resume:true}],
      server:{owner:`owner${i}`,invalid:'0',active:'0',client:'true',raw:String(batch+1),multipart:String(batch+1),cancelled:String(batch+1),ranges:String(batch+1),rawBytes:String(bytes),multipartBytes:String(bytes)}},batch*60000);
  }
  add('transferEndurance.ownerCompleted',{...scope,runId:'run',batches:31,elapsed:1801000,inputMode:'browser-programmatic'},1801000);
  add('transferEndurance.drained',{...scope,runId:'run',status:'owner=x;tempEmpty=true;active=0;'});
}
add('transferEndurance.completed',{runId:'run'});add('sidecar.exit',{processIdentifier:12,status:0,expected:true});add('host.sidecarShutdownCompleted',{succeeded:true});
const samples=Array.from({length:361},(_,i)=>({time:i*5000,processes:[{role:'host',pid:11,rssKiB:1000,descriptors:20},{role:'sidecar',pid:12,rssKiB:2000,descriptors:30}]}));
async function check(data,resources,pass){
  writeFileSync(join(root,'trace'),data.map(e=>'HOST_PROTOCOL '+JSON.stringify(e)).join('\n')+'\n');
  writeFileSync(join(root,'resources'),resources.map(s=>JSON.stringify(s)).join('\n')+'\n');
  if(pass)assert.equal((await verify(join(root,'trace'),join(root,'resources'))).result,'PASS');
  else await assert.rejects(verify(join(root,'trace'),join(root,'resources')));
}
try{
  await check(events,samples,true);
  for(const type of ['launch','standardBundleReady','sidecar.started','transferEndurance.batch','transferEndurance.ownerCompleted','transferEndurance.drained','transferEndurance.completed','sidecar.exit','host.sidecarShutdownCompleted'])await check(events.filter(e=>e.type!==type),samples,false);
  const mutate=async(type,fn)=>{const copy=structuredClone(events);fn(copy.find(e=>e.type===type));await check(copy,samples,false);};
  await mutate('transferEndurance.batch',e=>e.payload.server.invalid='1');
  await mutate('transferEndurance.batch',e=>e.payload.results[2].bytes--);
  await mutate('transferEndurance.batch',e=>e.payload.size=4194304);
  await mutate('transferEndurance.ownerCompleted',e=>e.payload.elapsed=1799999);
  await mutate('opaqueHttpStreamOpen',e=>e.payload.envelope.payload.contentLength=0);
  await check(events,samples.slice(0,300),false);
  const missing=structuredClone(samples);missing[10].processes[0].descriptors=null;await check(events,missing,false);
  console.log('PASS: JS syntax; verifier positive and 16 missing/corrupt evidence negatives (synthetic only)');
}finally{rmSync(root,{recursive:true,force:true});}
