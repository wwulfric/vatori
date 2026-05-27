# Goal: Vaadin Desktop Framework Long-Term Vision

本文件是仓库的长期总目标，用于持续跟踪 `Vaadin Desktop Framework` 从当前
v0/v1 runtime 切片走向可产品化桌面框架的最终完成状态。

历史阶段目标保留为不可变的阶段快照：

- `docs/goal-1_0.md`：Flow over IPC Runtime v1，已完成。
- `docs/goal-1_1.md`：Flow IPC runtime hardening，已完成。
- `docs/goal-1_2.md`：first real Vaadin Web Component over Flow IPC，已完成。
- `docs/goal-1_3.md`：framework-shaped Flow IPC validation app，已完成。
- `docs/goal-1_4.md`：Flow IPC frontend asset pipeline，已完成。
- `docs/goal-1_5.md`：no-localhost frontend bundle staging，已完成。
- `docs/goal-1_6.md`：TextField / npm graph parity，已完成。
- `docs/goal-1_7.md`：TextField production chunk side-effect split，已完成。
- `docs/goal-1_8.md`：Generated/lazy frontend execution boundary，已完成。
- `docs/goal-1_9.md`：Flow-client-triggered generated/lazy loading contract，已完成。
- `docs/goal-1_10.md`：Flow lazy dependency and production bundle runtime slice，已完成。
- `docs/goal-1_11.md`：Generated `Flow.loadOnDemand` chunk runtime path，已完成。
- `docs/goal-1_12.md`：Generated LoadOnDemand chunk file registry and side-effect proof，已完成。
- `docs/goal-1_13.md`：Production Bundle Owned Runtime Boundary，已完成。
- `docs/goal-1_14.md`：Production Owner Import and Side-Effect Boundary，已完成。
- `docs/goal-1_15.md`：Production Owner Dependency Resolver Closure，已完成。
- `docs/goal-1_16.md`：Production Owner Import Ordering and Component Reuse，已完成。
- `docs/goal-1_17.md`：Host-Owned Frontend Resource Scheme，已完成。
- `docs/goal-1_18.md`：Production Owner Custom-Scheme Resource Graph，已完成。

当前已完成到 `docs/goal-1_18.md`。建议下一个阶段继续从 production owner
custom-scheme graph 往真实 owner import / side-effect execution、generated chunk-file
execution 或 dev-mode/Vite execution 推进，而不是停留在 resource loader 证明层。

后续阶段应继续以 `docs/goal-1_19.md` 等文件拆分，
每个阶段只拿走本长期目标中的一个可评审切片。不要把本文件改写成短期任务清单；
它是长期北极星和完成度账本。

## 最终愿景

构建一个真实可用的 `Vaadin Desktop Framework`：

- Java 开发者继续使用普通 Vaadin Flow 编程模型，例如 `@Route`、`Button`、
  `Grid`、`FormLayout`、`Upload`、`Dialog`、`Binder`、listeners 和 server-side state。
- 桌面应用通过系统 WebView 渲染，但核心 UI transport 不依赖 localhost
  HTTP/Servlet/WebSocket。
- Flow bootstrap、client event、UIDL patch、server push、resync、resource loading、
  command 和 capability result 都有清晰 IPC contract。
- Swift/AppKit、Windows WebView2 和 Linux WebKitGTK host 管理窗口、菜单、托盘、
  clipboard、dialog、notification、drag/drop、快捷键、生命周期、打包和更新。
- Compatibility mode 明确保留为开发/迁移 fallback，但不伪装成最终架构。
- 框架像 Tauri + Vaadin Flow 的组合：开发者写 Java/Vaadin，框架负责 native host、
  IPC、能力网关、打包和运行时边界。

## 当前基线

截至 goal 1.18，仓库已经证明：

- `dev-ipc` 可以启动 no-localhost Flow IPC runtime。
- 真实 macOS Swift/AppKit/WKWebView 窗口可以运行 Flow IPC smoke app 和 framework
  validation app。
- Button、BigDecimalField、Checkbox、Select、list/repeated update、desktop capability
  baseline 都有 trace gate 或 host-log gate 证据。
- Java sidecar 通过 `UidlWriter`、`UidlRequestHandler`、`ServerRpcHandler`、
  desktop `VaadinRequest` / `VaadinResponse` adapter 切片生成真实 UIDL。
