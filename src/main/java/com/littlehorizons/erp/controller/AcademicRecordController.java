package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.AcademicRecordService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.AcademicRecordView;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Coordinates the grades menu: asks the view for data, calls the service and
 * asks the view to show the result.
 */
public class AcademicRecordController {

    private final AcademicRecordService recordService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final SubjectService subjectService;
    private final AcademicRecordView view;

    public AcademicRecordController(AcademicRecordService recordService,
            StudentService studentService, TeacherService teacherService,
            SubjectService subjectService, AcademicRecordView view) {
        this.recordService = recordService;
        this.studentService = studentService;
        this.teacherService = teacherService;
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
                case 1 -> execute(this::registerGrade);
                case 2 -> execute(this::correctGrade);
                case 3 -> execute(this::showHistory);
                case 4 -> execute(this::showCourse);
                case 5 -> execute(this::showReportCard);
                case 6 -> execute(this::deleteGrade);
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
            ErrorLogger.log("Grade operation failed", e);
            view.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void registerGrade() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        Student student = askForStudent();
        Subject subject = askForSubject();
        int academicYear = view.askAcademicYear();
        String period = view.askPeriod();
        BigDecimal value = view.askGradeValue();
        String notes = view.askNotes();
        recordService.registerGrade(teacher, student, subject, academicYear, period, value,
                notes);
        view.showMessage("Nota registrada.");
    }

    private void correctGrade() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        Student student = askForStudent();
        Subject subject = askForSubject();
        int academicYear = view.askAcademicYear();
        String period = view.askPeriod();
        Optional<AcademicRecord> current =
                recordService.findRecord(student, subject, academicYear, period);
        if (current.isPresent()) {
            view.showCurrentGrade(current.get());
        }
        BigDecimal value = view.askGradeValue();
        String notes = view.askNotes();
        recordService.correctGrade(teacher, student, subject, academicYear, period, value,
                notes);
        view.showMessage("Nota corregida.");
    }

    private void showHistory() throws ValidationException, SQLException {
        Student student = askForStudent();
        view.showHistory(student, recordService.findHistoryOf(student.getId()));
    }

    private void showCourse() throws ValidationException, SQLException {
        Subject subject = askForSubject();
        int academicYear = view.askAcademicYear();
        String section = view.askSection();
        view.showCourse(subject, academicYear, section,
                recordService.findCourseRecords(subject, academicYear, section));
    }

    private void showReportCard() throws ValidationException, SQLException {
        Student student = askForStudent();
        int academicYear = view.askAcademicYear();
        view.showHistory(student, recordService.findRecordsOfYear(student.getId(), academicYear));
    }

    private void deleteGrade() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        Student student = askForStudent();
        Subject subject = askForSubject();
        int academicYear = view.askAcademicYear();
        String period = view.askPeriod();
        if (view.confirm("¿Eliminar definitivamente esa nota?")) {
            recordService.deleteGrade(teacher, student, subject, academicYear, period);
            view.showMessage("Nota eliminada.");
        }
    }

    private Teacher askForTeacher() throws ValidationException, SQLException {
        AcademicRecordView.DocumentId documentId = view.askTeacherDocument();
        return teacherService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún profesor con ese documento."));
    }

    private Student askForStudent() throws ValidationException, SQLException {
        AcademicRecordView.DocumentId documentId = view.askStudentDocument();
        return studentService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún estudiante con ese documento."));
    }

    private Subject askForSubject() throws ValidationException, SQLException {
        String code = view.askSubjectCode();
        return subjectService.findByCode(code).orElseThrow(
                () -> new ValidationException("No se encontró ninguna materia con ese código."));
    }
}