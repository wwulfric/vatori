# Vaadin Desktop Smoke App

This independent validation app uses real Vaadin Flow views and public framework
APIs.

```bash
bash bin/vaadin-desktop dev-compat test-apps/vaadin-desktop-smoke-app
```

It covers basic route rendering, interaction, data display and a desktop API call
while the UI uses loopback HTTP. The macOS host still executes the native command
and returns its capability result over the shared sidecar channel.
