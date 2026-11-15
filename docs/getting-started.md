# Getting Started

## Requirements

- JDK 21+
- Maven 3.9+
- macOS with `/usr/bin/swiftc`

Set `JAVA_HOME` to your JDK and make `java`, `javac`, and `mvn` available on
`PATH`. Repository commands use `.m2/repository` as the local Maven cache.

## Run Pure Flow

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

The Java CLI builds one runnable sidecar JAR and the standard Vaadin production
webapp, stages both into a macOS app, and starts that app. WKWebView browser
requests and push cross `VFIPC ` framing opaquely; Vaadin's standard client owns
bootstrap, UIDL and rendering.

The command remains attached to the native host. Closing the final window
returns zero; fatal host or sidecar startup failures return nonzero and retain
their stdout/stderr under `.build/native-hosts/macos`.

## Run With Frontend HMR

```bash
bash bin/vaadin-desktop dev-vite examples/counter-app
```

The CLI prepares the standard Vaadin Vite workspace, starts Vite on a random
loopback port and keeps it alive with the native host. Vite serves frontend
modules and HMR only; Flow browser requests and push still cross opaque IPC.
Stopping the app also stops Vite and removes files that this session generated
without deleting pre-existing frontend workspace files. The CLI consumes the
building Maven session's selected effective reactor model, so active-profile
modules, effective compile source/resource roots, local POMs and root `.mvn`
configuration trigger a rebuild. Build-configuration changes also refresh the
standard Vaadin Vite workspace. Roots introduced only by lifecycle plugins
after Maven's projects-read phase are not yet observed. A failed build keeps the
current Java process
running; a successful build atomically stages the new JAR, replaces the sidecar
and reloads the retained native window with a fresh Flow session. The last ready
JAR remains available until the candidate reaches `ready`; candidate startup,
early-exit or readiness failure restores that JAR and reloads the same window
through the stable runtime.

This is rebuild-driven process replacement. It does not preserve process-local
UI state and does not provide JVM in-place class hot replacement or the complete
upstream Servlet-based DevTools server lifecycle.

## Create A Pure Flow Application

Depend on `vaadin-desktop-flow`, `flow-html-components` and the component modules
the application actually uses. Do not add Spring or `vaadin-dev` to the default
runtime. The application build must produce exactly one JAR with `Main-Class`;
repository examples use Maven Shade for this contract.

```java
public class MyDesktopApp {
    public static void main(String[] args) {
        DesktopLauncher.launch(
                DesktopApplication.builder("My App")
                        .mainWindow(DesktopWindow.builder("main")
                                .title("My App")
                                .size(1000, 720)
                                .build())
                        .build(),
                DesktopFlowApplication.builder(
                        MainView.class, DetailsView.class).build(),
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

Routes are explicit, exactly one route is rooted at `""`, and the default
instantiator requires constructible route classes. Applications needing another
DI mechanism can provide an `InstantiatorFactory`.

## Use Spring Boot

Add `vaadin-desktop-spring-boot` and use `SpringDesktopLauncher` when Spring DI,
Spring route scanning or a Servlet web application is required:

```java
@SpringBootApplication
public class SpringDesktopApp {
    public static void main(String[] args) {
        SpringDesktopLauncher.launch(
                SpringDesktopApp.class,
                DesktopApplication.builder("Spring App").build(),
                args);
    }
}
```

Spring applications support these commands:

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
bash bin/vaadin-desktop dev-vite test-apps/vaadin-desktop-smoke-app
bash bin/vaadin-desktop dev-compat test-apps/vaadin-desktop-smoke-app
```

`dev-compat` builds the production frontend, then loads it from a random loopback
URL through Spring. It preserves Servlet filters, web security and WebSocket
behavior. Pure Flow launchers reject this transport.

## Package

```bash
bash bin/vaadin-desktop package examples/counter-app
```

Output is `.build/native-hosts/macos/VaadinDesktopHost.app`. It contains
`Sidecar/application.jar`, `FlowWebApp`, the capability manifest and
`VatoriHost.json`; it does not depend on the source repository after staging.
Embedded JREs, installer signing and notarization are not complete.

## Application Branding And Resources

Application-owned desktop assets use this optional layout:

```text
src/main/desktop/
  resources/                 common bundle resources
  macos/
    AppIcon.icns             macOS application icon
    resources/               macOS-specific bundle resources
```

The macOS builder uses `AppIcon.icns` when present and otherwise falls back to
the vatori icon. It copies common resources first and macOS resources second to
`Contents/Resources/Application`, so a platform-specific file can override a
common file with the same relative path. Resource trees must be self-contained
and cannot contain symlinks.

These files are native bundle resources, distinct from Java classpath files in
`src/main/resources` and Vaadin frontend resources. Access a bundled file after
the desktop context is available:

```java
Path license = Desktop.current().resources().resolve("licenses/license.txt");
try (InputStream input = Desktop.current().resources().open("models/model.bin")) {
    // Read the packaged application resource.
}
```

`DesktopResources` rejects absolute paths and traversal. Bundle resources are
read-only application inputs; mutable application data belongs in an OS-owned
application support directory instead.

## Verify

```bash
bash bin/verify-smoke
```

See `docs/acceptance-tools.md` for explicit real-window automation.
