package com.arloor.trojan.server.trojan.handler;

import com.arloor.trojan.server.Main;
import com.arloor.trojan.server.trojan.model.TrojanRequest;
import com.arloor.trojan.server.util.OsHelper;
import com.arloor.trojan.server.util.RemoteActiveHandler;
import com.arloor.trojan.server.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class TrojanConnectHandler extends SimpleChannelInboundHandler<TrojanRequest> {
    private static final Logger log = LoggerFactory.getLogger(TrojanConnectHandler.class);

    private final Bootstrap b = new Bootstrap();

    private String host;
    private int port;
    private TrojanRequest request;

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, TrojanRequest msg) {
        request = (TrojanRequest) msg;
        log.info("{}", request);
        host = request.getDst().getHost();
        port = request.getDst().getPort();
        if (!Main.passwd.equals(request.getPasswd())) {
            log.error("密码不对:{}", request.getPasswd());
            SocksServerUtils.closeOnFlush(ctx.channel());
            return;
        }
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                new FutureListener<Channel>() {
                    @Override
                    public void operationComplete(final Future<Channel> future) {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            try {
                                ctx.pipeline().remove(TrojanConnectHandler.class);
                                outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                RelayHandler relayHandler = new RelayHandler(outboundChannel);
                                ctx.pipeline().addLast(relayHandler);
                                relayHandler.channelRead(ctx, msg.getPayload());
                            } catch (Exception e) {
                                log.error("post established error: {}", e.getMessage());
                            }
                        } else {
                            log.info("reply tunnel established Failed: ");
                            SocksServerUtils.closeOnFlush(ctx.channel());
                            SocksServerUtils.closeOnFlush(outboundChannel);
                        }
                    }
                });

        // 4.连接目标网站
        final Channel inboundChannel = ctx.channel();
        b.group(inboundChannel.eventLoop())
                .channel(OsHelper.socketChannelClazz())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new RemoteActiveHandler(promise));

        b.connect(host, port).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    // Connection established use handler provided results
                } else {
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION][" + clientHostname + "] " + cause.getMessage());
        ctx.close();
    }
}
