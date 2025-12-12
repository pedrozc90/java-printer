package com.pedrozc90.printers.sato.parser;

import com.pedrozc90.printers.core.Parser;
import com.pedrozc90.printers.core.exceptions.PrinterParserException;
import com.pedrozc90.printers.core.objects.RawPacket;
import com.pedrozc90.printers.sato.enums.*;
import com.pedrozc90.printers.sato.objects.SatoMessage;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple parser for SATO messages. Consumes a RawPacket (raw bytes) and emits zero or more SatoMessage
 * objects depending on the bytes contained in the packet.
 * <p>
 * Behaviour:
 * - Scans the byte buffer sequentially.
 * - For each 0x02 (STX) .. 0x03 (ETX) framed region, extracts ASCII payload between them,
 * strips trailing CRLF if present and attempts to parse it as either a PK (RequestTag) or PG (PrinterStatus).
 * - For single bytes outside frames: emits ACK (0x06) or NAK (0x15) messages.
 * <p>
 * Note: this parser is intentionally permissive / heuristic-based; it's meant to sit on top of a generic reader.
 */
public final class SatoParser implements Parser<SatoMessage> {

    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;

    private static SatoParser instance;

    public static SatoParser getInstance() {
        if (instance == null) {
            instance = new SatoParser();
        }
        return instance;
    }

    @Override
    public List<SatoMessage> parse(final RawPacket p) throws PrinterParserException {
        final List<SatoMessage> out = new ArrayList<>();
        if (p == null || p.getBytes() == null || p.getBytes().length == 0) {
            return out;
        }

        final Charset charset = p.getCharset();
        final byte[] data = p.getBytes();

        int idx = 0;
        while (idx < data.length) {
            byte b = data[idx];

            if (b == STX) {
                // find ETX
                int j = idx + 1;
                while (j < data.length && data[j] != ETX) j++;
                if (j >= data.length) {
                    // no ETX in this RawPacket - incomplete framed message, stop parsing
                    break;
                }

                // extract payload between STX and ETX
                int payloadStart = idx + 1;
                int payloadLen = j - payloadStart;
                String payload = new String(data, payloadStart, payloadLen, charset);
                // strip trailing CRLF if present
                if (payload.endsWith("\r\n")) {
                    payload = payload.substring(0, payload.length() - 2);
                } else if (payload.endsWith("\n")) {
                    payload = payload.substring(0, payload.length() - 1);
                }

                // Try to parse payload heuristically as PK (RequestTag) or PG (PrinterStatus)
                SatoMessage maybe = parseFramedPayload(payload, charset);
                if (maybe != null) {
                    out.add(maybe);
                }
                // advance past ETX
                idx = j + 1;
            } else {
                // outside framed block: treat single-byte control responses
                // final String hex = String.format("<0x%02X>", b);
                if (b == ACK) {
                    out.add(new SatoMessage.Ack(b, charset));
                } else if (b == NAK) {
                    out.add(new SatoMessage.Nak(b, charset));
                } else {
                    // Unknown/unexpected single byte: ignore it (or log); advance
                    // If you prefer to capture other bytes, add a corresponding message class.
                    out.add(new SatoMessage.Any(b, charset));
                }
                idx++;
            }
        }

        return out;
    }

    /**
     * Heuristic parser for a framed ASCII payload.
     * Recognizes:
     * - PK-like (RequestTag) payloads of form: a,b,c,d  (use split(",",4))
     * where d contains EP:... or ID:...
     * - PG-like (PrinterStatus) payloads of form: a,PSx,RSx,REx,PEx,ENxx,BTx,Qxxxxxx
     * <p>
     * Returns a concrete SatoMessage (RequestTag or PrinterStatus) or null if unrecognized.
     */
    private SatoMessage parseFramedPayload(final String payload, final Charset charset) {
        if (payload == null || payload.isEmpty()) return null;

        // Try PK-like: split into up to 4 parts so 'd' remains intact even if it contains commas
        final String[] parts = payload.split(",", 4);

        // If we have at least 4 parts and the last part contains EP: or ID: then it's very likely PK
        if (parts.length >= 4 && containsTagIdentifiers(parts[3])) {
            return buildRequestTag(payload, parts, charset);
        }

        // Otherwise, test for PG-like pattern: tokens after the first may start with PS,RS,RE,PE,EN,BT,Q
        if (payload.contains("PS") || payload.contains("RS") || payload.contains("RE,") || payload.contains("PE") || payload.contains("EN") || payload.contains("BT") || payload.contains("Q")) {
            return buildPrinterStatus(payload, charset);
        }

        // Fallback: if it looks like "a,b,c,d" but d doesn't have EP/ID, still try to interpret as tag response (less likely)
        if (parts.length >= 4) {
            return buildRequestTag(payload, parts, charset);
        }

        // Unknown framed payload
        return new SatoMessage.Any(payload.getBytes(charset), charset);
    }

