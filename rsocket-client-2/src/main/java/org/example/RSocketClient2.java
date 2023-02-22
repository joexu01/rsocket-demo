package org.example;

import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

// https://github.com/rsocket/rsocket-java/blob/master/rsocket-examples/src/main/java/io/rsocket/examples/transport/tcp/client/RSocketClientExample.java
public class RSocketClient2 {
    public static void main(String[] args) {

        final Logger logger = LoggerFactory.getLogger(RSocketClient2.class);

        // 在 acceptor 方法中定义客户端可以被服务端调用的函数
        Mono<RSocket> source = RSocketConnector.create().acceptor(
                        SocketAcceptor.forRequestResponse(
                                payload -> {
                                    String metadataUtf8 = payload.getMetadataUtf8();
                                    String dataUtf8 = payload.getDataUtf8();

                                    byte[] respBytes = String
                                            .format("Client received your message. Maybe someday I will do as you say. Your meta is %s and data is %s.",
                                                    metadataUtf8, dataUtf8).getBytes();
                                    payload.release(); //

                                    return Mono.just(DefaultPayload.create(respBytes));
                                }
                        )
                )
//                .acceptor()
                .reconnect(Retry.backoff(50, Duration.ofMillis(500)))
                .connect(TcpClientTransport.create("127.0.0.1", 8099));

        RSocketClient client = RSocketClient.from(source);

//        client.
    }


}