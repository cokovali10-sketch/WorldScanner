package org.worldscanner.model;

public class SearchResult {
    private final DimensionType dimension;
    private final int regionX;
    private final int regionZ;
    private final int x;
    private final int y;
    private final int z;
    private final String blockId;
    private final String itemId;
    private final String entityId;

    public SearchResult(DimensionType dimension, int regionX, int regionZ, int x, int y, int z,
                        String blockId, String itemId, String entityId) {
        this.dimension = dimension;
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockId = blockId;
        this.itemId = itemId;
        this.entityId = entityId;
    }

    public DimensionType getDimension() {
        return dimension;
    }

    public int getRegionX() {
        return regionX;
    }

    public int getRegionZ() {
        return regionZ;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getBlockId() {
        return blockId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getEntityId() {
        return entityId;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "dimension=" + dimension +
                ", regionX=" + regionX +
                ", regionZ=" + regionZ +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", blockId='" + blockId + '\'' +
                ", itemId='" + itemId + '\'' +
                ", entityId='" + entityId + '\'' +
                '}';
    }
}
