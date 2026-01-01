package com.contare.printers.sato.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum RibbonStatus {

    RIBBON_PRESENT(0, "RE0", "Ribbon present"),             // 0: Ribbon present
    RIBBON_NEAR_END(1, "RE1", "Ribbon near end"),           // 1: Ribbon near end
    NO_RIBBON(2, "RE2", "No ribbon"),                       // 2: No ribbon
    DIRECT_THERMAL_MODEL(3, "RE3", "Direct thermal model"); // 3: Direct thermal model

    private static final Map<Integer, RibbonStatus> _numbers = new HashMap<>();
    private static final Map<String, RibbonStatus> _codes = new HashMap<>();

    static {
        for (RibbonStatus row : values()) {
            _numbers.put(row.number, row);
            _codes.put(row.code, row);
        }
    }

    private final int number;
    private final String code;
    private final String description;

    public static RibbonStatus get(final Integer value) {
        if (value == null) return null;
        return _numbers.get(value);
    }

    public static RibbonStatus get(final String value) {
        if (value == null) return null;
        return _codes.get(value);
    }

    @Override
    public String toString() {
        return String.format("%s: %s", code, description);
    }

}
