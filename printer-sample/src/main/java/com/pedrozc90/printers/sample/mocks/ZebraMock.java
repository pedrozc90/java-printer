package com.pedrozc90.printers.sample.mocks;

import com.pedrozc90.printers.core.objects.RawPacket;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pedrozc90.printers.core.objects.ControlCmd.*;

public class ZebraMock extends BaseMock {

    private static final String START_FORMAT = "^XA"; // pg. 347
    private static final String END_FORMAT = "^XZ"; // pg. 352

    public ZebraMock() {
        super(StandardCharsets.UTF_8);
    }

    @Override
    public List<RawPacket> parseData(final byte[] data, final Charset charset) {
        final List<RawPacket> out = new ArrayList<>();

        int i = 0;
        int len = data.length;

        while (i < len) {
            byte b = data[i];
            if (b == '^') {
                int xaIndex = -1;
                int xzIndex = -1;
                int j = i;
                for (; j < len - 2; j++) {
                    if (data[j] == '^' && data[j + 1] == 'X' && data[j + 2] == 'A') {
                        xaIndex = j + 3;
                    } else if (data[j] == '^' && data[j + 1] == 'X' && data[j + 2] == 'Z') {
                        xzIndex = j - 1;
                    }
                }

                if (xaIndex != -1 && xzIndex != -1) {
                    final byte[] bytes = Arrays.copyOfRange(data, xaIndex, xzIndex + 1);
                    final RawPacket packet = new RawPacket(bytes, charset);
                    out.add(packet);
                    logger.debugf("Command packet: %s", packet);

                    i = j + 2;
                } else {
                    final byte[] bytes = Arrays.copyOfRange(data, i, len);
                    final RawPacket packet = new RawPacket(bytes, charset);
                    out.add(packet);
                    logger.debugf("Command packet: %s", packet);
                    i = j;
                }
            } else if (b == '~') {
                logger.debugf("Command at '%d'", i, b);

                int j = i + 1;
                for (; j < len; j++) {
                    byte n = data[j];
                    if (n == '^' || n == '~' || n == LF || n == EOF) {
                        break;
                    }
                }

                final byte[] bytes = Arrays.copyOfRange(data, i, j);
                final RawPacket packet = new RawPacket(bytes, charset);
                out.add(packet);
                logger.debugf("Command packet: %s", packet);

                i = j;
            } else if (b == STX) {
                logger.debugf("Start Format at '%d'", i);
                int j = i + 1;
                for (; j < len; j++) {
                    byte n = data[j];
                    if (n == ETX) {
                        break;
                    }
                }

                final byte[] bytes = Arrays.copyOfRange(data, i + 1, j);
                final RawPacket packet = new RawPacket(bytes, charset);
                out.add(packet);

                i = j;
            } else {
                logger.debugf("data[%d] = '%d'", i, b);
                i++;
            }
        }

        return out;
    }

    @Override
    public RawPacket handlePacket(final RawPacket packet) throws IOException {
        final String cmd = packet.toText();
        switch (cmd) {
            case "^HL":
            case "~HL":
                return handleTag();
            case "~PS":
                return handleResume();
            case "~PP":
                return handlePause();
            case "~JA":
                return handleCancel();
            default:
                return handleAny(packet);
        }
    }

    private RawPacket handleTag() {
        logger.debugf("Handler: ~HL");
        return null;
    }

    private RawPacket handleResume() {
        logger.debugf("Handler: ~PS");
        return null;
    }

    private RawPacket handlePause() {
        logger.debugf("Handler: ~PP");
        return null;
    }

    private RawPacket handleCancel() {
        logger.debugf("Handler: ~JA");
        return null;
    }

    private RawPacket handleAny(final RawPacket packet) {
        logger.debugf("Handler: <ANY> - %s", packet);
        return null;
    }

}