- WebView 中可见 Flow client engine 消费 bootstrap、event patch、push 和 resync。
- Flow client classpath resources、component frontend、theme/static、production bundle、
  dev-mode bundle 和 generated/lazy artifact families 已能通过 IPC manifest 表达和验证。
- v1.5 已建立 no-localhost frontend staging resolver：真实 artifact body 可被 stage 成
  Blob-backed module URL，并以 `resourceStage` trace 记录 staged、executed、failed 和
  strict blocker 状态。
- v1.6 已把 TextField/npm graph 的旧宽泛 blocker 收敛为可复现 staged module blocker：
  `META-INF/vatori/dev-bundle-entry/vaadin-text-field.js` 通过同一 no-localhost staging
  resolver 进入 direct probe，`./indexhtml-BfYYZi8S.js` 被重写到 staged dependency，
  并在 BigDecimalField 路径已经定义 `vaadin-text-field` 后触发 duplicate custom
  element definition。trace 记录 `blockerVersion=flow-ipc-textfield-v1_6`、
  custom element registry before/after、transitive artifacts、browser error、
  failure layer 和下一步拆解路径。
- v1.7 已越过 v1.6 duplicate custom element blocker：TextField entry 和 production
  `indexhtml` dependency 使用 stable resource-path Blob module identity，真实
  `vaadin-text-field` graph 执行，旧 TextField shim 只保留为 legacy fallback，
  `test-apps/flow-over-ipc-smoke-app` 中的真实 Flow `TextField` Java listener 能经
  visible Flow client engine -> IPC-backed XHR -> Java sidecar -> `UidlRequestHandler`
  -> UIDL patch -> visible DOM 更新为 `Vaadin TextField: Flow IPC typed`。
- v1.8 已越过 generated/lazy staged graph 的 `./markdown-helpers-31I0A-hg.js`
  unresolved relative import blocker：真实 Vaadin dev-bundle
  `generated-flow-imports-VW7hRaNw.js` 和 `markdown-helpers-31I0A-hg.js` 通过
  stable resource-path Blob module identity stage，relative import 被重写到 staged
  dependency，`resourceStage` / `webview.dom` 记录
  `flow-ipc-generated-lazy-resolver-v1_8`、resolved dependency、module identity、
  `frontendArtifactGeneratedLazyExecuted=true` 和
  `api-installed-by-staged-generated-flow-imports` 的受控 `Flow.loadOnDemand` probe。
- v1.9 已把 generated/lazy `Flow.loadOnDemand` blocker 从 direct staged import probe
  收敛到明确的 API / registry / integration 层：trace 记录
  `generatedLazyLoadOnDemandContractVersion=flow-ipc-loadondemand-v1_9`、
  API owner、install source、requested chunk、registry/mapping source、
  resolved artifact path、`blockerVersion=flow-ipc-loadondemand-v1_9`、
  `failureLayer=loadondemand-api-installed-by-generated-imports-not-flow-client-triggered`、
  `frontendArtifactGeneratedLazyDiagnosticBridge=true` 和
  `frontendArtifactGeneratedLazyFlowClientTriggered=false`。真实 dev-bundle
  generated/lazy artifact 仍会执行；它仍不是最终 Flow-client-engine-triggered 成功。
- v1.10 已增加真实 Flow `LoadMode.LAZY` dependency runtime path：smoke app 和 framework app
  都通过普通 `@JavaScript(..., loadMode = LoadMode.LAZY)` 声明
  `context://vatori-flow-ipc-lazy-v1_10.js`；可见 Flow client / DependencyLoader 创建的
  lazy script 会被 WebView bridge 拦截为 IPC `resourceRequest`，Java 通过 no-localhost
  resolver 返回真实 lazy body，host 将其 stage 为 Blob module URL 并执行。trace 记录
  `flow-ipc-lazy-dependency-v1_10`、`flowLazyDependencyTriggeredByFlowClient=true`、
  `flowLazyDependencyExecuted=true` 和
  `flowLazyDependencyExecutionResult=executed:vatori-flow-ipc-lazy-v1_10.js`。
