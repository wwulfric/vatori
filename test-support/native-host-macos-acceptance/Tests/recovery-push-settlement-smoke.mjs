import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const swift = readFileSync(new URL('../Sources/VaadinDesktopHostAcceptanceSupport/FlowIpcRecoveryPushSettlement.swift', import.meta.url), 'utf8');
const source = swift.match(/static let source = """\n([\s\S]*?)\n    """/)?.[1];
assert.ok(source);
let calls = 0;
const diagnostics = { standardBundlePushActiveConnectionId: 'connection-1',
  standardBundlePushLastAckSequence: 1, standardBundlePushLastAckServerSyncId: 1 };
const receiver = { receive(envelope) { calls++; assert.equal(this, receiver); return envelope; } };
const page = vm.createContext({ VaadinDesktopPush: receiver, VaadinDesktopIpcDiagnostics: diagnostics });
page.window = page;
vm.runInContext(source, page);
const consumed = () => page.VatoriRecoveryPushSettlement.consumed();
assert.equal(consumed(), false, 'no observed push is not settlement');
const push = { type: 'opaquePush', payload: { pushSequence: 1, flowServerSyncId: 3, connectionId: 'connection-1' } };
assert.equal(receiver.receive(push), push, 'forward original envelope and return value');
for (let i = 0; i < 1000; i++) assert.equal(consumed(), false, 'time/poll count cannot settle receipt-only ACK');
diagnostics.standardBundlePushLastAckServerSyncId = 3;
assert.equal(consumed(), true);
diagnostics.standardBundlePushLastAckSequence = 2;
assert.equal(consumed(), false, 'wrong sequence');
diagnostics.standardBundlePushLastAckSequence = 1;
diagnostics.standardBundlePushActiveConnectionId = 'foreign';
assert.equal(consumed(), false, 'wrong connection');
diagnostics.standardBundlePushActiveConnectionId = 'connection-1';
const installed = receiver.receive;
vm.runInContext(source, page);
assert.equal(receiver.receive, installed, 'idempotent install does not clear pending evidence');
assert.equal(consumed(), true);
receiver.receive({ type: 'opaquePush', payload: { ...push.payload, pushSequence: 2, flowServerSyncId: 4 } });
assert.equal(consumed(), false, 'next push needs its own consuming ACK');
diagnostics.standardBundlePushLastAckSequence = 2;
diagnostics.standardBundlePushLastAckServerSyncId = 4;
assert.equal(consumed(), true);
receiver.receive({ type: 'opaquePush', payload: { ...push.payload, flowServerSyncId: '3' } });
assert.equal(consumed(), false, 'malformed metadata fails closed');
assert.equal(calls, 3, 'observation never generates extra delivery or ACK');

// Exercise the actual product bridge unchanged. Only the official client's
// asynchronous processing and native transport are represented by test doubles.
const bridge = readFileSync(new URL('../../../native-hosts/macos/Resources/FlowIpcPushBridge.js', import.meta.url), 'utf8');
const sent = [];
let lastSeen = 1, delivered = 0;
const integrated = vm.createContext({ URL, document: { baseURI: 'https://fixture.invalid/' },
  webkit: { messageHandlers: { flowIpc: { postMessage: message => sent.push(message) } } } });
integrated.window = integrated;
vm.runInContext(bridge, integrated);
integrated.vaadinPush.atmosphere.subscribe({ url: '/push?v-uiId=0&v-pushId=test',
  headers: { 'X-Vaadin-LastSeenServerSyncId': () => lastSeen },
  onMessage: () => { delivered++; } });
vm.runInContext(source, integrated);
const actualPush = integrated.VaadinDesktopPush;
actualPush.receive({ version: 1, type: 'opaquePushConnected', payload: { connectionId: 'opaque-push-1' } });
const delivery = { version: 1, type: 'opaquePush', payload: { connectionId: 'opaque-push-1',
  pushSequence: 1, flowServerSyncId: 3, deliveryAttempt: 1, uidl: 'opaque payload' } };
actualPush.receive(delivery);
assert.equal(sent.at(-1).type, 'opaquePushAck');
assert.equal(sent.at(-1).payload.lastSeenServerSyncId, 1);
assert.equal(integrated.VatoriRecoveryPushSettlement.consumed(), false);
lastSeen = 3;
assert.equal(integrated.VatoriRecoveryPushSettlement.consumed(), false,
  'client consumption alone is insufficient before a consuming ACK is sent');
actualPush.receive({ ...delivery, payload: { ...delivery.payload, deliveryAttempt: 2 } });
assert.equal(sent.at(-1).payload.lastSeenServerSyncId, 3);
assert.equal(integrated.VatoriRecoveryPushSettlement.consumed(), true);
assert.equal(delivered, 1, 'the product bridge suppresses duplicate delivery');
assert.equal(sent.filter(message => message.type === 'opaquePushAck').length, 2);
console.log('Recovery push settlement smoke passed: metadata negatives and actual bridge with delayed-client/native-transport doubles (no real Flow/OS claim)');
