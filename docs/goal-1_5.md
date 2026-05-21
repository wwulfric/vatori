# Goal 1.5: No-Localhost Frontend Bundle Staging

`docs/goal.md` 可视为 **goal-1_0**，`docs/goal-1_1.md` 可视为
**goal-1_1**，`docs/goal-1_2.md` 可视为 **goal-1_2**，
`docs/goal-1_3.md` 可视为 **goal-1_3**，
`docs/goal-1_4.md` 可视为 **goal-1_4**。截至 v1.4，仓库已经形成可运行、
可评审的 Flow IPC frontend asset pipeline：`dev-ipc` 能通过 no-localhost
IPC 资源通道表达并验证 Flow client classpath、组件 classpath frontend、
npm/frontend、theme/static、production bundle、dev-mode bundle 和 generated/lazy
资源族。

v1.4 已经执行 `FlowBootstrap.js` / `FlowClient.js`、`BigDecimalField`
classpath module、focused Checkbox/Select dev-bundle entries，并验证 theme/static CSS、
repository generated-flow-imports fixture、真实 Vaadin dev-bundle
`generated-flow-imports-*.js` chunk、production `stats.json` graph 和 dev-mode
`package-lock.json` graph。它也把剩余缺口明确收敛到 TextField/npm 图谱、
production bundle execution、dev-mode/Vite execution 和 full generated/lazy
`loadOnDemand` execution。

本阶段目标不是继续增加 smoke-only 断言，也不是把新的组件支持写成另一个
WebView 专用 shim；而是把 v1.4 中已经能 request/verify 的真实 Vaadin frontend
artifact 推进到一个可复用的 **no-localhost staging/execution path**。完成后，
`dev-ipc` 应该能证明至少一个原先 verified-only 或 shimmed 的资源族已经通过
staged artifact graph 被 WebView 执行，或被严格定位为可复现、可继续拆解的
WebKit/Vaadin artifact blocker。

## 继承边界

### 已完成的 v1.4 能力

以下能力在 v1.5 中视为基线，不能回退：

- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app`
  继续启动 no-localhost Flow IPC smoke runtime。
- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app`
  继续启动更接近普通开发者项目的 framework validation app。
- Swift/AppKit host 用 `--transport flow-ipc` 打开本地 WKWebView shell，
  不通过 localhost HTTP URL 加载核心 UI path。
