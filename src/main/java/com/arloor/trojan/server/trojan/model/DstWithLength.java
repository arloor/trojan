package com.arloor.trojan.server.trojan.model;

import com.arloor.trojan.server.trojan.enums.ATYP;

public class DstWithLength extends Dst {

    private short contentLength;
    private ATYP udpAtype;

    public DstWithLength(String host, int port) {
        super(host, port);
    }

    public DstWithLength(String host, int port, short contentLength, ATYP atyp) {
        super(host, port);
        this.contentLength = contentLength;
        this.udpAtype = atyp;
    }

    public ATYP getUdpAtype() {
        return udpAtype;
    }

    public short getContentLength() {
        return contentLength;
    }

    @Override
    public String toString() {
        return "DstWithLength{" +
                "host='" + getHost() +
                ", port=" + getPort() +
                ", contentLength=" + contentLength +
                ", udpAtype=" + udpAtype +
                '}';
    }
}
