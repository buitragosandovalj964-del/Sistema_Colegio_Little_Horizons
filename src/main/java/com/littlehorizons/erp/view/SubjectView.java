package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.util.List;

/**
 * Console screens for the academic module (grades and subjects): asks for data
 * and shows results. Everything shown to the user is in Spanish.
 */
public class SubjectView {

    /**
     * A new subject plus the code prefix typed by the user (the service builds the code).
     */
    public record NewSubject(Subject subject, String codePrefix) { }

    private final ConsoleInput input;

    public SubjectView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("   GESTIÓN ACADÉMICA");
        System.out.println("==============================");
        System.out.println("1. Ver grados");
        System.out.println("2. Registrar materia");
        System.out.println("3. Listar todas las materias");
        System.out.println("4. Listar materias de un grado");
        System.out.println("5. Buscar materia por código");
        System.out.println("6. Actualizar materia");
        System.out.println("7. Activar / desactivar materia");
        System.out.println("8. Eliminar materia (registros creados por error)");
        System.out.println("0. Volver al menú principal");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public NewSubject askNewSubject(List<Grade> grades) {
        showMessage("Escriba '" + ConsoleInput.CANCEL_WORD
                + "' en cualquier momento para volver al menú.");
        Subject subject = new Subject();
        subject.setGrade(askGrade(grades));
        String codePrefix = input.readRequiredPattern(
                "Prefijo del código (3 letras, ej: MAT): ",
                SubjectService.CODE_PREFIX_PATTERN,
                "El prefijo debe tener exactamente 3 letras, sin números ni espacios.");
        subject.setName(input.readRequiredText(
                "Nombre de la materia: ", SubjectService.MAX_NAME_LENGTH));
        subject.setDescription(input.readOptionalText("Descripción"));
        subject.setWeeklyHours(input.readIntInRange(
                "Horas semanales (" + SubjectService.MIN_WEEKLY_HOURS + " a "
                        + SubjectService.MAX_WEEKLY_HOURS + "): ",
                SubjectService.MIN_WEEKLY_HOURS, SubjectService.MAX_WEEKLY_HOURS));
        return new NewSubject(subject, codePrefix);
    }

    public void askSubjectChanges(Subject subject) {
        showMessage("Presione Enter para conservar el valor actual. Escriba '"
                + ConsoleInput.CANCEL_WORD + "' para volver al menú. "
                + "El código y el grado no se pueden cambiar.");
        subject.setName(input.readTextKeepingCurrent("Nombre", subject.getName()));
        subject.setDescription(input.readTextKeepingCurrent(
                "Descripción", subject.getDescription()));
        subject.setWeeklyHours(input.readIntKeepingCurrent("Horas semanales",
                subject.getWeeklyHours(),
                SubjectService.MIN_WEEKLY_HOURS, SubjectService.MAX_WEEKLY_HOURS));
    }

    public Grade askGrade(List<Grade> grades) {
        List<String> names = grades.stream().map(Grade::getName).toList();
        String selected = input.readChoice("Grado", names);
        return grades.stream()
                .filter(grade -> grade.getName().equals(selected))
                .findFirst()
                .orElseThrow();
    }

    public String askSubjectCode() {
        return input.readRequiredPattern("Código de la materia (ej: MAT-5): ",
                SubjectService.CODE_PATTERN,
                "Código no válido. Use el formato LETRAS-NÚMERO, por ejemplo MAT-5.")
                .toUpperCase();
    }

    public boolean askActiveState() {
        return input.readYesNo("¿La materia debe quedar activa?");
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

    public void showGradeList(List<Grade> grades) {
        if (grades.isEmpty()) {
            showMessage("No hay grados registrados.");
            return;
        }
        System.out.printf("%-5s %-8s %-15s%n", "ID", "NIVEL", "GRADO");
        grades.forEach(grade -> System.out.printf("%-5d %-8d %-15s%n",
                grade.getId(), grade.getNumericLevel(), grade.getName()));
    }

    public void showSubjectList(List<Subject> subjects) {
        if (subjects.isEmpty()) {
            showMessage("No hay materias para mostrar.");
            return;
        }
        System.out.printf("%-5s %-9s %-30s %-12s %-7s %-8s%n",
                "ID", "CÓDIGO", "NOMBRE", "GRADO", "HORAS", "ACTIVA");
        subjects.forEach(subject -> System.out.printf("%-5d %-9s %-30s %-12s %-7d %-8s%n",
                subject.getId(), subject.getCode(), subject.getName(),
                subject.getGrade().getName(), subject.getWeeklyHours(),
                subject.isActive() ? "Sí" : "No"));
    }

    public void showSubjectDetail(Subject subject) {
        System.out.println("------------------------------");
        printField("Id", String.valueOf(subject.getId()));
        printField("Código", subject.getCode());
        printField("Nombre", subject.getName());
        printField("Grado", subject.getGrade().getName());
        printField("Descripción", subject.getDescription());
        printField("Horas semanales", String.valueOf(subject.getWeeklyHours()));
        printField("Activa", subject.isActive() ? "Sí" : "No");
        System.out.println("------------------------------");
    }

    private void printField(String label, String value) {
        System.out.printf("%-20s%s%n", label + ":", value == null ? "-" : value);
    }
}