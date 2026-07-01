package org.worldscanner.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Простая CLI-обработчик аргументов командной строки.
 */
public final class CommandLine {

    public enum CommandType {
        FIND_BLOCK,
        FIND_ITEM,
        FIND_ENTITY,
        MULTI_SEARCH,
        EXPORT_JSON,
        EXPORT_CSV,
        EXPORT_TXT,
        SCAN,
        HELP
    }

    private final CommandType commandType;
    private final String worldPath;
    private final String target;
    private final String filter;
    private final List<String> extraOptions;

    private CommandLine(CommandType commandType, String worldPath, String target, String filter, List<String> extraOptions) {
        this.commandType = commandType;
        this.worldPath = worldPath;
        this.target = target;
        this.filter = filter;
        this.extraOptions = extraOptions;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getWorldPath() {
        return worldPath;
    }

    public String getTarget() {
        return target;
    }

    public String getFilter() {
        return filter;
    }

    public List<String> getExtraOptions() {
        return extraOptions;
    }

    public static CommandLine parse(String[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        if (first.equals("help") || first.equals("--help") || first.equals("-h")) {
            return new CommandLine(CommandType.HELP, null, null, null, List.of());
        }

        if (args.length < 2) {
            return null;
        }

        String worldPath = args[0];
        String command = args[1].toLowerCase(Locale.ROOT);
        List<String> extraOptions = new ArrayList<>();

        if (command.equals("find")) {
            if (args.length < 4) {
                return null;
            }
            String subcommand = args[2].toLowerCase(Locale.ROOT);
            String target = args[3];
            for (int i = 4; i < args.length; i++) {
                extraOptions.add(args[i]);
            }
            return switch (subcommand) {
                case "block" -> new CommandLine(CommandType.FIND_BLOCK, worldPath, target, null, extraOptions);
                case "item" -> new CommandLine(CommandType.FIND_ITEM, worldPath, target, null, extraOptions);
                case "entity" -> new CommandLine(CommandType.FIND_ENTITY, worldPath, target, null, extraOptions);
                case "multi" -> new CommandLine(CommandType.MULTI_SEARCH, worldPath, target, null, extraOptions);
                default -> null;
            };
        }

        if (command.equals("scan")) {
            for (int i = 2; i < args.length; i++) {
                extraOptions.add(args[i]);
            }
            return new CommandLine(CommandType.SCAN, worldPath, null, null, extraOptions);
        }

        if (command.equals("export")) {
            if (args.length < 3) {
                return null;
            }
            String format = args[2].toLowerCase(Locale.ROOT);
            CommandType exportType = switch (format) {
                case "json" -> CommandType.EXPORT_JSON;
                case "csv" -> CommandType.EXPORT_CSV;
                case "txt" -> CommandType.EXPORT_TXT;
                default -> null;
            };

            if (exportType == null) {
                return null;
            }
            String outputPath = args.length > 3 ? args[3] : null;
            for (int i = 4; i < args.length; i++) {
                extraOptions.add(args[i]);
            }
            return new CommandLine(exportType, worldPath, outputPath, null, extraOptions);
        }

        return null;
    }

    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar WorldScanner.jar <world_path> find block <minecraft:block_name> [--dimension=overworld|nether|end] [--region=rx,rz] [--coords=x,y,z] [--radius=N] [--limit=N] [--summary]");
        System.out.println("  java -jar WorldScanner.jar <world_path> find item <minecraft:item_name> [--dimension=overworld|nether|end] [--region=rx,rz] [--coords=x,y,z] [--radius=N] [--limit=N] [--summary]");
        System.out.println("  java -jar WorldScanner.jar <world_path> find entity <minecraft:entity_type> [--dimension=overworld|nether|end] [--region=rx,rz] [--coords=x,y,z] [--radius=N] [--limit=N] [--summary]");
        System.out.println("  java -jar WorldScanner.jar <world_path> find multi <kind:block|item|entity> <target1,target2,...> [--limit=N] [--summary]");
        System.out.println("  java -jar WorldScanner.jar <world_path> scan [--dimension=overworld|nether|end] [--summary]");
        System.out.println("  java -jar WorldScanner.jar <world_path> export <json|csv|txt> <output_path> [--dimension=overworld|nether|end] [--summary]");
        System.out.println("  java -jar WorldScanner.jar help");
    }
}
