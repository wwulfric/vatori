package dev.vatori.ipcframework;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.router.Route;

import java.util.concurrent.atomic.AtomicInteger;

@Route("flow-overlay-stack")
public final class FlowOverlayStackRoute extends VerticalLayout {
    public FlowOverlayStackRoute() {
        setId("framework-flow-overlay-stack-route");
        setPadding(true);
        setSpacing(true);

        Span dialogLifecycleStatus =
                new Span("Overlay stack Dialog lifecycle: idle");
        dialogLifecycleStatus.setId(
                "framework-flow-overlay-stack-dialog-lifecycle-status");
        Span popoverLifecycleStatus =
                new Span("Overlay stack Popover lifecycle: idle");
        popoverLifecycleStatus.setId(
                "framework-flow-overlay-stack-popover-lifecycle-status");
        Span confirmLifecycleStatus =
                new Span("Overlay stack ConfirmDialog lifecycle: idle");
        confirmLifecycleStatus.setId(
                "framework-flow-overlay-stack-confirm-lifecycle-status");
        Span confirmClosedStatus =
                new Span("Overlay stack ConfirmDialog closed event: idle");
        confirmClosedStatus.setId(
                "framework-flow-overlay-stack-confirm-closed-status");
        Span confirmRequestStatus =
                new Span("Overlay stack confirm request: pending");
        confirmRequestStatus.setId(
                "framework-flow-overlay-stack-confirm-request-status");

        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setId("framework-flow-overlay-stack-confirm");
        confirmDialog.setHeader("Flow IPC top confirmation");
        confirmDialog.setText(
                "Verify the top modal layer without closing its parents");
        confirmDialog.setConfirmText("Confirm");
        confirmDialog.setCancelText("Cancel");
        confirmDialog.setCancelable(true);
        confirmDialog.setCloseOnEsc(true);
        confirmDialog.addOpenedChangeListener(event ->
                confirmLifecycleStatus.setText(
                        "Overlay stack ConfirmDialog lifecycle: "
                                + (event.isOpened() ? "opened" : "closed")
                                + " client=" + event.isFromClient()));
        confirmDialog.addClosedListener(event ->
                confirmClosedStatus.setText(
                        "Overlay stack ConfirmDialog closed event: client="
                                + event.isFromClient()));

        AtomicInteger confirmRequestCount = new AtomicInteger();
        Button confirmOpen = new Button("Open top confirmation", event -> {
            confirmRequestStatus.setText(
                    "Overlay stack confirm request: "
                            + confirmRequestCount.incrementAndGet());
            confirmDialog.open();
        });
        confirmOpen.setId("framework-flow-overlay-stack-confirm-open");

        Button popoverLastAction = new Button("Popover last action");
        popoverLastAction.setId(
                "framework-flow-overlay-stack-popover-last-action");
        // Let Vaadin auto-add the top overlay to the active modal stack.
        Popover popover = new Popover(
                new Span("Anchored modeless middle layer"),
                confirmOpen,
                popoverLastAction,
                confirmLifecycleStatus,
                confirmClosedStatus,
                confirmRequestStatus);
        popover.setId("framework-flow-overlay-stack-popover");
        popover.setOpenOnClick(true);
        popover.setModal(false);
        popover.setAutofocus(true);
        popover.setCloseOnEsc(true);
        popover.setCloseOnOutsideClick(true);
        popover.setRole("dialog");
        popover.setAriaLabel("Flow IPC middle modeless popover");
        popover.addOpenedChangeListener(event ->
                popoverLifecycleStatus.setText(
                        "Overlay stack Popover lifecycle: "
                                + (event.isOpened() ? "opened" : "closed")
                                + " client=" + event.isFromClient()));

        Button popoverTarget = new Button("Open middle popover");
        popoverTarget.setId("framework-flow-overlay-stack-popover-target");
        popover.setTarget(popoverTarget);

        Button dialogOutsideTarget = new Button("Dialog outside target");
        dialogOutsideTarget.setId(
                "framework-flow-overlay-stack-dialog-outside-target");

        Dialog dialog = new Dialog();
        dialog.setId("framework-flow-overlay-stack-dialog");
        dialog.setHeaderTitle("Flow IPC three-level overlay stack");
        dialog.setCloseOnEsc(true);
        dialog.setFocusTrap(true);
        dialog.addOpenedChangeListener(event ->
                dialogLifecycleStatus.setText(
                        "Overlay stack Dialog lifecycle: "
                                + (event.isOpened() ? "opened" : "closed")
                                + " client=" + event.isFromClient()));
        dialog.add(
                popoverTarget,
                popover,
                popoverLifecycleStatus,
                dialogOutsideTarget,
                dialogLifecycleStatus);

        AtomicInteger dialogRequestCount = new AtomicInteger();
        Span dialogRequestStatus =
                new Span("Overlay stack dialog request: pending");
        dialogRequestStatus.setId(
                "framework-flow-overlay-stack-dialog-request-status");
        Button dialogOpen = new Button("Open overlay stack", event -> {
            dialogRequestStatus.setText(
                    "Overlay stack dialog request: "
                            + dialogRequestCount.incrementAndGet());
            dialog.open();
            popoverTarget.focus();
        });
        dialogOpen.setId("framework-flow-overlay-stack-open");

        Button returnToFramework = new Button("Return to framework", event ->
                event.getSource().getUI()
                        .ifPresent(ui ->
                                ui.navigate(FlowOverIpcFrameworkView.class)));
        returnToFramework.setId("framework-flow-overlay-stack-return");

        add(
                new H1("Flow heterogeneous overlay stack acceptance"),
                dialogOpen,
                dialogRequestStatus,
                dialog,
                returnToFramework);
    }
}
