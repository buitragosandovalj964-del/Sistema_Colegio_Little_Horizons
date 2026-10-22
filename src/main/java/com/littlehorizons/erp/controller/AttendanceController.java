package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.AttendanceService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.AttendanceView;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Coordinates the attendance menu: asks the view for data, calls the service and
 * asks the view to show the result.
 */
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final SubjectService subjectService;
    private final AttendanceView view;

    public AttendanceController(AttendanceService attendanceService,
            StudentService studentService, TeacherService teacherService,
            SubjectService subjectService, AttendanceView view) {
        this.attendanceService = attendanceService;
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
                case 1 -> execute(this::takeAttendance);
                case 2 -> execute(this::correctAttendance);
                case 3 -> execute(this::showStudentAttendance);
                case 4 -> execute(this::showCourseAttendance);
                case 5 -> execute(this::showAbsenceReport);
                case 6 -> execute(this::deleteAttendance);
                case 0 -> running = false;
                default -> view.showError("Opción inválida.");
            }
        }
    }

    private void execute(Operation operation) {
        try {
            operation.run();
        } catch (ConsoleInput.CancelledException e) {
            view.showMessage("Operación cancelada. Los cambios completados anteriormente se conservan.");
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Attendance operation failed", e);
            view.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void takeAttendance() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        Subject subject = askForSubject();
        String section = view.askSection();
        LocalDate date = view.askClassDate();
        List<Student> students = attendanceService.findRollCall(teacher, subject, section, date);
        List<Attendance> entries = view.askRollCall(subject, section, date, students);
        attendanceService.saveAttendance(teacher, subject, section, date, entries);
        view.showSavedSummary(entries);
    }

    private void correctAttendance() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        Student student = askForStudent();
        Subject subject = askForSubject();
        LocalDate date = view.askClassDate();
        String status = view.askStatus();
        String notes = view.askNotes();
        attendanceService.correctAttendance(teacher, student, subject, date, status, notes);
        view.showMessage("Asistencia corregida.");
    }

    private void showStudentAttendance() throws ValidationException, SQLException {
        Student student = askForStudent();
        int academicYear = view.askAcademicYear();
        view.showStudentAttendance(student, academicYear,
                attendanceService.findAttendanceOf(student.getId(), academicYear));
    }

    private void showCourseAttendance() throws ValidationException, SQLException {
        Subject subject = askForSubject();
        String section = view.askSection();
        LocalDate date = view.askClassDate();
        view.showCourseAttendance(subject, section, date,
                attendanceService.findCourseAttendance(subject, section, date));
    }

    private void showAbsenceReport() throws ValidationException, SQLException {
        LocalDate from = view.askFromDate();
        LocalDate to = view.askToDate();
        int maxAbsences = view.askMaxAbsencesPerWeek();
        view.showAbsencePatterns(from, to, maxAbsences,
                attendanceService.findAbsencePatterns(from, to, maxAbsences));
    }

    private void deleteAttendance() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        Student student = askForStudent();
        Subject subject = askForSubject();
        LocalDate date = view.askClassDate();
        if (view.confirm("¿Eliminar definitivamente ese registro de asistencia?")) {
            attendanceService.deleteAttendance(teacher, student, subject, date);
            view.showMessage("Registro eliminado.");
        }
    }

    private Teacher askForTeacher() throws ValidationException, SQLException {
        AttendanceView.DocumentId documentId = view.askTeacherDocument();
        return teacherService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún profesor con ese documento."));
    }

    private Student askForStudent() throws ValidationException, SQLException {
        AttendanceView.DocumentId documentId = view.askStudentDocument();
        return studentService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún estudiante con ese documento."));
    }

    private Subject askForSubject() throws ValidationException, SQLException {
        String code = view.askSubjectCode();
        return subjectService.findByCode(code).orElseThrow(
                () -> new ValidationException("No se encontró ninguna materia con ese código."));
    }
}