- v1.10 也把 production bundle 从 `stats.json` verified-only 推进到 runtime registry /
  staged execution attempt：`vaadin-dev-bundle/config/stats.json` 选择
  `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js`，trace 记录
  `flow-ipc-production-runtime-v1_10`、stats source、selected artifact、dependency chain、
  stable module identity 和 `execution-attempt`。完整 production runtime ownership 仍是
  blocker，failure layer 是
  `production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports`。
- v1.11 已把 generated `Flow.loadOnDemand(chunkId)` 从 v1.9 diagnostic bridge 推进到真实
  UIDL lazy expression path：host 从 bootstrap UIDL dependency 捕获
  `return window.Vaadin.Flow.loadOnDemand('<chunkId>');`，可见 Flow client /
  `DependencyLoader` 调用 `window.Vaadin.Flow.loadOnDemand(chunkId)` 时由 no-localhost bridge
  转成 `purpose=generated-loadondemand-chunk-v1_11` 的 IPC resource request，映射到真实
  `vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js` artifact，并以
  stable resource-path module identity stage/import。trace 记录
  `flow-ipc-loadondemand-chunk-v1_11`、`generatedLazyExpressionSource=bootstrap-uidl-dependencies`、
  `loadOnDemandChunkTriggeredByFlowClient=true`、registry/mapping source、resolved artifact、
  stage evidence 和精确 blocker
  `failureLayer=dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file`。这已经越过
  v1.9 diagnostic bridge 和 v1.10 ordinary `LoadMode.LAZY` 层，但还不是完整 chunk-id ->
  real chunk file / side-effect ownership parity。
- v1.12 已把 v1.11 的 generic generated-flow-imports no-op blocker 收敛到真实
  generated-flow-imports body parser 证据：parser 读取
  `vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js`，识别当前
  Vaadin 输出的 `loadOnDemand` table 是 `Promise.resolve(0)` no-op，没有
  `./chunks/chunk-*.js` branch。trace 记录
  `flow-ipc-loadondemand-chunk-file-v1_12`、
  `generatedLoadOnDemandChunkFileRegistrySource=generated-flow-imports-body-parser-v1_12`、
  `loadOnDemandTableShape=no-op-promise-resolve-0`、`chunkFileStatus=not-generated`、
  `loadOnDemandChunkFileCallbackResolved=true` 和
  `loadOnDemandChunkFileFailureLayer=chunk-file-not-generated`。这证明了当前 app/build 没有
  真实 generated lazy chunk file；它不是完整 generated chunk-file execution 成功。
- v1.13 已把 v1.10 的宽泛 production runtime staged attempt 推进成明确的
  production bundle runtime owner 边界：Java manifest / resource response 声明
  `productionBundleRuntimeOwnerVersion=flow-ipc-production-owner-v1_13`、
  `productionBundleRuntimeOwner=flow-ipc-production-bundle-runtime`、entry artifact、
  stats source、selection rule、dependency chain、owner module identity、cache/cleanup
  strategy 和 component reuse 语义；Swift host 使用独立 owner namespace stage 真实
  `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js` artifact，并在
  `resourceStage` / `webview.dom` 记录 `owner=production-bundle-runtime`、registry
  before/after、side-effect proof、component owner reuse=false 和更精确 blocker
  `production-owner-runtime-contract-missing`。这不是完整 production bundle import
  success；它是 owner contract evidence，避免把 focused component import 成功继续算作
  production owner 成功。
- v1.14 已把 v1.13 的 owner contract 推进到真实 production owner import / side-effect
  probe：Java manifest / resource response 声明
  `productionBundleRuntimeOwnerImportVersion=flow-ipc-production-owner-import-v1_14`、
  import policy、artifact、expected side effects、acceptable failures、rewrite strategy 和
  component import order；Swift host 在 component focused imports 之后，由
  `owner=production-bundle-runtime` 对真实 stats-selected
  `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js` 发起受控 dynamic import。
  Trace 记录 import attempted、module identity、dependency rewrite result、resolved /
  unresolved specifiers、`window.Vaadin` before/after、custom element registry before/after、
  browser error/name、side-effect proof、component owner reuse=false 和最终 failure layer
  `production-owner-relative-import-unresolved`。这不是 production owner import success；
  但它不再停在 `production-owner-runtime-contract-missing`，而是来自真实 owner import
  attempt 的具体 blocker 证据。
