package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ThemeMatrixNavigationContentTraceInspector {
    private ThemeMatrixNavigationContentTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "tabsThemeVariantMatrixComplete", "true")
                && containsStringField(line,
                        "tabsThemeVariantMatrixVersion",
                        "flow-ipc-tabs-theme-variant-matrix-v1_81")
                && containsStringField(line,
                        "tabsThemeVariantMatrixTheme",
                        "small")
                && containsLiteralField(line,
                        "tabsThemeVariantMatrixSmallObserved", "true")
                && containsLiteralField(line,
                        "tabsThemeVariantMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "tabsThemeVariantMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.tabsDomObserved = true;
        }
        if (containsLiteralField(line,
                "detailsThemeVariantMatrixComplete", "true")
                && containsStringField(line,
                        "detailsThemeVariantMatrixVersion",
                        "flow-ipc-details-theme-variant-matrix-v1_82")
                && containsStringField(line,
                        "detailsThemeVariantMatrixTheme",
                        "small")
                && containsLiteralField(line,
                        "detailsThemeVariantMatrixSmallObserved", "true")
                && containsLiteralField(line,
                        "detailsThemeVariantMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "detailsThemeVariantMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.detailsDomObserved = true;
        }
        if (containsLiteralField(line,
                "menuBarThemeVariantMatrixComplete", "true")
                && containsStringField(line,
                        "menuBarThemeVariantMatrixVersion",
                        "flow-ipc-menu-bar-theme-variant-matrix-v1_83")
                && containsStringField(line,
                        "menuBarThemeVariantMatrixTheme",
                        "small")
                && containsLiteralField(line,
                        "menuBarThemeVariantMatrixSmallObserved", "true")
                && containsLiteralField(line,
                        "menuBarThemeVariantMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "menuBarThemeVariantMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.menuBarDomObserved = true;
        }
        if (containsLiteralField(line,
                "gridThemeVariantMatrixComplete", "true")
                && containsStringField(line,
                        "gridThemeVariantMatrixVersion",
                        "flow-ipc-grid-theme-variant-matrix-v1_84")
                && containsStringField(line,
                        "gridThemeVariantMatrixTheme",
                        "compact")
                && containsLiteralField(line,
                        "gridThemeVariantMatrixCompactObserved", "true")
                && containsLiteralField(line,
                        "gridThemeVariantMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "gridThemeVariantMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.gridDomObserved = true;
        }
        if (containsLiteralField(line,
                "avatarThemeVariantMatrixComplete", "true")
                && containsStringField(line,
                        "avatarThemeVariantMatrixVersion",
                        "flow-ipc-avatar-theme-variant-matrix-v1_85")
                && containsStringField(line,
                        "avatarThemeVariantMatrixTheme",
                        "small")
                && containsLiteralField(line,
                        "avatarThemeVariantMatrixSmallObserved", "true")
                && containsLiteralField(line,
                        "avatarThemeVariantMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "avatarThemeVariantMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.avatarDomObserved = true;
        }
        if (containsLiteralField(line,
                "avatarGroupThemeVariantMatrixComplete", "true")
                && containsStringField(line,
                        "avatarGroupThemeVariantMatrixVersion",
                        "flow-ipc-avatar-group-theme-variant-matrix-v1_86")
                && containsStringField(line,
                        "avatarGroupThemeVariantMatrixTheme",
                        "small")
                && containsLiteralField(line,
                        "avatarGroupThemeVariantMatrixSmallObserved", "true")
                && containsLiteralField(line,
                        "avatarGroupThemeVariantMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "avatarGroupThemeVariantMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.avatarGroupDomObserved = true;
        }
        if (containsLiteralField(line,
                "messageInputThemeVariantMatrixComplete", "true")
                && containsStringField(line,
                        "messageInputThemeVariantMatrixVersion",
                        "flow-ipc-message-input-theme-variant-matrix-v1_87")
                && containsStringField(line,
                        "messageInputThemeVariantMatrixTheme",
                        "icon-button")
                && containsLiteralField(line,
                        "messageInputThemeVariantMatrixIconButtonObserved", "true")
                && containsLiteralField(line,
                        "messageInputThemeVariantMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "messageInputThemeVariantMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.messageInputDomObserved = true;
        }
        if (containsLiteralField(line,
                "sideNavThemeVariantMatrixComplete", "true")
                && containsStringField(line,
                        "sideNavThemeVariantMatrixVersion",
                        "flow-ipc-side-nav-theme-variant-matrix-v1_88")
                && containsStringField(line,
                        "sideNavThemeVariantMatrixTheme",
                        "filled")
                && containsLiteralField(line,
                        "sideNavThemeVariantMatrixFilledObserved", "true")
                && containsLiteralField(line,
                        "sideNavThemeVariantMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "sideNavThemeVariantMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.sideNavDomObserved = true;
        }
        if (containsLiteralField(line,
                "messageListMarkdownMatrixComplete", "true")
                && containsStringField(line,
                        "messageListMarkdownMatrixVersion",
                        "flow-ipc-message-list-markdown-announce-matrix-v1_89")
                && containsLiteralField(line,
                        "messageListMarkdownMatrixMarkdownObserved", "true")
                && containsLiteralField(line,
                        "messageListMarkdownMatrixMarkdownAttributeObserved", "true")
                && containsLiteralField(line,
                        "messageListMarkdownMatrixAnnounceMessagesObserved", "true")
                && containsLiteralField(line,
                        "messageListMarkdownMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "messageListMarkdownMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.messageListMarkdownDomObserved = true;
        }
    }
}
