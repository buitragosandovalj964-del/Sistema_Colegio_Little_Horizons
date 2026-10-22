package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for payments and payment concepts.
 * User-facing messages are in Spanish.
 */
public class PaymentService {

    public static final List<String> PAYMENT_METHODS =
            List.of("Efectivo", "Transferencia", "Tarjeta Debito", "Tarjeta Credito", "Cheque");
    public static final List<String> ACADEMIC_PERIODS =
            List.of("I", "II", "III", "IV", "Anual");

    private static final int MAX_CODE_LENGTH = 20;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_NOTES_LENGTH = 500;
    private static final int RECEIPT_MAX_LENGTH = 50;
    private static final String STUDENT_ACTIVE = "Activo";
    private static final BigDecimal MAX_MONEY_AMOUNT = new BigDecimal("99999999.99");

    private final PaymentDao paymentDao;
    private final PaymentConceptDao conceptDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;

    public PaymentService() {
        this(new PaymentDao(), new PaymentConceptDao(),
                new StudentDao(), new GuardianDao());
    }

    public PaymentService(PaymentDao paymentDao, PaymentConceptDao conceptDao,
            StudentDao studentDao, GuardianDao guardianDao) {
        this.paymentDao = paymentDao;
        this.conceptDao = conceptDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
    }

    // ---------- Payment Concepts ----------

    public void registerConcept(PaymentConcept concept)
            throws ValidationException, SQLException {
        validateConcept(concept);
        if (conceptDao.findByCode(concept.getCode()).isPresent()) {
            throw new ValidationException("Ya existe un concepto con el codigo "
                    + concept.getCode() + ".");
        }
        concept.setActive(true);
        conceptDao.insert(concept);
    }

    public void updateConcept(PaymentConcept concept)
            throws ValidationException, SQLException {
        validateConcept(concept);
        Optional<PaymentConcept> existing = conceptDao.findByCode(concept.getCode());
        if (existing.isPresent() && !existing.get().getId().equals(concept.getId())) {
            throw new ValidationException("Otro concepto ya usa el codigo " + concept.getCode() + ".");
        }
        conceptDao.update(concept);
    }

    public List<PaymentConcept> listConcepts() throws SQLException {
        return conceptDao.findAll();
    }

    public List<PaymentConcept> listActiveConcepts() throws SQLException {
        return conceptDao.findActive();
    }

    public Optional<PaymentConcept> findConceptById(int id) throws SQLException {
        return conceptDao.findById(id);
    }

    // ---------- Payments ----------

    public Payment registerPayment(Student student, Guardian guardian, PaymentConcept concept,
            BigDecimal amountPaid, LocalDate paymentDate, String paymentMethod,
            String receiptNumber, Integer academicYear, String academicPeriod, String notes)
            throws ValidationException, SQLException {

        if (student == null || student.getId() == null) {
            throw new ValidationException("El estudiante es obligatorio.");
        }
        if (!STUDENT_ACTIVE.equals(student.getStatus())) {
            throw new ValidationException("El estudiante " + student.getFullName()
                    + " esta en estado " + student.getStatus()
                    + "; solo se registran pagos de estudiantes activos.");
        }
        if (concept == null || concept.getId() == null) {
            throw new ValidationException("El concepto de pago es obligatorio.");
        }
        if (!concept.isActive()) {
            throw new ValidationException("El concepto " + concept.getName() + " esta inactivo.");
        }
        validateMoneyAmount(amountPaid, "El monto pagado");
        if (paymentDate == null) {
            throw new ValidationException("La fecha de pago es obligatoria.");
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de pago no puede ser futura.");
        }
        FieldValidator.requireOneOf(paymentMethod, PAYMENT_METHODS, "El metodo de pago");
        if (receiptNumber != null && receiptNumber.length() > RECEIPT_MAX_LENGTH) {
            throw new ValidationException("El numero de recibo debe tener maximo "
                    + RECEIPT_MAX_LENGTH + " caracteres.");
        }
        FieldValidator.validateMaxLength(notes, "Las notas", MAX_NOTES_LENGTH);

        Integer guardianId = (guardian != null) ? guardian.getId() : null;
        int paymentId = paymentDao.insert(student.getId(), guardianId, concept.getId(),
                amountPaid, paymentDate, paymentMethod, receiptNumber,
                academicYear, academicPeriod, notes);

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStudent(student);
        payment.setGuardian(guardian);
        payment.setConcept(concept);
        payment.setAmountPaid(amountPaid);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentMethod(paymentMethod);
        payment.setReceiptNumber(receiptNumber);
        payment.setAcademicYear(academicYear);
        payment.setAcademicPeriod(academicPeriod);
        payment.setNotes(notes);
        return payment;
    }

