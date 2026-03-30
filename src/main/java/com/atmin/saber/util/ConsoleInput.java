package com.atmin.saber.util;

import java.util.Scanner;

public final class ConsoleInput {
    private ConsoleInput() {
    }

    /**
     * Signals interactive input ended (EOF / closed stream) so menus can exit gracefully.
     */
    public static final class EndOfInputException extends RuntimeException {
        public EndOfInputException() {
            super("End of input");
        }
    }

    /**
     * Read a line (trimmed). If input stream ended, throws {@link EndOfInputException}.
     */
    public static String readLineOrThrow(Scanner sc) {
        if (sc == null) throw new IllegalArgumentException("Scanner must not be null");
        if (!sc.hasNextLine()) throw new EndOfInputException();
        return sc.nextLine();
    }

    public static String readNonEmpty(Scanner sc, String prompt, String emptyMessage) {
        while (true) {
            System.out.print(prompt);
            String value = readLineOrThrow(sc).trim();
            if (!value.isEmpty()) return value;
            System.out.println(emptyMessage);
        }
    }

    public static int readInt(Scanner sc, String prompt, String invalidMessage) {
        while (true) {
            System.out.print(prompt);
            String value = readLineOrThrow(sc).trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                System.out.println(invalidMessage);
            }
        }
    }

    public static void pressEnterToContinue(Scanner sc) {
        System.out.print("\nPress Enter to continue...");
        readLineOrThrow(sc);
    }
}

