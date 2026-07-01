package org.worldscanner.anvil;

public class ChunkLocation {

    private final int offset;
    private final int sectorCount;

    public ChunkLocation(int offset, int sectorCount) {
        this.offset = offset;
        this.sectorCount = sectorCount;
    }

    public int getOffset() {
        return offset;
    }

    public int getSectorCount() {
        return sectorCount;
    }

    public boolean exists() {
        return offset != 0 && sectorCount != 0;
    }

    @Override
    public String toString() {
        return "ChunkLocation{" +
                "offset=" + offset +
                ", sectorCount=" + sectorCount +
                '}';
    }
}