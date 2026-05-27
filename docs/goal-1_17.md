# Goal 1.17: Host-Owned Frontend Resource Scheme

状态：已规划，尚未执行。

`docs/goal.md` 是长期愿景和完成度账本。v1.15 已规划为 production owner dependency
resolver closure，v1.16 已规划为 production owner import ordering / component reuse。v1.17
只应在 v1.15 和 v1.16 完成后执行。

v1.5-v1.16 的 frontend execution path 主要依赖 WebView 内部的 Blob module URL staging：
Swift host 通过 IPC 拿到 Java sidecar 返回的真实 Vaadin artifact body，然后在 WebView runtime
中把 body stage 成 Blob URL，再通过 import rewrite、stable resource-path identity 和 cache
诊断让模块执行。这条路径已经足够证明 no-localhost resource delivery、真实 component frontend
execution、generated/lazy staging、production owner import attempt 和 owner/component ordering
边界，但它还不是产品化资源加载形态。

Blob staging 的核心限制是：URL lifetime 和 module identity 由 WebView JS 管，relative import
必须改写，source URL / stack / CSP / cleanup / cache 语义都偏诊断形态。长期目标要求 WebView
resource loading 使用 app bundle、本地 staging、custom scheme 或等价 no-localhost mechanism。
因此 v1.17 的目标是把当前 Blob-first resource staging 推进到 host-owned frontend resource
scheme / file-backed loader 边界。

**核心目标：在 macOS Swift/AppKit/WKWebView `dev-ipc` runtime 中引入 host-owned no-localhost
frontend resource URL layer。Preferred 是通过 `WKURLSchemeHandler` 或等价 file-backed local
loader，让 Flow client resources、component frontend resources、generated/lazy artifacts 和
production owner artifact 可以使用稳定 scheme URL 执行，并保留真实事件链路。Acceptable 是先让
一条真实执行路径从 Blob URL 迁移到 host-owned scheme URL，production owner 或部分复杂 graph
仍可保留显式 Blob fallback，但 trace 必须证明 scheme handler、resource request/response、
cache、MIME、relative import base 和 fallback classification 都真实存在。**

## 为什么选择这个目标

v1.15 和 v1.16 继续拆 production owner：先让 owner dependency graph 闭合，再尝试 owner-first
import 和 component reuse。完成这些之后，如果仍然只依赖 Blob URL，就会继续遇到几个不适合产品化
runtime 的问题：

- relative import 必须在 JS source 字符串里改写，容易把 Vaadin 真实 artifact graph 变成
  resolver 特例集合；
- Blob URL 的 module identity、lifetime、cleanup 和 source diagnostics 不像 app-owned
  resource；
- production owner 和 focused component resolver 很难共享接近浏览器原生的同源 resource base；
- 后续 packaging-time staging、CSP/integrity、dev-mode/Vite 或 app bundle resource layout
  都缺少一个 host-owned URL 边界。

v1.17 不急着扩大 Grid/ComboBox/Dialog/Upload 覆盖。先把 resource loading 从“WebView JS 里造
Blob URL”推进到“host 拥有 URL scheme / file-backed loader”，可以让后续 production owner、
generated lazy chunk、dev-mode/Vite 和 packaging 都落在更稳的地基上。

## 继承边界

以下能力在 v1.17 中视为基线，不能回退：

- `dev-ipc` 继续启动 no-localhost Flow IPC runtime，核心 UI path 不通过 localhost HTTP。
- 可见 Flow client engine 继续消费 bootstrap、event patch、ordered push、resync、
  ordinary `LoadMode.LAZY` dependency 和 generated `Flow.loadOnDemand(chunkId)` trigger。
- v1.7 TextField graph execution 继续通过，旧 TextField shim 只能作为 legacy fallback。
- v1.8 generated/lazy relative dependency resolver 继续通过。
- v1.10 ordinary `LoadMode.LAZY` dependency path 继续通过。
- v1.11 real UIDL `Flow.loadOnDemand(chunkId)` trigger 继续通过。
- v1.12 generated-flow-imports no-op table parser 继续通过，并保留
  `chunkFileStatus=not-generated` 的真实证据。
- v1.13 production owner manifest / request / response / stage / DOM evidence 继续通过。
- v1.14 production owner import attempt 继续通过；不能回退到
  `production-owner-runtime-contract-missing`。
- v1.15 owner dependency resolver closure 继续通过；不能用未分类的
  `production-owner-relative-import-unresolved` 作为 v1.17 通过条件。
- v1.16 owner import ordering / component reuse decision 继续通过；不能只复用
  after-component diagnostic probe 作为 v1.17 证据。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、
  repeated/list update、desktop capability、file transfer、push ordering、resync、
  WebView error reporting 和 framework app validation 不退化。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者项目形态的 validation app。

