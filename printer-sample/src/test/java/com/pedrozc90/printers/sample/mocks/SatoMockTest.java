package com.pedrozc90.printers.sample.mocks;

import com.pedrozc90.printers.core.objects.RawPacket;
import com.pedrozc90.printers.tests.utils.ListUtils;
import com.pedrozc90.printers.tests.utils.ResourceUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SatoMockTest {

    private static SatoMock mock;

    private final ResourceUtils resources = ResourceUtils.getInstance();

    @BeforeAll
    public static void init() {
        mock = new SatoMock();
    }

    @AfterAll
    public static void close() throws IOException {
        mock.close();
    }

    @Test
    @DisplayName("Create mock")
    public void createMock() {
        assertEquals("0.0.0.0", mock.getHost());
        assertTrue(mock.getPort() != 0);
    }

    static Stream<Arguments> data() {
        return Stream.of(
            // DC2 + <COMMAND>
            Arguments.of("\u0002\u0012PH\u0003", ListUtils.of(new byte[]{ 0x12, 'P', 'H' })),
            Arguments.of("\u0012PH", ListUtils.of(new byte[]{ 0x12, 'P', 'H' })),
            // DC1 + <COMMAND>
            Arguments.of("\u0002\u0011H\u0003", ListUtils.of(new byte[]{ 0x11, 'H' })),
            Arguments.of("\u0011H", ListUtils.of(new byte[]{ 0x11, 'H' })),
            // DLE + <COMMAND>
            Arguments.of("\u0002\u0010H\u0003", ListUtils.of(new byte[]{ 0x10, 'H' })),
            Arguments.of("\u0010H", ListUtils.of(new byte[]{ 0x10, 'H' })),
            // Combined
            Arguments.of("\u0002\u0012PK\u0012PG\u0003", ListUtils.of(new byte[]{ 0x12, 'P', 'K' }, new byte[]{ 0x12, 'P', 'G' })),
            Arguments.of("\u0012PK\u0012PG", ListUtils.of(new byte[]{ 0x12, 'P', 'K' }, new byte[]{ 0x12, 'P', 'G' })),
            // SBPL File
            // ESC + A = start block
            // ESC + Z = stop block
            Arguments.of("\u0002\u001BA\u001BPI0\u001BZ\u0003", ListUtils.of(new byte[]{ 0x1B, 'A', 0x1B, 'P', 'I', '0', 0x1B, 'Z' })),
            Arguments.of("\u0002\u001BA\u001BPI0\u001BZ\u0003", ListUtils.of(new byte[]{ 0x1B, 'A', 0x1B, 'P', 'I', '0', 0x1B, 'Z' })),
            Arguments.of("\u0002\u001BA\u001BPI0\u001BZ\u0003\u0002\u001BA\u001BPI0\r\n\u001BQ0\u001BZ\u0003", ListUtils.of(new byte[]{ 0x1B, 'A', 0x1B, 'P', 'I', '0', 0x1B, 'Z' }, new byte[]{ 0x1B, 'A', 0x1B, 'P', 'I', '0', '\r', '\n', 0x1B, 'Q', '0', 0x1B, 'Z' })),
            Arguments.of("\u0002\u001BA\u001BPI0\u001BZ\u0003\r\n\r\r\n\n\u0002\u001BA\u001BQ0\u001BZ\u0003", ListUtils.of(new byte[]{ 0x1B, 'A', 0x1B, 'P', 'I', '0', 0x1B, 'Z' }, new byte[]{ 0x1B, 'A', 0x1B, 'Q', '0', 0x1B, 'Z' }))
        );
    }

    @ParameterizedTest
    @MethodSource(value = "data")
    public void parseData(final String value, final List<byte[]> expects) {
        final Charset charset = StandardCharsets.UTF_8;
        final byte[] bytes = value.getBytes(charset);

        final List<RawPacket> results = mock.parseData(bytes, charset);
        assertEquals(expects.size(), results.size());

        for (int i = 0; i < expects.size(); i++) {
            final RawPacket result = results.get(i);
            final byte[] expected = expects.get(i);
            assertArrayEquals(expected, result.getBytes());
        }
    }

    @Test
    public void parseSBPL() throws IOException {
        final Charset charset = StandardCharsets.UTF_8;
        final byte[] bytes = resources.getAsBytes("files/SBPL.txt");

        final List<RawPacket> results = mock.parseData(bytes, charset);
        assertEquals(3, results.size());
    }

}
