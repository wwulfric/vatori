package dev.vatori.desktop.devtools;

final class FrameworkBootstrapTraceInspector {
    private FrameworkBootstrapTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-root")
                && line.contains("framework-budget")
                && line.contains("framework-expedite")
                && line.contains("framework-priority")
                && line.contains("framework-apply")
                && line.contains("framework-updates")
                && line.contains("com.vaadin.flow.component.textfield.BigDecimalField")
                && line.contains("com.vaadin.flow.component.checkbox.Checkbox")
                && line.contains("com.vaadin.flow.component.select.Select")) {
            summary.frameworkApp.bootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-reviewer")
                && line.contains("framework-due-date")
                && line.contains("framework-dialog-open")
                && line.contains("framework-matrix-status")
                && line.contains("com.vaadin.flow.component.combobox.ComboBox")
                && line.contains("com.vaadin.flow.component.datepicker.DatePicker")
                && line.contains("com.vaadin.flow.component.dialog.Dialog")) {
            summary.frameworkComponentMatrix.bootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-grid")
                && line.contains("framework-virtual-list")
                && line.contains("framework-form-layout")
                && line.contains("framework-owner-name")
                && line.contains("framework-validation-action")
                && line.contains("framework-validation-status")
                && line.contains("com.vaadin.flow.component.grid.Grid")
                && line.contains("com.vaadin.flow.component.virtuallist.VirtualList")
                && line.contains("com.vaadin.flow.component.formlayout.FormLayout")
                && line.contains("com.vaadin.flow.data.binder.Binder")
                && line.contains("com.vaadin.flow.component.textfield.TextField")) {
            summary.frameworkComponentMatrix.dataBootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-decision")
                && line.contains("framework-notes")
                && line.contains("framework-contact-email")
                && line.contains("framework-score")
                && line.contains("framework-secret")
                && line.contains("framework-review-time")
                && line.contains("framework-input-matrix-status")
                && line.contains("com.vaadin.flow.component.radiobutton.RadioButtonGroup")
                && line.contains("com.vaadin.flow.component.textfield.TextArea")
                && line.contains("com.vaadin.flow.component.textfield.EmailField")
                && line.contains("com.vaadin.flow.component.textfield.IntegerField")
                && line.contains("com.vaadin.flow.component.textfield.PasswordField")
                && line.contains("com.vaadin.flow.component.timepicker.TimePicker")) {
            summary.frameworkInputComponentMatrix.bootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-assignees")
                && line.contains("framework-nav-tabs")
                && line.contains("framework-nav-tab-audit")
                && line.contains("framework-details")
                && line.contains("framework-accordion")
                && line.contains("framework-menu")
                && line.contains("framework-navigation-matrix-status")
                && line.contains("com.vaadin.flow.component.combobox.MultiSelectComboBox")
                && line.contains("com.vaadin.flow.component.tabs.Tabs")
                && line.contains("com.vaadin.flow.component.tabs.Tab")
                && line.contains("com.vaadin.flow.component.details.Details")
                && line.contains("com.vaadin.flow.component.accordion.Accordion")
                && line.contains("com.vaadin.flow.component.menubar.MenuBar")) {
            summary.frameworkNavigationComponentMatrix.bootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-context-menu-target")
                && line.contains("framework-context-menu")
                && line.contains("framework-context-menu-audit")
                && line.contains("framework-grid-context-menu")
                && line.contains("framework-grid-context-menu-audit")
                && line.contains("framework-popover-target")
                && line.contains("framework-popover")
                && line.contains("framework-overlay-matrix-status")
                && line.contains("com.vaadin.flow.component.contextmenu.ContextMenu")
                && line.contains("com.vaadin.flow.component.grid.contextmenu.GridContextMenu")
                && line.contains("com.vaadin.flow.component.popover.Popover")) {
            summary.frameworkOverlayComponentMatrix.bootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-avatar")
                && line.contains("framework-avatar-group")
                && line.contains("framework-message-list")
                && line.contains("framework-message-input")
                && line.contains("framework-side-nav")
                && line.contains("framework-side-nav-audit")
                && line.contains("framework-login-overlay")
                && line.contains("framework-rich-content-status")
                && line.contains("com.vaadin.flow.component.avatar.Avatar")
                && line.contains("com.vaadin.flow.component.avatar.AvatarGroup")
                && line.contains("com.vaadin.flow.component.messages.MessageList")
                && line.contains("com.vaadin.flow.component.messages.MessageInput")
                && line.contains("com.vaadin.flow.component.sidenav.SideNav")
                && line.contains("com.vaadin.flow.component.sidenav.SideNavItem")
                && line.contains("com.vaadin.flow.component.login.LoginOverlay")) {
            summary.frameworkContentAppShellLoginMatrix.bootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-lifecycle-focus")
                && line.contains("framework-lifecycle-toggle")
                && line.contains("framework-lifecycle-focus-status")
                && line.contains("com.vaadin.flow.component.textfield.TextField")
                && line.contains("com.vaadin.flow.component.button.Button")) {
            summary.frameworkLifecycleFocusBehaviorMatrix.bootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-lazy-grid")
                && line.contains("framework-lazy-grid-status")
                && line.contains("framework-lazy-grid-report")
                && line.contains("setViewportRange")
                && line.contains("com.vaadin.flow.component.grid.Grid")) {
            summary.gridResources.lazyBootstrapObserved = true;
            summary.gridResources.scrollBootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-lazy-grid")
                && line.contains("scrollToIndex")
                && line.contains("com.vaadin.flow.component.grid.Grid")) {
            summary.gridResources.scrollBootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-stream-resource-download")
                && line.contains("framework-stream-resource-status")
                && line.contains("com.vaadin.flow.component.html.Anchor")
                && line.contains("com.vaadin.flow.server.StreamResource")) {
            summary.streamResources.basicBootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-stream-resource-range-download")
                && line.contains("framework-stream-resource-range-status")
                && line.contains("com.vaadin.flow.component.html.Anchor")
                && line.contains("com.vaadin.flow.server.StreamResource")) {
            summary.streamResources.rangeBootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-stream-resource-range-download")
                && line.contains("framework-stream-resource-chunks-status")
                && line.contains("com.vaadin.flow.component.html.Anchor")
                && line.contains("com.vaadin.flow.server.StreamResource")) {
            summary.streamResources.chunksBootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-upload")
                && line.contains("framework-upload-status")
                && line.contains("com.vaadin.flow.component.upload.Upload")
                && line.contains("com.vaadin.flow.server.streams.UploadHandler")) {
            summary.uploads.receiverBootstrapObserved = true;
        }
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-download-handler-save-open")
                && line.contains("framework-download-handler-save-open-status")
                && line.contains("com.vaadin.flow.server.streams.DownloadHandler")
                && line.contains("com.vaadin.flow.server.streams.DownloadResponse")) {
            summary.downloads.saveOpenBootstrapObserved = true;
        }
    }
}
