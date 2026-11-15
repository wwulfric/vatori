import fs from 'node:fs';
import assert from 'node:assert/strict';
import {verifyCopy} from './native-copy-edit-verify.mjs';
const text=fs.readFileSync(process.argv[2],'utf8');
verifyCopy(text);
const rows=text.split('\n').filter(l=>l.startsWith('HOST_PROTOCOL ')).map(l=>JSON.parse(l.slice(14)));
const find=(r,n)=>r.find(x=>x.type==='nativeCopyEdit.'+n).payload;
const edits=[
 r=>{find(r,'copyObserved').copyQuietMilliseconds=999;},
 r=>{find(r,'copyObserved').fieldValue='';},
 r=>{find(r,'copyObserved').historyStatus='changed';},
 r=>{find(r,'copyObserved').copyTrustedEventCount=0;},
 r=>{find(r,'pasteboardObserved').ownedChangeCount=find(r,'pasteboardObserved').capturedChangeCount;},
 r=>{find(r,'pasteboardRestored').snapshotRestored=false;},
 r=>{find(r,'frameworkAcceptance').snapshotVerified=false;},
 r=>{find(r,'frameworkAcceptance').pasteboardContended=true;},
 r=>{find(r,'frameworkAcceptance').pasteTextMatches=false;},
 r=>{find(r,'frameworkAcceptance').keyboardStages=[];},
 r=>{find(r,'frameworkAcceptance').originalText='forbidden';},
 r=>{r.find(x=>x.type==='opaqueHttpStreamOpen').payload.envelope.sessionId='foreign';},
 r=>{r.find(x=>x.type==='opaqueHttpStreamResponseEnd').type='missing';},
 r=>{r.find(x=>x.type==='sidecar.exit').payload.status=9;},
 r=>{r.find(x=>x.type==='host.nativeResourceDrainCompleted').payload.pendingCount=1;},
 r=>{find(r,'frameworkAcceptance').physicalInputClaimed=true;},
];
for(const [i,edit] of edits.entries()){const changed=structuredClone(rows);edit(changed);assert.throws(()=>verifyCopy(changed.map(r=>'HOST_PROTOCOL '+JSON.stringify(r)).join('\n')),undefined,'mutation '+i);}
console.log(`Copy verifier: retained real positive and ${edits.length} negative mutations passed; no clipboard I/O`);
