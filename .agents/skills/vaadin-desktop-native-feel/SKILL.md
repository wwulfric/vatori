---
name: vaadin-desktop-native-feel
description: Use when designing, implementing, or reviewing Vaadin Desktop Framework apps that must feel like real desktop apps rather than web pages in a WebView. Trigger for Vaadin Flow desktop UI, Swift/AppKit host, WKWebView/WebView2 behavior, native windowing, menu/tray/clipboard/file-dialog/notification capabilities, Flow over IPC, compatibility-mode boundaries, desktop polish, native-feel audits, and "looks web-y" feedback. Do not use for ordinary browser-only Vaadin web apps with no desktop/native-feel requirement.
---

# Vaadin Desktop Native Feel

This skill adapts native-feel desktop architecture guidance to this repository's stack:

```text
Swift/AppKit or platform Native Host
  -> system WebView: WKWebView / WebView2 / WebKitGTK
  -> Vaadin Flow client engine as renderer
  -> Java Flow runtime / sidecar
  -> IPC + Native Capability Gateway
```

The rule of thumb: **the host owns native behavior, Vaadin owns the shared Java UI model, IPC owns cross-boundary contracts**. Do not let DOM/CSS imitate capabilities the OS should provide.

Reference basis: adapted for this project from the ideas in `yetone/native-feel-skill`, especially native-host ownership, system WebView discipline, intentional IPC and native-readiness auditing.

## Workflow

1. Read `AGENTS.md` and the relevant docs in `docs/` before changing architecture.
2. Classify the work surface:
   - Host/window/lifecycle/capability work -> read `references/architecture.md`.
   - WebView rendering, flicker, throttling, focus, IME, scroll, context menu -> read `references/webview-survival.md`.
   - Vaadin Flow UI/component/theme decisions -> read `references/vaadin-flow-ui.md`.
   - "Does this feel native enough?" -> run `checklists/native-readiness.md`.
3. Prefer local source references before web search:
   - Tauri: `/Users/bytedance/RustroverProjects/study/tauri` for process-model comparison only
   - Vaadin: `/Users/bytedance/IdeaProjects/study/vaadin`
4. Keep compatibility mode explicit. `dev-compat` loads `127.0.0.1` through the same native host and capability channel, but it is not Flow over IPC.
5. Before claiming success, verify with a running desktop app, not only a browser tab or unit test.

## Tenets For This Stack

1. **Native below the WebView.** Window shape, focus, materials, menu bar, tray, file dialogs, notifications, clipboard, drag/drop, shortcuts, accessibility and lifecycle belong to Swift/AppKit or platform code.
2. **Vaadin above the transport.** Developers should still write ordinary Flow views: `@Route`, `Button`, `Grid`, layouts and listeners.
3. **Transport is a contract.** Every host/WebView/Java crossing must be typed, traced, async where possible and low-frequency by design.
4. **Compatibility is a named adapter.** Do not hide Servlet/HTTP/WebSocket assumptions. Isolate them and document the remaining gap.
5. **Native feel is behavior first.** Menus, focus, keyboard navigation, window lifecycle, startup paint, IME and system gestures matter more than visual theming.
6. **Use the OS instead of copying it.** Native dialogs, notifications, clipboard, app menus, window shadows and system materials should come from the host.
7. **Optimize perceived latency.** Button clicks, list filtering, first paint, window show/hide and command palette input are the primary performance surfaces.
8. **Keep developer ergonomics.** The framework is only worth it if a Java/Vaadin developer can build a desktop app without hand-writing host IPC for every feature.

## Native-Feel Review Output

When reviewing or proposing changes:

- Name the layer affected: host, WebView, Flow UI, Java runtime, IPC, packaging.
- State whether the current path is final architecture or compatibility mode.
- For each recommendation, state the trade-off: complexity, platform-specific code, startup cost, memory, or delayed Flow-over-IPC work.
- If the app still feels web-y, cite concrete symptoms: focus, cursor, context menu, window lifecycle, dialogs, scrolling, first paint, or keyboard behavior.
- For implementation tasks, include a verification step using `test-apps/vaadin-desktop-smoke-app` or another real desktop app.

## Local Commands

Compile:

```bash
mvn -o -Dmaven.repo.local="$PWD/.m2/repository" -DskipTests compile
```

Run smoke app:

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
```

Run the localhost compatibility transport through the same native host:

```bash
bash bin/vaadin-desktop dev-compat test-apps/vaadin-desktop-smoke-app
```
