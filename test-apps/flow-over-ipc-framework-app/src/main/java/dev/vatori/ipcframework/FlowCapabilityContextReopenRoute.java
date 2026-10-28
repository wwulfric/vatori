package dev.vatori.ipcframework;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import dev.vatori.desktop.Desktop;
import dev.vatori.desktop.DesktopCapabilityContext;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.DesktopWindow;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Route("flow-capability-context-reopen")
public final class FlowCapabilityContextReopenRoute extends VerticalLayout {
    static final String SECONDARY_WINDOW_ID = "capability-reopen-secondary";
    static final String FIRST_TITLE = "Flow capability context incarnation A";
    static final String REOPENED_TITLE = "Flow capability context incarnation B";
    static final String THIRD_TITLE = "Flow capability context incarnation C";
    static final String STALE_BEFORE_TITLE =
            "Flow capability context stale before reopen";
    static final String STALE_AFTER_TITLE =
            "Flow capability context stale after reopen";
    static final String FIRST_STALE_BEFORE_THIRD_TITLE =
            "Flow capability context first stale before third";
    static final String REOPENED_STALE_BEFORE_THIRD_TITLE =
            "Flow capability context reopened stale before third";
    static final String FIRST_STALE_AFTER_THIRD_TITLE =
            "Flow capability context first stale after third";
    static final String REOPENED_STALE_AFTER_THIRD_TITLE =
            "Flow capability context reopened stale after third";

    private static final AtomicReference<DesktopCapabilityContext>
            FIRST_CAPABILITY_CONTEXT = new AtomicReference<>();
    private static final AtomicReference<DesktopCapabilityContext>
            REOPENED_CAPABILITY_CONTEXT = new AtomicReference<>();

    private final AtomicInteger windowChangeCount = new AtomicInteger();
    private final AtomicInteger windowOpenCount = new AtomicInteger();
    private final AtomicInteger firstChangeCount = new AtomicInteger();
    private final AtomicInteger reopenChangeCount = new AtomicInteger();
    private final AtomicInteger thirdChangeCount = new AtomicInteger();

    public FlowCapabilityContextReopenRoute() {
        setId("framework-flow-capability-context-reopen-route");
        setPadding(true);
        setSpacing(true);

        Span windowStatus = new Span(
                "Capability context reopen window: pending");
        windowStatus.setId(
                "framework-flow-capability-context-reopen-window-status");
        Button openWindow = new Button(
                "Open capability context window", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .open(DesktopWindow.builder(SECONDARY_WINDOW_ID)
                                    .title("Flow capability context secondary")
                                    .size(760, 520)
                                    .route("flow-capability-context-reopen")
                                    .build());
                    windowStatus.setText(
                            "Capability context reopen window: opened="
                                    + result.success()
                                    + " open=" + windowOpenCount.incrementAndGet()
                                    + " client=" + event.isFromClient()
                                    + " error=" + errorValue(result)
                                    + " change="
                                    + windowChangeCount.incrementAndGet());
                });
        openWindow.setId(
                "framework-flow-capability-context-reopen-open-window");

        Button closeWindow = new Button(
                "Close capability context window", event -> {
                    DesktopCommandResult result = Desktop.current().windows()
                            .close(SECONDARY_WINDOW_ID);
                    windowStatus.setText(
                            "Capability context reopen window: closing="
                                    + result.success()
                                    + " client=" + event.isFromClient()
                                    + " error=" + errorValue(result)
                                    + " change="
                                    + windowChangeCount.incrementAndGet());
                });
        closeWindow.setId(
                "framework-flow-capability-context-reopen-close-window");

        Span firstStatus = new Span(
                "Capability context reopen first: pending");
        firstStatus.setId(
                "framework-flow-capability-context-reopen-first-status");
        Button captureFirst = new Button(
                "Capture first capability context", event -> {
                    boolean fromClient = event.isFromClient();
                    DesktopCapabilityContext capabilityContext = Desktop.current()
                            .captureCapabilityContext();
                    REOPENED_CAPABILITY_CONTEXT.set(null);
                    FIRST_CAPABILITY_CONTEXT.set(capabilityContext);
                    UI ui = event.getSource().getUI().orElseThrow();
                    Thread.ofVirtual()
                            .name("capability-context-first")
                            .start(() -> {
                                boolean backgroundSessionAbsent =
                                        VaadinSession.getCurrent() == null;
                                DesktopCommandResult result = capabilityContext
                                        .setWindowTitle(FIRST_TITLE);
                                String status =
                                        "Capability context reopen first: firstSuccess="
                                                + result.success()
                                                + " capabilityContextCaptured=true"
                                                + " backgroundSessionAbsent="
                                                + backgroundSessionAbsent
                                                + " client=" + fromClient
                                                + " change="
                                                + firstChangeCount.incrementAndGet();
                                ui.access(() -> firstStatus.setText(status));
                            });
                });
        captureFirst.setId(
                "framework-flow-capability-context-reopen-capture-first");

