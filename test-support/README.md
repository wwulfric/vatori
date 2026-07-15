# Test Support

This directory contains repository-only test and acceptance infrastructure. Its
contents may be used by smoke apps and validation commands, but must not become
runtime dependencies or packaged resources of the published framework.

- `vaadin-desktop-host-swift-acceptance`: Swift and WKWebView automation support
  for explicit native-host acceptance profiles.
- `vaadin-desktop-trace-inspector`: repository-only trace gates used by smoke
  and acceptance commands; it is deliberately outside the framework reactor.
