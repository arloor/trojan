package com.arloor.trojan.server.trojan.handler;

import com.arloor.trojan.server.util.SocksServerUtils;
import com.arloor.trojan.server.trojan.enums.ATYP;
import com.arloor.trojan.server.trojan.enums.CMD;
import com.arloor.trojan.server.trojan.enums.Proto;
import com.arloor.trojan.server.trojan.model.Dst;
import com.arloor.trojan.server.trojan.model.TrojanRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
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
    private Dst dst;

    private State state = State.INIT;
    private static final Logger logger = LoggerFactory.getLogger(TrojanRequestDecoder.class);
    private static final int passwdLength = 56;

    private enum State {
        INIT, DST_ADDR_PORT, CRLF, TCP, UDP
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
                    if (!CMD.CONNECT.equals(cmd)) {
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
                    state = CMD.CONNECT.equals(cmd) ? State.TCP : State.UDP;
                }
                break;
            case TCP:
            case UDP:
                in.retain();
                ByteBuf payload = in.readSlice(in.readableBytes());
                switch (state) {
                    case TCP:
                        out.add(new TrojanRequest(passwd, dst, Proto.TCP, payload));
                        break;
                    case UDP:
                        out.add(new TrojanRequest(passwd, dst, Proto.UDP, payload));
                        break;
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
}
