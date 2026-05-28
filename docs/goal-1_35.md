# Goal 1.35: Current App Production Runtime Ownership Handoff

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.34 已把 current app source
`generated-flow-imports.js` root 的 package imports 从 diagnostic placeholder 推进到当前 app
production `generated-flow-imports-*.js` support graph，并证明 source root、source chunk 和
production support graph 可以在 no-localhost Flow-client-triggered path 中执行。

v1.35 的目标是把 v1.32-v1.34 分散的 current-app build/root/chunk 证据收束成明确的
production runtime ownership handoff contract：当前 app build output 已经具备成为 primary
frontend runtime candidate 的 artifact 条件，但真实 production owner side-effect / reuse 仍是下一阶段
blocker，不能被本阶段包装成成功。

## 核心目标

新增 `flow-ipc-current-app-production-runtime-ownership-v1_35` contract，覆盖：

- Candidate selection：记录 primary runtime candidate、selected source imports、source chunk、
  production support artifact、production entry、stats、`prod.bundle` 和 artifact inventory。
- Handoff readiness：当 current app source generated imports/chunk、root package graph、
  production generated imports support、stats 和 `prod.bundle` 都被观察到，trace 记录
  `currentAppProductionRuntimeOwnershipHandoffReady=true`。
- Runtime ownership boundary：明确当前成功仍停在 current-app generated root/chunk execution；
  production owner side-effect / owner-owned component reuse 尚未成功，记录
  `currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied=false`。
- No-localhost / no-fixture proof：candidate 不能依赖 localhost，也不能依赖 v1.22/v1.23 repository
  positive fixture；trace 必须记录 no-localhost、fixtureSubstituted=false。
- Remaining blocker：将 v1.34 的 broad blocker 收敛为
  `current-app-production-runtime-ownership-handoff-pending`，下一步是把 candidate 接入真正
  production owner import/side-effect/reuse path，或由 supported Vaadin build SPI 接管。

## Acceptable outcomes

- Preferred：v1.35 trace 同时证明 current app source root/chunk 执行、production support artifact
  执行、stats/prod.bundle 可用、primary candidate selected、handoffReady=true；并明确
  ownerSideEffectSatisfied=false。
- Acceptable：某个 current-app artifact 条件缺失，但 trace 必须精确说明缺失 artifact、failure layer
  和下一步，不能回退成 v1.34 以前的 placeholder/package graph blocker。

## 不做什么

- 不声明完整 frontend/resource pipeline 基本闭环。
- 不把 v1.23 repository production owner positive fixture 当作真实 current app production owner 成功。
- 不把 current app source generated imports/chunk execution 误称为 production owner side-effect success。
- 不引入 localhost/Vite dev server 作为成功证据。

## 实现要求

### Java resolver / metadata

- `FlowClientResources` 增加 v1.35 metadata：
  - `currentAppProductionRuntimeOwnershipVersion=flow-ipc-current-app-production-runtime-ownership-v1_35`
  - `currentAppProductionRuntimeOwnershipSource=current-app-build-frontend-output-v1_32-to-v1_34`
  - `currentAppProductionRuntimeOwnershipPrimaryCandidate=<classification>`
  - `currentAppProductionRuntimeOwnershipSelectedImportsArtifact=<virtual path>`
  - `currentAppProductionRuntimeOwnershipSelectedChunkArtifact=<virtual path>`
  - `currentAppProductionRuntimeOwnershipProductionSupportArtifact=<virtual path>`
  - `currentAppProductionRuntimeOwnershipProductionEntryArtifact=<virtual path>`
  - `currentAppProductionRuntimeOwnershipProdBundleObserved=<boolean>`
  - `currentAppProductionRuntimeOwnershipStatsObserved=<boolean>`
  - `currentAppProductionRuntimeOwnershipArtifactInventory=<csv>`
  - `currentAppProductionRuntimeOwnershipHandoffReady=<boolean>`
  - `currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied=false`
  - `currentAppProductionRuntimeOwnershipFixtureSubstituted=false`
  - `currentAppProductionRuntimeOwnershipFailureLayer=<classification>`
- 保留 v1.34 root package graph fields，且不得重新引入 connection indicator placeholder。

### Swift host / WebView diagnostics

- `resourceStage` 和 `webview.dom` 必须记录 v1.35 candidate metadata。
- 当 v1.34 的 source chunk、root support graph 都执行后，设置：
  - `currentAppProductionRuntimeOwnershipCandidateObserved=true`
  - `currentAppProductionRuntimeOwnershipArtifactsStaged=true`
  - `currentAppProductionRuntimeOwnershipPrimaryCandidateSelected=true`
  - `currentAppProductionRuntimeOwnershipHandoffReady=true`
  - `currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied=false`

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_35-current-app-production-runtime-ownership docs/traces/flow-ipc-v1_35.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_35 docs/traces/flow-ipc-v1_35.ndjson
```

`--require-flow-ipc-v1_35-current-app-production-runtime-ownership` 只验证 v1.35 delta；
`--require-flow-ipc-v1_35` 继承 v1.34 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_35-current-app-production-runtime-ownership .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_35 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_35.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_35-current-app-production-runtime-ownership docs/traces/flow-ipc-v1_35.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_35 docs/traces/flow-ipc-v1_35.ndjson
```

## 完成记录

- 已实现 `flow-ipc-current-app-production-runtime-ownership-v1_35` metadata，覆盖 current app
  source generated imports/chunk、production support artifact、production entry、stats、
  `prod.bundle` 和 artifact inventory。
- Swift host / WebView diagnostics 已在 `resourceStage` 和 `webview.dom` 中记录
  `currentAppProductionRuntimeOwnershipCandidateObserved=true`、
  `currentAppProductionRuntimeOwnershipArtifactsStaged=true`、
  `currentAppProductionRuntimeOwnershipPrimaryCandidateSelected=true`、
  `currentAppProductionRuntimeOwnershipHandoffReady=true`，同时明确
  `currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied=false` 和
  `currentAppProductionRuntimeOwnershipOwnerReuseSatisfied=false`。
- Trace inspector 已新增 delta/cumulative gates：
  `--require-flow-ipc-v1_35-current-app-production-runtime-ownership` 和
  `--require-flow-ipc-v1_35`。
- 固定 fixture：`docs/traces/flow-ipc-v1_35.ndjson`。
- 该阶段没有声明完整 frontend/resource pipeline 闭环；剩余 blocker 仍是 current app
  production owner import/side-effect/reuse、受支持 Vaadin lazy chunk SPI 或 dev-mode/Vite
  execution，以及更广组件/资源覆盖。

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
feat: add current app production runtime ownership handoff
```
