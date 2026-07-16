import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const testDirectory = dirname(fileURLToPath(import.meta.url));
const bridgePath = resolve(
  testDirectory,
  '../../..',
  'native-hosts/macos/Resources/FlowIpcOpaqueHttpBridge.js'
);
const bridgeSource = await readFile(bridgePath, 'utf8');
const messages = [];

class NativeXMLHttpRequestStub {
  addEventListener() {
  }
}

class MutationObserverStub {
  observe() {
  }
}

class HTMLScriptElementStub {
  get src() {
    return this.source || '';
  }

  set src(value) {
    this.source = String(value);
  }
}

const context = {
  XMLHttpRequest: NativeXMLHttpRequestStub,
  fetch: globalThis.fetch.bind(globalThis),
  Headers,
  Request,
  Response,
  FormData,
  Blob,
  URL,
  ArrayBuffer,
  Uint8Array,
  TextEncoder,
  TextDecoder,
  ReadableStream,
  DOMException,
  AbortController,
  MutationObserver: MutationObserverStub,
  HTMLScriptElement: HTMLScriptElementStub,
  btoa,
  atob,
  setTimeout,
  clearTimeout,
  console,
  VaadinDesktopHostContext: {
    windowId: 'secondary',
    sessionId: 'flow-ipc-session-secondary',
    uiId: 'secondary'
  },
  document: {
    baseURI: 'https://app.vatori.invalid/',
    body: { innerText: '' },
    querySelectorAll: () => []
  },
  addEventListener: () => {},
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

const productionPushScript = new context.HTMLScriptElement();
productionPushScript.src = 'https://app.vatori.invalid/VAADIN/static/push/vaadinPush-min.js';
assert.equal(productionPushScript.src, 'vatori-app://app/__vatori/FlowIpcPushBridge.js');
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundlePushScriptMapped, true);

const developmentPushScript = new context.HTMLScriptElement();
developmentPushScript.src = 'https://app.vatori.invalid/VAADIN/static/push/vaadinPush.js';
assert.equal(developmentPushScript.src, 'vatori-app://app/__vatori/FlowIpcPushBridge.js');

const unrelatedScript = new context.HTMLScriptElement();
unrelatedScript.src = 'https://app.vatori.invalid/VAADIN/build/application.js';
assert.equal(unrelatedScript.src, 'https://app.vatori.invalid/VAADIN/build/application.js');

async function nextMessage() {
  for (let attempt = 0; attempt < 100 && messages.length === 0; attempt += 1) {
    await new Promise((resolveDelay) => setTimeout(resolveDelay, 2));
  }
  assert.ok(
    messages.length > 0,
    `opaque bridge did not post a request: ${JSON.stringify(context.VaadinDesktopIpcDiagnostics)}`);
  const message = messages.shift();
  assert.equal(message.windowId, 'secondary');
  assert.equal(message.sessionId, 'flow-ipc-session-secondary');
  assert.equal(message.uiId, 'secondary');
  return message;
}

function header(headers, name) {
  const key = Object.keys(headers).find(
    (candidate) => candidate.toLowerCase() === name.toLowerCase()
  );
  return key ? headers[key] : null;
}

function receive(type, requestId, payload = {}) {
  context.VaadinDesktopOpaqueHttp.receive({
    type,
    correlationId: requestId,
    payload: {
      requestId,
      ...payload
    }
  });
}

async function collectRequest(initialCredit = 4) {
  const open = await nextMessage();
  assert.equal(open.type, 'opaqueHttpStreamOpen');
  const requestId = open.payload.requestId;
  receive('opaqueHttpStreamRequestCredit', requestId, { credit: initialCredit });
  const chunks = [];
  for (;;) {
    const message = await nextMessage();
    if (message.type === 'opaqueHttpStreamEnd') {
      assert.equal(message.payload.chunkCount, chunks.length);
      const body = Buffer.concat(chunks);
      assert.equal(message.payload.bodyBytes, body.length);
      return { open, requestId, body };
    }
    assert.equal(message.type, 'opaqueHttpStreamChunk');
    assert.equal(message.payload.chunkSequence, chunks.length);
    const chunk = Buffer.from(message.payload.bodyBase64, 'base64');
    assert.ok(chunk.length <= 32 * 1024);
    chunks.push(chunk);
    receive('opaqueHttpStreamRequestCredit', requestId, { credit: 1 });
  }
}