## 非目标

- 不把 localhost HTTP、`file://` 随机临时目录直读或 compatibility host 作为 Flow IPC resource
  scheme 成功证据。
- 不要求本阶段完成完整 production bundle parity。
- 不要求本阶段完成 dev-mode/Vite execution。
- 不要求本阶段解决 generated-flow-imports no-op table / missing generated chunk file。
- 不要求 Windows WebView2 / Linux WebKitGTK 同步实现，但设计和 metadata 不能把 macOS 细节泄露
  成 Java API contract。
- 不要求本阶段替换所有 Blob staging；如果保留 fallback，必须有明确 fallback reason 和 trace。
- 不为了启用 scheme URL 牺牲现有 component event chain。

## 实现范围

### 1. Host-Owned Scheme / File Loader Boundary

目标：

- 在 `vaadin-desktop-host-swift` 中为 `dev-ipc` 增加 host-owned frontend resource URL layer：
  - Preferred：`WKURLSchemeHandler`，例如 `vatori-resource://flow-ipc/...` 或等价明确命名；
  - Acceptable：host-owned file-backed staging loader，但不能依赖 localhost HTTP；
  - scheme / file path lifetime 由 Swift host 管理，而不是由 WebView JS 临时 Blob URL 独占。
- 该 layer 必须记录：
  - `frontendResourceSchemeVersion=flow-ipc-resource-scheme-v1_17`
  - scheme name 或 file-backed mode；
  - handler installed / request observed / response observed；
  - source artifact path；
  - MIME type；
  - byte count / sha256；
  - cache hit/miss；
  - relative import base；
  - cleanup policy；
  - no-localhost proof。

验收要求：

- 真实窗口 trace 中出现 host-owned scheme / file loader 的 request 和 response evidence。
- Trace 必须能区分 `stagingMode=custom-scheme|file-backed|blob-fallback`。
- 如果使用 file-backed loader，必须记录写入目录、cleanup policy、path redaction 和 no-localhost
  proof。

### 2. Scheme URL Execution Slice

目标：

- 至少迁移一条真实执行路径到 host-owned scheme URL：
  - Preferred：FlowBootstrap / FlowClient + BigDecimalField + TextField/Checkbox/Select focused
    entries + generated/lazy artifact + production owner root artifact；
  - Acceptable：FlowBootstrap / FlowClient 和至少一个真实 Vaadin component frontend artifact。
- Scheme URL import 必须仍然经过 IPC resource manifest / request / response：
  - Java sidecar 仍是 artifact body source；
  - Swift host 可以缓存 body 或落盘 staging；
  - WebView 通过 scheme URL import / script / stylesheet load；
  - resource metadata 必须回显到 `resourceStage` 或新的等价 `resourceLoad` trace。
- 对 theme/static CSS、Flow `LoadMode.LAZY` script、generated/lazy module 和 production owner
  module 分别声明是否已迁移到 scheme URL。

验收要求：

- 通过 scheme URL 执行的 artifact 必须记录 `executionStatus=executed`，不能只记录 verified-only。
- 现有 Blob path 可以作为 fallback，但 fallback 必须记录：
  - fallback artifact；
  - fallback reason；
  - blocked migration layer；
  - next resolution step。
- 真实 Flow event -> IPC-backed bridge -> Java listener -> UIDL patch -> visible DOM 链路继续通过。

### 3. Relative Import and Module Identity Semantics

目标：

- 对 scheme URL path 记录接近浏览器原生的 relative import base：
  - `resourceBaseUrl`
  - `relativeImportResolutionMode`
  - `resolvedRelativeImportPaths`
  - `missingRelativeImportPaths`
  - `moduleIdentityStrategy`
- 如果 scheme URL 能让 production owner graph 不再需要 source rewrite，应记录：
  - `productionBundleRuntimeOwnerResourceLoaderMode=custom-scheme`
  - `productionBundleRuntimeOwnerImportDependencyRewriteResult=not-needed-custom-scheme`
- 如果仍需要 rewrite，必须明确：
  - 为什么 scheme URL 还不足够；
  - rewrite 发生在哪一层；
  - 是否仍受 Blob fallback 影响。

验收要求：

- v1.17 trace 不能只复用 v1.15 dependency closure 或 v1.16 ordering evidence。
- Trace 必须说明 scheme URL 是否改变了 production owner dependency / ordering blocker。

### 4. Java Metadata and Smoke Assertions

目标：

- 在 Java manifest / resource response 中声明 v1.17 resource loader contract：
  - `frontendResourceLoaderVersion=flow-ipc-resource-scheme-v1_17`
  - `frontendResourceLoaderModes=custom-scheme,file-backed,blob-fallback`
  - `frontendResourceLoaderPreferredMode`
  - `frontendResourceLoaderNoLocalhost=true`
  - `frontendResourceLoaderAcceptableFallbacks`
