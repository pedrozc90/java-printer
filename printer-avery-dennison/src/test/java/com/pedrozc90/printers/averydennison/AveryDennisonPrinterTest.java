package com.pedrozc90.printers.averydennison;

import com.pedrozc90.printers.core.exceptions.PrinterException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AveryDennisonPrinterTest {

    private AveryDennisonPrinter printer;

    @BeforeEach
    public void setUp() {
        printer = new AveryDennisonPrinter("localhost", 0);
    }

    @AfterEach
    public void cleanUp() throws PrinterException {
        if (printer != null) {
            printer.close();
        }
    }

    @Test
    public void test() {
        assertNotNull(printer);
    }

}
