package org.example.controller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.util.ByteBufPayload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

@Service
public class RequestProcessor {
    public void processRequests(RSocketRequester rSocketRequester, UUID uuid) {
        System.out.println("I'm handling this!");
        ByteBuf routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("request.status.callback"));

        Mono.just("Your request " + uuid + "  is completed")
//                .delayElement(Duration.ofSeconds(ThreadLocalRandom.current().nextInt(5, 10)))
                .flatMap(m -> rSocketRequester.rsocketClient().requestResponse(
                                        Mono.just(ByteBufPayload.create(
                                                ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a message from client using rsocket-java libaray."),
                                                routeMetadata)))
                                .doOnSuccess(p -> System.out.println(p.getDataUtf8()))
//                        .route("request.status.callback")
//                        .data(m)
//                                .send()
                )
                .subscribe();
    }
}
