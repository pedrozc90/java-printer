package com.pedrozc90.printers.tests.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class ResourceUtils {

    private static ResourceUtils instance;

    private final ClassLoader loader = getClass().getClassLoader();

    public static ResourceUtils getInstance() {
        if (instance == null) {
            instance = new ResourceUtils();
        }
        return instance;
    }

    public byte[] getAsBytes(final String filename) throws IOException {
        try (final InputStream stream = loader.getResourceAsStream(filename)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + filename);
            }

            final ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;

            while ((length = stream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toByteArray();
        }
    }

    public String getAsString(final String filename, final Charset charset) throws IOException {
        final byte[] bytes = getAsBytes(filename);
        return new String(bytes, charset);
    }

}
