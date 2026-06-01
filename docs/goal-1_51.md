# Goal 1.51: Flow StreamResource Binary Range Download Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.50 已经证明普通 Flow `Anchor` /
`StreamResource` 的文本动态下载可以通过 no-localhost IPC `resourceRequest` 从
`VaadinSession.StreamResourceRegistry` 解析，并验证 filename、MIME、body 和 sha256。

v1.51 的目标是把该下载侧资源路径推进到更接近真实 Upload/Download streaming 的边界：
framework app 暴露第二个普通 Flow `StreamResource`，内容为确定性的二进制 payload；
WebView/host 读取 Vaadin 生成的 `VAADIN/dynamic/resource/...` href 后发送带 Range 语义的
IPC `resourceRequest`；Java sidecar 解析完整 resource，但只回传请求的 byte slice、range
metadata、full body integrity 和 full-body-omitted/backpressure 诊断。

## 核心目标

新增 `flow-ipc-stream-resource-range-v1_51` contract，覆盖：

- Framework app：新增普通 Flow `Anchor` / binary `StreamResource` 下载入口和可见状态。
- Dynamic resource URL：WebView 读取真实 `href`，必须是 Vaadin 生成的
  `VAADIN/dynamic/resource/.../flow-ipc-framework-v1_51.bin` path。
- Range request：host 通过 Flow IPC `resourceRequest` 发送 `Range: bytes=0-255` 等价字段，
  不通过 localhost HTTP header。
- Runtime resolver：`FlowIpcRuntime` / `DesktopFlowRuntimeSession` 从当前
  `StreamResourceRegistry` 解析完整 resource，计算 full byte count / sha256，但 response 只携带
  0-255 slice 的 base64、byte count、sha256 和 marker proof。
- Backpressure evidence：trace 必须记录 `streamResourceFullBodyOmitted=true`、
  `streamResourceResponseMode=range-slice`、range start/end/bytes、full bytes/hash、
  no-localhost/no-fixture proof。
- WebView verification：host/WebView 自动请求 range slice，验证 filename、MIME、range length、
  marker 和 omitted/full metadata，并在 `webview.dom` summary 中记录完成状态。
- Trace gates：新增 `--require-flow-ipc-v1_51-stream-resource-range` 和 `--require-flow-ipc-v1_51`。

## Acceptable outcomes

- Preferred：真实 Flow binary `StreamResource` 被 Vaadin 注册，WebView 观察到动态 resource href，
  IPC range request 返回正确 slice metadata 和 body marker，并记录 visible DOM summary。
- Acceptable blocker：如果当前 no-HTTP runtime 暂时无法表达 range 或 resource registry lookup，
  trace 必须明确记录 `stream-resource-range-*` failure layer、resource path、range request、
  full/slice metadata 状态和下一步拆解；不能用静态 fixture 或 localhost download 替代。

## 不做什么

- 不实现完整 Vaadin `Upload`、`DownloadHandler`、大文件流式管道、进度、取消或 OS save/open panel。
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
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_51-stream-resource-range .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_51 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_51-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_51-stream-resource-range docs/traces/flow-ipc-v1_51-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_51 docs/traces/flow-ipc-v1_51-framework.ndjson
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
feat: add Flow StreamResource range download contract
```

## 完成记录

- Framework app 新增普通 Flow binary `StreamResource` 下载入口，WebView 读取真实
  `VAADIN/dynamic/resource/.../flow-ipc-framework-v1_51.bin` href。
- Host 通过 no-localhost IPC `resourceRequest` 发送 `Range: bytes=0-255` 等价字段。
- Java sidecar 从 `VaadinSession.StreamResourceRegistry` 解析完整 resource，返回 256-byte slice、
  slice/full sha256、full byte count、`streamResourceFullBodyOmitted=true` 和
  `streamResourceResponseMode=range-slice`。
- 固定 trace：`docs/traces/flow-ipc-v1_51-framework.ndjson`。
- 已通过 `--require-flow-ipc-v1_51-stream-resource-range` 和 `--require-flow-ipc-v1_51`。
