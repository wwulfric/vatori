# Vaadin Desktop Swift Host Acceptance Support

This directory owns repository-only Swift host and WKWebView acceptance code.
It is included only when `bin/vaadin-desktop-build-swift-host` selects an
explicit acceptance profile.

- `Sources/VaadinDesktopHostAcceptanceSupport`: automation flags, DOM
  inspection, snapshots, scheduling and readiness checks compiled alongside
  the production host only for explicit acceptance builds.
- `Sources/VaadinDesktopHostAcceptanceSupport/FlowIpcStandardBundleFrameworkAcceptance.swift`:
  focused real-WKWebView workflow for the standard Vaadin production bundle.
- `Sources/VaadinDesktopHostAcceptanceSupport/FlowIpcStandardBundleSidecarRestartAcceptance.swift`:
  fault-injection workflow for bounded sidecar replacement, retained native
  window identity and recovered standard Flow request/push behavior.
- `Tests/FlowIpcOpaqueHttpBridgeSmoke.mjs` and `Tests/FlowIpcPushBridgeSmoke.mjs`:
  browser-side transport contract tests for the two production bridge scripts.

The default Swift host build must not compile or package any files from this
directory. Acceptance builds add only the selected automation sources; test
support is never copied into the application's Vaadin production webapp.
