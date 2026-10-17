package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentGuardian;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.GuardianService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.util.List;

/**
 * Console screens for the links between students and guardians.
 * Everything shown to the user is in Spanish.
 */
public class StudentGuardianView {

    /**
     * Identifies a person by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private final ConsoleInput input;

    public StudentGuardianView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println(" VÍNCULOS ESTUDIANTE-ACUDIENTE");
        System.out.println("==============================");
        System.out.println("1. Ver acudientes de un estudiante");
        System.out.println("2. Vincular acudiente a un estudiante");
        System.out.println("3. Desvincular acudiente de un estudiante");
        System.out.println("4. Cambiar acudiente principal / contacto de emergencia");
        System.out.println("5. Ver estudiantes de un acudiente (hermanos)");
        System.out.println("0. Volver");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public DocumentId askStudentDocument() {
        String type = input.readChoice("Documento del estudiante - tipo",
                StudentService.DOCUMENT_TYPES);
        String number = input.readRequiredText("Documento del estudiante - número: ",
                FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public DocumentId askGuardianDocument() {
        String type = input.readChoice("Documento del acudiente - tipo",
                GuardianService.DOCUMENT_TYPES);
        String number = input.readRequiredText("Documento del acudiente - número: ",
                FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public boolean askIsPrimary() {
        return input.readYesNo("¿Es el acudiente principal?");
    }

    public boolean askIsEmergencyContact() {
        return input.readYesNo("¿Es contacto de emergencia?");
    }

    public boolean askPrimaryKeepingCurrent(boolean current) {
        return input.readYesNoKeepingCurrent("¿Es el acudiente principal?", current);
    }

    public boolean askEmergencyKeepingCurrent(boolean current) {
        return input.readYesNoKeepingCurrent("¿Es contacto de emergencia?", current);
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

    public void showGuardiansOfStudent(Student student, List<StudentGuardian> links) {
        System.out.println("Acudientes de " + student.getFullName() + ":");
        if (links.isEmpty()) {
            showMessage("  Este estudiante todavía no tiene acudientes vinculados.");
            return;
        }
        System.out.printf("%-5s %-32s %-12s %-12s %-10s %-10s%n",
                "ID", "NOMBRE", "PARENTESCO", "TELÉFONO", "PRINCIPAL", "EMERGENCIA");
        links.forEach(link -> System.out.printf("%-5d %-32s %-12s %-12s %-10s %-10s%n",
                link.guardian().getId(), link.guardian().getFullName(),
                link.guardian().getRelationship(), link.guardian().getPrimaryPhone(),
                yesNo(link.primary()), yesNo(link.emergencyContact())));
    }

    public void showStudentsOfGuardian(Guardian guardian, List<Student> students) {
        System.out.println("Estudiantes a cargo de " + guardian.getFullName() + ":");
        if (students.isEmpty()) {
            showMessage("  Este acudiente todavía no tiene estudiantes vinculados.");
            return;
        }
        System.out.printf("%-5s %-10s %-14s %-38s %-10s%n",
                "ID", "TIPO", "DOCUMENTO", "NOMBRE", "ESTADO");
        students.forEach(student -> System.out.printf("%-5d %-10s %-14s %-38s %-10s%n",
                student.getId(), student.getDocumentType(), student.getDocumentNumber(),
                student.getFullName(), student.getStatus()));
    }

    private String yesNo(boolean value) {
        return value ? "Sí" : "No";
    }
}