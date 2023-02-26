package org.example.dto;

import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class ServerResponse {
    public String respStr;

    public ServerResponse() {
    }

    public ServerResponse(String resp) {
        this.respStr = resp;
    }
}
