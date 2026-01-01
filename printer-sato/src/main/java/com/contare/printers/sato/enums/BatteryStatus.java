package com.contare.printers.sato.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum BatteryStatus {

    NORMAL(0, "BT0", "Normal"),                     // 0: Normal
    BATTERY_NEAR_END(1, "BT1", "Battery near end"),  // 1: Battery near end
    BATTERY_ERROR(2, "BT2", "Battery error");        // 2: Battery error

    private static final Map<Integer, BatteryStatus> _numbers = new HashMap<>();
    private static final Map<String, BatteryStatus> _codes = new HashMap<>();

    static {
        for (BatteryStatus row : values()) {
            _numbers.put(row.number, row);
            _codes.put(row.code, row);
        }
    }

    private final int number;
    private final String code;
    private final String description;

    public static BatteryStatus get(final Integer value) {
        if (value == null) return null;
        return _numbers.get(value);
    }

    public static BatteryStatus get(final String value) {
        if (value == null) return null;
        return _codes.get(value);
    }

    @Override
    public String toString() {
        return String.format("%s: %s", code, description);
    }

}
