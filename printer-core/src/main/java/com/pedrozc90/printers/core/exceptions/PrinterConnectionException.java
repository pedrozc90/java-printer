package com.pedrozc90.printers.core.exceptions;

import java.io.IOException;

public class PrinterConnectionException extends IOException {

    public PrinterConnectionException() {
    }

    public PrinterConnectionException(final String message) {
        super(message);
    }

    public PrinterConnectionException(final String fmt, final Object... args) {
        this(String.format(fmt, args));
    }

    public PrinterConnectionException(final Throwable cause, final String message) {
        super(message, cause);
    }

    public PrinterConnectionException(final Throwable cause, final String fmt, final Object... args) {
        this(cause, String.format(fmt, args));
    }

    public PrinterConnectionException(final Throwable cause) {
        super(cause);
    }

}
