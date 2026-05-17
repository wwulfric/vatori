# Flow IPC Protocol v0

协议采用 newline-delimited JSON envelope。每行是一个完整消息，便于 stdio transport、日志和人工调试。

## Envelope

```json
{
  "type": "event",
  "id": "msg-2",
  "correlationId": "trace-2",
  "sessionId": "session-demo",
  "uiId": "ui-1",
  "payload": {},
  "error": null
}
```

字段：

- `type`：消息类型，例如 `bootstrap`、`bootstrapResult`、`event`、`patch`、`resync`、`ack`、`error`。
- `id`：当前消息 ID。
- `correlationId`：跨 host、WebView、Java sidecar 的请求链路 ID。
- `sessionId`：本地 desktop session。
- `uiId`：Flow UI scope。
- `payload`：消息负载。
- `error`：协议错误、运行时错误或权限错误。成功消息为 `null`。

## Message Types

### bootstrap

Host 请求 Java sidecar 创建或恢复 UI。

```json
{"type":"bootstrap","id":"msg-1","correlationId":"trace-1","sessionId":"session-demo","uiId":null,"payload":{"route":"/counter"},"error":null}
```

返回 `bootstrapResult`，payload 内包含 route、metadata 和 patches。

### event

WebView/host 把 DOM event 发送给 Java sidecar。

```json
{"type":"event","id":"msg-2","correlationId":"trace-2","sessionId":"session-demo","uiId":"ui-1","payload":{"nodeId":"counter.increment","eventType":"click","payload":{}},"error":null}
```

返回 `patch`。

### resync

WebView reload 或 host 恢复时请求完整 UI patch。

```json
{"type":"resync","id":"msg-5","correlationId":"trace-5","sessionId":"session-demo","uiId":"ui-1","payload":{"reason":"mock-webview-reload"},"error":null}
```

返回 `patch`，其中 `fullResync=true`。

### shutdown

测试 host 关闭 sidecar。

```json
{"type":"shutdown","id":"msg-6","correlationId":"trace-6","sessionId":"session-demo","uiId":"ui-1","payload":{},"error":null}
```

返回 `ack`。

## Patch Model

当前 patch 是刻意简化的 Flow-like 指令：

```json
{
  "op": "setText",
  "target": "#counter.value",
  "value": {"text": "2"}
}
```

真实 Vaadin Flow 需要把这里映射到 UIDL/JSON patch、node id、sync id、RPC invocation 和 client engine 的实际协议。

