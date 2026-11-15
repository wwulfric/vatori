import assert from 'node:assert/strict';
import fs from 'node:fs';
const source = fs.readFileSync(new URL('../Sources/VaadinDesktopHostAcceptanceSupport/FlowIpcStandardBundleFrameworkAcceptance.swift', import.meta.url), 'utf8');
const evaluator = source.slice(source.indexOf('private func evaluateStandardBundleFrameworkState('));
const begin = evaluator.indexOf('const diagnostics =');
const end = evaluator.indexOf('const pushStatus =');
assert.ok(begin >= 0 && end > begin);
const evaluate = new Function('window', 'document', evaluator.slice(begin, end));
const emptyDocument = {querySelectorAll: () => []};
const poll = (window, document = emptyDocument) => evaluate(window, document);
function fixture(clients) {
  const events = [];
  return {events, window: {
    Vaadin: {Flow: {clients}},
    VaadinDesktopIpcDiagnostics: {standardBundlePushMessageCount: 1, standardBundlePushActiveConnectionId: 'c', standardBundlePushLastSequence: 3},
    VaadinDesktopPush: {reconnect: () => events.push('reconnect'), receive: message => events.push(message)}
  }};
}
for (const clients of [{}, {a: {}}, {a: {isActive: () => true}},
  {a: {isActive: () => false}, b: {isActive: () => true}}]) {
  const f = fixture(clients); poll(f.window);
  assert.equal(f.events.length, 0);
  assert.equal(f.window.VaadinDesktopIpcDiagnostics.standardBundlePushReconnectTriggered, undefined);
}
let active = true;
const f = fixture({a: {isActive: () => active}});
poll(f.window); assert.equal(f.events.length, 0);
active = false; poll(f.window);
assert.equal(f.events.length, 2);
assert.equal(f.events[0], 'reconnect');
assert.equal(f.events[1].payload.pushSequence, 5);
assert.equal(f.events[1].payload.resync, false);
poll(f.window); assert.equal(f.events.length, 2, 'inject once');
const callbacks = [[() => {}, () => {}]];
const server = {load: () => {}};
Object.defineProperty(server, 'internal', {value: Object.assign(() => {}, {promises: callbacks})});
const element = {$server: server, localName: 'test-element', id: 'probe'};
const pendingFixture = fixture({a: {isActive: () => false}});
poll(pendingFixture.window, {querySelectorAll: () => [element]});
assert.equal(pendingFixture.events.length, 2, 'diagnostics must not mask in-flight resync');
poll(f.window, {querySelectorAll: () => [element]});
assert.deepEqual(f.window.VaadinDesktopIpcDiagnostics.frameworkPendingRpcOwners,
  [{tag: 'test-element', id: 'probe', methods: ['load'], pending: [0],
    calls: [{index: 0, method: 'unobserved'}]}]);
poll(f.window);
assert.equal(f.window.VaadinDesktopIpcDiagnostics.frameworkPendingRpcOwners.length, 1,
  'retain read-only owner reference after DOM replacement');
assert.equal(server.internal.promises, callbacks);
delete callbacks[0];
poll(pendingFixture.window);
assert.equal(pendingFixture.events.length, 2, 'still inject once');
poll(f.window);
assert.deepEqual(f.window.VaadinDesktopIpcDiagnostics.frameworkPendingRpcOwners, []);
const observed = fixture({a: {isActive: () => true}});
const observedCallbacks = [];
const returned = Object.freeze({then() { throw Error('must not observe result'); }});
const thrown = Error('original failure');
const observedServer = {
  load(value) {
    assert.equal(this, observedServer);
    assert.equal(value, 'argument');
    observedCallbacks.push([() => {}, () => {}]);
    return returned;
  },
  fail() { throw thrown; }
};
Object.defineProperty(observedServer, 'internal', {
  value: Object.assign(() => {}, {promises: observedCallbacks})
});
const observedDocument = {querySelectorAll: () => [{$server: observedServer, localName: 'test', id: ''}]};
poll(observed.window, observedDocument);
const wrapped = observedServer.load;
assert.equal(observedServer.load('argument'), returned);
assert.throws(() => observedServer.fail(), error => error === thrown);
poll(observed.window, observedDocument);
assert.equal(observedServer.load, wrapped, 'install once per server object');
assert.deepEqual(observed.window.VaadinDesktopIpcDiagnostics.frameworkPendingRpcOwners[0].calls,
  [{index: 0, method: 'load'}]);
