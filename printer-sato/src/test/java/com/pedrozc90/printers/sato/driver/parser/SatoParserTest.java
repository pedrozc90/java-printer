package com.pedrozc90.printers.sato.driver.parser;

import com.pedrozc90.printers.core.objects.RawPacket;
import com.pedrozc90.printers.sato.enums.*;
import com.pedrozc90.printers.sato.objects.SatoMessage;
import com.pedrozc90.printers.sato.parser.SatoParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SatoParserTest {

    private final Charset charset = StandardCharsets.UTF_8;

    private final SatoParser parser = SatoParser.getInstance();

    @ParameterizedTest
    @ValueSource(strings = {
        // PH
        "\u0006",
        "\u0002\u0006\u0003",
        "\u0015",
        "\u0002\u0015\u0003",
        // PG
        "\u000232,PS2,RS0,RE0,PE0,EN00,BT0,Q000000\u0003",
        // PK
        "\u000253,1,N,EP:E0123456789ABCDEF0123456,ID:E200680612345678\r\n\u0003", // write successful (EPC read successful, TID read successful)
        "\u000225,1,N,ID:E200680612345678\r\n\u0003",                             // write successful (TID read successful)
        "\u00029,1,T,ID:\r\n\u0003",                                              // write successful (TID read fail)
        "\u00029,0,E,ID:\r\n\u0003",                                              // write fail (EPC write fail)
    })
    public void parser(final String value) {
        final RawPacket packet = rawPacket(value);
        final List<SatoMessage> result = parser.parse(packet);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Parse PG return")
    public void parsePGReturn() {
        final String value = "\u000232,PS2,RS0,RE0,PE0,EN00,BT0,Q000100\u0003";
        final byte[] bytes = value.getBytes(charset);
        final RawPacket packet = new RawPacket(bytes, charset);

        final List<SatoMessage> results = parser.parse(packet);
        assertNotNull(results);
        assertEquals(1, results.size());

        final SatoMessage m0 = results.get(0);
        assertInstanceOf(SatoMessage.PrinterInfo.class, m0);

        final SatoMessage.PrinterInfo p = (SatoMessage.PrinterInfo) m0;
        assertAll(
            () -> assertEquals(32, p.getLength()),
            () -> assertEquals(PrinterStatus.ANALYZING, p.getPs()),
            () -> assertEquals(ReceiveBufferStatus.BUFFER_AVAILABLE, p.getRs()),
            () -> assertEquals(RibbonStatus.RIBBON_PRESENT, p.getRe()),
            () -> assertEquals(MediaStatus.MEDIA_PRESENT, p.getPe()),
            () -> assertEquals(ErrorNumber.ONLINE, p.getEn()),
            () -> assertEquals(100, p.getQ())
        );
    }

    @Test
    @DisplayName("Parse PK return")
    public void parsePKReturn() {
        final String value = "\u000253,1,N,EP:E0123456789ABCDEF0123456,ID:E200680612345678\r\n\u0003";
        final byte[] bytes = value.getBytes(charset);
        final RawPacket packet = new RawPacket(bytes, charset);

        final List<SatoMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final SatoMessage m0 = results.get(0);
        assertInstanceOf(SatoMessage.TagInfo.class, m0);

        final SatoMessage.TagInfo t = (SatoMessage.TagInfo) m0;
        assertAll(
            () -> assertEquals(53, t.getLength()),
            () -> assertEquals("1", t.getWr()),
            () -> assertEquals("N", t.getEs()),
            () -> assertEquals("E0123456789ABCDEF0123456", t.getEpc()),
            () -> assertEquals("E200680612345678", t.getTid())
        );
    }

    @Test
    @DisplayName("Parse PK return only TID")
    public void parsePKReturnOnlyTID() {
        final String value = "\u000225,1,N,ID:E200680612345678\r\n\u0003";
        final byte[] bytes = value.getBytes(charset);
        final RawPacket packet = new RawPacket(bytes, charset);

        final List<SatoMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final SatoMessage m0 = results.get(0);
        assertInstanceOf(SatoMessage.TagInfo.class, m0);

        final SatoMessage.TagInfo t = (SatoMessage.TagInfo) m0;
        assertAll(
            () -> assertEquals(25, t.getLength()),
            () -> assertEquals("1", t.getWr()),
            () -> assertEquals("N", t.getEs()),
            () -> assertNull(t.getEpc()),
            () -> assertEquals("E200680612345678", t.getTid())
        );
    }

    @Test
    @DisplayName("Parse PG and PK return")
    public void parsePGAndPKReturn() {
        final String value = "\u000232,PS2,RS0,RE0,PE0,EN00,BT0,Q000000\u0003\u000253,1,N,EP:E0123456789ABCDEF0123456,ID:E200680612345678\r\n\u0003";
        final byte[] bytes = value.getBytes(charset);
        final RawPacket packet = new RawPacket(bytes, charset);

        final List<SatoMessage> results = parser.parse(packet);
        assertNotNull(results);
        assertEquals(2, results.size());

        final SatoMessage m0 = results.get(0);
        assertInstanceOf(SatoMessage.PrinterInfo.class, m0);

        final SatoMessage.PrinterInfo p = (SatoMessage.PrinterInfo) m0;
        assertAll(
            () -> assertEquals(32, p.getLength()),
            () -> assertEquals(PrinterStatus.ANALYZING, p.getPs()),
            () -> assertEquals(ReceiveBufferStatus.BUFFER_AVAILABLE, p.getRs()),
            () -> assertEquals(RibbonStatus.RIBBON_PRESENT, p.getRe()),
            () -> assertEquals(MediaStatus.MEDIA_PRESENT, p.getPe()),
            () -> assertEquals(ErrorNumber.ONLINE, p.getEn()),
            () -> assertEquals(0, p.getQ())
        );

        final SatoMessage m1 = results.get(1);
        assertInstanceOf(SatoMessage.TagInfo.class, m1);

        final SatoMessage.TagInfo t = (SatoMessage.TagInfo) m1;
        assertAll(
            () -> assertEquals(53, t.getLength()),
            () -> assertEquals("1", t.getWr()),
            () -> assertEquals("N", t.getEs()),
            () -> assertEquals("E0123456789ABCDEF0123456", t.getEpc()),
            () -> assertEquals("E200680612345678", t.getTid())
        );
    }

    // UTILITIES
    private RawPacket rawPacket(final String data) {
        final byte[] bytes = data.getBytes(charset);
        return new RawPacket(bytes, charset);
    }

}
