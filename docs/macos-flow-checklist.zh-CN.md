# macOS 纯 Flow 桌面 GUI 接入清单

- [ ] **准备环境。** 此基线仅支持 macOS。确认有 `/usr/bin/swiftc`、JDK 21+ 和 Maven 3.9+，将 `JAVA_HOME` 指向该 JDK，并确保 `java`、`javac`、`mvn` 在 `PATH` 中。以下命令均在 vatori 仓库根目录运行。
- [ ] **确认源码基线。** 在干净检出中运行 `git rev-parse HEAD`，核对结果为 `1f23c44c2d0fc273ce152d6846a91dd1f6c2fdbd`，并确认 `git status --porcelain` 输出为空。这两项记录标识待构建的源码。
- [ ] **运行示例。** 执行 `bash bin/vaadin-desktop dev examples/counter-app`。在原生窗口中点击 **Increment**，确认计数增加，关闭窗口并确认命令成功退出。
- [ ] **验证打包。** 执行 `bash bin/vaadin-desktop package examples/counter-app`，再执行 `open .build/native-hosts/macos/VaadinDesktopHost.app`。在打包后的应用中复验计数并关闭窗口。
- [ ] **核实下游实际使用的框架。** 保存从上述干净提交构建框架的日志，记录生成的框架 JAR 坐标及 SHA-256（macOS 可用 `shasum -a 256 文件名`）。从下游应用的实际打包日志核对解析到的框架 JAR 路径，在打包时对这些输入文件计算并比对摘要；保留该次输出的应用 JAR 摘要。若框架与应用在同一 Maven reactor 构建，保留显示框架模块从该检出构建并参与应用打包的日志。仅查看框架 checkout 的 HEAD 或 POM 中的 `0.1.0-SNAPSHOT` 无法确认下游消费了哪次构建。
