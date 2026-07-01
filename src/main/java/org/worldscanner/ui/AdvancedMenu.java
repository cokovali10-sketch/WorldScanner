package org.worldscanner.ui;

import org.worldscanner.Console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class AdvancedMenu {

    private AdvancedMenu() {
    }

    public static void show() {
        Console.info("WorldScanner Advanced Menu");
        Console.info("1. Quick scan");
        Console.info("2. Search blocks");
        Console.info("3. Search items");
        Console.info("4. Search entities");
        Console.info("5. Export all results");
        Console.info("0. Exit");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Choose action: ");
            String choice = reader.readLine();
            if (choice == null) {
                return;
            }
            switch (choice.trim()) {
                case "1" -> Console.info("Use: gradlew.bat run --args='C:/world scan --summary'");
                case "2" -> Console.info("Use: gradlew.bat run --args='C:/world find block minecraft:diamond_ore --limit=10 --summary'");
                case "3" -> Console.info("Use: gradlew.bat run --args='C:/world find item minecraft:diamond --limit=10 --summary'");
                case "4" -> Console.info("Use: gradlew.bat run --args='C:/world find entity minecraft:zombie --limit=10 --summary'");
                case "5" -> Console.info("Use: gradlew.bat run --args='C:/world export json C:/tmp/results.json --summary'");
                default -> Console.warn("No action selected.");
            }
        } catch (IOException e) {
            Console.error("Menu input error: " + e.getMessage());
        }
    }
}
