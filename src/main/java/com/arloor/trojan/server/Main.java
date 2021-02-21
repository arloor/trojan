package com.arloor.trojan.server;

import com.arloor.trojan.server.util.OsHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static String port;
    public static String passwd;
    public static String pem;
    public static String key;
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        port = args[0];
        passwd = args[1];
        pem = args[2];
        key = args[3];


        EventLoopGroup bossGroup = OsHelper.buildEventLoopGroup(1);
        EventLoopGroup workerGroup = OsHelper.buildEventLoopGroup(0);
        try {
            Channel sslChannel = startSSl(bossGroup, workerGroup);
            assert sslChannel != null;
            sslChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("失败{}", "a", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static Channel startSSl(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        try {
            // Configure the server.
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            b.group(bossGroup, workerGroup)
                    .channel(OsHelper.serverSocketChannelClazz())
                    .childHandler(new TrojanServerInitializer());

            return b.bind(Integer.parseInt(port)).sync().channel();
        } catch (Exception e) {
            log.error("无法启动Https Proxy", e);
        }
        return null;
    }
}
