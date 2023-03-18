package org.example.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.util.ByteBufPayload;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class ClientService implements RSocket {

    Logger logger = LoggerFactory.getLogger(ClientService.class);

    static String decodeRoute(ByteBuf metadata) {
        final RoutingMetadata routingMetadata = new RoutingMetadata(metadata);
        return routingMetadata.iterator().next();
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        logger.info("FnF Receiving: " + payload.getDataUtf8());
        return Mono.empty();
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        logger.info("RR Receiving: " + payload.getDataUtf8());
        return Mono.just(DefaultPayload.create("Client received your RequestResponse"));
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
        return Flux.range(-5, 10)
                .delayElements(Duration.ofMillis(500))
                .map(obj ->
                        ByteBufPayload.create(
                                ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, obj.toString())));
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return Flux.range(-5, 10)
                .delayElements(Duration.ofMillis(500))
                .map(obj ->
                        ByteBufPayload.create(
                                ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, obj.toString())));
    }
}
