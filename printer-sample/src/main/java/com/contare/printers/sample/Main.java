package com.contare.printers.sample;

import com.contare.printers.core.Printer;
import org.jboss.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    private static final PrinterPool pool = PrinterPool.getInstance();

    public static void main(final String[] args) {
        try (final Printer printer = pool.create("SATO", "localhost", 0)) {
            logger.infof("Printer: %s", printer);
        } catch (Exception e) {
            logger.errorf(e, "Error");
        }
    }

}
