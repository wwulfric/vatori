// Test-only bounded profile verifier. Never interprets request URLs or UIDL bodies.
import assert from 'node:assert/strict';

export function verifyWire(events) {
  const one=(type,predicate=()=>true)=>{
    const found=events.filter(e=>e.type===type&&predicate(e));
    assert.equal(found.length,1,`one ${type}`); assert.equal(found[0].direction,'recv'); return found[0];
  };
  const before=(a,b)=>assert(events.indexOf(a)<events.indexOf(b),'lifecycle order');
  const scope=p=>{
    assert.equal(p.windowId,'main'); assert.equal(p.sessionId,'flow-ipc-session'); assert.equal(p.uiId,'main');
  };
  const envelope=e=>{
    const p=e.payload,v=p.envelope;
    assert(v,'missing envelope'); assert.equal(v.version,1); assert.equal(v.type,e.type);
    assert.equal(v.error,null); scope(v); assert.equal(p.transport,'flow-ipc');
    if(e.direction==='recv') { assert.equal(p.sidecarGeneration,1); assert.equal(p.source,'java-sidecar'); }
    return v;
  };
  const launch=one('launch');
  assert.equal(launch.payload.host,'swift-appkit-wkwebview'); assert.equal(launch.payload.transport,'flow-ipc');
  const started=one('sidecar.started');
  assert.equal(started.payload.generation,1);
  const pid=started.payload.processIdentifier; assert(Number.isSafeInteger(pid)&&pid>0);
  const ready=one('ready',e=>e.direction==='recv');
  const r=envelope(ready).payload;
  for(const [key,value] of Object.entries({runtime:'standard-vaadin-bundle-opaque-ipc',uidlFallback:'none',
    uidlConsumer:'standard-flow-client',uidlRpcProducer:'standard-flow-client',
    browserRequestTransport:'opaque-http-stream-ipc',flowClientResourceLoading:'standard-production-bundle',
    componentFrontendResourceLoading:'standard-production-bundle'})) assert.equal(r[key],value,key);
  const bundle=one('standardBundleReady');
  assert.equal(bundle.payload.requestTransport,'opaque-http-stream-ipc');
  assert.equal(bundle.payload.entry,'vatori-app://app/');
  const client=one('standardBundleResource',e=>/^\/VAADIN\/build\/FlowClient-[^/]+\.js$/.test(e.payload.path));
  assert.equal(client.payload.transport,'custom-scheme-static-file'); assert(client.payload.bytes>0);
  const paint=one('webview.firstMeaningfulPaint'); scope(paint.payload);
  assert.equal(paint.payload.source,'standard-vaadin-production-bundle');
  assert.equal(paint.payload.transport,'opaque-http-stream-ipc'); assert.equal(paint.payload.recoveryGeneration,0);
  const push=one('opaquePushConnected'); envelope(push);
  assert.equal(push.payload.envelope.payload.transport,'opaque-ipc-push');
  const complete=one('lazyDataBaseline.browserRunCompleted');
  assert.equal(complete.payload.processIdentifier,pid);
  const exit=one('sidecar.exit');
  assert.equal(exit.payload.processIdentifier,pid); assert.equal(exit.payload.generation,1);
  assert.equal(exit.payload.status,0); assert.equal(exit.payload.expected,true);
  const shutdown=one('host.sidecarShutdownCompleted'); assert.equal(shutdown.payload.succeeded,true);
  before(launch,started); before(started,ready); before(ready,bundle); before(bundle,client);
  before(client,paint); before(paint,complete); before(ready,push); before(push,complete); before(complete,exit); before(exit,shutdown);
  const requests=new Map(); let ended=0;
  for(const e of events) {
    assert(!/error|failed|failure/i.test(e.type),`unexpected failure event ${e.type}`);
    if(e.type==='lazyDataBaseline.stageObserved') {
      before(paint,e); before(e,complete); assert.equal(e.payload.processIdentifier,pid);
      assert(e.payload.requestCount<=requests.size,'snapshot requests exceed wire');
      assert(e.payload.responseCount<=ended,'snapshot responses exceed completed wire');
    }
    if(!['opaqueHttpStreamOpen','opaqueHttpStreamResponse','opaqueHttpStreamResponseChunk','opaqueHttpStreamResponseEnd'].includes(e.type)) continue;
    before(ready,e); before(e,complete);
    const v=envelope(e),p=v.payload,id=p.requestId;
    assert(typeof id==='string'&&id.length>0); assert.equal(v.correlationId,id);
    if(e.type==='opaqueHttpStreamOpen') {
      assert.equal(e.direction,'send'); assert(!requests.has(id),'duplicate request');
      requests.set(id,{response:false,end:false,chunks:0,bytes:0}); continue;
    }
    assert.equal(e.direction,'recv'); assert.equal(p.transport,'opaque-http-stream-ipc');
    const state=requests.get(id); assert(state,'orphan response'); assert(!state.end,'response after end');
    if(e.type==='opaqueHttpStreamResponse') {
      assert(!state.response,'duplicate response'); assert.equal(p.status,200); state.response=true;
    } else {
      assert(state.response,'body before response');
      assert(Number.isSafeInteger(p.bodyBytes)&&p.bodyBytes>=0);
      if(e.type==='opaqueHttpStreamResponseChunk') {
        assert.equal(p.bodyRedacted,true); assert.equal(p.chunkSequence,state.chunks++); state.bytes+=p.bodyBytes;
      } else {
        assert.equal(p.chunkCount,state.chunks); assert.equal(p.bodyBytes,state.bytes);
        state.end=true; ended++;
      }
    }
  }
  assert(ended>=2); assert.equal(ended,requests.size,'unfinished requests');
  return `PASS: bounded standard-bundle opaque IPC wire (${ended} requests), owner and graceful exit; no push-delivery/native-input claim`;
}
