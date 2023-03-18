package org.example.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.util.ByteBufPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RequestProcessor.class);

    public void processRequests(RSocketRequester rSocketRequester, UUID uuid) {
        logger.info("[RequestProcessor.processRequests]I'm handling this!");
        ByteBuf routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("request.status.callback"));

        Mono.just("Your request " + uuid + "  is completed")
                .delayElement(Duration.ofSeconds(ThreadLocalRandom.current().nextInt(1, 5)))
                .flatMap(
                        m -> rSocketRequester.rsocketClient()
                                .requestResponse(
                                        Mono.just(ByteBufPayload.create(
                                                ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT,
                                                        String.format("[TASK %s]This is a task result from server using spring.", uuid)),
                                                routeMetadata
                                        )))
                                .doOnSuccess(p -> logger.info("[RequestProcessor.processRequests]Received from client: {}", p.getDataUtf8()))
                )
                .subscribe();
    }
}
