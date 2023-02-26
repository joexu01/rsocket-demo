package org.example.manager;

import org.springframework.messaging.rsocket.RSocketRequester;

import java.util.Date;

public class ConnectedClient {
    public RSocketRequester requester;
    public Date connectedTime;

    ConnectedClient(RSocketRequester requester) {
        this.requester = requester;
        this.connectedTime = new Date();
    }
}
