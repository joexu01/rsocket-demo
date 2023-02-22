package org.example.client;

import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class RSocketClientExample {
    public static void main(String[] args) throws InterruptedException {
        
        Logger logger = LoggerFactory.getLogger("RSocket.Client.ID[Test]");

        Mono<RSocket> rSocketMono =
                RSocketConnector.create()
                        .setupPayload(DefaultPayload.create("Test"))
                        .acceptor(
                                SocketAcceptor.forFireAndForget(
                                        p -> {
                                            logger.info("Received Processed Task[{}]", p.getDataUtf8());
                                            p.release();
                                            return Mono.empty();
                                        }))
                        .connect(TcpClientTransport.create(9991));

        RSocket rSocketRequester1 = rSocketMono.block();

        for (int i = 0; i < 10; i++) {
            rSocketRequester1.fireAndForget(DefaultPayload.create("task" + i)).block();
        }

        Thread.sleep(4000);

        rSocketRequester1.dispose();
        logger.info("Disposed");

        Thread.sleep(4000);

        RSocket rSocketRequester2 = rSocketMono.block();

        logger.info("Reconnected");

        Thread.sleep(10000);
    }

    static class Task {
        final String id;
        final String content;

        Task(String id, String content) {
            this.id = id;
            this.content = content;
        }
    }
}
