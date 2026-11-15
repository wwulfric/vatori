import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync(new URL('../Sources/VaadinDesktopHostAcceptanceSupport/FlowIpcWindowChurnAcceptance.swift', import.meta.url), 'utf8');
const script = source.match(/static let snapshot = """\n([\s\S]*?)\n    """/)?.[1];
assert.ok(script, 'execute the actual Swift-embedded snapshot');
function snapshot({busy = false, connected = true, requests = 3, responses = 3, error = '', clients = true, probe='pending', consumed=false} = {}) {
  const fields = {'window-churn-document':'document-a','window-churn-state':'change=1 client=true',
    'window-churn-probe-result':probe,'window-churn-operation':'pending'};
  return JSON.parse(vm.runInNewContext(script, {
    document:{getElementById:id => fields[id] === undefined ? null : {textContent:fields[id]}},
    window:{VatoriRecoveryPushSettlement:{consumed:()=>consumed},VaadinDesktopIpcDiagnostics:{standardBundleOpaqueRequestCount:requests,
      standardBundleOpaqueResponseCount:responses,standardBundlePushConnected:connected,
      standardBundlePushMessageCount:1,standardBundleBrowserError:error},
      Vaadin:{Flow:{clients:clients ? {test:{isActive:()=>busy}} : {}}}}
  }));
}
const healthy = snapshot();
assert.equal(healthy.settled, true);
assert.equal(healthy.document, 'document-a');
assert.equal(healthy.state, 'change=1 client=true');
assert.equal(healthy.probe, 'pending');
assert.equal(healthy.requestCount, 3);
assert.equal(healthy.pushMessageCount, 1);
for (const options of [{busy:true},{connected:false},{requests:4},{responses:2},{clients:false}])
  assert.equal(snapshot(options).settled, false);
assert.equal(snapshot({error:'visible failure'}).error, 'visible failure');
assert.equal(snapshot({probe:'cycle=0'}).settled, false);
assert.equal(snapshot({probe:'cycle=0',consumed:true}).settled, true);
assert.equal(healthy.consumingAckProven, undefined, 'snapshot cannot certify consuming ACK');
console.log('PASS window churn snapshot: actual embedded script, readiness negatives and evidence boundaries (synthetic only)');
