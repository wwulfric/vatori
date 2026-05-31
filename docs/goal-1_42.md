# Goal 1.42: Generated Imports Owner Runtime Contract

状态：待开始

`docs/goal.md` 是长期愿景和完成度账本。v1.41 已证明 strict smoke app 的当前
`build-frontend` output 可以通过 production `generated-flow-imports-*.js` support artifact
注册并复用 `vaadin-big-decimal-field`、`vaadin-checkbox`、`vaadin-select` 和
`vaadin-text-field`，四个 focused imports 全部 skipped，且 Select 可见事件链通过。

v1.42 的目标是把这条路径从 smoke trace candidate 提升为明确 runtime contract：Java manifest、
resource response、Swift owner import、component resolver 和 inspector gate 都必须把
current app generated imports owner 作为稳定的 covered component owner 输入，而不是只把它当作
诊断性 side-effect probe。同时，trace 必须为下一步真实 generated lazy chunk / Vaadin build SPI
决策保留清晰入口，避免再把 fixture success 或 no-op table 误算成最终闭环。

## 核心目标

新增 `flow-ipc-current-app-generated-imports-owner-runtime-contract-v1_42` contract，覆盖：

- Runtime owner contract：Java manifest/resource response 声明 selected generated imports support
  artifact、artifact source、expected covered tags、owner priority、skip policy、fallback policy、
  integrity metadata 和 no-fixture/no-localhost proof。
- Host import contract：Swift host 在 covered component focused imports 之前导入 selected support
  artifact，并把 module identity、registry/global before/after、import status、browser error 和
  side-effect status 写入 `resourceStage`。
- Resolver handoff：component resolver 使用 v1.42 owner contract 作为可复用 owner 输入；四个
  covered components 在 owner side effect 满足时必须稳定跳过 focused imports，而不是依赖最后一次
  diagnostic merge 的偶然状态。
- Lazy chunk SPI bridge evidence：trace 同步记录 current app generated imports body 的
  `Flow.loadOnDemand` table shape、chunk ids、chunk file availability、build output source 和
  supported-SPI decision placeholder，为后续阶段接入真实 Vaadin lazy chunk SPI 做准备。
- Visible Flow regression guard：Button、BigDecimalField、Checkbox、TextField、Select、server push、
  resync、file dialog cancel 和 error reporting 继续通过真实 Flow client -> IPC-backed UIDL path。

## Acceptable outcomes

- Preferred：v1.42 trace 证明 generated imports owner runtime contract 是稳定 component resolver
  输入，四个 covered focused imports 全部由 contract-driven owner reuse 跳过，fallback tags 为空；
  lazy chunk SPI bridge 记录当前真实 build output 的 table/chunk state 和下一步 supported-SPI
  decision。
- Acceptable：如果 owner contract 不能稳定驱动 resolver，trace 必须记录 owner artifact、contract
  fields、import result、observed/missing tags、resolver decision、browser/native error 和具体
  failure layer，不能回退成 v1.41 的 side-effect candidate 分类。

## 不做什么

- 不声明完整 frontend/resource pipeline 闭环。
- 不把 v1.22 generated chunk fixture 或 v1.23 production owner fixture 当作 current app runtime
  success。
- 不引入 localhost/Vite dev server 作为 Flow IPC 成功证据。
- 不一次性解决 dev-mode/Vite parity、真实 app generated lazy chunk SPI、Windows/Linux host、
  installer packaging 或 broad component matrix。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-smoke-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_42-generated-imports-owner-runtime-contract .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_42 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_42.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_42-generated-imports-owner-runtime-contract docs/traces/flow-ipc-v1_42.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_42 docs/traces/flow-ipc-v1_42.ndjson
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
feat: promote generated imports owner runtime contract
```
