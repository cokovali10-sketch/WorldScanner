package org.worldscanner.export;

import org.worldscanner.model.SearchResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Экспорт search results в TXT формат.
 */
public final class TxtExporter {

    private TxtExporter() {
    }

    public static void export(List<SearchResult> results, String outputPath) {
        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException("Output path is required for TXT export.");
        }

        File outputFile = new File(outputPath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (SearchResult result : results) {
                writer.write(result.toString());
                writer.newLine();
            }
            writer.flush();
            System.out.println("TXT export complete: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export TXT file", e);
        }
    }
}
