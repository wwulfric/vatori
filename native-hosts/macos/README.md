# macOS Native Host

This directory contains the production AppKit/WKWebView host for vatori. It owns
the Java sidecar process, window lifecycle, standard Vaadin bundle loading,
opaque Flow IPC browser bridges and macOS capability execution.

The host accepts one strict transport from the CLI:

- `flow-ipc`: load the packaged standard Vaadin production bundle;
- `compatibility-http`: load the loopback URL from Java `ready`.

Both modes use VFIPC1 for lifecycle and native `command`/`capabilityResult`
messages. Window identity, title and size come from the Java application
descriptor, not host CLI flags.

Build directly:

```bash
bash bin/vaadin-desktop-build-macos-host \
  /Users/bytedance/IdeaProjects/vatori examples/counter-app flow-ipc
```

Output is `.build/native-hosts/macos/VaadinDesktopHost.app`. Repository-only
automation lives in `test-support/native-host-macos-acceptance` and is compiled
only for an explicit acceptance build.

See `docs/native-host.md` and `docs/ipc-protocol.md` for lifecycle and protocol
contracts.
