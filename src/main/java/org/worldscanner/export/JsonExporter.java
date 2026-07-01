package org.worldscanner.export;

import org.worldscanner.model.SearchResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Экспорт результатов поиска в JSON формат.
 */
public final class JsonExporter {

    private JsonExporter() {
    }

    public static void export(List<SearchResult> results, String outputPath) {
        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException("Output path is required for JSON export.");
        }

        File outputFile = new File(outputPath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("[");
            writer.newLine();
            for (int i = 0; i < results.size(); i++) {
                SearchResult result = results.get(i);
                writer.write("  {");
                writer.newLine();
                writer.write("    \"type\": \"" + escape(result.getBlockId() != null ? "block" : result.getItemId() != null ? "item" : "entity") + "\",");
                writer.newLine();
                writer.write("    \"identifier\": \"" + escape(result.getBlockId() != null ? result.getBlockId() : result.getItemId() != null ? result.getItemId() : result.getEntityId()) + "\",");
                writer.newLine();
                writer.write("    \"dimension\": \"" + escape(result.getDimension().name()) + "\",");
                writer.newLine();
                writer.write("    \"regionX\": " + result.getRegionX() + ",");
                writer.newLine();
                writer.write("    \"regionZ\": " + result.getRegionZ() + ",");
                writer.newLine();
                writer.write("    \"chunkX\": " + result.getX() + ",");
                writer.newLine();
                writer.write("    \"chunkZ\": " + result.getZ() + ",");
                writer.newLine();
                writer.write("    \"blockX\": " + result.getX() + ",");
                writer.newLine();
                writer.write("    \"blockY\": " + result.getY() + ",");
                writer.newLine();
                writer.write("    \"blockZ\": " + result.getZ() + ",");
                writer.newLine();
                writer.write("    \"additionalInfo\": \"scanned from region file\"");
                writer.newLine();
                writer.write("  }");
                if (i < results.size() - 1) {
                    writer.write(",");
                }
                writer.newLine();
            }
            writer.write("]");
            writer.newLine();
            writer.flush();
            System.out.println("JSON export complete: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export JSON file", e);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
