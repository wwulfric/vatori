# Goal 1.33: Current App Generated Chunk Package Edge

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.32 已完成
`flow-ipc-current-app-build-frontend-output-v1_32`：显式 full `build-frontend` 已能产生当前
app 的 source generated imports/chunk、production webapp build output 和 `prod.bundle`，并把
source generated imports/chunk 暴露为 Flow IPC no-localhost runtime candidate。当前 accepted
blocker 是 `current-app-source-generated-chunk-npm-specifier-unresolved`。

v1.33 的目标是把这个 blocker 从“当前 app chunk body 内仍有 bare npm specifier”推进到
“当前 app generated chunk 的第一个真实 package edge 可通过 no-localhost resolver stage/import
执行”。本阶段聚焦 source generated chunk 中实际出现的
`@vaadin/vertical-layout/src/vaadin-vertical-layout.js`，将它解析到已验证的 Vaadin production
indexhtml module identity，并让 `Flow.loadOnDemand(chunkId)` callback 能导入当前 app chunk。

## 核心目标

新增 `flow-ipc-current-app-generated-chunk-package-resolver-v1_33` contract，覆盖：

- Chunk package edge：识别当前 app generated chunk body 中的 bare import specifier，并记录
  resolved/unresolved specifier 集合。
- No-localhost rewrite：为当前 app source generated imports/chunk 增加 manifest import rewrite，
  支持 static side-effect import 和 `import('./chunks/chunk-*.js')` dynamic import。
- Chunk execution：WebView 通过 IPC 请求当前 app generated imports/chunk，stage 为 Blob module，
  把 `@vaadin/vertical-layout/src/vaadin-vertical-layout.js` 重写到稳定 production indexhtml module
  identity，并记录 chunk callback resolved / chunk file executed 证据。
- Remaining blocker：如果 source generated imports root 仍需要 placeholder 或仍有未闭合 package
  graph，trace 必须明确 remaining blocker，不能把 v1.33 描述成完整 frontend/resource pipeline
  闭环。

## Acceptable outcomes

- Preferred：当前 app source generated imports 被 stage/import，`Flow.loadOnDemand(chunkId)` delegate
  触发当前 app chunk dynamic import，chunk 内 bare specifier 被 no-localhost resolver 重写并执行；
  trace 记录 `loadOnDemandChunkFileExecuted=true` 和 v1.33 resolver metadata。
- Acceptable：当前 app chunk 直接执行成功，但 source generated imports root 仍停在明确的 package
  graph / placeholder / Flow-client trigger 层；trace 必须记录 source artifact、resolved specifier、
  unresolved specifier、browser error 和下一步。
- Acceptable：chunk package edge 解析失败；trace 必须把 blocker 收敛到具体 specifier 和 resource
  path，而不能回退为 v1.32 的 generic
  `current-app-source-generated-chunk-npm-specifier-unresolved`。

## 不做什么

- 不声明完整 frontend/resource pipeline 基本闭环，除非当前 app generated imports/chunk 真正由
  Flow client path 触发并且不依赖 diagnostic placeholder。
- 不把 repository fixture 或 packaged dev-bundle generated-flow-imports 当作当前 app source chunk
  parity。
- 不引入 localhost/Vite dev server 作为成功证据。
- 不把 compatibility placeholder 伪装成真实 Vaadin package graph resolution。

## 实现要求

### Java resolver / metadata

- `FlowClientResources` 增加 v1.33 metadata：
  - `currentAppGeneratedChunkPackageResolverVersion=flow-ipc-current-app-generated-chunk-package-resolver-v1_33`
  - `currentAppGeneratedChunkPackageResolverSource=current-app-source-generated-chunk`
  - `currentAppGeneratedChunkPackageResolvedSpecifiers=<csv>`
  - `currentAppGeneratedChunkPackageUnresolvedSpecifiers=<csv>`
  - `currentAppGeneratedChunkPackageResolverMode=<classification>`
  - `currentAppGeneratedChunkPackageFailureLayer=<classification>`
- 当前 app generated imports/chunk resource 必须声明 import rewrites 和 transitive resource paths。
- v1.33 成功时，v1.32 的 blocker 应被替换为更窄的 v1.33 remaining blocker。

### Swift host / WebView diagnostics

- `rewriteStaticImportSpecifier` 必须能处理 static import 和 literal dynamic import。
- `componentModuleBlobUrl` 必须能对 generated-lazy/current-app resource 应用 manifest import
  rewrites，而不只对 primary component resource 应用。
- `resourceStage` / `webview.dom` 必须记录 v1.33 resolver metadata、chunk callback result、
  chunk file executed state 和 remaining blocker。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_33-current-app-generated-chunk-package-edge docs/traces/flow-ipc-v1_33.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_33 docs/traces/flow-ipc-v1_33.ndjson
```

`--require-flow-ipc-v1_33-current-app-generated-chunk-package-edge` 只验证 v1.33 delta；
`--require-flow-ipc-v1_33` 继承 v1.32 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_33-current-app-generated-chunk-package-edge .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_33 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_33.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_33-current-app-generated-chunk-package-edge docs/traces/flow-ipc-v1_33.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_33 docs/traces/flow-ipc-v1_33.ndjson
```

## 完成后更新

- `docs/goal.md`
- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/acceptance-tools.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/roadmap.md`
- `test-apps/flow-over-ipc-smoke-app/README.md`

完成提交必须使用 Conventional Commit，例如：

```text
feat: resolve current app generated chunk package edge
```

## 完成记录

- Java `FlowClientResources` 增加
  `flow-ipc-current-app-generated-chunk-package-resolver-v1_33` metadata，并为当前 app
  source generated imports/chunk 声明 import rewrites、transitive resources、resolved /
  unresolved specifier 集合、remaining blocker 和下一步。
- Swift host 对 generated/current-app resource 应用 manifest import rewrites，覆盖 static
  side-effect import 和 literal dynamic import；当前 app chunk 中的
  `@vaadin/vertical-layout/src/vaadin-vertical-layout.js` 被重写到稳定 production
  `indexhtml` module identity，并记录 module-init retry / cache invalidation 证据。
- Trace inspector 增加
  `--require-flow-ipc-v1_33-current-app-generated-chunk-package-edge` 和
  `--require-flow-ipc-v1_33` gates。
- 固定 trace 已保存为 `docs/traces/flow-ipc-v1_33.ndjson`，fixture gate 证明 manifest、
  request、response、stage、DOM 和 `loadOnDemandChunkFileExecuted=true` 证据齐全。

v1.33 不是完整 frontend/resource pipeline 基本闭环。它只证明当前 app generated chunk
的第一个真实 package edge 已能经 no-localhost resolver stage/import 执行；当前 app
generated imports root 仍使用 diagnostic placeholder，完整 package graph、production
owner side-effect / reuse 和 dev-mode/Vite parity 仍是后续 blocker。
