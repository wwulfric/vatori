(() => {
  if (window.VaadinDesktopOpaqueHttp) {
    return;
  }

  const NativeXMLHttpRequest = window.XMLHttpRequest;
  const NativeFetch = window.fetch.bind(window);
  const NativeHeaders = window.Headers;
  const NativeRequest = window.Request;
  const NativeResponse = window.Response;
  const NativeReadableStream = window.ReadableStream;
  const virtualRequestOrigin = 'https://app.vatori.invalid';
  const transportChunkSize = 32 * 1024;
  const pending = new Map();
  const ignoredResponses = new Set();
  const cookieJar = new Map();
  let sidecarConnected = true;
  let nextRequestId = 0;
  const diagnostics = window.VaadinDesktopIpcDiagnostics =
    window.VaadinDesktopIpcDiagnostics || {};

  function bytesToBase64(bytes) {
    let binary = '';
    const chunkSize = 0x8000;
    for (let offset = 0; offset < bytes.length; offset += chunkSize) {
      binary += String.fromCharCode(...bytes.subarray(offset, offset + chunkSize));
    }
    return btoa(binary);
  }

  function base64ToBytes(value) {
    const binary = atob(value || '');
    const bytes = new Uint8Array(binary.length);
    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index);
    }
    return bytes;
  }

  function isBridgedUrl(url) {
    return url.protocol === 'vatori-app:' || url.origin === virtualRequestOrigin;
  }

  function virtualRequestUrl(url) {
    if (url.protocol !== 'vatori-app:') {
      return url.href;
    }
    return new URL(`${url.pathname}${url.search}`, `${virtualRequestOrigin}/`).href;
  }

  function installPushScriptMapping() {
    if (!window.HTMLScriptElement) {
      return;
    }
    const descriptor = Object.getOwnPropertyDescriptor(
      window.HTMLScriptElement.prototype,
      'src');
    if (!descriptor?.get || !descriptor?.set || descriptor.configurable === false) {
      diagnostics.standardBundlePushScriptMappingError = 'script src is not configurable';
      return;
    }
    Object.defineProperty(window.HTMLScriptElement.prototype, 'src', {
      configurable: true,
      enumerable: descriptor.enumerable,
      get: descriptor.get,
      set(value) {
        const resolved = new URL(String(value), document.baseURI);
        if (isBridgedUrl(resolved)
            && (resolved.pathname === '/VAADIN/static/push/vaadinPush.js'
              || resolved.pathname === '/VAADIN/static/push/vaadinPush-min.js')) {
          descriptor.set.call(this, 'vatori-app://app/__vatori/FlowIpcPushBridge.js');
          diagnostics.standardBundlePushScriptMapped = true;
          return;
        }
        descriptor.set.call(this, value);
      }
    });
    diagnostics.standardBundlePushScriptMappingInstalled = true;
  }

  function installHistoryBridge() {
    for (const methodName of ['pushState', 'replaceState']) {
      const nativeMethod = window.history?.[methodName];
      if (typeof nativeMethod !== 'function') {
        continue;
      }
      window.history[methodName] = function (...args) {
        const mappedArgs = [...args];
        if (mappedArgs.length >= 3
            && mappedArgs[2] != null
            && window.location?.protocol === 'vatori-app:') {
          const resolved = new URL(String(mappedArgs[2]), document.baseURI);
          if (resolved.origin === virtualRequestOrigin) {
            mappedArgs[2] =
              `${window.location.protocol}//${window.location.host}`
                + `${resolved.pathname}${resolved.search}${resolved.hash}`;
            diagnostics.standardBundleHistoryMappedCount =
              Number(diagnostics.standardBundleHistoryMappedCount || 0) + 1;
          }
        }
        try {
          return nativeMethod.apply(this, mappedArgs);
        } catch (error) {
          diagnostics.standardBundleHistoryError =
            `${methodName} ${String(args[2] ?? '')}: `
              + `${error?.name || 'Error'}: ${error?.message || String(error)}`;
          throw error;
        }
      };
    }
    diagnostics.standardBundleHistoryBridgeInstalled = true;
  }

  function headerName(headers, name) {
    const normalized = String(name).toLowerCase();
    return Object.keys(headers).find((candidate) => candidate.toLowerCase() === normalized);
  }

  function setHeader(headers, name, value, append = false) {
    const existing = headerName(headers, name);
    if (existing) {
      if (append) {
        headers[existing] = `${headers[existing]}, ${value}`;
      } else {
        headers[existing] = String(value);
      }
    } else {
      headers[String(name)] = String(value);
    }
  }

  function headersObject(...sources) {
    const result = {};
    for (const source of sources) {
      if (!source) {
        continue;
      }
      const headers = new NativeHeaders(source);
      headers.forEach((value, name) => setHeader(result, name, value));
    }
    return result;
  }

  async function requestBody(body, headers) {
    if (body == null) {
      return { reader: null, contentLength: 0 };
    }
    let contentLength = -1;
    if (body instanceof ArrayBuffer) {
      contentLength = body.byteLength;
    } else if (ArrayBuffer.isView(body)) {
      contentLength = body.byteLength;
    } else if (typeof body === 'string') {
      contentLength = new TextEncoder().encode(body).byteLength;
    } else if (body instanceof Blob) {
      contentLength = body.size;
    }
    const encoded = new NativeResponse(body);
    const contentType = encoded.headers.get('content-type');
    if (contentType && !headerName(headers, 'content-type')) {
      setHeader(headers, 'Content-Type', contentType);
    }
    return {
      reader: encoded.body ? encoded.body.getReader() : null,
      contentLength
    };
  }

  function normalizeResponseHeaders(headers) {
    const normalized = new Map();
    Object.entries(headers || {}).forEach(([name, value]) => {
      const lowerName = name.toLowerCase();
      if (lowerName !== 'set-cookie' && lowerName !== 'set-cookie2') {
        normalized.set(lowerName, Array.isArray(value) ? value.join(', ') : String(value));
      }
    });
    return normalized;
  }

  function responseHeadersInit(headers) {
    const result = {};
    Object.entries(headers || {}).forEach(([name, value]) => {
      const lowerName = name.toLowerCase();
      if (lowerName !== 'set-cookie' && lowerName !== 'set-cookie2') {
        result[name] = Array.isArray(value) ? value.join(', ') : String(value);
      }
    });
    return result;
  }

  function defaultCookiePath(pathname) {
    if (!pathname || !pathname.startsWith('/') || pathname === '/') {
      return '/';
    }
    const lastSlash = pathname.lastIndexOf('/');
    return lastSlash <= 0 ? '/' : pathname.slice(0, lastSlash);
  }

  function domainMatches(hostname, domain, hostOnly) {
    return hostOnly ? hostname === domain : hostname === domain || hostname.endsWith(`.${domain}`);
  }

  function pathMatches(pathname, cookiePath) {
    if (pathname === cookiePath) {
      return true;
    }
    if (!pathname.startsWith(cookiePath)) {
      return false;
    }
    return cookiePath.endsWith('/') || pathname.charAt(cookiePath.length) === '/';
  }

  function setCookieValues(headers) {
    const entry = Object.entries(headers || {})
      .find(([name]) => name.toLowerCase() === 'set-cookie');
    if (!entry) {
      return [];
    }
    if (Array.isArray(entry[1])) {
      return entry[1].map(String);
    }
    return String(entry[1]).split(/,(?=\s*[!#$%&'*+\-.^_`|~0-9A-Za-z]+=)/);
  }

  function storeResponseCookies(urlValue, headers) {
    const url = new URL(urlValue);
    const hostname = url.hostname.toLowerCase();
    for (const serialized of setCookieValues(headers)) {
      const segments = serialized.split(';');
      const nameValue = segments.shift() || '';
      const separator = nameValue.indexOf('=');
      if (separator <= 0) {
        continue;
      }
      const name = nameValue.slice(0, separator).trim();
      const value = nameValue.slice(separator + 1).trim();
      const attributes = new Map();
      for (const segment of segments) {
        const attributeSeparator = segment.indexOf('=');
        const attributeName = (attributeSeparator < 0 ? segment : segment.slice(0, attributeSeparator))
          .trim()
          .toLowerCase();
        const attributeValue = attributeSeparator < 0
          ? ''
          : segment.slice(attributeSeparator + 1).trim();
        if (attributeName) {
          attributes.set(attributeName, attributeValue);
        }
      }

      const domainAttribute = attributes.get('domain');
      const domain = (domainAttribute || hostname).replace(/^\./, '').toLowerCase();
      const hostOnly = !domainAttribute;
      if (!domainMatches(hostname, domain, hostOnly)) {
        continue;
      }
      const requestedPath = attributes.get('path');
      const path = requestedPath?.startsWith('/') ? requestedPath : defaultCookiePath(url.pathname);
      const key = `${domain}\t${path}\t${name}`;
      let expiresAt = null;
      if (attributes.has('max-age')) {
        const maxAge = Number.parseInt(attributes.get('max-age'), 10);
        if (!Number.isNaN(maxAge)) {
          expiresAt = maxAge <= 0 ? 0 : Date.now() + maxAge * 1000;
        }
      } else if (attributes.has('expires')) {
        const parsed = Date.parse(attributes.get('expires'));
        if (!Number.isNaN(parsed)) {
          expiresAt = parsed;
        }
      }
      if (expiresAt != null && expiresAt <= Date.now()) {
        cookieJar.delete(key);
        continue;
      }
      cookieJar.set(key, {
        name,
        value,
        domain,
        hostOnly,
        path,
        secure: attributes.has('secure'),
        expiresAt
      });
    }
    diagnostics.standardBundleCookieCount = cookieJar.size;
  }

  function cookieHeader(urlValue) {
    const url = new URL(urlValue);
    const hostname = url.hostname.toLowerCase();
    const now = Date.now();
    const cookies = [];
    for (const [key, cookie] of cookieJar.entries()) {
      if (cookie.expiresAt != null && cookie.expiresAt <= now) {
        cookieJar.delete(key);
        continue;
      }
      if (!domainMatches(hostname, cookie.domain, cookie.hostOnly)
          || !pathMatches(url.pathname || '/', cookie.path)
          || (cookie.secure && url.protocol !== 'https:' && url.protocol !== 'vatori-app:')) {
        continue;
      }
      cookies.push(cookie);
    }
    diagnostics.standardBundleCookieCount = cookieJar.size;
    return cookies
      .sort((left, right) => right.path.length - left.path.length)
      .map((cookie) => `${cookie.name}=${cookie.value}`)
      .join('; ');
  }

  function postToHost(message) {
    const handler = window.webkit
      && window.webkit.messageHandlers
      && window.webkit.messageHandlers.flowIpc;
    if (!handler) {
      throw new Error('Flow IPC native message handler is unavailable');
    }
    handler.postMessage(message);
  }

  function postEnvelope(type, requestId, payload = {}) {
    const hostContext = window.VaadinDesktopHostContext || {};
    postToHost({
      version: 1,
      type,
      id: `${requestId}-${type}`,
      correlationId: requestId,
      sessionId: hostContext.sessionId || 'flow-ipc-session',
      uiId: hostContext.uiId || 'main',
      windowId: hostContext.windowId || 'main',
      route: '',
      payload: { requestId, ...payload },
      error: null,
      timestamp: Date.now()
    });
  }

  function forgetLateResponse(requestId) {
    if (!requestId) {
      return;
    }
    const request = pending.get(requestId);
    if (request) {
      request.requestCancelled = true;
    }
    request?.requestReader?.cancel?.().catch?.(() => {});
    request?.responseController?.error?.(
      new DOMException('The operation was aborted', 'AbortError'));
    pending.delete(requestId);
    ignoredResponses.add(requestId);
    if (sidecarConnected) {
      postEnvelope('opaqueHttpStreamCancel', requestId, { reason: 'client-cancelled' });
    }
    const cleanupTimer = setTimeout(() => ignoredResponses.delete(requestId), 60000);
    cleanupTimer?.unref?.();
  }

  function sidecarDisconnected() {
    diagnostics.standardBundleSidecarDisconnected = true;
    if (!sidecarConnected) {
      return;
    }
    sidecarConnected = false;
    const error = new TypeError('Flow IPC sidecar disconnected');
    for (const [requestId, request] of pending.entries()) {
      request.requestCancelled = true;
      request.requestReader?.cancel?.().catch?.(() => {});
      request.responseController?.error?.(error);
      pending.delete(requestId);
      ignoredResponses.add(requestId);
      request.fail(error);
    }
  }

  function dispatchOpaqueRequest({
    method,
    url,
    headers,
    body,
    responseStart,
    responseChunk,
    responseEnd,
    fail,
    includeCookies = true,
    uploadProgress = null,
    uploadComplete = null
  }) {
    if (!sidecarConnected) {
      diagnostics.standardBundleOpaqueDisconnectedRequestCount =
        Number(diagnostics.standardBundleOpaqueDisconnectedRequestCount || 0) + 1;
      body.reader?.cancel?.().catch?.(() => {});
      fail(new TypeError('Flow IPC sidecar disconnected'));
      return '';
    }
    const requestId = `opaque-http-${++nextRequestId}`;
    const storedCookie = includeCookies ? cookieHeader(url) : '';
    if (storedCookie && !headerName(headers, 'cookie')) {
      setHeader(headers, 'Cookie', storedCookie);
    }
    const request = {
      requestId,
      fail,
      url,
      includeCookies,
      requestReader: body.reader,
      requestContentLength: body.contentLength,
      requestCredit: 0,
      requestSequence: 0,
      requestBytes: 0,
      requestCurrent: null,
      requestCurrentOffset: 0,
      requestDone: false,
      requestCancelled: false,
      requestPumping: false,
      responseStart,
      responseChunk,
      responseEnd,
      responseStarted: false,
      responseSequence: 0,
      responseBytes: 0,
      responseOutstandingCredits: 0,
      responseController: null,
      uploadProgress,
      uploadComplete
    };
    pending.set(requestId, request);
    diagnostics.standardBundleOpaqueRequestCount =
      Number(diagnostics.standardBundleOpaqueRequestCount || 0) + 1;
    postEnvelope('opaqueHttpStreamOpen', requestId, {
      method,
      url,
      headers,
      contentLength: body.contentLength
    });
    pumpRequest(requestId, request);
    return requestId;
  }

  async function pumpRequest(requestId, request) {
    if (request.requestPumping || request.requestDone || request.requestCancelled) {
      return;
    }
    request.requestPumping = true;
    try {
      if (!request.requestReader) {
        if (request.requestCancelled) {
          return;
        }
        request.requestDone = true;
        postEnvelope('opaqueHttpStreamEnd', requestId, {
          chunkCount: 0,
          bodyBytes: 0
        });
        request.uploadComplete?.(0, 0);
        return;
      }
      while (request.requestCredit > 0 && !request.requestDone) {
        if (!request.requestCurrent) {
          const next = await request.requestReader.read();
          if (request.requestCancelled) {
            return;
          }
          if (next.done) {
            request.requestDone = true;
            postEnvelope('opaqueHttpStreamEnd', requestId, {
              chunkCount: request.requestSequence,
              bodyBytes: request.requestBytes
            });
            request.uploadComplete?.(
              request.requestBytes,
              request.requestContentLength);
            break;
          }
          request.requestCurrent = next.value instanceof Uint8Array
            ? next.value
            : new Uint8Array(next.value);
          request.requestCurrentOffset = 0;
          if (request.requestCurrent.byteLength === 0) {
            request.requestCurrent = null;
            continue;
          }
        }
        const end = Math.min(
          request.requestCurrent.byteLength,
          request.requestCurrentOffset + transportChunkSize);
        const chunk = request.requestCurrent.subarray(request.requestCurrentOffset, end);
        if (request.requestCancelled) {
          return;
        }
        postEnvelope('opaqueHttpStreamChunk', requestId, {
          chunkSequence: request.requestSequence,
          bodyBase64: bytesToBase64(chunk),
          bodyBytes: chunk.byteLength
        });
        request.requestSequence += 1;
        request.requestBytes += chunk.byteLength;
        request.requestCredit -= 1;
        request.uploadProgress?.(request.requestBytes, request.requestContentLength);
        request.requestCurrentOffset = end;
        if (end === request.requestCurrent.byteLength) {
          request.requestCurrent = null;
        }
      }
    } catch (error) {
      if (request.requestCancelled) {
        return;
      }
      pending.delete(requestId);
      postEnvelope('opaqueHttpStreamCancel', requestId, { reason: 'request-body-error' });
      request.fail(error);
    } finally {
      request.requestPumping = false;
      if (request.requestCredit > 0 && !request.requestDone && !request.requestCancelled) {
        Promise.resolve().then(() => pumpRequest(requestId, request));
      }
    }
  }

  function grantResponseCredit(requestId, request, credit = 1) {
    diagnostics.standardBundleOpaqueResponseCreditAttemptCount =
      Number(diagnostics.standardBundleOpaqueResponseCreditAttemptCount || 0) + 1;
    if (!pending.has(requestId) || credit <= 0) {
      diagnostics.standardBundleOpaqueResponseCreditRejectedCount =
        Number(diagnostics.standardBundleOpaqueResponseCreditRejectedCount || 0) + 1;
      return;
    }
    diagnostics.standardBundleOpaqueResponseCreditCount =
      Number(diagnostics.standardBundleOpaqueResponseCreditCount || 0) + credit;
    request.responseOutstandingCredits += credit;
    postEnvelope('opaqueHttpStreamResponseCredit', requestId, { credit });
  }

  class OpaqueEventTarget {
    constructor() {
      this._listeners = new Map();
    }

    addEventListener(type, listener) {
      if (typeof listener !== 'function' && typeof listener?.handleEvent !== 'function') {
        return;
      }
      const listeners = this._listeners.get(type) || new Set();
      listeners.add(listener);
      this._listeners.set(type, listeners);
    }

    removeEventListener(type, listener) {
      this._listeners.get(type)?.delete(listener);
    }

    _dispatch(type, properties = {}) {
      const event = {
        type,
        target: this,
        currentTarget: this,
        lengthComputable: false,
        loaded: 0,
        total: 0,
        ...properties
      };
      const propertyHandler = this[`on${type}`];
      if (typeof propertyHandler === 'function') {
        propertyHandler.call(this, event);
      }
      for (const listener of this._listeners.get(type) || []) {
        if (typeof listener === 'function') {
          listener.call(this, event);
        } else {
          listener.handleEvent(event);
        }
      }
    }
  }

  class OpaqueIpcXMLHttpRequest extends OpaqueEventTarget {
    constructor() {
      super();
      this.readyState = 0;
      this.status = 0;
      this.statusText = '';
      this.response = null;
      this.responseText = '';
      this.responseType = '';
      this.responseURL = '';
      this.timeout = 0;
      this.withCredentials = false;
      this.upload = new OpaqueEventTarget();
      this.onreadystatechange = null;
      this.onloadstart = null;
      this.onprogress = null;
      this.onload = null;
      this.onerror = null;
      this.ontimeout = null;
      this.onloadend = null;
      this.onabort = null;
      this._headers = {};
      this._responseHeaders = new Map();
      this._delegate = null;
      this._bridged = false;
      this._method = 'GET';
      this._url = '';
      this._requestId = '';
      this._sendStarted = false;
      this._aborted = false;
      this._timeoutId = null;
      this._responseChunks = [];
      this._responseBytes = 0;
    }

    open(method, url, async = true, username, password) {
      const resolved = new URL(String(url), document.baseURI);
      this._method = String(method || 'GET').toUpperCase();
      this._bridged = isBridgedUrl(resolved);
      this._url = this._bridged ? virtualRequestUrl(resolved) : resolved.href;
      if (!this._bridged) {
        this._delegate = new NativeXMLHttpRequest();
        this._wireDelegate();
        this._delegate.open(method, url, async, username, password);
        return;
      }
      if (async === false) {
        throw new Error('Synchronous opaque IPC XMLHttpRequest is not supported');
      }
      this._headers = {};
      this._responseHeaders = new Map();
      this._requestId = '';
      this._sendStarted = false;
      this._aborted = false;
      this.readyState = 1;
      this._dispatch('readystatechange');
    }

    setRequestHeader(name, value) {
      if (this._delegate) {
        this._delegate.setRequestHeader(name, value);
        return;
      }
      if (this.readyState !== 1 || this._sendStarted) {
        throw new Error('setRequestHeader requires an opened, unsent XMLHttpRequest');
      }
      setHeader(this._headers, name, value, true);
    }

    getResponseHeader(name) {
      if (this._delegate) {
        return this._delegate.getResponseHeader(name);
      }
      return this._responseHeaders.get(String(name).toLowerCase()) || null;
    }

    getAllResponseHeaders() {
      if (this._delegate) {
        return this._delegate.getAllResponseHeaders();
      }
      return Array.from(this._responseHeaders.entries())
        .map(([name, value]) => `${name}: ${value}\r\n`)
        .join('');
    }

    overrideMimeType() {
    }

    send(body = null) {
      if (this._delegate) {
        this._delegate.responseType = this.responseType;
        this._delegate.timeout = this.timeout;
        this._delegate.withCredentials = this.withCredentials;
        this._delegate.send(body);
        return;
      }
      if (!this._bridged || this.readyState !== 1 || this._sendStarted) {
        throw new Error('XMLHttpRequest must be opened once before send');
      }
      this._sendStarted = true;
      this._dispatch('loadstart');
      if (this.timeout > 0) {
        this._timeoutId = setTimeout(() => this._fail('timeout'), this.timeout);
      }

      requestBody(body, this._headers).then((requestBodyStream) => {
        if (this._aborted) {
          return;
        }
        const total = requestBodyStream.contentLength;
        this.upload._dispatch('loadstart', {
          lengthComputable: total >= 0,
          loaded: 0,
          total: total >= 0 ? total : 0
        });
        this._requestId = dispatchOpaqueRequest({
          method: this._method,
          url: this._url,
          headers: this._headers,
          body: requestBodyStream,
          responseStart: (payload, request) => this._startResponse(payload, request),
          responseChunk: (bytes) => this._receiveResponseChunk(bytes),
          responseEnd: () => this._endResponse(),
          fail: () => this._fail('error'),
          uploadProgress: (loaded, contentLength) => {
            this.upload._dispatch('progress', {
              lengthComputable: contentLength >= 0,
              loaded,
              total: contentLength >= 0 ? contentLength : 0
            });
          },
          uploadComplete: (loaded, contentLength) => {
            const properties = {
              lengthComputable: contentLength >= 0,
              loaded,
              total: contentLength >= 0 ? contentLength : 0
            };
            this.upload._dispatch('load', properties);
            this.upload._dispatch('loadend', properties);
          }
        });
      }).catch((error) => {
        diagnostics.standardBundleOpaqueRequestError = error?.message || String(error);
        this._fail('error');
      });
    }

    abort() {
      if (this._delegate) {
        this._delegate.abort();
        return;
      }
      this._aborted = true;
      forgetLateResponse(this._requestId);
      this._clearTimeout();
      this.readyState = 0;
      this._dispatch('abort');
      this._dispatch('loadend');
    }

    _wireDelegate() {
      const events = [
        'readystatechange', 'loadstart', 'progress', 'load',
        'error', 'timeout', 'loadend', 'abort'
      ];
      events.forEach((type) => {
        this._delegate.addEventListener(type, (event) => {
          this.readyState = this._delegate.readyState;
          this.status = this._delegate.status;
          this.statusText = this._delegate.statusText;
          this.response = this._delegate.response;
          if (this._delegate.responseType === '' || this._delegate.responseType === 'text') {
            this.responseText = this._delegate.responseText;
          }
          this.responseURL = this._delegate.responseURL;
          this._dispatch(type, {
            lengthComputable: event.lengthComputable,
            loaded: event.loaded,
            total: event.total
          });
        });
      });
    }

    _clearTimeout() {
      if (this._timeoutId != null) {
        clearTimeout(this._timeoutId);
        this._timeoutId = null;
      }
    }

    _fail(type) {
      if (this.readyState === 0 && type !== 'abort') {
        return;
      }
      forgetLateResponse(this._requestId);
      this._clearTimeout();
      this._aborted = true;
      this.status = 0;
      this.statusText = '';
      this.response = null;
      this.responseText = '';
      this.readyState = 4;
      this._dispatch('readystatechange');
      this._dispatch(type);
      this._dispatch('loadend');
    }

    _startResponse(payload, request) {
      this._clearTimeout();
      this.status = Number(payload.status || 0);
      this.statusText = String(payload.statusText || '');
      this.responseURL = this._url;
      this._responseHeaders = normalizeResponseHeaders(payload.headers);
      this.readyState = 2;
      this._dispatch('readystatechange');
      grantResponseCredit(this._requestId, request, 4);
    }

    _receiveResponseChunk(bytes) {
      this._responseChunks.push(bytes);
      this._responseBytes += bytes.byteLength;
      if (this.readyState !== 3) {
        this.readyState = 3;
        this._dispatch('readystatechange');
      }
      this._dispatch('progress', {
        lengthComputable: false,
        loaded: this._responseBytes,
        total: 0
      });
      const request = pending.get(this._requestId);
      if (request) {
        grantResponseCredit(this._requestId, request, 1);
      }
    }

    _endResponse() {
      const bytes = new Uint8Array(this._responseBytes);
      let offset = 0;
      for (const chunk of this._responseChunks) {
        bytes.set(chunk, offset);
        offset += chunk.byteLength;
      }
      const text = new TextDecoder('utf-8').decode(bytes);
      this.responseText = text;
      if (this.responseType === 'arraybuffer') {
        this.response = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
      } else if (this.responseType === 'blob') {
        this.response = new Blob([bytes], {
          type: this.getResponseHeader('content-type') || 'application/octet-stream'
        });
      } else if (this.responseType === 'json') {
        this.response = text ? JSON.parse(text) : null;
      } else {
        this.response = text;
      }
      this.readyState = 4;
      this._dispatch('readystatechange');
      this._dispatch('load');
      this._dispatch('loadend');
    }

    _complete(payload) {
      this._startResponse(payload, pending.get(this._requestId));
      this._receiveResponseChunk(base64ToBytes(payload.bodyBase64));
      this._endResponse();
    }
  }

  Object.assign(OpaqueIpcXMLHttpRequest, {
    UNSENT: 0,
    OPENED: 1,
    HEADERS_RECEIVED: 2,
    LOADING: 3,
    DONE: 4
  });
  Object.assign(OpaqueIpcXMLHttpRequest.prototype, {
    UNSENT: 0,
    OPENED: 1,
    HEADERS_RECEIVED: 2,
    LOADING: 3,
    DONE: 4
  });

  async function opaqueFetch(input, init = {}) {
    const inputRequest = input instanceof NativeRequest ? input : null;
    const resolved = new URL(inputRequest ? inputRequest.url : String(input), document.baseURI);
    if (!isBridgedUrl(resolved)) {
      return NativeFetch(input, init);
    }
    const requestUrl = virtualRequestUrl(resolved);

    const method = String(init.method || inputRequest?.method || 'GET').toUpperCase();
    const headers = headersObject(inputRequest?.headers, init.headers);
    const signal = init.signal || inputRequest?.signal || null;
    const credentials = init.credentials || inputRequest?.credentials || 'same-origin';
    if (signal?.aborted) {
      throw new DOMException('The operation was aborted', 'AbortError');
    }
    let body = Object.prototype.hasOwnProperty.call(init, 'body') ? init.body : null;
    if (body == null && inputRequest?.body && method !== 'GET' && method !== 'HEAD') {
      body = inputRequest.clone().body;
    }
    const requestBodyStream = await requestBody(body, headers);

    return new Promise((resolve, reject) => {
      let requestId = '';
      let settled = false;
      const abort = () => {
        forgetLateResponse(requestId);
        if (!settled) {
          settled = true;
          reject(new DOMException('The operation was aborted', 'AbortError'));
        }
      };
      signal?.addEventListener('abort', abort, { once: true });
      requestId = dispatchOpaqueRequest({
        method,
        url: requestUrl,
        headers,
        body: requestBodyStream,
        responseStart: (payload, request) => {
          const status = Number(payload.status || 200);
          const noBody = status === 204 || status === 205 || status === 304;
          const responseBody = noBody ? null : new NativeReadableStream({
            start(controller) {
              request.responseController = controller;
              grantResponseCredit(request.requestId, request, 1);
            },
            pull() {
              if (request.responseOutstandingCredits === 0) {
                grantResponseCredit(request.requestId, request, 1);
              }
            },
            cancel() {
              forgetLateResponse(request.requestId);
            }
          });
          const response = new NativeResponse(responseBody, {
            status,
            statusText: String(payload.statusText || ''),
            headers: responseHeadersInit(payload.headers)
          });
          try {
            Object.defineProperty(response, 'url', { value: requestUrl });
          } catch {
          }
          settled = true;
          resolve(response);
        },
        responseChunk: (responseBytes, request) => {
          request.responseController?.enqueue(responseBytes);
        },
        responseEnd: (request) => {
          signal?.removeEventListener('abort', abort);
          request.responseController?.close();
        },
        fail: (error) => {
          signal?.removeEventListener('abort', abort);
          if (!settled) {
            settled = true;
            reject(error || new TypeError('Opaque IPC request failed'));
          }
        },
        includeCookies: credentials !== 'omit'
      });
      diagnostics.standardBundleOpaqueFetchRequestCount =
        Number(diagnostics.standardBundleOpaqueFetchRequestCount || 0) + 1;
    });
  }

  function receive(envelope) {
    if (!envelope || envelope.version !== 1) {
      diagnostics.standardBundleOpaqueResponseError =
        `unsupported sidecar version ${String(envelope?.version)}`;
      return;
    }
    const payload = envelope && envelope.payload ? envelope.payload : {};
    const requestId = payload.requestId || envelope.correlationId;
    const request = pending.get(requestId);
    if (!request) {
      if (ignoredResponses.has(requestId)) {
        return;
      }
      diagnostics.standardBundleOpaqueResponseError = `unknown request ${requestId || ''}`;
      return;
    }
    try {
      switch (envelope.type) {
        case 'opaqueHttpStreamRequestCredit':
          request.requestCredit += Number(payload.credit || 0);
          pumpRequest(requestId, request);
          return;
        case 'opaqueHttpStreamResponse':
          if (request.responseStarted) {
            throw new Error('opaque response metadata was received twice');
          }
          request.responseStarted = true;
          if (request.includeCookies) {
            storeResponseCookies(request.url, payload.headers);
          }
          request.responseStart(payload, request);
          return;
        case 'opaqueHttpStreamResponseChunk': {
          if (!request.responseStarted) {
            throw new Error('opaque response chunk arrived before metadata');
          }
          const sequence = Number(payload.chunkSequence);
          if (sequence !== request.responseSequence) {
            throw new Error(
              `unexpected opaque response chunk ${sequence}, expected ${request.responseSequence}`);
          }
          if (request.responseOutstandingCredits <= 0) {
            throw new Error('opaque response exceeded its granted chunk credit');
          }
          const bytes = base64ToBytes(payload.bodyBase64);
          request.responseSequence += 1;
          request.responseBytes += bytes.byteLength;
          request.responseOutstandingCredits -= 1;
          request.responseChunk(bytes, request);
          return;
        }
        case 'opaqueHttpStreamResponseEnd':
          if (Number(payload.chunkCount) !== request.responseSequence
              || Number(payload.bodyBytes) !== request.responseBytes) {
            throw new Error('opaque response completion metadata does not match received chunks');
          }
          pending.delete(requestId);
          request.responseEnd(request);
          diagnostics.standardBundleOpaqueResponseCount =
            Number(diagnostics.standardBundleOpaqueResponseCount || 0) + 1;
          return;
        case 'opaqueHttpStreamError':
          pending.delete(requestId);
          request.responseController?.error?.(
            new TypeError(payload.message || 'Opaque IPC response stream failed'));
          request.fail(new TypeError(payload.message || 'Opaque IPC request failed'));
          return;
        case 'opaqueHttpResponse': {
          if (request.includeCookies) {
            storeResponseCookies(request.url, payload.headers);
          }
          request.responseStart(payload, request);
          const bytes = base64ToBytes(payload.bodyBase64);
          request.responseChunk(bytes, request);
          pending.delete(requestId);
          request.responseEnd(request);
          diagnostics.standardBundleOpaqueResponseCount =
            Number(diagnostics.standardBundleOpaqueResponseCount || 0) + 1;
          return;
        }
        default:
          throw new Error(`unsupported opaque response type ${envelope.type || ''}`);
      }
    } catch (error) {
      pending.delete(requestId);
      diagnostics.standardBundleOpaqueResponseError = error?.message || String(error);
      request.responseController?.error?.(error);
      request.fail(error);
    }
  }

  function reportMeaningfulPaint() {
    if (diagnostics.standardBundleFirstMeaningfulPaintReported || !document.body) {
      return;
    }
    const container = Array.from(document.querySelectorAll('*'))
      .find((element) => element.localName.startsWith('flow-container-'));
    const text = document.body.innerText.trim();
    if (!container || text.length === 0) {
      return;
    }
    diagnostics.standardBundleFirstMeaningfulPaintReported = true;
    const id = `webview-first-meaningful-paint-${Date.now()}`;
    const hostContext = window.VaadinDesktopHostContext || {};
    postToHost({
      version: 1,
      type: 'webview.firstMeaningfulPaint',
      id,
      correlationId: id,
      sessionId: hostContext.sessionId || 'flow-ipc-session',
      uiId: hostContext.uiId || 'main',
      windowId: hostContext.windowId || 'main',
      route: '',
      payload: {
        source:
          diagnostics.flowWebAppMode === 'development-server'
            ? 'vite-development-server'
            : 'standard-vaadin-production-bundle',
        transport: 'opaque-http-stream-ipc',
        bodyTextLength: text.length
      },
      error: null,
      timestamp: Date.now()
    });
  }

  window.VaadinDesktopOpaqueHttp = { receive, sidecarDisconnected };
  window.XMLHttpRequest = OpaqueIpcXMLHttpRequest;
  window.fetch = opaqueFetch;
  diagnostics.standardBundleBridgeInstalled = true;
  diagnostics.flowWebAppMode =
    window.__vatoriFlowWebAppMode || 'production-bundle';
  diagnostics.standardBundleStaticAssetOwner =
    diagnostics.flowWebAppMode === 'development-server'
      ? 'development-web-app-server'
      : 'vaadin-production-build';
  diagnostics.standardBundleRequestSemantics = 'opaque';
  diagnostics.standardBundleOpaqueBodyTransport = 'chunk-credit-cancel';
  diagnostics.standardBundleVirtualRequestOrigin = virtualRequestOrigin;
  diagnostics.standardBundleFetchBridgeInstalled = true;
  diagnostics.standardBundleCookieBridgeInstalled = true;
  diagnostics.standardBundleCookieCount = 0;
  diagnostics.standardBundleOpaqueDisconnectedRequestCount = 0;
  diagnostics.standardBundleBodyInitSupport =
    'string,array-buffer,typed-array,url-search-params,blob,form-data,readable-stream';
  installPushScriptMapping();
  installHistoryBridge();
  window.addEventListener('error', (event) => {
    const source = event.filename
      ? ` at ${event.filename}:${event.lineno || 0}:${event.colno || 0}`
      : '';
    diagnostics.standardBundleBrowserError = event.error?.stack
      || event.error?.message
      || `${event.message || 'browser error'}${source}`;
  });
  window.addEventListener('unhandledrejection', (event) => {
    diagnostics.standardBundleBrowserError = event.reason?.stack
      || event.reason?.message
      || String(event.reason || 'unhandled rejection');
  });
  new MutationObserver(reportMeaningfulPaint).observe(document, {
    childList: true,
    subtree: true,
    characterData: true
  });
  window.addEventListener('DOMContentLoaded', reportMeaningfulPaint);
})();
