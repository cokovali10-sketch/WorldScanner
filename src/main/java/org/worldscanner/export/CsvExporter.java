package org.worldscanner.export;

import org.worldscanner.model.SearchResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Экспорт результатов поиска в CSV формат.
 */
public final class CsvExporter {

    private CsvExporter() {
    }

    public static void export(List<SearchResult> results, String outputPath) {
        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException("Output path is required for CSV export.");
        }

        File outputFile = new File(outputPath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("type,identifier,dimension,regionX,regionZ,chunkX,chunkZ,blockX,blockY,blockZ,additionalInfo");
            writer.newLine();
            for (SearchResult result : results) {
                writer.write(escape(result.getBlockId() != null ? result.getBlockId() : result.getItemId() != null ? result.getItemId() : result.getEntityId()));
                writer.write(',');
                writer.write(escape(result.getBlockId() != null ? "block" : result.getItemId() != null ? "item" : "entity"));
                writer.write(',');
                writer.write(escape(result.getDimension().name()));
                writer.write(',');
                writer.write(String.valueOf(result.getRegionX()));
                writer.write(',');
                writer.write(String.valueOf(result.getRegionZ()));
                writer.write(',');
                writer.write(String.valueOf(result.getX()));
                writer.write(',');
                writer.write(String.valueOf(result.getZ()));
                writer.write(',');
                writer.write(String.valueOf(result.getX()));
                writer.write(',');
                writer.write(String.valueOf(result.getY()));
                writer.write(',');
                writer.write(String.valueOf(result.getZ()));
                writer.write(',');
                writer.write(escape("scanned from region file"));
                writer.newLine();
            }
            writer.flush();
            System.out.println("CSV export complete: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export CSV file", e);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"").replace("\n", "\\n");
    }
}
