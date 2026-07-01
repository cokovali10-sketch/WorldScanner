package org.worldscanner;

import org.worldscanner.anvil.ChunkData;
import org.worldscanner.anvil.RegionFile;
import org.worldscanner.model.DimensionType;
import org.worldscanner.model.SearchResult;
import org.worldscanner.search.SearchEngine;
import org.worldscanner.search.SearchFilter;
import org.worldscanner.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Класс, который сканирует папку мира и выдаёт найденные регионы и чанки.
 */
public class RegionScanner {

    private final File worldRoot;
    private final List<File> regionFiles;
    private final Map<File, DimensionType> regionDimensionMap;

    public RegionScanner(File worldRoot) {
        this.worldRoot = worldRoot;
        this.regionDimensionMap = FileUtils.locateRegionFiles(worldRoot);
        this.regionFiles = new ArrayList<>(regionDimensionMap.keySet());
    }

    public void scanWorld() {
        if (regionFiles.isEmpty()) {
            Console.warn("No region files found in the world directory.");
            return;
        }

        Console.info("Scanning world: " + worldRoot.getAbsolutePath());
        Console.info("Detected region files: " + regionFiles.size());

        Map<DimensionType, Long> dimensionCounts = new HashMap<>();
        regionDimensionMap.forEach((file, dimension) -> dimensionCounts.merge(dimension, 1L, Long::sum));

        dimensionCounts.forEach((dimension, count) -> System.out.println(dimension + ": " + count + " region files"));
        regionFiles.stream().map(File::getName).sorted().forEach(System.out::println);
    }

    public List<SearchResult> scanWorldForResults() {
        SearchFilter filter = new SearchFilter(DimensionType.UNKNOWN, null, null, null, null, null, null);
        List<SearchResult> results = new ArrayList<>();
        for (File file : regionFiles) {
            DimensionType dimension = regionDimensionMap.getOrDefault(file, DimensionType.UNKNOWN);
            results.addAll(scanRegionFile(file, dimension, filter, null, null, null));
        }
        return results;
    }

    public List<SearchResult> searchBlocks(String blockId, SearchFilter filter) {
        if (blockId == null || blockId.isBlank()) {
            return Collections.emptyList();
        }
        List<SearchResult> results = new ArrayList<>();
        for (File file : regionFiles) {
            DimensionType dimension = regionDimensionMap.getOrDefault(file, DimensionType.UNKNOWN);
            results.addAll(scanRegionFile(file, dimension, filter, blockId, null, null));
        }
        return results;
    }

    public List<SearchResult> searchItems(String itemId, SearchFilter filter) {
        if (itemId == null || itemId.isBlank()) {
            return Collections.emptyList();
        }
        List<SearchResult> results = new ArrayList<>();
        for (File file : regionFiles) {
            DimensionType dimension = regionDimensionMap.getOrDefault(file, DimensionType.UNKNOWN);
            results.addAll(scanRegionFile(file, dimension, filter, null, itemId, null));
        }
        return results;
    }

    public List<SearchResult> searchEntities(String entityId, SearchFilter filter) {
        if (entityId == null || entityId.isBlank()) {
            return Collections.emptyList();
        }
        List<SearchResult> results = new ArrayList<>();
        for (File file : regionFiles) {
            DimensionType dimension = regionDimensionMap.getOrDefault(file, DimensionType.UNKNOWN);
            results.addAll(scanRegionFile(file, dimension, filter, null, null, entityId));
        }
        return results;
    }

    private List<SearchResult> scanRegionFile(File regionFile,
                                              DimensionType dimension,
                                              SearchFilter filter,
                                              String blockTarget,
                                              String itemTarget,
                                              String entityTarget) {
        if (!filter.matchesDimension(dimension)) {
            return Collections.emptyList();
        }

        RegionFile region;
        try {
            region = new RegionFile(regionFile);
        } catch (IOException e) {
            Console.error("Failed to open region file: " + regionFile.getName() + " - " + e.getMessage());
            return Collections.emptyList();
        }

        List<Callable<List<SearchResult>>> tasks = new ArrayList<>();
        for (int chunkIndex = 0; chunkIndex < RegionFile.CHUNK_COUNT; chunkIndex++) {
            final int index = chunkIndex;
            if (!region.hasChunk(index)) {
                continue;
            }
            tasks.add(() -> scanRegionChunk(region, regionFile, index, dimension, filter, blockTarget, itemTarget, entityTarget));
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));
        List<SearchResult> results = new ArrayList<>();
        try {
            List<Future<List<SearchResult>>> futures = executor.invokeAll(tasks);
            for (Future<List<SearchResult>> future : futures) {
                try {
                    List<SearchResult> chunkResults = future.get();
                    if (chunkResults != null) {
                        results.addAll(chunkResults);
                    }
                } catch (ExecutionException e) {
                    Console.error("Chunk task failed: " + e.getCause().getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Console.error("World scan was interrupted.");
        } finally {
            executor.shutdown();
        }

        try {
            region.close();
        } catch (IOException ignored) {
        }
        return results;
    }

    private List<SearchResult> scanRegionChunk(RegionFile region,
                                               File regionFile,
                                               int chunkIndex,
                                               DimensionType dimension,
                                               SearchFilter filter,
                                               String blockTarget,
                                               String itemTarget,
                                               String entityTarget) {
        List<SearchResult> results = new ArrayList<>();
        ChunkData chunkData;
        try {
            chunkData = region.readChunkData(chunkIndex);
        } catch (IOException e) {
            Console.error("Failed to read chunk data in " + regionFile.getName() + ": " + e.getMessage());
            return results;
        }

        int[] regionCoords = parseRegionCoords(regionFile.getName());
        int regionX = regionCoords[0];
        int regionZ = regionCoords[1];

        if (!filter.matchesRegion(regionX, regionZ)) {
            return results;
        }

        if (blockTarget != null) {
            results.addAll(SearchEngine.searchBlocks(chunkData, blockTarget, dimension, regionX, regionZ, filter));
        }
        if (itemTarget != null) {
            results.addAll(SearchEngine.searchItems(chunkData, itemTarget, dimension, regionX, regionZ, filter));
        }
        if (entityTarget != null) {
            results.addAll(SearchEngine.searchEntities(chunkData, entityTarget, dimension, regionX, regionZ, filter));
        }
        if (blockTarget == null && itemTarget == null && entityTarget == null) {
            results.addAll(SearchEngine.collectAllResults(chunkData, dimension, regionX, regionZ, filter));
        }

        return results;
    }

    private int[] parseRegionCoords(String fileName) {
        String[] parts = fileName.replace(".mca", "").split("\\.");
        if (parts.length != 3) {
            return new int[]{0, 0};
        }
        try {
            return new int[]{Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
        } catch (NumberFormatException ignored) {
            return new int[]{0, 0};
        }
    }
}
