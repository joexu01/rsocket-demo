package org.example.controller;

import org.example.dto.ServerResponse;
import org.example.dto.Status;
import org.example.manager.ConnectedClientsManager;
import org.example.service.MathService;
import org.example.service.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Duration;
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
        clientsManager.putClientRequester(data, rSocketRequester);
//        return Mono.just(String.format("Connection established: %s.", data));
        return Mono.empty();
    }

    @MessageMapping("test.echo")
    public Mono<String> simplyEcho(String data) throws InterruptedException {
//        Thread.sleep(3000);
        logger.info("[test.echo]Received echo string from client: {}", data);
        return Mono.just(String.format("[test.echo]I received your string: %s. Thank you.", data));
    }

    @MessageMapping("test.echo.block")
    public Mono<String> blockEcho(String data) throws InterruptedException {
        Thread.sleep(300000);
        logger.info("[test.echo.block]Received echo string from client: {}", data);
        return Mono.just(String.format("[test.echo.block]I received your string: %s. Thank you.", data));
    }

    @MessageMapping("test.echo.mono")
    public Mono<String> monoEcho(Mono<String> data) throws InterruptedException {
        return data.delayElement(Duration.ofMillis(3000)).map(s -> {
            return String.format("[test.echo.mono]I received your string: %s. Thanks!", s);
        });
    }

    @MessageMapping("test.echo.mono.5")
    public Mono<String> monoEcho5(Mono<String> data) throws InterruptedException {
        return data.delayElement(Duration.ofMillis(5000)).map(s -> {
            return String.format("[test.echo.mono]I received your string: %s. Thanks!", s);
        });
    }

    @MessageMapping("")
    public Mono<String> simplyEchoNoHandler(@Headers Map<String, Object> header, String data) {
        header.forEach((k, v) -> System.out.printf("[No Handler]header key: %s, val: %s\n", k, v));
        logger.info("No Handler: Received echo string from client: {}", data);
        return Mono.just(String.format("[No Handler]I received your string: %s. Thank you.", data));
    }

    @MessageMapping("upload.log")
    public void fireAndForgetHandler(@Headers Map<String, Object> header, RSocketRequester requester, String data) {
        header.forEach((k, v) -> System.out.printf("[upload.log]header key: %s, val: %s\n", k, v));
        System.out.printf("[upload.log]UploadEventLogs: Received log string from client: %s\n", data);
    }


    // 接收主动上传的状态信息 @MessageMapping -> 使用 RSocket 处理
    @MessageMapping("upload.status")
    public Mono<ServerResponse> receiveActivelyUploadStatus(Status status) {
        logger.info("[upload.status]Received Status from client: {}", status.uuid);
        return Mono.just(new ServerResponse(String.format("I received your status. The Uuid was %s.", status.uuid)));
    }


    @Autowired
    private RequestProcessor requestProcessor;

    @MessageMapping("handler.task")
    public Mono<String> task(String request, RSocketRequester rSocketRequester) {
       logger.info("[handler.request]Client request: {}", request);
        UUID uuid = UUID.randomUUID();
        this.requestProcessor.processRequests(rSocketRequester, uuid);
        return Mono.just(uuid.toString());
    }

    @MessageMapping("handler.request.stream")
    public Flux<String> responseStreaming(Mono<String> request) {
        request
                .doOnNext(s -> logger.info("[handler.request.stream]: {}", s))
                // 使用 then() 结束操作链
                .then()
                .subscribe();
        // 事实上，严格按照响应式编程的策略，这里应该直接对 Mono 进行操作，可以使用 flatMapMany()
        // flatMapMany() 操作符可以把生成的数据流通过异步方式处理，扩展出新的数据流
        // 一个示例：
        // Mono.just(3)
        //    .flatMapMany(i -> Flux.range(0, i))
        //    .subscribe(System.out::println);
        // 就是这样
        return Flux
                .range(1, 10)
                .map(idx -> String.format("Resp from Server: %s, Thank you!", idx));
    }

    @Autowired
    private MathService mathService;

    @MessageMapping("handler.request.channel")
    public Flux<String> responseChannel(Flux<String> payloads) {
        return this.mathService.doubleInteger(payloads);
    }
}
