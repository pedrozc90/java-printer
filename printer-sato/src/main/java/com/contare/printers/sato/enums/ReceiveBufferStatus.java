package com.contare.printers.sato.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum ReceiveBufferStatus {

    BUFFER_AVAILABLE(0, "RS0", "Buffer available"), // 0: Buffer available
    BUFFER_NEAR_FULL(1, "RS1", "Buffer near full"), // 1: Buffer near full
    BUFFER_FULL(2, "RS2", "Buffer full");           // 2: Buffer full

    private static final Map<Integer, ReceiveBufferStatus> _numbers = new HashMap<>();
    private static final Map<String, ReceiveBufferStatus> _codes = new HashMap<>();

    static {
        for (ReceiveBufferStatus row : values()) {
            _numbers.put(row.number, row);
            _codes.put(row.code, row);
        }
    }

    private final int number;
    private final String code;
    private final String description;

    public static ReceiveBufferStatus get(final Integer value) {
        if (value == null) return null;
        return _numbers.get(value);
    }

    public static ReceiveBufferStatus get(final String code) {
        if (code == null) return null;
        return _codes.get(code);
    }

    @Override
    public String toString() {
        return String.format("%s: %s", code, description);
    }

}