- Visible Flow client engine 通过 generic viewport promotion 负责可见 renderer。
- v1/v1.1/v1.2/v1.3/v1.4 fixture gates 和
  `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。
- Frontend manifest 能表达 Flow client classpath、component frontend、
  npm/frontend、theme/static、production bundle、dev-mode bundle 和 generated/lazy
  artifact families。
- BigDecimalField、Checkbox、Select 的当前 v1.4 资源图、事件 RPC、Java listener、
  UIDL patch 和 visible DOM update 继续可验证。
- v1.4 的 theme/static、generated-flow-imports、production stats graph 和
  dev-mode package graph 证据继续存在。

### 本阶段关键约束

- v1.5 验收路径仍然不能依赖 `WKWebView.load(http://127.0.0.1:port)`。
- 不能通过恢复 Servlet/HTTP/WebSocket compatibility transport 满足验收。
- 应用开发者仍然编写普通 Vaadin Flow Java View，不手写 IPC 或 host-specific
  JavaScript。
- 不能把 `@vaadin/text-field`、production bundle 或 generated/lazy 支持实现为
  新的泛化不明的 inline shim。
- 如果某个资源仍无法执行，必须记录 source artifact、staged artifact、
  transitive dependency、WebView error 和 failure classification。
- Compatibility mode 可以继续保留为 `dev` / `dev-swift`，但不能被描述成 Flow IPC
  v1.5 的成功路径。

## 实现范围

### 1. Frontend Staging Resolver v1.5

目标：

- 在 Java sidecar、Swift host 和 WebView shell 之间建立 durable staging abstraction。
- 优先实现一种可复用路径：
  - `.build/vaadin-desktop-flow-ipc/resources/<app>/<hash>/...` staging。
  - app bundle local resource staging。
  - `WKURLSchemeHandler` / custom scheme。
  - staged file URL 加 import-map 或 module specifier rewrite。
- Staging resolver 必须服务真实 Vaadin artifact body，而不是只服务 probe body。
- 每个 staged artifact 至少记录：
  - source path
  - staged path 或 scheme URL
  - artifact family
  - module specifier
  - sha256
  - bytes
  - dependencyOf / requestedBy
  - cache hit / cache miss
  - cleanup policy
  - execution status

验收证据：

- Trace 中有 `resourceStage` 或等价 Host Protocol 事件，能关联
  `resourceRequest` / `resourceResponse` / module execution。
- `webview.dom` 或 inspector summary 能区分 IPC-only verified resource、
  staged resource、executed staged resource、failed staged resource。
- Staging cache 不依赖 smoke app 固定 DOM id；framework app 也能观察到同一 resolver。

### 2. TextField / npm Graph Parity Slice

目标：

- 优先处理 v1.4 仍保留的 `@vaadin/text-field/src/vaadin-text-field.js` shim。
- 尝试用真实 Vaadin dev-bundle 或 production bundle staged resource 替代该 shim。
- 如果替代后仍触发 `outlet._root` 或其他 WebView execution error，必须将错误定位到：
  - 具体 staged module
  - 具体 transitive import
  - 具体 browser exception
  - 是否与 direct replacement、generated imports、bundle bootstrap order 或 opaque origin 相关
- 不允许把 TextField 仍走 shim 的路径计入 v1.5 成功验收。

验收证据：

- `FlowClientResourceSmokeMain` 或等价 smoke 检查 TextField/npm graph metadata。
- Inspector v1.5 gate 能判断：
  - TextField staged graph executed successfully，或
  - TextField staged graph failed with exact blocker evidence。
- 文档明确 `@vaadin/text-field` shim 是否已删除；若未删除，必须说明它只是旧路径回归保护，
  不是 v1.5 成功路径。

### 3. Execute One Previously Verified-Only Resource Family

v1.4 已经验证 production stats graph、dev-mode package graph 和 generated/lazy chunks，
但这些仍主要是 verified-only。v1.5 至少要把其中一个资源族推进到 controlled execution。

优先级：

1. Generated/lazy `generated-flow-imports-*.js` chunk 通过 staging 被 WebView import，
   并与 Flow client `loadOnDemand` 或等价 lazy path 关联。
2. Production bundle chunk 通过 staging 被 WebView import，至少执行一个可验证 entry
   或 component dependency。
3. Dev-mode/Vite adjacent resource 通过 staging 被 WebView 加载并严格记录尚未执行的
   Vite runtime blocker。

要求：

- 只能把真实 Vaadin artifact execution 计为成功。
- 若选择 generated/lazy path，必须记录 `Flow.loadOnDemand` 调用或无法调用的原因。
- 若选择 production bundle path，必须记录 stats graph 到 staged chunk 的解析链路。
- 若选择 dev-mode path，必须记录 Vite server 假设、缺失 endpoint、HMR 或 module graph blocker。

验收证据：

- v1.5 trace 中至少一个 v1.4 verified-only resource family 变成
  `executionStatus=executed`，或产生严格 blocker 且带完整 staging evidence。
- `webview.dom` 记录具体执行路径，例如 `frontendArtifactGeneratedLazyExecuted=true`
  或 `frontendArtifactProductionBundleChunkExecuted=true`。
- 未执行的 transitive dependencies 有 `blockedReason` / `failureClassification`。

### 4. Flow Client Lazy / LoadOnDemand Boundary

目标：

- 把 v1.4 的 `Flow.loadOnDemand-minimum-probe` 推进为更接近真实 Flow client lazy path
  的验证。
- WebView shell 应尝试从 initialized Flow client engine 触发 lazy import。
- Java sidecar / host 必须能把 lazy request 关联回 manifest/staging resolver。

验收证据：

- Trace 中能看到 lazy request 的调用来源、resource family、resolver 和 staged target。
- 如果 Flow client 内部 API 暂不可稳定调用，必须把 blocker 定位到具体 Flow client API
  或 bootstrap config 缺口，而不是只保留 probe-only lookup。

### 5. IPC Protocol v1.5 And Inspector Gate

目标：

- 新增或扩展 inspector 参数：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
```

- 保持以下命令继续通过：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

- v1.5 strict gate 至少要求：
  - no-localhost `transport=flow-ipc`
  - v1.4 frontend manifest baseline
  - resource staging evidence
  - one staged artifact execution or strict staged blocker
  - TextField/npm graph execution or exact blocker evidence
  - generated/lazy or production/dev bundle execution attempt evidence
  - event RPC -> Java listener -> UIDL patch -> visible DOM baseline still passing
  - failure classification fields for every not-executed staged dependency。

交付：

- 新增 `docs/traces/flow-ipc-v1_5.ndjson`。
- 更新 `docs/ipc-protocol-v1.md`，必要时新增 `docs/ipc-protocol-v1_5.md`。
- `DesktopTraceInspector` 支持 `--require-flow-ipc-v1_5`。

### 6. Verification Apps

目标：

- 保留 `test-apps/flow-over-ipc-smoke-app` 作为 strict fixture source。
- 保留 `test-apps/flow-over-ipc-framework-app` 作为普通开发者形态验证。
- 允许新增一个 frontend-staging focused app，但只有当隔离 production/dev/generated
  resource 更清晰时才新增。

验证 app 必须保持：

- 普通 Flow Java API。
- Button baseline。
- BigDecimalField 或 TextField path。
- Checkbox 和 Select baseline。
- Repeated/list update baseline。
- 至少一个 manifest-gated native capability call。

自动化 hook 只能触发验证，不能成为 runtime 正常路径。

### 7. Documentation

更新或新增：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md` 或 `docs/ipc-protocol-v1_5.md`
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`

文档必须明确：

- v1.4 与 v1.5 的差异。
- Staging resolver 的所有权边界：Java sidecar、Swift host、WKWebView 各自负责什么。
- 哪些 artifacts 是 IPC verified-only，哪些已经 staged，哪些已经 executed。
- TextField/npm graph 当前是否仍需要 shim。
- production bundle、dev-mode/Vite、generated/lazy `loadOnDemand` 的当前边界。
- 为什么该路径仍然不是 localhost WebView wrapper。

## 验收标准

1. v1 fixture gate 继续通过。
2. v1.1 fixture gate 继续通过。
3. v1.2 fixture gate 继续通过。
4. v1.3 fixture gate 继续通过。
5. v1.4 fixture gate 继续通过。
6. `--require-flow-ipc-v1_3-framework` host-log gate 继续通过。
7. 新增 v1.5 fixture gate，并且 `--require-flow-ipc-v1_5` 通过。
8. 真实 macOS `dev-ipc` 窗口运行通过，无 localhost HTTP。
9. Trace 能证明至少一个真实 Vaadin frontend artifact 被 staged，并被 WebView 执行，
   或被 strict blocker 精确定位到具体 artifact/dependency/browser error。
10. TextField/npm graph 不再只有泛化 shim 证据；必须有真实 staged graph execution，
    或 exact staged blocker evidence。
11. 至少一个 v1.4 verified-only 的 production/dev/generated/lazy resource family
    进入 execution attempt，并产出 executed 或 strict blocker 证据。
12. BigDecimalField、Checkbox、Select、Button、list update 和 native capability baseline
    不回退。
13. Docs 明确哪些是框架能力，哪些只是 focused partial bridge、diagnostic shim 或 blocker。
14. `git diff --check`、Swift compile、Maven compile、resource smoke 通过。
15. 真实窗口运行结束后没有 Java sidecar 或 native host 残留。
16. 按适合粒度提交 commit。

## 非目标

- 不要求一次完成完整 Vaadin component parity。
- 不要求完整 Grid/VirtualList。
- 不要求完整 Flow Push/WebSocket parity 或 durable reconnect。
- 不要求生产级签名、公证、安装器、自动更新。
- 不要求 Windows/Linux host。
- 不删除 compatibility mode。
- 不要求本阶段 upstream 到 Vaadin Flow。
- 不要求完整 Vite dev server 替代；本阶段可以把 Vite 假设收敛为严格 blocker。

## 停止条件

当前仓库形成一个可运行、可评审的 **Flow IPC v1.5 no-localhost frontend staging slice**：

- `dev-ipc` 不只验证资源图，也能通过 staging resolver 将至少一个真实 frontend
  artifact 带入 WebView execution path。
- TextField/npm graph 的剩余 shim 被替代，或被严格定位为 staged artifact blocker。
- generated/lazy、production bundle 或 dev-mode bundle 至少一个资源族从 verified-only
  推进到 execution attempt。
- v1/v1.1/v1.2/v1.3/v1.4/v1.5 trace gates 和真实窗口 host log 都有 strict inspector 证据。
- 文档清楚说明已完成的 staging 能力、仍保留的 partial bridge / diagnostic shim、
  blocked resource 和下一阶段拆解。

## 关键约束

本 goal 的关键验收约束是：

**把 v1.4 的 frontend artifact graph 从 request/verify 推进到 no-localhost
staging/execution；如果某个真实 Vaadin artifact 不能执行，必须给出可复现的 staged
blocker，而不是继续扩大未定位的 shim。**
