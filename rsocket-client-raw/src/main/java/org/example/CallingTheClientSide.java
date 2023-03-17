package org.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
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
server:
  port: 8080
*/


public class CallingTheClientSide {

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

                .acceptor(SocketAcceptor.forRequestResponse(
                        payload -> {
                            String route = decodeRoute(payload.sliceMetadata());
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

                .acceptor(SocketAcceptor.forFireAndForget(
                        payload -> {
                            String route = decodeRoute(payload.sliceMetadata());
                            logger.info("[Client Acceptor] Received RequestResponse[route={}]", route);

                            String metadataUtf8 = payload.getMetadataUtf8();
                            String dataUtf8 = payload.getDataUtf8();
                            logger.info("[Client Acceptor] This Req&Resp contains data: {}, metadata: {}", dataUtf8, metadataUtf8);

                            payload.release();

                            return Mono.empty();
                        }
                ))

                // 设置重连策略
                .reconnect(Retry.backoff(2, Duration.ofMillis(500)))
                .connect(
                        TcpClientTransport.create(
                                TcpClient.create()
                                        .host("127.0.0.1")
                                        .port(8099)))
                .block();


        assert socket != null;

        // 测试 Server request (callback)
        ByteBuf routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("handler.task"));
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

        socket.onClose().block();
    }
}
