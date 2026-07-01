package org.worldscanner.ui;

import javax.swing.JOptionPane;

public final class GuiLauncher {

    private GuiLauncher() {
    }

    public static void show() {
        String worldPath = JOptionPane.showInputDialog(null, "Enter world folder path:");
        if (worldPath == null || worldPath.isBlank()) {
            return;
        }
        String action = (String) JOptionPane.showInputDialog(
                null,
                "Choose action",
                "WorldScanner",
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Scan", "Find block", "Find item", "Find entity", "Export JSON"},
                "Scan");
        if (action == null) {
            return;
        }
        JOptionPane.showMessageDialog(null, "Prepared action: " + action + " for " + worldPath);
    }
}