        Span reopenStatus = new Span(
                "Capability context reopen probe: pending");
        reopenStatus.setId(
                "framework-flow-capability-context-reopen-reopen-status");
        Button probeReopened = new Button(
                "Probe reopened capability context", event -> {
                    boolean fromClient = event.isFromClient();
                    DesktopCapabilityContext retiredContext =
                            FIRST_CAPABILITY_CONTEXT.get();
                    DesktopCapabilityContext freshContext = Desktop.current()
                            .captureCapabilityContext();
                    REOPENED_CAPABILITY_CONTEXT.set(freshContext);
                    UI ui = event.getSource().getUI().orElseThrow();
                    Thread.ofVirtual()
                            .name("capability-context-reopened")
                            .start(() -> {
                                boolean backgroundSessionAbsent =
                                        VaadinSession.getCurrent() == null;
                                DesktopCommandResult retiredBefore =
                                        invokeRetiredContext(
                                                retiredContext,
                                                STALE_BEFORE_TITLE);
                                DesktopCommandResult fresh = freshContext
                                        .setWindowTitle(REOPENED_TITLE);
                                DesktopCommandResult retiredAfter =
                                        invokeRetiredContext(
                                                retiredContext,
                                                STALE_AFTER_TITLE);
                                String status =
                                        "Capability context reopen probe: "
                                                + "retiredBeforeInvalidated="
                                                + invalidated(retiredBefore)
                                                + " retiredBeforeReason="
                                                + cancelReason(retiredBefore)
                                                + " freshSuccess="
                                                + fresh.success()
                                                + " retiredAfterInvalidated="
                                                + invalidated(retiredAfter)
                                                + " retiredAfterReason="
                                                + cancelReason(retiredAfter)
                                                + " capabilityContextCaptured=true"
                                                + " backgroundSessionAbsent="
                                                + backgroundSessionAbsent
                                                + " client=" + fromClient
                                                + " change="
                                                + reopenChangeCount.incrementAndGet();
                                ui.access(() -> reopenStatus.setText(status));
                            });
                });
        probeReopened.setId(
                "framework-flow-capability-context-reopen-probe-reopened");

        Span thirdStatus = new Span(
                "Capability context reopen third: pending");
        thirdStatus.setId(
                "framework-flow-capability-context-reopen-third-status");
        Button probeThird = new Button(
                "Probe third capability context", event -> {
                    boolean fromClient = event.isFromClient();
                    DesktopCapabilityContext firstRetiredContext =
                            FIRST_CAPABILITY_CONTEXT.get();
                    DesktopCapabilityContext reopenedRetiredContext =
                            REOPENED_CAPABILITY_CONTEXT.get();
                    DesktopCapabilityContext freshContext = Desktop.current()
                            .captureCapabilityContext();
                    UI ui = event.getSource().getUI().orElseThrow();
                    Thread.ofVirtual()
                            .name("capability-context-third")
                            .start(() -> {
                                boolean backgroundSessionAbsent =
                                        VaadinSession.getCurrent() == null;
                                DesktopCommandResult firstBefore =
                                        invokeRetiredContext(
                                                firstRetiredContext,
                                                FIRST_STALE_BEFORE_THIRD_TITLE);
                                DesktopCommandResult reopenedBefore =
                                        invokeRetiredContext(
                                                reopenedRetiredContext,
                                                REOPENED_STALE_BEFORE_THIRD_TITLE);
                                DesktopCommandResult fresh = freshContext
                                        .setWindowTitle(THIRD_TITLE);
                                DesktopCommandResult firstAfter =
                                        invokeRetiredContext(
                                                firstRetiredContext,
                                                FIRST_STALE_AFTER_THIRD_TITLE);
                                DesktopCommandResult reopenedAfter =
                                        invokeRetiredContext(
                                                reopenedRetiredContext,
                                                REOPENED_STALE_AFTER_THIRD_TITLE);
                                String status =
                                        "Capability context reopen third: "
                                                + "firstBeforeInvalidated="
                                                + invalidated(firstBefore)
                                                + " firstBeforeReason="
                                                + cancelReason(firstBefore)
                                                + " reopenedBeforeInvalidated="
                                                + invalidated(reopenedBefore)
                                                + " reopenedBeforeReason="
                                                + cancelReason(reopenedBefore)
                                                + " freshSuccess="
                                                + fresh.success()
                                                + " firstAfterInvalidated="
                                                + invalidated(firstAfter)
                                                + " firstAfterReason="
                                                + cancelReason(firstAfter)
                                                + " reopenedAfterInvalidated="
                                                + invalidated(reopenedAfter)
                                                + " reopenedAfterReason="
                                                + cancelReason(reopenedAfter)
                                                + " capabilityContextCaptured=true"
                                                + " backgroundSessionAbsent="
                                                + backgroundSessionAbsent
                                                + " client=" + fromClient
                                                + " change="
                                                + thirdChangeCount.incrementAndGet();
                                ui.access(() -> thirdStatus.setText(status));
                            });
                });
        probeThird.setId(
                "framework-flow-capability-context-reopen-probe-third");

        add(new H1("Flow capability context reopen acceptance"),
                openWindow,
                closeWindow,
                windowStatus,
                captureFirst,
                firstStatus,
                probeReopened,
                reopenStatus,
                probeThird,
                thirdStatus);
    }

    private static DesktopCommandResult invokeRetiredContext(
            DesktopCapabilityContext context, String title) {
        if (context == null) {
            return DesktopCommandResult.error(
                    "Captured capability context is missing");
        }
        return context.setWindowTitle(title);
    }

    private static boolean invalidated(DesktopCommandResult result) {
        return !result.success()
                && Boolean.TRUE.equals(result.payload().get("canceled"))
                && Boolean.TRUE.equals(
                        result.payload().get("operationInvalidated"));
    }

    private static String cancelReason(DesktopCommandResult result) {
        Object reason = result.payload().get("cancelReason");
        return reason == null ? "none" : reason.toString();
    }

    private static String errorValue(DesktopCommandResult result) {
        String error = result.error();
        return error == null || error.isBlank() ? "none" : error;
    }
}
