package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.PaymentService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.util.ConsoleFormat;
import com.littlehorizons.erp.util.ConsoleInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Console screens for financial operations: payments and payment concepts.
 * Everything shown to the user is in Spanish.
 */
public class FinancialView {

    public record DocumentId(String type, String number) { }

    private static final String SEPARATOR = "-".repeat(110);
    private static final String AMOUNT_PATTERN = "^[0-9]+([.,][0-9]{1,2})?$";

    private final ConsoleInput input;

    public FinancialView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("     GESTION FINANCIERA");
        System.out.println("==============================");
        System.out.println("-- Conceptos de Pago --");
        System.out.println("1. Registrar concepto de pago");
        System.out.println("2. Listar conceptos de pago");
        System.out.println("-- Pagos y Facturacion --");
        System.out.println("3. Registrar nuevo pago");
        System.out.println("4. Listar todos los pagos");
        System.out.println("5. Consultar historial de pagos de un estudiante");
        System.out.println("6. Consultar historial de pagos de un acudiente");
        System.out.println("7. Reporte de estudiantes con morosidad (pagos pendientes)");
        System.out.println("8. Generar Factura Electronica DIAN en PDF de un pago");
        System.out.println("0. Volver al menu principal");
        return input.readMenuOption("Seleccione una opcion: ");
    }

    // ---------- Conceptos ----------

    public PaymentConcept askNewConcept() {
        showMessage("Escriba '" + ConsoleInput.CANCEL_WORD + "' en cualquier momento para volver al menu.");
        PaymentConcept concept = new PaymentConcept();
        concept.setCode(input.readRequiredText("Codigo del concepto (ej: PENSION, MAT): ", 20).toUpperCase());
        concept.setName(input.readRequiredText("Nombre del concepto: ", 100));
        concept.setDescription(input.readOptionalText("Descripcion"));
        concept.setBaseAmount(askAmount("Monto base en pesos"));
        concept.setRecurring(input.readYesNo("Es un pago recurrente (ej: mensual)?"));
        concept.setActive(true);
        return concept;
    }

    public void showConceptList(List<PaymentConcept> concepts) {
        if (concepts.isEmpty()) {
            showMessage("No hay conceptos de pago registrados.");
            return;
        }
        System.out.println(SEPARATOR);
        System.out.printf("%-5s %-12s %-32s %-16s %-12s %-10s%n",
                "ID", "CODIGO", "NOMBRE", "MONTO BASE", "RECURRENTE", "ESTADO");
        System.out.println(SEPARATOR);
        for (PaymentConcept c : concepts) {
            System.out.printf("%-5d %-12s %-32s $%-15s %-12s %-10s%n",
                    c.getId(),
                    ConsoleFormat.fit(c.getCode(), 12),
                    ConsoleFormat.fit(c.getName(), 32),
                    c.getBaseAmount().toPlainString(),
                    c.isRecurring() ? "Si" : "No",
                    c.isActive() ? "Activo" : "Inactivo");
        }
        System.out.println(SEPARATOR);
        System.out.println("Total: " + concepts.size() + " conceptos");
    }

    public PaymentConcept chooseConcept(List<PaymentConcept> concepts) {
        System.out.println("\nSeleccione el concepto de pago:");
        for (int i = 0; i < concepts.size(); i++) {
            PaymentConcept c = concepts.get(i);
            System.out.printf("%d. %s - %s ($%s)%n", i + 1, c.getCode(), c.getName(), c.getBaseAmount().toPlainString());
        }
        int option = input.readIntInRange("Elija una opcion: ", 1, concepts.size());
        return concepts.get(option - 1);
    }

    // ---------- Pagos ----------

    public DocumentId askStudentDocument() {
        String type = input.readChoice("Documento del estudiante - tipo", StudentService.DOCUMENT_TYPES);
        String number = input.readRequiredText("Documento del estudiante - numero: ",
                FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public DocumentId askGuardianDocument() {
        String type = input.readChoice("Documento del acudiente - tipo", List.of("CC", "CE", "Pasaporte"));
        String number = input.readRequiredText("Documento del acudiente - numero: ",
                FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public Guardian chooseGuardian(List<Guardian> guardians) {
        if (guardians.isEmpty()) {
            return null;
        }
        System.out.println("\nAcudientes asociados al estudiante:");
        for (int i = 0; i < guardians.size(); i++) {
            Guardian g = guardians.get(i);
            System.out.printf("%d. %s (%s %s) - %s%n", i + 1, g.getFullName(),
                    g.getDocumentType(), g.getDocumentNumber(), g.getRelationship());
        }
        System.out.printf("%d. Ninguno / Pago directo%n", guardians.size() + 1);
        int opt = input.readIntInRange("Seleccione quien realiza el pago: ", 1, guardians.size() + 1);
        return opt <= guardians.size() ? guardians.get(opt - 1) : null;
    }

    public BigDecimal askPaymentAmount(BigDecimal defaultAmount) {
        if (input.readYesNo("El monto base es $" + defaultAmount + ". Desea aplicar este valor?")) {
            return defaultAmount;
        }
        return askAmount("Monto a pagar (con descuento o ajuste)");
    }

    public BigDecimal askAmount(String prompt) {
        String text = input.readRequiredPattern(prompt + ": ", AMOUNT_PATTERN,
                "Monto no valido. Ingrese un valor numerico valido (ej: 350000 o 350000.00).");
        return new BigDecimal(text.replace(',', '.'));
    }

    public LocalDate askPaymentDate() {
        return input.readDateKeepingCurrent("Fecha de pago", LocalDate.now());
    }

    public String askPaymentMethod() {
        return input.readChoice("Metodo de pago", PaymentService.PAYMENT_METHODS);
    }

    public String askReceiptNumber(int nextSeq) {
        String defaultReceipt = "REC-" + LocalDate.now().getYear() + "-" + String.format("%04d", nextSeq);
        return input.readTextKeepingCurrent("Numero de recibo", defaultReceipt);
    }

    public int askAcademicYear() {
        return input.readIntKeepingCurrent("Ano academico", LocalDate.now().getYear(), 2000, 2100);
    }

    public String askAcademicPeriod() {
        return input.readChoice("Periodo academico", PaymentService.ACADEMIC_PERIODS);
    }

    public String askNotes() {
        return input.readOptionalText("Notas u observaciones");
    }

    public int askPaymentId() {
        return input.readIntInRange("Ingrese el ID del pago: ", 1, Integer.MAX_VALUE);
    }

    public void showPaymentDetail(Payment payment) {
        System.out.println(SEPARATOR);
        System.out.println("DETALLE DEL PAGO");
        System.out.println(SEPARATOR);
        printField("ID Pago", String.valueOf(payment.getId()));
        printField("Recibo N.", payment.getReceiptNumber());
        printField("Estudiante", payment.getStudent() != null ? payment.getStudent().getFullName() : "-");
        printField("Acudiente", payment.getGuardian() != null ? payment.getGuardian().getFullName() : "No registrado");
        printField("Concepto", payment.getConcept() != null ? payment.getConcept().getName() : "-");
        printField("Monto Pagado", "$" + (payment.getAmountPaid() != null ? payment.getAmountPaid().toPlainString() : "0.00"));
        printField("Fecha de Pago", payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : "-");
        printField("Metodo de Pago", payment.getPaymentMethod());
        printField("Ano Academico", String.valueOf(payment.getAcademicYear()));
        printField("Periodo", payment.getAcademicPeriod());
        printField("Notas", payment.getNotes());
        System.out.println(SEPARATOR);
    }

    public void showPaymentList(List<Payment> payments, String title) {
        System.out.println(SEPARATOR);
        System.out.println(title);
        System.out.println(SEPARATOR);
        if (payments.isEmpty()) {
            showMessage("No hay pagos registrados.");
            return;
        }
        System.out.printf("%-6s %-14s %-26s %-20s %-14s %-12s %-12s%n",
                "ID", "RECIBO", "ESTUDIANTE", "CONCEPTO", "MONTO", "FECHA", "METODO");
        System.out.println(SEPARATOR);
        BigDecimal total = BigDecimal.ZERO;
        for (Payment p : payments) {
            String studentName = p.getStudent() != null ? p.getStudent().getFullName() : "-";
            String conceptName = p.getConcept() != null ? p.getConcept().getName() : "-";
            System.out.printf("%-6d %-14s %-26s %-20s $%-13s %-12s %-12s%n",
                    p.getId(),
                    ConsoleFormat.fit(p.getReceiptNumber(), 14),
                    ConsoleFormat.fit(studentName, 26),
                    ConsoleFormat.fit(conceptName, 20),
                    p.getAmountPaid().toPlainString(),
                    p.getPaymentDate(),
                    ConsoleFormat.fit(p.getPaymentMethod(), 12));
            total = total.add(p.getAmountPaid());
        }
        System.out.println(SEPARATOR);
        System.out.printf("Total: %d pagos | Recaudado: $%s%n", payments.size(), total.toPlainString());
    }

    public void showDelinquentReport(List<Student> students, int academicYear, String period) {
        System.out.println(SEPARATOR);
        System.out.printf("REPORTE DE MOROSIDAD - PENSION %s (Ano %d)%n", period, academicYear);
        System.out.println(SEPARATOR);
        if (students.isEmpty()) {
            showMessage("No hay estudiantes morosos. Todos los estudiantes al dia.");
            return;
        }
        System.out.printf("%-6s %-14s %-32s %-20s %-12s%n",
                "ID", "DOCUMENTO", "ESTUDIANTE", "TELEFONO", "ESTADO");
        System.out.println(SEPARATOR);
        for (Student s : students) {
            System.out.printf("%-6d %-14s %-32s %-20s %-12s%n",
                    s.getId(),
                    s.getDocumentType() + " " + s.getDocumentNumber(),
                    ConsoleFormat.fit(s.getFullName(), 32),
                    ConsoleFormat.fit(s.getPhone(), 20),
                    s.getStatus());
        }
        System.out.println(SEPARATOR);
        System.out.println("Total estudiantes con pago pendiente: " + students.size());
    }

    public void showInvoiceSuccess(String invoiceNumber, String cufe, String path) {
        System.out.println(SEPARATOR);
        System.out.println("FACTURA ELECTRONICA GENERADA EXITOSAMENTE (CUMPLIMIENTO DIAN)");
        System.out.println(SEPARATOR);
        printField("Numero Factura", invoiceNumber);
        printField("CUFE", cufe);
        printField("Archivo PDF", path);
        System.out.println("El documento ha sido guardado y esta listo para impresion o envio.");
        System.out.println(SEPARATOR);
    }

    public boolean confirm(String question) {
        return input.confirm(question);
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showError(String message) {
        input.showError(message);
    }

    private void printField(String label, String value) {
        System.out.printf("%-22s: %s%n", label, value == null ? "-" : value);
    }
}
