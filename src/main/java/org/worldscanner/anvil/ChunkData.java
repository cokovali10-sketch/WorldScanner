package org.worldscanner.anvil;

public class ChunkData {

    private final int x;
    private final int z;

    private final byte[] rawNBT;

    public ChunkData(int x, int z, byte[] rawNBT) {

        this.x = x;
        this.z = z;

        this.rawNBT = rawNBT;

    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public byte[] getRawNBT() {
        return rawNBT;
    }

}