package dev.vatori.desktop.spring.routefixture;

import org.springframework.stereotype.Service;

@Service
public final class RouteDiscoveryService {
    private String lastMessage;

    public String message(String route, RouteScopedLabel scopedLabel) {
        lastMessage = "Spring injected " + route + " route " + scopedLabel.value();
        return lastMessage;
    }

    public String lastMessage() {
        return lastMessage;
    }
}
