# Goal 1.55: Flow DownloadHandler Save/Open Grant Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.50-v1.52 已经证明普通 Flow
`StreamResource` download path 可以通过 no-localhost IPC 被解析为文本、binary range 和
sequential chunks；v1.53-v1.54 已经证明普通 Flow `Upload` target 可以通过真实
`ElementRequestHandler` / `UploadHandler` 接收 raw 与 bounded multipart 请求。

v1.55 的目标是补上下载资源到桌面文件能力的关键边界：framework validation app 暴露普通
Vaadin `DownloadHandler` download action，WebView/host 观察 Vaadin 生成的
`VAADIN/dynamic/resource/...` href，经 Flow IPC `resourceRequest` 调用真实
`DownloadHandler`，再由 Swift host 通过 manifest-gated `fileTransfer.download` 能力写入受控
save path，并记录 save/open grant、redacted path、byte count、hash、permission 和 no-localhost
证据。

## 核心目标

新增 `flow-ipc-download-handler-save-open-v1_55` contract，覆盖：

- Framework app：新增普通 Flow `Anchor` + `DownloadHandler.fromInputStream(...)` 下载入口和可见
  save/open status。
- Target discovery：host/WebView 读取真实 Vaadin-generated
  `VAADIN/dynamic/resource/.../flow-ipc-download-handler-v1_55.txt` href。
- Runtime resolver：Java sidecar 从当前 `VaadinSession.StreamResourceRegistry` 解析真实
  `StreamResourceRegistry.ElementStreamResource`，调用真实 `DownloadHandler`，并通过 desktop
  `VaadinRequest` / `VaadinResponse` adapter 捕获 response body、headers、filename、MIME 和 status。
- Native save/open bridge：Swift host 在收到 v1.55 download response 后，通过 manifest-gated
  `fileTransfer.download` 能力写入 controlled test save path；trace 必须记录 permission mode、
  grant type、redacted path/filename、byte count、sha256、body redaction、security-scoped
  classification 和 open-after-save classification。
- WebView verification：WebView 记录 resource request/response、native capability result、
  save/open completion 和 visible DOM diagnostics。
- Trace gates：新增 `--require-flow-ipc-v1_55-download-save-open` 和
  `--require-flow-ipc-v1_55`。

## Acceptable outcomes

- Preferred：真实 Flow `DownloadHandler` 被 Vaadin 注册，WebView 观察到 dynamic resource href，
  Java sidecar 调用真实 handler 返回 body/headers，Swift host 写入受控 save path，DOM summary
  和 capability result 共同证明 no-localhost download -> native save/open bridge。
- Acceptable blocker：如果当前 no-HTTP runtime 不能完整调用 `DownloadHandler` 或能力网关不能写入文件，
  trace 必须明确记录 `download-handler-*` failure layer、resource path、resource/handler type、
  response status、capability decision、permission mode、save path classification 和下一步拆解；不能用
  `StreamResource`、localhost download 或 repository fixture 替代。

## 不做什么

- 不实现完整 OS `NSSavePanel` / open-after-save user flow；自动验收使用 controlled test path。
- 不声明大文件 streaming、parallel chunk、retry/resume、真实 backpressure 或 sandbox production
  grants 完成。
- 不通过 localhost HTTP compatibility mode 下载。
- 不把 v1.50-v1.52 `StreamResource` success 当作 `DownloadHandler` success。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_MULTIPART=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DOWNLOAD_SAVE_OPEN=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_55-download-save-open .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_55 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_55-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_55-download-save-open docs/traces/flow-ipc-v1_55-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_55 docs/traces/flow-ipc-v1_55-framework.ndjson
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
feat: add Flow DownloadHandler save-open contract
```

## 完成记录

- Framework validation app 新增普通 Flow `Anchor` + `DownloadHandler` 下载入口，固定文件名
  `flow-ipc-download-handler-v1_55.txt`。
- Java Flow IPC runtime 通过当前 `VaadinSession.StreamResourceRegistry` 解析真实
  `StreamResourceRegistry.ElementStreamResource`，调用真实 `DownloadHandler` / `DownloadResponse`
  并返回 body、headers、filename、MIME、status、sha256 和 no-localhost/no-fixture metadata。
- Swift host 在收到 v1.55 `resourceResponse` 后走 manifest-gated `fileTransfer.download`
  能力，把 body 写入 controlled save path，并记录 permission、grant、redacted path、byte count、
  body redaction 和 open-after-save classification。
- WebView DOM summary 记录 request/response/capability/file-written/body/hash 全部 verified。
- 固定 trace：`docs/traces/flow-ipc-v1_55-framework.ndjson`。

已通过：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_MULTIPART=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DOWNLOAD_SAVE_OPEN=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_55-download-save-open .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_55 .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_55-download-save-open docs/traces/flow-ipc-v1_55-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_55 docs/traces/flow-ipc-v1_55-framework.ndjson
```
