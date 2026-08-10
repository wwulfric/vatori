# macOS Native Host

This directory contains the production AppKit/WKWebView host for vatori. It owns
the Java sidecar process, window lifecycle, standard Vaadin bundle loading,
opaque Flow IPC browser bridges and macOS capability execution.

The host reads one strict transport from bundled `VatoriHost.json`:

- `flow-ipc`: load either the packaged standard Vaadin production bundle or a
  CLI-owned generic loopback development webapp;
- `compatibility-http`: load the loopback URL from Java `ready`.

The development webapp path is resource delivery only. Flow bootstrap, UIDL,
events and push still use the opaque IPC bridges, and the host does not interpret
Vite or HMR.

In development mode, separate versioned stdin control lines can request planned
replacement or rollback of the already-staged sidecar. Request-correlated result
lines on stdout let the CLI commit a ready candidate or restore the last ready
JAR after candidate failure. The host retains native windows and reloads their
WebViews after replacement or rollback `ready`; it does not know the Java source
tree, build tool or module.

Both modes use `VFIPC ` framing and envelope version `1` for lifecycle and native `command`/`capabilityResult`
messages. Window identity, title and size come from the Java application
descriptor, not host CLI flags.

Build an application package through the CLI:

```bash
bash bin/vaadin-desktop package examples/counter-app
```

The app contains a runnable JAR, production webapp, capability manifest,
application icon, optional application-owned resources and bundle-relative
launch manifest. `src/main/desktop/macos/AppIcon.icns` overrides the vatori
fallback icon; common and macOS resource layers are staged under
`Contents/Resources/Application`. Swift never reads Maven, a source module or a
repository path. Output is `.build/native-hosts/macos/VaadinDesktopHost.app`.
Repository-only automation lives in `test-support/native-host-macos-acceptance`
and is compiled only for an explicit acceptance build.

Development commands run the bundled host executable as a child process.
Normal final-window shutdown exits zero; fatal manifest, sidecar startup,
invalid-`ready` and recovery-exhaustion paths exit nonzero for the CLI to
propagate. `VatoriHost.json` also bounds the initial Java `ready` handshake so a
live but unresponsive sidecar fails before any window is created.

See `docs/native-host.md` and `docs/ipc-protocol.md` for lifecycle and protocol
contracts.