- `FlowClientResourceSmokeMain` 或 focused smoke 断言这些 metadata 存在。
- 保留 v1.13 / v1.14 / v1.15 / v1.16 metadata，不能破坏历史 gates。

验收要求：

- `bash bin/flow-client-resource-smoke` 验证 v1.17 metadata。
- v1.16 fixture gate 继续通过。

### 5. Inspector Gate Semantics

新增两类 gate：

- `--require-flow-ipc-v1_17-resource-scheme`：只验证 v1.17 host-owned frontend resource loader
  delta。
- `--require-flow-ipc-v1_17`：累计阶段完成 gate，表示 v1.16 baseline + v1.17 delta。

规则：

- v1.17 delta 必须证明 host-owned scheme / file loader 真实处理过 frontend artifact request。
- v1.17 delta 必须证明至少一个真实 executed artifact 通过 scheme URL 或 file-backed URL 执行。
- v1.17 delta 不能只复用 Blob `resourceStage` 证据。
- 如果保留 Blob fallback，必须记录 fallback reason，并且 cumulative gate 仍需看到 scheme/file
  executed artifact。
- `production-owner-runtime-contract-missing`、未分类的
  `production-owner-relative-import-unresolved`、单纯 after-component owner probe 不能作为 v1.17
  通过条件。

交付：

- 新增 `docs/traces/flow-ipc-v1_17.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.17 delta/cumulative gate。
- 保持 v1-v1.16 fixture gates 继续可运行。
- 保持 `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。

### 6. Documentation

更新或新增：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/goal.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md`
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- `docs/acceptance-tools.md`
- `test-apps/flow-over-ipc-smoke-app/README.md`
- 如 framework app 行为变化，也更新对应 README。

文档必须明确：

- v1.17 是 host-owned frontend resource loader boundary，不是 broad production parity。
- Blob staging 仍可作为 explicit fallback，但不能继续被描述为最终 resource loading 形态。
- custom scheme / file-backed loader、production owner import、component focused import、
  generated/lazy 和 ordinary Flow lazy dependency 是不同证据链。
- Compatibility mode 仍是 fallback，不能作为 Flow IPC 成功证据。

## 验收命令

最小验收集合：

```bash
git diff --check
bash -n bin/vaadin-desktop
source /Users/bytedance/.sdkman/bin/sdkman-init.sh && mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
bash bin/flow-client-resource-smoke
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
```

Trace gate matrix：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_6 docs/traces/flow-ipc-v1_6.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_7 docs/traces/flow-ipc-v1_7.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_8 docs/traces/flow-ipc-v1_8.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_9-loadondemand docs/traces/flow-ipc-v1_9.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_9 docs/traces/flow-ipc-v1_9.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10-lazy-dependency docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10-production-runtime docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10 docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_11-loadondemand-chunk docs/traces/flow-ipc-v1_11.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_11 docs/traces/flow-ipc-v1_11.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_12-loadondemand-chunk-file docs/traces/flow-ipc-v1_12.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_12 docs/traces/flow-ipc-v1_12.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_13-production-owner docs/traces/flow-ipc-v1_13.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_13 docs/traces/flow-ipc-v1_13.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14-production-owner-import docs/traces/flow-ipc-v1_14.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14 docs/traces/flow-ipc-v1_14.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15-production-owner-dependencies docs/traces/flow-ipc-v1_15.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15 docs/traces/flow-ipc-v1_15.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16-production-owner-ordering docs/traces/flow-ipc-v1_16.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16 docs/traces/flow-ipc-v1_16.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_17-resource-scheme docs/traces/flow-ipc-v1_17.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_17 docs/traces/flow-ipc-v1_17.ndjson
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_17 .build/vaadin-desktop-host-swift/host.out.log

VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```

## 完成判定

v1.17 可以用两种方式完成：

1. Preferred：Flow client resources、当前真实 component frontend entries、generated/lazy artifact
   和 production owner root/dependency graph 都通过 host-owned scheme URL 或 file-backed URL 执行；
   Blob staging 只保留为兼容 fallback；现有真实事件链路和 framework app gate 不退化。
2. Acceptable：至少一条真实 executed artifact path 从 Blob 迁移到 host-owned scheme/file loader，
   production owner 或部分复杂 graph 仍保留 Blob fallback，但 trace 明确记录 fallback reason、
   blocked layer 和下一步拆解，且 v1.17 gate 能证明 scheme/file loader 不是空壳。

无论采用哪种方式，都必须新增 v1.17 trace fixture、strict inspector gate、文档更新和真实窗口
验证结果。
