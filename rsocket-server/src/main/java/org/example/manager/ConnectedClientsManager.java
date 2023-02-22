package org.example.manager;

import org.springframework.messaging.rsocket.RSocketRequester;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectedClientsManager {
    private final ConcurrentHashMap<String, RSocketRequester> clients;

    public ConnectedClientsManager() {
        this.clients = new ConcurrentHashMap<>();
    }

    public Set<String> getAllClientIdentifier() {
        return this.clients.keySet();
    }

    public RSocketRequester getClientRequester(String clientIdentifier) {
        return this.clients.get(clientIdentifier);
    }

    public RSocketRequester putClientRequester(String clientIdentifier, RSocketRequester requester) {
        return this.clients.put(clientIdentifier, requester);
    }

    public RSocketRequester removeClientRequester(String clientIdentifier, RSocketRequester requester) {
        return this.clients.remove(clientIdentifier);
    }
}
