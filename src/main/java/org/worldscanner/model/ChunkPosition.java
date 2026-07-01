package org.worldscanner.model;

/**
 * Координаты чанка внутри мира Minecraft.
 */
public final class ChunkPosition {

    private final int regionX;
    private final int regionZ;
    private final int chunkX;
    private final int chunkZ;

    public ChunkPosition(int regionX, int regionZ, int chunkX, int chunkZ) {
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getRegionX() {
        return regionX;
    }

    public int getRegionZ() {
        return regionZ;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }
}
