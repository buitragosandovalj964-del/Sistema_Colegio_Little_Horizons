package com.littlehorizons.erp.util;

/**
 * Helpers to print aligned tables in the console.
 */
public final class ConsoleFormat {

    private ConsoleFormat() {
        // Utility class: it should not be instantiated.
    }

    /**
     * Returns the text cut with "..." when it is longer than the column width,
     * or "-" when there is no text. Use it with a "%-Ns" format of the same width.
     */
    public static String fit(String text, int width) {
        if (text == null || text.isBlank()) {
            return "-";
        }
        if (text.length() <= width) {
            return text;
        }
        return text.substring(0, Math.max(1, width - 3)) + "...";
    }
}