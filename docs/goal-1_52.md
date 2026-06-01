# Goal 1.52: Flow StreamResource Sequential Chunk Download Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.51 已经证明真实 Flow binary
`StreamResource` 可以通过 no-localhost IPC `resourceRequest` 请求单个 Range slice，并记录
full body omitted、slice/full hash 和 backpressure 诊断。

v1.52 的目标是把该路径推进到更接近 streaming 的 bounded sequential chunk download：
host 不再只请求一个 `bytes=0-255` slice，而是对同一个 Vaadin-generated
`VAADIN/dynamic/resource/.../flow-ipc-framework-v1_51.bin` href 发起顺序单窗口 chunk 请求
`0-255`、`256-511`、`512-767`。Java sidecar 仍从当前
`VaadinSession.StreamResourceRegistry` 解析真实 resource，每个 response 只返回当前 chunk，
并回显 chunk index、count、range、full body integrity 和 full-body-omitted 证据。

## 核心目标

新增 `flow-ipc-stream-resource-chunks-v1_52` contract，覆盖：

- Framework app：复用 v1.51 的普通 Flow binary `StreamResource`，新增可见 chunk status。
- Sequential request：host 对同一 dynamic resource href 顺序发送 3 个 IPC range chunk request，
  每次只允许一个 in-flight chunk。
- Runtime resolver：Java sidecar 解析完整 resource，但每个 response 只携带当前 chunk 的
  base64、byte count、sha256、chunk index/count、range 和 full body integrity。
- Backpressure evidence：trace 必须记录 chunk window size、requested/response order、
  aggregate downloaded bytes、full bytes/hash、`streamResourceFullBodyOmitted=true`、
  `streamResourceResponseMode=range-chunk-sequential`、no-localhost/no-fixture proof。
- WebView verification：host/WebView 验证 3 个 chunk 都按序返回，第一块包含 marker，
  每块长度 256，aggregate bytes 为 768，full bytes 大于 aggregate bytes。
- Trace gates：新增 `--require-flow-ipc-v1_52-stream-resource-chunks` 和
  `--require-flow-ipc-v1_52`。

## Acceptable outcomes

- Preferred：三个 sequential chunk request/response 全部通过同一个真实 Flow `StreamResource`
  registry lookup 完成，DOM summary 记录 request/response order、chunk count、aggregate bytes
  和 full-body-omitted proof。
- Acceptable blocker：如果当前 no-HTTP runtime 暂时无法表达顺序 chunk 或回压窗口，trace 必须明确
  记录 `stream-resource-chunks-*` failure layer、resource path、已请求 chunk、已响应 chunk、
  full/slice metadata 状态和下一步拆解；不能用静态 fixture 或 localhost download 替代。

## 不做什么

- 不实现完整 Vaadin `Upload`、`DownloadHandler`、进度、取消、OS save/open panel 或 sandbox grants。
- 不声明完整大文件流式传输、所有 Range 组合、parallel chunk、retry/resume、dev-mode/Vite、
  installer packaging 或 durable reconnect parity。
- 不通过 localhost HTTP compatibility mode 下载 resource。
- 不把 repository-local static fixture 当作 Vaadin `StreamResource` 成功。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_52-stream-resource-chunks .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_52 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_52-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_52-stream-resource-chunks docs/traces/flow-ipc-v1_52-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_52 docs/traces/flow-ipc-v1_52-framework.ndjson
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
feat: add Flow StreamResource sequential chunk download contract
```

## 完成记录

v1.52 已实现并验证 `flow-ipc-stream-resource-chunks-v1_52`：

- framework validation app 复用 v1.51 binary Flow `StreamResource`，新增 chunk status。
- Swift host/WebView 对同一个 Vaadin-generated dynamic resource href 顺序请求
  `0-255`、`256-511`、`512-767`，保持单窗口 sequential chunk。
- Java sidecar 通过当前 `VaadinSession.StreamResourceRegistry` 解析真实 resource，每次只返回当前
  256-byte chunk，并保留 full byte count / full sha256 / full body omitted proof。
- DOM summary 记录 request order、response order、verified chunk count、aggregate bytes、full bytes、
  response mode、no-localhost 和 no-fixture proof。
- 固定 trace 保存为 `docs/traces/flow-ipc-v1_52-framework.ndjson`。

已执行验收：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_52-stream-resource-chunks .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_52 .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_52-stream-resource-chunks docs/traces/flow-ipc-v1_52-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_52 docs/traces/flow-ipc-v1_52-framework.ndjson
```
