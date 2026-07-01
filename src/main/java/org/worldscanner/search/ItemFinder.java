package org.worldscanner.search;

import org.worldscanner.RegionScanner;
import org.worldscanner.model.SearchResult;

import java.util.List;

public class ItemFinder {
    private final RegionScanner regionScanner;

    public ItemFinder(RegionScanner regionScanner) {
        this.regionScanner = regionScanner;
    }

    public List<SearchResult> findItem(String itemId, SearchFilter filter) {
        return regionScanner.searchItems(itemId, filter);
    }
}
