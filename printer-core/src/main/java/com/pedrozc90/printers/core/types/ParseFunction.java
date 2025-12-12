package com.pedrozc90.printers.core.types;

import java.io.IOException;

@FunctionalInterface
public interface ParseFunction<T, R> {

    R apply(T t) throws IOException;

}
