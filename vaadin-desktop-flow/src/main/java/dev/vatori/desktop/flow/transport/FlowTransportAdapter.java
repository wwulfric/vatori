package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.UI;

import java.io.IOException;

public interface FlowTransportAdapter {
    FlowPatchResponse handleClientMessage(UI ui, FlowClientMessage message) throws IOException;

    String createUidl(UI ui, boolean resync);

    String createUidl(UI ui, boolean async, boolean resync);
}
