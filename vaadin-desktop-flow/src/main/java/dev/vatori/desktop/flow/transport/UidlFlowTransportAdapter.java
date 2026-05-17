package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.UidlRequestHandler;
import com.vaadin.flow.server.communication.UidlWriter;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UidlFlowTransportAdapter implements FlowTransportAdapter {
    private final UidlRequestHandler uidlRequestHandler;
    private final UidlWriter uidlWriter;

    public UidlFlowTransportAdapter() {
        this(new UidlRequestHandler(), new UidlWriter());
    }

    UidlFlowTransportAdapter(UidlRequestHandler uidlRequestHandler, UidlWriter uidlWriter) {
        this.uidlRequestHandler = Objects.requireNonNull(uidlRequestHandler, "uidlRequestHandler");
        this.uidlWriter = Objects.requireNonNull(uidlWriter, "uidlWriter");
    }

    @Override
    public FlowPatchResponse handleClientMessage(UI ui, FlowClientMessage message) throws IOException {
        Objects.requireNonNull(ui, "ui");
        Objects.requireNonNull(message, "message");

        VaadinSession session = Objects.requireNonNull(ui.getSession(), "ui.session");
        DesktopVaadinRequest request = DesktopVaadinRequest.uidl(session, message);
        DesktopVaadinResponse response = new DesktopVaadinResponse(session.getService());

        Map<Class<?>, CurrentInstance> oldInstances = CurrentInstance.getInstances();
        CurrentInstance.setCurrent(ui);
        CurrentInstance.set(VaadinRequest.class, request);
        CurrentInstance.set(VaadinResponse.class, response);
        try {
            session.lock();
            Optional<SynchronizedRequestHandler.ResponseWriter> responseWriter = uidlRequestHandler
                    .synchronizedHandleRequest(session, request, response, message.requestBody());
            session.unlock();
            if (responseWriter.isPresent()) {
                responseWriter.get().writeResponse();
            }
        } finally {
            if (session.hasLock()) {
                session.unlock();
            }
            CurrentInstance.clearAll();
            CurrentInstance.restoreInstances(oldInstances);
        }

        return new FlowPatchResponse(
                message.sessionId(),
                message.uiId(),
                response.bodyAsString(),
                response.contentType(),
                response.statusCode(),
                response.headers(),
                true,
                message.resync());
    }

    @Override
    public String createUidl(UI ui, boolean resync) {
        Objects.requireNonNull(ui, "ui");
        String uidlJson = uidlWriter.createUidl(ui, false, resync).toString();
        ui.getInternals().setLastRequestResponse(uidlJson);
        return uidlJson;
    }
}
