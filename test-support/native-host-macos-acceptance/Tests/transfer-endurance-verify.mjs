import assert from 'node:assert/strict';
import {createReadStream,readFileSync} from 'node:fs';
import {createInterface} from 'node:readline';
import {pathToFileURL} from 'node:url';

export async function verify(path,resourcePath){
  const requests=new Map(),owners=new Map(),completed=new Set(),drained=new Set();
  const live=new Map(),max=new Map(),firstTransfer=new Map();let simultaneous=false,runId,pid,launch=false,bundle=false,exited=false,shutdown=false,terminal=false;
  const scope=e=>`${e.windowId}/${e.sessionId}/${e.uiId}`;
  const integer=n=>assert(Number.isSafeInteger(n)&&n>=0,'nonnegative integer');
  for await(const line of createInterface({input:createReadStream(path),crlfDelay:Infinity})){
    if(!line.startsWith('HOST_PROTOCOL '))continue;
    const e=JSON.parse(line.slice(14)),p=e.payload;
    if(e.type==='launch'){assert.equal(p.transport,'flow-ipc');assert.equal(p.host,'swift-appkit-wkwebview');launch=true;}
    if(e.type==='standardBundleReady'){assert.equal(p.requestTransport,'opaque-http-stream-ipc');bundle=true;}
    if(e.type==='sidecar.started'){assert(!pid,'unexpected replacement');pid=p.processIdentifier;assert(pid>0);}
    if(e.type==='error')throw Error(JSON.stringify(p));
    if(e.type==='sidecar.exit'){assert.equal(p.processIdentifier,pid);assert.equal(p.status,0);assert.equal(p.expected,true);exited=true;}
    if(e.type==='host.sidecarShutdownCompleted'){assert.equal(p.succeeded,true);shutdown=true;}
    if(e.type.startsWith('transferEndurance.')){
      runId??=p.runId;assert.equal(p.runId,runId);
      const key=scope(p);
      if(e.type==='transferEndurance.batch'){
        assert.equal(p.processIdentifier,pid);assert(!completed.has(key));
        let state=owners.get(key);
        if(!state){state={count:0,bytes:0,first:e.at,last:e.at,owner:p.owner,seed:p.seed};owners.set(key,state);}
        assert.equal(p.batch,state.count);assert.equal(p.owner,state.owner);assert.equal(p.seed,state.seed);
        const size=state.count===0?256*1024*1024:4*1024*1024;
        assert.equal(p.size,size);assert.equal(p.peak,3);assert(p.ended>p.started);
        assert(e.at-state.last<180000,'workload stalled');state.last=e.at;state.count++;state.bytes+=size;
        assert.equal(p.results.length,3);
        for(const result of p.results)assert.equal(result.bytes,size);
        assert.equal(p.results[0].retry,true);assert(p.results[0].firstProgress>=262144&&p.results[0].firstProgress<size);
        assert.equal(p.results[1].retry,false);assert.equal(p.results[2].resume,true);assert(p.results[2].prefix>=262144&&p.results[2].prefix<size);
        const s=p.server;
        assert.equal(s.owner,state.owner);assert.equal(s.invalid,'0');assert.equal(s.active,'0');assert.equal(s.client,'true');
        for(const field of ['raw','multipart','cancelled','ranges'])assert.equal(Number(s[field]),state.count,field);
        for(const field of ['rawBytes','multipartBytes'])assert.equal(Number(s[field]),state.bytes,field);
      }
      if(e.type==='transferEndurance.ownerCompleted'){
        const state=owners.get(key);assert(state);assert(!completed.has(key));assert.equal(p.batches,state.count);
        assert(p.elapsed>=1800000);assert(e.at-firstTransfer.get(key)>=1800000,'independent sustained wall time');
        assert.equal(p.inputMode,'browser-programmatic');completed.add(key);
      }
      if(e.type==='transferEndurance.drained'){assert(completed.has(key));assert(p.status.includes(';tempEmpty=true;'));assert(p.status.includes(';active=0;'));drained.add(key);}
      if(e.type==='transferEndurance.completed'){assert.equal(completed.size,2);assert.equal(drained.size,2);terminal=true;}
    }
    if(!e.type.startsWith('opaqueHttpStream'))continue;
    const v=p.envelope;assert(v,'full envelope required');assert.equal(v.version,1);assert.equal(v.type,e.type);
    const b=v.payload,id=b.requestId,key=scope(v);assert(id);assert.equal(v.correlationId,id);
    const requestKey=key+'/'+id;
    if(e.type==='opaqueHttpStreamOpen'){
      assert(!requests.has(requestKey));
      const headers=Object.fromEntries(Object.entries(b.headers||{}).map(([k,v])=>[k.toLowerCase(),v]));
      const transfer=Number(b.contentLength)>=4*1024*1024||String(headers['content-type']||'').startsWith('multipart/form-data')||/[?&]bytes=(4194304|268435456)(?:&|$)/.test(b.url||'');
      requests.set(requestKey,{key,transfer,headers,url:b.url||'',length:Number(b.contentLength),req:0,res:0,rseq:0,sseq:0,requestCredit:0,responseCredit:0,end:false,cancel:false});
      if(transfer){if(!firstTransfer.has(key))firstTransfer.set(key,e.at);live.set(key,(live.get(key)||0)+1);max.set(key,Math.max(max.get(key)||0,live.get(key)));if([...live.values()].filter(n=>n>=3).length===2)simultaneous=true;}
      continue;
    }
    const s=requests.get(requestKey);assert(s,'orphan stream');assert.equal(s.key,key,'scope contamination');
    if(e.type==='opaqueHttpStreamResponse'){
      assert(!s.response,'duplicate response');s.response=true;s.status=b.status;
      s.responseHeaders=Object.fromEntries(Object.entries(b.headers||{}).map(([k,v])=>[k.toLowerCase(),Array.isArray(v)?v.join(','):v]));
    }
    if(e.type==='opaqueHttpStreamRequestCredit'){integer(b.credit);s.requestCredit+=b.credit;}
    if(e.type==='opaqueHttpStreamResponseCredit'){integer(b.credit);s.responseCredit+=b.credit;}
    if(e.type==='opaqueHttpStreamChunk'){assert(s.requestCredit-->0,'request exceeded credit');assert.equal(b.chunkSequence,s.rseq++);integer(b.bodyBytes);s.req+=b.bodyBytes;}
    if(e.type==='opaqueHttpStreamResponseChunk'){assert(s.responseCredit-->0,'response exceeded credit');assert.equal(b.chunkSequence,s.sseq++);integer(b.bodyBytes);s.res+=b.bodyBytes;}
    if(e.type==='opaqueHttpStreamEnd'){assert.equal(b.chunkCount,s.rseq);assert.equal(b.bodyBytes,s.req);}
    if(e.type==='opaqueHttpStreamResponseEnd'){assert.equal(b.chunkCount,s.sseq);assert.equal(b.bodyBytes,s.res);}
    if(['opaqueHttpStreamCancel','opaqueHttpStreamResponseEnd','opaqueHttpStreamError'].includes(e.type)&&!s.end){
      if(e.type==='opaqueHttpStreamError')assert(s.cancel,'unexpected stream error');
      s.cancel=e.type==='opaqueHttpStreamCancel';s.end=true;if(s.transfer)live.set(key,live.get(key)-1);
    }
  }
  assert(launch&&bundle&&pid&&terminal&&exited&&shutdown,'complete real lifecycle');
  assert.equal(owners.size,2);assert.equal(new Set([...owners.values()].map(s=>s.owner)).size,2);
  assert(simultaneous,'two windows each three simultaneously active transfer requests');
  for(const s of requests.values())assert(s.end,'undrained request');
  for(const [key,owner] of owners){
    const transfers=[...requests.values()].filter(s=>s.key===key&&s.transfer);
    const raw=transfers.filter(s=>s.headers['x-filename']);
    const multipart=transfers.filter(s=>String(s.headers['content-type']).startsWith('multipart/form-data'));
    const downloads=transfers.filter(s=>/[?&]bytes=/.test(s.url));
    assert.equal(raw.length,owner.count*2,'fresh retry request count');
    assert.equal(multipart.length,owner.count);assert.equal(downloads.length,owner.count*2);
    for(let batch=0;batch<owner.count;batch++){
      const size=batch===0?268435456:4194304;
      for(let attempt=0;attempt<2;attempt++){
        const rows=raw.filter(s=>s.headers['x-filename']===`${owner.owner}-${batch}-raw-${attempt}-${size}.bin`);
        assert.equal(rows.length,1);const s=rows[0];assert.equal(s.length,size);
        if(attempt===0){assert(s.cancel&&s.req>0&&s.req<size,'raw cancellation on wire');}
        else {assert(!s.cancel);assert.equal(s.req,size);assert.equal(s.status,200);}
      }
      const m=multipart[batch];assert(!m.cancel);assert(m.req>size&&m.req<size+65536,'multipart encoded length');assert.equal(m.status,200);
      const pair=downloads.slice(batch*2,batch*2+2),initial=pair[0],resumed=pair[1];
      assert(initial.cancel);assert.equal(initial.status,200);assert(initial.res>0&&initial.res<size);
      assert(!resumed.cancel);assert.equal(resumed.status,206);
      const range=String(resumed.headers.range).match(/^bytes=(\d+)-$/);assert(range);
      const prefix=Number(range[1]);assert(prefix>0&&prefix<size);
      assert.equal(resumed.res,size-prefix);assert.equal(resumed.responseHeaders['content-range'],`bytes ${prefix}-${size-1}/${size}`);
    }
  }
  const samples=readFileSync(resourcePath,'utf8').trim().split('\n').map(JSON.parse);
  const trend={};
  for(const role of ['host','sidecar']){
    const values=samples.flatMap(s=>s.processes.filter(p=>p.role===role).map(p=>({...p,time:s.time})));
    assert(values.length>=300);assert(values.at(-1).time-values[0].time>=1800000);assert.equal(new Set(values.map(v=>v.pid)).size,1);
    assert(values.every(v=>v.rssKiB>0&&Number.isInteger(v.descriptors)&&v.descriptors>0));
    for(let i=1;i<values.length;i++)assert(values[i].time-values[i-1].time<20000,'resource sampling gap');
    trend[role]={samples:values.length,first:values[0],last:values.at(-1),peakRssKiB:Math.max(...values.map(v=>v.rssKiB)),peakDescriptors:Math.max(...values.map(v=>v.descriptors))};
  }
  return {result:'PASS',owners:[...owners.values()],trend};
}
if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href){
  verify(process.argv[2],process.argv[3]).then(r=>console.log(JSON.stringify(r,null,2))).catch(e=>{console.error(e);process.exitCode=1;});
}
