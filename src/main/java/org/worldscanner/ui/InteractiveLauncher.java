package org.worldscanner.ui;

import org.worldscanner.Console;
import org.worldscanner.cli.CommandLine;
import org.worldscanner.core.ScannerApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class InteractiveLauncher {

    private InteractiveLauncher() {
    }

    public static void start() {
        Console.info("Interactive launcher started. Choose a preset command.");
        Console.info("1. Scan world");
        Console.info("2. Find block");
        Console.info("3. Find item");
        Console.info("4. Find entity");
        Console.info("5. Export JSON");
        Console.info("6. Export CSV");
        Console.info("7. Export TXT");
        Console.info("8. Show help");
        Console.info("9. Show recent commands");
        Console.info("0. Exit");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("Select action: ");
                String choice = reader.readLine();
                if (choice == null) {
                    break;
                }
                switch (choice.trim()) {
                    case "1" -> runPreset(reader, "scan");
                    case "2" -> runPreset(reader, "find-block");
                    case "3" -> runPreset(reader, "find-item");
                    case "4" -> runPreset(reader, "find-entity");
                    case "5" -> runPreset(reader, "export-json");
                    case "6" -> runPreset(reader, "export-csv");
                    case "7" -> runPreset(reader, "export-txt");
                    case "8" -> CommandLine.printUsage();
                    case "9" -> PresetHistory.printHistory();
                    case "0" -> {
                        Console.info("Goodbye.");
                        return;
                    }
                    default -> Console.warn("Unknown action. Try again.");
                }
            }
        } catch (IOException e) {
            Console.error("Input error: " + e.getMessage());
        }
    }

    private static void runPreset(BufferedReader reader, String preset) {
        try {
            System.out.print("Enter world folder: ");
            String worldPath = reader.readLine();
            if (worldPath == null || worldPath.isBlank()) {
                Console.warn("World path is empty.");
                return;
            }

            List<String> args = new ArrayList<>();
            args.add(worldPath);
            switch (preset) {
                case "scan" -> args.add("scan");
                case "find-block" -> {
                    args.add("find");
                    args.add("block");
                    System.out.print("Enter block id: ");
                    String target = reader.readLine();
                    if (target == null || target.isBlank()) {
                        Console.warn("Block id is empty.");
                        return;
                    }
                    args.add(target);
                }
                case "find-item" -> {
                    args.add("find");
                    args.add("item");
                    System.out.print("Enter item id: ");
                    String target = reader.readLine();
                    if (target == null || target.isBlank()) {
                        Console.warn("Item id is empty.");
                        return;
                    }
                    args.add(target);
                }
                case "find-entity" -> {
                    args.add("find");
                    args.add("entity");
                    System.out.print("Enter entity id: ");
                    String target = reader.readLine();
                    if (target == null || target.isBlank()) {
                        Console.warn("Entity id is empty.");
                        return;
                    }
                    args.add(target);
                }
                case "export-json" -> {
                    args.add("export");
                    args.add("json");
                    System.out.print("Enter output file: ");
                    String output = reader.readLine();
                    if (output == null || output.isBlank()) {
                        Console.warn("Output file is empty.");
                        return;
                    }
                    args.add(output);
                }
                case "export-csv" -> {
                    args.add("export");
                    args.add("csv");
                    System.out.print("Enter output file: ");
                    String output = reader.readLine();
                    if (output == null || output.isBlank()) {
                        Console.warn("Output file is empty.");
                        return;
                    }
                    args.add(output);
                }
                case "export-txt" -> {
                    args.add("export");
                    args.add("txt");
                    System.out.print("Enter output file: ");
                    String output = reader.readLine();
                    if (output == null || output.isBlank()) {
                        Console.warn("Output file is empty.");
                        return;
                    }
                    args.add(output);
                }
            }

            String commandText = String.join(" ", args);
            PresetHistory.save(commandText);

            CommandLine command = CommandLine.parse(args.toArray(String[]::new));
            if (command == null) {
                Console.error("Could not build command from your input.");
                return;
            }

            new ScannerApplication(command).execute();
        } catch (IOException e) {
            Console.error("Input error: " + e.getMessage());
        }
    }
}
