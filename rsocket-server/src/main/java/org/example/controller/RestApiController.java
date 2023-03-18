package org.example.controller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.util.ByteBufPayload;
import org.example.dto.ConnectedClientDto;
import org.example.dto.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class RestApiController {
    private static Logger logger = LoggerFactory.getLogger(RestApiController.class);

    @ResponseBody
    @GetMapping("/client/list")
    public List<ConnectedClientDto> clientsInfo() {
        List<ConnectedClientDto> info = new ArrayList<>();
        RSocketController.clientsManager.clients.forEach((key, value) -> {
            info.add(new ConnectedClientDto(key, value.connectedTime));
        });

        return info;
    }

    // http://127.0.0.1:8080/api/client/call?clientRoute=request.server.call&clientUUID=3bfb483e-1d34-4eba-aeb7-00cd67793463
    @GetMapping("/client/call")
    public ServerResponse callFromServer(String clientRoute, String clientUUID) {
        RSocketRequester requester = RSocketController.clientsManager.getClientRequester(clientUUID);
        if (requester == null) {
            return new ServerResponse("failed: client rSocket has closed.");
        }
        ByteBuf routeMetadata = TaggingMetadataCodec
                .createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList(clientRoute));

        Mono.just("Server is calling you.")
//                .delayElement(Duration.ofSeconds(ThreadLocalRandom.current().nextInt(5, 10)))
                .flatMap(m -> requester.rsocketClient().requestResponse(
                                Mono.just(
                                        ByteBufPayload.create(
                                                ByteBufUtil.writeUtf8(
                                                        ByteBufAllocator.DEFAULT,
                                                        "This is a message from server using spring-stack."),
                                                routeMetadata)))
                        .doOnSubscribe(subscription -> logger.info("subscribed."))
                        .doOnError(throwable -> logger.error("Error when calling client: {}", throwable.toString()))
                        .doOnSuccess(p -> logger.info("[test.connect.requester]Received from client: {}.", p.getDataUtf8()))
                )
                .subscribe();
        return new ServerResponse(String.format("request from server has sent to the client %s.", clientUUID));
    }
}
