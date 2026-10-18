package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.util.List;

/**
 * Console screens for teacher management: asks for data and shows results.
 * Everything shown to the user is in Spanish.
 */
public class TeacherView {

    /**
     * Identifies a teacher by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private final ConsoleInput input;

    public TeacherView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("    GESTIÓN DE PROFESORES");
        System.out.println("==============================");
        System.out.println("1. Registrar profesor");
        System.out.println("2. Listar profesores");
        System.out.println("3. Buscar profesor por documento");
        System.out.println("4. Actualizar profesor");
        System.out.println("5. Cambiar estado (Activo, Inactivo, Licencia)");
        System.out.println("6. Eliminar profesor (registros creados por error)");
        System.out.println("7. Asignaciones y horarios");
        System.out.println("0. Volver al menú principal");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public Teacher askNewTeacher() {
        showMessage("Escriba '" + ConsoleInput.CANCEL_WORD
                + "' en cualquier momento para volver al menú.");
        Teacher teacher = new Teacher();
        teacher.setDocumentType(input.readChoice(
                "Tipo de documento", TeacherService.DOCUMENT_TYPES));
        teacher.setDocumentNumber(input.readRequiredText(
                "Número de documento: ", FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH));
        teacher.setFirstName(input.readRequiredName("Primer nombre: "));
        teacher.setMiddleName(input.readOptionalName("Segundo nombre"));
        teacher.setFirstLastName(input.readRequiredName("Primer apellido: "));
        teacher.setSecondLastName(input.readOptionalName("Segundo apellido"));
        teacher.setEmail(input.readRequiredEmail("Correo electrónico: "));
        teacher.setPhone(input.readOptionalPhone("Teléfono",
                FieldValidator.PHONE_MIN_LENGTH, FieldValidator.PHONE_MAX_LENGTH));
        teacher.setSpecialization(input.readOptionalText("Especialización (ej: Matemáticas)"));
        teacher.setHireDate(input.readDate("Fecha de contratación"));
        return teacher;
    }

    public void askTeacherChanges(Teacher teacher) {
        showMessage("Presione Enter para conservar el valor actual. Escriba '"
                + ConsoleInput.CANCEL_WORD + "' para volver al menú.");
        teacher.setFirstName(input.readNameKeepingCurrent("Primer nombre", teacher.getFirstName()));
        teacher.setMiddleName(input.readNameKeepingCurrent(
                "Segundo nombre", teacher.getMiddleName()));
        teacher.setFirstLastName(input.readNameKeepingCurrent(
                "Primer apellido", teacher.getFirstLastName()));
        teacher.setSecondLastName(input.readNameKeepingCurrent(
                "Segundo apellido", teacher.getSecondLastName()));
        teacher.setEmail(input.readEmailKeepingCurrent(
                "Correo electrónico", teacher.getEmail()));
        teacher.setPhone(input.readPhoneKeepingCurrent("Teléfono", teacher.getPhone(),
                FieldValidator.PHONE_MIN_LENGTH, FieldValidator.PHONE_MAX_LENGTH));
        teacher.setSpecialization(input.readTextKeepingCurrent(
                "Especialización", teacher.getSpecialization()));
        teacher.setHireDate(input.readDateKeepingCurrent(
                "Fecha de contratación", teacher.getHireDate()));
    }

    public DocumentId askDocumentId() {
        String type = input.readChoice("Tipo de documento", TeacherService.DOCUMENT_TYPES);
        String number = input.readRequiredText(
                "Número de documento: ", FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public String askNewStatus() {
        return input.readChoice("Nuevo estado", TeacherService.STATUSES);
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

    public void showTeacherList(List<Teacher> teachers) {
        if (teachers.isEmpty()) {
            showMessage("No hay profesores registrados.");
            return;
        }
        System.out.printf("%-5s %-10s %-12s %-34s %-20s %-10s%n",
                "ID", "TIPO", "DOCUMENTO", "NOMBRE", "ESPECIALIZACIÓN", "ESTADO");
        teachers.forEach(teacher -> System.out.printf("%-5d %-10s %-12s %-34s %-20s %-10s%n",
                teacher.getId(), teacher.getDocumentType(), teacher.getDocumentNumber(),
                teacher.getFullName(), valueOrDash(teacher.getSpecialization()),
                teacher.getStatus()));
    }

    public void showTeacherDetail(Teacher teacher) {
        System.out.println("------------------------------");
        printField("Id", String.valueOf(teacher.getId()));
        printField("Nombre", teacher.getFullName());
        printField("Documento", teacher.getDocumentType() + " " + teacher.getDocumentNumber());
        printField("Correo electrónico", teacher.getEmail());
        printField("Teléfono", teacher.getPhone());
        printField("Especialización", teacher.getSpecialization());
        printField("Fecha de contratación", String.valueOf(teacher.getHireDate()));
        printField("Estado", teacher.getStatus());
        System.out.println("------------------------------");
    }

    private String valueOrDash(String value) {
        return value == null ? "-" : value;
    }

    private void printField(String label, String value) {
        System.out.printf("%-24s%s%n", label + ":", valueOrDash(value));
    }
}