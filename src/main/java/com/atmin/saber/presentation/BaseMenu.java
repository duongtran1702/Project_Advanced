package com.atmin.saber.presentation;

import com.atmin.saber.util.ConsoleInput;

import java.util.Scanner;

/**
 * Base class for all menu implementations.
 * Provides common UI utilities and helper methods.
 */
public abstract class BaseMenu {
    protected final Scanner scanner;

    protected BaseMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Read a line or return null if no more input.
     */
    protected String readLineOrNull() {
        if (scanner == null || !scanner.hasNextLine()) return null;
        return scanner.nextLine();
    }

    /**
     * Print dashboard row with two columns.
     */
    protected static void printDashboardRow(String f, String r, String left, String right) {
        System.out.printf(f + "  ║ " + r + "%-24s" + f + " ║ " + r + "%-25s" + f + " ║%n" + r, left, right);
    }

    /**
     * Safe run action with exception handling.
     */
    protected void safeRun(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        pause();
    }

    /**
     * Safe run action with ConsoleInput pause (for StaffMenu, etc.).
     */
    protected void safeRunWithConsoleInputPause(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
            ConsoleInput.pressEnterToContinue(scanner);
        }
    }

    /**
     * Pause and wait for user input.
     */
    protected void pause() {
        System.out.print("\nPress Enter to continue...");
        if (scanner != null && scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }

    /**
     * Safe string length (max length).
     */
    protected static String safeLength(String s) {
        if (s == null) return "";
        return s.length() <= 8 ? s : s.substring(0, 8 - 3) + "...";
    }

    /**
     * Safe short string.
     */
    protected static String safeShort(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.length() <= 28) return trimmed;
        return trimmed.substring(0, 25) + "...";
    }
}

