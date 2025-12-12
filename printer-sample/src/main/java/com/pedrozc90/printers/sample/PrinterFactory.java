package com.pedrozc90.printers.sample;

import com.pedrozc90.printers.averydennison.AveryDennisonPrinter;
import com.pedrozc90.printers.core.Printer;
import com.pedrozc90.printers.sato.SatoPrinter;
import com.pedrozc90.printers.zebra.ZebraPrinter;
import org.jboss.logging.Logger;

import java.util.Objects;

public class PrinterFactory {

    private static PrinterFactory instance;

    private final Logger logger = Logger.getLogger(PrinterFactory.class);

    public static PrinterFactory getInstance() {
        if (instance == null) {
            instance = new PrinterFactory();
        }
        return instance;
    }

    public Printer create(final String type, final String ip, final Integer port) {
        final String t = Objects.requireNonNull(type, "Type is required");
        switch (t) {
            case "SATO":
                return new SatoPrinter(ip, port);
            case "ZEBRA":
                return new ZebraPrinter(ip, port);
            case "AVERY_DENNISON":
                return new AveryDennisonPrinter(ip, port);
            default:
                final String message = String.format("Invalid printer type '%s'", t);
                throw new IllegalArgumentException(message);
        }
    }

}
