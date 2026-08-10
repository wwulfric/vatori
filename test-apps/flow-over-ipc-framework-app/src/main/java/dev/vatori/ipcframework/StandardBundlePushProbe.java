package dev.vatori.ipcframework;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.html.Span;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

final class StandardBundlePushProbe {
    private StandardBundlePushProbe() {
    }

    static Span attachTo(Component owner) {
        PushStatus status = new PushStatus();
        status.setId("ipc-standard-push-status");

        AtomicBoolean scheduled = new AtomicBoolean();
        owner.addAttachListener(event -> {
            if (!scheduled.compareAndSet(false, true)) {
                return;
            }
            UI ui = event.getUI();
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(Duration.ofMillis(600));
                    ui.access(() -> {
                        if (status.isAttached()) {
                            status.setText("Standard push 1 READY");
                        }
                    });
                    Thread.sleep(Duration.ofMillis(900));
                    ui.access(() -> {
                        if (status.isAttached()) {
                            status.setText("Standard push 2 READY");
                        }
                    });
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        return status;
    }

    private static final class PushStatus extends Span {
        private PushStatus() {
            super("Standard push: pending");
        }

        @ClientCallable
        public void triggerPush(String marker) {
            UI ui = getUI().orElseThrow();
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(Duration.ofMillis(200));
                    ui.access(() -> {
                        if (isAttached()) {
                            setText(marker);
                        }
                    });
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (UIDetachedException ignored) {
                    // The focused fixture can close while the delayed push is pending.
                }
            });
        }
    }
}