    private boolean containsTagIdentifiers(final String s) {
        if (s == null) return false;
        String up = s.toUpperCase();
        return up.contains("EP:") || up.startsWith("EP") || up.contains("ID:") || up.startsWith("ID") || up.contains("EPC:") || up.contains("TID:");
    }

    private SatoMessage buildRequestTag(final String payload, final String[] parts, final Charset charset) {
        // parts[0] = a (bytes), parts[1] = wr (0/1), parts[2] = es (error symbol), parts[3] = d (EP/ID data)
        int bytesVal = 0;
        try {
            bytesVal = Integer.parseInt(parts[0].trim());
        } catch (Exception ignored) {
            /* leave 0 if not parseable */
        }

        final String wr = parts.length > 1 ? parts[1].trim() : "";
        final String es = parts.length > 2 ? parts[2].trim() : "";
        final String d = parts.length > 3 ? parts[3].trim() : "";

        String epc = null;
        String tid = null;

        // d may contain multiple comma-separated tokens such as "EP:...,ID:..."
        final String[] tokens = d.split(",");
        for (String token : tokens) {
            String t = token.trim();
            int colon = t.indexOf(':');
            if (colon > 0) {
                String key = t.substring(0, colon).trim().toUpperCase();
                String val = t.substring(colon + 1).trim();
                if (key.equals("EP") || key.equals("EPC")) epc = val;
                else if (key.equals("ID") || key.equals("TID")) tid = val;
            } else {
                // if no colon, try to infer by prefix
                String up = t.toUpperCase();
                if (up.startsWith("EP")) epc = t.substring(2).trim();
                else if (up.startsWith("ID") || up.startsWith("TI")) tid = t.replaceFirst("^[A-Z]+", "").trim();
            }
        }

        return new SatoMessage.TagInfo(payload.getBytes(charset), charset, bytesVal, wr, es, epc, tid);
    }

    private SatoMessage buildPrinterStatus(final String payload, final Charset charset) {
        // expected: "a,PS0,RS0,RE0,PE0,EN00,BT0,Q000000"
        final String[] tokens = payload.split(",");
        int bytesVal = 0;
        if (tokens.length > 0) {
            try {
                bytesVal = Integer.parseInt(tokens[0].trim());
            } catch (Exception ignored) {
                // ignore
            }
        }

        // map token key -> value (e.g. "PS0" -> key "PS", value "0")
        final Map<String, String> kv = new HashMap<>();
        for (int i = 1; i < tokens.length; i++) {
            String t = tokens[i].trim();
            if (t.isEmpty()) continue;
            int pos = 0;
            while (pos < t.length() && Character.isLetter(t.charAt(pos))) pos++;
            if (pos == 0) continue;
            String key = t.substring(0, pos).toUpperCase();
            String val = t.substring(pos);
            kv.put(key, val);
        }

        final Integer _ps = Integer.parseInt(kv.get("PS"));
        final Integer _rs = Integer.parseInt(kv.get("RS"));
        final Integer _re = Integer.parseInt(kv.get("RE"));
        final Integer _pe = Integer.parseInt(kv.get("PE"));
        final Integer _en = Integer.parseInt(kv.get("EN"));
        final Integer _bt = Integer.parseInt(kv.get("BT"));
        final Integer q = Integer.parseInt(kv.get("Q"));

        final PrinterStatus ps = PrinterStatus.get(_ps);
        final ReceiveBufferStatus rs = ReceiveBufferStatus.get(_rs);
        final RibbonStatus re = RibbonStatus.get(_re);
        final MediaStatus pe = MediaStatus.get(_pe);
        final ErrorNumber en = ErrorNumber.get(_en);
        final BatteryStatus bt = BatteryStatus.get(_bt);

        return new SatoMessage.PrinterInfo(payload.getBytes(charset), charset, bytesVal, ps, rs, re, pe, en, bt, q);
    }

}
