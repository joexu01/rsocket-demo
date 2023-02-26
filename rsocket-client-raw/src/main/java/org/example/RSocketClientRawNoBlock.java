package org.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import io.rsocket.util.DefaultPayload;
import io.rsocket.metadata.RoutingMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;


public class RSocketClientRawNoBlock {
    static String decodeRoute(ByteBuf metadata) {
        final RoutingMetadata routingMetadata = new RoutingMetadata(metadata);

        return routingMetadata.iterator().next();
    }

    // 加载 trust store
    static {
        System.setProperty("javax.net.ssl.trustStore",
                Objects.requireNonNull(
                                RSocketClientRaw.class
                                        .getClassLoader()
                                        .getResource("truststore/client.truststore"))
                        .getPath()
        );
    }

    public static void main(String[] args) {
        final Logger logger = LoggerFactory.getLogger(RSocketClientRaw.class);

        // 随机生成 UUID 标识客户端
        UUID uuid = UUID.randomUUID();
        // 生成 SETUP 阶段（建立连接时） Payload 使用的 route 信息
        ByteBuf setupRouteMetadata = TaggingMetadataCodec.createTaggingContent(
                ByteBufAllocator.DEFAULT,
                Collections.singletonList("connect.setup"));

        RSocket socket = RSocketConnector.create()
                .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString())
                // SETUP 阶段的 Payload，data 里面存放 UUID
                .setupPayload(ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, uuid.toString()),
                        setupRouteMetadata))
                .acceptor(SocketAcceptor.forRequestResponse(
                        payload -> {
                            final String route = decodeRoute(payload.sliceMetadata());
                            logger.info("[Client Acceptor] Received RequestResponse[route={}]", route);

                            String metadataUtf8 = payload.getMetadataUtf8();
                            String dataUtf8 = payload.getDataUtf8();
                            logger.info("[Client Acceptor] This Req&Resp contains data: {}, metadata: {}", dataUtf8, metadataUtf8);

                            payload.release();

                            if ("request.status.callback".equals(route)) {
                                return Mono.just(ByteBufPayload.create("Thanks for handling my task!"));
                            } else if ("request.server.call".equals(route)) {
                                return Mono.just(ByteBufPayload.create("You called my handler actively from server!"));
                            }

                            byte[] respBytes = String
                                    .format("Client received your message. Maybe someday I will do as you say. Your meta is %s and data is %s",
                                            metadataUtf8, dataUtf8).getBytes();
                            return Mono.just(DefaultPayload.create(respBytes));
                        }
                ))
                .reconnect(Retry.backoff(2, Duration.ofMillis(500)))
                .connect(
                        TcpClientTransport.create(
                                TcpClient.create()
                                        .host("127.0.0.1")
                                        .port(8099)
                                        .secure()))
                .block();

        assert socket != null;

        // 测试 Req&Resp
        ByteBuf routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("test.echo"));
        Mono<Payload> requestResponse = socket.requestResponse(
                ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a message from client using rsocket-java library."),
                        routeMetadata));

        requestResponse
                // 当携带请求的 Mono 被 Server 接收后要做的事情
                .doOnSubscribe(subscription -> logger.info("Test1 - R&R subscribed by server: {}", subscription.toString()))
                // 当携带的请求成功后要做的事情
                .doOnSuccess(payload -> {
                    logger.info("Test1 - Successfully returned: {}", payload.getDataUtf8());
                    payload.release();
                })
                .doOnError(throwable -> logger.info("Test1 doOnError - R&R returned error: {}", throwable.toString()))
                // 可以使用 timeout 丢弃等待超时的 Mono
//                .timeout(Duration.ofSeconds(1))
                // 可以使用 doOnTerminate 在请求结束后做一些工作
                // .doOnTerminate(() -> {})
                // 但是一定要设置 doOnError
                .doOnError(TimeoutException.class, e -> logger.info("Test1 doOnError: {}", e.toString()))
                .onErrorReturn(TimeoutException.class, DefaultPayload.create("Payload: Test1 - timeout"))
                // 可以使用 log() 来观察数据的状态
                // .log()
                // 订阅服务器生成的消息
                .subscribe();

        // 测试 FnF
        routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("upload.log"));
        socket.fireAndForget(
                        ByteBufPayload.create(
                                ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a log from client using rsocket-java library."),
                                routeMetadata))
                .doOnSubscribe(subscription -> logger.info("Test2 - Fire And Forget onSubscribe: {}", subscription.toString()))
                .subscribe();

        // 测试 Server request (callback)
        routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("handler.task"));
        socket.requestResponse(
                        ByteBufPayload.create(
                                ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a task request from client using rsocket-java library."),
                                routeMetadata))
                .doOnSubscribe(subscription -> logger.info("Test3 - R&R subscribed by server: {}", subscription.toString()))
                .doOnSuccess(payload -> {
                    logger.info("Test3 - Successfully returned: {}", payload.getDataUtf8());
                    payload.release();
                })
                .doOnError(throwable -> logger.info("Test3 - R&R Server returned error: {}", throwable.toString()))
                .subscribe();

        routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("test.echo"));
        Mono<Payload> requestResponse2 = socket.requestResponse(
                ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "TEST4 - Message"),
                        routeMetadata));

        requestResponse2
                // 当携带请求的 Mono 被 Server 接收后要做的事情
                .doOnSubscribe(subscription -> logger.info("Test4 - R&R subscribed by server: {}", subscription.toString()))
                // 当携带的请求成功后要做的事情
                .doOnSuccess(payload -> {
                    logger.info("Test4 - Successfully returned: {}", payload.getDataUtf8());
                    payload.release();
                })
                .doOnError(throwable -> logger.info("Test4 doOnError - R&R returned error: {}", throwable.toString()))
                .doOnError(TimeoutException.class, e -> logger.info("Test4 doOnError: {}", e.toString()))
                .onErrorReturn(TimeoutException.class, DefaultPayload.create("Payload: Test4 - timeout"))
                .subscribe();

        socket.onClose().block();
    }
}
