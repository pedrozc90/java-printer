package com.pedrozc90.printers.tests.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ListUtils {

    @SafeVarargs
    public static <T> List<T> of(final T... values) {
        return Arrays.asList(values);
    }

    public static <T> List<T> of(final T values) {
        return Collections.singletonList(values);
    }

}
