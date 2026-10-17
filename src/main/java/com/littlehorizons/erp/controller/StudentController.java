package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.StudentView;
import java.sql.SQLException;

/**
 * Coordinates the student menu: reads user actions, calls the service and
 * asks the view to show the result.
 */
public class StudentController {

    private final StudentService studentService;
    private final StudentView studentView;

    public StudentController(StudentService studentService, StudentView studentView) {
        this.studentService = studentService;
        this.studentView = studentView;
    }

    private interface Operation {
        void run() throws ValidationException, SQLException;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = studentView.showMenuAndReadOption();
            switch (option) {
                case 1 -> execute(this::registerStudent);
                case 2 -> execute(this::listStudents);
                case 3 -> execute(this::searchStudent);
                case 4 -> execute(this::updateStudent);
                case 5 -> execute(this::withdrawStudent);
                case 6 -> execute(this::deleteStudent);
                case 0 -> running = false;
                default -> studentView.showError("Opción inválida.");
            }
        }
    }

    private void execute(Operation operation) {
        try {
            operation.run();
        } catch (ValidationException e) {
            studentView.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Student operation failed", e);
            studentView.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void registerStudent() throws ValidationException, SQLException {
        Student student = new Student();
        student.setDocumentType(studentView.readChoice(
                "Tipo de documento", StudentService.DOCUMENT_TYPES));
        student.setDocumentNumber(studentView.readRequiredText(
                "Número de documento: ", StudentService.MAX_DOCUMENT_NUMBER_LENGTH));
        student.setFirstName(studentView.readRequiredName("Primer nombre: "));
        student.setMiddleName(studentView.readOptionalName("Segundo nombre"));
        student.setFirstLastName(studentView.readRequiredName("Primer apellido: "));
        student.setSecondLastName(studentView.readOptionalName("Segundo apellido"));
        student.setBirthDate(studentView.readDate("Fecha de nacimiento"));
        student.setGender(studentView.readChoice("Género", StudentService.GENDERS));
        student.setAddress(studentView.readOptionalText("Dirección"));
        student.setPhone(studentView.readOptionalPhone("Teléfono",
                StudentService.PHONE_MIN_LENGTH, StudentService.PHONE_MAX_LENGTH));
        student.setEmail(studentView.readOptionalText("Correo electrónico"));
        student.setBloodType(studentView.readOptionalText("Tipo de sangre"));
        student.setAllergies(studentView.readOptionalText("Alergias"));
        student.setMedicalConditions(studentView.readOptionalText("Condiciones médicas"));
        student.setAdmissionDate(studentView.readDate("Fecha de ingreso"));

        studentService.registerStudent(student);
        studentView.showMessage("Estudiante registrado con id " + student.getId() + ".");
    }

    private void listStudents() throws SQLException {
        studentView.showStudentList(studentService.listStudents());
    }

    private void searchStudent() throws ValidationException, SQLException {
        studentView.showStudentDetail(askForExistingStudent());
    }

    private void updateStudent() throws ValidationException, SQLException {
        Student student = askForExistingStudent();
        studentView.showMessage("Presione Enter para conservar el valor actual.");
        student.setFirstName(studentView.readNameKeepingCurrent(
                "Primer nombre", student.getFirstName()));
        student.setMiddleName(studentView.readNameKeepingCurrent(
                "Segundo nombre", student.getMiddleName()));
        student.setFirstLastName(studentView.readNameKeepingCurrent(
                "Primer apellido", student.getFirstLastName()));
        student.setSecondLastName(studentView.readNameKeepingCurrent(
                "Segundo apellido", student.getSecondLastName()));
        student.setBirthDate(studentView.readDateKeepingCurrent(
                "Fecha de nacimiento", student.getBirthDate()));
        student.setGender(studentView.readChoiceKeepingCurrent(
                "Género", student.getGender(), StudentService.GENDERS));
        student.setAddress(studentView.readTextKeepingCurrent("Dirección", student.getAddress()));
        student.setPhone(studentView.readPhoneKeepingCurrent("Teléfono", student.getPhone(),
                StudentService.PHONE_MIN_LENGTH, StudentService.PHONE_MAX_LENGTH));
        student.setEmail(studentView.readTextKeepingCurrent(
                "Correo electrónico", student.getEmail()));
        student.setBloodType(studentView.readTextKeepingCurrent(
                "Tipo de sangre", student.getBloodType()));
        student.setAllergies(studentView.readTextKeepingCurrent(
                "Alergias", student.getAllergies()));
        student.setMedicalConditions(studentView.readTextKeepingCurrent(
                "Condiciones médicas", student.getMedicalConditions()));

        studentService.updateStudent(student);
        studentView.showMessage("Estudiante actualizado.");
    }

    private void withdrawStudent() throws ValidationException, SQLException {
        Student student = askForExistingStudent();
        if (studentView.confirm("¿Retirar a " + student.getFullName() + "?")) {
            studentService.withdrawStudent(student.getId());
            studentView.showMessage("Estudiante retirado.");
        }
    }

    private void deleteStudent() throws ValidationException, SQLException {
        Student student = askForExistingStudent();
        studentView.showMessage("ADVERTENCIA: eliminar también borra las matrículas, notas, "
                + "asistencia y pagos de este estudiante. Para estudiantes reales use 'Retirar'.");
        if (studentView.confirm("¿Eliminar definitivamente a " + student.getFullName() + "?")) {
            studentService.deleteStudent(student.getId());
            studentView.showMessage("Estudiante eliminado.");
        }
    }

    private Student askForExistingStudent() throws ValidationException, SQLException {
        String documentType = studentView.readChoice(
                "Tipo de documento", StudentService.DOCUMENT_TYPES);
        String documentNumber = studentView.readRequiredText(
                "Número de documento: ", StudentService.MAX_DOCUMENT_NUMBER_LENGTH);
        return studentService.findByDocument(documentType, documentNumber).orElseThrow(
                () -> new ValidationException("No se encontró ningún estudiante con ese documento."));
    }
}