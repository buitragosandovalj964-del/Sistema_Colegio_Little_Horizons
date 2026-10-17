package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.StudentView;
import java.sql.SQLException;

/**
 * Coordinates the student menu: asks the view for data, calls the service and
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
        } catch (ConsoleInput.CancelledException e) {
            studentView.showMessage("Operación cancelada. No se guardó ningún cambio.");
        } catch (ValidationException e) {
            studentView.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Student operation failed", e);
            studentView.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void registerStudent() throws ValidationException, SQLException {
        Student student = studentView.askNewStudent();
        studentService.registerStudent(student);
        studentView.showMessage("Estudiante registrado con id " + student.getId() + ".");
    }

    private void listStudents() throws SQLException {
        studentView.showStudentList(studentService.listStudents());
    }

    private void searchStudent() throws ValidationException, SQLException {
        studentView.showStudentDetail(findStudentFromUser());
    }

    private void updateStudent() throws ValidationException, SQLException {
        Student student = findStudentFromUser();
        studentView.askStudentChanges(student);
        studentService.updateStudent(student);
        studentView.showMessage("Estudiante actualizado.");
    }

    private void withdrawStudent() throws ValidationException, SQLException {
        Student student = findStudentFromUser();
        if (studentView.confirm("¿Retirar a " + student.getFullName() + "?")) {
            studentService.withdrawStudent(student.getId());
            studentView.showMessage("Estudiante retirado.");
        }
    }

    private void deleteStudent() throws ValidationException, SQLException {
        Student student = findStudentFromUser();
        studentView.showMessage("ADVERTENCIA: eliminar también borra las matrículas, notas, "
                + "asistencia y pagos de este estudiante. Para estudiantes reales use 'Retirar'.");
        if (studentView.confirm("¿Eliminar definitivamente a " + student.getFullName() + "?")) {
            studentService.deleteStudent(student.getId());
            studentView.showMessage("Estudiante eliminado.");
        }
    }

    private Student findStudentFromUser() throws ValidationException, SQLException {
        StudentView.DocumentId documentId = studentView.askDocumentId();
        return studentService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún estudiante con ese documento."));
    }
}