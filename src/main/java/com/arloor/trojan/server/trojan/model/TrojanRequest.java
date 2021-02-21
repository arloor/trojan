package com.arloor.trojan.server.trojan.model;

import com.arloor.trojan.server.trojan.enums.Proto;
import com.arloor.trojan.server.trojan.model.Dst;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public class TrojanRequest {
    private Dst dst;
    private String passwd;
    private Proto proto;
    private ByteBuf payload;

    public TrojanRequest(String passwd, Dst dst, Proto proto,ByteBuf payload) {
        this.passwd = passwd;
        this.dst = dst;
        this.proto = proto;
        this.payload=payload;
    }

    public String getPasswd() {
        return passwd;
    }

    public Dst getDst() {
        return dst;
    }

    public Proto getProto() {
        return proto;
    }

    public ByteBuf getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "TrojanRequest{" +
                "dst=" + dst +
                ", passwd='" + passwd + '\'' +
                ", proto=" + proto +
                '}';
    }
}
