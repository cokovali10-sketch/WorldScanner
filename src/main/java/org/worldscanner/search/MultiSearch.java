package org.worldscanner.search;

import org.worldscanner.RegionScanner;
import org.worldscanner.model.SearchResult;
import org.worldscanner.model.DimensionType;

import java.util.ArrayList;
import java.util.List;

public final class MultiSearch {

    private MultiSearch() {
    }

    public static List<SearchResult> findMany(RegionScanner scanner, List<String> targets, String kind, SearchFilter filter) {
        List<SearchResult> results = new ArrayList<>();
        for (String target : targets) {
            if (target == null || target.isBlank()) {
                continue;
            }
            switch (kind) {
                case "block" -> results.addAll(scanner.searchBlocks(target, filter));
                case "item" -> results.addAll(scanner.searchItems(target, filter));
                case "entity" -> results.addAll(scanner.searchEntities(target, filter));
                default -> {
                }
            }
        }
        return results;
    }
}
