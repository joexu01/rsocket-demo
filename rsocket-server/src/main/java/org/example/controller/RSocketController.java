package org.example.controller;

import org.example.dto.ServerResponse;
import org.example.dto.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

// See docs: https://docs.spring.io/spring-framework/docs/current/reference/html/rsocket.html
// Client responder: https://www.vinsguru.com/rsocket-client-responders/
@Controller
public class RSocketController {

    // 接收主动上传的状态信息 @MessageMapping -> 使用 RSocket 处理
    @MessageMapping("upload.status")
    public Mono<ServerResponse> receiveActivelyUploadStatus(Status status) {
        System.out.printf("[upload.status]Received Status from client: %s\n", status.uuid);
        return Mono.just(new ServerResponse(String.format("I received your status. The Uuid was %s.", status.uuid)));
    }

    @MessageMapping("test.echo")
    public Mono<String> simplyEcho(String data) {
        System.out.printf("[test.echo]Received echo string from client: %s\n", data);
        return Mono.just(String.format("[test.echo]I received your string: %s. Thank you.", data));
    }

    @MessageMapping("")
    public Mono<String> simplyEchoNoHandler(String data) {
        System.out.printf("No Handler: Received echo string from client: %s\n", data);
        return Mono.just(String.format("[No Handler]I received your string: %s. Thank you.", data));
    }

    @MessageMapping("upload.log")
    public void fireAndForgetHandler(@Headers Map<String, Object> header, RSocketRequester requester, String data) {
        header.forEach((k, v) -> System.out.printf("[upload.log]header key: %s, val: %s\n", k, v));
        System.out.printf("[upload.log]UploadEventLogs: Received log string from client: %s\n", data);
    }

    @Autowired
    private RequestProcessor requestProcessor;

    @MessageMapping("handler.task")
    public Mono<String> task(String request, RSocketRequester rSocketRequester) {
        System.out.printf("[handler.request]Client request: %s\n", request);
        UUID uuid = UUID.randomUUID();
        this.requestProcessor.processRequests(rSocketRequester, uuid);
        return Mono.just(uuid.toString());
    }
}
