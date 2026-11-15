import assert from 'node:assert/strict';
import fs from 'node:fs';
import {pathToFileURL} from 'node:url';
import {verifyWindowChurnLifecycle} from './window-churn-lifecycle-verify.mjs';

export function verifyWindowChurnInteractions(text) {
  verifyWindowChurnLifecycle(text);
  const rows = text.split('\n').filter(line => line.startsWith('HOST_PROTOCOL ')).map(line => JSON.parse(line.slice(14)));
  const all = type => rows.filter(row => row.type === type);
  const index = row => rows.indexOf(row);
  const scope = p => JSON.stringify([p.windowId,p.sessionId,p.uiId]);
  const body = row => row.payload.envelope.payload;
  verifyWindowChurnActions(rows);
  const stages = all('windowChurn.stageObserved'), actions = all('windowChurn.action');
  const finals = all('windowChurn.browserRunCompleted');
  assert.equal(stages.length, 33); assert.equal(finals.length, 1);
  const final = finals[0], pid = all('sidecar.started')[0].payload.processIdentifier;
  assert.equal(final.payload.cycles, 10); assert.equal(final.payload.incarnations, 11);
  assert.equal(final.payload.independentGatePassed, false);
  const runId = final.payload.runId;
  assert.ok(typeof runId === 'string' && runId.length > 0);
  for (const row of [...stages,...actions,final]) {
    assert.equal(row.payload.runId, runId); assert.equal(row.payload.processIdentifier, pid);
    assert.equal(row.payload.inputMode, 'browser-programmatic');
  }
  const retained = new Map(), documents = new Set(), windowNumbers = new Set();
  for (let cycle = 0; cycle <= 10; cycle++) {
    const group = stages.slice(cycle*3,cycle*3+3);
    assert.deepEqual(group.map(r => r.payload.windowId), ['main','churn-sibling','churn-secondary']);
    for (const stage of group) {
      const s = stage.payload;
      assert.equal(s.cycle, cycle); assert.equal(s.settled, true); assert.equal(s.error, '');
      assert.equal(s.state, 'change=1 client=true');
      assert.ok(typeof s.document === 'string' && /^[0-9a-f-]{36}$/i.test(s.document));
      assert.ok(Number.isInteger(s.windowNumber) && s.windowNumber > 0);
      assert.ok(Number.isInteger(s.requestCount) && s.requestCount > 0);
      assert.equal(s.requestCount, s.responseCount);
      const creates = all('window.create').filter(r => scope(r.payload) === scope(s));
      assert.equal(creates.length, 1); assert.ok(index(creates[0]) < index(stage));
      const closes = all('windowClosed').filter(r => scope(r.payload) === scope(s));
      assert.equal(closes.length, 1); assert.ok(index(stage) < index(closes[0]));
      if (s.windowId !== 'churn-secondary') {
        assert.equal(s.probe, 'pending');
        const identity = [scope(s),s.document,s.windowNumber];
        if (cycle === 0) retained.set(s.windowId, identity);
        else assert.deepEqual(identity, retained.get(s.windowId), 'sibling identity retained');
      } else {
        assert.equal(s.probe, `cycle=${cycle} before=${cycle} after=${cycle} fresh=true background=true client=true`);
      }
      if (cycle === 0 || s.windowId === 'churn-secondary') {
        assert.ok(!documents.has(s.document)); documents.add(s.document);
        assert.ok(!windowNumbers.has(s.windowNumber)); windowNumbers.add(s.windowNumber);
      }
      const epoch = rows.slice(index(creates[0]),index(stage));
      const wire = type => epoch.filter(r => r.type === type && scope(r.payload.envelope) === scope(s));
      const opens = wire('opaqueHttpStreamOpen'), responses = wire('opaqueHttpStreamResponse'), ends = wire('opaqueHttpStreamResponseEnd');
      assert.equal(opens.length,s.requestCount); assert.equal(ends.length,s.responseCount);
      assert.equal(responses.length,opens.length);
      assert.equal(new Set(opens.map(r => body(r).requestId)).size,opens.length);
      for (const open of opens) {
        const id = body(open).requestId;
        const response = responses.filter(r => body(r).requestId === id), end = ends.filter(r => body(r).requestId === id);
        assert.equal(response.length,1); assert.equal(end.length,1);
        assert.ok(index(open)<index(response[0]) && index(response[0])<index(end[0]));
        assert.ok(body(response[0]).status >= 200 && body(response[0]).status < 400);
      }
      if (s.windowId === 'churn-secondary') {
        const probes = actions.filter(r => scope(r.payload) === scope(s) && r.payload.element === 'window-churn-probe');
        assert.equal(probes.length,1); const probe = probes[0];
        assert.equal(probe.payload.cycle,cycle); assert.ok(index(probe)<index(stage));
        assert.ok(opens.some(r => index(r)>index(probe) && body(r).method === 'POST'));
        const commands = wire('command').filter(r => body(r).command === 'window.setTitle');
        assert.equal(commands.length,1, 'stale calls must not escape to host');
        assert.equal(body(commands[0]).args.title,`Churn owner ${cycle}`);
        assert.ok(index(commands[0])>index(probe));
        const results = wire('capabilityResult').filter(r => r.payload.envelope.correlationId === commands[0].payload.envelope.id);
        assert.equal(results.length,1); assert.equal(body(results[0]).success,true);
        assert.equal(body(results[0]).permissionDecision,'allowed');
        assert.ok(index(results[0])>index(commands[0]));
        const pushes = wire('opaquePush').filter(r => index(r)>index(results[0]));
        assert.ok(pushes.length>0, 'background result must reach Flow push');
        const acks = wire('opaquePushAck');
        for (const push of pushes) {
          const p = body(push); assert.ok(p.uidlBytes>0 && Number.isInteger(p.flowServerSyncId));
          assert.ok(acks.some(ack => index(ack)>index(push) && body(ack).connectionId===p.connectionId
            && body(ack).pushSequence===p.pushSequence && body(ack).flowUiId===p.flowUiId
            && Number.isInteger(body(ack).lastSeenServerSyncId) && body(ack).lastSeenServerSyncId>=p.flowServerSyncId),
          'consuming ACK before observation');
        }
      }
    }
  }
  for (const row of rows.filter(r => r.payload?.envelope)) {
    const e = row.payload.envelope;
    assert.equal(e.version,1); assert.equal(e.type,row.type);
    assert.equal(row.payload.transport,'flow-ipc');
    if (row.direction === 'recv') assert.equal(row.payload.sidecarGeneration,1);
    const inbound=['command','opaqueHttpStreamResponse','opaqueHttpStreamResponseEnd','opaquePush'];
    const outbound=['capabilityResult','opaqueHttpStreamOpen','opaquePushAck'];
    if(inbound.includes(row.type) || outbound.includes(row.type)) {
      const receive=inbound.includes(row.type);
      assert.equal(row.direction,receive?'recv':'send');
      assert.equal(row.payload[receive?'source':'target'],'java-sidecar');
    }
  }
  assert.ok(index(stages.at(-1))<index(final));
  assert.ok(index(final)<index(all('sidecar.exit')[0]));
  return {windows:3,reopenCycles:10,observations:33,input:'browser-programmatic',physicalInput:false};
}

