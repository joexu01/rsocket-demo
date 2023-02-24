package org.example.controller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.util.ByteBufPayload;
import org.example.dto.ServerResponse;
import org.example.dto.Status;
import org.example.manager.ConnectedClientsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

// See docs: https://docs.spring.io/spring-framework/docs/current/reference/html/rsocket.html
// Client responder: https://www.vinsguru.com/rsocket-client-responders/
@Controller
public class RSocketController {

    private static Logger logger = LoggerFactory.getLogger(RSocketController.class);

    /*
        初始化客户端管理 HashMap，将每一个连接到 Server 的 Client 的 RSocketRequester 保存起来
        Map 的 Key 是连接 SETUP 阶段 Client 发送的 uuid
     */
    public static ConnectedClientsManager clientsManager;

    @Autowired
    private void initializeClientsManager() {
        clientsManager = new ConnectedClientsManager();
    }

    // 对到来的连接做一些处理
    @ConnectMapping("connect.setup")
    public Mono<Void> setup(String data, RSocketRequester rSocketRequester) {
        logger.info("[connect.setup]Client connection: {}\n", data);
        clientsManager.putClientRequester("123", rSocketRequester);
//        return Mono.just(String.format("Connection established: %s.", data));
        return Mono.empty();
    }

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


    @MessageMapping("test.connect.requester")
    public Mono<String> testRequester(String data) {
        System.out.printf("[test.connect.requester]Received echo string from client: %s\n", data);

        // Test rSocket Requester
        RSocketRequester requester = clientsManager.getClientRequester("123");

        // 注意 Metadata 的 Route
        ByteBuf routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("request.server.call"));

        if (requester != null) {
            Mono.just("Server is calling you.")
//                .delayElement(Duration.ofSeconds(ThreadLocalRandom.current().nextInt(5, 10)))
                    .flatMap(m -> requester.rsocketClient().requestResponse(
                                            Mono.just(ByteBufPayload.create(
                                                    ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a message from server using spring-stack."),
                                                    routeMetadata)))
                                    .doOnSuccess(p -> System.out.printf("[test.connect.requester]Received from client: %s.", p.getDataUtf8()))
//                        .route("request.status.callback")
//                        .data(m)
//                                .send()
                    )
                    .subscribe();
        }
        return Mono.just(String.format("[test.connect.requester]I received your string: %s. I will call your handler.", data));
    }
}
