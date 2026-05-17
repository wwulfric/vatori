# Flow Transport SPI 草案

SPI 源码位于：

- `prototypes/phase0-phase1-spike/src/main/java/dev/vatori/flowipc/spi/FlowTransport.java`

当前最小契约覆盖：

- `bootstrap`：创建 session/ui scope，并返回初始 UI patch。
- `dispatch`：接收 client event，执行 Java listener，返回 UI patch。
- `resync`：WebView reload 或 host 恢复时重建完整 UI patch。
- `push`：预留后台线程向 UI 推送的扩展点，当前原型未实现。

这不是最终 Vaadin API，而是用于验证 Flow 的传输层最小可抽象边界。

