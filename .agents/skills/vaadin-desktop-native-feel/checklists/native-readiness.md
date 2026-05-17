# Vaadin Desktop Native-Readiness Audit

Use before claiming an app or PR improves native feel. Mark each item:

- `green`: verified in a running desktop app.
- `red`: failed or missing.
- `gray`: not applicable.

## Launch And Lifecycle

1. App opens a desktop window, not only a browser tab or printed URL.
2. Window close terminates or backgrounds the Java process intentionally.
3. No orphan Java sidecar remains after normal quit.
4. First paint does not show a white/black WebView frame.
5. App title, identifier and icon are host-owned, not only HTML metadata.
6. Startup trace records host launch, Java ready, URL/asset loaded and window shown.
7. Compatibility HTTP, if present, is labeled in trace and docs.

## Window And Host Behavior

8. Native close/minimize/zoom behavior works.
9. Window size and position are stable or intentionally restored.
10. Multi-monitor behavior is considered for launcher/settings windows.
11. Menu/tray/Dock/Taskbar behavior is owned by host.
12. Settings, file dialogs and save panels use native host APIs where expected.
13. Notifications are native for system-level events.
14. Clipboard supports expected formats, not only plain text if rich content is needed.

## Input And Interaction

15. Initial focus is correct.
16. Escape has a predictable action.
17. Keyboard navigation works for main workflows.
18. Common shortcuts do not conflict with text editing.
19. Chinese Pinyin IME is tested in text inputs.
20. Rows, tabs and command items do not use web-style pointer cursor.
21. Static labels and toolbar text are not accidentally selectable.
22. Browser context menu does not leak into app chrome.

## Vaadin UI

23. First screen is the product workspace, not a landing page.
24. Layout density matches desktop productivity software.
25. Toolbars and sidebars have stable dimensions.
26. `Grid` or list virtualization scrolls smoothly.
27. Button/list interactions give feedback within 200 ms.
28. No unnecessary route fade/page transitions.
29. Dialogs are used only where Flow overlay is appropriate.
30. Native capability actions go through `Desktop.current()`.

## Visual Fit

31. System font is used for app chrome.
32. Dark/light mode follows the OS or has a documented gap.
33. Accent color is not a hardcoded one-note palette.
34. OS window shadow/corners are not faked in CSS.
35. Scrollbars feel platform-appropriate.
36. Motion respects reduced-motion preferences where applicable.
37. Text does not overflow buttons, tabs, sidebars or grid cells.

## Performance And IPC

38. Idle background CPU is measured after hiding/minimizing.
39. Resident memory baseline is classified as WebView/Java baseline vs app margin.
40. High-frequency UI events are batched/throttled.
41. IPC/host calls are traceable with type, correlationId/session/window context.
42. Grid/list data fetch frequency is understood.
43. Push/resync/reload behavior is visible in logs.
44. Host bridge failures become user-visible errors, not silent console logs.

## Cross-Platform Readiness

45. macOS behavior is not assumed to generalize to Windows.
46. Swift/AppKit host design leaves a Windows WebView2 host path.
47. Host protocol versioning is explicit.
48. Native capability differences are represented in API/result types.
49. Packaging/updater gaps are listed if the work claims app readiness.
50. The independent smoke app verifies the path, not only framework examples.
