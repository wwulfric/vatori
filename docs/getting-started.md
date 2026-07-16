# Getting Started

## Requirements

- JDK 21+
- Maven 3.9+
- macOS with `/usr/bin/swiftc`

Repository scripts initialize SDKMAN when needed and use `.m2/repository` as the
local Maven cache.

## Run Flow IPC

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

The command builds the application's standard Vaadin production webapp and
runnable JAR, packages the webapp into the macOS host, starts the Java sidecar and
opens WKWebView. Browser requests and push cross VFIPC1 opaquely; Vaadin's
standard client owns bootstrap, UIDL and rendering.

The launch fails if the production bundle, runnable sidecar or structured Java
`ready` descriptor is missing.

## Run Compatibility HTTP

```bash
bash bin/vaadin-desktop dev-compat examples/counter-app
```

The same macOS host starts the sidecar and loads its random loopback URL. Use
this mode for Servlet filters, servlet-backed security or other integrations
that require a normal web server. Native capability calls still use VFIPC1 and
return real host results.

## Package

```bash
bash bin/vaadin-desktop package examples/counter-app
```

The current command produces the application package output and development
macOS host app under `.build/native-hosts/macos`. Installer signing,
notarization and distribution are not complete.

## Create An Application

1. Create a Maven module.
2. Depend on `dev.vatori:vaadin-desktop-flow`.
3. Add `com.vaadin:vaadin-spring-boot-starter` and optional `vaadin-dev`.
4. Add a Spring Boot application class that calls `DesktopLauncher.launch(...)`.
5. Define ordinary Vaadin Flow views with `@Route`.

```java
@SpringBootApplication
public class MyDesktopApp {
    public static void main(String[] args) {
        DesktopLauncher.launch(MyDesktopApp.class,
                DesktopApplication.builder("My App")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("My App")
                                .size(1000, 720)
                                .build())
                        .build(),
                args);
    }
}
```

```java
@Route("")
public class MainView extends VerticalLayout {
    public MainView() {
        add(new Button("Click me", event -> add(new Span("Clicked"))));
    }
}
```

Application name, window id/title and dimensions must be valid because Java
publishes them in `ready` and the native host uses them to create the window.

## Verify

```bash
bash bin/verify-smoke
```

The suite checks repository boundaries, sidecar launcher lifecycle, request
handler integration, scope isolation, trace inspector contracts and browser
bridges. See `docs/acceptance-tools.md` for real-window automation.

## Current Limits

Flow IPC uses a production frontend build even for the development command.
Dev-mode/Vite over IPC, complete Flow product parity, installers and non-macOS
hosts remain open.
