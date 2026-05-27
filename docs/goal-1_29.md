# Goal 1.29: Generated Lazy App Build Output Connection

状态：待执行

`docs/goal.md` 是长期愿景和完成度账本。v1.28 已完成
`flow-ipc-generated-lazy-build-lifecycle-v1_28`：trace 现在能证明真实
`@Route` + `@JavaScript(loadMode = LoadMode.LAZY)` 输入已经被 scanner 观察到，但
`dev-ipc` runtime 仍从 packaged Vaadin dev-bundle jar 读取
`generated-flow-imports-VW7hRaNw.js`。当前 blocker 已收敛为
`runtime-using-stale-packaged-dev-bundle`。

v1.29 的目标是继续沿着这个 blocker 往产品路径推进：让 no-localhost `dev-ipc`
frontend resolver 不再只依赖静态 packaged dev-bundle generated imports，而是连接当前
app 的 frontend build output，或明确接入/验证一个受支持 Vaadin lazy chunk SPI。如果当前
Vaadin 版本仍不会为该 probe 生成 chunk file，也必须把失败层收敛到当前 app build output /
SPI 层，而不是停留在 packaged dev-bundle artifact 选择层。

## 核心目标

新增 `flow-ipc-generated-lazy-app-build-output-v1_29` contract，覆盖：

- build output discovery：定位当前 smoke/framework app 的 `prepare-frontend`、
  `update-imports`、generated imports、frontend generated file 或等价 Vaadin build output
  目录。
- runtime source switch：`dev-ipc` generated/lazy resource resolver 优先使用当前 app build
  output；只有在明确缺失且被 trace 分类时，才 fallback 到 packaged dev-bundle。
- SPI assessment：如果 Vaadin 当前 build 输出仍没有 generated lazy chunk file，trace 必须说明
  是否存在可用 public/internal SPI、Maven plugin step、`FrontendDependencies` / route registry
  handoff 或需要 packaging-time staging 介入。
- parity decision：把 v1.22 positive fixture 的 executor path 与当前 app build output 对齐；
  如果真实 chunk 出现，必须沿用 v1.22 import/callback/side-effect proof；如果仍未出现，
  failure layer 必须比 v1.28 更窄。

Acceptable outcomes:

- Preferred：当前 app build output 中出现真实 `generated-flow-imports` chunk table 和
  `./chunks/chunk-*.js` artifact，WebView 通过 IPC resolver / custom-scheme staging 执行该
 真实 chunk，并记录 callback + side-effect proof。
- Acceptable：当前 app build output 被 runtime 使用，但 Vaadin 仍没有为 probe 生成真实 chunk；
  blocker 收敛为 `app-build-output-has-no-generated-lazy-chunk`、
  `vaadin-lazy-chunk-spi-not-invoked` 或同等精确层。
- Acceptable：发现当前路径必须走 Maven/packaging-time frontend build step 或 Vaadin internal
  SPI，但仓库 runtime 还未接入；trace 必须记录具体 source artifact、target path、SPI/build step
  和下一步，不可回退成 generic blocker。

## 不做什么

- 不把 v1.22 repository-local fixture 当成当前 app/build 输出。
- 不把 v1.28 的 packaged dev-bundle jar artifact 继续算作当前 app build output。
- 不把普通 `LoadMode.LAZY` runtime script 执行当成 generated
  `Flow.loadOnDemand(chunkId)` chunk-file parity。
- 不扩大到 production owner side-effect success、dev-mode/Vite 完整执行、Grid/Upload 或 durable
  push。
- 不使用 localhost compatibility mode 作为 Flow IPC 成功证据。

## 实现要求

### Java / Vaadin build output resolver

- 在 `vaadin-desktop-flow` 中新增当前 app build output discovery helper，优先检查：
  - test app target/generated frontend output；
  - Vaadin generated imports/write target；
  - Maven/prepare-frontend/update-imports output；
  - classpath packaged dev-bundle fallback。
- 对每个候选记录 redacted path、exists、protocol、mtime、byte count、sha256、table shape、
  chunk references 和 source classification。
- `FlowClientResources` 的 generated/lazy resource metadata 必须区分：
  - `current-app-build-output`
  - `packaged-dev-bundle-fallback`
  - `missing-build-output`
  - `spi-required`
- 如果最终使用 fallback，必须记录 fallback reason 和 next resolution step。

建议 metadata：

- `generatedLazyAppBuildOutputVersion=flow-ipc-generated-lazy-app-build-output-v1_29`
- `generatedLazyAppBuildOutputDiscoverySource=<source>`
- `generatedLazyAppBuildOutputRuntimeSource=<current-app-build-output|packaged-dev-bundle-fallback|missing-build-output>`
- `generatedLazyAppBuildOutputGeneratedImportsPath=<redacted path>`
- `generatedLazyAppBuildOutputGeneratedImportsObserved=<true|false>`
- `generatedLazyAppBuildOutputGeneratedImportsSha256=<sha256>`
- `generatedLazyAppBuildOutputGeneratedImportsTableShape=<no-op-promise-resolve-0|branch-table-dynamic-import|missing>`
- `generatedLazyAppBuildOutputChunkReferences=<count/list>`
- `generatedLazyAppBuildOutputChunkStatus=<available|not-generated|missing-build-output|blocked>`
- `generatedLazyAppBuildOutputFallbackReason=<reason>`
- `generatedLazyAppBuildOutputSpiAssessment=<available|not-found|internal-only|requires-build-step>`
- `generatedLazyAppBuildOutputFailureLayer=<classification>`
- `generatedLazyAppBuildOutputNextResolutionStep=<next step>`

### Swift host / WebView diagnostics

- Swift host / WebView 要把 v1.29 metadata 传播到 `resourceRequest`、`resourceResponse`、
  `resourceStage` 和 `webview.dom`。
- DOM summary 必须展示 runtime source 是否已经离开 packaged dev-bundle fallback。
- 如果真实 chunk 可用，必须通过同一 no-localhost resolver/staging path 执行，并记录
  callback / side-effect proof。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_29-generated-lazy-app-build-output docs/traces/flow-ipc-v1_29.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_29 docs/traces/flow-ipc-v1_29.ndjson
```

`--require-flow-ipc-v1_29-generated-lazy-app-build-output` 只验证 v1.29 delta；
`--require-flow-ipc-v1_29` 继承 v1.28 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_29-generated-lazy-app-build-output .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_29 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_29.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_29-generated-lazy-app-build-output docs/traces/flow-ipc-v1_29.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_29 docs/traces/flow-ipc-v1_29.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_28 docs/traces/flow-ipc-v1_28.ndjson
```

## 完成后更新

- `docs/goal.md`
- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/acceptance-tools.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/flow-transport-adapter.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/ipc-protocol-v1.md`
- `docs/native-host.md`
- `docs/roadmap.md`
- `test-apps/flow-over-ipc-smoke-app/README.md`

完成提交必须使用 Conventional Commit，例如：

```text
feat: connect generated lazy resolver to app build output
```
