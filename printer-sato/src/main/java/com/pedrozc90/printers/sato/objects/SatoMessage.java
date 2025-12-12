package com.pedrozc90.printers.sato.objects;

import com.pedrozc90.printers.core.objects.PrinterMessage;
import com.pedrozc90.printers.sato.enums.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.nio.charset.Charset;
import java.util.Arrays;

@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class SatoMessage extends PrinterMessage {

    public SatoMessage(final byte[] bytes, final Charset charset) {
        super(bytes, charset);
    }

    public SatoMessage(final byte b, final Charset charset) {
        super(b, charset);
    }

    public boolean isFramed() {
        return (bytes.length >= 2)
            && (bytes[0] == (byte) 0x02) // STX
            && (bytes[bytes.length - 1] == (byte) 0x03); // ETX
    }

    @EqualsAndHashCode(callSuper = false)
    public static class Any extends SatoMessage {
        public Any(final byte[] bytes, final Charset charset) {
            super(bytes, charset);
        }

        public Any(final byte b, final Charset charset) {
            super(b, charset);
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static class Ack extends SatoMessage {
        public Ack(final byte b, final Charset charset) {
            super(b, charset);
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static class Nak extends SatoMessage {
        public Nak(final byte b, final Charset charset) {
            super(b, charset);
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class PrinterInfo extends SatoMessage {

        private final int length; // number of bytes between STX and ETX
        private final PrinterStatus ps; // printer status
        private final ReceiveBufferStatus rs; // receive buffer status
        private final RibbonStatus re; // ribbon status
        private final MediaStatus pe; // media status
        private final ErrorNumber en; // error number
        private final BatteryStatus bt; // battery status
        private final Integer q;  // remaining number of print

        public PrinterInfo(final byte[] bytes,
                           final Charset charset,
                           final int length,
                           final PrinterStatus ps,
                           final ReceiveBufferStatus rs,
                           final RibbonStatus re,
                           final MediaStatus pe,
                           final ErrorNumber en,
                           final BatteryStatus bt,
                           final Integer q) {
            super(bytes, charset);
            this.length = length;
            this.ps = ps;
            this.rs = rs;
            this.re = re;
            this.pe = pe;
            this.en = en;
            this.bt = bt;
            this.q = q;
        }

        @Override
        public String toString() {
            final String clazz = getClass().getSimpleName();
            return String.format("%s{ bytes = %s, text = '%s', len = %d, ps = %s, rs = %s, re = %s, pe = %s, en = %s, bt = %s, q = '%s' }",
                clazz,
                Arrays.toString(bytes),
                toText(),
                length,
                ps,
                rs,
                re,
                pe,
                en,
                bt,
                q
            );
        }

    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class TagInfo extends SatoMessage {

        private final int length; // data size, number of bytes between STX and ETX (max 5 digits)
        private final String wr; // write result status (0 = Write failure, 1 = Write success)
        private final String es; // error symbol (N = No error, E = EPC write error, T = TID write error, M = MCS error (Chip inconsistent or not supported), A = All errors)
        private final String epc; // epc hexadecimal string
        private final String tid; // tid hexadecimal string

        public TagInfo(final byte[] bytes,
                       final Charset charset,
                       final int length,
                       final String wr,
                       final String es,
                       final String epc,
                       final String tid) {
            super(bytes, charset);
            this.length = length;
            this.wr = wr;
            this.es = es;
            this.epc = epc;
            this.tid = tid;
        }

        @Override
        public String toString() {
            final String clazz = getClass().getSimpleName();
            return String.format("%s{ bytes = %s, len = %d, wr = %s, es = %s, epc = %s, tid =  %s }",
                clazz,
                Arrays.toString(bytes),
                length,
                wr,
                es,
                epc,
                tid
            );
        }

    }

}
