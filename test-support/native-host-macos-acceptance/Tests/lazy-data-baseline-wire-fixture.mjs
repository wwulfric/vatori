// Generated protocol fixture for verifier tests only; not captured runtime evidence.
export function withWire(stages) {
  const scope={windowId:'main',sessionId:'flow-ipc-session',uiId:'main'};
  const event=(type,payload,direction='recv')=>({type,payload,direction});
  const wire=(type,payload,id='control',direction='recv')=>event(type,{transport:'flow-ipc',
    source:'java-sidecar',sidecarGeneration:1,envelope:{...scope,version:1,type,error:null,
      correlationId:id,payload}},direction);
  const events=[event('launch',{host:'swift-appkit-wkwebview',transport:'flow-ipc'}),
    event('sidecar.started',{generation:1,processIdentifier:123}),
    wire('ready',{runtime:'standard-vaadin-bundle-opaque-ipc',uidlFallback:'none',
      uidlConsumer:'standard-flow-client',uidlRpcProducer:'standard-flow-client',
      browserRequestTransport:'opaque-http-stream-ipc',flowClientResourceLoading:'standard-production-bundle',
      componentFrontendResourceLoading:'standard-production-bundle'}),
    event('standardBundleReady',{requestTransport:'opaque-http-stream-ipc',entry:'vatori-app://app/'}),
    event('standardBundleResource',{path:'/VAADIN/build/FlowClient-test.js',bytes:1,transport:'custom-scheme-static-file'}),
    event('webview.firstMeaningfulPaint',{...scope,source:'standard-vaadin-production-bundle',
      transport:'opaque-http-stream-ipc',recoveryGeneration:0}),
    wire('opaquePushConnected',{transport:'opaque-ipc-push'})];
  let count=0;
  for(const stage of stages) {
    while(count<(stage.payload.requestCount||0)) {
      const id=`request-${++count}`,p={requestId:id,transport:'opaque-http-stream-ipc'};
      events.push(wire('opaqueHttpStreamOpen',p,id,'send'),
        wire('opaqueHttpStreamResponse',{...p,status:200},id),
        wire('opaqueHttpStreamResponseChunk',{...p,chunkSequence:0,bodyRedacted:true,bodyBytes:10},id),
        wire('opaqueHttpStreamResponseEnd',{...p,chunkCount:1,bodyBytes:10},id));
    }
    events.push(stage);
  }
  events.push(event('sidecar.exit',{processIdentifier:123,generation:1,status:0,expected:true}),
    event('host.sidecarShutdownCompleted',{succeeded:true}));
  return events;
}
