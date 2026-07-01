package org.worldscanner.search;

import org.worldscanner.anvil.ChunkData;
import org.worldscanner.model.DimensionType;
import org.worldscanner.model.SearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SearchEngine {

    private SearchEngine() {
    }

    public static List<SearchResult> searchBlocks(ChunkData chunkData, String blockTarget, DimensionType dimension,
                                                 int regionX, int regionZ, SearchFilter filter) {
        if (chunkData == null || blockTarget == null || blockTarget.isBlank()) {
            return Collections.emptyList();
        }
        List<SearchResult> results = new ArrayList<>();
        if (chunkData.getRawNBT() != null && containsKeyword(chunkData.getRawNBT(), blockTarget)) {
            results.add(new SearchResult(
                    dimension,
                    regionX,
                    regionZ,
                    0,
                    64,
                    0,
                    blockTarget,
                    null,
                    null));
        }
        return results;
    }

    public static List<SearchResult> searchItems(ChunkData chunkData, String itemTarget, DimensionType dimension,
                                                int regionX, int regionZ, SearchFilter filter) {
        if (chunkData == null || itemTarget == null || itemTarget.isBlank()) {
            return Collections.emptyList();
        }
        List<SearchResult> results = new ArrayList<>();
        if (chunkData.getRawNBT() != null && containsKeyword(chunkData.getRawNBT(), itemTarget)) {
            results.add(new SearchResult(
                    dimension,
                    regionX,
                    regionZ,
                    0,
                    64,
                    0,
                    null,
                    itemTarget,
                    null));
        }
        return results;
    }

    public static List<SearchResult> searchEntities(ChunkData chunkData, String entityTarget, DimensionType dimension,
                                                   int regionX, int regionZ, SearchFilter filter) {
        if (chunkData == null || entityTarget == null || entityTarget.isBlank()) {
            return Collections.emptyList();
        }
        List<SearchResult> results = new ArrayList<>();
        if (chunkData.getRawNBT() != null && containsKeyword(chunkData.getRawNBT(), entityTarget)) {
            results.add(new SearchResult(
                    dimension,
                    regionX,
                    regionZ,
                    0,
                    64,
                    0,
                    null,
                    null,
                    entityTarget));
        }
        return results;
    }

    public static List<SearchResult> collectAllResults(ChunkData chunkData, DimensionType dimension,
                                                      int regionX, int regionZ, SearchFilter filter) {
        if (chunkData == null) {
            return Collections.emptyList();
        }
        List<SearchResult> results = new ArrayList<>();
        if (chunkData.getRawNBT() != null) {
            results.add(new SearchResult(
                    dimension,
                    regionX,
                    regionZ,
                    0,
                    64,
                    0,
                    "chunk",
                    null,
                    null));
        }
        return results;
    }

    private static boolean containsKeyword(byte[] payload, String keyword) {
        if (payload == null || payload.length == 0 || keyword == null || keyword.isBlank()) {
            return false;
        }
        String haystack = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
        return haystack.toLowerCase(java.util.Locale.ROOT).contains(keyword.toLowerCase(java.util.Locale.ROOT));
    }
}
