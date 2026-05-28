# Goal 1.34: Current App Generated Imports Root Package Graph

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.33 已证明当前 app source generated chunk
内的第一个真实 package edge 可以经 no-localhost resolver stage/import 执行：
`@vaadin/vertical-layout/src/vaadin-vertical-layout.js` 被重写到稳定 production module identity，
并且 `Flow.loadOnDemand(chunkId)` callback 触发的 current app chunk 可以执行。剩余 blocker 是
source generated imports root 仍用 diagnostic placeholder 处理
`@vaadin/common-frontend/ConnectionIndicator.js`，因此还不能称为完整 current app generated imports
package graph parity。

v1.34 的目标是把这个 blocker 推进到“source generated imports root 的 package imports 由当前 app
真实 production webapp build artifact graph 支持”。本阶段移除 root placeholder，解析 source root
imports 与 `stats.json` `bundleImports`，把 root side-effect package imports 重写到当前 app production
`generated-flow-imports-*.js` 支持 artifact，并 stage 该 artifact 的 relative dependency graph。

## 核心目标

新增 `flow-ipc-current-app-generated-imports-root-package-graph-v1_34` contract，覆盖：

- Root package graph：解析当前 app source `generated-flow-imports.js` root 中的 bare import
  specifiers，并记录 root bare/resolved/unresolved specifier 集合。
- Stats / production support：用当前 app `target/classes/META-INF/VAADIN/config/stats.json`
  的 `bundleImports` 和 production webapp build output 证明 root imports 有真实 build artifact
  支持。
- No-placeholder execution：`@vaadin/common-frontend/ConnectionIndicator.js` 不再重写到
  diagnostic placeholder；root imports 由当前 app production `generated-flow-imports-*.js`
  support artifact 承载。
- Relative dependency graph：当前 app production `generated-flow-imports-*.js`、`indexhtml-*.js`、
  `commonjsHelpers-*.js`、`markdown-helpers-*.js`、FlowBootstrap/FlowClient 等 production build
  files 作为 no-localhost resources stage，并为 relative imports 建立 manifest rewrites。
- Remaining blocker：即便 root package graph 执行成功，也不能把 v1.34 描述成完整
  frontend/resource pipeline 闭环；production owner side-effect / reuse、dev-mode/Vite execution
  和 broader component parity 仍是后续 blocker。

## Acceptable outcomes

- Preferred：source generated imports root 和 current app chunk 都通过 Flow-client-triggered
  `Flow.loadOnDemand(chunkId)` 路径执行；root bare imports 全部解析到 current app production
  support graph；trace 记录 placeholder=false、root unresolved specifiers 为空、support artifact
  staged/executed、chunk file executed=true。
- Acceptable：source root package graph 仍有 unresolved specifier，但 trace 必须明确 specifier、
  stats coverage、production support artifact、browser/native error 和下一步，不能回退为 v1.33 的
  generic root placeholder blocker。

## 不做什么

- 不声明完整 frontend/resource pipeline 基本闭环，除非 production owner runtime、generated root/chunk、
  dev-mode 或替代 SPI 都有真实闭合证据。
- 不把 diagnostic placeholder 或空 module 当作 `@vaadin/common-frontend/ConnectionIndicator.js`
  成功。
- 不引入 localhost/Vite dev server 作为成功证据。
- 不把 current app production generated imports support artifact 误称为 production owner-owned
  runtime success。

## 实现要求

### Java resolver / metadata

- `FlowClientResources` 增加 v1.34 metadata：
  - `currentAppGeneratedImportsRootPackageGraphVersion=flow-ipc-current-app-generated-imports-root-package-graph-v1_34`
  - `currentAppGeneratedImportsRootPackageGraphSource=current-app-source-generated-imports-root`
  - `currentAppGeneratedImportsRootPackageResolverMode=<classification>`
  - `currentAppGeneratedImportsRootBareSpecifiers=<csv>`
  - `currentAppGeneratedImportsRootStatsBundleImports=<csv>`
  - `currentAppGeneratedImportsRootResolvedSpecifiers=<csv>`
  - `currentAppGeneratedImportsRootUnresolvedSpecifiers=<csv>`
  - `currentAppGeneratedImportsRootProductionSupportArtifact=<virtual path>`
  - `currentAppGeneratedImportsRootProductionSupportObserved=<boolean>`
  - `currentAppGeneratedImportsRootPlaceholderUsed=false`
  - `currentAppGeneratedImportsRootFailureLayer=<classification>`
- 将当前 app production webapp build `.js` artifacts 暴露为 component frontend resources，并为其
  relative imports 声明 manifest rewrites。
- source generated imports root 的 `importRewrites` 不再指向
  `META-INF/vatori/current-app-frontend-entry/connection-indicator-placeholder.js`。

### Swift host / WebView diagnostics

- `componentModuleBlobUrl` / manifest rewrite path 必须能 stage current app production support
  artifact 及其 relative dependencies。
- `resourceStage` / `webview.dom` 必须记录 v1.34 root package graph metadata、placeholder=false、
  support artifact executed state 和 remaining blocker。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_34-current-app-generated-imports-root-package-graph docs/traces/flow-ipc-v1_34.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_34 docs/traces/flow-ipc-v1_34.ndjson
```

`--require-flow-ipc-v1_34-current-app-generated-imports-root-package-graph` 只验证 v1.34 delta；
`--require-flow-ipc-v1_34` 继承 v1.33 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_34-current-app-generated-imports-root-package-graph .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_34 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_34.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_34-current-app-generated-imports-root-package-graph docs/traces/flow-ipc-v1_34.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_34 docs/traces/flow-ipc-v1_34.ndjson
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
feat: resolve current app generated imports root package graph
```

## 完成记录

- Java resolver 移除了 source generated imports root 的 diagnostic placeholder 路径，
  将 root bare imports 统一重写到当前 app production
  `generated-flow-imports-*.js` support artifact，并暴露当前 app production webapp build
  `.js` artifact graph。
- Swift host 支持 minified ESM `from"./..."` / `import"./..."` 形式的 manifest rewrite，
  stage/import 当前 app production support graph，并记录
  `currentAppGeneratedImportsRootProductionSupportExecuted=true` 与
  `currentAppGeneratedImportsRootPackageGraphExecuted=true`。
- Trace inspector 新增并通过：
  `--require-flow-ipc-v1_34-current-app-generated-imports-root-package-graph` 和
  `--require-flow-ipc-v1_34`。
- 固定 trace：`docs/traces/flow-ipc-v1_34.ndjson`。

v1.34 完成的是 current app generated imports root package graph 支持路径，不是完整
frontend/resource pipeline 闭环；production owner side-effect/reuse、dev-mode/Vite execution
和更广组件覆盖仍需后续阶段推进。
