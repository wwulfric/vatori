package dev.vatori.desktop.devtools;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class AcceptanceGateOptions {
    private static final String GATE_NAME_PREFIX = "flow_ipc_";
    private static final String CLI_OPTION_PREFIX = "--require-";
    private static final Map<String, AcceptanceGate> BY_OPTION = buildOptionIndex();

    private AcceptanceGateOptions() {
    }

    static AcceptanceGate fromOption(String arg) {
        return BY_OPTION.get(arg);
    }

    static String usageOptions() {
        return String.join("|", BY_OPTION.keySet());
    }

    static String option(AcceptanceGate gate) {
        return CLI_OPTION_PREFIX + optionNameSuffix(gate);
    }

    private static String optionNameSuffix(AcceptanceGate gate) {
        String lowerName = gate.name().toLowerCase(Locale.ROOT);
        if (!lowerName.startsWith(GATE_NAME_PREFIX)) {
            throw new IllegalStateException("Unsupported acceptance gate name: " + gate.name());
        }
        String versionAndSuffix = lowerName.substring(GATE_NAME_PREFIX.length());
        int versionSeparator = versionAndSuffix.indexOf('_');
        if (versionSeparator < 0) {
            return "flow-ipc-" + versionAndSuffix;
        }
        int suffixSeparator = versionAndSuffix.indexOf('_', versionSeparator + 1);
        if (suffixSeparator < 0) {
            return "flow-ipc-" + versionAndSuffix;
        }
        String version = versionAndSuffix.substring(0, suffixSeparator);
        String suffix = versionAndSuffix.substring(suffixSeparator + 1).replace('_', '-');
        return "flow-ipc-" + version + "-" + suffix;
    }

    private static Map<String, AcceptanceGate> buildOptionIndex() {
        Map<String, AcceptanceGate> index = new LinkedHashMap<>();
        for (AcceptanceGate gate : AcceptanceGate.values()) {
            String option = option(gate);
            AcceptanceGate duplicate = index.put(option, gate);
            if (duplicate != null) {
                throw new IllegalStateException(
                        "Duplicate acceptance gate option: " + option);
            }
        }
        return Collections.unmodifiableMap(index);
    }
}
