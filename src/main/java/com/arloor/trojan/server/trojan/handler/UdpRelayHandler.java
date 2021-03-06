package com.arloor.trojan.server.trojan.handler;


import com.arloor.trojan.server.trojan.enums.ATYP;
import com.arloor.trojan.server.trojan.model.TrojanRequest;
import com.arloor.trojan.server.util.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public final class UdpRelayHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(UdpRelayHandler.class);

    private final Channel relayChannel;
    private final String host;
    private final int port;
    private final ATYP atyp;

    public UdpRelayHandler(Channel relayChannel, String host, int port, ATYP atyp) {
        this.relayChannel = relayChannel;
        this.host = host;
        this.port = port;
        this.atyp = atyp;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        boolean canWrite = ctx.channel().isWritable();
        relayChannel.config().setAutoRead(canWrite);
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {

        if (relayChannel.isActive()) {
            if (msg instanceof TrojanRequest) {
                ByteBuf buf = ((TrojanRequest) msg).getPayload();
                relayChannel.writeAndFlush(new DatagramPacket(buf, new InetSocketAddress(host, port))).sync();
            } else if (msg instanceof DatagramPacket) {
                ByteBuf buffer = ctx.alloc().buffer();
                putAddr(buffer);
                buffer.writeShort(((DatagramPacket) msg).content().readableBytes());
                buffer.writeByte(13);
                buffer.writeByte(10);
                buffer.writeBytes(((DatagramPacket) msg).content());
                relayChannel.writeAndFlush(buffer).addListener(future -> {
                    log.error("" + future.isSuccess(), future.cause());
                });
            }
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    private void putAddr(ByteBuf buf) {
        buf.writeByte(atyp.getValue());
        if (atyp != ATYP.DOMAIN) {
            try {
                buf.writeBytes(InetAddress.getByName(host).getAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
            buf.writeByte(host.length());
            buf.writeBytes(host.getBytes(StandardCharsets.UTF_8));
        }
        buf.writeShort(port);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (relayChannel.isActive()) {
            SocksServerUtils.closeOnFlush(relayChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION][" + clientHostname + "] " + cause.getMessage());
        ctx.close();
    }
}
