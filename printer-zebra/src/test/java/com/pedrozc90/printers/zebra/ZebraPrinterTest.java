package com.pedrozc90.printers.zebra;

import com.pedrozc90.printers.core.exceptions.PrinterException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ZebraPrinterTest {

    private static ZebraPrinter printer;

    @BeforeAll
    public static void setUp() {
        printer = new ZebraPrinter("localhost", 0);
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
