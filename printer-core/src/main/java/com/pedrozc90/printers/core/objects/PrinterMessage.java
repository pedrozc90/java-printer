package com.pedrozc90.printers.core.objects;

import com.pedrozc90.printers.core.utils.CmdUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.nio.charset.Charset;
import java.util.Arrays;

@Getter
@EqualsAndHashCode
public abstract class PrinterMessage {

    protected final byte[] bytes;
    protected final Charset charset;
    protected final String raw;

    public PrinterMessage(final byte[] bytes, final Charset charset) {
        this.bytes = (bytes != null) ? bytes : new byte[0];
        this.charset = (charset != null) ? charset : Charset.defaultCharset();
        this.raw = new String(this.bytes, this.charset);
    }

    public PrinterMessage(final byte b, final Charset charset) {
        this(new byte[]{ b }, charset);
    }

    public PrinterMessage(final String value, final Charset charset) {
        this.raw = value;
        this.charset = (charset != null) ? charset : Charset.defaultCharset();
        this.bytes = (value != null) ? value.getBytes(this.charset) : new byte[0];
    }

    public String toText() {
        return new String(bytes, charset);
    }

    public String toHex() {
        return CmdUtils.toHex(bytes);
    }

    public abstract boolean isFramed();

    public byte[] getContent() {
        if (isFramed()) {
            return Arrays.copyOfRange(bytes, 1, bytes.length - 1);
        }
        return bytes;
    }

    public String getContentAsText() {
        return new String(getContent(), charset);
    }

    public String getContentAsHex() {
        return new String(getContent(), charset);
    }

    @Override
    public String toString() {
        final String clazz = getClass().getSimpleName();
        return String.format("%s{ bytes = %s, charset = %s, framed = %b, text = '%s', hex = '%s' }", clazz, Arrays.toString(bytes), charset, isFramed(), toText(), toHex());
    }

}
