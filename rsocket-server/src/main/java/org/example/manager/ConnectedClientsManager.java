package org.example.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectedClientsManager {
    private static Logger logger = LoggerFactory.getLogger(ConnectedClientsManager.class);
    public final ConcurrentHashMap<String, ConnectedClient> clients;

    public ConnectedClientsManager() {
        this.clients = new ConcurrentHashMap<>();
    }

    public Set<String> getAllClientIdentifier() {
        return this.clients.keySet();
    }

    public RSocketRequester getClientRequester(String clientIdentifier) {
        return this.clients.get(clientIdentifier).requester;
    }

    public void putClientRequester(String clientIdentifier, RSocketRequester requester) {
        // Reference: https://github.com/vinsguru/rsocket-course/blob/master/spring-rsocket/src/main/java/com/vinsguru/springrsocket/service/MathClientManager.java
        requester.rsocket()
                .onClose()
                .doFirst(() -> this.clients.put(clientIdentifier, new ConnectedClient(requester)))
                .doFinally(sig -> {
                    logger.info("Client closed, uuid is {}. signal is {}.", clientIdentifier, sig.toString());
                    this.clients.remove(clientIdentifier);
                }).subscribe();
    }

    public void removeClientRequester(String clientIdentifier) {
        this.clients.remove(clientIdentifier);
    }
}
