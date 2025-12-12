package com.contare.printers.core.exceptions;

public class PrinterException extends Exception {

    public PrinterException() {
    }

    public PrinterException(final String message) {
        super(message);
    }

    public PrinterException(final String fmt, final Object... args) {
        this(String.format(fmt, args));
    }

    public PrinterException(final Throwable cause, final String message) {
        super(message, cause);
    }

    public PrinterException(final Throwable cause, final String fmt, final Object... args) {
        this(cause, String.format(fmt, args));
    }

    public PrinterException(final Throwable cause) {
        super(cause);
    }

    public PrinterException(final Throwable cause, boolean enableSuppression, boolean writableStackTrace, final String message) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
