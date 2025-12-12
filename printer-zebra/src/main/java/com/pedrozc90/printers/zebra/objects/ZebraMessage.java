package com.pedrozc90.printers.zebra.objects;

import com.pedrozc90.printers.core.objects.PrinterMessage;
import com.pedrozc90.printers.zebra.enums.RFIDOperation;
import com.pedrozc90.printers.zebra.enums.RFIDStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.nio.charset.Charset;
import java.util.Arrays;

@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class ZebraMessage extends PrinterMessage {

    private static final String START_TAG = "<start>";
    private static final String END_TAG = "<end>";
    private static final int START_LENGTH = START_TAG.length(); // length = 7
    private static final int END_LENGTH = END_TAG.length(); // length = 5

    public ZebraMessage(final String value, final Charset charset) {
        super(value, charset);
    }

    @Override
    public boolean isFramed() {
        final int len = bytes.length;
        final String text = toText();
        return ((len >= 2) && (bytes[0] == (byte) 0x02) && (bytes[len - 1] == (byte) 0x03))
            || (text.startsWith(START_TAG) && text.endsWith(END_TAG));
    }

    @Override
    public byte[] getContent() {
        int len = bytes.length;

        int i = raw.indexOf(START_TAG);
        if (i != -1) {
            i += START_LENGTH;
        } else if ((len > 1) && (bytes[0] == (byte) 0x02)) {
            i = 1;
        } else {
            i = 0;
        }

        int j = raw.indexOf(END_TAG);
        if (j != -1) {
            // ignore
        } else if ((len > 1) && (bytes[len - 1] == (byte) 0x03)) {
            j = len - 1;
        } else {
            j = len;
        }

        if (i != 0 || j != len) {
            return Arrays.copyOfRange(bytes, i, j);
        }
        return bytes;
    }

    @Getter
    public static class Any extends ZebraMessage {
        public Any(final String value, final Charset charset) {
            super(value, charset);
        }
    }

    @Getter
    public static class Blank extends ZebraMessage {
        public Blank(final String value, final Charset charset) {
            super(value, charset);
        }
    }

    @Getter
    public static class Empty extends ZebraMessage {
        public Empty() {
            super(null, null);
        }
    }

    @Getter
    public static class RFIDData extends ZebraMessage {

        private final String datetime;          // a time stamp for the log entry (some older versions of firmware, this parameter does not display)
        private final RFIDOperation operation;
        private final String position;          // program position
        private final String antenna;           // antenna element
        private final Integer power;            // read & write power
        private final RFIDStatus status;
        private final String data;

        public RFIDData(final String value,
                        final Charset charset,
                        final String datetime,
                        final RFIDOperation operation,
                        final String position,
                        final String antenna,
                        final Integer power,
                        final RFIDStatus status,
                        final String data) {
            super(value, charset);
            this.datetime = datetime;
            this.operation = operation;
            this.position = position;
            this.antenna = antenna;
            this.power = power;
            this.status = status;
            this.data = data;
        }

        public RFIDData(final String value,
                        final Charset charset,
                        final RFIDOperation operation,
                        final RFIDStatus status,
                        final String data) {
            this(value, charset, null, operation, null, null, null, status, data);
        }
    }

}
