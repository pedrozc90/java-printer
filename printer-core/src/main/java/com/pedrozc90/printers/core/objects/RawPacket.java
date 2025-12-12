package com.pedrozc90.printers.core.objects;

import com.pedrozc90.printers.core.utils.CmdUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.charset.Charset;
import java.util.Arrays;

@Data
@EqualsAndHashCode
public class RawPacket {

    private final byte[] bytes;
    private final Charset charset;
    private final long timestamp = System.currentTimeMillis();

    public RawPacket(final byte[] bytes, final Charset charset) {
        this.bytes = (bytes != null) ? Arrays.copyOf(bytes, bytes.length) : new byte[0];
        this.charset = (charset != null) ? charset : Charset.defaultCharset();
    }

    public RawPacket(final byte b, final Charset charset) {
        this(new byte[]{ b }, charset);
    }

    public int length() {
        return (bytes != null) ? bytes.length : -1;
    }

    public String toHex() {
        return CmdUtils.toHex(bytes);
    }

    public String toText() {
        return new String(bytes, charset);
    }

    @Override
    public String toString() {
        final String clazz = getClass().getSimpleName();
        return String.format("%s{ bytes = %s, charset = %s, ts = %s, len = %d, hex = '%s', text = '%s' }", clazz, Arrays.toString(bytes), charset, timestamp, length(), toHex(), toText());
    }

    public static RawPacket of(final String value, final Charset charset) {
        final byte[] bytes = value.getBytes(charset);
        return new RawPacket(bytes, charset);
    }

}
