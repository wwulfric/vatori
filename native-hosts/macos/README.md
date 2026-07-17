# macOS Native Host

This directory contains the production AppKit/WKWebView host for vatori. It owns
the Java sidecar process, window lifecycle, standard Vaadin bundle loading,
opaque Flow IPC browser bridges and macOS capability execution.

The host reads one strict transport from bundled `VatoriHost.json`:

- `flow-ipc`: load the packaged standard Vaadin production bundle;
- `compatibility-http`: load the loopback URL from Java `ready`.

Both modes use `VFIPC ` framing and envelope version `1` for lifecycle and native `command`/`capabilityResult`
messages. Window identity, title and size come from the Java application
descriptor, not host CLI flags.

Build an application package through the CLI:

```bash
bash bin/vaadin-desktop package examples/counter-app
```

The app contains a runnable JAR, production webapp, capability manifest and
bundle-relative launch manifest. Swift never reads Maven, a source module or a
repository path. Output is `.build/native-hosts/macos/VaadinDesktopHost.app`. Repository-only
automation lives in `test-support/native-host-macos-acceptance` and is compiled
only for an explicit acceptance build.

See `docs/native-host.md` and `docs/ipc-protocol.md` for lifecycle and protocol
contracts.
