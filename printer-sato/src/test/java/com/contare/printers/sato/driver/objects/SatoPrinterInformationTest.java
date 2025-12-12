package com.contare.printers.sato.driver.objects;

import com.contare.printers.sato.enums.*;
import com.contare.printers.sato.objects.SatoPrinterInformation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SatoPrinterInformationTest {

    @Test
    @DisplayName("Parse PG return")
    public void parsePGReturn() {
        final String value = "\u000232,PS2,RS0,RE0,PE0,EN00,BT0,Q000100\u0003";
        final SatoPrinterInformation obj = SatoPrinterInformation.parse(value);
        assertNotNull(obj);
        assertAll(
            () -> assertEquals(PrinterStatus.ANALYZING, obj.getPrinterStatus()),
            () -> assertEquals(ReceiveBufferStatus.BUFFER_AVAILABLE, obj.getReceiveBuffer()),
            () -> assertEquals(RibbonStatus.RIBBON_PRESENT, obj.getRibbonStatus()),
            () -> assertEquals(MediaStatus.MEDIA_PRESENT, obj.getMediaStatus()),
            () -> assertEquals(ErrorNumber.ONLINE, obj.getErrorNumber()),
            () -> assertEquals(100, obj.getRemainBuffer()),
            () -> assertNull(obj.getEpc()),
            () -> assertNull(obj.getTid())
        );
    }

    @Test
    @DisplayName("Parse PK return")
    public void parsePKReturn() {
        final String value = "\u000253,1,N,EP:E0123456789ABCDEF0123456,ID:E200680612345678\r\n\u0003";
        final SatoPrinterInformation obj = SatoPrinterInformation.parse(value);
        assertNotNull(obj);
        assertAll(
            () -> assertEquals("E0123456789ABCDEF0123456", obj.getEpc()),
            () -> assertEquals("E200680612345678", obj.getTid())
        );
    }

    @Test
    @DisplayName("Parse PK return only TID")
    public void parsePKReturnOnlyTID() {
        final String value = "\u000225,1,N,ID:E200680612345678\r\n\u0003";
        final SatoPrinterInformation obj = SatoPrinterInformation.parse(value);
        assertNotNull(obj);
        assertAll(
            () -> assertNull(obj.getEpc()),
            () -> assertEquals("E200680612345678", obj.getTid())
        );
    }

    @Test
    @DisplayName("Parse PG and PK return")
    public void parsePGAndPKReturn() {
        final String value = "\u000232,PS2,RS0,RE0,PE0,EN00,BT0,Q000000\u0003\u000253,1,N,EP:E0123456789ABCDEF0123456,ID:E200680612345678\r\n\u0003";
        final SatoPrinterInformation obj = SatoPrinterInformation.parse(value);
        assertNotNull(obj);
        assertAll(
            () -> assertEquals(PrinterStatus.ANALYZING, obj.getPrinterStatus()),
            () -> assertEquals(ReceiveBufferStatus.BUFFER_AVAILABLE, obj.getReceiveBuffer()),
            () -> assertEquals(RibbonStatus.RIBBON_PRESENT, obj.getRibbonStatus()),
            () -> assertEquals(MediaStatus.MEDIA_PRESENT, obj.getMediaStatus()),
            () -> assertEquals(ErrorNumber.ONLINE, obj.getErrorNumber()),
            () -> assertEquals("E0123456789ABCDEF0123456", obj.getEpc()),
            () -> assertEquals("E200680612345678", obj.getTid())
        );
    }

}
