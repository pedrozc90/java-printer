package com.pedrozc90.printers.zebra.parser;

import com.pedrozc90.printers.core.objects.RawPacket;
import com.pedrozc90.printers.tests.utils.ResourceUtils;
import com.pedrozc90.printers.zebra.enums.RFIDOperation;
import com.pedrozc90.printers.zebra.enums.RFIDStatus;
import com.pedrozc90.printers.zebra.objects.ZebraMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class ZebraParserTest {

    private final ZebraParser parser = ZebraParser.getInstance();
    private final ResourceUtils resources = ResourceUtils.getInstance();

    @Test
    @DisplayName("Parse '<start><end>'")
    public void parseBlockNoContent() {
        final String content = "<start><end>";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final ZebraMessage result = results.get(0);
        assertInstanceOf(ZebraMessage.Blank.class, result);
    }

    @Test
    @DisplayName("Parse '<start>AB<end>'")
    public void parseBlockWithContent() {
        final String content = "<start>AB<end>";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final ZebraMessage result = results.get(0);
        assertInstanceOf(ZebraMessage.Any.class, result);
        assertEquals("AB", result.getRaw());
    }

    @Test
    @DisplayName("Parse '<start>'")
    public void parseStartTagNoContent() {
        final String content = "<start>";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final ZebraMessage result = results.get(0);
        assertInstanceOf(ZebraMessage.Any.class, result);
        assertEquals("<start>", result.getRaw());
    }

    @Test
    @DisplayName("Parse '<start>AB'")
    public void parseStartTagWithContent() {
        final String content = "<start>AB";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final ZebraMessage result = results.get(0);
        assertInstanceOf(ZebraMessage.Any.class, result);
        assertEquals("AB", result.getRaw());
    }

    @Test
    @DisplayName("Parse '<end>'")
    public void parseEndTagNoContent() {
        final String content = "<end>";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final ZebraMessage result = results.get(0);
        assertInstanceOf(ZebraMessage.Blank.class, result);
    }

    @Test
    @DisplayName("Parse 'AB<end>'")
    public void parseEndTagWithContent() {
        final String content = "AB<end>";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Parse multiple blocks '<start>A<end><start>B<end>' returns multiple messages")
    public void parseMultipleBlocks() {
        final String content = "<start>AB<end><start>CDE<end>";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(2, results.size());

        final ZebraMessage r1 = results.get(0);
        assertEquals("AB", r1.getContentAsText());

        final ZebraMessage r2 = results.get(1);
        assertEquals("CDE", r2.getContentAsText());
    }

    @Test
    public void parseRfidData_110ix4() {
        final String content = "<start>\r\nW,0000,3be1000020a9dcf7773bc3e3\r\n<end>";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final ZebraMessage r1 = results.get(0);
        assertEquals("W,0000,3be1000020a9dcf7773bc3e3", r1.getContentAsText());
        assertInstanceOf(ZebraMessage.RFIDData.class, r1);

        final ZebraMessage.RFIDData data = (ZebraMessage.RFIDData) r1;
        assertEquals(RFIDOperation.WRITE, data.getOperation());
        assertEquals(RFIDStatus.RFID_OK, data.getStatus());
        assertEquals("3be1000020a9dcf7773bc3e3", data.getData());
    }

    @Test
    public void parseRfidData_2() {
        final String content = "<start>\r\nW,0408,3be1000020a9dcf7773bc3e3,0\r\n<end>\r\n";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final ZebraMessage m0 = results.get(0);
        assertEquals("W,0000,3be1000020a9dcf7773bc3e3", m0.getContentAsText());
        assertInstanceOf(ZebraMessage.RFIDData.class, m0);

        final ZebraMessage.RFIDData data = (ZebraMessage.RFIDData) m0;
        assertEquals(RFIDOperation.WRITE, data.getOperation());
        assertEquals(RFIDStatus.RFID_OK, data.getStatus());
        assertEquals("3be1000020a9dcf7773bc3e3", data.getData());

        final ZebraMessage m1 = results.get(1);
        assertEquals("\r\n", m1.getRaw());
    }

    @Test
    public void parseRfidData_3() {
        final String content = "<start>W,0408,3be1000020a9dcf7773bc3e3,0<end>";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final ZebraMessage m0 = results.get(0);
        assertEquals("W,0408,3be1000020a9dcf7773bc3e3,0", m0.getContentAsText());
        assertInstanceOf(ZebraMessage.RFIDData.class, m0);

        final ZebraMessage.RFIDData data = (ZebraMessage.RFIDData) m0;
        assertEquals(RFIDOperation.WRITE, data.getOperation());
        assertEquals(RFIDStatus.INVALID_WRITE_DATA, data.getStatus());
        assertEquals("3be1000020a9dcf7773bc3e3", data.getData());
    }

    @Test
    public void parseRfidData_4() {
        final String content = "<start>R,F1,D3,27,00000000,DATA<end>";
        final RawPacket packet = RawPacket.of(content, StandardCharsets.UTF_8);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(1, results.size());

        final ZebraMessage m0 = results.get(0);
        assertEquals("R,F1,D3,27,00000000,DATA", m0.getContentAsText());
        assertInstanceOf(ZebraMessage.RFIDData.class, m0);

        final ZebraMessage.RFIDData data = (ZebraMessage.RFIDData) m0;
        assertEquals(RFIDOperation.READ, data.getOperation());
        assertEquals("F1", data.getPosition());
        assertEquals("D3", data.getAntenna());
        assertEquals(27, data.getPower());
        assertEquals(RFIDStatus.RFID_OK, data.getStatus());
        assertEquals("DATA", data.getData());
    }

    @Test
    public void parseSample() throws IOException {
        final Charset charset = StandardCharsets.UTF_8;
        final byte[] bytes = resources.getAsBytes("sample.txt");
        final RawPacket packet = new RawPacket(bytes, charset);

        final List<ZebraMessage> results = parser.parse(packet);
        assertEquals(12, results.size());

        final Set<String> epcs = results.stream()
            .map((m) -> {
                if (m instanceof ZebraMessage.RFIDData) {
                    final ZebraMessage.RFIDData data = (ZebraMessage.RFIDData) m;
                    return data.getData();
                }
                return null;
            })
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toSet());
        assertIterableEquals(Arrays.asList("3be1000020a9dcf7773bc3e3", "3be1000020a9dcf7773bc260"), epcs);
    }

}
