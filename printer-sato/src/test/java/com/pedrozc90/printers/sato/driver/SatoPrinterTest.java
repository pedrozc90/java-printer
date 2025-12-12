package com.pedrozc90.printers.sato.driver;

import com.pedrozc90.printers.core.exceptions.PrinterException;
import com.pedrozc90.printers.sato.SatoPrinter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SatoPrinterTest {

    private static SatoPrinter printer;

    @BeforeAll
    public static void setUp() {
        printer = new SatoPrinter("localhost", 0);
    }

    @AfterAll
    public static void cleanUp() throws PrinterException {
        printer.close();
    }

    @Test
    public void test() {
        assertNotNull(printer);
        assertFalse(printer.isPrinting());
    }

}
