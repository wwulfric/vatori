# Desktop Host Spike

当前 host 是 headless/mock host，源码位于：

- `prototypes/phase0-phase1-spike/src/main/java/dev/vatori/flowipc/host/MockHostMain.java`

运行：

```bash
bash prototypes/phase0-phase1-spike/bin/run-ipc-demo
```

它模拟真实 desktop host 的职责：

- 启动 Java sidecar 进程。
- 发送 `bootstrap` 消息。
- 模拟 WebView DOM event。
- 接收 Java 返回的 patch。
- 应用 patch 到 mock WebView DOM。
- 模拟 reload 后的 `resync`。
- 捕获 sidecar stderr trace。

后续真实 host 替换路径：

1. 保留 stdio JSON-RPC envelope。
2. 用 Tauri 或最薄系统 WebView host 替换 `MockHostMain`。
3. WebView JS bridge 负责把 DOM event 转为同样的 `event` envelope。
4. Java sidecar 保持相同协议入口。

