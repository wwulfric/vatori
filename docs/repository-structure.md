# 仓库结构与心智模型

本文解释 vatori 仓库中每类代码的所有权、依赖方向和发布边界。它回答的不是
“当前做到哪一步”，而是“某段代码为什么属于这里，以及新增代码应该放在哪里”。

## 一条主链路

vatori 同时包含 Java 框架、macOS 原生 host、开发者示例和仓库验收设施。理解目录
之前，先记住两条运行路径：

```text
Vaadin 应用调用桌面能力
  -> vaadin-desktop-core
  -> Java native bridge / host protocol
  -> vaadin-desktop-host-swift
  -> AppKit、WKWebView 和 macOS 能力

WKWebView 运行 Vaadin UI
  -> 标准 Vaadin production bundle
  -> production request / push bridge
  -> vaadin-desktop-host-swift
  -> VFIPC1
  -> vaadin-desktop-flow 中的 Vaadin request-handler chain
```

第一条链路处理窗口、剪贴板、通知和文件对话框等桌面命令。第二条链路处理
bootstrap、UIDL、资源请求和 push。`vaadin-desktop-ipc` 为这些跨进程通信提供通用
envelope 和 trace primitive，但不解释具体 UI 组件。

## 框架模块

所有顶层 `vaadin-desktop-*` 目录都是框架所有。它们可以有正常的 `src/test`，但
`src/main`、Swift `Sources` 和 production `Resources` 只能包含可发布框架能力或
通用扩展点。

| 目录 | 责任 | 不应包含 |
| --- | --- | --- |
| `vaadin-desktop-core` | 开发者直接使用的 Java desktop API，例如窗口、剪贴板、通知、文件对话框和文件传输 | AppKit 实现、Vaadin transport、测试路径 |
| `vaadin-desktop-ipc` | IPC envelope、JSON 和 trace primitive | Flow 组件语义、具体 host UI |
| `vaadin-desktop-host` | Java host abstraction、host 选择、compatibility host 和 native bridge support | 示例应用、验收 fixture、macOS 主线自动化 |
| `vaadin-desktop-flow` | Vaadin/Spring launcher、标准 bundle runtime、opaque request/push adapter 和 Flow session 生命周期 | 自定义 Flow client renderer、测试应用组件矩阵 |
| `vaadin-desktop-host-swift` | macOS AppKit/WKWebView host、sidecar 生命周期、production bridge 和原生能力执行 | acceptance flags、截图脚本、故障注入 workflow |
| `vaadin-desktop-cli` | 可发布 CLI artifact 的入口 | 仓库示例的硬编码路径、smoke orchestration |

Java 模块的主要依赖方向是：

```text
core <- ipc <- host
  ^       ^      ^
  +-------+------+- flow
```

`core` 保持最小公开契约；`ipc` 可以表达 core command；`host` 使用 core 和 ipc；
`flow` 负责把 Vaadin runtime 接到这几层。框架模块不得依赖 `examples`、`test-apps`
或 `test-support`。

## 示例与验证应用

`examples` 和 `test-apps` 都是应用，但面向的读者不同。

| 目录 | 用途 |
| --- | --- |
| `examples/counter-app` | 最小、易读的框架接入故事 |
| `examples/grid-app` | Grid 和数据组件示例 |
| `examples/desktop-capabilities-app` | 桌面 capability API 和 manifest 示例 |
| `examples/component-gallery-app` | 面向开发者阅读的组件展示 |
| `test-apps/vaadin-desktop-smoke-app` | compatibility 启动和基础 host 验证 |
| `test-apps/flow-over-ipc-smoke-app` | 小型 no-localhost IPC 闭环验证 |
| `test-apps/flow-over-ipc-framework-app` | 更广的 Flow 组件和 native capability 验收面 |

`examples` 应优先可读、简洁，并像外部项目一样只使用公开 API。`test-apps` 可以使用
稳定 DOM id、明确状态文字和确定性操作，以便自动化检查。二者都只能依赖框架，不能
被框架反向引用。

## 仓库测试支持

`test-support` 保存不应发布给框架用户、也不应进入默认 runtime 的验收实现。

| 目录 | 用途 |
| --- | --- |
| `test-support/vaadin-desktop-host-swift-acceptance` | 显式 acceptance profile 使用的 Swift/WKWebView 自动化、DOM 检查、截图、多窗口和 sidecar recovery workflow |
| `test-support/vaadin-desktop-trace-inspector` | 读取 host trace 并执行仓库验收 gate 的独立工具 |

默认 Swift host 使用 production hook factory。只有显式 acceptance build 才用
test-support factory 替换它并加入自动化源码。trace inspector 不属于根 Maven
reactor，也不能成为任何 framework artifact 的依赖。

## 仓库配套目录

| 目录 | 用途 |
| --- | --- |
| `bin` | 框架启动、Swift host 构建、preflight、smoke、trace 和 hygiene 命令 |
| `docs` | 稳定架构、协议、runtime 边界、验收工具和路线图 |
| `assets` | 品牌源文件和正式导出资产 |
| `.agents` | 仓库本地 agent skills 和长期工作约束 |

这些目录服务开发和维护，但不是 Java/Swift runtime artifact。`bin/vaadin-desktop`
是开发者入口；其他 smoke、inspector 和 hygiene 命令主要服务仓库验证。

## 本地生成目录

`.build`、`.m2` 和各模块的 `target` 都是本地生成物：

- `.build`：Swift app、frontend bundle、日志和截图。
- `.m2`：项目本地 Maven repository。
- `target`：Maven 编译、测试和打包输出。

它们不表达架构所有权，不应加入版本管理。即使它们出现在某个源码目录旁边，也不能
据此判断该目录属于框架或测试。

## 新代码放置判断

新增内容时按以下顺序判断：

1. 外部应用开发者是否需要调用它？需要则优先放 `vaadin-desktop-core`。
2. 它是否只定义跨进程消息和序列化？是则放 `vaadin-desktop-ipc`。
3. 它是否负责 Java host 选择或 native bridge？是则放 `vaadin-desktop-host`。
4. 它是否依赖 Vaadin request、session、bundle 或 push？是则放 `vaadin-desktop-flow`。
5. 它是否直接操作 AppKit、WKWebView 或 Java sidecar 进程？是则放 `vaadin-desktop-host-swift`。
6. 它是否只为自动化、故障注入或验收 gate 服务？是则放 `test-support`。
7. 它是否是可读的用户故事？是则放 `examples`；若以机器断言为主，则放 `test-apps`。

无法明确归类通常意味着接口边界还没有想清楚，不应先把代码塞进最方便的模块。
