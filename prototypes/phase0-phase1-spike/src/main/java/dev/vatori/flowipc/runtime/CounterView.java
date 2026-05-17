package dev.vatori.flowipc.runtime;

import java.util.Map;

final class CounterView {
    private int value;

    int onIncrementClicked() {
        value++;
        return value;
    }

    int value() {
        return value;
    }

    Map<String, Object> render() {
        return MockFlowRuntime.mapOf(
                "buttonId", "counter.increment",
                "valueId", "counter.value",
                "value", value);
    }
}