    public List<Payment> findPaymentsOfStudent(int studentId) throws SQLException {
        return toPayments(paymentDao.findByStudent(studentId));
    }

    public List<Payment> findPaymentsOfGuardian(int guardianId) throws SQLException {
        return toPayments(paymentDao.findByGuardian(guardianId));
    }

    public List<Payment> listAllPayments() throws SQLException {
        return toPayments(paymentDao.findAll());
    }

    public int getNextReceiptSequence() throws SQLException {
        return paymentDao.findAll().size() + 1;
    }

    public List<Student> findDelinquentStudents(int academicYear, String period) throws SQLException {
        List<Student> activeStudents = studentDao.findAll().stream()
                .filter(s -> STUDENT_ACTIVE.equalsIgnoreCase(s.getStatus()))
                .toList();
        List<PaymentDao.PaymentRow> allPayments = paymentDao.findAll();

        List<Student> delinquent = new ArrayList<>();
        for (Student student : activeStudents) {
            boolean hasPaid = allPayments.stream().anyMatch(p ->
                    p.studentId() == student.getId()
                    && p.academicYear() != null && p.academicYear() == academicYear
                    && (period == null || period.equalsIgnoreCase(p.academicPeriod())));
            if (!hasPaid) {
                delinquent.add(student);
            }
        }
        return delinquent;
    }

    // ---------- Helpers ----------

    private void validateConcept(PaymentConcept concept) throws ValidationException {
        if (concept.getCode() == null || concept.getCode().isBlank()) {
            throw new ValidationException("El codigo del concepto es obligatorio.");
        }
        if (concept.getCode().length() > MAX_CODE_LENGTH) {
            throw new ValidationException("El codigo debe tener maximo " + MAX_CODE_LENGTH + " caracteres.");
        }
        if (concept.getName() == null || concept.getName().isBlank()) {
            throw new ValidationException("El nombre del concepto es obligatorio.");
        }
        if (concept.getName().length() > MAX_NAME_LENGTH) {
            throw new ValidationException("El nombre debe tener maximo " + MAX_NAME_LENGTH + " caracteres.");
        }
        validateMoneyAmount(concept.getBaseAmount(), "El monto base");
    }

    private void validateMoneyAmount(BigDecimal amount, String label) throws ValidationException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(label + " debe ser mayor a cero.");
        }
        if (amount.compareTo(MAX_MONEY_AMOUNT) > 0
                || amount.stripTrailingZeros().scale() > 2) {
            throw new ValidationException(label
                    + " no puede superar $99.999.999,99 ni tener más de dos decimales.");
        }
    }

    private List<Payment> toPayments(List<PaymentDao.PaymentRow> rows) throws SQLException {
        List<Payment> payments = new ArrayList<>();
        for (PaymentDao.PaymentRow row : rows) {
            Payment payment = new Payment();
            payment.setId(row.id());
            payment.setAmountPaid(row.amountPaid());
            payment.setPaymentDate(row.paymentDate());
            payment.setPaymentMethod(row.paymentMethod());
            payment.setReceiptNumber(row.receiptNumber());
            payment.setAcademicYear(row.academicYear());
            payment.setAcademicPeriod(row.academicPeriod());
            payment.setNotes(row.notes());
            studentDao.findById(row.studentId()).ifPresent(payment::setStudent);
            if (row.guardianId() != null) {
                guardianDao.findById(row.guardianId()).ifPresent(payment::setGuardian);
            }
            conceptDao.findById(row.conceptId()).ifPresent(payment::setConcept);
            payments.add(payment);
        }
        return payments;
    }
}
