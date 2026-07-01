package org.worldscanner.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class CompressionUtils {

    public static InputStream createStream(int compressionType, byte[] data) throws IOException {

        return switch (compressionType) {

            case 1 -> new GZIPInputStream(new ByteArrayInputStream(data));

            case 2 -> new InflaterInputStream(new ByteArrayInputStream(data));

            case 3 -> new ByteArrayInputStream(data);

            default ->
                    throw new IOException("Unknown compression type: " + compressionType);

        };

    }

}