- v1.15 已把 v1.14 的 focused single-dependency rewrite 推进到 owner-scoped relative
  dependency graph closure：Java manifest / response 声明
  `productionBundleRuntimeOwnerDependencyResolverVersion=flow-ipc-production-owner-deps-v1_15`、
  resolver root、policy、expected artifacts、acceptable failures 和 import phase diagnostics；Swift host 对真实
  stats-selected `indexhtml` artifact 递归扫描 relative imports，请求并 stage 真实存在的
  dev-bundle build artifacts，记录 graph total/resolved/missing/staged count、module identity
  namespace、dependency classifications、phase candidates、missing dependency paths/specifiers 和 import rewrite
  result。当前真实 trace 解析并 stage 了 24 个 owner dependency，并把 53 个缺失真实 artifact
  明确分类为 `missing-real-artifact`；最终 blocker 从未分类的
  `production-owner-relative-import-unresolved` 收敛为 `production-owner-missing-real-artifact`。
  这仍不是 production owner import success，也不是 broad production parity。
- v1.16 已把 production owner 从单纯 after-component diagnostic probe 推进到可审计
  import ordering / component reuse 边界：Java manifest / response 声明
  `productionBundleRuntimeOwnerImportOrderingVersion=flow-ipc-production-owner-ordering-v1_16`
  和 `componentProductionOwnerReuseVersion=flow-ipc-component-owner-reuse-v1_16`；
  Swift host 在 component focused imports 之前记录 `before-component-imports` owner-first
  preflight，在 fallback owner import 后记录 `after-component-imports` 结果，并把 observed
  phases、ordering result、component reuse mode/reason、duplicate custom element browser error
  和 final blocker 写入 `resourceStage` / `webview.dom`。当前真实 trace 的 acceptable 完成形态是：
  owner-first preflight 真实发生，after-component fallback 到达真实 duplicate custom element
  blocker，component resolver 明确拒绝复用 owner side effects，最终层收敛到
  `production-owner-duplicate-custom-element` / component reuse contract，而不是回退到
  dependency resolver 或 runtime contract missing。
- v1.17 已把 frontend resource loading 从纯 WebView Blob staging 推进到 host-owned
  custom-scheme/file-backed loader 边界：Java manifest / resource response 声明
  `frontendResourceLoaderVersion=flow-ipc-resource-scheme-v1_17`、
  `frontendResourceLoaderModes=custom-scheme,file-backed,blob-fallback`、
  `frontendResourceLoaderPreferredMode=custom-scheme-file-backed` 和 no-localhost contract；
  Swift host 安装 `vatori-resource` `WKURLSchemeHandler`，将真实 Flow
  `LoadMode.LAZY` dependency body 写入
  `.build/vaadin-desktop-host-swift/flow-ipc-resources`，再通过
  `vatori-resource://...` 导入执行。Trace 在 `resourceLoad` / `resourceStage` /
  `webview.dom` 中记录 scheme handler install、request/response、MIME、byte count、
  sha256、redacted file path、relative import base、cleanup policy、no-localhost proof 和
  executed status。production owner root/dependency graph 仍保留显式 Blob fallback，
  并记录 fallback reason、blocked layer 和下一步 custom-scheme owner graph work。
- v1.18 已把 v1.17 的 host-owned resource loader 用到 production owner root/dependency
  graph：Java manifest / resource response 声明
  `productionBundleRuntimeOwnerResourceLoaderVersion=flow-ipc-production-owner-resource-loader-v1_18`、
  `productionBundleRuntimeOwnerResourceLoaderMode=custom-scheme-file-backed`、
  `productionBundleRuntimeOwnerResourceScheme=vatori-resource`、
  `productionBundleRuntimeOwnerResourceGraphMode=owner-root-and-resolved-dependencies`、
  stable source-path base policy 和 root Blob fallback disabled；Swift host 将真实
  stats-selected `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js` root artifact
  及已解析 owner dependencies stage 为
  `vatori-resource://flow-ipc/production-owner/...` file-backed custom-scheme URL，并记录
  root/dependency `resourceLoad` request/response、byte count、sha256、redacted file path、
  resource base URL、relative import base、cache/cleanup policy、no-localhost proof、owner-first
  import over custom scheme、component reuse decision 和 custom-scheme graph layer blocker。
  v1.18 不复用 v1.17 Flow lazy dependency 证据作为 owner graph 成功，也不允许 production
  owner root Blob fallback 作为 v1.18 通过条件。
