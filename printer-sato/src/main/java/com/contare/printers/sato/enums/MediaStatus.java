package com.contare.printers.sato.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum MediaStatus {

    MEDIA_PRESENT(0, "PE0", "Media present (including during startup)"),    // 0: Media present (including during startup)
    NO_MEDIA(2, "PE2", "No media");                                         // 2: No media

    private static final Map<Integer, MediaStatus> _numbers = new HashMap<>();
    private static final Map<String, MediaStatus> _codes = new HashMap<>();

    static {
        for (MediaStatus row : values()) {
            _numbers.put(row.number, row);
            _codes.put(row.code, row);
        }
    }

    private final int number;
    private final String code;
    private final String description;

    public static MediaStatus get(final Integer value) {
        if (value == null) return null;
        return _numbers.get(value);
    }

    public static MediaStatus get(final String value) {
        if (value == null) return null;
        return _codes.get(value);
    }

    @Override
    public String toString() {
        return String.format("%s: %s", code, description);
    }

}
