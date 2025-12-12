package com.pedrozc90.printers.sato.driver.objects;

import com.pedrozc90.printers.sato.objects.SatoMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class SatoMessageTest {

    @Test
    public void testBasicMessage() {
        final String value = "A";
        final Charset charset = StandardCharsets.ISO_8859_1;
        final byte[] bytes = value.getBytes(charset);
        final SatoMessage result = new SatoMessage.Any(bytes, charset);
        assertArrayEquals(bytes, result.getBytes());
        assertEquals(charset, result.getCharset());
        assertEquals(value, result.toText());
        assertArrayEquals(bytes, result.getContent());
        assertEquals(value, result.getContentAsText());
        assertFalse(result.isFramed());
    }

    @Test
    public void testFrameMessage() {
        final String value = "\u0002A\u0003";
        final Charset charset = StandardCharsets.ISO_8859_1;
        final byte[] bytes = value.getBytes(charset);
        final SatoMessage message = new SatoMessage.Any(bytes, charset);
        assertArrayEquals(bytes, message.getBytes());
        assertEquals(charset, message.getCharset());
        assertEquals(value, message.toText());
        assertArrayEquals(new byte[]{ 'A' }, message.getContent());
        assertEquals("A", message.getContentAsText());
        assertTrue(message.isFramed());
    }

}
