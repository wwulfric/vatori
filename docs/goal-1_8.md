# Goal 1.8: Generated/Lazy Frontend Execution Boundary

`docs/goal.md` 是长期愿景和完成度账本。v1.7 已完成的是 TextField production
`indexhtml` side-effect split：真实 `TextField` graph 已通过 no-localhost staged
graph 执行，且旧 `@vaadin/text-field` shim 只保留为 legacy fallback。

本阶段承接 v1.7 后仍存在的 frontend artifact blocker，把 generated/lazy resource
从“verified / staged attempt / strict blocker”推进到一个更真实的 no-localhost
execution boundary。

**核心目标：解决当前 `generated-flow-imports-*.js` staged graph 的相对依赖与
lazy execution 边界，使至少一个真实 generated/lazy artifact path 在 Flow IPC
trace 中进入可执行状态；若仍失败，blocker 必须从当前 `markdown-helpers`
relative dependency 层收敛到新的、更具体的 Flow client / Vaadin bootstrap /
runtime integration 层。**

## 继承边界

以下能力在 v1.8 中视为基线，不能回退：

- `dev-ipc` 继续启动 no-localhost Flow IPC smoke runtime，不通过 localhost HTTP URL
  加载核心 UI path。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者项目形态的 validation app。
- v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7 fixture gates 继续可运行。
- `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。
- Visible Flow client engine 继续负责 bootstrap、event patch、push 和 resync 的可见
  renderer。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、
  repeated/list update、desktop capability、file transfer、push ordering、resync 和
  WebView error-reporting baseline 继续可验证。
- v1.7 TextField graph execution 不能回退：真实 `vaadin-text-field` graph 通过 stable
  resource-path module identity 执行，`Vaadin TextField: Flow IPC typed` event path
  继续通过。
- 旧 TextField shim 仍只能作为 legacy fallback / regression guard，不能作为 v1.8
  成功证据。

## 当前 blocker

当前 generated/lazy graph 已能通过 manifest 表达和 IPC 请求验证：

- repository fixture resource：
  `META-INF/vatori/generated/flow/generated-flow-imports.js`
- real Vaadin dev-bundle chunk：
  `vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js`
- transitive helper：
  `vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js`

但 strict trace 仍记录：

- `frontendArtifactGeneratedLazyExecutionAttempted=true`
- `frontendArtifactGeneratedLazyExecutionResult=strict-blocker:...generated-flow-imports-VW7hRaNw.js`
- browser error：
  `Module name, './markdown-helpers-31I0A-hg.js' does not resolve to a valid URL.`
- `frontendArtifactGeneratedLazyLoadOnDemandResult=api-not-exposed-on-current-bootstrap;staged-import-attempted`

v1.8 要么消除这个 relative dependency blocker，要么证明它已经越过当前层级，进入
更具体的 Flow client lazy API / bootstrap / execution side-effect blocker。

## 实现范围

### 1. Generated/Lazy Dependency Resolver

目标：

- 为 generated/lazy artifact family 增加 relative dependency resolver。
- 支持 `generated-flow-imports-*.js` 中的 `./markdown-helpers-*.js` 等相对 import
  被 rewrite 到 stable staged module identity。
- 复用或扩展 v1.7 stable resource-path Blob module identity 策略，避免同一 helper
  以多个 Blob URL 重复执行。
- Trace 必须记录：
  - generated/lazy resolver version
  - source artifact
  - relative import specifier
  - resolved artifact path
  - staged URL scheme
  - cache hit/miss
  - cleanup policy
  - module identity key
  - execution status。

验收证据：

- `resourceStage` 中出现 generated/lazy dependency rewrite / resolution 记录。
- `markdown-helpers-31I0A-hg.js` 不再作为 unresolved relative URL 最终 blocker。
- `webview.dom` 汇总 generated/lazy staged dependency graph、executed / failed paths、
  failure layer 和 next step。

### 2. Lazy Execution Contract

目标：

- 明确当前 no-localhost runtime 如何触发 generated/lazy execution。
- 优先使用真实 Flow client engine 可接受的 lazy path；如果当前 `Flow.loadOnDemand`
  API 不暴露，需要建立受控 bridge / probe，并清楚标记它与最终 Flow lazy API 的距离。
- 不能把 repository-local inline shim 或假模块当作 generated/lazy 成功。
- 不能绕开真实 Vaadin artifact body。

可接受的两种结果：

1. **执行成功**
   - 至少一个真实 generated/lazy dev-bundle chunk 经 no-localhost staged graph 执行。
   - DOM 记录 `frontendArtifactGeneratedLazyExecuted=true` 或等价 v1.8 字段。
2. **blocker 前移**
   - relative dependency blocker 已解决。
   - 新 blocker 必须记录为 `blockerVersion=flow-ipc-generated-lazy-v1_8` 或等价字段。
   - `failureLayer` 不能仍是 unresolved `./markdown-helpers-*.js` relative URL。

### 3. Inspector Gate And Fixture

目标：

- 新增 inspector 参数：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_8 docs/traces/flow-ipc-v1_8.ndjson
```

