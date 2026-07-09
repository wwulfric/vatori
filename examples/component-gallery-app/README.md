# vatori Component Gallery Example

This example is a developer-facing gallery for checking how common Vaadin Flow
components render and behave inside a vatori local desktop application window.

Run it in the current compatibility desktop host:

```bash
bash bin/vaadin-desktop dev examples/component-gallery-app
```

Or run it without a visible window:

```bash
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/component-gallery-app
```

Coverage is intentionally practical rather than exhaustive. The app includes
major zero-config components across inputs, choices, data display, feedback,
navigation, layouts, content, upload, generated downloads, app shell/login and
rich content. Heavy, commercial, data-source-heavy or packaging-specific
components remain visible as `尚未实现` placeholders instead of being forced
into the first sample.
