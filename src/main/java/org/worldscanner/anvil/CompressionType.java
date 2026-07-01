package org.worldscanner.anvil;

public enum CompressionType {

    GZIP(1),
    ZLIB(2),
    UNCOMPRESSED(3),
    LZ4(4);

    private final int id;

    CompressionType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static CompressionType fromId(int id) {

        for (CompressionType type : values()) {
            if (type.id == id) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unknown compression type: " + id
        );
    }

}