package com.arloor.trojan.server.trojan.enums;

import java.util.HashMap;
import java.util.Map;

public enum CMD {
    CONNECT((byte) 1), UDP_ASSOCIATE((byte) 3);
    private byte value;
    private static final Map<Byte, CMD> lookup = new HashMap<>();

    static {
        for (CMD value : CMD.values()) {
            lookup.put(value.value, value);
        }
    }

    CMD(byte value) {
        this.value = value;
    }

    public static CMD parse(byte value) {
        return lookup.get(value);
    }
}
