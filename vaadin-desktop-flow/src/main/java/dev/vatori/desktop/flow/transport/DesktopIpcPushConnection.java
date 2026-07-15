package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.server.communication.UidlWriter;
import com.vaadin.flow.shared.ApplicationConstants;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;
import java.util.function.Consumer;

final class DesktopIpcPushConnection implements PushConnection {
    private final UI ui;
    private final Consumer<DesktopFlowPushMessage> sender;
    private boolean connected;
    private boolean pending;
    private String connectionId = "";
    private int lastSentFlowSyncId = -1;

    DesktopIpcPushConnection(UI ui, Consumer<DesktopFlowPushMessage> sender) {
        this.ui = Objects.requireNonNull(ui, "ui");
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    @Override
    public void push() {
        if (!connected) {
            pending = true;
            return;
        }
        send(false);
    }

    void connect(String connectionId, int lastSeenServerSyncId) {
        this.connectionId = Objects.requireNonNull(connectionId, "connectionId");
        connected = true;
        if (lastSentFlowSyncId >= 0 && lastSeenServerSyncId < lastSentFlowSyncId) {
            pending = false;
            send(true);
        } else if (pending && ui.getInternals().isDirty()) {
            pending = false;
            send(false);
        } else {
            pending = false;
        }
    }

    void sendResync() {
        if (!connected) {
            pending = true;
            return;
        }
        send(true);
    }

    boolean matches(String connectionId) {
        return connected && Objects.equals(this.connectionId, connectionId);
    }

    private void send(boolean resync) {
        if (resync) {
            ui.getInternals().getStateTree().prepareForResync();
        }
        ObjectNode response = new UidlWriter().createUidl(ui, true, resync);
        lastSentFlowSyncId = response.path(ApplicationConstants.SERVER_SYNC_ID)
                .asInt(lastSentFlowSyncId);
        String uidl = response.toString();
        ui.getInternals().setLastRequestResponse(uidl);
        sender.accept(new DesktopFlowPushMessage(
                ui.getSession().getSession().getId(),
                connectionId,
                ui.getUIId(),
                uidl,
                resync));
    }

    @Override
    public void disconnect() {
        connected = false;
        pending = false;
        connectionId = "";
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
