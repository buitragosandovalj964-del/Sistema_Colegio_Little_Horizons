package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.AssignmentService;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Console screens for teacher assignments and class schedules.
 * Everything shown to the user is in Spanish.
 */
public class AssignmentView {

    /**
     * Identifies a teacher by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private record ScheduleRow(ScheduleSlot slot, Assignment assignment) { }

    private static final String TIME_PATTERN = "([01]\\d|2[0-3]):[0-5]\\d";
    private static final String POSITIVE_NUMBER_PATTERN = "[1-9]\\d{0,8}";
    private static final String SEPARATOR = "-".repeat(78);

    private final ConsoleInput input;

    public AssignmentView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("  ASIGNACIONES Y HORARIOS");
        System.out.println("==============================");
        System.out.println("1. Asignar materia a un profesor");
        System.out.println("2. Ver asignaciones de un profesor");
        System.out.println("3. Quitar una asignación");
        System.out.println("4. Agregar franja horaria a una asignación");
        System.out.println("5. Quitar una franja horaria");
        System.out.println("6. Ver horario semanal de un profesor");
        System.out.println("0. Volver");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public DocumentId askTeacherDocument() {
        showMessage("Escriba '" + ConsoleInput.CANCEL_WORD
                + "' en cualquier momento para volver al menú.");
        String type = input.readChoice("Documento del profesor - tipo",
                TeacherService.DOCUMENT_TYPES);
        String number = input.readRequiredText("Documento del profesor - número: ",
                FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public String askSubjectCode() {
        return input.readRequiredPattern("Código de la materia (ej: MAT-5): ",
                SubjectService.CODE_PATTERN,
                "Código no válido. Use el formato LETRAS-NÚMERO, por ejemplo MAT-5.")
                .toUpperCase();
    }

    public int askAcademicYear() {
        return input.readIntInRange("Año académico (" + AssignmentService.minAcademicYear()
                + " a " + AssignmentService.maxAcademicYear() + "): ",
                AssignmentService.minAcademicYear(), AssignmentService.maxAcademicYear());
    }

    public String askSection() {
        return input.readRequiredPattern("Sección o grupo (una letra: A, B, C...): ",
                AssignmentService.SECTION_PATTERN, "La sección debe ser una sola letra.");
    }

    public int askAssignmentId() {
        String text = input.readRequiredPattern("Número de la asignación (columna ID): ",
                POSITIVE_NUMBER_PATTERN,
                "Ingrese solo el número de la asignación, por ejemplo 3.");
        return Integer.parseInt(text);
    }

    public int askSlotId() {
        String text = input.readRequiredPattern(
                "Número de la franja (el que aparece como 'Franja N'): ",
                POSITIVE_NUMBER_PATTERN,
                "Ingrese solo el número de la franja, por ejemplo 4.");
        return Integer.parseInt(text);
    }

    public String askDay() {
        return input.readChoice("Día", AssignmentService.DAYS);
    }

    public LocalTime askTime(String label) {
        String text = input.readRequiredPattern(label + " (formato HH:mm, ej: 08:00): ",
                TIME_PATTERN, "Hora no válida. Use el formato HH:mm de 24 horas, por ejemplo 08:00.");
        return LocalTime.parse(text);
    }

    public boolean confirm(String question) {
        return input.confirm(question);
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showError(String message) {
        input.showError(message);
    }

    public void showAssignments(Teacher teacher, List<Assignment> assignments) {
        System.out.println(SEPARATOR);
        System.out.println("Profesor: " + teacher.getFullName() + " | "
                + teacher.getDocumentType() + " " + teacher.getDocumentNumber()
                + " | Estado: " + teacher.getStatus());
        System.out.println("Total de asignaciones: " + assignments.size());
        System.out.println(SEPARATOR);
        if (assignments.isEmpty()) {
            showMessage("Este profesor todavía no tiene materias asignadas.");
            return;
        }
        System.out.printf("%-5s %-6s %-10s %-24s %-12s %-5s %-16s%n",
                "ID", "AÑO", "CÓDIGO", "MATERIA", "GRADO", "SEC.", "HORAS PROG.");
        for (Assignment assignment : assignments) {
            Subject subject = assignment.getSubject();
            System.out.printf("%-5d %-6d %-10s %-24s %-12s %-5s %-16s%n",
                    assignment.getId(), assignment.getAcademicYear(), subject.getCode(),
                    subject.getName(), subject.getGrade().getName(), assignment.getSection(),
                    formatDuration(assignment.getScheduledMinutes()) + " de "
                            + formatDuration(subject.getWeeklyHours() * 60L));
            if (assignment.getSlots().isEmpty()) {
                System.out.println("      (sin franjas horarias todavía)");
            }
            assignment.getSlots().forEach(slot -> System.out.printf(
                    "      Franja %d: %-10s %s - %s (%s)%n",
                    slot.getId(), slot.getDay(), slot.getStartTime(), slot.getEndTime(),
                    formatDuration(slot.getDurationMinutes())));
        }
        System.out.println(SEPARATOR);
    }

    public void showWeeklySchedule(Teacher teacher, int academicYear,
            List<Assignment> assignments) {
        System.out.println(SEPARATOR);
        System.out.println("Horario semanal de " + teacher.getFullName()
                + " - año " + academicYear);
        System.out.println(SEPARATOR);
        List<ScheduleRow> rows = assignments.stream()
                .filter(assignment -> assignment.getAcademicYear() == academicYear)
                .flatMap(assignment -> assignment.getSlots().stream()
                        .map(slot -> new ScheduleRow(slot, assignment)))
                .sorted(Comparator
                        .comparingInt((ScheduleRow row) ->
                                AssignmentService.DAYS.indexOf(row.slot().getDay()))
                        .thenComparing(row -> row.slot().getStartTime()))
                .toList();
        if (rows.isEmpty()) {
            showMessage("No hay franjas horarias registradas para ese año.");
            return;
        }
        System.out.printf("%-10s %-13s %-9s %-24s %-12s %-5s%n",
                "DÍA", "HORARIO", "CÓDIGO", "MATERIA", "GRADO", "SEC.");
        rows.forEach(row -> System.out.printf("%-10s %-13s %-9s %-24s %-12s %-5s%n",
                row.slot().getDay(),
                row.slot().getStartTime() + "-" + row.slot().getEndTime(),
                row.assignment().getSubject().getCode(),
                row.assignment().getSubject().getName(),
                row.assignment().getSubject().getGrade().getName(),
                row.assignment().getSection()));
        long totalMinutes = rows.stream()
                .mapToLong(row -> row.slot().getDurationMinutes())
                .sum();
        System.out.println(SEPARATOR);
        System.out.println("Total semanal: " + formatDuration(totalMinutes));
    }

    /**
     * Shows minutes as hours when possible: 60 -> "1 h", 90 -> "1 h 30 min", 45 -> "45 min".
     */
    private String formatDuration(long minutes) {
        long hours = minutes / 60;
        long remainder = minutes % 60;
        if (hours == 0 && remainder == 0) {
            return "0 h";
        }
        if (remainder == 0) {
            return hours + " h";
        }
        if (hours == 0) {
            return remainder + " min";
        }
        return hours + " h " + remainder + " min";
    }
}