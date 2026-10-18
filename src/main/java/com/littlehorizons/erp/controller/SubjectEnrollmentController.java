package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.SubjectEnrollmentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.SubjectEnrollmentView;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Coordinates the subject enrollment menu.
 */
public class SubjectEnrollmentController {

    private final SubjectEnrollmentService enrollmentService;
    private final StudentService studentService;
    private final SubjectService subjectService;
    private final SubjectEnrollmentView view;

    public SubjectEnrollmentController(SubjectEnrollmentService enrollmentService,
            StudentService studentService, SubjectService subjectService,
            SubjectEnrollmentView view) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
        this.subjectService = subjectService;
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
                case 2 -> execute(this::showInscriptionsOfStudent);
                case 3 -> execute(this::showRoster);
                case 4 -> execute(this::changeStatus);
                case 5 -> execute(this::deleteInscription);
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
            ErrorLogger.log("Subject enrollment operation failed", e);
            view.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void enrollStudent() throws ValidationException, SQLException {
        Student student = askForStudent();
        Subject subject = askForSubject();
        int academicYear = view.askAcademicYear();
        String period = view.askPeriodOrAll();
        String section = view.askSection();
        LocalDate date = view.askEnrollmentDate();
        enrollmentService.enroll(student, subject, academicYear, period, section, date);
        view.showMessage("Estudiante inscrito en " + subject.getCode() + ".");
        showInscriptionsOf(student, academicYear);
    }

    private void showInscriptionsOfStudent() throws ValidationException, SQLException {
        Student student = askForStudent();
        showInscriptionsOf(student, view.askAcademicYear());
    }

    private void showRoster() throws ValidationException, SQLException {
        Subject subject = askForSubject();
        int academicYear = view.askAcademicYear();
        String period = view.askPeriod();
        String section = view.askSection();
        view.showRoster(subject, academicYear, period, section,
                enrollmentService.findRoster(subject, academicYear, period, section));
    }

    private void changeStatus() throws ValidationException, SQLException {
        Student student = askForStudent();
        int academicYear = view.askAcademicYear();
        showInscriptionsOf(student, academicYear);
        int inscriptionId = view.askInscriptionId();
        String newStatus = view.askNewStatus();
        enrollmentService.changeStatus(student, inscriptionId, newStatus);
        view.showMessage("Inscripción actualizada a " + newStatus + ".");
        showInscriptionsOf(student, academicYear);
    }

    private void deleteInscription() throws ValidationException, SQLException {
        Student student = askForStudent();
        int academicYear = view.askAcademicYear();
        showInscriptionsOf(student, academicYear);
        int inscriptionId = view.askInscriptionId();
        if (view.confirm("¿Eliminar definitivamente la inscripción " + inscriptionId + "?")) {
            enrollmentService.deleteInscription(student, inscriptionId);
            view.showMessage("Inscripción eliminada.");
            showInscriptionsOf(student, academicYear);
        }
    }

    private void showInscriptionsOf(Student student, int academicYear) throws SQLException {
        view.showInscriptions(student, academicYear,
                enrollmentService.findInscriptionsOf(student.getId(), academicYear));
    }

    private Student askForStudent() throws ValidationException, SQLException {
        SubjectEnrollmentView.DocumentId documentId = view.askStudentDocument();
        return studentService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún estudiante con ese documento."));
    }

    private Subject askForSubject() throws ValidationException, SQLException {
        String code = view.askSubjectCode();
        return subjectService.findByCode(code).orElseThrow(
                () -> new ValidationException("No se encontró ninguna materia con ese código."));
    }
}