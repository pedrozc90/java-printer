package com.pedrozc90.printers.core.utils;

public class ObjectUtils {

    public static void requires(final boolean condition, final String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void requires(final Object obj) {
        requires(obj != null, "must not be null");
    }

}
