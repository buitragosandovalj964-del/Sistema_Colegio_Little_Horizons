package com.littlehorizons.erp.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Reads and validates console input for every view. A single instance is shared
 * so that all views use the same keyboard reader. Typing the cancel word in any
 * form field throws CancelledException, which controllers catch to go back to the menu.
 */
public class ConsoleInput {

    public static final String CANCEL_WORD = "cancelar";

    private static final String NAME_PATTERN = "\\p{L}+([ '\\-]\\p{L}+)*";
    private static final String NAME_ERROR =
            "Solo se permiten letras, espacios, guiones y apóstrofes (sin números).";
    private static final String DATE_ERROR =
            "Fecha inválida. Use el formato aaaa-mm-dd, por ejemplo 2014-05-12.";

    private final Scanner scanner = new Scanner(System.in);

    /**
     * Thrown when the user types the cancel word in the middle of a form.
     */
    public static class CancelledException extends RuntimeException {

        public CancelledException() {
            super("Operation cancelled by the user.");
        }
    }

    public void showError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public int readMenuOption(String prompt) {
        while (true) {
            String text = readRaw(prompt);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                showError("Por favor ingrese un número válido.");
            }
        }
    }

    public String readRequiredText(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isEmpty()) {
                return text;
            }
            showError("Este campo es obligatorio.");
        }
    }

    public String readRequiredText(String prompt, int maxLength) {
        while (true) {
            String text = readRequiredText(prompt);
            if (text.length() <= maxLength) {
                return text;
            }
            showError("Máximo " + maxLength + " caracteres (ingresó " + text.length() + ").");
        }
    }

    public String readOptionalText(String prompt) {
        String text = readLine(prompt + " (opcional): ");
        return text.isEmpty() ? null : text;
    }

    public String readTextKeepingCurrent(String label, String current) {
        String text = readLine(label + " [" + orEmpty(current) + "]: ");
        return text.isEmpty() ? current : text;
    }

    public String readRequiredName(String prompt) {
        while (true) {
            String text = readRequiredText(prompt);
            if (text.matches(NAME_PATTERN)) {
                return text;
            }
            showError(NAME_ERROR);
        }
    }

    public String readOptionalName(String prompt) {
        while (true) {
            String text = readLine(prompt + " (opcional): ");
            if (text.isEmpty()) {
                return null;
            }
            if (text.matches(NAME_PATTERN)) {
                return text;
            }
            showError(NAME_ERROR);
        }
    }

    public String readNameKeepingCurrent(String label, String current) {
        while (true) {
            String text = readLine(label + " [" + orEmpty(current) + "]: ");
            if (text.isEmpty()) {
                return current;
            }
            if (text.matches(NAME_PATTERN)) {
                return text;
            }
            showError(NAME_ERROR);
        }
    }

    public String readOptionalPhone(String prompt, int minLength, int maxLength) {
        while (true) {
            String text = readLine(prompt + " (opcional): ");
            if (text.isEmpty()) {
                return null;
            }
            if (isValidPhone(text, minLength, maxLength)) {
                return text;
            }
            showError(phoneErrorMessage(minLength, maxLength));
        }
    }

    public String readPhoneKeepingCurrent(String label, String current,
            int minLength, int maxLength) {
        while (true) {
            String text = readLine(label + " [" + orEmpty(current) + "]: ");
            if (text.isEmpty()) {
                return current;
            }
            if (isValidPhone(text, minLength, maxLength)) {
                return text;
            }
            showError(phoneErrorMessage(minLength, maxLength));
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            String text = readLine(prompt + " (aaaa-mm-dd): ");
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                showError(DATE_ERROR);
            }
        }
    }

    public LocalDate readDateKeepingCurrent(String label, LocalDate current) {
        while (true) {
            String text = readLine(label + " [" + current + "] (aaaa-mm-dd): ");
            if (text.isEmpty()) {
                return current;
            }
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                showError(DATE_ERROR);
            }
        }
    }

    public String readChoice(String label, List<String> options) {
        while (true) {
            String text = readLine(label + " (" + String.join(", ", options) + "): ");
            Optional<String> match = findOption(text, options);
            if (match.isPresent()) {
                return match.get();
            }
            showError("Opción no válida. Elija una de: " + String.join(", ", options) + ".");
        }
    }

    public String readChoiceKeepingCurrent(String label, String current, List<String> options) {
        while (true) {
            String text = readLine(label + " [" + current + "] ("
                    + String.join(", ", options) + "): ");
            if (text.isEmpty()) {
                return current;
            }
            Optional<String> match = findOption(text, options);
            if (match.isPresent()) {
                return match.get();
            }
            showError("Opción no válida. Elija una de: " + String.join(", ", options) + ".");
        }
    }

    public boolean confirm(String question) {
        while (true) {
            String text = readRaw(question + " (si/no): ").toLowerCase();
            if (text.equals("si") || text.equals("sí")) {
                return true;
            }
            if (text.equals("no")) {
                return false;
            }
            showError("Responda si o no.");
        }
    }

    private String readLine(String prompt) {
        String text = readRaw(prompt);
        if (text.equalsIgnoreCase(CANCEL_WORD)) {
            throw new CancelledException();
        }
        return text;
    }

    private String readRaw(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private Optional<String> findOption(String text, List<String> options) {
        return options.stream()
                .filter(option -> option.equalsIgnoreCase(text))
                .findFirst();
    }

    private boolean isValidPhone(String text, int minLength, int maxLength) {
        return text.matches("\\d+") && text.length() >= minLength && text.length() <= maxLength;
    }

    private String phoneErrorMessage(int minLength, int maxLength) {
        return "El teléfono debe tener solo números, entre " + minLength
                + " y " + maxLength + " dígitos.";
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}   