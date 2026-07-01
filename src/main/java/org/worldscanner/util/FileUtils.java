package org.worldscanner.util;

import org.worldscanner.model.DimensionType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Утилиты для поиска региональных файлов и распознавания измерений.
 */
public final class FileUtils {

    private FileUtils() {
    }

    public static Map<File, DimensionType> locateRegionFiles(File worldRoot) {
        Map<File, DimensionType> regionFiles = new HashMap<>();
        collectRegionFiles(new File(worldRoot, "region"), DimensionType.OVERWORLD, regionFiles);
        collectRegionFiles(new File(worldRoot, "DIM-1"), DimensionType.NETHER, regionFiles);
        collectRegionFiles(new File(worldRoot, "DIM1"), DimensionType.END, regionFiles);
        return regionFiles;
    }

    private static void collectRegionFiles(File regionFolder, DimensionType dimension, Map<File, DimensionType> regionFiles) {
        if (regionFolder == null || !regionFolder.exists() || !regionFolder.isDirectory()) {
            return;
        }
        File[] files = regionFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mca"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            regionFiles.put(file, dimension);
        }
    }
}
