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
const historyCalls = [];
const tombstoneTimers = [];
let mutationCallback;

const nativeSetTimeout = globalThis.setTimeout;
const nativeClearTimeout = globalThis.clearTimeout;

function bridgeSetTimeout(callback, delay, ...args) {
  if (delay === 60000) {
    const timer = {
      callback,
      args,
      cancelled: false,
      fired: false,
      unref() {
        return this;
      }
    };
    tombstoneTimers.push(timer);
    return timer;
  }
  return nativeSetTimeout(callback, delay, ...args);
}

function bridgeClearTimeout(timer) {
  if (tombstoneTimers.includes(timer)) {
    timer.cancelled = true;
    return;
  }
  nativeClearTimeout(timer);
}

function expireTombstone(timer) {
  assert.ok(timer, 'expected a pending cancellation tombstone timer');
  assert.equal(timer.cancelled, false);
  assert.equal(timer.fired, false);
  timer.fired = true;
  timer.callback(...timer.args);
}

class NativeXMLHttpRequestStub {
  addEventListener() {
  }
}

class MutationObserverStub {
  constructor(callback) {
    mutationCallback = callback;
  }

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
  setTimeout: bridgeSetTimeout,
  clearTimeout: bridgeClearTimeout,
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
  location: new URL('vatori-app://app/'),
  history: {
    pushState(state, unused, url) {
      historyCalls.push({ method: 'pushState', state, unused, url });
    },
    replaceState(state, unused, url) {
      historyCalls.push({ method: 'replaceState', state, unused, url });
    }
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

context.document.body.innerText = 'Flow ready';
context.document.querySelectorAll = () => [{ localName: 'flow-container-root-1' }];
mutationCallback();
const meaningfulPaint = messages.shift();
assert.equal(meaningfulPaint.version, 1);
assert.equal(meaningfulPaint.type, 'webview.firstMeaningfulPaint');
assert.equal(meaningfulPaint.windowId, 'secondary');
assert.equal(meaningfulPaint.sessionId, 'flow-ipc-session-secondary');
assert.equal(meaningfulPaint.uiId, 'secondary');

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

context.history.pushState({ route: 'lazy' }, '', 'lazy-frontend?tab=details#probe');
context.history.replaceState({ route: 'root' }, '', '/');
assert.deepEqual(
  historyCalls.map(({ method, url }) => ({ method, url })),
  [
    {
      method: 'pushState',
      url: 'vatori-app://app/lazy-frontend?tab=details#probe'
    },
    { method: 'replaceState', url: 'vatori-app://app/' }
  ]
);
assert.equal(
  context.VaadinDesktopIpcDiagnostics.standardBundleHistoryMappedCount,
  2
);

async function nextMessage() {
  for (let attempt = 0; attempt < 100 && messages.length === 0; attempt += 1) {
    await new Promise((resolveDelay) => setTimeout(resolveDelay, 2));
  }
  assert.ok(
    messages.length > 0,
    `opaque bridge did not post a request: ${JSON.stringify(context.VaadinDesktopIpcDiagnostics)}`);
  const message = messages.shift();
  assert.equal(message.version, 1);
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
    version: 1,
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
assert.equal(formExchange.open.payload.url, 'https://app.vatori.invalid/?upload');
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
assert.equal(fetchExchange.open.payload.url, 'https://app.vatori.invalid/?blob');
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

const rangeFetchPromise = context.fetch('?large-range-resume', {
  headers: { Range: 'bytes=262144-' }
});
const rangeExchange = await collectRequest();
assert.equal(header(rangeExchange.open.payload.headers, 'range'), 'bytes=262144-');
receive('opaqueHttpStreamResponse', rangeExchange.requestId, {
  status: 206,
  statusText: 'Partial Content',
  headers: {
    'Accept-Ranges': 'bytes',
    'Content-Range': 'bytes 262144-4194416/4194417'
  }
});
const rangeResponse = await rangeFetchPromise;
assert.equal(rangeResponse.status, 206);
assert.equal(
  rangeResponse.headers.get('content-range'),
  'bytes 262144-4194416/4194417'
);
const rangeReader = rangeResponse.body.getReader();
const rangeCredit = await nextMessage();
assert.equal(rangeCredit.type, 'opaqueHttpStreamResponseCredit');
assert.equal(rangeCredit.payload.requestId, rangeExchange.requestId);
const rangeChunk = Buffer.alloc(32 * 1024, 0x72);
receive('opaqueHttpStreamResponseChunk', rangeExchange.requestId, {
  chunkSequence: 0,
  bodyBase64: rangeChunk.toString('base64'),
  bodyBytes: rangeChunk.length
});
const firstRangeRead = await rangeReader.read();
assert.equal(firstRangeRead.done, false);
assert.deepEqual(Buffer.from(firstRangeRead.value), rangeChunk);
await rangeReader.cancel('resume from consumed offset');
let rangeCancel;
while (!rangeCancel) {
  const message = await nextMessage();
  if (message.type === 'opaqueHttpStreamCancel') {
    rangeCancel = message;
  } else {
    assert.equal(message.type, 'opaqueHttpStreamResponseCredit');
    assert.equal(message.payload.requestId, rangeExchange.requestId);
  }
}
assert.equal(rangeCancel.payload.requestId, rangeExchange.requestId);
receive('opaqueHttpStreamResponseEnd', rangeExchange.requestId, {
  chunkCount: 1,
  bodyBytes: rangeChunk.length
});
assert.equal(
  context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueResponseError,
  undefined
);

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

const omittedCookieFetch = context.fetch(
  'vatori-app://app/nested/omitted',
  { credentials: 'omit' }
);
const omittedCookieExchange = await collectRequest();
assert.equal(
  omittedCookieExchange.open.payload.url,
  'https://app.vatori.invalid/nested/omitted'
);
assert.equal(header(omittedCookieExchange.open.payload.headers, 'cookie'), null);
await respond(omittedCookieExchange, 200, 'omitted', {
  'Set-Cookie': ['omitted=ignored; Path=/; Secure']
});
await omittedCookieFetch;

const noContentPromise = context.fetch('?empty');
const noContentExchange = await collectRequest();
await respond(noContentExchange, 204, '', {});
assert.equal((await noContentPromise).status, 204);

const tombstoneCountBeforeAbort = tombstoneTimers.length;
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
receive('opaqueHttpStreamResponseChunk', abortExchange.requestId, {
  chunkSequence: 0,
  bodyBase64: '',
  bodyBytes: 0
});
receive('opaqueHttpStreamResponseEnd', abortExchange.requestId, {
  chunkCount: 0,
  bodyBytes: 0
});
receive('opaqueHttpStreamError', abortExchange.requestId, {
  message: 'late cancelled response'
});
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueResponseError, undefined);
assert.equal(tombstoneTimers.length, tombstoneCountBeforeAbort + 1);
expireTombstone(tombstoneTimers.at(-1));
receive('opaqueHttpStreamResponse', abortExchange.requestId, {
  status: 599,
  statusText: 'Expired',
  headers: {}
});
assert.equal(
  context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueResponseError,
  `unknown request ${abortExchange.requestId}`
);
delete context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueResponseError;

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

const uploadRetryBytes = Buffer.alloc(10 * 32 * 1024 + 29);
for (let index = 0; index < uploadRetryBytes.length; index += 1) {
  uploadRetryBytes[index] = ((index * 31 + 17) & 0xff);
}
const cancelledUploadXhr = new context.XMLHttpRequest();
const cancelledUploadProgress = [];
const cancelledUploadAborted = new Promise((resolveAbort) => {
  cancelledUploadXhr.onabort = resolveAbort;
});
cancelledUploadXhr.upload.onprogress = (event) => {
  cancelledUploadProgress.push(event.loaded);
  if (event.loaded >= 4 * 32 * 1024 && event.loaded < event.total) {
    cancelledUploadXhr.abort();
  }
};
cancelledUploadXhr.open('POST', '?large-upload-cancel');
cancelledUploadXhr.send(new Blob(
  [uploadRetryBytes],
  { type: 'application/octet-stream' }
));
const cancelledUploadOpen = await nextMessage();
assert.equal(cancelledUploadOpen.type, 'opaqueHttpStreamOpen');
assert.equal(cancelledUploadOpen.payload.contentLength, uploadRetryBytes.length);
receive(
  'opaqueHttpStreamRequestCredit',
  cancelledUploadOpen.payload.requestId,
  { credit: 8 }
);
const cancelledUploadChunks = [];
let cancelledUploadMessage;
while (!cancelledUploadMessage) {
  const message = await nextMessage();
  if (message.type === 'opaqueHttpStreamCancel') {
    cancelledUploadMessage = message;
  } else {
    assert.equal(message.type, 'opaqueHttpStreamChunk');
    cancelledUploadChunks.push(
      Buffer.from(message.payload.bodyBase64, 'base64')
    );
  }
}
await cancelledUploadAborted;
assert.equal(
  cancelledUploadMessage.payload.requestId,
  cancelledUploadOpen.payload.requestId
);
assert.equal(cancelledUploadProgress.at(-1), 4 * 32 * 1024);
assert.equal(
  Buffer.concat(cancelledUploadChunks).length,
  4 * 32 * 1024
);
await new Promise((resolveDelay) => setTimeout(resolveDelay, 20));
assert.equal(messages.length, 0, 'aborted XHR upload emitted a late chunk or end frame');

const retriedUploadXhr = new context.XMLHttpRequest();
const retriedUploadProgress = [];
let retriedUploadBodyComplete = false;
retriedUploadXhr.upload.onprogress = (event) => {
  retriedUploadProgress.push(event.loaded);
};
retriedUploadXhr.upload.onload = () => {
  retriedUploadBodyComplete = true;
};
const retriedUploadLoaded = new Promise((resolveLoaded, rejectLoaded) => {
  retriedUploadXhr.onload = resolveLoaded;
  retriedUploadXhr.onerror = () => rejectLoaded(new Error('retried upload failed'));
});
retriedUploadXhr.open('POST', '?large-upload-retry');
retriedUploadXhr.send(new Blob(
  [uploadRetryBytes],
  { type: 'application/octet-stream' }
));
const retriedUploadExchange = await collectRequest();
assert.equal(
  retriedUploadExchange.open.payload.contentLength,
  uploadRetryBytes.length
);
assert.deepEqual(retriedUploadExchange.body, uploadRetryBytes);
assert.equal(retriedUploadProgress.at(-1), uploadRetryBytes.length);
assert.equal(retriedUploadBodyComplete, true);
await respond(retriedUploadExchange, 200, 'upload retried');
await retriedUploadLoaded;
assert.equal(retriedUploadXhr.status, 200);
assert.equal(retriedUploadXhr.responseText, 'upload retried');
receive('opaqueHttpStreamResponse', retriedUploadExchange.requestId, {
  status: 599,
  statusText: 'Late',
  headers: {}
});
assert.equal(
  context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueResponseError,
  `unknown request ${retriedUploadExchange.requestId}`,
  'completed request remained in the browser registry'
);
delete context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueResponseError;

const multipartForm = () => {
  const formData = new FormData();
  formData.append(
    'file',
    new Blob([uploadRetryBytes], { type: 'application/octet-stream' }),
    'multipart-retry.bin'
  );
  return formData;
};
const cancelledMultipartXhr = new context.XMLHttpRequest();
const cancelledMultipartProgress = [];
const cancelledMultipartAborted = new Promise((resolveAbort) => {
  cancelledMultipartXhr.onabort = resolveAbort;
});
cancelledMultipartXhr.upload.onprogress = (event) => {
  cancelledMultipartProgress.push(event.loaded);
  if (event.loaded >= 4 * 32 * 1024) {
    cancelledMultipartXhr.abort();
  }
};
cancelledMultipartXhr.open('POST', '?large-multipart-upload-cancel');
cancelledMultipartXhr.send(multipartForm());
const cancelledMultipartOpen = await nextMessage();
assert.equal(cancelledMultipartOpen.type, 'opaqueHttpStreamOpen');
assert.equal(cancelledMultipartOpen.payload.contentLength, -1);
assert.match(
  header(cancelledMultipartOpen.payload.headers, 'content-type'),
  /^multipart\/form-data; boundary=/
);
receive(
  'opaqueHttpStreamRequestCredit',
  cancelledMultipartOpen.payload.requestId,
  { credit: 8 }
);
const cancelledMultipartChunks = [];
let cancelledMultipartMessage;
while (!cancelledMultipartMessage) {
  const message = await nextMessage();
  if (message.type === 'opaqueHttpStreamCancel') {
    cancelledMultipartMessage = message;
  } else {
    assert.equal(message.type, 'opaqueHttpStreamChunk');
    cancelledMultipartChunks.push(
      Buffer.from(message.payload.bodyBase64, 'base64')
    );
  }
}
await cancelledMultipartAborted;
assert.equal(
  cancelledMultipartMessage.payload.requestId,
  cancelledMultipartOpen.payload.requestId
);
const cancelledMultipartBytes = Buffer.concat(cancelledMultipartChunks).length;
assert.ok(cancelledMultipartProgress.at(-1) >= 4 * 32 * 1024);
assert.ok(cancelledMultipartProgress.at(-1) < 5 * 32 * 1024 + 1024);
assert.equal(cancelledMultipartBytes, cancelledMultipartProgress.at(-1));
await new Promise((resolveDelay) => setTimeout(resolveDelay, 20));
assert.equal(
  messages.length,
  0,
  'aborted multipart XHR emitted a late chunk or end frame'
);

const retriedMultipartXhr = new context.XMLHttpRequest();
const retriedMultipartProgress = [];
let retriedMultipartBodyComplete = false;
retriedMultipartXhr.upload.onprogress = (event) => {
  retriedMultipartProgress.push(event.loaded);
};
retriedMultipartXhr.upload.onload = () => {
  retriedMultipartBodyComplete = true;
};
const retriedMultipartLoaded = new Promise((resolveLoaded, rejectLoaded) => {
  retriedMultipartXhr.onload = resolveLoaded;
  retriedMultipartXhr.onerror = () => rejectLoaded(
    new Error('retried multipart upload failed')
  );
});
retriedMultipartXhr.open('POST', '?large-multipart-upload-retry');
retriedMultipartXhr.send(multipartForm());
const retriedMultipartExchange = await collectRequest();
assert.equal(retriedMultipartExchange.open.payload.contentLength, -1);
assert.match(
  header(retriedMultipartExchange.open.payload.headers, 'content-type'),
  /^multipart\/form-data; boundary=/
);
const multipartFileOffset = retriedMultipartExchange.body.indexOf(
  uploadRetryBytes
);
assert.ok(multipartFileOffset > 0, 'multipart body omitted the file payload');
assert.deepEqual(
  retriedMultipartExchange.body.subarray(
    multipartFileOffset,
    multipartFileOffset + uploadRetryBytes.length
  ),
  uploadRetryBytes
);
assert.ok(retriedMultipartExchange.body.length > uploadRetryBytes.length);
assert.equal(
  retriedMultipartProgress.at(-1),
  retriedMultipartExchange.body.length
);
assert.equal(retriedMultipartBodyComplete, true);
await respond(retriedMultipartExchange, 200, 'multipart upload retried');
await retriedMultipartLoaded;
assert.equal(retriedMultipartXhr.status, 200);
assert.equal(
  retriedMultipartXhr.responseText,
  'multipart upload retried'
);

const concurrentCancelledBytes = Buffer.alloc(8 * 32 * 1024 + 31);
const concurrentSurvivorBytes = Buffer.alloc(7 * 32 * 1024 + 47);
for (let index = 0; index < concurrentCancelledBytes.length; index += 1) {
  concurrentCancelledBytes[index] = ((index * 17 + 3) & 0xff);
}
for (let index = 0; index < concurrentSurvivorBytes.length; index += 1) {
  concurrentSurvivorBytes[index] = ((index * 29 + 7) & 0xff);
}
const concurrentCancelledXhr = new context.XMLHttpRequest();
const concurrentSurvivorXhr = new context.XMLHttpRequest();
const concurrentCancelledProgress = [];
const concurrentSurvivorProgress = [];
const concurrentCancelled = new Promise((resolveAbort) => {
  concurrentCancelledXhr.onabort = resolveAbort;
});
let concurrentSurvivorBodyComplete = false;
concurrentCancelledXhr.upload.onprogress = (event) => {
  concurrentCancelledProgress.push(event.loaded);
};
concurrentSurvivorXhr.upload.onprogress = (event) => {
  concurrentSurvivorProgress.push(event.loaded);
};
concurrentSurvivorXhr.upload.onload = () => {
  concurrentSurvivorBodyComplete = true;
};
const concurrentSurvivorLoaded = new Promise((resolveLoaded, rejectLoaded) => {
  concurrentSurvivorXhr.onload = resolveLoaded;
  concurrentSurvivorXhr.onerror = () => rejectLoaded(
    new Error('concurrent survivor request failed')
  );
});
concurrentCancelledXhr.open('POST', '?concurrent-cancel');
concurrentSurvivorXhr.open('POST', '?concurrent-survivor');
concurrentCancelledXhr.send(new Blob(
  [concurrentCancelledBytes],
  { type: 'application/octet-stream' }
));
concurrentSurvivorXhr.send(new Blob(
  [concurrentSurvivorBytes],
  { type: 'application/octet-stream' }
));

const concurrentOpens = [];
while (concurrentOpens.length < 2) {
  const message = await nextMessage();
  assert.equal(message.type, 'opaqueHttpStreamOpen');
  concurrentOpens.push(message);
}
const concurrentCancelledOpen = concurrentOpens.find(
  (message) => message.payload.url.endsWith('?concurrent-cancel')
);
const concurrentSurvivorOpen = concurrentOpens.find(
  (message) => message.payload.url.endsWith('?concurrent-survivor')
);
assert.ok(concurrentCancelledOpen);
assert.ok(concurrentSurvivorOpen);
assert.notEqual(
  concurrentCancelledOpen.payload.requestId,
  concurrentSurvivorOpen.payload.requestId
);
receive(
  'opaqueHttpStreamRequestCredit',
  concurrentCancelledOpen.payload.requestId,
  { credit: 1 }
);
receive(
  'opaqueHttpStreamRequestCredit',
  concurrentSurvivorOpen.payload.requestId,
  { credit: 1 }
);

const concurrentBodies = new Map([
  [concurrentCancelledOpen.payload.requestId, []],
  [concurrentSurvivorOpen.payload.requestId, []]
]);
while (Array.from(concurrentBodies.values()).some((chunks) => chunks.length === 0)) {
  const message = await nextMessage();
  assert.equal(message.type, 'opaqueHttpStreamChunk');
  const chunks = concurrentBodies.get(message.payload.requestId);
  assert.ok(chunks, 'concurrent chunk used an unknown request id');
  assert.equal(message.payload.chunkSequence, chunks.length);
  chunks.push(Buffer.from(message.payload.bodyBase64, 'base64'));
}
assert.equal(concurrentCancelledProgress.at(-1), 32 * 1024);
assert.equal(concurrentSurvivorProgress.at(-1), 32 * 1024);

concurrentCancelledXhr.abort();
await concurrentCancelled;
const concurrentCancelMessage = await nextMessage();
assert.equal(concurrentCancelMessage.type, 'opaqueHttpStreamCancel');
assert.equal(
  concurrentCancelMessage.payload.requestId,
  concurrentCancelledOpen.payload.requestId
);

receive(
  'opaqueHttpStreamRequestCredit',
  concurrentSurvivorOpen.payload.requestId,
  { credit: 16 }
);
let concurrentSurvivorEnd;
while (!concurrentSurvivorEnd) {
  const message = await nextMessage();
  assert.equal(message.payload.requestId, concurrentSurvivorOpen.payload.requestId);
  if (message.type === 'opaqueHttpStreamEnd') {
    concurrentSurvivorEnd = message;
    break;
  }
  assert.equal(message.type, 'opaqueHttpStreamChunk');
  const chunks = concurrentBodies.get(message.payload.requestId);
  assert.equal(message.payload.chunkSequence, chunks.length);
  chunks.push(Buffer.from(message.payload.bodyBase64, 'base64'));
}
const concurrentCancelledBody = Buffer.concat(
  concurrentBodies.get(concurrentCancelledOpen.payload.requestId)
);
const concurrentSurvivorBody = Buffer.concat(
  concurrentBodies.get(concurrentSurvivorOpen.payload.requestId)
);
assert.equal(concurrentCancelledBody.length, 32 * 1024);
assert.deepEqual(concurrentCancelledBody, concurrentCancelledBytes.subarray(0, 32 * 1024));
assert.equal(concurrentSurvivorEnd.payload.chunkCount, 8);
assert.equal(concurrentSurvivorEnd.payload.bodyBytes, concurrentSurvivorBytes.length);
assert.deepEqual(concurrentSurvivorBody, concurrentSurvivorBytes);
assert.equal(concurrentSurvivorProgress.at(-1), concurrentSurvivorBytes.length);
assert.equal(concurrentSurvivorBodyComplete, true);
await new Promise((resolveDelay) => setTimeout(resolveDelay, 20));
assert.equal(messages.length, 0, 'cancelled sibling emitted a late request frame');

const concurrentSurvivorExchange = {
  open: concurrentSurvivorOpen,
  requestId: concurrentSurvivorOpen.payload.requestId,
  body: concurrentSurvivorBody
};
await respond(concurrentSurvivorExchange, 200, 'concurrent survivor complete');
await concurrentSurvivorLoaded;
assert.equal(concurrentSurvivorXhr.status, 200);
assert.equal(
  concurrentSurvivorXhr.responseText,
  'concurrent survivor complete'
);
assert.equal(
  context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueRequestError,
  undefined
);
assert.equal(
  context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueResponseError,
  undefined
);

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

const messagesAfterDisconnect = messages.length;
await assert.rejects(
  context.fetch('?request-after-sidecar-disconnect'),
  /sidecar disconnected/
);
assert.equal(messages.length, messagesAfterDisconnect);

const disconnectedXhr = new context.XMLHttpRequest();
const disconnectedXhrFailed = new Promise((resolveFailure) => {
  disconnectedXhr.onerror = resolveFailure;
});
disconnectedXhr.open('POST', '?xhr-after-sidecar-disconnect');
disconnectedXhr.send(new Blob(['must not reach a replacement sidecar']));
await disconnectedXhrFailed;
assert.equal(disconnectedXhr.status, 0);
assert.equal(disconnectedXhr.readyState, 4);
assert.equal(messages.length, messagesAfterDisconnect);
assert.equal(
  context.VaadinDesktopIpcDiagnostics.standardBundleOpaqueDisconnectedRequestCount,
  2
);
context.VaadinDesktopOpaqueHttp.sidecarDisconnected();
assert.equal(messages.length, messagesAfterDisconnect);

assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleFetchBridgeInstalled, true);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleCookieBridgeInstalled, true);
assert.equal(context.VaadinDesktopIpcDiagnostics.standardBundleCookieCount, 1);
assert.match(context.VaadinDesktopIpcDiagnostics.standardBundleBodyInitSupport, /form-data/);
assert.doesNotMatch(JSON.stringify(context.VaadinDesktopIpcDiagnostics), /abc123|nested=value/);
console.log('Opaque HTTP bridge smoke passed');
