package dev.vatori.examples.counter;

import org.springframework.stereotype.Service;

@Service
public final class CounterApplicationService {
    public String title() {
        return "Vaadin Desktop Counter";
    }

    public String details() {
        return "Created through the no-web Spring application context.";
    }
}
