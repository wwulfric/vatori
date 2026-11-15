import Foundation

// Uses the bundled official Upload client and fetch transport; no custom renderer.
enum FlowIpcTransferEnduranceScript {
    static let source = #"""
    (() => {
      if(window.VatoriTransferEndurance) return true;
      const identity=document.getElementById('transfer-owner');
      if(!identity) return false;
      const owner=identity.textContent, seed=Number(identity.dataset.seed);
      const run=window.VatoriTransferEndurance={started:performance.now(),done:false,error:'',receipts:[],batch:0};
      const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));
      const require=(ok,message)=>{if(!ok)throw Error(message);};
      const raw=document.getElementById('transfer-raw'), multi=document.getElementById('transfer-multipart');
      const small=4*1024*1024, large=256*1024*1024;
      const unit=new Uint8Array(1024*1024);
      for(let i=0;i<unit.length;i++)unit[i]=(i*31+seed)&255;
      const blob=size=>new Blob(Array(size/unit.length).fill(unit),{type:'application/octet-stream'});
      const parse=text=>Object.fromEntries(text.split(';').map(part=>part.split('=')));
      async function wait(predicate) {
        for(let i=0;i<600;i++){if(predicate())return;await sleep(100);}
        throw Error('observation-timeout');
      }
      const active=new Set(); let peak=0;
      const enter=id=>{active.add(id);peak=Math.max(peak,active.size);};
      const leave=id=>active.delete(id);
      async function upload(control,mode,size,batch,cancel) {
        async function attempt(number) {
          return new Promise((resolve,reject)=>{
            const file=new File([blob(size)],`${owner}-${batch}-${mode}-${number}-${size}.bin`,{type:'application/octet-stream'});
            const key=mode+'-'+number; let progress=0,aborting=false;
            const handlers={
              'upload-start':e=>{if(e.detail.file===file)enter(key);},
              'upload-progress':e=>{
                if(e.detail.file!==file)return;
                progress=Math.max(progress,Number(file.loaded||0));
                if(cancel&&number===0&&!aborting&&progress>=256*1024&&progress<size&&peak>=3){
                  aborting=true;control.dispatchEvent(new CustomEvent('file-abort',{detail:{file}}));
                }
              },
              'upload-abort':e=>{if(e.detail.file===file)finish(true);},
              'upload-success':e=>{if(e.detail.file===file)finish(false);},
              'upload-error':e=>{if(e.detail.file===file){cleanup();reject(Error(mode+' upload-error'));}}
            };
            function cleanup(){for(const [name,fn] of Object.entries(handlers))control.removeEventListener(name,fn);leave(key);}
            function finish(aborted){cleanup();control.dispatchEvent(new CustomEvent('file-remove',{detail:{file}}));resolve({aborted,progress});}
            for(const [name,fn] of Object.entries(handlers))control.addEventListener(name,fn);
            control._addFiles([file]);control.uploadFiles(file);
          });
        }
        const first=await attempt(0);
        if(cancel){require(first.aborted&&first.progress>=256*1024&&first.progress<size,'raw-cancellation');await sleep(200);}
        else require(!first.aborted,'multipart-cancelled');
        const result=cancel?await attempt(1):first;
        require(!result.aborted&&(mode==='multipart'?result.progress>=size:result.progress===size),'upload-length/progress');
        return {firstProgress:first.progress,bytes:size,retry:cancel};
      }
      async function download(size) {
        const link=document.getElementById('transfer-download');
        const url=new URL(link.getAttribute('href'),document.baseURI);url.searchParams.set('bytes',String(size));
        enter('download');
        const response=await fetch(url);require(response.status===200,'download-status');
        require(Number(response.headers.get('content-length'))===size,'download-size');
        const reader=response.body.getReader();let prefix=0;
        function check(bytes,start){for(let i=0;i<bytes.length;i++)require(bytes[i]===((start+i)*31+seed&255),'download-content');}
        while(prefix<256*1024){const r=await reader.read();require(!r.done,'short-prefix');check(r.value,prefix);prefix+=r.value.length;}
        await reader.cancel();leave('download');await sleep(200);enter('resume');
        const resumed=await fetch(url,{headers:{Range:`bytes=${prefix}-`}});
        require(resumed.status===206&&resumed.headers.get('content-range')===`bytes ${prefix}-${size-1}/${size}`,'range-response');
        const tail=resumed.body.getReader();let received=prefix;
        while(true){const r=await tail.read();if(r.done)break;check(r.value,received);received+=r.value.length;
          // Pace the consumer, exercising bounded transport backpressure throughout the mixed run.
          await sleep(10);
        }
        leave('resume');require(received===size,'range-exact-length');return {bytes:received,prefix,resume:true};
      }
      (async()=>{
        let bytes=0;
        do {
          const batch=run.batch, size=batch===0?large:small;peak=0;
          const started=performance.now();
          const results=await Promise.all([upload(raw,'raw',size,batch,true),upload(multi,'multipart',size,batch,false),download(size)]);
          require(active.size===0&&peak===3,'three-active-and-drained');bytes+=size;
          document.getElementById('transfer-confirm').click();
          let server;
          await wait(()=>{
            server=parse(document.getElementById('transfer-status').textContent);
            return Number(server.raw)===batch+1&&Number(server.multipart)===batch+1&&Number(server.active)===0
              &&Number(server.rawBytes)===bytes&&Number(server.multipartBytes)===bytes;
          });
          require(server.owner===owner&&server.invalid==='0'&&Number(server.cancelled)===batch+1&&server.client==='true','server-result');
          require(Number(server.ranges)===batch+1,'server-range-count');
          run.receipts.push({batch,size,owner,seed,peak,started,ended:performance.now(),results,server});
          run.batch++;
          require(run.batch<=2000,'bounded-batch-count');
        } while(performance.now()-run.started<30*60*1000);
        run.elapsed=performance.now()-run.started;run.done=true;
      })().catch(e=>{run.error=String(e?.message||e)+'\n'+String(e?.stack||'');run.done=true;});
      return true;
    })()
    """#
}
