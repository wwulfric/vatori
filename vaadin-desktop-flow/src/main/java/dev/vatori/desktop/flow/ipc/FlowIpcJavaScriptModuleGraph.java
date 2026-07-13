package dev.vatori.desktop.flow.ipc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FlowIpcJavaScriptModuleGraph {
    private static final Pattern BARE_MODULE_IMPORT = Pattern.compile(
            "(?m)^\\s*import\\s+(?:[^'\";]+\\s+from\\s+)?['\"](?!\\.|/|[a-zA-Z][a-zA-Z0-9+.-]*:)([^'\"]+)['\"]");

    private FlowIpcJavaScriptModuleGraph() {
    }

    static List<String> bareModuleImportSpecifiers(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        Matcher matcher = BARE_MODULE_IMPORT.matcher(body);
        List<String> specifiers = new ArrayList<>();
        while (matcher.find()) {
            String specifier = matcher.group(1);
            if (specifier != null && !specifier.isBlank() && !specifiers.contains(specifier)) {
                specifiers.add(specifier);
            }
        }
        return specifiers;
    }

    static List<String> csvValues(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isBlank() && !values.contains(trimmed)) {
                values.add(trimmed);
            }
        }
        return values;
    }
}
