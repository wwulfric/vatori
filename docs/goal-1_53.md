# Goal 1.53: Flow Upload ElementRequestHandler Receiver Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.50-v1.52 已经把真实 Flow
`StreamResource` download path 从 dynamic resource lookup 推进到 binary range 和 sequential
chunk download。

v1.53 的目标是把 resource pipeline 推到上传方向：在 framework validation app 中加入普通
Flow `Upload` 组件，WebView/host 观察 `vaadin-upload` 生成的
`VAADIN/dynamic/resource/...` target，通过 Flow IPC 发送 bounded raw upload request，Java
sidecar 从 `VaadinSession.StreamResourceRegistry` 解析真实 `ElementStreamResource`，调用真实
`ElementRequestHandler` / `UploadHandler`，然后用 UIDL patch 更新可见 DOM。

## 核心目标

新增 `flow-ipc-upload-receiver-v1_53` contract，覆盖：

- Framework app：新增普通 Flow `Upload` 组件和可见 upload status。
- Target discovery：host/WebView 读取真实 `vaadin-upload` `target` dynamic resource href。
- Runtime receiver：Java sidecar 解析当前 `VaadinSession.StreamResourceRegistry` 中的
  `StreamResourceRegistry.ElementStreamResource`，调用真实 `UploadHandler`。
- Bounded payload：host 发送一个小型 text/plain raw-XHR-compatible upload body，并记录文件名、
  content type、byte count、sha256、response status、no-localhost 和 no-fixture proof。
- UIDL patch：upload handler 更新 server-side status，Java sidecar 下发 patch，WebView 验证
  visible DOM 出现 `Upload received: flow-ipc-upload-v1_53.txt` 和 marker。
- Trace gates：新增 `--require-flow-ipc-v1_53-upload-receiver` 和 `--require-flow-ipc-v1_53`。

## Acceptable outcomes

- Preferred：真实 Flow `Upload` target 被发现，raw upload 经 no-localhost IPC 到达
  `ElementRequestHandler` / `UploadHandler`，status patch 被可见 DOM 验证。
- Acceptable blocker：如果当前 no-HTTP runtime 无法调用 upload target，trace 必须明确记录
  `upload-receiver-*` failure layer、resource path、resource type、handler status、response status、
  body/hash metadata 和下一步拆解；不能用 localhost upload 或纯 `$server` 诊断桥替代。

## 不做什么

- 不声明完整 Vaadin multipart upload parity。
- 不实现多文件、大文件 streaming、progress、cancel、retry/resume 或 backpressure。
- 不实现 OS open/save panel、sandbox grants 或 security-scoped file access。
- 不通过 localhost HTTP compatibility mode 上传。
- 不把 `$server` client-callable 或 repository fixture 当作 Upload receiver 成功。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_53-upload-receiver .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_53 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_53-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_53-upload-receiver docs/traces/flow-ipc-v1_53-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_53 docs/traces/flow-ipc-v1_53-framework.ndjson
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
feat: add Flow Upload bounded receiver contract
```

## 完成记录

v1.53 已实现并验证 `flow-ipc-upload-receiver-v1_53`：

- framework validation app 新增普通 Flow `Upload` 组件和可见 upload status。
- Swift host/WebView 观察真实 `vaadin-upload` target dynamic resource href，并通过 no-localhost
  Flow IPC 发送 bounded raw upload request。
- Java sidecar 通过当前 `VaadinSession.StreamResourceRegistry` 解析真实
  `StreamResourceRegistry.ElementStreamResource`，调用真实 `UploadHandler`，并用 UIDL patch 更新可见 DOM。
- DOM summary 记录 target/request/response/patch/body/status verified、文件名、content type、
  byte count、sha256、handler/resource type、no-localhost 和 no-fixture proof。
- 固定 trace 保存为 `docs/traces/flow-ipc-v1_53-framework.ndjson`。

已执行验收：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_53-upload-receiver .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_53 .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_53-upload-receiver docs/traces/flow-ipc-v1_53-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_53 docs/traces/flow-ipc-v1_53-framework.ndjson
```
