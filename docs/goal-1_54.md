# Goal 1.54: Flow Upload Multipart Progress and Cancel Contract

状态：待执行

`docs/goal.md` 是长期愿景和完成度账本。v1.53 已经证明普通 Flow `Upload`
的 Vaadin dynamic target 可以通过 no-localhost IPC 被调用：host 观察真实
`vaadin-upload` target，发送 bounded raw upload request，Java sidecar 从当前
`VaadinSession.StreamResourceRegistry` 解析真实 `ElementStreamResource`，调用真实
`UploadHandler`，并通过 UIDL patch 更新可见 DOM。

v1.54 的目标是把 Upload receiver 从 raw-body positive path 推进到更接近真实浏览器上传的
bounded multipart/progress/cancel contract：host 构造单文件 `multipart/form-data` 请求，
通过同一 no-localhost Upload target 进入真实 `ElementRequestHandler` / `UploadHandler`，
并记录 progress/cancel 的可审计边界。成功路径必须继续由 visible Flow DOM status 证明；如果
Vaadin 当前 upload handler 对 no-HTTP multipart 形状仍有缺口，trace 必须把缺口定位到 request
shape、headers、multipart parser、receiver metadata 或 Flow Upload client contract 的具体层。

## 核心目标

新增 `flow-ipc-upload-multipart-v1_54` contract，覆盖：

- Framework app：复用普通 Flow `Upload` 组件，新增 multipart/progress/cancel 可见 status。
- Multipart request：host/WebView 对真实 `vaadin-upload` target dynamic resource href 发送
  bounded single-file `multipart/form-data` upload，记录 boundary、part headers、filename、
  content type、file byte count、file sha256、multipart byte count 和 request body mode。
- Runtime receiver：Java sidecar 仍从当前 `VaadinSession.StreamResourceRegistry` 解析真实
  `StreamResourceRegistry.ElementStreamResource`，调用真实 `ElementRequestHandler` /
  `UploadHandler`，并记录 handler type、resource type、status code 和 receiver metadata。
- Progress diagnostics：trace 至少记录 host-side progress checkpoints、total bytes、delivered bytes、
  terminal status，以及当前是否被真实 Vaadin Upload client progress UI 消费。
- Cancel diagnostics：trace 必须有一个 bounded cancel probe 或明确 blocker classification，
  说明 cancel 发生在 host request、WebView bridge、Upload client event、Java handler 或不可支持层。
- UIDL patch：成功 upload handler 更新 server-side status，Java sidecar 下发 patch，WebView 验证
  visible DOM 出现 multipart upload success marker。
- Trace gates：新增 `--require-flow-ipc-v1_54-upload-multipart` 和 `--require-flow-ipc-v1_54`。

## Acceptable outcomes

- Preferred：单文件 multipart upload 通过真实 Flow `Upload` target 到达
  `ElementRequestHandler` / `UploadHandler`，visible DOM 验证 multipart success；progress
  checkpoints 和 cancel probe 都被 trace 分类。
- Acceptable blocker：如果当前 no-HTTP runtime 不能完整表达 multipart/progress/cancel，trace 必须
  明确记录 `upload-multipart-*` failure layer、target path、request headers、body mode、resource
  type、handler type、status code、receiver metadata、progress/cancel 状态和下一步拆解；不能用
  localhost upload、纯 `$server` call 或 repository fixture 替代。

## 不做什么

- 不声明完整 Vaadin multi-file upload parity。
- 不实现大文件 streaming、parallel upload、retry/resume 或真实 backpressure。
- 不实现 OS open/save panel、sandbox grants 或 security-scoped file access。
- 不通过 localhost HTTP compatibility mode 上传。
- 不把 v1.53 raw-body success 当作 v1.54 multipart success。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_MULTIPART=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_54-upload-multipart .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_54 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_54-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_54-upload-multipart docs/traces/flow-ipc-v1_54-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_54 docs/traces/flow-ipc-v1_54-framework.ndjson
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
feat: add Flow Upload multipart receiver contract
```
