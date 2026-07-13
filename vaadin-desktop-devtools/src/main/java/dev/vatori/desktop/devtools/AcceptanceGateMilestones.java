package dev.vatori.desktop.devtools;

final class AcceptanceGateMilestones {
    private static final String GATE_NAME_PREFIX = "FLOW_IPC_V1";

    private AcceptanceGateMilestones() {
    }

    static boolean between(AcceptanceGate gate, int first, int last) {
        int milestone = number(gate);
        return milestone >= first && milestone <= last;
    }

    static boolean between(AcceptanceGate gate, AcceptanceGate first, AcceptanceGate last) {
        return between(gate, number(first), number(last));
    }

    static int number(AcceptanceGate gate) {
        String name = gate.name();
        if (!name.startsWith(GATE_NAME_PREFIX)) {
            throw new IllegalArgumentException("Unsupported acceptance gate name: " + name);
        }
        if (name.length() == GATE_NAME_PREFIX.length()) {
            return 1;
        }
        int start = GATE_NAME_PREFIX.length() + 1;
        int end = start;
        while (end < name.length() && Character.isDigit(name.charAt(end))) {
            end++;
        }
        if (end == start) {
            throw new IllegalArgumentException("Unsupported acceptance gate name: " + name);
        }
        return Integer.parseInt(name.substring(start, end));
    }
}
