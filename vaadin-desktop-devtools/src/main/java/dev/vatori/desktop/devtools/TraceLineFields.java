package dev.vatori.desktop.devtools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TraceLineFields {
    private static final Pattern LAZY_GRID_FETCH_RANGE = Pattern.compile(
            "Lazy grid fetch\\b[^\\r\\n]*?\\b([0-9]+-[0-9]+)\\b");
    private static final Pattern LAZY_GRID_REPORT_RANGE = Pattern.compile(
            "Lazy grid fetches:[^\\r\\n]*?range: ([0-9]+-[0-9]+)");

    private TraceLineFields() {
    }

    static String normalize(String line) {
        String trimmed = line.trim();
        String payload = trimmed.startsWith("HOST_PROTOCOL ") ? trimmed.substring("HOST_PROTOCOL ".length()) : trimmed;
        return payload.replace("\\/", "/");
    }

    static String first(Pattern pattern, String line, String fallback) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    static boolean containsLiteralField(String line, String field, String value) {
        return line.contains("\"" + field + "\":" + value)
                || line.contains("\\\"" + field + "\\\":" + value);
    }

    static boolean containsBooleanField(String line, String field) {
        return containsLiteralField(line, field, "true")
                || containsLiteralField(line, field, "false");
    }

    static boolean isBootstrapResponse(String line) {
        return line.contains("\"type\":\"bootstrapResponse\"");
    }

    static boolean isBootstrapComponentResourceResponse(String line) {
        return isBootstrapResponse(line)
                && line.contains("\"componentFrontendResources\"");
    }

    static boolean isResourceRequest(String line) {
        return line.contains("\"type\":\"resourceRequest\"");
    }

    static boolean isResourceResponse(String line) {
        return line.contains("\"type\":\"resourceResponse\"");
    }

    static boolean isResourceStageLine(String line) {
        return line.contains("\"type\":\"resourceStage\"");
    }

    static boolean isWebviewDom(String line) {
        return line.contains("\"type\":\"webview.dom\"");
    }

    static boolean isRedactedResourceResponse(String line) {
        return isResourceResponse(line)
                && line.contains("\"bodyRedacted\":true");
    }

    static boolean containsTrueFields(String line, String... fields) {
        return containsLiteralFields(line, "true", fields);
    }

    static boolean containsFalseFields(String line, String... fields) {
        return containsLiteralFields(line, "false", fields);
    }

    static boolean containsAnyTrueField(String line, String... fields) {
        return containsAnyLiteralField(line, "true", fields);
    }

    static boolean containsAll(String line, String... fragments) {
        for (String fragment : fragments) {
            if (!line.contains(fragment)) {
                return false;
            }
        }
        return true;
    }

    static boolean containsStringField(String line, String field, String value) {
        String slashEscapedValue = value.replace("/", "\\/");
        return line.contains("\"" + field + "\":\"" + value + "\"")
                || line.contains("\"" + field + "\":\"" + slashEscapedValue + "\"")
                || line.contains("\\\"" + field + "\\\":\\\"" + value + "\\\"")
                || line.contains("\\\"" + field + "\\\":\\\"" + slashEscapedValue + "\\\"");
    }

    static boolean containsStringFieldPrefix(String line, String field, String valuePrefix) {
        String slashEscapedValuePrefix = valuePrefix.replace("/", "\\/");
        return line.contains("\"" + field + "\":\"" + valuePrefix)
                || line.contains("\"" + field + "\":\"" + slashEscapedValuePrefix)
                || line.contains("\\\"" + field + "\\\":\\\"" + valuePrefix)
                || line.contains("\\\"" + field + "\\\":\\\"" + slashEscapedValuePrefix);
    }

    static boolean containsPath(String line, String path) {
        return line.contains(path) || line.contains(path.replace("/", "\\/"));
    }

    static boolean containsPositiveIntegerField(String line, String field) {
        for (int digit = 1; digit <= 9; digit++) {
            if (line.contains("\"" + field + "\":" + digit)
                    || line.contains("\\\"" + field + "\\\":" + digit)) {
                return true;
            }
        }
        return false;
    }

    static boolean containsClickedText(String line) {
        for (int digit = 1; digit <= 9; digit++) {
            if (line.contains("Clicks: " + digit)) {
                return true;
            }
        }
        return false;
    }

    static String lazyGridFetchRange(String line) {
        Matcher matcher = LAZY_GRID_FETCH_RANGE.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }

    static String lazyGridReportRange(String line) {
        Matcher matcher = LAZY_GRID_REPORT_RANGE.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }

    static boolean containsLazyGridReportRange(String line) {
        return !lazyGridReportRange(line).isEmpty();
    }

    static boolean containsClickedStatusField(String line, String field) {
        for (int digit = 1; digit <= 9; digit++) {
            String prefix = "Clicks: " + digit;
            if (line.contains("\"" + field + "\":\"" + prefix)
                    || line.contains("\\\"" + field + "\\\":\\\"" + prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsLiteralFields(String line, String value, String... fields) {
        for (String field : fields) {
            if (!containsLiteralField(line, field, value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAnyLiteralField(String line, String value, String... fields) {
        for (String field : fields) {
            if (containsLiteralField(line, field, value)) {
                return true;
            }
        }
        return false;
    }
}
