package com.arloor.trojan.server.trojan.handler;

import com.arloor.trojan.server.Main;
import com.arloor.trojan.server.trojan.enums.ATYP;
import com.arloor.trojan.server.trojan.model.DstWithLength;
import com.arloor.trojan.server.trojan.model.TrojanRequest;
import com.arloor.trojan.server.util.OsHelper;
import com.arloor.trojan.server.util.RemoteActiveHandler;
import com.arloor.trojan.server.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DatagramDnsResponseDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class TrojanUdpConnectHandler extends SimpleChannelInboundHandler<TrojanRequest> {
    private static final Logger log = LoggerFactory.getLogger(TrojanUdpConnectHandler.class);

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, TrojanRequest request) throws InterruptedException {
        log.info("{}", request);
        String host = request.getDst().getHost();
        int port = request.getDst().getPort();
        ATYP udpAtype = ((DstWithLength) request.getDst()).getUdpAtype();
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
                            log.info("promise connect success");
                            try {
                                ctx.pipeline().remove(TrojanUdpConnectHandler.class);
//                                outboundChannel.pipeline().addLast(new DatagramDnsResponseDecoder());
                                outboundChannel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                                // 这里要改
                                outboundChannel.pipeline().addLast(new UdpRelayHandler(ctx.channel(), host, port, udpAtype));
                                UdpRelayHandler relayHandler = new UdpRelayHandler(outboundChannel, host, port, udpAtype);
                                ctx.pipeline().addLast(relayHandler);
                                relayHandler.channelRead(ctx, request);
                            } catch (Exception e) {
                                log.error("post established error", e);
                            }
                        } else {
                            log.info("reply tunnel established Failed: ");
                            SocksServerUtils.closeOnFlush(ctx.channel());
                            SocksServerUtils.closeOnFlush(outboundChannel);
                        }
                    }
                });
        final Bootstrap b = new Bootstrap();
        final Channel inboundChannel = ctx.channel();
        b.group(inboundChannel.eventLoop())
                .channel(OsHelper.isMac()?KQueueDatagramChannel.class:OsHelper.isUnix()?EpollDatagramChannel.class: NioDatagramChannel.class)
                .handler(new RemoteActiveHandler(promise));
        b.bind(0).addListener(future -> {
            if (future.isSuccess()) {
//                log.info("connect success");
            } else {
                log.error("connect error", future.cause());
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
