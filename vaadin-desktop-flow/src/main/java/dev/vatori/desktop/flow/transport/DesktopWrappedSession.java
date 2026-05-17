package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.server.WrappedSession;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class DesktopWrappedSession implements WrappedSession {
    private final String id;
    private final long createdAt = System.currentTimeMillis();
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private int maxInactiveInterval = -1;
    private boolean valid = true;

    DesktopWrappedSession(String id) {
        this.id = id;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public Set<String> getAttributeNames() {
        return Set.copyOf(attributes.keySet());
    }

    @Override
    public void invalidate() {
        valid = false;
        attributes.clear();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getCreationTime() {
        assertValid();
        return createdAt;
    }

    @Override
    public long getLastAccessedTime() {
        assertValid();
        return System.currentTimeMillis();
    }

    @Override
    public boolean isNew() {
        assertValid();
        return false;
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        maxInactiveInterval = interval;
    }

    private void assertValid() {
        if (!valid) {
            throw new IllegalStateException("DesktopWrappedSession is invalidated");
        }
    }
}
