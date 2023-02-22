package org.example;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import javafx.application.Application;
import org.example.dto.ServerResponse;
import org.example.dto.Status;
import org.springframework.boot.autoconfigure.integration.IntegrationProperties;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

public class RSocketClientApplication {
    public static void main(String[] args) {
        boolean interceptNeeded = true;

        // RSocketStrategies 在这一步可以添加编解码器
        RSocketStrategies strategies = RSocketStrategies.builder()
                .encoders(encoders -> encoders.add(new Jackson2JsonEncoder()))
                .decoders(decoders -> decoders.add(new Jackson2JsonDecoder()))
                .build();

        // 请求构建器，指定连接需要的参数
        RSocketRequester.Builder requestBuilder = RSocketRequester.builder();
        RSocketRequester requester = requestBuilder.rsocketConnector(rSocketConnector -> rSocketConnector.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(5)))
                        .dataMimeType(MimeTypeUtils.APPLICATION_JSON_VALUE))
                .rsocketStrategies(strategies)
                .tcp("127.0.0.1", 8099);

        // 建立请求
        Mono<ServerResponse> result = requester.route("currentStatus")
                .data(new Status("988bc-ade32-ccc98-2087b", "23 ℃", "26%"))
                .retrieveMono(ServerResponse.class);

        // 业务代码
        // 根据需要拦截

        if (interceptNeeded) {
//            result.block();
            // or
            ServerResponse response = result.block(Duration.ofSeconds(20));
            // 执行业务代码...
            assert response != null;
            System.out.printf("Response: %s\n", response.respStr);
        }
    }
}