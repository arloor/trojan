package com.arloor.trojan.server.trojan.handler;

import com.arloor.trojan.server.trojan.model.DstWithLength;
import com.arloor.trojan.server.util.SocksServerUtils;
import com.arloor.trojan.server.trojan.enums.ATYP;
import com.arloor.trojan.server.trojan.enums.CMD;
import com.arloor.trojan.server.trojan.enums.Proto;
import com.arloor.trojan.server.trojan.model.Dst;
import com.arloor.trojan.server.trojan.model.TrojanRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ByteProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TrojanRequestDecoder extends ByteToMessageDecoder {

    private String passwd;
    private CMD cmd;
    private ATYP atyp;
    private ATYP udpAtyp;
    private short udpPacketLength;
    private Dst dst;

    private State state = State.INIT;
    private static final Logger logger = LoggerFactory.getLogger(TrojanRequestDecoder.class);
    private static final int passwdLength = 56;

    private enum State {
        INIT, DST_ADDR_PORT, CRLF, TCP, PASRSE_UDP_DST_LENGTH, UDP_CONTENT
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state) {
            case INIT:
                if (in.readableBytes() > passwdLength + 2 + 2) {
                    CharSequence cs = in.readCharSequence(56, StandardCharsets.UTF_8);
                    passwd = cs.toString();
                    in.skipBytes(2);
                    byte cmdByte = in.readByte();
                    cmd = CMD.parse(cmdByte);
                    if (cmd == null) {
                        logger.error("不支持的CMD:{}", cmdByte);
                        SocksServerUtils.closeOnFlush(ctx.channel());
                        break;
                    }
                    byte atypByte = in.readByte();
                    atyp = ATYP.parse(atypByte);
                    if (atyp == null) {
                        logger.error("不支持的ATYP：{}", atypByte);
                        SocksServerUtils.closeOnFlush(ctx.channel());
                        break;
                    }
                    state = State.DST_ADDR_PORT;
                }
                break;
            case DST_ADDR_PORT:
                dst = parseDst(in);
                if (dst != null) {
                    state = State.CRLF;
                } else {
                    break;
                }
            case CRLF:
                if (in.readableBytes() > 2) {
                    in.skipBytes(2);
                    state = CMD.CONNECT.equals(cmd) ? State.TCP : State.PASRSE_UDP_DST_LENGTH;
                    if (state == State.PASRSE_UDP_DST_LENGTH) {
                        ctx.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                    } else {
                        ctx.pipeline().addLast(new TrojanConnectHandler());
                    }

                }
                break;
            case TCP:
                in.retain();
                ByteBuf payload = in.readSlice(in.readableBytes());
                out.add(new TrojanRequest(passwd, dst, Proto.TCP, payload));
                break;
            case PASRSE_UDP_DST_LENGTH:
                DstWithLength dst = parseUdpDst(in);
                if (dst != null) {
                    udpPacketLength = dst.getContentLength();
                    this.dst = dst;
                    state = State.UDP_CONTENT;
                } else {
                    break;
                }
            case UDP_CONTENT:
                if (in.readableBytes() == udpPacketLength) {
                    in.retain();
                    ByteBuf content = in.readSlice(udpPacketLength);
                    out.add(new TrojanRequest(passwd, this.dst, Proto.UDP, Unpooled.EMPTY_BUFFER));
                    out.add(content);
                    state = State.PASRSE_UDP_DST_LENGTH;
                }
                break;
        }
    }

    private Dst parseDst(ByteBuf in) {
        int index = in.forEachByte(ByteProcessor.FIND_CRLF);
        if (index != -1) {
            if (ATYP.DOMAIN.equals(atyp)) {
                byte domainLength = in.readByte();
                CharSequence domain = in.readCharSequence(domainLength, StandardCharsets.UTF_8);
                short port = in.readShort();
                return new Dst(domain.toString(), port);
            } else {
                byte[] ip = ATYP.IPV4.equals(atyp) ? new byte[4] : new byte[16];
                in.readBytes(ip);
                try {
                    String host = InetAddress.getByAddress(ip).getHostAddress();
                    short port = in.readShort();
                    return new Dst(host, port);
                } catch (UnknownHostException e) {
                    logger.error("无法读取DST", e);
                }
            }
        }
        return null;
    }

    private DstWithLength parseUdpDst(ByteBuf in) {
        Dst dst = null;
        int index = in.forEachByte(ByteProcessor.FIND_CRLF);
        if (index != -1) {
            udpAtyp = ATYP.parse(in.readByte());
            if (udpAtyp == null) {
                return null;
            }
            if (ATYP.DOMAIN.equals(udpAtyp)) {
                byte domainLength = in.readByte();
                CharSequence domain = in.readCharSequence(domainLength, StandardCharsets.UTF_8);
                short port = in.readShort();
                logger.info("{}", port);
                dst = new Dst(domain.toString(), port);
            } else {
                byte[] ip = ATYP.IPV4.equals(udpAtyp) ? new byte[4] : new byte[16];
                in.readBytes(ip);
                try {
                    String host = InetAddress.getByAddress(ip).getHostAddress();
                    short port = in.readShort();
                    dst = new Dst(host, port);
                } catch (UnknownHostException e) {
                    logger.error("无法读取DST", e);
                }
            }
        }
        if (dst != null) {
            short contentLength = in.readShort();
            in.skipBytes(2);
            return new DstWithLength(dst.getHost(), dst.getPort(), contentLength);
        }
        return null;
    }
}
