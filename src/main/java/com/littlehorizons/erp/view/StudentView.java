package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Student;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console screens for student management. It only reads input and prints output.
 * Everything shown to the user is in Spanish.
 */
public class StudentView {

    private static final String DATE_ERROR =
            "Fecha inválida. Use el formato aaaa-mm-dd, por ejemplo 2014-05-12.";
    private static final String NAME_PATTERN = "\\p{L}+([ '\\-]\\p{L}+)*";
    private static final String NAME_ERROR =
            "Solo se permiten letras, espacios, guiones y apóstrofes (sin números).";

    private final Scanner scanner = new Scanner(System.in);

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("   GESTIÓN DE ESTUDIANTES");
        System.out.println("==============================");
        System.out.println("1. Registrar estudiante");
        System.out.println("2. Listar estudiantes");
        System.out.println("3. Buscar estudiante por documento");
        System.out.println("4. Actualizar estudiante");
        System.out.println("5. Retirar estudiante");
        System.out.println("6. Eliminar estudiante (registros creados por error)");
        System.out.println("0. Salir");
        return readInt("Seleccione una opción: ");
    }

    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                showError("Por favor ingrese un número válido.");
            }
        }
    }

    public String readRequiredText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            showError("Este campo es obligatorio.");
        }
    }

    public String readRequiredText(String prompt, int maxLength) {
        while (true) {
            String input = readRequiredText(prompt);
            if (input.length() <= maxLength) {
                return input;
            }
            showError("Máximo " + maxLength + " caracteres (ingresó " + input.length() + ").");
        }
    }

    public String readOptionalText(String prompt) {
        System.out.print(prompt + " (opcional): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? null : input;
    }

    public String readTextKeepingCurrent(String label, String current) {
        System.out.print(label + " [" + (current == null ? "" : current) + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? current : input;
    }

    public String readRequiredName(String prompt) {
        while (true) {
            String input = readRequiredText(prompt);
            if (input.matches(NAME_PATTERN)) {
                return input;
            }
            showError(NAME_ERROR);
        }
    }

    public String readOptionalName(String prompt) {
        while (true) {
            System.out.print(prompt + " (opcional): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            if (input.matches(NAME_PATTERN)) {
                return input;
            }
            showError(NAME_ERROR);
        }
    }

    public String readNameKeepingCurrent(String label, String current) {
        while (true) {
            System.out.print(label + " [" + (current == null ? "" : current) + "]: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return current;
            }
            if (input.matches(NAME_PATTERN)) {
                return input;
            }
            showError(NAME_ERROR);
        }
    }

    public String readOptionalPhone(String prompt, int minLength, int maxLength) {
        while (true) {
            System.out.print(prompt + " (opcional): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            if (isValidPhone(input, minLength, maxLength)) {
                return input;
            }
            showError(phoneErrorMessage(minLength, maxLength));
        }
    }

    public String readPhoneKeepingCurrent(String label, String current,
            int minLength, int maxLength) {
        while (true) {
            System.out.print(label + " [" + (current == null ? "" : current) + "]: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return current;
            }
            if (isValidPhone(input, minLength, maxLength)) {
                return input;
            }
            showError(phoneErrorMessage(minLength, maxLength));
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (aaaa-mm-dd): ");
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                showError(DATE_ERROR);
            }
        }
    }

    public LocalDate readDateKeepingCurrent(String label, LocalDate current) {
        while (true) {
            System.out.print(label + " [" + current + "] (aaaa-mm-dd): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return current;
            }
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                showError(DATE_ERROR);
            }
        }
    }

    public String readChoice(String label, List<String> options) {
        while (true) {
            System.out.print(label + " (" + String.join(", ", options) + "): ");
            String input = scanner.nextLine().trim();
            Optional<String> match = findOption(input, options);
            if (match.isPresent()) {
                return match.get();
            }
            showError("Opción no válida. Elija una de: " + String.join(", ", options) + ".");
        }
    }

    public String readChoiceKeepingCurrent(String label, String current, List<String> options) {
        while (true) {
            System.out.print(label + " [" + current + "] ("
                    + String.join(", ", options) + "): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return current;
            }
            Optional<String> match = findOption(input, options);
            if (match.isPresent()) {
                return match.get();
            }
            showError("Opción no válida. Elija una de: " + String.join(", ", options) + ".");
        }
    }

    public boolean confirm(String question) {
        while (true) {
            System.out.print(question + " (si/no): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("si") || input.equals("sí")) {
                return true;
            }
            if (input.equals("no")) {
                return false;
            }
            showError("Responda si o no.");
        }
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public void showStudentList(List<Student> students) {
        if (students.isEmpty()) {
            showMessage("No hay estudiantes registrados.");
            return;
        }
        System.out.printf("%-5s %-10s %-14s %-38s %-10s%n",
                "ID", "TIPO", "DOCUMENTO", "NOMBRE", "ESTADO");
        students.forEach(student -> System.out.printf("%-5d %-10s %-14s %-38s %-10s%n",
                student.getId(), student.getDocumentType(), student.getDocumentNumber(),
                student.getFullName(), student.getStatus()));
    }

    public void showStudentDetail(Student student) {
        System.out.println("------------------------------");
        printField("Id", String.valueOf(student.getId()));
        printField("Nombre", student.getFullName());
        printField("Documento", student.getDocumentType() + " " + student.getDocumentNumber());
        printField("Fecha de nacimiento", String.valueOf(student.getBirthDate()));
        printField("Género", student.getGender());
        printField("Dirección", student.getAddress());
        printField("Teléfono", student.getPhone());
        printField("Correo electrónico", student.getEmail());
        printField("Tipo de sangre", student.getBloodType());
        printField("Alergias", student.getAllergies());
        printField("Condiciones médicas", student.getMedicalConditions());
        printField("Fecha de ingreso", String.valueOf(student.getAdmissionDate()));
        printField("Estado", student.getStatus());
        System.out.println("------------------------------");
    }

    private Optional<String> findOption(String input, List<String> options) {
        return options.stream()
                .filter(option -> option.equalsIgnoreCase(input))
                .findFirst();
    }

    private boolean isValidPhone(String input, int minLength, int maxLength) {
        return input.matches("\\d+")
                && input.length() >= minLength
                && input.length() <= maxLength;
    }

    private String phoneErrorMessage(int minLength, int maxLength) {
        return "El teléfono debe tener solo números, entre " + minLength
                + " y " + maxLength + " dígitos.";
    }

    private void printField(String label, String value) {
        System.out.printf("%-22s%s%n", label + ":", value == null ? "-" : value);
    }
}