- 完整 generated/lazy chunk file execution、production owner import/side-effect success
  和 dev-mode/Vite execution 仍是明确 blocker，不计入最终完成。

## 最终完成标准

长期目标完成时，仓库应满足以下条件。

### 1. Flow Over IPC 是默认主线

- `dev-ipc` 或其后继命令是默认推荐的桌面开发路径。
- 核心 UI 闭环不依赖 localhost HTTP/Servlet/WebSocket：
  bootstrap、events、UIDL patches、push、resync 和 resource loading 都通过 IPC。
- Compatibility mode 仍可运行，但文档、日志和 CLI 明确标记为 fallback。
- Runtime 不依赖 smoke-only hooks 才能正常工作。

### 2. Flow Adapter 边界稳定

- `vaadin-desktop-flow` 中有清晰、可维护的 Flow transport adapter。
- `UidlRequestHandler`、`ServerRpcHandler`、`UidlWriter`、`VaadinSession`、
  UI lock/access、CSRF/security key、sync id 和 resync 语义被封装在稳定边界内。
- 不直接修改本地 upstream Vaadin 源码；如需 upstream SPI，先以仓库内 adapter/fork
  证明并记录。
- 错误路径、session lifecycle、multi-window/multi-UI 行为有明确 trace 和测试。

### 3. Frontend Artifact Pipeline 可产品化

- Flow client resources、component frontend modules、theme/static、generated/lazy、
  production bundle 和 dev-mode/Vite resource graph 都有统一 resolver。
- TextField/npm graph 不再依赖旧 shim；真实 TextField、Checkbox、Select 等组件通过
  真实 Vaadin frontend artifact 执行。
- Generated/lazy `Flow.loadOnDemand` path 可以稳定工作，或有可接受的替代 SPI。
- Production bundle / dev-mode bundle 有明确 staging、cache、cleanup 和 integrity 策略。
- WebView resource loading 使用 app bundle、本地 staging、custom scheme 或等价
  no-localhost mechanism。

### 4. 组件和 Flow 功能覆盖足够真实

- 不只覆盖 Button 和少数组件；至少覆盖真实业务应用常见路径：
  TextField、Number/BigDecimalField、Checkbox、Select、ComboBox、DatePicker、
  Dialog、FormLayout、Grid/VirtualList、Upload/Download、theme variants。
- 事件必须经 Flow client engine -> IPC-backed bridge -> Java listener ->
  UIDL patch -> visible DOM 的真实链路。
- Repeated data、selection、validation、enabled/disabled、attach/detach、locale/format、
  focus/keyboard 和 error state 有回归验证。

### 5. Native Host 具备桌面框架能力

- macOS Swift/AppKit host 是稳定主线，并有可重复 smoke tests。
- Windows WebView2 和 Linux WebKitGTK host 至少达到可运行的同级架构切片。
- Host 负责 native 行为：窗口、菜单、托盘、clipboard、file dialog、notification、
  drag/drop、shortcuts、IME/focus、context menu、lifecycle、crash cleanup。
- Window activation、window level、first meaningful paint、blank/flicker recovery、
  close/shutdown 和 sidecar cleanup 有自动化验证。

### 6. Capability Gateway 有权限模型

- Java desktop API 稳定且面向开发者，例如 window、clipboard、notification、
  file dialog、file transfer、shell/open URL、menu/tray。
- IPC `command` / `capabilityResult` typed、async、可追踪，并带 correlation id。
- `desktop-capabilities.json` 或后继 manifest 支持 allow、deny、trace-only、dev override
  和 production policy。
- 文件能力支持受控路径、真实 native panel、security-scoped metadata、redacted trace、
  byte count 和错误分类。

### 7. CLI、打包和开发体验完整

- CLI 能创建、运行、调试、打包和检查桌面应用。
- Maven/Gradle 插件或等价工具能处理 framework dependencies、frontend bundle staging、
  native host assets、capability manifest 和 release metadata。
- 开发者项目像普通外部项目一样依赖 framework modules，不需要复制 test-app 特殊代码。
- Trace inspector、doctor、resource graph inspector 和 failure diagnostics 足够让工程师
  快速拆 issue。

