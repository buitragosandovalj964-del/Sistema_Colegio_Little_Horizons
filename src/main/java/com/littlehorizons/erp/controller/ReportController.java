package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.AttendanceService;
import com.littlehorizons.erp.service.AttendanceService.AbsencePattern;
import com.littlehorizons.erp.service.ClubService;
import com.littlehorizons.erp.service.EventService;
import com.littlehorizons.erp.service.LoanService;
import com.littlehorizons.erp.service.PaymentService;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ReportView;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinates the reports and statistics module: queries domain services and
 * presents structured summaries and alert reports to the user.
 */
public class ReportController {

    private final AttendanceService attendanceService;
    private final PaymentService paymentService;
    private final LoanService loanService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final SubjectService subjectService;
    private final ClubService clubService;
    private final EventService eventService;
    private final ResourceService resourceService;
    private final ReportView view;

    public ReportController(AttendanceService attendanceService, PaymentService paymentService,
            LoanService loanService, StudentService studentService, TeacherService teacherService,
            SubjectService subjectService, ClubService clubService, EventService eventService,
            ResourceService resourceService, ReportView view) {
        this.attendanceService = attendanceService;
        this.paymentService = paymentService;
        this.loanService = loanService;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.subjectService = subjectService;
        this.clubService = clubService;
        this.eventService = eventService;
        this.resourceService = resourceService;
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
                case 1 -> execute(this::showAbsenceAlerts);
                case 2 -> execute(this::showDelinquentStudents);
                case 3 -> execute(this::showOverdueLoans);
                case 4 -> execute(this::showGeneralSummary);
                case 0 -> running = false;
                default -> view.showError("Opcion invalida.");
            }
        }
    }

    private void execute(Operation operation) {
        try {
            operation.run();
        } catch (ConsoleInput.CancelledException e) {
            view.showMessage("Operacion cancelada.");
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Report operation failed", e);
            view.showError("Ocurrio un error en la base de datos al generar el reporte. Revise error_log.txt.");
        }
    }

    private void showAbsenceAlerts() throws ValidationException, SQLException {
        LocalDate from = view.askStartDate();
        LocalDate to = view.askEndDate();
        List<AbsencePattern> patterns = attendanceService.findAbsencePatterns(from, to,
                AttendanceService.DEFAULT_MAX_ABSENCES_PER_WEEK);
        view.showAbsenceReport(patterns);
    }

    private void showDelinquentStudents() throws SQLException {
        int year = view.askAcademicYear();
        String period = view.askAcademicPeriod();
        List<Student> delinquent = paymentService.findDelinquentStudents(year, period);
        view.showDelinquentReport(delinquent, year, period);
    }

    private void showOverdueLoans() throws SQLException {
        List<Loan> overdue = loanService.listOverdueLoans();
        view.showOverdueLoansReport(overdue);
    }

    private void showGeneralSummary() throws SQLException {
        List<Student> students = studentService.listStudents();
        long activeStudents = students.stream().filter(s -> "Activo".equalsIgnoreCase(s.getStatus())).count();
        long graduatedStudents = students.stream().filter(s -> "Graduado".equalsIgnoreCase(s.getStatus())).count();

        List<Teacher> teachers = teacherService.listTeachers();
        long activeTeachers = teachers.stream().filter(t -> "Activo".equalsIgnoreCase(t.getStatus())).count();

        int totalSubjects = subjectService.listSubjects().size();
        long activeClubs = clubService.listClubs().stream().filter(Club::isActive).count();
        int totalEvents = eventService.listEvents().size();
        int totalResources = resourceService.listResources(null, null).size();

        int activeLoans = loanService.listOpenLoans().size();
        int overdueLoans = loanService.listOverdueLoans().size();

        List<Payment> payments = paymentService.listAllPayments();
        BigDecimal totalRevenue = payments.stream()
                .map(Payment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeStudents", activeStudents);
        stats.put("graduatedStudents", graduatedStudents);
        stats.put("activeTeachers", activeTeachers);
        stats.put("totalSubjects", totalSubjects);
        stats.put("activeClubs", activeClubs);
        stats.put("totalEvents", totalEvents);
        stats.put("totalResources", totalResources);
        stats.put("activeLoans", activeLoans);
        stats.put("overdueLoans", overdueLoans);
        stats.put("totalRevenue", totalRevenue);

        view.showInstitutionSummary(stats);
    }
}
