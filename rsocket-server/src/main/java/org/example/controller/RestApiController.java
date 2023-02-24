package org.example.controller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.util.ByteBufPayload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;

@RestController
@RequestMapping("/api")
public class RestApiController {
    @GetMapping("/cool")
    public String returnCool() {
        RSocketRequester requester = RSocketController.clientsManager.getClientRequester("123");
        ByteBuf routeMetadata = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList("request.server.call"));

        Mono.just("Server is calling you.")
//                .delayElement(Duration.ofSeconds(ThreadLocalRandom.current().nextInt(5, 10)))
                .flatMap(m -> requester.rsocketClient().requestResponse(
                                        Mono.just(ByteBufPayload.create(
                                                ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "This is a message from server using spring-stack."),
                                                routeMetadata)))
                                .doOnError(throwable -> {
                                    System.out.printf("[Error When Call]%s", throwable.toString());
                                ;})
                                .doOnSuccess(p -> System.out.printf("[test.connect.requester]Received from client: %s.", p.getDataUtf8()))
//                        .route("request.status.callback")
//                        .data(m)
//                                .send()
                )
                .subscribe();
        return "cool";
    }
}
