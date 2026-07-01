package org.worldscanner.export;

import org.worldscanner.model.SearchResult;

import java.util.List;

public final class TableFormatter {

    private TableFormatter() {
    }

    public static void printTable(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            System.out.println("No results.");
            return;
        }

        String header = String.format("%-12s %-20s %-10s %-8s %-8s %-8s", "TYPE", "IDENTIFIER", "DIMENSION", "REGIONX", "REGIONZ", "Y");
        System.out.println(header);
        System.out.println("-".repeat(header.length()));
        for (SearchResult result : results) {
            String type = result.getBlockId() != null ? "block" : result.getItemId() != null ? "item" : "entity";
            String identifier = result.getBlockId() != null ? result.getBlockId() : result.getItemId() != null ? result.getItemId() : result.getEntityId();
            System.out.printf("%-12s %-20s %-10s %-8d %-8d %-8d%n",
                    type,
                    identifier,
                    result.getDimension().name(),
                    result.getRegionX(),
                    result.getRegionZ(),
                    result.getY());
        }
    }
}
