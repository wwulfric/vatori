// Independent snapshot semantics plus the separate bounded wire-profile check.
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {pathToFileURL} from 'node:url';
import {verifyWire} from './lazy-data-baseline-wire-verify.mjs';

export function verifySnapshots(events) {
  const stages=events.filter(e=>e.type==='lazyDataBaseline.stageObserved');
  const completed=events.filter(e=>e.type==='lazyDataBaseline.browserRunCompleted');
  assert.equal(stages.length,50,'requires all 50 snapshots');
  assert.equal(completed.length,1,'requires one completion');
  assert(!events.some(e=>e.type==='error'),'trace contains error');
  const pid=stages[0].payload.processIdentifier;
  assert(Number.isSafeInteger(pid)&&pid>0,'positive owner PID');
  const scope=p=>{
    assert.equal(p.processIdentifier,pid);
    assert.equal(p.windowId,'main'); assert.equal(p.uiId,'main');
    assert.equal(p.sessionId,'flow-ipc-session');
    assert.equal(p.inputMode,'browser-programmatic');
  };
  const integer=(o,k)=>{
    assert(/^(0|[1-9][0-9]*)$/.test(String(o[k])),`invalid ${k}`);
    const n=Number(o[k]); assert(Number.isSafeInteger(n)); return n;
  };
  let previousResponse=0, previous;
  for(let i=0;i<50;i++) {
    const p=stages[i].payload, local=i%25, mode=i<25?'exact':'unknown';
    const stage=local<22?(local%2?'tail':'top'):['selected','removed','survivor'][local-22];
    const cycle=local<22?Math.floor(local/2):10, removed=local>=23;
    const ids=Array.from({length:1000},(_,n)=>n+1)
      .filter(id=>(!removed||id!==1)&&(cycle===0||id%11!==cycle))
      .sort((a,b)=>cycle%2?b-a:a-b);
    scope(p); assert.equal(p.index,i); assert.equal(p.mode,mode); assert.equal(p.stage,stage);
    const s=p.status,q=p.query;
    assert.equal(integer(s,'cycle'),cycle); assert.equal(integer(q,'cycle'),cycle);
    assert.equal(integer(s,'total'),removed?999:1000); assert.equal(integer(s,'matches'),ids.length);
    assert.equal(integer(s,'removals'),removed?1:0); assert.equal(p.removedId,removed?1:0);
    assert.equal(integer(s,'selections'),local<22?0:local-21);
    assert.deepEqual(JSON.parse(s.selected),stage==='selected'?[1]:stage==='survivor'?[2]:[]);
    assert.equal(s.client,cycle===0?'false':'true');
    assert.equal(p.selectionClient,local<22?null:stage==='removed'?'false':'true');
    const fetches=integer(s,'fetches'),counts=integer(s,'counts');
    assert(fetches>integer(s,'fetchBaseline'),'fresh fetch');
    if(mode==='exact') assert(counts>integer(s,'countBaseline'),'fresh count');
    else { assert.equal(counts,0); assert.equal(integer(s,'countBaseline'),0); }
    if(local>0) {
      assert(fetches>=integer(previous.status,'fetches')); assert(counts>=integer(previous.status,'counts'));
      if((stage==='top'&&cycle>0)||stage==='removed') {
        assert(integer(s,'fetchBaseline')>=integer(previous.status,'fetches'),'fresh action fetch baseline');
        assert(integer(s,'countBaseline')>=integer(previous.status,'counts'),'fresh action count baseline');
      }
    }
    assert.equal(q.direction,cycle%2?'desc':'asc');
    const offset=integer(q,'offset'),limit=integer(q,'limit');
    assert.equal(limit,50,'page size');
    assert(offset%50===0&&offset<=Math.ceil(ids.length/50)*50,'bounded page offset');
    const queryIds=JSON.parse(q.ids);
    assert.deepEqual(queryIds,ids.slice(offset,offset+limit),'query IDs/order');
    assert.equal(integer(q,'returned'),queryIds.length);
    assert(Array.isArray(p.visible)&&p.visible.length>0,'visible rows');
    const first=ids.indexOf(p.visible[0]); assert(first>=0);
    assert.deepEqual(p.visible,ids.slice(first,first+p.visible.length),'visible contiguous order');
    if(stage==='tail') { assert.equal(p.visible.at(-1),ids.at(-1)); assert.equal(p.size,ids.length); }
    else assert.equal(p.visible[0],ids[0]);
    assert(Number.isSafeInteger(p.size)&&p.size>0&&p.size<=ids.length);
    if(mode==='exact') assert.equal(p.size,ids.length);
    assert(Number.isSafeInteger(p.requestCount)&&p.requestCount>0);
    assert.equal(p.responseCount,p.requestCount,'settled IPC counters');
    assert(p.responseCount>=previousResponse,'IPC counters monotonic');
    if(i>0&&local!==0) assert(p.responseCount>previousResponse,'action IPC roundtrip');
    previousResponse=p.responseCount; previous=p;
  }
  scope(completed[0].payload);
  assert.equal(completed[0].payload.receiptCount,50);
  assert.equal(completed[0].payload.independentGatePassed,false);
  assert(events.indexOf(completed[0])>events.indexOf(stages.at(-1)),'completion after snapshots');
  return 'PASS: 50 lazy-data snapshots; browser-programmatic semantics, not native-input evidence';
}

export function readEvents(path) {
  return readFileSync(path,'utf8').split('\n').filter(l=>l.startsWith('HOST_PROTOCOL '))
    .map(l=>JSON.parse(l.slice('HOST_PROTOCOL '.length)));
}
if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href) {
  try { assert.equal(process.argv.length,3,'usage: node lazy-data-baseline-verify.mjs TRACE');
    const events=readEvents(process.argv[2]);
    const snapshots=verifySnapshots(events),wire=verifyWire(events);
    console.log(snapshots); console.log(wire);
  } catch(error) { console.error(`FAIL: ${error.message}`); process.exitCode=1; }
}
