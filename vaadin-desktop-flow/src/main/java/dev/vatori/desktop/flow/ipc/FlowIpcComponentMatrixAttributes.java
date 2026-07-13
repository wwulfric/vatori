package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcComponentMatrixAttributes {
    private static final Spec BROADER_COMPONENT_RESOURCE_MATRIX = new Spec(
            "broaderComponentResourceMatrix",
            "flow-ipc-broader-component-resource-matrix-v1_46",
            "packaging-runtime-handoff-reuse-v1_45",
            "framework-validation-app-combobox-datepicker-dialog",
            "vaadin-combo-box,vaadin-date-picker,vaadin-dialog",
            "com.vaadin.flow.component.combobox.ComboBox,com.vaadin.flow.component.datepicker.DatePicker,com.vaadin.flow.component.dialog.Dialog",
            "framework-reviewer,framework-due-date,framework-dialog-open,framework-dialog",
            "@vaadin/combo-box/src/vaadin-combo-box.js,@vaadin/date-picker/src/vaadin-date-picker.js,@vaadin/dialog/src/vaadin-dialog.js",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_46",
            "component-tag-not-registered,component-event-rpc-missing,component-patch-missing,resource-specifier-unresolved,browser-import-error",
            "framework-component-matrix-runtime-validation-pending",
            "grid-virtualization,upload-download,binder-validation,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn the framework component matrix into reusable broad component/resource acceptance instead of a three-component validation slice");
    private static final Spec DATA_COMPONENT_RESOURCE_MATRIX = new Spec(
            "dataComponentResourceMatrix",
            "flow-ipc-data-component-resource-matrix-v1_47",
            "broader-component-resource-matrix-v1_46",
            "framework-validation-app-grid-virtual-list-form-binder",
            "vaadin-grid,vaadin-grid-column,vaadin-virtual-list,vaadin-form-layout,vaadin-text-field",
            "com.vaadin.flow.component.grid.Grid,com.vaadin.flow.component.virtuallist.VirtualList,com.vaadin.flow.component.formlayout.FormLayout,com.vaadin.flow.data.binder.Binder,com.vaadin.flow.component.textfield.TextField",
            "framework-grid,framework-virtual-list,framework-form-layout,framework-owner-name,framework-validation-action,framework-validation-status",
            "@vaadin/grid/src/vaadin-grid.js,@vaadin/virtual-list/src/vaadin-virtual-list.js,@vaadin/form-layout/src/vaadin-form-layout.js,@vaadin/text-field/src/vaadin-text-field.js",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_47",
            "grid-virtualized-rows-not-materialized,virtual-list-item-renderer-pending,data-provider-rpc-boundary,resource-specifier-unresolved,browser-import-error",
            "framework-data-component-matrix-runtime-validation-pending",
            "full-grid-virtualization,lazy-data-provider,upload-download,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn bounded data and binder component evidence into deeper Grid/VirtualList virtualization or Upload/Download resource parity");
    private static final Spec INPUT_COMPONENT_PARITY_MATRIX = new Spec(
            "inputComponentParityMatrix",
            "flow-ipc-input-component-parity-matrix-v1_57",
            "frontend-resource-pipeline-basic-closure-v1_56",
            "framework-validation-app-common-input-components",
            "vaadin-radio-group,vaadin-text-area,vaadin-email-field,vaadin-integer-field,vaadin-password-field,vaadin-time-picker",
            "com.vaadin.flow.component.radiobutton.RadioButtonGroup,com.vaadin.flow.component.textfield.TextArea,com.vaadin.flow.component.textfield.EmailField,com.vaadin.flow.component.textfield.IntegerField,com.vaadin.flow.component.textfield.PasswordField,com.vaadin.flow.component.timepicker.TimePicker",
            "framework-decision,framework-notes,framework-contact-email,framework-score,framework-secret,framework-review-time,framework-input-matrix-status",
            "@vaadin/radio-group/src/vaadin-radio-group.js,@vaadin/text-area/src/vaadin-text-area.js,@vaadin/email-field/src/vaadin-email-field.js,@vaadin/integer-field/src/vaadin-integer-field.js,@vaadin/password-field/src/vaadin-password-field.js,@vaadin/time-picker/src/vaadin-time-picker.js",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_57",
            "input-component-tag-not-registered,input-component-event-rpc-missing,input-component-patch-missing,resource-specifier-unresolved,browser-import-error",
            "framework-input-component-matrix-runtime-validation-pending",
            "multi-value-inputs,rich-text-and-file-adjacent-components,menus-navigation-overlay-components,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "continue broadening component parity into multi-value input, navigation/menu/overlay, theme, and remaining Vaadin component families");
    private static final Spec MULTI_VALUE_NAVIGATION_DISCLOSURE_MATRIX = new Spec(
            "multiValueNavigationDisclosureMatrix",
            "flow-ipc-multi-value-navigation-disclosure-matrix-v1_58",
            "input-component-parity-matrix-v1_57",
            "framework-validation-app-multi-value-navigation-disclosure-menu",
            "vaadin-multi-select-combo-box,vaadin-tabs,vaadin-tab,vaadin-details,vaadin-accordion,vaadin-menu-bar",
            "com.vaadin.flow.component.combobox.MultiSelectComboBox,com.vaadin.flow.component.tabs.Tabs,com.vaadin.flow.component.tabs.Tab,com.vaadin.flow.component.details.Details,com.vaadin.flow.component.accordion.Accordion,com.vaadin.flow.component.menubar.MenuBar",
            "framework-assignees,framework-nav-tabs,framework-nav-tab-audit,framework-details,framework-accordion,framework-menu,framework-menu-audit,framework-navigation-matrix-status",
            "@vaadin/multi-select-combo-box/src/vaadin-multi-select-combo-box.js,@vaadin/tabs/src/vaadin-tabs.js,@vaadin/tabs/src/vaadin-tab.js,@vaadin/details/src/vaadin-details.js,@vaadin/accordion/src/vaadin-accordion.js,@vaadin/menu-bar/src/vaadin-menu-bar.js",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_58",
            "multi-value-component-tag-not-registered,navigation-component-event-rpc-missing,navigation-component-patch-missing,resource-specifier-unresolved,browser-import-error,overlay-focus-keyboard-behavior-not-covered",
            "framework-multi-value-navigation-disclosure-matrix-runtime-validation-pending",
            "context-menu-rich-overlay-behavior,overlay-focus-keyboard-parity,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn this bounded multi-value/navigation/disclosure/menu matrix into broader overlay, keyboard, focus and theme parity");
    private static final Spec OVERLAY_CONTEXT_MENU_MATRIX = new Spec(
            "overlayContextMenuMatrix",
            "flow-ipc-overlay-context-menu-matrix-v1_59",
            "multi-value-navigation-disclosure-matrix-v1_58",
            "framework-validation-app-context-menu-grid-context-menu-popover",
            "vaadin-context-menu,vaadin-popover,vaadin-grid",
            "com.vaadin.flow.component.contextmenu.ContextMenu,com.vaadin.flow.component.grid.contextmenu.GridContextMenu,com.vaadin.flow.component.popover.Popover",
            "framework-context-menu-target,framework-context-menu,framework-context-menu-audit,framework-grid-context-menu,framework-grid-context-menu-audit,framework-popover-target,framework-popover,framework-overlay-matrix-status",
            "@vaadin/context-menu/src/vaadin-context-menu.js,@vaadin/grid/src/vaadin-grid.js,@vaadin/popover/src/vaadin-popover.js",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_59",
            "overlay-component-tag-not-registered,overlay-component-event-rpc-missing,overlay-component-patch-missing,resource-specifier-unresolved,browser-import-error,overlay-focus-keyboard-behavior-not-covered",
            "framework-overlay-context-menu-matrix-runtime-validation-pending",
            "overlay-focus-keyboard-parity,rich-text-editor,avatar-message-components,theme-variants,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn bounded context menu and popover event parity into full overlay focus, keyboard, positioning and rich component parity");
    private static final Spec CONTENT_APP_SHELL_LOGIN_MATRIX = new Spec(
            "contentAppShellLoginMatrix",
            "flow-ipc-content-app-shell-login-matrix-v1_60",
            "overlay-context-menu-matrix-v1_59",
            "framework-validation-app-avatar-message-side-nav-login-overlay",
            "vaadin-avatar,vaadin-avatar-group,vaadin-message-list,vaadin-message-input,vaadin-side-nav,vaadin-side-nav-item,vaadin-login-overlay,vaadin-login-form",
            "com.vaadin.flow.component.avatar.Avatar,com.vaadin.flow.component.avatar.AvatarGroup,com.vaadin.flow.component.messages.MessageList,com.vaadin.flow.component.messages.MessageInput,com.vaadin.flow.component.sidenav.SideNav,com.vaadin.flow.component.sidenav.SideNavItem,com.vaadin.flow.component.login.LoginOverlay,com.vaadin.flow.component.login.LoginForm",
            "framework-avatar,framework-avatar-group,framework-message-list,framework-message-input,framework-side-nav,framework-side-nav-audit,framework-login-overlay,framework-login-form,framework-rich-content-status",
            "@vaadin/avatar/src/vaadin-avatar.js,@vaadin/avatar-group/src/vaadin-avatar-group.js,@vaadin/message-list/src/vaadin-message-list.js,@vaadin/message-input/src/vaadin-message-input.js,@vaadin/side-nav/src/vaadin-side-nav.js,@vaadin/side-nav/src/vaadin-side-nav-item.js,@vaadin/login/src/vaadin-login-overlay.js,@vaadin/login/src/vaadin-login-form.js",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_60",
            "content-component-tag-not-registered,content-component-event-rpc-missing,content-component-patch-missing,resource-specifier-unresolved,browser-import-error,login-overlay-event-shape-not-covered,side-nav-router-navigation-not-covered",
            "framework-content-app-shell-login-matrix-runtime-validation-pending",
            "rich-text-editor,full-login-security-flow,side-nav-router-navigation,theme-variants,overlay-focus-keyboard-parity,dev-mode-vite-parity,installer-packaging,durable-reconnect",
            "turn bounded content, message, side navigation and login overlay event parity into richer editor, app-shell navigation and auth flow parity");
    private static final Spec RICH_TEXT_EDITOR_MATRIX = new Spec(
            "richTextEditorMatrix",
            "flow-ipc-rich-text-editor-matrix-v1_62",
            "lifecycle-focus-behavior-matrix-v1_61",
            "framework-validation-app-rich-text-editor",
            "vaadin-rich-text-editor",
            "com.vaadin.flow.component.richtexteditor.RichTextEditor",
            "framework-rich-text-editor,framework-rich-text-editor-status",
            "@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_62",
            "rich-text-editor-tag-not-registered,rich-text-editor-event-rpc-missing,rich-text-editor-patch-missing,resource-specifier-unresolved,browser-import-error,rich-text-editor-selection-toolbar-not-covered",
            "framework-rich-text-editor-matrix-runtime-validation-pending",
            "rich-text-editor-selection-toolbar-parity,theme-variants,dev-mode-vite-parity,overlay-focus-keyboard-parity,installer-packaging,durable-reconnect",
            "turn bounded RichTextEditor value-change parity into full editor toolbar, selection, keyboard and theme parity");
    private static final Spec RICH_TEXT_EDITOR_BEHAVIOR_MATRIX = new Spec(
            "richTextEditorBehaviorMatrix",
            "flow-ipc-rich-text-editor-behavior-matrix-v1_63",
            "rich-text-editor-matrix-v1_62",
            "framework-validation-app-rich-text-editor-toolbar-selection-keyboard",
            "vaadin-rich-text-editor",
            "com.vaadin.flow.component.richtexteditor.RichTextEditor,com.vaadin.flow.component.richtexteditor.RichTextEditorVariant",
            "framework-rich-text-editor,framework-rich-text-editor-status,framework-rich-text-editor-behavior-status",
            "@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_63",
            "rich-text-editor-toolbar-not-rendered,rich-text-editor-quill-api-missing,rich-text-editor-selection-event-rpc-missing,rich-text-editor-toolbar-event-rpc-missing,rich-text-editor-keyboard-event-rpc-missing,rich-text-editor-delta-html-event-rpc-missing,browser-import-error",
            "framework-rich-text-editor-behavior-matrix-runtime-validation-pending",
            "full-rich-text-editor-toolbar-parity,clipboard,full-selection-range-parity,theme-variants,dev-mode-vite-parity,overlay-focus-keyboard-parity,installer-packaging,durable-reconnect",
            "turn bounded RichTextEditor toolbar, selection, keyboard and delta/html behavior into full editing, clipboard, selection and theme parity");
    private static final Spec RICH_TEXT_EDITOR_EDITING_MATRIX = new Spec(
            "richTextEditorEditingMatrix",
            "flow-ipc-rich-text-editor-editing-matrix-v1_64",
            "rich-text-editor-behavior-matrix-v1_63",
            "framework-validation-app-rich-text-editor-format-clipboard-link-list",
            "vaadin-rich-text-editor",
            "com.vaadin.flow.component.richtexteditor.RichTextEditor,com.vaadin.flow.component.richtexteditor.RichTextEditorVariant",
            "framework-rich-text-editor,framework-rich-text-editor-status,framework-rich-text-editor-behavior-status,framework-rich-text-editor-editing-status",
            "@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js",
            "VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_64",
            "rich-text-editor-quill-api-missing,rich-text-editor-format-event-rpc-missing,rich-text-editor-clipboard-event-rpc-missing,rich-text-editor-link-event-rpc-missing,rich-text-editor-editing-patch-missing,browser-import-error",
            "framework-rich-text-editor-editing-matrix-runtime-validation-pending",
            "native-clipboard-integration,full-rich-text-editor-toolbar-parity,full-selection-range-parity,theme-variants,accessibility,dev-mode-vite-parity,overlay-focus-keyboard-parity,installer-packaging,durable-reconnect",
            "turn bounded RichTextEditor format, clipboard, link/list and extended selection evidence into complete editor behavior, native clipboard, theme and accessibility parity");
    private static final String RICH_TEXT_EDITOR_BEHAVIOR_EXPECTED_EVENTS =
            "vatori-rich-text-editor-selection,vatori-rich-text-editor-toolbar,keydown,value-changed";
    private static final String RICH_TEXT_EDITOR_BEHAVIOR_EXPECTED_BEHAVIOR =
            "selection-range,toolbar-bold,keyboard-bold,delta-html-value,compact-theme-variant";
    private static final String RICH_TEXT_EDITOR_EDITING_EXPECTED_EVENTS =
            "vatori-rich-text-editor-format,vatori-rich-text-editor-clipboard,vatori-rich-text-editor-link";
    private static final String RICH_TEXT_EDITOR_EDITING_EXPECTED_BEHAVIOR =
            "italic-format,ordered-list,link-format,clear-format,clipboard-copy-paste,extended-selection-range,compact-theme-variant";

    private FlowIpcComponentMatrixAttributes() {
    }

    static Map<String, Object> frameworkValidationMatrixAttributes(
            Context defaultContext, Context productionChunkContext) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        put(attributes, BROADER_COMPONENT_RESOURCE_MATRIX, defaultContext);
        put(attributes, DATA_COMPONENT_RESOURCE_MATRIX, defaultContext);
        put(attributes, INPUT_COMPONENT_PARITY_MATRIX, defaultContext);
        put(attributes, MULTI_VALUE_NAVIGATION_DISCLOSURE_MATRIX, defaultContext);
        put(attributes, OVERLAY_CONTEXT_MENU_MATRIX, defaultContext);
        put(attributes, CONTENT_APP_SHELL_LOGIN_MATRIX, productionChunkContext);
        put(attributes, RICH_TEXT_EDITOR_MATRIX, productionChunkContext);
        put(attributes, RICH_TEXT_EDITOR_BEHAVIOR_MATRIX, productionChunkContext);
        attributes.put("richTextEditorBehaviorMatrixExpectedEvents",
                RICH_TEXT_EDITOR_BEHAVIOR_EXPECTED_EVENTS);
        attributes.put("richTextEditorBehaviorMatrixExpectedBehavior",
                RICH_TEXT_EDITOR_BEHAVIOR_EXPECTED_BEHAVIOR);
        put(attributes, RICH_TEXT_EDITOR_EDITING_MATRIX, productionChunkContext);
        attributes.put("richTextEditorEditingMatrixExpectedEvents",
                RICH_TEXT_EDITOR_EDITING_EXPECTED_EVENTS);
        attributes.put("richTextEditorEditingMatrixExpectedBehavior",
                RICH_TEXT_EDITOR_EDITING_EXPECTED_BEHAVIOR);
        return attributes;
    }

    static void put(Map<String, Object> attributes, Spec spec, Context context) {
        String prefix = spec.prefix();
        attributes.put(prefix + "Version", spec.version());
        attributes.put(prefix + "Source", spec.source());
        attributes.put(prefix + "Scope", spec.scope());
        attributes.put(prefix + "AppShape", context.appShape());
        attributes.put(prefix + "TargetTags", spec.targetTags());
        attributes.put(prefix + "JavaClasses", spec.javaClasses());
        attributes.put(prefix + "ComponentIds", spec.componentIds());
        attributes.put(prefix + "ModuleSpecifiers", spec.moduleSpecifiers());
        attributes.put(prefix + "SelectedArtifacts", context.selectedArtifacts());
        attributes.put(prefix + "SelectedArtifactCount",
                FlowIpcJavaScriptModuleGraph.csvValues(context.selectedArtifacts()).size());
        attributes.put(prefix + "GeneratedImportsOwnerSource", context.generatedImportsOwnerSource());
        attributes.put(prefix + "GeneratedImportsOwnerArtifact", context.generatedImportsOwnerArtifact());
        attributes.put(prefix + "ProductionSupportArtifact", context.productionSupportArtifact());
        attributes.put(prefix + "ProductionSupportSha256", context.productionSupportSha256());
        attributes.put(prefix + "ProductionSupportBytes", context.productionSupportBytes());
        attributes.put(prefix + "ProductionEntryArtifact", context.productionEntryArtifact());
        attributes.put(prefix + "SourceChunkArtifact", context.sourceChunkArtifact());
        attributes.put(prefix + "SourceChunkStatus", context.sourceChunkStatus());
        attributes.put(prefix + "ResolvedSpecifiers", context.resolvedSpecifiers());
        attributes.put(prefix + "UnresolvedSpecifiers", context.unresolvedSpecifiers());
        attributes.put(prefix + "AllowedSchemes", context.allowedSchemes());
        attributes.put(prefix + "IntegrityPolicy", context.integrityPolicy());
        attributes.put(prefix + "MavenGoal", context.mavenGoal());
        attributes.put(prefix + "BuildStatus", context.buildStatus());
        attributes.put(prefix + "OutputUsability", context.outputUsability());
        attributes.put(prefix + "FrameworkAppRequired", true);
        attributes.put(prefix + "FrameworkAppObserved", context.frameworkApp());
        attributes.put(prefix + "CliContractEnabled", "1".equals(System.getenv(spec.cliFlag())));
        attributes.put(prefix + "ManifestReady", context.runtimeHandoffReady() && context.frameworkApp());
        attributes.put(prefix + "RuntimeHandoffRequired", true);
        attributes.put(prefix + "RuntimeHandoffReady", context.runtimeHandoffReady());
        attributes.put(prefix + "RuntimeHandoffSatisfied", false);
        attributes.put(prefix + "Staged", false);
        attributes.put(prefix + "CustomSchemeObserved", false);
        attributes.put(prefix + "BlobFallbackObserved", false);
        attributes.put(prefix + "NoLocalhost", true);
        attributes.put(prefix + "FixtureSubstituted", false);
        attributes.put(prefix + "AcceptableFailures", spec.acceptableFailures());
        attributes.put(prefix + "FailureLayer", context.frameworkApp()
                ? spec.frameworkPendingFailureLayer()
                : "framework-validation-app-not-selected");
        attributes.put(prefix + "RemainingBlockers", spec.remainingBlockers());
        attributes.put(prefix + "NextResolutionStep", spec.nextResolutionStep());
    }

    record Spec(
            String prefix,
            String version,
            String source,
            String scope,
            String targetTags,
            String javaClasses,
            String componentIds,
            String moduleSpecifiers,
            String cliFlag,
            String acceptableFailures,
            String frameworkPendingFailureLayer,
            String remainingBlockers,
            String nextResolutionStep) {
    }

    record Context(
            String appShape,
            boolean frameworkApp,
            String selectedArtifacts,
            String generatedImportsOwnerSource,
            String generatedImportsOwnerArtifact,
            String productionSupportArtifact,
            String productionSupportSha256,
            int productionSupportBytes,
            String productionEntryArtifact,
            String sourceChunkArtifact,
            String sourceChunkStatus,
            String resolvedSpecifiers,
            String unresolvedSpecifiers,
            String allowedSchemes,
            String integrityPolicy,
            String mavenGoal,
            String buildStatus,
            String outputUsability,
            boolean runtimeHandoffReady) {
    }
}
