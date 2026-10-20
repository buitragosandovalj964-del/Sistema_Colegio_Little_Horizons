package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for lending and returning resources. User-facing messages are in Spanish.
 */
public class LoanService {

    public static final String STATUS_ACTIVE = "Activo";
    public static final String STATUS_OVERDUE = "Vencido";
    public static final int DEFAULT_LOAN_DAYS = 7;
    public static final int MAX_LOAN_DAYS = 30;
    public static final int MAX_OPEN_LOANS_STUDENT = 3;
    public static final int MAX_OPEN_LOANS_TEACHER = 5;

    private static final int MAX_NOTES_LENGTH = 500;
    private static final String PERSON_ACTIVE = "Activo";

    private final LoanDao loanDao;
    private final ResourceDao resourceDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public LoanService() {
        this(new LoanDao(), new ResourceDao(), new StudentDao(), new TeacherDao());
    }

    public LoanService(LoanDao loanDao, ResourceDao resourceDao, StudentDao studentDao,
            TeacherDao teacherDao) {
        this.loanDao = loanDao;
        this.resourceDao = resourceDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // ---------- Lending and returning ----------

    public void lendToStudent(Resource resource, Student student, LocalDate loanDate,
            LocalDate dueDate, String notes) throws ValidationException, SQLException {
        if (!PERSON_ACTIVE.equals(student.getStatus())) {
            throw new ValidationException("El estudiante " + student.getFullName()
                    + " está en estado " + student.getStatus()
                    + "; solo se presta a estudiantes activos.");
        }
        lend(resource, BorrowerType.STUDENT, student.getId(), student.getFullName(),
                MAX_OPEN_LOANS_STUDENT, loanDate, dueDate, notes);
    }

    public void lendToTeacher(Resource resource, Teacher teacher, LocalDate loanDate,
            LocalDate dueDate, String notes) throws ValidationException, SQLException {
        if (!PERSON_ACTIVE.equals(teacher.getStatus())) {
            throw new ValidationException("El profesor " + teacher.getFullName()
                    + " está en estado " + teacher.getStatus()
                    + "; solo se presta a profesores activos.");
        }
        lend(resource, BorrowerType.TEACHER, teacher.getId(), teacher.getFullName(),
                MAX_OPEN_LOANS_TEACHER, loanDate, dueDate, notes);
    }

    /**
     * Registers the return of a loan. Returns the days of delay (0 if it was on time).
     */
    public long returnLoan(Loan loan, String notes) throws ValidationException, SQLException {
        FieldValidator.validateMaxLength(notes, "Las observaciones", MAX_NOTES_LENGTH);
        LocalDate today = LocalDate.now();
        boolean returned = loanDao.returnLoan(loan.getBorrowerType(), loan.getId(),
                loan.getResource().getId(), today, notes);
        if (!returned) {
            throw new ValidationException("Ese préstamo ya fue devuelto.");
        }
        return Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), today));
    }

    // ---------- Queries ----------

    public Optional<Loan> findOpenLoanOf(Resource resource) throws SQLException {
        loanDao.markOverdue(LocalDate.now());
        for (BorrowerType type : BorrowerType.values()) {
            Optional<LoanDao.LoanRow> row = loanDao.findOpenByResource(type, resource.getId());
            if (row.isPresent()) {
                return Optional.of(toLoan(row.get()));
            }
        }
        return Optional.empty();
    }

    public List<Loan> listOpenLoans() throws SQLException {
        loanDao.markOverdue(LocalDate.now());
        List<Loan> loans = new ArrayList<>();
        for (BorrowerType type : BorrowerType.values()) {
            for (LoanDao.LoanRow row : loanDao.findOpen(type)) {
                loans.add(toLoan(row));
            }
        }
        loans.sort(Comparator.comparing(Loan::getDueDate));
        return loans;
    }

    /**
     * Open loans past their due date, the oldest first.
     */
    public List<Loan> listOverdueLoans() throws SQLException {
        loanDao.markOverdue(LocalDate.now());
        List<Loan> loans = new ArrayList<>();
        for (BorrowerType type : BorrowerType.values()) {
            for (LoanDao.LoanRow row : loanDao.findOverdue(type)) {
                loans.add(toLoan(row));
            }
        }
        loans.sort(Comparator.comparing(Loan::getDueDate));
        return loans;
    }

    public List<Loan> findLoansOf(Student student) throws SQLException {
        return findLoansOf(BorrowerType.STUDENT, student.getId());
    }

    public List<Loan> findLoansOf(Teacher teacher) throws SQLException {
        return findLoansOf(BorrowerType.TEACHER, teacher.getId());
    }

    // ---------- Helpers ----------

    private void lend(Resource resource, BorrowerType type, int borrowerId, String borrowerName,
            int maxOpenLoans, LocalDate loanDate, LocalDate dueDate, String notes)
            throws ValidationException, SQLException {
        loanDao.markOverdue(LocalDate.now());
        validateDates(loanDate, dueDate);
        FieldValidator.validateMaxLength(notes, "Las observaciones", MAX_NOTES_LENGTH);

        Resource current = resourceDao.findById(resource.getId()).orElseThrow(
                () -> new ValidationException("No se encontró el recurso."));
        checkAvailable(current);

        if (loanDao.countOverdueByBorrower(type, borrowerId) > 0) {
            throw new ValidationException(borrowerName + " tiene préstamos vencidos; debe "
                    + "devolverlos antes de pedir otro recurso.");
        }
        int open = loanDao.countOpenByBorrower(type, borrowerId);
        if (open >= maxOpenLoans) {
            throw new ValidationException(borrowerName + " ya tiene " + open
                    + " préstamos abiertos (máximo " + maxOpenLoans + ").");
        }
        if (!loanDao.lend(type, current.getId(), borrowerId, loanDate, dueDate, notes)) {
            throw new ValidationException("El recurso " + current.getCode()
                    + " ya no está disponible.");
        }
    }

    private void checkAvailable(Resource resource) throws ValidationException {
        if (ResourceService.AVAILABLE.equals(resource.getStatus())) {
            return;
        }
        String reason = switch (resource.getStatus()) {
            case ResourceService.LOANED -> "ya está prestado";
            case ResourceService.MAINTENANCE -> "está en mantenimiento";
            default -> "está dado de baja";
        };
        throw new ValidationException("El recurso " + resource.getCode() + " " + reason + ".");
    }

    private void validateDates(LocalDate loanDate, LocalDate dueDate) throws ValidationException {
        if (loanDate == null || dueDate == null) {
            throw new ValidationException("Las fechas del préstamo son obligatorias.");
        }
        if (loanDate.isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha del préstamo no puede ser futura.");
        }
        if (!dueDate.isAfter(loanDate)) {
            throw new ValidationException(
                    "La fecha de devolución debe ser posterior a la del préstamo.");
        }
        if (dueDate.isAfter(loanDate.plusDays(MAX_LOAN_DAYS))) {
            throw new ValidationException(
                    "Un préstamo no puede durar más de " + MAX_LOAN_DAYS + " días.");
        }
    }

    private List<Loan> findLoansOf(BorrowerType type, int borrowerId) throws SQLException {
        loanDao.markOverdue(LocalDate.now());
        List<Loan> loans = new ArrayList<>();
        for (LoanDao.LoanRow row : loanDao.findByBorrower(type, borrowerId)) {
            loans.add(toLoan(row));
        }
        return loans;
    }

    private Loan toLoan(LoanDao.LoanRow row) throws SQLException {
        Loan loan = new Loan();
        loan.setId(row.id());
        loan.setBorrowerType(row.type());
        loan.setBorrowerId(row.borrowerId());
        loan.setLoanDate(row.loanDate());
        loan.setDueDate(row.dueDate());
        loan.setReturnDate(row.returnDate());
        loan.setStatus(row.status());
        loan.setNotes(row.notes());
        resourceDao.findById(row.resourceId()).ifPresent(loan::setResource);
        if (row.type() == BorrowerType.STUDENT) {
            studentDao.findById(row.borrowerId()).ifPresent(student -> {
                loan.setBorrowerName(student.getFullName());
                loan.setBorrowerDocument(student.getDocumentType() + " "
                        + student.getDocumentNumber());
            });
        } else {
            teacherDao.findById(row.borrowerId()).ifPresent(teacher -> {
                loan.setBorrowerName(teacher.getFullName());
                loan.setBorrowerDocument(teacher.getDocumentType() + " "
                        + teacher.getDocumentNumber());
            });
        }
        return loan;
    }
}