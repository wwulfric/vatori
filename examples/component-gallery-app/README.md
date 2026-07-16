# vatori Component Gallery Example

This example is a developer-facing gallery for checking how common Vaadin Flow
components render and behave inside a vatori desktop application window.

Run it through the standard Flow IPC runtime:

```bash
bash bin/vaadin-desktop dev examples/component-gallery-app
```

Use the loopback compatibility transport when required:

```bash
bash bin/vaadin-desktop dev-compat examples/component-gallery-app
```

Coverage is intentionally practical rather than exhaustive. The app includes
major zero-config components across inputs, choices, data display, feedback,
navigation, layouts, content, upload, generated downloads, app shell/login and
rich content. Heavy, commercial, data-source-heavy or packaging-specific
components remain visible as `尚未实现` placeholders instead of being forced
into the first sample.
