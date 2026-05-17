# Phase 0/1 Spike

这是当前仓库的可运行实现。

## Run

```bash
bash prototypes/phase0-phase1-spike/bin/run-inmemory-demo
bash prototypes/phase0-phase1-spike/bin/run-ipc-demo
```

## Code Map

- `spi/FlowTransport.java`：transport-neutral SPI 草案。
- `runtime/CounterView.java`：按钮点击更新计数的 Java view/listener。
- `runtime/OrdersGridView.java`：简化列表数据请求视图。
- `runtime/MockFlowRuntime.java`：Flow-like Java UI runtime，维护 session/ui state、listener 和 patch。
- `demo/InMemoryDemoMain.java`：Phase 0 内存闭环。
- `ipc/IpcEnvelope.java`、`ipc/IpcCodec.java`：Phase 1 protocol v0。
- `sidecar/SidecarMain.java`：stdio JSON-RPC Java sidecar。
- `host/MockHostMain.java`：headless desktop host/webview bridge。
- `json/Json.java`：无外部依赖的最小 JSON codec。
- `traces/ipc-trace-sample.ndjson`：协议 trace 示例。
