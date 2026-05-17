# Phase 0: In-memory Prototype

运行：

```bash
bash prototypes/phase0-phase1-spike/bin/run-inmemory-demo
```

验证目标：

```text
bootstrap -> button click -> Java listener -> UI state change -> patch output
```

当前实现用 `MockFlowRuntime` 代替真实 Vaadin Flow 内部类。这样做是有意的：仓库当前没有 Vaadin 源码或构建配置，第一步先固定 transport abstraction 的输入输出和验收形态。

