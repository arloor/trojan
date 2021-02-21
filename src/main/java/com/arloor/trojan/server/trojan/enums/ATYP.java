package com.arloor.trojan.server.trojan.enums;

import java.util.HashMap;
import java.util.Map;

public enum ATYP {
    DOMAIN((byte) 3), IPV4((byte) 1), IPV6((byte) 4);
    private byte value;
    private static final Map<Byte, ATYP> lookup = new HashMap<>();

    static {
        for (ATYP value : ATYP.values()) {
            lookup.put(value.value, value);
        }
    }

    ATYP(byte value) {
        this.value = value;
    }

    public static ATYP parse(byte value) {
        return lookup.get(value);
    }
}
