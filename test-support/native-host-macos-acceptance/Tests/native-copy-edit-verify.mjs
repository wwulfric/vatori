import fs from 'node:fs';
import assert from 'node:assert/strict';
import {pathToFileURL} from 'node:url';

export function verifyCopy(text) {
  const rows = text.split('\n').filter(l => l.startsWith('HOST_PROTOCOL ')).map((l, i) => ({...JSON.parse(l.slice(14)), i}));
  const privateKeys=new Set(['pasteText','clipboardText','originalText','originalSnapshot','originalItems','originalTypes','originalData','originalRepresentations','clipboardData','pasteboardData','snapshotBytes']);
  const privateCheck=value=>{if(value&&typeof value==='object') for(const [k,v] of Object.entries(value)){assert.ok(!privateKeys.has(k),'private clipboard data in trace');privateCheck(v);}};
  rows.filter(r=>r.type.startsWith('nativeCopyEdit.')).forEach(r=>privateCheck(r.payload));
  const all = type => rows.filter(r => r.type === type);
  const one = type => { const a = all(type); assert.equal(a.length, 1, type); return a[0]; };
  const event = name => one('nativeCopyEdit.' + name);
  assert.equal(all('error').length, 0);
  assert.equal(rows.filter(r => /sidecar\.(recovery|productReplacement)|acceptance\.sidecarFailure/.test(r.type)).length, 0);
  assert.equal(all('command').length, 0, 'No Desktop clipboard substitute');
  const accepted = event('frameworkAcceptance'), f = accepted.payload;
  const start = one('sidecar.started'), exit = one('sidecar.exit');
  assert.equal(start.payload.processIdentifier, f.processIdentifier);
  assert.equal(exit.payload.processIdentifier, f.processIdentifier);
  assert.equal(exit.payload.status, 0); assert.equal(exit.payload.expected, true);
  assert.ok(f.hostProcessIdentifier > 0 && f.processIdentifier > 0 && f.hostProcessIdentifier !== f.processIdentifier);
  const frame = r => {
    const p = r.payload;
    for (const [key, value] of Object.entries({windowId:'main', sessionId:'flow-ipc-session', uiId:'main', recoveryGeneration:0,
      hostProcessIdentifier:f.hostProcessIdentifier, processIdentifier:f.processIdentifier, windowNumber:f.windowNumber})) assert.equal(p[key], value, key);
    for (const key of ['contextRetained','windowRetained','webViewRetained']) assert.equal(p[key], true, key);
  };
  const stages = all('nativeCopyEdit.stageObserved'), keys = all('nativeCopyEdit.nativeKeyDispatch');
  assert.deepEqual(stages.map(r=>r.payload.stage), ['initial','inserted','cleared','pasted']);
  assert.deepEqual(keys.map(r=>[r.payload.stage,r.payload.key,r.payload.keyCode,r.payload.modifiers]),
    [['insert','x',7,'none'],['select-all','a',0,'command'],['copy','c',8,'command'],['clear','Backspace',51,'none'],['paste','v',9,'command']]);
  const pointer = event('nativePointerDispatch'), selected = event('selectionObserved'), copied = event('copyObserved');
  const observed = event('pasteboardObserved'), restored = event('pasteboardRestored');
  for (const r of [...stages,...keys,pointer,selected,copied,observed,restored,accepted]) { frame(r); assert.equal(r.direction, 'recv'); }
  for (const r of [...keys,pointer]) {
    for (const key of ['activeApplication','keyWindow','nativeResponderOwned']) assert.equal(r.payload[key], true);
    assert.equal(r.payload.eventCount, 2);
    assert.equal(r.payload.inputMode, r === pointer ? 'appkit-window-pointer-event' : 'appkit-application-key-event');
  }
  const order = [start,stages[0],pointer,keys[0],stages[1],keys[1],selected,keys[2],observed,copied,keys[3],stages[2],keys[4],stages[3],restored,accepted,exit];
  order.slice(1).forEach((r,i)=>assert.ok(order[i].i < r.i, 'causal order'));
  assert.equal(selected.payload.beforeSelectionStart,1); assert.equal(selected.payload.beforeSelectionEnd,1);
  assert.equal(selected.payload.selectionStart,0); assert.equal(selected.payload.selectionEnd,1);
  const changes = ['value=x, length=1, client=true, change=1, oldValue=empty, oldLength=0',
    'value=empty, length=0, client=true, change=2, oldValue=x, oldLength=1',
    'value=x, length=1, client=true, change=3, oldValue=empty, oldLength=0'];
  const snapshot = (r,n) => {
    const p=r.payload, value=n===1||n===3?'x':'';
    assert.equal(p.displayValue,value); assert.equal(p.fieldValue,value);
    assert.equal(p.valueStatus, 'Native edit value: '+(n?changes[n-1]:'value=empty, length=0, client=false, change=0, oldValue=none, oldLength=-1'));
    assert.equal(p.historyStatus,'Native edit history: '+(n?changes.slice(0,n).join(' | '):'pending'));
    assert.deepEqual(p.inputStages,['insert','clear','paste'].slice(0,n));
    assert.deepEqual(p.inputTypes,['insertText','deleteContentBackward','insertFromPaste'].slice(0,n));
    assert.deepEqual(p.inputValues,['x','','x'].slice(0,n));
    assert.equal(p.nativeInputEventCount,n); assert.equal(p.nativeInputTrustedEventCount,n);
    for(const k of ['routeReady','firstMeaningfulPaint','enabled']) assert.equal(p[k],true,k);
    assert.equal(typeof p.ownerFocused,'boolean'); if(n>0) assert.equal(p.ownerFocused,true);
    for(const k of ['invalid','readOnly']) assert.equal(p[k],false,k);
    for(const k of ['browserError','requestError','responseError','pushError','markerError']) assert.equal(p[k],'',k);
    assert.equal(p.flowClientCount,1); assert.equal(p.requestCount,p.responseCount); assert.ok(p.requestCount>0);
    assert.equal(p.nativeKeyboardEventCount,p.nativeKeyboardTrustedEventCount);
    assert.equal(p.nativePointerEventCount,n?1:0); assert.equal(p.nativePointerTrustedEventCount,n?1:0);
  };
  // Initial snapshot precedes the native focus click.
  stages.forEach((r,i)=>snapshot(r,i));
  snapshot(selected,1); snapshot(observed,1); snapshot(copied,1); snapshot(accepted,3);
  assert.ok(copied.payload.copyQuietMilliseconds>=1000 && copied.payload.quietPollCount>=3);
  for(const r of [observed,copied,accepted]) {
    for(const k of ['copySelectedValueMatches','copyTextMatches','copyAdopted']) assert.equal(r.payload[k],true,k);
    assert.equal(r.payload.copyEventCount,1); assert.equal(r.payload.copyTrustedEventCount,1);
    assert.equal(r.payload.copySelectionStart,0); assert.equal(r.payload.copySelectionEnd,1);
    assert.equal(r.payload.copyTextLength,1);
    assert.ok(r.payload.ownedChangeCount>r.payload.capturedChangeCount);
    assert.equal(r.payload.pasteboardContended,false); assert.equal(r.payload.ownershipUncertain,false);
  }
  assert.deepEqual(copied.payload.clipboardEventTypes,['copy']);
  assert.deepEqual(f.clipboardEventTypes,['copy','paste']); assert.deepEqual(f.clipboardTargetOwned,[true,true]);
  assert.equal(f.pasteEventCount,1); assert.equal(f.pasteTrustedEventCount,1); assert.equal(f.pasteTextMatches,true); assert.equal(f.pasteTextLength,1);
  assert.equal(f.nativeKeyDispatchCount,5); assert.ok(f.drainQuietPollCount>=3);
  const keyNames=['insert','select-all','copy','clear','paste'];
  const optional=new Set(['select-all','copy','paste']);
  const seen=new Set(f.keyboardStages);
  assert.ok(seen.has('insert')&&seen.has('clear'));
  assert.deepEqual(f.keyboardStages,keyNames.filter(k=>!optional.has(k)||seen.has(k)));
  assert.deepEqual(f.keyboardKeys,f.keyboardStages.map(k=>({insert:'x','select-all':'a',copy:'c',clear:'Backspace',paste:'v'})[k]));
  assert.equal(f.nativeKeyboardEventCount,f.keyboardStages.length);
  const domOrder=['pointer:focus'];
  for(const k of keyNames){if(seen.has(k))domOrder.push('key:'+k);if(k==='copy'||k==='paste')domOrder.push('clipboard:'+k);if(['insert','clear','paste'].includes(k))domOrder.push('input:'+k);}
  assert.deepEqual(f.domEventOrder,domOrder);assert.deepEqual(f.pointerStages,['focus']);
  assert.deepEqual(f.browserProgrammaticStages,['navigate','position-focus']);
  for(const k of ['complete','standardBundleMode','flowClientOwned','copyObserved','pasteObserved','nativePasteboardCopyObserved','nativePasteboardRestored','snapshotRestored','snapshotVerified']) assert.equal(f[k],true,k);
  for(const k of ['physicalInputClaimed','imeClaimed','assistiveTechnologyClaimed','pushObserved']) assert.equal(f[k],false,k);
  for(const r of [restored,accepted]) {
    for(const k of ['snapshotCaptured','snapshotRestored','snapshotVerified']) assert.equal(r.payload[k],true,k);
    assert.equal(r.payload.pasteboardContended,false); assert.equal(r.payload.ownershipUncertain,false);
    assert.ok(r.payload.restoreAttempts>=1 && r.payload.restoreAttempts<=6);
    assert.equal(r.payload.restoreBeforeChangeCount,observed.payload.ownedChangeCount);
    assert.ok(r.payload.restoredChangeCount>observed.payload.ownedChangeCount);
  }
  const wire=rows.filter(r=>r.type.startsWith('opaque'));
  for(const r of wire) {
    assert.equal(r.payload.transport,'flow-ipc');
    const e=r.payload.envelope; assert.ok(e); assert.equal(e.version,1); assert.equal(e.type,r.type);
    assert.equal(e.windowId,'main'); assert.equal(e.uiId,'main'); assert.equal(e.sessionId,'flow-ipc-session');
  }
  const opens=wire.filter(r=>r.type==='opaqueHttpStreamOpen'), ends=wire.filter(r=>r.type==='opaqueHttpStreamResponseEnd');
  assert.equal(opens.length,ends.length);
  for(const o of opens) assert.equal(ends.filter(e=>e.payload.envelope.correlationId===o.payload.envelope.correlationId&&e.i>o.i).length,1);
  for(const [n,k] of [[1,0],[2,3],[3,4]]) {
    const p=stages[n].payload;
    assert.equal(p.uidlRoundTripObserved,true); assert.ok(p.requestCount>p.requestBaseline && p.responseCount>p.responseBaseline);
    assert.ok(opens.some(o=>o.i>keys[k].i&&o.i<stages[n].i&&o.payload.envelope.payload.method==='POST'
      && /[?&]v-r=uidl(?:&|$)/.test(o.payload.envelope.payload.url)
      && ends.some(e=>e.i>o.i&&e.i<stages[n].i&&e.payload.envelope.correlationId===o.payload.envelope.correlationId&&e.payload.envelope.payload.bodyBytes>0)));
  }
  const drain=one('host.nativeResourceDrainCompleted'); assert.ok(accepted.i<drain.i&&drain.i<exit.i);
  assert.equal(drain.payload.drained,true); assert.equal(drain.payload.pendingCount,0); assert.equal(drain.payload.failureCount,0);
  assert.equal(one('host.sidecarShutdownCompleted').payload.succeeded,true);
  assert.equal(one('launch').payload.transport,'flow-ipc');
  return {status:'PASS',pid:f.processIdentifier,copyPreservedValue:true,restorationVerified:true};
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) console.log(JSON.stringify(verifyCopy(fs.readFileSync(process.argv[2],'utf8')),null,2));
