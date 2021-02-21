package com.arloor.trojan.server;

import com.arloor.trojan.server.util.SslContextFactory;
import com.arloor.trojan.server.trojan.handler.TrojanConnectHandler;
import com.arloor.trojan.server.trojan.handler.TrojanRequestDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TrojanServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public TrojanServerInitializer() throws IOException, GeneralSecurityException {
        this.sslCtx=SslContextFactory.getSSLContext(Main.pem,Main.key);
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();

        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }

        p.addLast(new TrojanRequestDecoder());
//        p.addLast(new LoggingHandler(LogLevel.INFO));
        p.addLast(new TrojanConnectHandler());
    }
}
