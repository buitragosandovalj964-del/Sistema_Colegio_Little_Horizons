package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.util.List;

/**
 * Console screens for student management: asks for data and shows results.
 * Everything shown to the user is in Spanish.
 */
public class StudentView {

    /**
     * Identifies a student by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private final ConsoleInput input;

    public StudentView(ConsoleInput input) {
        this.input = input;
    }

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
        System.out.println("0. Volver al menú principal");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public Student askNewStudent() {
        showMessage("Escriba '" + ConsoleInput.CANCEL_WORD
                + "' en cualquier momento para volver al menú.");
        Student student = new Student();
        student.setDocumentType(input.readChoice(
                "Tipo de documento", StudentService.DOCUMENT_TYPES));
        student.setDocumentNumber(input.readRequiredText(
                "Número de documento: ", StudentService.MAX_DOCUMENT_NUMBER_LENGTH));
        student.setFirstName(input.readRequiredName("Primer nombre: "));
        student.setMiddleName(input.readOptionalName("Segundo nombre"));
        student.setFirstLastName(input.readRequiredName("Primer apellido: "));
        student.setSecondLastName(input.readOptionalName("Segundo apellido"));
        student.setBirthDate(input.readDate("Fecha de nacimiento"));
        student.setGender(input.readChoice("Género", StudentService.GENDERS));
        student.setAddress(input.readOptionalText("Dirección"));
        student.setPhone(input.readOptionalPhone("Teléfono",
                StudentService.PHONE_MIN_LENGTH, StudentService.PHONE_MAX_LENGTH));
        student.setEmail(input.readOptionalText("Correo electrónico"));
        student.setBloodType(input.readOptionalText("Tipo de sangre"));
        student.setAllergies(input.readOptionalText("Alergias"));
        student.setMedicalConditions(input.readOptionalText("Condiciones médicas"));
        student.setAdmissionDate(input.readDate("Fecha de ingreso"));
        return student;
    }

    public void askStudentChanges(Student student) {
        showMessage("Presione Enter para conservar el valor actual. Escriba '"
                + ConsoleInput.CANCEL_WORD + "' para volver al menú.");
        student.setFirstName(input.readNameKeepingCurrent("Primer nombre", student.getFirstName()));
        student.setMiddleName(input.readNameKeepingCurrent(
                "Segundo nombre", student.getMiddleName()));
        student.setFirstLastName(input.readNameKeepingCurrent(
                "Primer apellido", student.getFirstLastName()));
        student.setSecondLastName(input.readNameKeepingCurrent(
                "Segundo apellido", student.getSecondLastName()));
        student.setBirthDate(input.readDateKeepingCurrent(
                "Fecha de nacimiento", student.getBirthDate()));
        student.setGender(input.readChoiceKeepingCurrent(
                "Género", student.getGender(), StudentService.GENDERS));
        student.setAddress(input.readTextKeepingCurrent("Dirección", student.getAddress()));
        student.setPhone(input.readPhoneKeepingCurrent("Teléfono", student.getPhone(),
                StudentService.PHONE_MIN_LENGTH, StudentService.PHONE_MAX_LENGTH));
        student.setEmail(input.readTextKeepingCurrent(
                "Correo electrónico", student.getEmail()));
        student.setBloodType(input.readTextKeepingCurrent(
                "Tipo de sangre", student.getBloodType()));
        student.setAllergies(input.readTextKeepingCurrent("Alergias", student.getAllergies()));
        student.setMedicalConditions(input.readTextKeepingCurrent(
                "Condiciones médicas", student.getMedicalConditions()));
    }

    public DocumentId askDocumentId() {
        String type = input.readChoice("Tipo de documento", StudentService.DOCUMENT_TYPES);
        String number = input.readRequiredText(
                "Número de documento: ", StudentService.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
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

    private void printField(String label, String value) {
        System.out.printf("%-22s%s%n", label + ":", value == null ? "-" : value);
    }
}