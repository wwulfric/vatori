# Test Support

Repository-only acceptance infrastructure lives here and stays outside published
framework dependencies and the root Maven reactor.

- `native-host-macos-acceptance`: explicit WKWebView automation overlay.
- `vaadin-desktop-trace-inspector`: independent `HOST_PROTOCOL` gate inspector.

Production host builds and framework artifacts must not package these sources or
resources.
