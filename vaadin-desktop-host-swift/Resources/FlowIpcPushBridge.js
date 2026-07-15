(() => {
  if (window.VaadinDesktopPush) {
    return;
  }

  const diagnostics = window.VaadinDesktopIpcDiagnostics =
    window.VaadinDesktopIpcDiagnostics || {};
  const subscriptions = new Map();
  let nextConnectionId = 0;

  function postToHost(type, payload) {
    const handler = window.webkit
      && window.webkit.messageHandlers
      && window.webkit.messageHandlers.flowIpc;
    if (!handler) {
      throw new Error('Flow IPC native message handler is unavailable');
    }
    const id = `${type}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const hostContext = window.VaadinDesktopHostContext || {};
    handler.postMessage({
      version: 'flow-ipc-v1',
      type,
      id,
      correlationId: id,
      sessionId: hostContext.sessionId || 'flow-ipc-session',
      uiId: hostContext.uiId || 'main',
      windowId: hostContext.windowId || 'main',
      route: '',
      payload,
      error: null,
      timestamp: Date.now()
    });
  }

  function responseFor(subscription, state, body = '') {
    return {
      status: 200,
      state,
      transport: 'long-polling',
      responseBody: body,
      request: subscription.config
    };
  }

  function connectionMetadata(subscription, lastSeenOverride = null) {
    const url = new URL(subscription.url, document.baseURI);
    const lastSeenHeader = subscription.config.headers
      && subscription.config.headers['X-Vaadin-LastSeenServerSyncId'];
    const lastSeenServerSyncId = typeof lastSeenHeader === 'function'
      ? Number(lastSeenHeader())
      : -1;
    return {
      connectionId: subscription.id,
      flowUiId: Number(url.searchParams.get('v-uiId')),
      pushId: url.searchParams.get('v-pushId') || '',
      lastSeenServerSyncId: lastSeenOverride == null
        ? (Number.isFinite(lastSeenServerSyncId) ? lastSeenServerSyncId : -1)
        : Number(lastSeenOverride),
      transport: 'opaque-ipc-push'
    };
  }

  function connect(subscription, lastSeenOverride = null) {
    postToHost('opaquePushConnect', connectionMetadata(subscription, lastSeenOverride));
    diagnostics.standardBundlePushConnectRequestCount =
      Number(diagnostics.standardBundlePushConnectRequestCount || 0) + 1;
  }

  function subscribe(config) {
    const subscription = {
      id: `opaque-push-${++nextConnectionId}`,
      url: String(config.url || ''),
      config,
      connected: false,
      reopening: false,
      awaitingResync: false,
      lastPushSequence: 0,
      queuedMessages: []
    };
    subscriptions.set(subscription.id, subscription);
    diagnostics.standardBundlePushActiveConnectionId = subscription.id;
    connect(subscription);
    return {
      push() {
        diagnostics.standardBundleUnexpectedBidirectionalPush = true;
        throw new Error('Client messages use the opaque XMLHttpRequest bridge');
      }
    };
  }

  function unsubscribeUrl(url) {
    const target = String(url || '');
    for (const [id, subscription] of subscriptions.entries()) {
      if (subscription.url !== target) {
        continue;
      }
      const metadata = connectionMetadata(subscription);
      postToHost('opaquePushDisconnect', metadata);
      subscriptions.delete(id);
      subscription.connected = false;
    }
  }

  function deliverMessage(subscription, payload) {
    subscription.config.onMessage?.(
      responseFor(subscription, 'messageReceived', String(payload.uidl || '')));
    const previousMessageCount = Number(diagnostics.standardBundlePushMessageCount || 0);
    const sequence = Number(payload.pushSequence || 0);
    if (previousMessageCount === 0) {
      diagnostics.standardBundlePushFirstSequence = sequence;
    }
    diagnostics.standardBundlePushMessageCount = previousMessageCount + 1;
    diagnostics.standardBundlePushLastSequence = sequence;
  }

  function receiveConnected(payload) {
    const subscription = subscriptions.get(payload.connectionId);
    if (!subscription) {
      diagnostics.standardBundlePushError = 'connected response for unknown subscription';
      return;
    }
    subscription.connected = true;
    if (subscription.reopening) {
      subscription.reopening = false;
      subscription.config.onReopen?.(responseFor(subscription, 're-opening'));
    } else {
      subscription.config.onOpen?.(responseFor(subscription, 'opening'));
    }
    diagnostics.standardBundlePushConnected = true;
    diagnostics.standardBundlePushConnectionCount = subscriptions.size;
    for (const queued of subscription.queuedMessages.splice(0)) {
      deliverMessage(subscription, queued);
    }
  }

  function receivePush(payload) {
    const subscription = subscriptions.get(payload.connectionId);
    if (!subscription) {
      diagnostics.standardBundlePushError = 'push for unknown subscription';
      return;
    }
    const sequence = Number(payload.pushSequence || 0);
    if (subscription.awaitingResync && !payload.resync) {
      diagnostics.standardBundlePushDeferredWhileResyncCount =
        Number(diagnostics.standardBundlePushDeferredWhileResyncCount || 0) + 1;
      return;
    }
    if (!payload.resync && sequence <= subscription.lastPushSequence) {
      diagnostics.standardBundlePushDuplicateCount =
        Number(diagnostics.standardBundlePushDuplicateCount || 0) + 1;
      return;
    }
    if (!payload.resync && subscription.lastPushSequence > 0
        && sequence !== subscription.lastPushSequence + 1) {
      diagnostics.standardBundlePushGapCount =
        Number(diagnostics.standardBundlePushGapCount || 0) + 1;
      subscription.awaitingResync = true;
      postToHost('opaquePushResync', connectionMetadata(subscription));
      return;
    }
    subscription.lastPushSequence = sequence;
    if (payload.resync) {
      subscription.awaitingResync = false;
      diagnostics.standardBundlePushResyncCount =
        Number(diagnostics.standardBundlePushResyncCount || 0) + 1;
    }
    if (!subscription.connected) {
      subscription.queuedMessages.push(payload);
      return;
    }
    deliverMessage(subscription, payload);
  }

  function receive(envelope) {
    const payload = envelope?.payload || {};
    if (envelope?.type === 'opaquePushConnected') {
      receiveConnected(payload);
    } else if (envelope?.type === 'opaquePush') {
      receivePush(payload);
    }
  }

  function reconnect() {
    for (const subscription of subscriptions.values()) {
      subscription.connected = false;
      subscription.reopening = true;
      subscription.config.onReconnect?.(
        subscription.config,
        responseFor(subscription, 're-connecting'));
      postToHost('opaquePushDisconnect', connectionMetadata(subscription));
      const currentLastSeen = connectionMetadata(subscription).lastSeenServerSyncId;
      connect(subscription, currentLastSeen);
      diagnostics.standardBundlePushReconnectCount =
        Number(diagnostics.standardBundlePushReconnectCount || 0) + 1;
    }
  }

  function sidecarDisconnected() {
    diagnostics.standardBundleSidecarDisconnected = true;
    diagnostics.standardBundlePushConnected = false;
    for (const subscription of subscriptions.values()) {
      subscription.connected = false;
      subscription.queuedMessages.length = 0;
    }
  }

  window.vaadinPush = window.vaadinPush || {};
  window.vaadinPush.atmosphere = {
    version: 'vatori-opaque-ipc',
    subscribe,
    unsubscribeUrl
  };
  window.VaadinDesktopPush = { receive, reconnect, sidecarDisconnected };
  diagnostics.standardBundlePushBridgeInstalled = true;
  diagnostics.standardBundlePushTransport = 'opaque-ipc-push';
  diagnostics.standardBundlePushMessageCount = 0;
  diagnostics.standardBundlePushFirstSequence = 0;
  diagnostics.standardBundlePushReconnectCount = 0;
  diagnostics.standardBundlePushResyncCount = 0;
})();
