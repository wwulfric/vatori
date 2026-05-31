# Goal 1.40: Current App Generated Imports Owner Side-Effect Candidate

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.39 已证明 current app production owner
可以在 covered component focused imports 之前提前请求、stage 和 import，但导入的
production entry `indexhtml-*.js` 没有暴露可复用的 covered component tags，真实 trace
停在 `current-app-owner-early-import-side-effect-not-reusable`。

v1.40 的目标是验证并推进一个更精确的 owner side-effect candidate：当前 app
`build-frontend` 产物中的 production `generated-flow-imports-*.js` support artifact。该
artifact 是当前 app production build 输出里实际包含 Vaadin component class definitions、
`customElements.define(...)` wrapper 和 `window.Vaadin.Flow.loadOnDemand` 安装的模块。

## 核心目标

新增 `flow-ipc-current-app-generated-imports-owner-side-effect-v1_40` contract，覆盖：

- Java manifest / resource response 声明 generated imports owner side-effect candidate、
  source、policy、candidate artifact、expected tags/globals、known missing tags、
  decision、failure layer 和 next step。
- Swift host 在 covered component focused imports 之前，除了 v1.39 的 production entry
  early import 外，还必须对当前 app production `generated-flow-imports-*.js` support
  artifact 发起 host-owned `vatori-resource://` import attempt。
- Trace 必须记录 support artifact 的 request、response、stage/import result、module identity、
  registry/global before/after、observed/missing tags、`loadOnDemand` 状态、browser error、
  no-localhost 和 no-fixture proof。
- Component resolver 必须消费 generated imports owner side effect：如果该 support artifact
  已注册某个 covered component tag，后续对应 focused import 应被跳过；未被 support artifact
  覆盖的组件必须明确记录 fallback 原因，而不是继续归因到 owner not ready。
- 可见 Flow path 必须继续通过：Button、BigDecimalField、Checkbox、TextField、resync、
  file dialog cancel 和 error reporting 不能回退。

## Acceptable outcomes

- Preferred：production `generated-flow-imports-*.js` import 成功，至少复用当前 app owner 已
  注册的 `vaadin-checkbox` 和 `vaadin-text-field`，并跳过对应 focused imports；未覆盖的
  `vaadin-select` 和/或 `vaadin-big-decimal-field` 明确分类为
  `generated-imports-owner-does-not-own-tag` 或 owner-ready focused fallback。
- Acceptable：support artifact import 真实发生但被 browser/global/custom-element 兼容性阻塞；
  trace 必须记录具体 artifact、custom-scheme URL/module identity、browser error、registry/global
  diagnostics 和精确 failure layer，不能继续停留在 v1.39 的 generic entry side-effect
  unobservable blocker。

## 不做什么

- 不声明完整 frontend/resource pipeline 闭环。
- 不把 repository fixture 的 owner success 计为 current app owner success。
- 不引入 localhost/Vite dev server 作为成功证据。
- 不删除 focused fallback；未被 generated imports owner 覆盖的组件仍必须保持可见行为正确。
- 不一次性解决 dev-mode/Vite parity、Windows/Linux host 或 broad component/resource parity。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_40-current-app-generated-imports-owner-side-effect .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_40 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_40.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_40-current-app-generated-imports-owner-side-effect docs/traces/flow-ipc-v1_40.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_40 docs/traces/flow-ipc-v1_40.ndjson
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
feat: import generated imports owner side effects
```

## 完成记录

v1.40 已新增并验证
`flow-ipc-current-app-generated-imports-owner-side-effect-v1_40` contract。真实窗口 trace 证明
当前 app production `generated-flow-imports-*.js` support artifact 会在 covered component
focused imports 前通过 host-owned `vatori-resource://` stage/import，并记录 module identity、
registry/global before/after、`loadOnDemand` status、no-localhost/no-fixture proof。

验收 trace 记录：

- `currentAppGeneratedImportsOwnerSideEffectImported=true`
- `currentAppGeneratedImportsOwnerSideEffectObservedTags=vaadin-big-decimal-field,vaadin-checkbox,vaadin-text-field`
- `currentAppGeneratedImportsOwnerSideEffectReusedTags=vaadin-big-decimal-field,vaadin-checkbox,vaadin-text-field`
- `currentAppGeneratedImportsOwnerSideEffectSkippedFocusedImports=...vaadin-big-decimal-field.js,...vaadin-checkbox.js,...vaadin-text-field.js`
- `currentAppGeneratedImportsOwnerSideEffectFallbackTags=vaadin-select`

BigDecimalField、Checkbox、TextField 的可见 Flow event -> Java listener -> UIDL patch -> DOM path
继续通过；`vaadin-select` 被保留为明确 fallback。该阶段仍不声明完整 frontend/resource pipeline
闭环，剩余 blocker 是 `vaadin-select` / broader tag coverage、真实 generated chunk parity、
supported Vaadin build/runtime SPI 和 dev-mode/Vite parity。
