# Getting Started

## Requirements

- JDK 21+
- Maven 3.9+
- macOS with `/usr/bin/swiftc`

Repository commands use `.m2/repository` as the local Maven cache.

## Run Pure Flow

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

The Java CLI builds one runnable sidecar JAR and the standard Vaadin production
webapp, stages both into a macOS app, and starts that app. WKWebView browser
requests and push cross `VFIPC ` framing opaquely; Vaadin's standard client owns
bootstrap, UIDL and rendering.

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

Spring applications support both commands:

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
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

## Verify

```bash
bash bin/verify-smoke
```

See `docs/acceptance-tools.md` for explicit real-window automation.
