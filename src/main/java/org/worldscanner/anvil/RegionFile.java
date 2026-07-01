package org.worldscanner.anvil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RegionFile {

    public static final int SECTOR_BYTES = 4096;
    public static final int HEADER_SIZE = SECTOR_BYTES * 2;
    public static final int CHUNK_COUNT = 1024;

    private final File file;
    private final RandomAccessFile raf;

    private final ChunkLocation[] locations = new ChunkLocation[CHUNK_COUNT];
    private final int[] timestamps = new int[CHUNK_COUNT];

    public RegionFile(File file) throws IOException {
        this.file = file;
        this.raf = new RandomAccessFile(file, "r");

        readHeader();
    }

    public File getFile() {
        return file;
    }

    public ChunkLocation getChunkLocation(int index) {
        return locations[index];
    }

    public int getTimestamp(int index) {
        return timestamps[index];
    }
    private void readHeader() throws IOException {

        raf.seek(0);

        for (int i = 0; i < CHUNK_COUNT; i++) {

            int b1 = raf.readUnsignedByte();
            int b2 = raf.readUnsignedByte();
            int b3 = raf.readUnsignedByte();
            int sectors = raf.readUnsignedByte();

            int offset =
                    (b1 << 16) |
                            (b2 << 8) |
                            b3;

            locations[i] = new ChunkLocation(offset, sectors);

        }

        for (int i = 0; i < CHUNK_COUNT; i++) {

            timestamps[i] = raf.readInt();

        }

    }
    public boolean hasChunk(int index) {

        if (index < 0 || index >= CHUNK_COUNT) {
            return false;
        }

        return locations[index] != null && locations[index].exists();

    }

    public int getChunkCount() {

        int count = 0;

        for (ChunkLocation location : locations) {
            if (location != null && location.exists()) {
                count++;
            }
        }

        return count;

    }

    public ChunkData readChunkData(int index) throws IOException {
        if (!hasChunk(index)) {
            return null;
        }

        ChunkLocation location = locations[index];
        if (location == null || !location.exists()) {
            return null;
        }

        raf.seek(location.getOffset() * SECTOR_BYTES);
        int chunkSize = raf.readInt();
        int compressionType = raf.readByte() & 0xFF;

        byte[] payload = new byte[Math.max(0, chunkSize - 1)];
        int read = raf.read(payload);
        if (read < payload.length) {
            throw new IOException("Unexpected end of chunk data");
        }

        return new ChunkData(index / 32, index % 32, payload);
    }

    public void close() throws IOException {
        raf.close();
    }

}