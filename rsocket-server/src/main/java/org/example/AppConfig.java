package org.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

// Reference:
// https://docs.spring.io/spring-integration/reference/html/rsocket.html#rsocket
@Configuration
@EnableAsync
@ComponentScan(basePackages = {"org.example.controller", "org.example.dto", "org.example.manager",})
public class AppConfig {
    @Bean
    public RSocketMessageHandler rsocketMessageHandler() {
        RSocketMessageHandler handler = new RSocketMessageHandler();
        handler.setRouteMatcher(new PathPatternRouteMatcher());
        return handler;
    }

    @Bean
    public RSocketStrategies rsocketStrategies() {
        return RSocketStrategies.builder()
                .encoders(encoders -> encoders.add(new Jackson2CborEncoder()))
                .decoders(decoders -> decoders.add(new Jackson2CborDecoder()))
                .routeMatcher(new PathPatternRouteMatcher())
                .build();
    }
}
