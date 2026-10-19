package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.service.AssignmentService;
import com.littlehorizons.erp.service.AttendanceService;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Console screens for attendance. Everything shown to the user is in Spanish.
 */
public class AttendanceView {

    /**
     * Identifies a person by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private static final String SEPARATOR = "-".repeat(78);

    private final ConsoleInput input;

    public AttendanceView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("          ASISTENCIA");
        System.out.println("==============================");
        System.out.println("1. Tomar asistencia de un curso");
        System.out.println("2. Corregir asistencia de un estudiante");
        System.out.println("3. Ver asistencia de un estudiante");
        System.out.println("4. Ver asistencia de un curso en una fecha");
        System.out.println("5. Reporte de inasistencias (patrones preocupantes)");
        System.out.println("6. Eliminar registro de asistencia (creado por error)");
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

    public DocumentId askStudentDocument() {
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

    public String askSection() {
        return input.readRequiredPattern("Sección o grupo (una letra: A, B, C...): ",
                AssignmentService.SECTION_PATTERN, "La sección debe ser una sola letra.");
    }

    public LocalDate askClassDate() {
        return input.readDateKeepingCurrent("Fecha de la clase", LocalDate.now());
    }

    public int askAcademicYear() {
        return input.readIntInRange("Año (" + AssignmentService.minAcademicYear()
                + " a " + AssignmentService.maxAcademicYear() + "): ",
                AssignmentService.minAcademicYear(), AssignmentService.maxAcademicYear());
    }

    public String askStatus() {
        return input.readChoice("Estado de asistencia", AttendanceService.STATUSES);
    }

    public String askNotes() {
        return input.readOptionalText("Observaciones");
    }

    public LocalDate askFromDate() {
        return input.readDateKeepingCurrent("Desde", LocalDate.now().minusDays(28));
    }

    public LocalDate askToDate() {
        return input.readDateKeepingCurrent("Hasta", LocalDate.now());
    }

    public int askMaxAbsencesPerWeek() {
        return input.readIntKeepingCurrent("Reportar si hay MÁS de cuántas ausencias en una semana",
                AttendanceService.DEFAULT_MAX_ABSENCES_PER_WEEK, 1, 20);
    }

    /**
     * Asks the status of each student of the class. Pressing Enter keeps "Presente".
     */
    public List<Attendance> askRollCall(Subject subject, String section, LocalDate date,
            List<Student> students) {
        System.out.println(SEPARATOR);
        System.out.println("Asistencia de " + subject.getCode() + " - " + subject.getName()
                + " | sección " + section.toUpperCase() + " | " + dayName(date) + " " + date);
        System.out.println("Estudiantes: " + students.size()
                + ". Pulse Enter para marcar 'Presente'.");
        System.out.println(SEPARATOR);
        List<Attendance> entries = new ArrayList<>();
        for (int index = 0; index < students.size(); index++) {
            Student student = students.get(index);
            String status = input.readChoiceKeepingCurrent(
                    (index + 1) + "/" + students.size() + " " + student.getFullName(),
                    AttendanceService.PRESENT, AttendanceService.STATUSES);
            Attendance entry = new Attendance();
            entry.setStudent(student);
            entry.setStatus(status);
            if (!AttendanceService.PRESENT.equals(status)) {
                entry.setNotes(input.readOptionalText("   Observaciones"));
            }
            entries.add(entry);
        }
        return entries;
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

    public void showSavedSummary(List<Attendance> entries) {
        Map<String, Long> counts = AttendanceService.countByStatus(entries);
        showMessage("Asistencia guardada: " + summaryOf(counts) + ".");
    }

    public void showStudentAttendance(Student student, int academicYear,
            List<Attendance> records) {
        System.out.println(SEPARATOR);
        System.out.println("Estudiante: " + student.getFullName() + " | "
                + student.getDocumentType() + " " + student.getDocumentNumber()
                + " | año " + academicYear);
        System.out.println(SEPARATOR);
        if (records.isEmpty()) {
            showMessage("No hay asistencia registrada en ese año.");
            return;
        }
        System.out.printf("%-12s %-11s %-9s %-26s %-15s%n",
                "FECHA", "DÍA", "CÓDIGO", "MATERIA", "ESTADO");
        for (Attendance record : records) {
            System.out.printf("%-12s %-11s %-9s %-26s %-15s%n",
                    record.getDate(), dayName(record.getDate()), record.getSubject().getCode(),
                    record.getSubject().getName(), record.getStatus());
            if (record.getNotes() != null) {
                System.out.println("             Observaciones: " + record.getNotes());
            }
        }
        System.out.println(SEPARATOR);
        System.out.println("Resumen: " + summaryOf(AttendanceService.countByStatus(records)));
    }

    public void showCourseAttendance(Subject subject, String section, LocalDate date,
            List<Attendance> records) {
        System.out.println(SEPARATOR);
        System.out.println(subject.getCode() + " - " + subject.getName() + " | sección "
                + section.toUpperCase() + " | " + dayName(date) + " " + date);
        System.out.println(SEPARATOR);
        if (records.isEmpty()) {
            showMessage("No hay asistencia registrada para ese curso en esa fecha.");
            return;
        }
        System.out.printf("%-38s %-15s %-20s%n", "ESTUDIANTE", "ESTADO", "OBSERVACIONES");
        records.forEach(record -> System.out.printf("%-38s %-15s %-20s%n",
                record.getStudent().getFullName(), record.getStatus(),
                record.getNotes() == null ? "-" : record.getNotes()));
        System.out.println(SEPARATOR);
        System.out.println("Resumen: " + summaryOf(AttendanceService.countByStatus(records)));
    }

    public void showAbsencePatterns(LocalDate from, LocalDate to, int maxAbsences,
            List<AttendanceService.AbsencePattern> patterns) {
        System.out.println(SEPARATOR);
        System.out.println("Reporte de inasistencias del " + from + " al " + to);
        System.out.println("Criterio: más de " + maxAbsences
                + " ausencias en una semana (todas las materias; no cuentan excusas ni tardanzas)");
        System.out.println(SEPARATOR);
        if (patterns.isEmpty()) {
            showMessage("Ningún estudiante supera ese límite en el rango indicado.");
            return;
        }
        System.out.printf("%-12s %-28s %-38s %-10s%n",
                "SEMANA DEL", "AL (VIERNES)", "ESTUDIANTE", "AUSENCIAS");
        patterns.forEach(pattern -> System.out.printf("%-12s %-28s %-38s %-10d%n",
                pattern.weekStart(), pattern.weekStart().plusDays(4),
                pattern.student().getFullName(), pattern.absences()));
        System.out.println(SEPARATOR);
        System.out.println("Total de casos: " + patterns.size());
    }

    private String summaryOf(Map<String, Long> counts) {
        StringBuilder summary = new StringBuilder();
        for (String status : AttendanceService.STATUSES) {
            if (summary.length() > 0) {
                summary.append(" | ");
            }
            summary.append(status).append(": ").append(counts.getOrDefault(status, 0L));
        }
        return summary.toString();
    }

    private String dayName(LocalDate date) {
        int index = date.getDayOfWeek().getValue() - 1;
        return index < AssignmentService.DAYS.size() ? AssignmentService.DAYS.get(index) : "Fin de semana";
    }
}