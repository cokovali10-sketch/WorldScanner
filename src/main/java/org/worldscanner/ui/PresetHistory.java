package org.worldscanner.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PresetHistory {
    private static final Path HISTORY_FILE = Path.of("worldscanner-history.txt");

    private PresetHistory() {
    }

    public static void save(String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        try {
            List<String> lines = Files.exists(HISTORY_FILE) ? Files.readAllLines(HISTORY_FILE) : new ArrayList<>();
            if (lines.contains(command)) {
                lines.remove(command);
            }
            lines.add(0, command);
            while (lines.size() > 10) {
                lines.remove(lines.size() - 1);
            }
            Files.write(HISTORY_FILE, lines);
        } catch (IOException ignored) {
        }
    }

    public static void printHistory() {
        if (!Files.exists(HISTORY_FILE)) {
            System.out.println("No recent commands yet.");
            return;
        }
        try {
            List<String> lines = Files.readAllLines(HISTORY_FILE);
            System.out.println("Recent commands:");
            for (int i = 0; i < lines.size(); i++) {
                System.out.println((i + 1) + ". " + lines.get(i));
            }
        } catch (IOException e) {
            System.out.println("Unable to read history.");
        }
    }
}
