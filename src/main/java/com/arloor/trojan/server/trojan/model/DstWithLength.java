package com.arloor.trojan.server.trojan.model;

public class DstWithLength extends Dst {

    private short contentLength;

    public DstWithLength(String host, int port) {
        super(host, port);
    }

    public DstWithLength(String host, int port, short contentLength) {
        super(host, port);
        this.contentLength = contentLength;
    }

    public short getContentLength() {
        return contentLength;
    }

    @Override
    public String toString() {
        return "DstWithLength{" +
                "host='" + super.getHost() + '\'' +
                ", port=" + getPort() +
                ", contentLength=" + contentLength +
                '}';
    }
}
