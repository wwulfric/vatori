# WebView Survival Notes

Use this when WKWebView/WebView2 behavior makes the app feel like a webpage or when host work touches rendering lifecycle.

## First Paint

Native-feeling desktop apps should not show a blank WebView frame.

- Keep the host window hidden or transparent until the first meaningful paint.
- Use a host-controlled background color that matches the Vaadin shell theme.
- Trace timings: Java started, Flow ready, URL loaded, DOM ready, first paint, window shown.
- Avoid showing generic loading text during normal warm launch.

Current v0 gap: `SystemWebViewDesktopHost` opens the URL directly. The Swift native host should add an explicit ready/first-paint handshake.

## Hidden Window Throttling

Launcher-style apps need prewarmed windows. WKWebView/WebView2 may throttle timers and animation when hidden or occluded.

- Prefer host-managed prewarm over Flow-side polling.
- Keep background CPU near zero while hidden.
- On re-show, verify the first keystroke and first animation do not hitch.
- Do not create cross-boundary polling loops to fight throttling.

## Resize And Layout

Expanding desktop panels should not expose blank areas.

- Host should own window size and animation.
- WebView should avoid resize churn during animated window changes.
- Vaadin content should use stable dimensions for toolbars, grids, command rows and side panels.
- Avoid route/page transitions for desktop tool surfaces.

## Context Menu, Cursor, Selection

The WebView default is browser-like; desktop chrome should not feel like a webpage.

- Disable browser context menus where the app has no native menu.
- Provide native context menus for desktop objects when needed.
- Avoid `cursor: pointer` on rows, tabs, tree items and command palette entries.
- Disable text selection on labels, button text, toolbar text and static chrome.
- Keep text selection enabled in text inputs, editors and user content.

## Input, Focus, IME

Desktop polish breaks fastest in input handling.

- Initial focus should land in the most likely control.
- Escape should close popovers, cancel edits or dismiss the window depending on context.
- Tab/Shift-Tab/Enter/Space must work without a mouse.
- Test Chinese Pinyin IME, Japanese kana and Korean Hangul in Flow text fields.
- Host shortcuts should not steal text-editing shortcuts from Vaadin components.

## Scrolling

- Let the platform draw scrollbars when possible.
- Avoid JavaScript smooth scrolling in app chrome.
- Preserve scroll position across view changes within the same window.
- Grid/list virtualization must not hitch when scrolling with trackpad inertia.

## Verification Checklist

For any WebView host change, run a real desktop app and check:

- no visible blank first frame,
- window closes the Java process,
- typing works immediately,
- IME composition works,
- WebView context menu does not leak,
- Grid/list scrolls smoothly,
- hidden/minimized CPU is low.
