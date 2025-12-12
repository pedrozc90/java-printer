package com.pedrozc90.printers.sample.mocks;

import com.pedrozc90.printers.core.objects.RawPacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.pedrozc90.printers.core.objects.ControlCmd.*;

public class SatoMock extends BaseMock {

    // responses
    private static final String PK_PAYLOAD = "%d,1,N,EP:%s,ID:%s\r\n";
    private static final String PG_PAYLOAD = "32,PS%s,RS0,RE0,PE0,EN00,BT0,Q%06d";
    private static final String IP0 = "\u001BIP0";

    private final Deque<String> buffer = new ArrayDeque<>(64);
    private boolean printing = false;
    private boolean paused = false;
    private int status = 0; // 0 = standby, 1 = waiting, 2 = analyzing, 3 = printing, 4 = offline, 5 = error

    public SatoMock() {
        super(StandardCharsets.UTF_8);
    }

    @Override
    public List<RawPacket> parseData(final byte[] data, final Charset charset) {
        final List<RawPacket> out = new ArrayList<>();

        int i = 0;
        int len = data.length;

        while (i < len) {
            byte b = data[i];

            if (b == STX) {
                int stx = i;
                logger.tracef("data[%d] = '%d' (STX)", i, b);

                int etx = len - 1;
                for (int j = i + 1; j < len; j++) {
                    byte n = data[j];
                    if (n == ETX) {
                        etx = j;
                        break;
                    }
                }

                // final byte[] bytes = Arrays.copyOfRange(data, stx, etx + 1); // contains stx and etx bytes
                final byte[] bytes = Arrays.copyOfRange(data, stx + 1, etx);

                final List<RawPacket> tmp = parseData(bytes, charset);
                out.addAll(tmp);

                i = etx + 1;
            } else if (b == ETX) {
                logger.tracef("data[%d] = '%d' (ETX)", i, b);
            } else if (b == ESC) {
                logger.tracef("data[%d] = '%d' (ESC)", i, b);

                byte n = data[i + 1];
                if (n == 'A') {
                    logger.tracef("data[%d] = '%d' (ESC + A)", i, b);
                    int escAIndex = i;
                    int escZIndex = len - 1;

                    for (int j = i + 2, k = j + 1; j < len && k < len; j++, k += 2) {
                        byte b2 = data[j];
                        byte b3 = data[k];
                        if (b2 == ESC && b3 == 'Z') {
                            escZIndex = k;
                            break;
                        }
                    }

                    final byte[] bytes = Arrays.copyOfRange(data, escAIndex, escZIndex + 1);
                    final RawPacket packet = new RawPacket(bytes, charset);
                    out.add(packet);
                    logger.debugf("coded payload: %s", packet);

                    i = escZIndex + 1;
                }
            } else if (b == DC2 || b == DC1 || b == DLE) {
                logger.tracef("data[%d] = '%d' (DC2 or DC1 or DLE)", i, b);

                int j = i + 1;
                for (; j < len; j++) {
                    byte n = data[j];
                    if (n == STX || n == ETX || n == DC2 || n == DC1 || n == DLE || n == LF) {
                        break;
                    }
                }

                final byte[] bytes = Arrays.copyOfRange(data, i, j);
                final RawPacket packet = new RawPacket(bytes, charset);
                out.add(packet);
                logger.debugf("Command packet: %s", packet);

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
            // DC2 + PG (Get printer status)
            case "\u0012PG":
                return onPG();
            // DC2 + PK (Get printer information)
            case "\u0012PK":
                return onPK();
            // DC2 + PH: Cancel
            case "\u0012PH":
                return onCancel();
            // DLE + H: Pause
            case "\u0010H":
                return onPause();
            // DC1 + H: Resume
            case "\u0011H":
                return onResume();
            default:
                return onAny(packet);
        }
    }

    private RawPacket onPG() throws IOException {
        logger.debugf("Handler: <DC2 + PG> - Get printer status");
        final int ps = status;
        final int remaining = buffer.size();
        final String value = String.format(PG_PAYLOAD, ps, remaining);
        return createFramedPacket(value);
    }

    private RawPacket onPK() throws IOException {
        logger.infof("Handler: <DC2 + PK> - Get EPC/TID");

        if (buffer.isEmpty()) {
            return null;
        }

        final String epc = buffer.pop();
        final String tid = epc.replaceAll("\\w", "0");
        final int bytes = epc.length() + tid.length() + 13;
        // [STX]25,1,N,ID:E200680612345678[CR][LF][ETX]
        // [STX]53,1,N,EP:E0123456789ABCDEF0123456,ID:E200680612345678[CR][LF][ETX]
        // [STX]x,1,N,EP:y,ID:zrn[ETX]
        final String value = String.format(PK_PAYLOAD, bytes, epc, tid);
        return createFramedPacket(value);
    }

    private RawPacket onCancel() {
        logger.debugf("Handler: <CD2 + PH> - Cancel printing");

        printing = false;
        paused = false;
        buffer.clear();

        return createPacket(ACK);
    }

    private RawPacket onPause() {
        logger.infof("Handler: <DLE + H> - Pause printing");

        if (paused) {
            return createPacket(NAK);
        }

        paused = true;
        return createPacket(ACK);
    }

    private RawPacket onResume() {
        logger.infof("Handler: <DC1 + H> - Resume printing");

        if (!paused) {
            return createPacket(NAK);
        }

        paused = false;
        return createPacket(ACK);
    }

    // extract epc from "epc:<value>,fsw:0"
    final Pattern pattern = Pattern.compile("epc:([^,;]+)");


    private RawPacket onAny(final RawPacket payload) {
        final String text = payload.toText();

        // check if contains label body
        if (text.contains(IP0)) {
            // collect epcs
            final Matcher matcher = pattern.matcher(text);
            int n = 1;
            while (matcher.find()) {
                final String epc = matcher.group(n++);
                buffer.add(epc);
                printing = true;
                paused = false;
            }
        } else {
            logger.warnf("Unknown command: %s", payload);
        }
        return null;
    }

    // HELPERS
    private RawPacket createPacket(final byte[] bytes) {
        return new RawPacket(bytes, charset);
    }

    private RawPacket createPacket(final byte b) {
        return new RawPacket(new byte[]{ b }, charset);
    }

    private RawPacket createFramedPacket(final byte[] bytes) throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bout.write(STX); // STX
        bout.write(bytes);
        bout.write(ETX); // ETX

        final byte[] framed = bout.toByteArray();

        return createPacket(framed);
    }

    private RawPacket createFramedPacket(final String value) throws IOException {
        final byte[] bytes = value.getBytes(charset);
        return createFramedPacket(bytes);
    }

}
