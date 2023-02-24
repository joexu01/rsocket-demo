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
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.ByteBufPayload;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;

public class RSocketClientWS {
    static String decodeRoute(ByteBuf metadata) {
        final RoutingMetadata routingMetadata = new RoutingMetadata(metadata);

        return routingMetadata.iterator().next();
    }

    public static void main(String[] args) throws InterruptedException, NoSuchAlgorithmException {
        final Logger logger = LoggerFactory.getLogger(RSocketClientRaw.class);

        ByteBuf setupRouteMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("connect.setup"));

        RSocket socket = RSocketConnector.create()
//                .acceptor()
                .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString())
                // 建立连接时的 Payload
                .setupPayload(ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a message from client using rsocket-java libaray."),
                        setupRouteMetadata))
                .acceptor(SocketAcceptor.forRequestResponse(
                        payload -> {
                            final String route = decodeRoute(payload.sliceMetadata());
                            logger.info("Received RequestResponse[route={}]", route);

                            String metadataUtf8 = payload.getMetadataUtf8();
                            String dataUtf8 = payload.getDataUtf8();
                            logger.info("This Req&Resp contains data: {}, metadata: {}.", dataUtf8, metadataUtf8);

                            payload.release();

                            if ("request.status.callback".equals(route)) {
                                return Mono.just(ByteBufPayload.create("Thanks for handling my task!"));
                            } else if ("request.server.call".equals(route)) {
                                return Mono.just(ByteBufPayload.create("You called my handler actively from server."));
                            }

                            byte[] respBytes = String
                                    .format("Client received your message. Maybe someday I will do as you say. Your meta is %s and data is %s.",
                                            metadataUtf8, dataUtf8).getBytes();
                            return Mono.just(DefaultPayload.create(respBytes));
                        }
                ))
                .reconnect(Retry.backoff(50, Duration.ofMillis(500)))
                .connect(WebsocketClientTransport.create(URI.create("ws://127.0.0.1:8099")))
//                .connect(TcpClientTransport.create("127.0.0.1", 8099))
                .block();

        // 测试 Req&Resp
        ByteBuf routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("test.echo"));

        assert socket != null;
        Mono<Payload> requestResponse = socket.requestResponse(
                ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a message from client using rsocket-java libaray."),
                        routeMetadata));

        Payload payload1 = requestResponse.block();
        assert payload1 != null;
        System.out.println(payload1.getDataUtf8());

        // 测试 FnF
        routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("upload.log"));
        Mono<Void> fireAndForget = socket.fireAndForget(
                ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a log from client using rsocket-java libaray."),
                        routeMetadata));

        fireAndForget.block();

        // 测试 Server request (callback)
        routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("handler.task"));
        Mono<Payload> taskResp = socket.requestResponse(
                ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a task request from client using rsocket-java libaray."),
                        routeMetadata));

        System.out.println(taskResp.block().getDataUtf8());

        // 测试 server 保存的 requester 是否能正常调用 client 函数  test.connect.requester
        routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("test.connect.requester"));
        Mono<Payload> serverResp = socket.requestResponse(
                ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "Requesting test.connect.requester / This is a message from client using rsocket-java libaray."),
                        routeMetadata));

        System.out.println(serverResp.block().getDataUtf8());

        socket.onClose().block();

    }


}
