package com.arloor.trojan.server.trojan.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UdpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final Logger log= LoggerFactory.getLogger(UdpChannelHandler.class);
    //接受服务端发送的内容
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {

        String msg = packet.content().toString(Charset.forName("GBK"));
        log.info("udp响应：{}",msg);
    }

}