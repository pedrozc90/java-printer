package com.contare.printers.core.utils;

import java.nio.charset.Charset;

public class CmdUtils {

    public static String toHex(final String cmd, final Charset charset) {
        final byte[] bytes = cmd.getBytes(charset);
        return toHex(bytes);
    }

    public static String toHex(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder(bytes.length * 6); // "<0xHH>" has 6 characters
        for (byte b : bytes) {
            final String hex = String.format("<0x%02X>", b & 0xFF);
            sb.append(hex);
        }
        return sb.toString();
    }

}
