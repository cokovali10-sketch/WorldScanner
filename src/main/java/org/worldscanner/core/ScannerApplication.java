package org.worldscanner.core;

import org.worldscanner.Console;
import org.worldscanner.RegionScanner;
import org.worldscanner.cli.CommandLine;
import org.worldscanner.export.CsvExporter;
import org.worldscanner.export.JsonExporter;
import org.worldscanner.export.TableFormatter;
import org.worldscanner.export.TxtExporter;
import org.worldscanner.model.SearchResult;
import org.worldscanner.search.BlockFinder;
import org.worldscanner.search.CombinedSearch;
import org.worldscanner.search.EntityFinder;
import org.worldscanner.search.ItemFinder;
import org.worldscanner.search.MultiSearch;
import org.worldscanner.search.SearchFilter;

import java.io.File;
import java.util.List;

/**
 * Основной класс приложения. Отвечает за выбор задач и подзадач команды.
 */
public class ScannerApplication {

    private final CommandLine commandLine;

    public ScannerApplication(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    public void execute() {
        if (commandLine == null) {
            Console.error("Invalid command line arguments.");
            CommandLine.printUsage();
            return;
        }

        File worldFolder = new File(commandLine.getWorldPath());
        if (!worldFolder.exists() || !worldFolder.isDirectory()) {
            Console.error("World path does not exist or is not a directory: " + commandLine.getWorldPath());
            return;
        }

        RegionScanner scanner = new RegionScanner(worldFolder);
        SearchFilter filter = SearchFilter.fromOptions(commandLine.getExtraOptions());
        List<SearchResult> results;

        switch (commandLine.getCommandType()) {
            case SCAN -> {
                results = scanner.scanWorldForResults();
                printResults(results, filter);
            }
            case FIND_BLOCK -> {
                BlockFinder finder = new BlockFinder(scanner);
                results = finder.findBlock(commandLine.getTarget(), filter);
                printResults(results, filter);
            }
            case FIND_ITEM -> {
                ItemFinder finder = new ItemFinder(scanner);
                results = finder.findItem(commandLine.getTarget(), filter);
                printResults(results, filter);
            }
            case FIND_ENTITY -> {
                EntityFinder finder = new EntityFinder(scanner);
                results = finder.findEntity(commandLine.getTarget(), filter);
                printResults(results, filter);
            }
            case MULTI_SEARCH -> {
                String targetList = commandLine.getTarget();
                if (targetList == null || targetList.isBlank()) {
                    Console.error("No targets provided for multi search.");
                    return;
                }
                String[] parts = targetList.split(";");
                List<String> blockTargets = List.of();
                List<String> itemTargets = List.of();
                List<String> entityTargets = List.of();
                if (parts.length >= 1) {
                    blockTargets = List.of(parts[0].split(","));
                }
                if (parts.length >= 2) {
                    itemTargets = List.of(parts[1].split(","));
                }
                if (parts.length >= 3) {
                    entityTargets = List.of(parts[2].split(","));
                }
                results = CombinedSearch.search(scanner, blockTargets, itemTargets, entityTargets, filter);
                printResults(results, filter);
            }
            case EXPORT_JSON -> {
                results = scanner.scanWorldForResults();
                JsonExporter.export(results, commandLine.getTarget());
                printSummary(results, filter);
            }
            case EXPORT_CSV -> {
                results = scanner.scanWorldForResults();
                CsvExporter.export(results, commandLine.getTarget());
                printSummary(results, filter);
            }
            case EXPORT_TXT -> {
                results = scanner.scanWorldForResults();
                TxtExporter.export(results, commandLine.getTarget());
                printSummary(results, filter);
            }
            default -> {
                Console.error("Unsupported command.");
                CommandLine.printUsage();
            }
        }
    }

    private void printResults(List<SearchResult> results, SearchFilter filter) {
        if (results == null || results.isEmpty()) {
            Console.info("No search results found.");
            return;
        }
        List<SearchResult> limitedResults = results.stream().limit(filter.getLimit()).toList();
        if (filter.isShowSummary()) {
            TableFormatter.printTable(limitedResults);
            printSummary(limitedResults, filter);
        } else {
            limitedResults.forEach(System.out::println);
        }
    }

    private void printSummary(List<SearchResult> results, SearchFilter filter) {
        if (results == null || results.isEmpty()) {
            Console.info("Summary: 0 results");
            return;
        }
        Console.info("Summary: " + Math.min(results.size(), filter.getLimit()) + " result(s) in the current selection");
    }
}