async function respond(exchange, status, body, headers = {}, chunkSize = 32 * 1024) {
  receive('opaqueHttpStreamResponse', exchange.requestId, {
    status,
    statusText: status === 204 ? 'No Content' : 'OK',
    headers
  });
  const bytes = Buffer.from(body);
  let offset = 0;
  let sequence = 0;
  let credit = 0;
  while (offset < bytes.length) {
    if (credit === 0) {
      const creditMessage = await nextMessage();
      assert.equal(creditMessage.type, 'opaqueHttpStreamResponseCredit');
      credit += creditMessage.payload.credit;
    }
    const end = Math.min(bytes.length, offset + chunkSize);
    const chunk = bytes.subarray(offset, end);
    receive('opaqueHttpStreamResponseChunk', exchange.requestId, {
      chunkSequence: sequence,
      bodyBase64: chunk.toString('base64'),
      bodyBytes: chunk.length
    });
    sequence += 1;
    offset = end;
    credit -= 1;
  }
  receive('opaqueHttpStreamResponseEnd', exchange.requestId, {
    chunkCount: sequence,
    bodyBytes: bytes.length
  });
  await Promise.resolve();
  for (let index = messages.length - 1; index >= 0; index -= 1) {
    if (messages[index].type === 'opaqueHttpStreamResponseCredit'
        && messages[index].payload.requestId === exchange.requestId) {
      messages.splice(index, 1);
    }
  }
}

const form = new FormData();
form.append('title', 'Quarterly report');
form.append('attachment', new Blob(['hello upload'], { type: 'text/plain' }), 'report.txt');
const xhr = new context.XMLHttpRequest();
const xhrLoaded = new Promise((resolveLoaded, rejectLoaded) => {
  xhr.onload = resolveLoaded;
  xhr.onerror = () => rejectLoaded(new Error('FormData XHR failed'));
});
xhr.open('POST', '?upload');
xhr.send(form);
const formExchange = await collectRequest();
assert.equal(formExchange.open.payload.method, 'POST');
assert.match(header(formExchange.open.payload.headers, 'content-type'), /^multipart\/form-data; boundary=/);
const multipartBody = formExchange.body.toString('utf8');
assert.match(multipartBody, /name="title"/);
assert.match(multipartBody, /filename="report.txt"/);
assert.match(multipartBody, /hello upload/);
await respond(formExchange, 200, 'uploaded', { 'Content-Type': 'text/plain' });
await xhrLoaded;
assert.equal(xhr.responseText, 'uploaded');

const fetchPromise = context.fetch('?blob', {
  method: 'PUT',
  body: new Blob(['fetch body'], { type: 'text/custom' })
});
const fetchExchange = await collectRequest();
assert.equal(fetchExchange.open.payload.method, 'PUT');
assert.equal(header(fetchExchange.open.payload.headers, 'content-type'), 'text/custom');
assert.equal(fetchExchange.body.toString(), 'fetch body');
await respond(fetchExchange, 200, 'fetch response', {
  'Content-Type': ['text/plain'],
  'Set-Cookie': [
    'desktop_session=abc123; Path=/; HttpOnly; Secure; SameSite=Strict',
    'nested=value; Path=/nested; Secure',
    'foreign=ignored; Domain=example.com; Path=/'
  ]
});
const fetchResponse = await fetchPromise;
assert.equal(fetchResponse.status, 200);
assert.equal(await fetchResponse.text(), 'fetch response');
assert.equal(fetchResponse.headers.get('set-cookie'), null);

const largeRequestBytes = Buffer.alloc(6 * 32 * 1024 + 17, 0x5a);
const largeFetchPromise = context.fetch('?large-stream', {
  method: 'POST',
  body: new Blob([largeRequestBytes], { type: 'application/octet-stream' })
});
const largeOpen = await nextMessage();
assert.equal(largeOpen.type, 'opaqueHttpStreamOpen');
const largeRequestId = largeOpen.payload.requestId;
receive('opaqueHttpStreamRequestCredit', largeRequestId, { credit: 2 });
const largeRequestChunks = [];
for (let index = 0; index < 2; index += 1) {
  const message = await nextMessage();
  assert.equal(message.type, 'opaqueHttpStreamChunk');
  largeRequestChunks.push(Buffer.from(message.payload.bodyBase64, 'base64'));
}
await new Promise((resolveDelay) => setTimeout(resolveDelay, 10));
assert.equal(messages.length, 0, 'request producer ignored the Java credit window');
receive('opaqueHttpStreamRequestCredit', largeRequestId, { credit: 8 });
for (;;) {
  const message = await nextMessage();
  if (message.type === 'opaqueHttpStreamEnd') {
    break;
  }
  assert.equal(message.type, 'opaqueHttpStreamChunk');
  largeRequestChunks.push(Buffer.from(message.payload.bodyBase64, 'base64'));
}
const collectedLargeRequest = Buffer.concat(largeRequestChunks);
assert.equal(collectedLargeRequest.length, largeRequestBytes.length);
assert.deepEqual(collectedLargeRequest, largeRequestBytes);
const largeExchange = { open: largeOpen, requestId: largeRequestId, body: collectedLargeRequest };
const largeResponseBytes = Buffer.alloc(3 * 32 * 1024 + 9, 0x41);
let largeResponseFinished = false;
const largeResponseSend = respond(
  largeExchange,
  200,
  largeResponseBytes,
  { 'Content-Type': 'application/octet-stream' },
  32 * 1024
).then(() => {
  largeResponseFinished = true;
});
const largeFetchResponse = await largeFetchPromise;
await new Promise((resolveDelay) => setTimeout(resolveDelay, 10));
assert.equal(largeResponseFinished, false, 'response producer ignored Fetch stream backpressure');
const collectedLargeResponse = Buffer.from(await largeFetchResponse.arrayBuffer());
await largeResponseSend;
assert.deepEqual(collectedLargeResponse, largeResponseBytes);

