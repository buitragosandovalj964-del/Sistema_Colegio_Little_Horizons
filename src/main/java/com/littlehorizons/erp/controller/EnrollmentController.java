package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.EnrollmentService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.EnrollmentView;
import java.sql.SQLException;

/**
 * Coordinates the enrollment menu: asks the view for data, calls the service
 * and asks the view to show the result.
 */
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final StudentService studentService;
    private final EnrollmentView view;

    public EnrollmentController(EnrollmentService enrollmentService,
            StudentService studentService, EnrollmentView view) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
        this.view = view;
    }

    private interface Operation {
        void run() throws ValidationException, SQLException;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = view.showMenuAndReadOption();
            switch (option) {
                case 1 -> execute(this::enrollStudent);
                case 2 -> execute(this::showEnrollmentsOfStudent);
                case 3 -> execute(this::showRoster);
                case 4 -> execute(this::changeStatus);
                case 5 -> execute(this::deleteEnrollment);
                case 0 -> running = false;
                default -> view.showError("Opción inválida.");
            }
        }
    }

    private void execute(Operation operation) {
        try {
            operation.run();
        } catch (ConsoleInput.CancelledException e) {
            view.showMessage("Operación cancelada. No se guardó ningún cambio.");
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Enrollment operation failed", e);
            view.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void enrollStudent() throws ValidationException, SQLException {
        Student student = askForStudent();
        EnrollmentView.NewEnrollment data = view.askNewEnrollment(enrollmentService.listGrades());
        enrollmentService.registerEnrollment(student, data.grade(), data.academicYear(),
                data.date(), data.notes());
        view.showMessage("Estudiante matriculado en " + data.grade().getName()
                + " para el año " + data.academicYear() + ".");
        showEnrollmentsOf(student);
    }

    private void showEnrollmentsOfStudent() throws ValidationException, SQLException {
        showEnrollmentsOf(askForStudent());
    }

    private void showRoster() throws SQLException {
        Grade grade = view.askGrade(enrollmentService.listGrades());
        int academicYear = view.askAcademicYear();
        view.showRoster(grade, academicYear,
                enrollmentService.findRoster(grade.getId(), academicYear));
    }

    private void changeStatus() throws ValidationException, SQLException {
        Student student = askForStudent();
        showEnrollmentsOf(student);
        int enrollmentId = view.askEnrollmentId();
        String newStatus = view.askNewStatus();
        String notes = view.askClosingNotes();
        enrollmentService.changeStatus(student, enrollmentId, newStatus, notes);
        view.showMessage("Matrícula actualizada a " + newStatus + ".");
        if ("Graduado".equals(student.getStatus())) {
            view.showMessage("El estudiante completó el último grado y quedó como Graduado.");
        }
        showEnrollmentsOf(student);
    }

    private void deleteEnrollment() throws ValidationException, SQLException {
        Student student = askForStudent();
        showEnrollmentsOf(student);
        int enrollmentId = view.askEnrollmentId();
        if (view.confirm("¿Eliminar definitivamente la matrícula " + enrollmentId + "?")) {
            enrollmentService.deleteEnrollment(student, enrollmentId);
            view.showMessage("Matrícula eliminada.");
            showEnrollmentsOf(student);
        }
    }

    private void showEnrollmentsOf(Student student) throws SQLException {
        view.showEnrollments(student, enrollmentService.findEnrollmentsOf(student.getId()));
    }

    private Student askForStudent() throws ValidationException, SQLException {
        EnrollmentView.DocumentId documentId = view.askStudentDocument();
        return studentService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún estudiante con ese documento."));
    }
}