assert.equal(observedServer.internal.promises, observedCallbacks);
console.log('Framework forced resync waits for official Flow client idle (extracted test script)');
const automation = fs.readFileSync(new URL('../Sources/VaadinDesktopHostAcceptanceSupport/FlowIpcAutomationScripts.swift', import.meta.url), 'utf8');
const observerSource = automation.split('static let source = #"""')[1].split('"""#')[0];
const delivered = [];
const nativeResult = {};
const nativeFailure = Error('native failure');
class ObservedXHR {send(message) {
  assert.equal(this, handler);
  delivered.push(message);
  if (message.fail) throw nativeFailure;
  return nativeResult;
}}
const handler = new ObservedXHR();
const observerWindow = {XMLHttpRequest: class NativeXHR {}};
new Function('window', observerSource)(observerWindow);
observerWindow.XMLHttpRequest = ObservedXHR;
assert.equal(Object.getOwnPropertyDescriptor(observerWindow, 'XMLHttpRequest').value, ObservedXHR);
const request = JSON.stringify({csrfToken: 'secret', rpc: [
  {node: 7, promise: 2, templateEventMethodName: 'confirmUpdate', templateEventMethodArgs: ['secret']}
]});
assert.equal(handler.send(request), nativeResult);
assert.equal(delivered[0], request);
assert.deepEqual(observerWindow.VaadinDesktopIpcDiagnostics.frameworkRpcRequests,
  [{send: 1, node: 7, promise: 2, method: 'confirmUpdate'}]);
assert.equal(handler.send('not-json'), nativeResult);
assert.throws(() => handler.send({fail: true}), error => error === nativeFailure);
for (let i = 0; i < 70; i++) handler.send(request);
assert.equal(observerWindow.VaadinDesktopIpcDiagnostics.frameworkRpcRequests.length, 64);
assert.equal(JSON.stringify(observerWindow.VaadinDesktopIpcDiagnostics).includes('secret'), false);
console.log('Document-start RPC metadata observer preserves delivery and excludes arguments (synthetic)');
for (const deduplicate of [true, false]) {
  const replayFixture = fixture({a: {isActive: () => false}});
  const diagnostics = replayFixture.window.VaadinDesktopIpcDiagnostics;
  const received = [];
  const token = {};
  const original = replayFixture.window.VaadinDesktopPush.receive = function(envelope) {
    if (envelope.payload.resync) {
      received.push(envelope);
      diagnostics.standardBundlePushAckCount = (diagnostics.standardBundlePushAckCount || 0) + 1;
      if (received.length > 1 && deduplicate) diagnostics.standardBundlePushDuplicateCount = 1;
      else {
        diagnostics.standardBundlePushMessageCount++;
        diagnostics.standardBundlePushResyncCount = (diagnostics.standardBundlePushResyncCount || 0) + 1;
      }
    }
    return token;
  };
  poll(replayFixture.window);
  const envelope = {type: 'opaquePush', payload: {resync: true, connectionId: 'c', pushSequence: 4, uidl: 'unchanged'}};
  assert.equal(replayFixture.window.VaadinDesktopPush.receive(envelope), token);
  assert.equal(received.length, 2);
  assert.equal(received[0], envelope);
  assert.equal(received[1], envelope);
  assert.equal(replayFixture.window.VaadinDesktopPush.receive, original);
  assert.equal(diagnostics.frameworkResyncReplay.observed, deduplicate,
    'double client delivery must fail the replay observation');
}
console.log('Resync replay observer rejects double client delivery (synthetic)');
