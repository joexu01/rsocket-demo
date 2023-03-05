package org.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/*
运行这个示例时，rsocket-server 的 application.yaml 应该为：
spring:
  rsocket:
    server:
      port: 8099
      transport: tcp
*/


public class ConnectionSetup {

    public static void main(String[] args) {
        final Logger logger = LoggerFactory.getLogger(RSocketClientRaw.class);

        // 随机生成 UUID 标识客户端
        UUID uuid = UUID.randomUUID();
        logger.info("My UUID is {}", uuid);
        // 生成 SETUP 阶段（建立连接时） Payload 使用的 route 信息
        ByteBuf setupRouteMetadata = TaggingMetadataCodec.createTaggingContent(
                ByteBufAllocator.DEFAULT,
                Collections.singletonList("connect.setup"));

        RSocket socket = RSocketConnector.create()
                // 设置 metadata MIME Type，方便服务端根据 MIME 类型确定 metadata 内容
                .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString())
                // SETUP 阶段的 Payload，data 里面存放 UUID
                .setupPayload(ByteBufPayload.create(
                        ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, uuid.toString()),
                        setupRouteMetadata))
                // 设置重连策略
                .reconnect(Retry.backoff(2, Duration.ofMillis(500)))
                .connect(
                        TcpClientTransport.create(
                                TcpClient.create()
                                        .host("127.0.0.1")
                                        .port(8099)))
                .block();

        assert socket != null;

        socket.onClose().block();
    }
}
