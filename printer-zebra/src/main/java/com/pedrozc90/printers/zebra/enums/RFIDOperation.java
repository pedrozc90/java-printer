package com.pedrozc90.printers.zebra.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum RFIDOperation {

    PERMA_LOCK("B", "Permalock"), // pg. 363 e 383
    LOG_FILE_RESET("E", "Log File Reset"),
    LOCK("L", "Lock"),
    LOCK_UNLOCK_MEMORY_BANK("M", "Lock/Unlock"), // pg. 363 e 382
    RFID_SETTINGS("S", "RFID Settings"),
    READ("R", "Read"),
    WRITE("W", "Write");

    private static final Map<String, RFIDOperation> _codes = new HashMap<>();

    static {
        for (RFIDOperation row : values()) {
            _codes.put(row.code, row);
        }
    }

    private final String code;
    private final String description;

    public static RFIDOperation get(final String value) {
        if (value == null) return null;
        return _codes.get(value);
    }

    @Override
    public String toString() {
        return String.format("%s: %s", code, description);
    }

}
