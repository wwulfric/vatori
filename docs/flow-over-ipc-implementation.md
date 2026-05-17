# Vaadin Flow over IPC Phase 0/1 实现总结

本文记录当前仓库中 Phase 0/Phase 1 原型的实现状态。设计依据是 `docs/flow-over-ipc-design.md`。

## 当前实际架构

```text
MockHostMain
  -> stdio NDJSON IPC
  -> SidecarMain
  -> FlowTransport SPI
  -> MockFlowRuntime
  -> PatchResult
  -> MockWebView.apply(...)
```

当前使用 Java 标准库实现，不依赖外部 HTTP server、Servlet container、Maven、Gradle 或网络下载。

## 与设计文档的对应关系

- Java Flow Core / UI State：由 `MockFlowRuntime` 表达服务端 UI state、listener 和 patch。
- Counter/Grid demo：由 `CounterView` 和 `OrdersGridView` 表达 Java view 与事件处理逻辑。
- Flow Transport Abstraction：由 `FlowTransport` 表达 bootstrap、dispatch、resync、push extension point。
- IPC Bridge：由 `IpcEnvelope`、`IpcCodec`、`SidecarMain` 和 stdio NDJSON 表达。
- Native Host：由 `MockHostMain` 表达 headless host，模拟启动 sidecar、WebView event 转发和 patch apply。
- System WebView Runtime：当前是 `MockWebView`，未接入真实 WebView。
- Native Capability Gateway：暂不实现，仅在协议中保留 capability/metadata 位置。

## 已验证结论

1. 一个 Flow-like server-driven UI 可以在没有 HTTP server 的情况下完成：
   - bootstrap
   - session/ui scope 创建
   - button click event
   - Java listener 更新状态
   - patch 返回 host
   - reload/resync

2. stdio newline-delimited JSON 可以作为 Phase 1 最小 IPC：
   - 易打印、易复制、易写 trace。
   - 可以承载 `type/id/correlationId/sessionId/uiId/payload/error` envelope。
   - 足够验证 host 和 Java sidecar 的职责边界。

3. host 与 sidecar 的错误边界可以先稳定下来：
   - sidecar stdout 只传协议消息。
   - sidecar stderr 传 trace。
   - host 可以在 sidecar 异常退出或返回 error envelope 时给出明确错误。

## 未验证结论

这些结论仍需真实 Vaadin Flow 代码级 spike：

- Flow 是否能在不创建 `VaadinServletService` 的情况下初始化 UI。
- `VaadinSession` 是否可以变成 desktop session，还是必须兼容 HTTP session。
- Flow bootstrap 是否能脱离 HTTP response 和 servlet resource loading。
- UIDL/JSON patch 生成是否能直接复用，而不是重写一套 patch model。
- client engine 是否能通过本地 asset protocol 初始化。
- push、upload/download、dynamic resources、dev mode 是否能映射到 IPC。
- Vaadin add-on 访问 request/session/filter/security context 的比例。

## 发现的 Flow Servlet/HTTP 假设

当前原型尚未拉入真实 Flow 源码，因此这里是设计层面的待验证清单，不是代码审计结论：

| 区域 | 典型 Web/Servlet 假设 | Desktop transport 需要验证的抽象 |
| --- | --- | --- |
| Bootstrap | 从 HTTP request/response 生成 bootstrap page 和初始 UIDL | 能否由 host 请求 `bootstrap` 并从本地 asset protocol 加载 client engine |
| Session/UI scope | HTTP session、VaadinSession、service locking、request lifecycle | 能否替换为 desktop session/ui scope，并保留线程安全规则 |
| Event dispatch | Client RPC/UIDL request handler 接收浏览器事件 | 能否把 IPC `event` 映射到同一个 Java listener 调用链 |
| Patch response | UIDL/JSON 写入 HTTP response | 能否直接生成 patch payload，由 IPC 返回 host/WebView |
| Push | WebSocket/Atmosphere 等 server push transport | 能否映射到 IPC event channel |
| Resource loading | Servlet path、theme、frontend dev mode、dynamic resources | 能否改成本地 asset protocol 和受控 resource channel |
| Upload/download | HTTP multipart、download response stream | 能否拆成独立 binary/stream channel |
| Web integrations | Spring Security、Servlet filter、OAuth redirect、reverse proxy | Desktop session 下需要重定义语义或声明不兼容 |

## 当前限制

- 没有真实 Vaadin Flow 依赖。
- 没有真实 DOM 或 WebView，只是 mock apply patch。
- patch model 是简化模型，不是 Vaadin UIDL。
- 没有二进制 channel、backpressure、cancellation。
- 没有权限 manifest 或 native capability gateway。
- 没有 jlink/jpackage/GraalVM packaging。

## 下一步建议

下一步应优先做真实 Vaadin Flow 源码级 spike，而不是继续扩展 mock runtime：

1. 建立一个带 Vaadin Flow 依赖的最小 Java 项目。
2. 找到 client event 到 Java listener 的入口。
3. 找到 Java 组件树变化到 UIDL/JSON response 的出口。
4. 尝试用 `FlowTransport` 包一层最小 adapter。
5. 若 adapter 需要大量 servlet mock，记录必须抽出的 Flow SPI。
6. 只有 button click -> listener -> UIDL/patch 成功后，再接真实 WebView host。

## 可拆 issue

1. `flow-real-code-audit`: 基于 Vaadin Flow 源码定位 bootstrap/event/patch 入口出口。
2. `flow-servlet-dependency-map`: 输出 Servlet/HTTP/WebSocket 依赖图。
3. `flow-transport-adapter-spike`: 用真实 Flow 内部类尝试 adapter。
4. `webview-host-tauri-spike`: 用 Tauri 替换 `MockHostMain`。
5. `asset-protocol-bootstrap`: 验证本地 asset protocol 加载 Flow client engine。
6. `ipc-inspector`: 把当前 trace 扩展为可筛选 inspector。
7. `runtime-packaging-jlink`: 将 sidecar 和 host 打包成 jlink/jpackage demo。
