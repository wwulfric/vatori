package dev.vatori.desktop.flow.launcherfixture;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public final class LauncherContractLifecycle implements DisposableBean {
    private static final AtomicInteger CLOSED_CONTEXTS = new AtomicInteger();

    @Override
    public void destroy() {
        CLOSED_CONTEXTS.incrementAndGet();
    }

    public static int closedContexts() {
        return CLOSED_CONTEXTS.get();
    }
}