const cookieFetch = context.fetch('/next');
const cookieExchange = await collectRequest();
assert.equal(header(cookieExchange.open.payload.headers, 'cookie'), 'desktop_session=abc123');
await respond(cookieExchange, 200, 'cookie accepted', {
  'Set-Cookie': ['desktop_session=; Path=/; Max-Age=0; HttpOnly; Secure']
});
await cookieFetch;

const nestedFetch = context.fetch('/nested/resource');
const nestedExchange = await collectRequest();
assert.equal(header(nestedExchange.open.payload.headers, 'cookie'), 'nested=value');
await respond(nestedExchange, 200, 'nested accepted');
await nestedFetch;

const omittedCookieFetch = context.fetch('/nested/omitted', { credentials: 'omit' });
const omittedCookieExchange = await collectRequest();
assert.equal(header(omittedCookieExchange.open.payload.headers, 'cookie'), null);
await respond(omittedCookieExchange, 200, 'omitted', {
  'Set-Cookie': ['omitted=ignored; Path=/; Secure']
});
await omittedCookieFetch;

const noContentPromise = context.fetch('?empty');
const noContentExchange = await collectRequest();
await respond(noContentExchange, 204, '', {});
assert.equal((await noContentPromise).status, 204);

const abortController = new AbortController();
const abortedFetch = context.fetch('?abort', { signal: abortController.signal });
const abortExchange = await collectRequest();
abortController.abort();
await assert.rejects(abortedFetch, (error) => error.name === 'AbortError');
assert.equal((await nextMessage()).type, 'opaqueHttpStreamCancel');
receive('opaqueHttpStreamResponse', abortExchange.requestId, {
  status: 200,
  statusText: 'OK',
  headers: {}
});
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueResponseError, undefined);

let slowBodyCancelled = false;
let slowBodyChunk = 0;
const slowBody = new ReadableStream({
  async pull(controller) {
    if (slowBodyChunk > 0) {
      await new Promise((resolveDelay) => setTimeout(resolveDelay, 30));
    }
    if (!slowBodyCancelled) {
      controller.enqueue(new Uint8Array(32 * 1024));
      slowBodyChunk += 1;
    }
  },
  cancel() {
    slowBodyCancelled = true;
  }
});
const streamingAbortController = new AbortController();
const streamingAbortFetch = context.fetch('?streaming-abort', {
  method: 'POST',
  body: slowBody,
  signal: streamingAbortController.signal
});
const streamingAbortOpen = await nextMessage();
assert.equal(streamingAbortOpen.type, 'opaqueHttpStreamOpen');
receive('opaqueHttpStreamRequestCredit', streamingAbortOpen.payload.requestId, { credit: 4 });
assert.equal((await nextMessage()).type, 'opaqueHttpStreamChunk');
streamingAbortController.abort();
await assert.rejects(streamingAbortFetch, (error) => error.name === 'AbortError');
assert.equal((await nextMessage()).type, 'opaqueHttpStreamCancel');
await new Promise((resolveDelay) => setTimeout(resolveDelay, 50));
assert.equal(messages.length, 0, 'cancelled request emitted a late chunk or end frame');
assert.equal(slowBodyCancelled, true);

const timeoutXhr = new context.XMLHttpRequest();
timeoutXhr.timeout = 5;
const timedOut = new Promise((resolveTimeout) => {
  timeoutXhr.ontimeout = resolveTimeout;
});
timeoutXhr.open('POST', '?timeout');
timeoutXhr.send(new Blob(['slow body']));
const timeoutExchange = await collectRequest();
await timedOut;
assert.equal(timeoutXhr.status, 0);
assert.equal((await nextMessage()).type, 'opaqueHttpStreamCancel');
receive('opaqueHttpStreamResponseEnd', timeoutExchange.requestId, {
  chunkCount: 0,
  bodyBytes: 0
});
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueResponseError, undefined);

const disconnectedFetch = context.fetch('?sidecar-disconnect');
const disconnectedExchange = await collectRequest();
context.VaadinDesktopOpaqueHttp.sidecarDisconnected();
await assert.rejects(disconnectedFetch, /sidecar disconnected/);
assert.equal(
  context.VaadinDesktopIpcDiagnostics.standardBundleSidecarDisconnected,
  true
);
receive('opaqueHttpStreamResponse', disconnectedExchange.requestId, {
  status: 200,
  statusText: 'OK',
  headers: {}
});
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueResponseError, undefined);

assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleFetchBridgeInstalled, true);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleCookieBridgeInstalled, true);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleCookieCount, 1);
assert.match(context.VaadinDesktopIpcDiagnostics.standardBundleBodyInitSupport, /form-data/);
assert.doesNotMatch(JSON.stringify(context.VaadinDesktopIpcDiagnostics), /abc123|nested=value/);
console.log('Opaque HTTP bridge smoke passed');
