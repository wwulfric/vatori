package dev.vatori.flowipc.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class OrdersGridView {
    private List<Map<String, Object>> rows = List.of();

    List<Map<String, Object>> requestRows(int offset, int limit) {
        List<Map<String, Object>> nextRows = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            int id = offset + i + 1;
            nextRows.add(MockFlowRuntime.mapOf(
                    "id", id,
                    "customer", "Customer " + id,
                    "status", id % 2 == 0 ? "READY" : "PENDING"));
        }
        rows = List.copyOf(nextRows);
        return rows;
    }

    Map<String, Object> render() {
        return MockFlowRuntime.mapOf(
                "nodeId", "orders.grid",
                "rows", rows);
    }
}

