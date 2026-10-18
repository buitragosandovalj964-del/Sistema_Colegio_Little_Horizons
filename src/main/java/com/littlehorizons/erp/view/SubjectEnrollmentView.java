package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.service.AssignmentService;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.SubjectEnrollmentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.time.LocalDate;
import java.util.List;

/**
 * Console screens for subject enrollments. Everything shown to the user is in Spanish.
 */
public class SubjectEnrollmentView {

    /**
     * Identifies a student by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private static final String POSITIVE_NUMBER_PATTERN = "[1-9]\\d{0,8}";
    private static final String SEPARATOR = "-".repeat(78);

    private final ConsoleInput input;

    public SubjectEnrollmentView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("  INSCRIPCIONES EN MATERIAS");
        System.out.println("==============================");
        System.out.println("1. Inscribir estudiante en una materia");
        System.out.println("2. Ver inscripciones de un estudiante");
        System.out.println("3. Ver estudiantes inscritos en una materia");
        System.out.println("4. Completar o retirar una inscripción");
        System.out.println("5. Eliminar inscripción (registros creados por error)");
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

    public String askPeriodOrAll() {
        return input.readChoice("Período", SubjectEnrollmentService.PERIOD_CHOICES);
    }

    public String askPeriod() {
        return input.readChoice("Período", SubjectEnrollmentService.PERIODS);
    }

    public String askSection() {
        return input.readRequiredPattern("Sección o grupo (una letra: A, B, C...): ",
                AssignmentService.SECTION_PATTERN, "La sección debe ser una sola letra.");
    }

    public LocalDate askEnrollmentDate() {
        return input.readDateKeepingCurrent("Fecha de inscripción", LocalDate.now());
    }

    public int askInscriptionId() {
        String text = input.readRequiredPattern("Número de la inscripción (columna ID): ",
                POSITIVE_NUMBER_PATTERN,
                "Ingrese solo el número de la inscripción, por ejemplo 3.");
        return Integer.parseInt(text);
    }

    public String askNewStatus() {
        return input.readChoice("Nuevo estado", SubjectEnrollmentService.CLOSING_STATUSES);
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

    public void showInscriptions(Student student, int academicYear,
            List<SubjectEnrollment> inscriptions) {
        System.out.println(SEPARATOR);
        System.out.println("Estudiante: " + student.getFullName() + " | "
                + student.getDocumentType() + " " + student.getDocumentNumber());
        System.out.println("Inscripciones del año " + academicYear + ": " + inscriptions.size());
        System.out.println(SEPARATOR);
        if (inscriptions.isEmpty()) {
            showMessage("Este estudiante no tiene inscripciones en ese año.");
            return;
        }
        System.out.printf("%-5s %-8s %-9s %-26s %-5s %-12s %-12s%n",
                "ID", "PERÍODO", "CÓDIGO", "MATERIA", "SEC.", "ESTADO", "FECHA");
        inscriptions.forEach(inscription -> System.out.printf(
                "%-5d %-8s %-9s %-26s %-5s %-12s %-12s%n",
                inscription.getId(), inscription.getPeriod(),
                inscription.getSubject().getCode(), inscription.getSubject().getName(),
                inscription.getSection(), inscription.getStatus(),
                inscription.getEnrollmentDate()));
        System.out.println(SEPARATOR);
    }

    public void showRoster(Subject subject, int academicYear, String period, String section,
            List<SubjectEnrollment> inscriptions) {
        System.out.println(SEPARATOR);
        System.out.println(subject.getCode() + " - " + subject.getName() + " | año "
                + academicYear + " | período " + period + " | sección " + section.toUpperCase());
        System.out.println("Estudiantes inscritos: " + inscriptions.size());
        System.out.println(SEPARATOR);
        if (inscriptions.isEmpty()) {
            showMessage("No hay estudiantes inscritos con esos datos.");
            return;
        }
        System.out.printf("%-5s %-10s %-12s %-38s %-12s%n",
                "ID", "TIPO", "DOCUMENTO", "ESTUDIANTE", "ESTADO");
        inscriptions.forEach(inscription -> System.out.printf("%-5d %-10s %-12s %-38s %-12s%n",
                inscription.getId(), inscription.getStudent().getDocumentType(),
                inscription.getStudent().getDocumentNumber(),
                inscription.getStudent().getFullName(), inscription.getStatus()));
        System.out.println(SEPARATOR);
    }
}