### 8. 分发和更新路径可落地

- macOS 至少支持 app bundle、签名/公证规划、DMG/PKG 或等价分发。
- Windows/Linux 有清楚打包边界和后续实现计划。
- Java sidecar、native host、frontend artifacts、capability manifest 和 updater metadata
  有一致的发布结构。
- 自动更新可以先是设计和 skeleton，但最终需要可验证的安全更新路径。

### 9. 文档和验证体系完整

- README、getting started、architecture、native host、Flow adapter、IPC protocol、
  runtime limitation、roadmap、troubleshooting 都保持同步。
- 每个阶段都有 fixture trace、strict inspector gate、真实窗口验证命令和 commit。
- 文档明确已完成能力、compatibility fallback、partial bridge、diagnostic shim、
  strict blocker 和下一步拆解。
- 不把实验路径包装成 final，不把 localhost compatibility mode 包装成 Flow over IPC。

## 长期未完成账本

以下项目仍需要后续阶段推进：

- 基于 v1.12 的真实 generated-flow-imports no-op table 证据，继续让当前 app/build 产生真实
  generated lazy chunk file，接入受支持 Vaadin lazy chunk SPI，或把 production bundle owned
  execution 和 dev-mode/Vite execution 从 partial/blocker 推进到真实可执行路径。
- 完整 generated/lazy `Flow.loadOnDemand` execution，不再停在 staged generated-flow-imports
  no-op / missing chunk file blocker。
- Production bundle owner import/side-effect success、chunk side-effect ownership、
  packaging-time staging，以及把 v1.18 custom-scheme owner graph 从可审计 import attempt
  推进到可复用 owner-owned runtime path。
- Dev-mode/Vite graph execution 或可靠替代模式。
- Broader Vaadin component parity，尤其是 Grid/VirtualList、ComboBox、DatePicker、
  Dialog、Upload。
- Durable push queue、ack、reconnect、reload 和 resync 语义。
- Multi-window / multi-UI / multi-session lifecycle。
- Host-side smoke tests：window launch、activation、sidecar cleanup、capability calls。
- macOS host 默认化、Windows/Linux host 切片、打包和更新。
- Production permission model 和 security-scoped file access。
- Upstream Vaadin SPI 评估。

## 后续阶段制定规则

每个新的 `goal-1_x.md` 应满足：

- 从本文件的未完成账本中选择一个可评审切片。
- 明确继承哪些已完成阶段能力，且不得回退。
- 明确不使用 localhost compatibility mode 作为 Flow IPC 成功证据。
- 明确验收命令、fixture trace、真实窗口验证和文档更新范围。
- 如果选择 blocker 化路径，必须给出 source artifact、runtime layer、browser/native error、
  failure classification 和下一步拆解。
- 每轮工作按适合粒度提交 Conventional Commit。

## 当前建议的下一阶段

`docs/goal-1_19.md` 应该基于 v1.18 的 production owner custom-scheme graph 证据继续推进，
不要重复证明 `vatori-resource` handler 是否可用。合理方向是让 owner root import 之后的
runtime side effect / component reuse 进入可审计闭环：preferred 是 production owner 通过
custom-scheme URL 成为可复用 owner-owned runtime path，focused TextField / Checkbox / Select
resolver 能明确复用 owner side effects；acceptable 是 import 已经由 custom-scheme graph 负责，
但 blocker 被进一步收敛到具体 downstream Vaadin runtime side-effect、custom element registry、
global initialization 或 unsupported production owner contract 层，并给出下一步工程切口。

后续阶段可继续从以下方向选择：

- 让当前 smoke/framework build 产生真实 generated lazy chunk file，或评估受支持 Vaadin lazy
  chunk SPI，越过 v1.12 的 `chunk-file-not-generated` blocker。
- Dev-mode/Vite graph execution 或可靠替代模式。
- 统一 component/generated/theme/production owner resource graph 的 resolver、cache、cleanup
  和 integrity 策略。
- Broader Vaadin component parity，尤其是 Grid/VirtualList、ComboBox、DatePicker、Dialog、
  Upload。
- Swift host tests / default-host readiness。
- Broader component parity、durable push / reconnect / resync、打包和更新。
