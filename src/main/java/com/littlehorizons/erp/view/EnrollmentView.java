package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.AssignmentService;
import com.littlehorizons.erp.service.EnrollmentService;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.time.LocalDate;
import java.util.List;

/**
 * Console screens for enrollments. Everything shown to the user is in Spanish.
 */
public class EnrollmentView {

    /**
     * Identifies a student by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    /**
     * The data asked when enrolling a student.
     */
    public record NewEnrollment(Grade grade, int academicYear, LocalDate date, String notes) { }

    private static final String POSITIVE_NUMBER_PATTERN = "[1-9]\\d{0,8}";
    private static final String SEPARATOR = "-".repeat(78);

    private final ConsoleInput input;

    public EnrollmentView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("         MATRÍCULAS");
        System.out.println("==============================");
        System.out.println("1. Matricular estudiante");
        System.out.println("2. Ver matrículas de un estudiante");
        System.out.println("3. Ver estudiantes matriculados en un grado y año");
        System.out.println("4. Completar o retirar una matrícula");
        System.out.println("5. Eliminar matrícula (registros creados por error)");
        System.out.println("0. Volver");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public DocumentId askStudentDocument() {
        showMessage("Escriba '" + ConsoleInput.CANCEL_WORD
                + "' en cualquier momento para volver al menú.");
        String type = input.readChoice("Documento del estudiante - tipo",
                StudentService.DOCUMENT_TYPES);
        String number = input.readRequiredText("Documento del estudiante - número: ",
                FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public NewEnrollment askNewEnrollment(List<Grade> grades) {
        Grade grade = askGrade(grades);
        int academicYear = askAcademicYear();
        LocalDate date = input.readDateKeepingCurrent("Fecha de matrícula", LocalDate.now());
        String notes = input.readOptionalText("Observaciones");
        return new NewEnrollment(grade, academicYear, date, notes);
    }

    public Grade askGrade(List<Grade> grades) {
        List<String> names = grades.stream().map(Grade::getName).toList();
        String selected = input.readChoice("Grado", names);
        return grades.stream()
                .filter(grade -> grade.getName().equals(selected))
                .findFirst()
                .orElseThrow();
    }

    public int askAcademicYear() {
        return input.readIntInRange("Año académico (" + AssignmentService.minAcademicYear()
                + " a " + AssignmentService.maxAcademicYear() + "): ",
                AssignmentService.minAcademicYear(), AssignmentService.maxAcademicYear());
    }

    public int askEnrollmentId() {
        String text = input.readRequiredPattern("Número de la matrícula (columna ID): ",
                POSITIVE_NUMBER_PATTERN,
                "Ingrese solo el número de la matrícula, por ejemplo 3.");
        return Integer.parseInt(text);
    }

    public String askNewStatus() {
        return input.readChoice("Nuevo estado", EnrollmentService.CLOSING_STATUSES);
    }

    public String askClosingNotes() {
        return input.readOptionalText("Motivo u observaciones");
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

    public void showEnrollments(Student student, List<Enrollment> enrollments) {
        System.out.println(SEPARATOR);
        System.out.println("Estudiante: " + student.getFullName() + " | "
                + student.getDocumentType() + " " + student.getDocumentNumber()
                + " | Estado: " + student.getStatus());
        System.out.println("Total de matrículas: " + enrollments.size());
        System.out.println(SEPARATOR);
        if (enrollments.isEmpty()) {
            showMessage("Este estudiante todavía no tiene matrículas.");
            return;
        }
        System.out.printf("%-5s %-6s %-12s %-12s %-16s %-14s%n",
                "ID", "AÑO", "GRADO", "ESTADO", "F. MATRÍCULA", "F. RETIRO");
        for (Enrollment enrollment : enrollments) {
            System.out.printf("%-5d %-6d %-12s %-12s %-16s %-14s%n",
                    enrollment.getId(), enrollment.getAcademicYear(),
                    enrollment.getGrade().getName(), enrollment.getStatus(),
                    enrollment.getEnrollmentDate(),
                    enrollment.getWithdrawalDate() == null ? "-" : enrollment.getWithdrawalDate());
            if (enrollment.getNotes() != null) {
                System.out.println("      Observaciones: " + enrollment.getNotes());
            }
        }
        System.out.println(SEPARATOR);
    }

    public void showRoster(Grade grade, int academicYear, List<Enrollment> enrollments) {
        System.out.println(SEPARATOR);
        System.out.println("Matriculados en " + grade.getName() + " - año " + academicYear);
        System.out.println("Total: " + enrollments.size());
        System.out.println(SEPARATOR);
        if (enrollments.isEmpty()) {
            showMessage("No hay estudiantes matriculados en ese grado y año.");
            return;
        }
        System.out.printf("%-5s %-10s %-12s %-38s %-12s%n",
                "ID", "TIPO", "DOCUMENTO", "ESTUDIANTE", "ESTADO");
        enrollments.forEach(enrollment -> System.out.printf("%-5d %-10s %-12s %-38s %-12s%n",
                enrollment.getId(), enrollment.getStudent().getDocumentType(),
                enrollment.getStudent().getDocumentNumber(),
                enrollment.getStudent().getFullName(), enrollment.getStatus()));
        System.out.println(SEPARATOR);
    }
}