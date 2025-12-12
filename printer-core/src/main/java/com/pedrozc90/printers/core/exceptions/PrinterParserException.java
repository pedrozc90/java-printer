package com.pedrozc90.printers.core.exceptions;

public class PrinterParserException extends RuntimeException {

    public PrinterParserException() {
    }

    public PrinterParserException(final String message) {
        super(message);
    }

    public PrinterParserException(final String fmt, final Object... args) {
        this(String.format(fmt, args));
    }

    public PrinterParserException(final Throwable cause, final String message) {
        super(message, cause);
    }

    public PrinterParserException(final Throwable cause, final String fmt, final Object... args) {
        this(cause, String.format(fmt, args));
    }

    public PrinterParserException(final Throwable cause) {
        super(cause);
    }

}
