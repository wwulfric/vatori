# Flow over IPC Framework Validation App

This app is a developer-shaped validation project for the `dev-ipc` runtime. It is separate from the strict smoke fixture so the framework shell can be checked against a less test-specific Flow view.

Run:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

Current scope:

- The view is written with ordinary Flow Java APIs: `@Route`, `Button`, `VerticalLayout`, `Span`, `BigDecimalField`, `Checkbox`, `Select`, `ComboBox`, `DatePicker`, `Dialog`, `Grid`, `VirtualList`, `FormLayout`, Binder-backed `TextField` validation, a callback-backed lazy `Grid`, `Anchor`/`StreamResource` download, binary StreamResource range/chunk download, raw and multipart `Upload` receiver paths, `DownloadHandler` save/open verification and a `LoadMode.LAZY` JavaScript dependency.
- It also includes the v1.27 lazy real-build probe input shape so explicit `build-frontend` runs can produce current-app generated imports/chunk evidence for the v1.45 packaging runtime handoff reuse gate.
- It uses semantic component ids only for optional diagnostics; the runtime must not depend on them for renderer promotion, layout, push/resync delivery or bridge installation.
- It covers a Button command path, real Vaadin Web Components, bounded data/Binder validation paths, a lazy DataProvider viewport RPC, a real Flow `StreamResource` dynamic download, a binary range download slice, sequential chunk download evidence, bounded raw and multipart Flow `Upload` receiver paths, a Flow `DownloadHandler` save/open path, a v1.10 lazy dependency side effect, a repeated-list text update and manifest-gated desktop capability calls.
- It has a dedicated framework-shape gate, while `test-apps/flow-over-ipc-smoke-app` remains the strict trace fixture for historical cumulative resource gates. v1.45 adds a dedicated framework packaging handoff gate over this app, v1.46 adds a broader component matrix gate for ComboBox, DatePicker and Dialog, v1.47 adds a data component/Binder matrix gate for Grid, VirtualList, FormLayout and owner validation, v1.48 adds a Grid lazy DataProvider viewport RPC gate, v1.49 adds a bounded Grid client `scrollToIndex(45)` probe gate, v1.50 adds a Flow `StreamResource` dynamic download gate, v1.51 adds a binary StreamResource range download gate, v1.52 adds a sequential StreamResource chunk download gate, v1.53 adds a Flow `Upload` ElementRequestHandler receiver gate, v1.54 adds a bounded multipart Upload/progress/cancel classification gate, and v1.55 adds a Flow `DownloadHandler` save/open grant gate.

For unattended first-paint verification:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

For unattended interaction verification:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

The framework gate requires generic viewport promotion, first meaningful paint, component events, a list text update and manifest-allowed desktop capability command/result traces.

For v1.45 packaging runtime handoff reuse verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45-framework-packaging-runtime-handoff-reuse .build/vaadin-desktop-host-swift/host.out.log
```

For v1.46 broader component resource matrix verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_46-broader-component-resource-matrix .build/vaadin-desktop-host-swift/host.out.log
```

For v1.47 data component resource matrix verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_47-data-component-resource-matrix .build/vaadin-desktop-host-swift/host.out.log
```

For v1.48 Grid lazy DataProvider viewport RPC verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_48-grid-lazy-data-provider .build/vaadin-desktop-host-swift/host.out.log
```

For v1.49 Grid client scroll probe verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_SCROLL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_49-grid-scroll-virtualization .build/vaadin-desktop-host-swift/host.out.log
```

For v1.50 StreamResource dynamic download verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_SCROLL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_50-stream-resource .build/vaadin-desktop-host-swift/host.out.log
```

For v1.51 StreamResource binary range verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_51-stream-resource-range .build/vaadin-desktop-host-swift/host.out.log
```

For v1.52 StreamResource sequential chunk verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_52-stream-resource-chunks .build/vaadin-desktop-host-swift/host.out.log
```

For v1.53 Flow Upload receiver verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_53-upload-receiver .build/vaadin-desktop-host-swift/host.out.log
```

For v1.54 Flow Upload multipart verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_MULTIPART=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_54-upload-multipart .build/vaadin-desktop-host-swift/host.out.log
```

For v1.55 Flow DownloadHandler save/open verification:

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_MULTIPART=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DOWNLOAD_SAVE_OPEN=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_55-download-save-open .build/vaadin-desktop-host-swift/host.out.log
```
