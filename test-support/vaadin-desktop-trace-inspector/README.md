# Vaadin Desktop Trace Inspector Test Support

This repository-only module validates traces emitted by explicit Flow IPC
acceptance runs. It is not a framework runtime or published developer-tool
dependency and is therefore not part of the root Maven reactor.

Use `bin/flow-ipc-trace-inspect` and
`bin/trace-inspector-acceptance-gate-smoke` instead of invoking the module
directly.
