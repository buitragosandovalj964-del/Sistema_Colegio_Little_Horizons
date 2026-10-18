package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.service.AcademicRecordService;
import com.littlehorizons.erp.service.AssignmentService;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Console screens for grades and averages. Everything shown to the user is in Spanish.
 */
public class AcademicRecordView {

    /**
     * Identifies a person by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private static final String GRADE_PATTERN = "([0-4]([.,]\\d)?|5([.,]0)?)";
    private static final String SEPARATOR = "-".repeat(78);

    private final ConsoleInput input;

    public AcademicRecordView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("  CALIFICACIONES Y PROMEDIOS");
        System.out.println("==============================");
        System.out.println("1. Registrar nota");
        System.out.println("2. Corregir nota");
        System.out.println("3. Ver historial académico de un estudiante");
        System.out.println("4. Ver notas y promedios de una materia (curso)");
        System.out.println("5. Ver boletín y promedios de un estudiante en un año");
        System.out.println("6. Eliminar nota (registros creados por error)");
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

    public int askAcademicYear() {
        return input.readIntInRange("Año académico (" + AssignmentService.minAcademicYear()
                + " a " + AssignmentService.maxAcademicYear() + "): ",
                AssignmentService.minAcademicYear(), AssignmentService.maxAcademicYear());
    }

    public String askPeriod() {
        return input.readChoice("Período", AcademicRecordService.PERIODS_WITH_FINAL);
    }

    public String askSection() {
        return input.readRequiredPattern("Sección o grupo (una letra: A, B, C...): ",
                AssignmentService.SECTION_PATTERN, "La sección debe ser una sola letra.");
    }

    public BigDecimal askGradeValue() {
        String text = input.readRequiredPattern("Nota (0.0 a 5.0, un decimal; ej: 4.5): ",
                GRADE_PATTERN, "Nota no válida. Use un valor de 0.0 a 5.0 con un decimal.");
        return new BigDecimal(text.replace(',', '.'));
    }

    public String askNotes() {
        return input.readOptionalText("Observaciones");
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

    public void showCurrentGrade(AcademicRecord record) {
        showMessage("Nota actual: " + record.getValue().toPlainString() + " (registrada por "
                + record.getTeacher().getFullName() + " el " + record.getRecordDate() + ")");
    }

    /**
     * Shows the grades of a student year by year (a report card per year).
     */
    public void showHistory(Student student, List<AcademicRecord> records) {
        System.out.println(SEPARATOR);
        System.out.println("Estudiante: " + student.getFullName() + " | "
                + student.getDocumentType() + " " + student.getDocumentNumber());
        System.out.println(SEPARATOR);
        if (records.isEmpty()) {
            showMessage("No hay notas registradas.");
            return;
        }
        Map<Integer, List<AcademicRecord>> byYear = records.stream().collect(
                Collectors.groupingBy(AcademicRecord::getAcademicYear,
                        () -> new TreeMap<>(Comparator.reverseOrder()), Collectors.toList()));
        byYear.forEach((year, yearRecords) -> {
            System.out.println("AÑO " + year);
            printMatrix("MATERIA", yearRecords,
                    record -> record.getSubject().getCode() + " " + record.getSubject().getName(),
                    "Promedio general");
            System.out.println(SEPARATOR);
        });
    }

    public void showCourse(Subject subject, int academicYear, String section,
            List<AcademicRecord> records) {
        System.out.println(SEPARATOR);
        System.out.println(subject.getCode() + " - " + subject.getName() + " | año "
                + academicYear + " | sección " + section.toUpperCase());
        System.out.println(SEPARATOR);
        if (records.isEmpty()) {
            showMessage("No hay notas registradas para ese curso.");
            return;
        }
        printMatrix("ESTUDIANTE", records,
                record -> record.getStudent().getFullName(), "Promedio del curso");
        System.out.println(SEPARATOR);
    }

    /**
     * Prints one row per label with the grade of each period, the row average and a
     * footer with the average of each period.
     */
    private void printMatrix(String labelHeader, List<AcademicRecord> records,
            Function<AcademicRecord, String> labelOf, String footerLabel) {
        Map<String, Map<String, BigDecimal>> rows = new TreeMap<>();
        for (AcademicRecord record : records) {
            rows.computeIfAbsent(labelOf.apply(record), key -> new HashMap<>())
                    .put(record.getPeriod(), record.getValue());
        }
        System.out.printf("%-34s %-6s %-6s %-6s %-6s %-6s %-6s%n",
                labelHeader, "I", "II", "III", "IV", "FINAL", "PROM.");
        rows.forEach((label, grades) -> {
            StringBuilder line = new StringBuilder(String.format("%-34s", label));
            for (String period : AcademicRecordService.PERIODS_WITH_FINAL) {
                BigDecimal value = grades.get(period);
                line.append(String.format(" %-6s", value == null ? "-" : value.toPlainString()));
            }
            line.append(String.format(" %-6s",
                    formatAverage(AcademicRecordService.averageOfPeriods(grades))));
            System.out.println(line);
        });

        Map<String, Double> averages = AcademicRecordService.averagesByPeriod(records);
        StringBuilder footer = new StringBuilder(String.format("%-34s", footerLabel));
        for (String period : AcademicRecordService.PERIODS_WITH_FINAL) {
            Double average = averages.get(period);
            footer.append(String.format(" %-6s",
                    average == null ? "-" : String.format(Locale.US, "%.2f", average)));
        }
        footer.append(String.format(" %-6s",
                formatAverage(AcademicRecordService.overallAverage(records))));
        System.out.println(footer);
    }

    private String formatAverage(OptionalDouble average) {
        return average.isPresent() ? String.format(Locale.US, "%.2f", average.getAsDouble()) : "-";
    }
}