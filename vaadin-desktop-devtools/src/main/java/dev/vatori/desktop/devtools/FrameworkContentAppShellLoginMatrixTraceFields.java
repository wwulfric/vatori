package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrameworkContentAppShellLoginMatrixTraceFields {
    boolean appShellLoginV160ManifestObserved;
    boolean appShellLoginV160StageObserved;
    boolean appShellLoginV160CompleteStageObserved;
    boolean appShellLoginV160DomObserved;
    boolean bootstrapObserved;
    boolean avatarEventRpcObserved;
    boolean messageEventRpcObserved;
    boolean sideNavEventRpcObserved;
    boolean loginOpenEventRpcObserved;
    boolean loginSubmitEventRpcObserved;
    boolean avatarPatchObserved;
    boolean messagePatchObserved;
    boolean sideNavPatchObserved;
    boolean loginOpenPatchObserved;
    boolean loginSubmitPatchObserved;
    boolean domObserved;

    boolean hasFrameworkSignals() {
        return bootstrapObserved
                && avatarEventRpcObserved
                && messageEventRpcObserved
                && sideNavEventRpcObserved
                && loginOpenEventRpcObserved
                && loginSubmitEventRpcObserved
                && avatarPatchObserved
                && messagePatchObserved
                && sideNavPatchObserved
                && loginOpenPatchObserved
                && loginSubmitPatchObserved
                && domObserved;
    }

    boolean hasAppShellLoginGateSignals() {
        return appShellLoginV160ManifestObserved
                && appShellLoginV160StageObserved
                && (appShellLoginV160CompleteStageObserved || appShellLoginV160DomObserved)
                && appShellLoginV160DomObserved;
    }

    static boolean containsContentAppShellLoginMatrixV160Fields(String line) {
        return containsStringField(line, "contentAppShellLoginMatrixVersion",
                        "flow-ipc-content-app-shell-login-matrix-v1_60")
                && containsStringField(line, "contentAppShellLoginMatrixSource",
                        "overlay-context-menu-matrix-v1_59")
                && containsStringField(line, "contentAppShellLoginMatrixScope",
                        "framework-validation-app-avatar-message-side-nav-login-overlay")
                && containsStringField(line, "contentAppShellLoginMatrixAppShape",
                        "framework-validation-app")
                && containsStringField(line, "contentAppShellLoginMatrixTargetTags",
                        "vaadin-avatar,vaadin-avatar-group,vaadin-message-list,vaadin-message-input,vaadin-side-nav,vaadin-side-nav-item,vaadin-login-overlay,vaadin-login-form")
                && containsStringField(line, "contentAppShellLoginMatrixJavaClasses",
                        "com.vaadin.flow.component.avatar.Avatar,com.vaadin.flow.component.avatar.AvatarGroup,com.vaadin.flow.component.messages.MessageList,com.vaadin.flow.component.messages.MessageInput,com.vaadin.flow.component.sidenav.SideNav,com.vaadin.flow.component.sidenav.SideNavItem,com.vaadin.flow.component.login.LoginOverlay,com.vaadin.flow.component.login.LoginForm")
                && containsStringField(line, "contentAppShellLoginMatrixComponentIds",
                        "framework-avatar,framework-avatar-group,framework-message-list,framework-message-input,framework-side-nav,framework-side-nav-audit,framework-login-overlay,framework-login-form,framework-rich-content-status")
                && containsStringField(line, "contentAppShellLoginMatrixModuleSpecifiers",
                        "@vaadin/avatar/src/vaadin-avatar.js,@vaadin/avatar-group/src/vaadin-avatar-group.js,@vaadin/message-list/src/vaadin-message-list.js,@vaadin/message-input/src/vaadin-message-input.js,@vaadin/side-nav/src/vaadin-side-nav.js,@vaadin/side-nav/src/vaadin-side-nav-item.js,@vaadin/login/src/vaadin-login-overlay.js,@vaadin/login/src/vaadin-login-form.js")
                && containsStringFieldPrefix(line, "contentAppShellLoginMatrixSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line,
                        "contentAppShellLoginMatrixSelectedArtifactCount")
                && containsStringField(line,
                        "contentAppShellLoginMatrixGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line,
                        "contentAppShellLoginMatrixGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && line.contains("contentAppShellLoginMatrixProductionSupportArtifact")
                && line.contains("contentAppShellLoginMatrixProductionSupportSha256")
                && line.contains("contentAppShellLoginMatrixProductionSupportBytes")
                && containsStringFieldPrefix(line,
                        "contentAppShellLoginMatrixProductionEntryArtifact",
                        "current-app-production/VAADIN/build/indexhtml-")
                && containsStringFieldPrefix(line,
                        "contentAppShellLoginMatrixSourceChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && containsStringField(line,
                        "contentAppShellLoginMatrixSourceChunkStatus", "available")
                && line.contains("contentAppShellLoginMatrixResolvedSpecifiers")
                && line.contains("contentAppShellLoginMatrixUnresolvedSpecifiers")
                && containsStringField(line,
                        "contentAppShellLoginMatrixAllowedSchemes", "vatori-resource,blob")
                && containsStringField(line,
                        "contentAppShellLoginMatrixIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line,
                        "contentAppShellLoginMatrixMavenGoal", "build-frontend")
                && containsStringField(line,
                        "contentAppShellLoginMatrixBuildStatus", "success")
                && line.contains("contentAppShellLoginMatrixOutputUsability")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixFrameworkAppRequired", "true")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixFrameworkAppObserved", "true")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixCliContractEnabled", "true")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixManifestReady", "true")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixRuntimeHandoffRequired", "true")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixRuntimeHandoffReady", "true")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixFixtureSubstituted", "false")
                && line.contains("contentAppShellLoginMatrixAcceptableFailures")
                && line.contains("login-overlay-event-shape-not-covered")
                && line.contains("contentAppShellLoginMatrixRemainingBlockers")
                && line.contains("rich-text-editor")
                && line.contains("contentAppShellLoginMatrixNextResolutionStep");
    }

    static boolean containsContentAppShellLoginMatrixV160Complete(String line) {
        return containsContentAppShellLoginMatrixV160Fields(line)
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixStaged", "true")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixCustomSchemeObserved", "true")
                && containsLiteralField(line,
                        "contentAppShellLoginMatrixNoLocalhost", "true");
    }
}
