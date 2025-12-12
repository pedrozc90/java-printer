package com.pedrozc90.printers.sample;

import com.pedrozc90.printers.averydennison.AveryDennisonPrinter;
import com.pedrozc90.printers.core.Printer;
import com.pedrozc90.printers.sato.SatoPrinter;
import com.pedrozc90.printers.zebra.ZebraPrinter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrinterPoolTest {

    private final PrinterPool pool = PrinterPool.getInstance();

    @BeforeEach
    public void setUp() {
        pool.clear();
    }

    @AfterEach
    public void cleanUp() {
        pool.clear();
    }

    @Test
    @DisplayName("Test Printer Pool")
    public void testPrinterPool() {
        assertAll(
            () -> {
                final Printer printer = pool.create("AVERY_DENNISON", "localhost", 4000);
                assertInstanceOf(AveryDennisonPrinter.class, printer);
            },
            () -> {
                final Printer printer = pool.create("SATO", "localhost", 4001);
                assertInstanceOf(SatoPrinter.class, printer);
            },
            () -> {
                final Printer printer = pool.create("ZEBRA", "localhost", 4002);
                assertInstanceOf(ZebraPrinter.class, printer);
            },
            () -> assertThrows(IllegalArgumentException.class, () -> pool.create("NONE", "localhost", 4003)),
            () -> assertEquals(3, pool.getPool().size())
        );
    }

}