- 保持以下命令继续通过：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_6 docs/traces/flow-ipc-v1_6.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_7 docs/traces/flow-ipc-v1_7.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

v1.8 gate 至少要求：

- no-localhost `transport=flow-ipc`。
- v1.7 TextField execution baseline。
- generated/lazy manifest metadata。
- generated/lazy relative dependency resolution evidence。
- `resourceStage` / `webview.dom` 中的 v1.8 generated/lazy execution or narrowed blocker
  evidence。
- old TextField shim not counted as success。
- existing Button / BigDecimalField / TextField / Checkbox / Select / list / capability
  baseline still passing。

交付：

- 新增 `docs/traces/flow-ipc-v1_8.ndjson`。
- 更新 `DesktopTraceInspector` 支持 `--require-flow-ipc-v1_8`。

### 4. Verification Apps

目标：

- `test-apps/flow-over-ipc-smoke-app` 继续作为 strict fixture source。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者形态验证。
- 只有在隔离 generated/lazy execution 更清晰时，才新增 focused validation app。

验证 app 必须保持：

- 普通 Flow Java API。
- Button baseline。
- BigDecimalField baseline。
- TextField baseline。
- Checkbox 和 Select baseline。
- Repeated/list update baseline。
- 至少一个 manifest-gated native capability call。

### 5. Documentation

更新或新增：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/goal.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md` 或新增 focused v1.8 protocol note
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`

文档必须明确：

- v1.7 与 v1.8 的差异。
- generated/lazy resolver 如何处理 relative imports。
- `Flow.loadOnDemand` 当前是被真实使用、被 bridge/probe 替代，还是仍是 blocker。
- 哪些 generated/lazy artifacts 已执行，哪些仍只是 verified-only。
- 如果仍有 blocker，它已经从当前 `markdown-helpers` relative URL 层进入哪个新层。

## 验收命令

最小验收集合：

```bash
git diff --check
bash bin/flow-client-resource-smoke
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_6 docs/traces/flow-ipc-v1_6.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_7 docs/traces/flow-ipc-v1_7.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_8 docs/traces/flow-ipc-v1_8.ndjson
```

真实窗口 smoke 验证：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

framework app 回归：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

## 明确非目标

- 不要求完整 production bundle execution。
- 不要求完整 dev-mode/Vite runtime。
- 不要求 broader component parity，例如 Grid、ComboBox、DatePicker、Dialog、Upload。
- 不要求 durable push queue、ack、reconnect 或 crash recovery。
- 不要求 Windows/Linux host。
- 不要求生产打包、签名、公证或自动更新。

这些仍保留在 `docs/goal.md` 的长期未完成账本中。
