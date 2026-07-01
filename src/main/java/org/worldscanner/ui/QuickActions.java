package org.worldscanner.ui;

import org.worldscanner.Console;

import java.util.List;

public final class QuickActions {

    private QuickActions() {
    }

    public static void printQuickActions() {
        Console.info("Quick presets:");
        System.out.println("  1) scan <world> --summary");
        System.out.println("  2) find block <world> minecraft:diamond_ore --limit=10 --summary");
        System.out.println("  3) find item <world> minecraft:diamond --limit=10 --summary");
        System.out.println("  4) find entity <world> minecraft:zombie --limit=10 --summary");
        System.out.println("  5) export json <world> output.json --summary");
    }
}
