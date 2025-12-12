package com.contare.printers.averydennison;

import com.contare.printers.core.exceptions.PrinterException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AveryDennisonPrinterTest {

    private AveryDennisonPrinter printer;

    @AfterEach
    public void setUp() {
        printer = new AveryDennisonPrinter("localhost", 0);
    }

    @AfterEach
    public void cleanUp() throws PrinterException {
        printer.close();
    }

    @Test
    public void test() {
        assertNotNull(printer);
    }

}
