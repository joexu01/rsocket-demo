package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        // 使用 Spring 启动 RSocket 服务
        SpringApplication.run(Application.class, args);

        // 也可以使用原生 RSocket Library 启动
        // See: https://docs.spring.io/spring-framework/docs/current/reference/html/rsocket.html#rsocket-annot-messagemapping

//        ApplicationContext context = ... ;
//        RSocketMessageHandler handler = context.getBean(RSocketMessageHandler.class);
//
//        CloseableChannel server =
//                RSocketServer.create(handler.responder())
//                        .bind(TcpServerTransport.create("localhost", 7000))
//                        .block();
    }
}