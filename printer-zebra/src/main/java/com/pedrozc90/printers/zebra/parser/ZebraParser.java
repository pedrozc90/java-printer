package com.pedrozc90.printers.zebra.parser;

import com.pedrozc90.printers.core.Parser;
import com.pedrozc90.printers.core.exceptions.PrinterParserException;
import com.pedrozc90.printers.core.objects.RawPacket;
import com.pedrozc90.printers.zebra.enums.RFIDOperation;
import com.pedrozc90.printers.zebra.enums.RFIDStatus;
import com.pedrozc90.printers.zebra.objects.ZebraMessage;
import org.jboss.logging.Logger;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ZebraParser implements Parser<ZebraMessage> {

    private static final String START_TAG = "<start>";
    private static final String END_TAG = "<end>";

    private static ZebraParser instance;

    private final Logger logger = Logger.getLogger(ZebraParser.class);

    public static ZebraParser getInstance() {
        if (instance == null) {
            instance = new ZebraParser();
        }
        return instance;
    }

    @Override
    public List<ZebraMessage> parse(final RawPacket packet) throws PrinterParserException {
        final List<ZebraMessage> out = new ArrayList<>();

        if (packet != null) {
            final Charset charset = packet.getCharset();
            final String text = packet.toText();

            final int startIdx = text.indexOf(START_TAG);
            final int endIdx = text.indexOf(END_TAG);

            if (startIdx != -1) {
                int length = endIdx - startIdx;
                if (length > 0) {
                    final String subtext = text.substring(startIdx + 7, endIdx);
                    logger.debugf("Found start and end tags in packet: '%s'", subtext);

                    final String[] splited = subtext.split("\\r?\\n");
                    for (String s : splited) {
                        if (s.isEmpty()) continue;
                        final ZebraMessage message = parseRFIDData(s, charset);
                        if (message != null) {
                            out.add(message);
                        }
                    }

                    int k = endIdx + 5;
                    if (k < text.length()) {
                        final String remaining = text.substring(k);
                        final RawPacket p = RawPacket.of(remaining, charset);
                        final List<ZebraMessage> ms = parse(p);
                        out.addAll(ms);
                    }
                } else {
                    final String subtext = text.substring(startIdx + 7);
                    final ZebraMessage.Blank message = new ZebraMessage.Blank(subtext, charset);
                    out.add(message);
                }
            } else if (endIdx != -1) {
                final String subtext = text.substring(0, endIdx);
                final ZebraMessage.Any message = new ZebraMessage.Any(subtext, charset);
                out.add(message);
            } else {
                logger.debugf("No start and end tags found in packet: %s", text);
                final ZebraMessage.Any message = new ZebraMessage.Any(text, charset);
                out.add(message);
            }
        }

        return out;
    }

    private ZebraMessage parseRFIDData(final String value, final Charset charset) {
        if (value == null) return null;

        final String trimmed = value.trim();
        final String[] args = trimmed.split(",");

        if (args.length < 3) {
            return new ZebraMessage.Any(trimmed, charset);
        }

        // 110ix4
        else if (args.length == 3) {
            final RFIDOperation operation = RFIDOperation.get(args[0]);
            final RFIDStatus status = RFIDStatus.get(args[1]);
            final String data = args[2];
            return new ZebraMessage.RFIDData(trimmed, charset, operation, status, data);
        }
        // ???
        else if (args.length == 4) {
            final RFIDOperation operation = RFIDOperation.get(args[0]);
            final RFIDStatus status = RFIDStatus.get(args[1]);
            final String data = args[2];
            final Integer result = Integer.parseInt(args[3]);
            return new ZebraMessage.RFIDData(trimmed, charset, operation, status, data);
        }

        // zt410
        final RFIDOperation operation = RFIDOperation.get(args[0]);
        if (operation == RFIDOperation.RFID_SETTINGS) {
            /* S,RPWR=29,WPWR=29,ANT=A2,PPOS=F0 */
            return null;
        }

        final String programPosition = args[1];
        final String antennaElement = args[2];
        final Integer power = Integer.parseInt(args[3]);
        final RFIDStatus status = RFIDStatus.get(args[4]);
        final String data = args[5];
        return new ZebraMessage.RFIDData(trimmed, charset, null, operation, programPosition, antennaElement, power, status, data);
    }

}
