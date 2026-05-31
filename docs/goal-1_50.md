# Goal 1.50: Flow StreamResource Dynamic Download Resource Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.49 已经把 framework validation app 的
callback-backed lazy `Grid` 从直接 published RPC 推进到真实 Grid `scrollToIndex(45)` client
API probe，但 frontend/resource pipeline 仍缺少普通 Flow 动态资源 URL 的 no-localhost
正向证据。

v1.50 的目标是把 resource resolver 从构建期 frontend artifact 扩展到一个真实 Flow
`StreamResource` 动态下载：framework app 暴露普通 `Anchor` + `StreamResource`，WebView 观察
Vaadin 生成的 `VAADIN/dynamic/resource/...` href，host 通过 Flow IPC `resourceRequest` 向
Java sidecar 请求该动态资源，Java 从当前 `VaadinSession` 的 `StreamResourceRegistry` 解析并
返回 body / MIME / filename / integrity metadata，trace gate 验证 no-localhost 下载资源链路。

## 核心目标

新增 `flow-ipc-stream-resource-v1_50` contract，覆盖：

- Framework app：新增普通 Flow `Anchor` / `StreamResource` 下载入口和可见状态。
- Dynamic resource URL：WebView 读取真实 `href`，必须是 Vaadin 生成的
  `VAADIN/dynamic/resource/...` path，而不是 repository-local fake URL。
- Runtime resolver：`FlowIpcRuntime` 支持动态 `VAADIN/dynamic/resource/...` `resourceRequest`，
  通过 `DesktopFlowRuntimeSession` 从当前 `StreamResourceRegistry` 解析真实 resource。
- Resource response：返回 filename、content type、byte count、sha256、base64 body、
  registry source、transport、no-localhost/no-fixture proof 和 request purpose。
- WebView verification：host/WebView 自动请求该 resource，验证 response metadata 和文本 body，
  并在 `webview.dom` summary 中记录 anchor href、request、response、hash/body proof 和完成状态。
- Trace gates：新增 `--require-flow-ipc-v1_50-stream-resource` 和 `--require-flow-ipc-v1_50`。

## Acceptable outcomes

- Preferred：真实 Flow `StreamResource` 被 Vaadin 注册，WebView 观察到动态 resource href，
  IPC resource request 返回正确 body / MIME / filename / sha256，并记录 visible DOM summary。
- Acceptable blocker：如果当前 no-HTTP runtime 暂时无法通过公开 API 解析该 resource，trace 必须明确
  记录 `stream-resource-registry-unavailable`、resource path、session/ui metadata、failure layer 和
  下一步拆解；不能用静态 fixture 或 localhost download 替代。

## 不做什么

- 不实现完整 Upload/Download streaming、进度、取消、Range request 或大文件背压。
- 不声明全部 dynamic resource、theme/static、Grid、dev-mode/Vite、installer packaging 或 durable
  reconnect parity。
- 不通过 localhost HTTP compatibility mode 下载 resource。
- 不把 repository-local static fixture 当作 Vaadin `StreamResource` 成功。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_SCROLL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_50-stream-resource .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_50-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_50-stream-resource docs/traces/flow-ipc-v1_50-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_50 docs/traces/flow-ipc-v1_50-framework.ndjson
```

## 完成后更新

- `docs/goal.md`
- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/acceptance-tools.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/roadmap.md`
- `test-apps/flow-over-ipc-framework-app/README.md`

完成提交必须使用 Conventional Commit，例如：

```text
feat: add Flow StreamResource IPC resolver contract
```

## 完成记录

- Framework validation app 新增普通 Flow `Anchor` + `StreamResource` 下载入口，生成真实
  `VAADIN/dynamic/resource/.../flow-ipc-framework-v1_50.txt` href。
- `FlowIpcRuntime` 对动态 resource path 走当前 `VaadinSession.StreamResourceRegistry`，
  `DesktopFlowRuntimeSession` 返回 filename、content type、byte count、sha256、base64/text body、
  no-localhost/no-fixture proof。
- Swift/WebView 自动读取 anchor href 并发送 `resourceRequest`，验证 response body marker、
  filename、MIME、hash/body metadata，并在 `webview.dom` summary 记录
  `frameworkStreamResourceComplete=true`。
- Trace inspector 新增 `--require-flow-ipc-v1_50-stream-resource` / `--require-flow-ipc-v1_50`。
  该 gate 只验证 v1.50 自身和必要 framework baseline，不再把最新 delta 绑定到整条历史 gate。
- 固定 trace：`docs/traces/flow-ipc-v1_50-framework.ndjson`。
