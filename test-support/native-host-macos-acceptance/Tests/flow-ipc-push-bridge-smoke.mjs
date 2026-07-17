import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const testDirectory = dirname(fileURLToPath(import.meta.url));
const bridgePath = resolve(
  testDirectory,
  '../../..',
  'native-hosts/macos/Resources/FlowIpcPushBridge.js'
);
const bridgeSource = await readFile(bridgePath, 'utf8');
const messages = [];
const receivedBodies = [];
let opened = 0;
let reopened = 0;
let reconnecting = 0;

const context = {
  URL,
  Date,
  Math,
  console,
  VaadinDesktopHostContext: {
    windowId: 'secondary',
    sessionId: 'flow-ipc-session-secondary',
    uiId: 'secondary'
  },
  document: { baseURI: 'https://app.vatori.invalid/' },
  webkit: {
    messageHandlers: {
      flowIpc: {
        postMessage: (message) => messages.push(message)
      }
    }
  }
};
context.window = context;
vm.runInNewContext(bridgeSource, context, { filename: bridgePath });

const socket = context.vaadinPush.atmosphere.subscribe({
  url: 'https://app.vatori.invalid/VAADIN/push?v-r=push&v-uiId=7&v-pushId=secret',
  headers: { 'X-Vaadin-LastSeenServerSyncId': () => 4 },
  onOpen: () => opened += 1,
  onReopen: () => reopened += 1,
  onReconnect: () => reconnecting += 1,
  onMessage: (response) => receivedBodies.push(response.responseBody)
});

const connect = messages.shift();
assert.equal(connect.version, 1);
assert.equal(connect.type, 'opaquePushConnect');
assert.equal(connect.windowId, 'secondary');
assert.equal(connect.sessionId, 'flow-ipc-session-secondary');
assert.equal(connect.uiId, 'secondary');
assert.equal(connect.payload.flowUiId, 7);
assert.equal(connect.payload.pushId, 'secret');
assert.equal(connect.payload.lastSeenServerSyncId, 4);
const connectionId = connect.payload.connectionId;

context.VaadinDesktopPush.receive({
  version: 1,
  type: 'opaquePushConnected',
  payload: { connectionId }
});
assert.equal(opened, 1);

context.VaadinDesktopPush.receive({
  version: 1,
  type: 'opaquePush',
  payload: { connectionId, pushSequence: 1, uidl: '{"syncId":1}', resync: false }
});
assert.deepEqual(receivedBodies, ['{"syncId":1}']);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundlePushFirstSequence, 1);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundlePushLastSequence, 1);

context.VaadinDesktopPush.receive({
  version: 1,
  type: 'opaquePush',
  payload: { connectionId, pushSequence: 1, uidl: 'duplicate', resync: false }
});
assert.equal(receivedBodies.length, 1);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundlePushDuplicateCount, 1);

context.VaadinDesktopPush.receive({
  version: 1,
  type: 'opaquePush',
  payload: { connectionId, pushSequence: 3, uidl: 'gap', resync: false }
});
assert.equal(messages.shift().type, 'opaquePushResync');
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundlePushGapCount, 1);

context.VaadinDesktopPush.receive({
  version: 1,
  type: 'opaquePush',
  payload: { connectionId, pushSequence: 4, uidl: 'still waiting', resync: false }
});
assert.equal(messages.length, 0);
assert.equal(
  context.VaadinDesktopIpcDiagnostics.standardBundlePushDeferredWhileResyncCount,
  1
);

context.VaadinDesktopPush.receive({
  version: 1,
  type: 'opaquePush',
  payload: { connectionId, pushSequence: 5, uidl: '{"syncId":5}', resync: true }
});
assert.deepEqual(receivedBodies, ['{"syncId":1}', '{"syncId":5}']);

context.VaadinDesktopPush.reconnect();
assert.equal(reconnecting, 1);
const reconnectMessages = messages.splice(0, 2);
assert.deepEqual(reconnectMessages.map((message) => message.type), [
  'opaquePushDisconnect',
  'opaquePushConnect'
]);
assert.equal(reconnectMessages[1].payload.lastSeenServerSyncId, 4);
assert.equal(messages.length, 0);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundlePushReconnectCount, 1);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundlePushResyncCount, 1);
context.VaadinDesktopPush.receive({
  version: 1,
  type: 'opaquePushConnected',
  payload: { connectionId }
});
assert.equal(reopened, 1);

const messagesBeforeDisconnect = messages.length;
context.VaadinDesktopPush.sidecarDisconnected();
assert.equal(messages.length, messagesBeforeDisconnect);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundlePushConnected, false);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleSidecarDisconnected, true);

assert.throws(() => socket.push('{}'), /opaque XMLHttpRequest bridge/);
context.vaadinPush.atmosphere.unsubscribeUrl(
  'https://app.vatori.invalid/VAADIN/push?v-r=push&v-uiId=7&v-pushId=secret'
);
assert.equal(messages.shift().type, 'opaquePushDisconnect');
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundlePushBridgeInstalled, true);
console.log('Opaque push bridge smoke passed');
