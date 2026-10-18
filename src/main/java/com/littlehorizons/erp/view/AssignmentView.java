package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.ScheduleSlot;
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
        return input.readIntInRange("Número de la asignación (el que aparece entre corchetes): ",
                1, Integer.MAX_VALUE);
    }

    public int askSlotId() {
        return input.readIntInRange("Número de la franja (el que aparece entre corchetes): ",
                1, Integer.MAX_VALUE);
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
        System.out.println("Asignaciones de " + teacher.getFullName() + ":");
        if (assignments.isEmpty()) {
            showMessage("  Este profesor todavía no tiene materias asignadas.");
            return;
        }
        for (Assignment assignment : assignments) {
            System.out.printf("[Asignación %d] %s - %s | %s | Sección %s | Año %d%n",
                    assignment.getId(), assignment.getSubject().getCode(),
                    assignment.getSubject().getName(), assignment.getSubject().getGrade().getName(),
                    assignment.getSection(), assignment.getAcademicYear());
            System.out.printf("    Programado: %d min de %d min semanales%n",
                    assignment.getScheduledMinutes(),
                    assignment.getSubject().getWeeklyHours() * 60);
            if (assignment.getSlots().isEmpty()) {
                System.out.println("    Sin franjas horarias.");
            }
            assignment.getSlots().forEach(slot -> System.out.printf(
                    "    [Franja %d] %s %s - %s%n",
                    slot.getId(), slot.getDay(), slot.getStartTime(), slot.getEndTime()));
        }
    }

    public void showWeeklySchedule(Teacher teacher, int academicYear,
            List<Assignment> assignments) {
        System.out.println("Horario semanal de " + teacher.getFullName()
                + " - año " + academicYear + ":");
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
            showMessage("  No hay franjas horarias registradas para ese año.");
            return;
        }
        System.out.printf("%-10s %-13s %-9s %-24s %-12s %-8s%n",
                "DÍA", "HORARIO", "CÓDIGO", "MATERIA", "GRADO", "SECCIÓN");
        rows.forEach(row -> System.out.printf("%-10s %-13s %-9s %-24s %-12s %-8s%n",
                row.slot().getDay(),
                row.slot().getStartTime() + "-" + row.slot().getEndTime(),
                row.assignment().getSubject().getCode(),
                row.assignment().getSubject().getName(),
                row.assignment().getSubject().getGrade().getName(),
                row.assignment().getSection()));
    }
}