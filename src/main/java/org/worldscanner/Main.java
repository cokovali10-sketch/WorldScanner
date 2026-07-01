package org.worldscanner;

import org.worldscanner.cli.CommandLine;
import org.worldscanner.core.ScannerApplication;
import org.worldscanner.ui.AdvancedMenu;
import org.worldscanner.ui.GuiLauncher;
import org.worldscanner.ui.InteractiveLauncher;
import org.worldscanner.ui.QuickActions;

public class Main {

    public static void main(String[] args) {
        Console.info("WorldScanner 1.0 - Minecraft Anvil world scanner");

        if (args.length == 0) {
            QuickActions.printQuickActions();
            Console.info("Starting interactive launcher...");
            InteractiveLauncher.start();
            return;
        }

        if (args.length == 1 && ("--interactive".equals(args[0]) || "interactive".equals(args[0]) || "-i".equals(args[0]))) {
            InteractiveLauncher.start();
            return;
        }

        if (args.length == 1 && ("--menu".equals(args[0]) || "menu".equals(args[0]))) {
            AdvancedMenu.show();
            return;
        }

        if (args.length == 1 && ("--gui".equals(args[0]) || "gui".equals(args[0]))) {
            GuiLauncher.show();
            return;
        }

        CommandLine command = CommandLine.parse(args);
        if (command == null) {
            CommandLine.printUsage();
            return;
        }

        ScannerApplication application = new ScannerApplication(command);
        application.execute();
    }

}