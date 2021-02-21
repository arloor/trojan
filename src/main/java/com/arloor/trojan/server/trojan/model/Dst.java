package com.arloor.trojan.server.trojan.model;

public class Dst {
    private String host;
    private int port;

    public Dst(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Dst{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
