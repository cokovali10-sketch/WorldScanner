package org.worldscanner.search;

import org.worldscanner.RegionScanner;
import org.worldscanner.model.SearchResult;

import java.util.List;

public class BlockFinder {
    private final RegionScanner regionScanner;

    public BlockFinder(RegionScanner regionScanner) {
        this.regionScanner = regionScanner;
    }

    public List<SearchResult> findBlock(String blockId, SearchFilter filter) {
        return regionScanner.searchBlocks(blockId, filter);
    }
}