export function verifyWindowChurnActions(rows) {
  const all = type => rows.filter(r => r.type === type);
  const index = r => rows.indexOf(r);
  const scope = p => JSON.stringify([p.windowId,p.sessionId,p.uiId]);
  const body = r => r.payload.envelope.payload;
  const actions = all('windowChurn.action');
  const expected = [['main','window-churn-mutate',0],['main','window-churn-open-churn-sibling',0],
    ['churn-sibling','window-churn-mutate',0]];
  for (let cycle=0;cycle<=10;cycle++) expected.push(
    ['main','window-churn-open-churn-secondary',cycle],['churn-secondary','window-churn-mutate',cycle],
    ['churn-secondary','window-churn-probe',cycle],['main','window-churn-close-churn-secondary',cycle]);
  expected.push(['main','window-churn-close-churn-sibling',10]);
  assert.deepEqual(actions.map(r=>[r.payload.windowId,r.payload.element,r.payload.cycle]),expected,
    'all 48 public actions in profile order');
  assert.equal(all('command').length,35, 'exactly 24 open/close and 11 fresh title commands');
  assert.equal(all('capabilityResult').length,35);
  for (const [i,action] of actions.entries()) {
    const end = actions[i+1] || all('windowChurn.browserRunCompleted')[0];
    assert.ok(end && index(end)>index(action));
    const interval = rows.slice(index(action)+1,index(end));
    const owned = type => interval.filter(r=>r.type===type && scope(r.payload.envelope)===scope(action.payload));
    const posts = owned('opaqueHttpStreamOpen').filter(r=>body(r).method==='POST');
    assert.ok(posts.length>0, 'every action must enter its owner Flow request');
    for (const post of posts) {
      const id=body(post).requestId;
      const response=owned('opaqueHttpStreamResponse').filter(r=>body(r).requestId===id);
      const done=owned('opaqueHttpStreamResponseEnd').filter(r=>body(r).requestId===id);
      assert.equal(response.length,1); assert.equal(done.length,1);
      assert.ok(index(post)<index(response[0]) && index(response[0])<index(done[0]));
      assert.ok(body(response[0]).status>=200 && body(response[0]).status<400);
      for(const r of [post,response[0],done[0]]) assert.equal(r.payload.envelope.correlationId,id);
    }
    const element=action.payload.element;
    const commands=owned('command'), results=owned('capabilityResult');
    if(element==='window-churn-mutate') { assert.equal(commands.length,0); continue; }
    assert.equal(commands.length,1); assert.equal(results.length,1);
    const command=commands[0], result=results[0], c=body(command), r=body(result);
    assert.ok(index(command)<index(result));
    assert.equal(result.payload.envelope.correlationId,command.payload.envelope.id);
    assert.equal(r.success,true); assert.equal(r.permissionDecision,'allowed');
    assert.equal(r.command,c.command);
    if(element==='window-churn-probe') {
      assert.equal(c.command,'window.setTitle'); assert.equal(c.args.title,`Churn owner ${action.payload.cycle}`);
    } else {
      const opening=element.startsWith('window-churn-open-');
      assert.equal(c.command,opening?'window.open':'window.close');
      assert.equal(c.args.id,element.replace(opening?'window-churn-open-':'window-churn-close-',''));
      const native=interval.filter(row=>row.type===(opening?'window.create':'windowClosed') && row.payload.windowId===c.args.id);
      assert.equal(native.length,1); assert.ok(index(command)<index(native[0]));
    }
  }
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  assert.equal(process.argv.length,3);
  console.log('PASS window churn interactions',verifyWindowChurnInteractions(fs.readFileSync(process.argv[2],'utf8')));
}
