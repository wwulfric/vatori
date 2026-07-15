package dev.vatori.ipcsmoke;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

final class StandardBundlePushProbe {
    private StandardBundlePushProbe() {
    }

    static Span attachTo(Component owner) {
        Span status = new Span("Standard push: pending");
        status.setId("ipc-standard-push-status");

        AtomicBoolean scheduled = new AtomicBoolean();
        owner.addAttachListener(event -> {
            if (!scheduled.compareAndSet(false, true)) {
                return;
            }
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(Duration.ofMillis(600));
                    event.getUI().access(() -> status.setText("Standard push 1 READY"));
                    Thread.sleep(Duration.ofMillis(900));
                    event.getUI().access(() -> status.setText("Standard push 2 READY"));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        return status;
    }
}
