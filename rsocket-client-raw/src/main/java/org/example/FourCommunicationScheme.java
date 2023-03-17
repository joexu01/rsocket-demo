package org.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/*
运行这个示例时，rsocket-server 的 application.yaml 应该为：
spring:
  rsocket:
    server:
      port: 8099
      transport: tcp
*/


public class FourCommunicationScheme {

    static String decodeRoute(ByteBuf metadata) {
        final RoutingMetadata routingMetadata = new RoutingMetadata(metadata);
        return routingMetadata.iterator().next();
    }

    static ByteBuf encodeRoute(String route) {
        return TaggingMetadataCodec.createTaggingContent(
                ByteBufAllocator.DEFAULT,
                Collections.singletonList(route));
    }

    public static void main(String[] args) {
        final Logger logger = LoggerFactory.getLogger(RSocketClientRaw.class);

        // 随机生成 UUID 标识客户端
        UUID uuid = UUID.randomUUID();
        logger.info("My UUID is {}", uuid);
        // 生成 SETUP 阶段（建立连接时） Payload 使用的 route 信息
        ByteBuf setupRouteMetadata = encodeRoute("connect.setup");

        RSocket socket = RSocketConnector.create()
                // 设置 metadata MIME Type，方便服务端根据 MIME 类型确定 metadata 内容
                .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString())
                // SETUP 阶段的 Payload，data 里面存放 UUID
                .setupPayload(ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, uuid.toString()),
                        setupRouteMetadata))
                // 设置重连策略
                .reconnect(Retry.backoff(2, Duration.ofMillis(500)))
                .connect(
                        TcpClientTransport.create(
                                TcpClient.create()
                                        .host("127.0.0.1")
                                        .port(8099)))
                .block();


        assert socket != null;

        // 测试 Req&Resp
        ByteBuf routeMetadata = encodeRoute("test.echo.mono.5");
        Payload echoPayload = ByteBufPayload.create(
                ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a message from client using rsocket-java library."),
                routeMetadata);

        Mono<Payload> requestResponse = socket.requestResponse(echoPayload);
        requestResponse
                // 当 subscribe() 操作开始执行时打印一下日志
                .doOnSubscribe(subscription -> logger.info("Test1 subscribed to {}", subscription.toString()))
                // 当携带的请求成功后要做的事情
                .doOnSuccess(payload -> {
                    logger.info("Test1 - Successfully returned: {}", payload.getDataUtf8());
                    payload.release();
                })
                .doOnError(throwable -> logger.info("Test1 doOnError: {}", throwable.toString()))
                // 可以使用 timeout 丢弃等待超时的 Mono
                //.timeout(Duration.ofSeconds(1))
                // 可以使用 doOnTerminate 在请求结束后做一些工作
                // .doOnTerminate(() -> {})
                // 但是一定要设置 doOnError
                //.doOnError(TimeoutException.class, e -> logger.info("Test1 doOnError: {}", e.toString()))
                .onErrorReturn(TimeoutException.class, DefaultPayload.create("Payload: Test1 - timeout"))
                // 可以使用 log() 来观察数据的状态
                //.log()
                // 客户端在执行 subscribe() 操作时才会开始从服务端接收数据流
                // 在响应式编程中使用 subscribe 操作符是订阅一个数据流并处理发布的数据、错误和完成信号的核心方式之一
                .subscribe();

        // 测试 FnF
        routeMetadata = encodeRoute("upload.log");
        socket.fireAndForget(
                        ByteBufPayload.create(
                                ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a log from client using rsocket-java library."),
                                routeMetadata))
                .doOnSubscribe(subscription -> logger.info("Test2 subscribed to {}", subscription.toString()))
                .subscribe();


        // 测试 RequestStream
        routeMetadata = encodeRoute("handler.request.stream");
        Flux<Payload> requestStream = socket.requestStream(
                ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "TEST3 - Request&Stream"),
                        routeMetadata));

        requestStream
                // 当然可以使用 map 对每个 Payload 进行操作
                // .map(payload -> System.out.printf("%s\n", payload.getDataUtf8()))
                .doOnSubscribe(subscription -> logger.info("Test3 subscribed to {}", subscription.toString()))
                // 使用 doOnNext 不会对流的数据进行改变
                // doOnNext()是一个 Reactor 式流操作符，它允许编写者注册一个在每次出现新元素时执行的回调函数
                .doOnNext(nextPayload -> System.out.println("Test3 Received payload: " + nextPayload.getDataUtf8()))
                // 当需要从流中选择一些特定的元素时，可以使用 Flux.take(long n) 操作符
                // 该操作符将创建一个新的 Flux，该 Flux 包含原始 Flux 的前 n 个元素
                // take 操作符发出了指定数量的元素之后，就不再接收任何元素，并且将取消其上游发布者的订阅
                // 在这里服务端使用 Flux.range 来限定 Flux 流中的元素个数
                // 如果服务端使用 Flux.interval 生成一个无限长度的流，客户端使用 take 接收限定个数的元素
                // 便会取消发布者的订阅
                .take(5)
                .subscribe();

        // 测试 Channel
        Flux<Payload> payloadFlux = Flux.range(-5, 10)
                .delayElements(Duration.ofMillis(500))
                .map(obj ->
                {
                    ByteBuf metadata = encodeRoute("handler.request.channel");
                    return ByteBufPayload.create(
                            ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, obj.toString()), metadata);
                });

        Flux<Payload> channelResp = socket.requestChannel(payloadFlux);
        channelResp
                .doOnSubscribe(subscription -> logger.info("Test4 subscribed to {}", subscription.toString()))
                .doOnError(throwable -> logger.info(throwable.toString()))
                .doOnNext(nextPayload -> System.out.println("Test4 Received payload: " + nextPayload.getDataUtf8()))
                .subscribe();


        socket.onClose().block();
    }
}
