package org.worldscanner.search;

import org.worldscanner.RegionScanner;
import org.worldscanner.model.SearchResult;

import java.util.List;

public class EntityFinder {
    private final RegionScanner regionScanner;

    public EntityFinder(RegionScanner regionScanner) {
        this.regionScanner = regionScanner;
    }

    public List<SearchResult> findEntity(String entityId, SearchFilter filter) {
        return regionScanner.searchEntities(entityId, filter);
    }
}
