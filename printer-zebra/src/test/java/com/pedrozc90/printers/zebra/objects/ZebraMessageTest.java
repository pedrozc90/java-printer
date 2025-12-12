package com.pedrozc90.printers.zebra.objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ZebraMessageTest {

    @Test
    @DisplayName("Normal message")
    public void testNormalMessage() {
        final String value = "DATA";
        final ZebraMessage.Any data = new ZebraMessage.Any(value, StandardCharsets.UTF_8);
        assertFalse(data.isFramed());
        assertEquals(value, data.getRaw());
        assertEquals(value, data.getContentAsText());
    }

    @Test
    @DisplayName("Frame message")
    public void testFrameMessage() {
        final String content = "DATA";
        final String value = "<start>" + content + "<end>";
        final ZebraMessage.Any data = new ZebraMessage.Any(value, StandardCharsets.UTF_8);
        assertTrue(data.isFramed());
        assertEquals(value, data.getRaw());
        assertEquals(content, data.getContentAsText());
    }

    @Test
    @DisplayName("Message with only <start>")
    public void testMessageWithOnlyStartTag() {
        final String content = "DATA";
        final String value = "<start>" + content;
        final ZebraMessage.Any data = new ZebraMessage.Any(value, StandardCharsets.UTF_8);
        assertFalse(data.isFramed());
        assertEquals(value, data.getRaw());
        assertEquals(content, data.getContentAsText());
    }

}
