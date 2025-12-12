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

public class ZebraMockTest {

    private static ZebraMock mock;

    private final ResourceUtils resources = ResourceUtils.getInstance();

    @BeforeAll
    public static void init() {
        mock = new ZebraMock();
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
            Arguments.of("~RVy,0", ListUtils.of(new byte[]{ '~', 'R', 'V', 'y', ',', '0' })),
            Arguments.of("~JA", ListUtils.of(new byte[]{ '~', 'J', 'A' })),
            Arguments.of("~HL", ListUtils.of(new byte[]{ '~', 'H', 'L' })),
            Arguments.of("^XA^HL^XZ", ListUtils.of(new byte[]{ '^', 'H', 'L' })),
            Arguments.of("\u0002~HL\u0003", ListUtils.of(new byte[]{ '~', 'H', 'L' })),
            Arguments.of("~RVy,0^XA^FO680,130^AFI,30^FS^XZ",
                ListUtils.of(
                    new byte[]{ '~', 'R', 'V', 'y', ',', '0' },
                    new byte[]{ '^', 'F', 'O', '6', '8', '0', ',', '1', '3', '0', '^', 'A', 'F', 'I', ',', '3', '0', '^', 'F', 'S' }
                ))
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
        final byte[] bytes = resources.getAsBytes("files/ZPL.txt");

        final List<RawPacket> results = mock.parseData(bytes, charset);
        assertEquals(2, results.size());
    }

}
