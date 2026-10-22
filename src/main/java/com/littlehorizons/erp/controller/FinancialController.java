package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentGuardian;
import com.littlehorizons.erp.service.GuardianService;
import com.littlehorizons.erp.service.InvoicePdfService;
import com.littlehorizons.erp.service.PaymentService;
import com.littlehorizons.erp.service.StudentGuardianService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.FinancialView;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates the financial menu: payment concepts, payment registrations,
 * history queries, delinquency reports and DIAN electronic invoice PDF generation.
 */
public class FinancialController {

    private final PaymentService paymentService;
    private final StudentService studentService;
    private final GuardianService guardianService;
    private final StudentGuardianService studentGuardianService;
    private final InvoicePdfService invoicePdfService;
    private final FinancialView view;

    public FinancialController(PaymentService paymentService, StudentService studentService,
            GuardianService guardianService, StudentGuardianService studentGuardianService,
            FinancialView view) {
        this(paymentService, studentService, guardianService, studentGuardianService,
                new InvoicePdfService(), view);
    }

    public FinancialController(PaymentService paymentService, StudentService studentService,
            GuardianService guardianService, StudentGuardianService studentGuardianService,
            InvoicePdfService invoicePdfService, FinancialView view) {
        this.paymentService = paymentService;
        this.studentService = studentService;
        this.guardianService = guardianService;
        this.studentGuardianService = studentGuardianService;
        this.invoicePdfService = invoicePdfService;
        this.view = view;
    }

    private interface Operation {
        void run() throws ValidationException, SQLException, Exception;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = view.showMenuAndReadOption();
            switch (option) {
                case 1 -> execute(this::registerConcept);
                case 2 -> execute(this::listConcepts);
                case 3 -> execute(this::registerPayment);
                case 4 -> execute(this::listAllPayments);
                case 5 -> execute(this::showPaymentsByStudent);
                case 6 -> execute(this::showPaymentsByGuardian);
                case 7 -> execute(this::showDelinquentReport);
                case 8 -> execute(this::generateInvoiceForPayment);
                case 0 -> running = false;
                default -> view.showError("Opcion invalida.");
            }
        }
    }

    private void execute(Operation operation) {
        try {
            operation.run();
        } catch (ConsoleInput.CancelledException e) {
            view.showMessage("Operacion cancelada. Los cambios completados anteriormente se conservan.");
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Financial database operation failed", e);
            view.showError("Ocurrio un error en la base de datos. Revise error_log.txt para mas detalles.");
        } catch (Exception e) {
            ErrorLogger.log("Financial general operation failed", e);
            view.showError("Ocurrio un error: " + e.getMessage());
        }
    }

    private void registerConcept() throws ValidationException, SQLException {
        PaymentConcept concept = view.askNewConcept();
        paymentService.registerConcept(concept);
        view.showMessage("Concepto registrado con codigo " + concept.getCode() + ".");
    }

    private void listConcepts() throws SQLException {
        view.showConceptList(paymentService.listConcepts());
    }

    private void registerPayment() throws Exception {
        Student student = findStudentFromUser();
        view.showMessage("Estudiante seleccionado: " + student.getFullName()
                + " (" + student.getDocumentType() + " " + student.getDocumentNumber() + ")");

        List<Guardian> guardians = studentGuardianService.findGuardiansOf(student.getId()).stream()
                .map(StudentGuardian::guardian)
                .toList();

        Guardian guardian = view.chooseGuardian(guardians);

        List<PaymentConcept> activeConcepts = paymentService.listActiveConcepts();
        if (activeConcepts.isEmpty()) {
            throw new ValidationException("No hay conceptos de pago activos registrados.");
        }

        PaymentConcept concept = view.chooseConcept(activeConcepts);
        BigDecimal amount = view.askPaymentAmount(concept.getBaseAmount());
        LocalDate paymentDate = view.askPaymentDate();
        String method = view.askPaymentMethod();
        String receipt = view.askReceiptNumber(paymentService.getNextReceiptSequence());
        int academicYear = view.askAcademicYear();
        String period = view.askAcademicPeriod();
        String notes = view.askNotes();

        Payment payment = paymentService.registerPayment(student, guardian, concept,
                amount, paymentDate, method, receipt, academicYear, period, notes);

        view.showMessage("Pago registrado exitosamente con recibo " + payment.getReceiptNumber() + ".");
        view.showPaymentDetail(payment);

        if (view.confirm("¿Desea generar la Factura Electronica DIAN en PDF para este pago?")) {
            InvoicePdfService.InvoiceResult result = invoicePdfService.generateInvoice(payment);
            view.showInvoiceSuccess(result.invoiceNumber(), result.cufe(), result.pdfPath());
        }
    }

    private void listAllPayments() throws SQLException {
        view.showPaymentList(paymentService.listAllPayments(), "HISTORIAL GENERAL DE PAGOS");
    }

    private void showPaymentsByStudent() throws ValidationException, SQLException {
        Student student = findStudentFromUser();
        List<Payment> payments = paymentService.findPaymentsOfStudent(student.getId());
        view.showPaymentList(payments, "PAGOS DE " + student.getFullName().toUpperCase());
    }

    private void showPaymentsByGuardian() throws ValidationException, SQLException {
        Guardian guardian = findGuardianFromUser();
        List<Payment> payments = paymentService.findPaymentsOfGuardian(guardian.getId());
        view.showPaymentList(payments, "PAGOS REALIZADOS POR " + guardian.getFullName().toUpperCase());
    }

    private void showDelinquentReport() throws SQLException {
        int year = view.askAcademicYear();
        String period = view.askAcademicPeriod();
        List<Student> delinquent = paymentService.findDelinquentStudents(year, period);
        view.showDelinquentReport(delinquent, year, period);
    }

    private void generateInvoiceForPayment() throws Exception {
        int paymentId = view.askPaymentId();
        List<Payment> all = paymentService.listAllPayments();
        Optional<Payment> match = all.stream().filter(p -> p.getId() == paymentId).findFirst();

        if (match.isEmpty()) {
            throw new ValidationException("No se encontro ningun pago con el ID " + paymentId + ".");
        }

        Payment payment = match.get();
        view.showPaymentDetail(payment);

        if (view.confirm("¿Confirmar generacion de Factura Electronica DIAN (PDF)?")) {
            InvoicePdfService.InvoiceResult result = invoicePdfService.generateInvoice(payment);
            view.showInvoiceSuccess(result.invoiceNumber(), result.cufe(), result.pdfPath());
        }
    }

    private Student findStudentFromUser() throws ValidationException, SQLException {
        FinancialView.DocumentId doc = view.askStudentDocument();
        return studentService.findByDocument(doc.type(), doc.number()).orElseThrow(
                () -> new ValidationException("No se encontro ningun estudiante con ese documento."));
    }

    private Guardian findGuardianFromUser() throws ValidationException, SQLException {
        FinancialView.DocumentId doc = view.askGuardianDocument();
        return guardianService.findByDocument(doc.type(), doc.number()).orElseThrow(
                () -> new ValidationException("No se encontro ningun acudiente con ese documento."));
    }
}
