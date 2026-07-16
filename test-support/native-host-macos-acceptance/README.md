# macOS Native Host Acceptance Support

This directory contains repository-only WKWebView automation, DOM inspection,
snapshots, multi-window checks and sidecar recovery fault injection.

`bin/vaadin-desktop-build-macos-host` compiles this overlay only for an explicit
acceptance run. The default host never compiles or packages it.

`Tests/flow-ipc-opaque-http-bridge-smoke.mjs` and
`Tests/flow-ipc-push-bridge-smoke.mjs` verify the production browser bridge
scripts without adding code to an application production bundle.
