package org.worldscanner.search;

import org.worldscanner.RegionScanner;
import org.worldscanner.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

public final class CombinedSearch {

    private CombinedSearch() {
    }

    public static List<SearchResult> search(RegionScanner scanner, List<String> blockTargets, List<String> itemTargets, List<String> entityTargets, SearchFilter filter) {
        List<SearchResult> results = new ArrayList<>();
        for (String block : blockTargets) {
            results.addAll(scanner.searchBlocks(block, filter));
        }
        for (String item : itemTargets) {
            results.addAll(scanner.searchItems(item, filter));
        }
        for (String entity : entityTargets) {
            results.addAll(scanner.searchEntities(entity, filter));
        }
        return results;
    }
}
