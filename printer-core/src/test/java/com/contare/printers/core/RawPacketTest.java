package com.contare.printers.core;

import com.contare.printers.core.objects.RawPacket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class RawPacketTest {

    @Test
    @DisplayName("Successfully create a RawPacket from bytes")
    public void createRawPacketFromBytes() {
        final Charset charset = StandardCharsets.UTF_8;
        final byte[] bytes = { 0x02, 0x48, 0x03, 0x0D, 0x0A };
        final RawPacket result = new RawPacket(bytes, charset);
        assertNotNull(result);
        assertEquals(charset, result.getCharset());
        assertArrayEquals(bytes, result.getBytes());
        assertEquals(5, result.length());
        assertEquals("<0x02><0x48><0x03><0x0D><0x0A>", result.toHex());
        assertEquals("\u0002H\u0003\r\n", result.toText());
    }

    @Test
    @DisplayName("Successfully create a RawPacket from a string")
    public void createRawPacketFromString() {
        final Charset charset = StandardCharsets.UTF_8;
        final String text = "\u0002H\u0003\r\n";
        final RawPacket result = RawPacket.of(text, charset);
        assertNotNull(result);
        assertEquals(charset, result.getCharset());
        assertArrayEquals(new byte[]{ 0x02, 0x48, 0x03, 0x0D, 0x0A }, result.getBytes());
        assertEquals(5, result.length());
        assertEquals("<0x02><0x48><0x03><0x0D><0x0A>", result.toHex());
        assertEquals(text, result.toText());
    }

}
