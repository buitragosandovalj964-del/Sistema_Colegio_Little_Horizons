package com.littlehorizons.erp.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes application errors to error_log.txt with a timestamp.
 */
public final class ErrorLogger {

    private static final String LOG_FILE = "error_log.txt";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ErrorLogger() {
        // Utility class: it should not be instantiated.
    }

    public static void log(String context, Throwable error) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(LocalDateTime.now().format(TIMESTAMP_FORMAT)
                    + " [ERROR] " + context);
            error.printStackTrace(writer);
            writer.println();
        } catch (IOException e) {
            System.err.println("Could not write to " + LOG_FILE + ": " + e.getMessage());
        }
    }
}