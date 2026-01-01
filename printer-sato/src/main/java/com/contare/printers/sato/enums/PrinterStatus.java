package com.contare.printers.sato.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum PrinterStatus {

    STANDBY(0, "PS0", "Standby"),        // 0: Standby (waiting for receiving data)
    WAITING(1, "PS1", "Waiting"),        // 1: Waiting for dispensing
    ANALYZING(2, "PS2", "Analyzing"),    // 2: Analyzing
    PRINTING(3, "PS3", "Printing"),      // 3: Printing
    OFFLINE(4, "PS4", "Offline"),        // 4: Offline
    ERROR(5, "PS5", "Error");            // 5: Error

    private static final Map<String, PrinterStatus> _codes = new HashMap<>();
    private static final Map<Integer, PrinterStatus> _numbers = new HashMap<>();

    static {
        for (PrinterStatus row : values()) {
            _numbers.put(row.number, row);
            _codes.put(row.code, row);
        }
    }

    private final int number;
    private final String code;
    private final String description;

    public static PrinterStatus get(final Integer value) {
        if (value == null) return null;
        return _numbers.get(value);
    }

    public static PrinterStatus get(final String value) {
        if (value == null) return null;
        return _codes.get(value);
    }

    @Override
    public String toString() {
        return String.format("%s: %s", code, description);
    }

}
