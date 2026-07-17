# vatori

vatori 是一个 Vaadin 桌面端开发框架。应用开发者继续使用 Java Vaadin Flow
编程模型；平台 native host 负责系统 WebView、原生能力、生命周期和打包边界。

主运行时加载应用自己的标准 Vaadin production bundle，并通过 IPC 将浏览器请求
原样交给 Vaadin request-handler chain。Host 和 bridge 不实现自定义 Flow client，
也不解析 UIDL。

## 快速开始

要求：

- JDK 21+
- Maven 3.9+
- 带 Swift toolchain 的 macOS

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

当应用需要 Servlet WebApplicationContext 时，使用可选 Spring Boot adapter：

```bash
bash bin/vaadin-desktop dev-compat test-apps/vaadin-desktop-smoke-app
```

构建应用和当前平台 host：

```bash
bash bin/vaadin-desktop package examples/counter-app
```

两种模式都会构建标准 production bundle。`dev` 将其暂存给 WKWebView 本地加载；
`dev-compat` 由 Spring 从随机 `127.0.0.1` URL 提供同一应用。两种模式都通过同一条
双向 sidecar channel 处理窗口生命周期和真实 native capability result。直接运行应用
JAR 而没有 native-host transport 会立即失败。

## 应用入口

默认 runtime 是纯 Vaadin Flow，应用显式注册 route：

```java
public class MyDesktopApp {
    public static void main(String[] args) {
        DesktopLauncher.launch(
                DesktopApplication.builder("My App").build(),
                DesktopFlowApplication.builder(MainView.class).build(),
                args);
    }
}
```

`DesktopApplication` 提供 native host 创建主窗口所需的 metadata、id、title 和
size。页面仍然是普通 Vaadin `@Route` 组件。Spring 应用额外依赖
`vaadin-desktop-spring-boot` 并使用 `SpringDesktopLauncher`，只有这类应用可以使用
`dev-compat`。

## 验证

运行集中式非窗口检查：

```bash
bash bin/verify-smoke
```

具体子检查以脚本为准。真实窗口自动化、trace inspector 和生成产物规则见
[Acceptance Tools](docs/acceptance-tools.md)。

当前 validation surface 不能等同于完整 Vaadin 产品级 parity。当前边界维护在
[Flow over IPC Runtime](docs/flow-over-ipc-runtime.md)，长期工作维护在
[Roadmap](docs/roadmap.md)。

## 仓库

稳定心智模型和代码放置规则见
[仓库结构与心智模型](docs/repository-structure.md)。主要所有权划分是：

- `vaadin-desktop-core`：公开 desktop application 和 capability API。
- `vaadin-desktop-ipc`：sidecar envelope 和序列化基础设施。
- `vaadin-desktop-flow`：纯 Vaadin sidecar runtime 和 transport adapter。
- `vaadin-desktop-spring-boot`：可选 Spring DI 和 compatibility HTTP adapter。
- `native-hosts/macos`：production AppKit/WKWebView host。
- `examples`：可读的开发者应用。
- `test-apps`：确定性的验证应用。
- `test-support`：仅供仓库验收使用的自动化和 inspector。

## 文档

- [Getting Started](docs/getting-started.md)
- [仓库结构与心智模型](docs/repository-structure.md)
- [Framework Architecture](docs/framework-architecture.md)
- [Flow Integration](docs/flow-integration.md)
- [Flow over IPC Runtime](docs/flow-over-ipc-runtime.md)
- [Flow Transport Adapter](docs/flow-transport-adapter.md)
- [Sidecar IPC Protocol](docs/ipc-protocol.md)
- [Native Host](docs/native-host.md)
- [Acceptance Tools](docs/acceptance-tools.md)
- [Repository Hygiene](docs/repository-hygiene.md)
- [Roadmap](docs/roadmap.md)
