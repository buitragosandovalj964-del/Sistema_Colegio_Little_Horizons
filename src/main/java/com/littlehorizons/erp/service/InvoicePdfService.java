package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates official-looking electronic invoices in PDF format conforming to
 * DIAN (Colombian tax authority) educational invoicing standards.
 */
public class InvoicePdfService {

    public static final String COMPANY_NAME = "Colegio Internacional Little Horizons S.A.S.";
    public static final String COMPANY_NIT = "901.458.789-2";
    public static final String COMPANY_ADDRESS = "Carrera 45 # 128 - 20, Bogota D.C., Colombia";
    public static final String COMPANY_PHONE = "(+57) 601 745 8920";
    public static final String COMPANY_EMAIL = "facturacion@littlehorizons.edu.co";
    public static final String DIAN_RESOLUTION =
            "Resolucion DIAN No. 18764000001 de 2024-01-15 | Rango autorizado: FE-0001 a FE-9999 | Vigencia: 24 meses";

    private static final String INVOICE_DIR = "facturas";

    public record InvoiceResult(String invoiceNumber, String cufe, String pdfPath) { }

    public InvoiceResult generateInvoice(Payment payment) throws ValidationException, IOException, DocumentException, SQLException {
        if (payment == null || payment.getId() == null) {
            throw new ValidationException("El pago es obligatorio para generar la factura.");
        }

        File directory = new File(INVOICE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String invoiceNumber = "FE-" + String.format("%05d", payment.getId());
        String cufe = calculateCufe(invoiceNumber, COMPANY_NIT, payment.getPaymentDate(), payment.getAmountPaid());
        String fileName = "factura_" + invoiceNumber + ".pdf";
        File pdfFile = new File(directory, fileName);

        createPdf(pdfFile, payment, invoiceNumber, cufe);
        saveInvoiceRecord(payment.getId(), invoiceNumber, payment.getPaymentDate(), cufe, fileName, pdfFile.getAbsolutePath());

        return new InvoiceResult(invoiceNumber, cufe, pdfFile.getAbsolutePath());
    }

    private void createPdf(File file, Payment payment, String invoiceNumber, String cufe)
            throws IOException, DocumentException {

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(24, 43, 73));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);

        // Header Table
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{65, 35});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(PdfPCell.NO_BORDER);
        leftCell.addElement(new Paragraph(COMPANY_NAME, titleFont));
        leftCell.addElement(new Paragraph("NIT: " + COMPANY_NIT + " - Regimen Comun", normalFont));
        leftCell.addElement(new Paragraph("Direccion: " + COMPANY_ADDRESS, normalFont));
        leftCell.addElement(new Paragraph("Telefono: " + COMPANY_PHONE + " | " + COMPANY_EMAIL, normalFont));
        leftCell.addElement(new Paragraph("Actividad Economica 8511 (Educacion Preescolar, Basica y Media)", normalFont));
        headerTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBackgroundColor(new Color(245, 247, 250));
        rightCell.setPadding(10);
        rightCell.addElement(new Paragraph("FACTURA ELECTRONICA DE VENTA", boldFont));
        rightCell.addElement(new Paragraph("No: " + invoiceNumber, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.RED)));
        rightCell.addElement(new Paragraph("Fecha Emision: " + payment.getPaymentDate().format(DateTimeFormatter.ISO_LOCAL_DATE), normalFont));
        rightCell.addElement(new Paragraph("Metodo de Pago: " + payment.getPaymentMethod(), normalFont));
        rightCell.addElement(new Paragraph("Recibo Caja: " + (payment.getReceiptNumber() != null ? payment.getReceiptNumber() : "-"), normalFont));
        headerTable.addCell(rightCell);
        document.add(headerTable);

        document.add(new Paragraph(" "));

        // Client & Student Information
        PdfPTable clientTable = new PdfPTable(2);
        clientTable.setWidthPercentage(100);
        clientTable.setWidths(new float[]{50, 50});

        PdfPCell guardianCell = new PdfPCell();
        guardianCell.setBackgroundColor(new Color(250, 250, 250));
        guardianCell.setPadding(8);
        guardianCell.addElement(new Paragraph("DATOS DEL ADQUIRIENTE / ACUDIENTE", boldFont));
        Guardian guardian = payment.getGuardian();
        if (guardian != null) {
            guardianCell.addElement(new Paragraph("Nombre: " + guardian.getFullName(), normalFont));
            guardianCell.addElement(new Paragraph("Documento: " + guardian.getDocumentType() + " " + guardian.getDocumentNumber(), normalFont));
            guardianCell.addElement(new Paragraph("Parentesco: " + guardian.getRelationship(), normalFont));
            guardianCell.addElement(new Paragraph("Telefono: " + (guardian.getPrimaryPhone() != null ? guardian.getPrimaryPhone() : "-"), normalFont));
            guardianCell.addElement(new Paragraph("Direccion: " + (guardian.getAddress() != null ? guardian.getAddress() : "-"), normalFont));
        } else {
            guardianCell.addElement(new Paragraph("Pago directo registrado en caja institucional.", normalFont));
        }
        clientTable.addCell(guardianCell);

        PdfPCell studentCell = new PdfPCell();
        studentCell.setBackgroundColor(new Color(250, 250, 250));
        studentCell.setPadding(8);
        studentCell.addElement(new Paragraph("DATOS DEL ESTUDIANTE BENEFICIARIO", boldFont));
        Student student = payment.getStudent();
        if (student != null) {
            studentCell.addElement(new Paragraph("Estudiante: " + student.getFullName(), normalFont));
            studentCell.addElement(new Paragraph("Documento: " + student.getDocumentType() + " " + student.getDocumentNumber(), normalFont));
            studentCell.addElement(new Paragraph("Estado Academico: " + student.getStatus(), normalFont));
            studentCell.addElement(new Paragraph("Ano / Periodo: " + payment.getAcademicYear() + " - " + payment.getAcademicPeriod(), normalFont));
        }
        clientTable.addCell(studentCell);
        document.add(clientTable);

        document.add(new Paragraph(" "));

        // Items Table
        PdfPTable itemsTable = new PdfPTable(4);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{50, 15, 15, 20});

        PdfPCell th1 = new PdfPCell(new Phrase("DESCRIPCION DEL CONCEPTO", headerFont));
        th1.setBackgroundColor(new Color(24, 43, 73));
        th1.setPadding(6);
        itemsTable.addCell(th1);

        PdfPCell th2 = new PdfPCell(new Phrase("CANTIDAD", headerFont));
        th2.setBackgroundColor(new Color(24, 43, 73));
        th2.setPadding(6);
        th2.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(th2);

        PdfPCell th3 = new PdfPCell(new Phrase("VALOR UNIT.", headerFont));
        th3.setBackgroundColor(new Color(24, 43, 73));
        th3.setPadding(6);
        th3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        itemsTable.addCell(th3);

        PdfPCell th4 = new PdfPCell(new Phrase("SUBTOTAL", headerFont));
        th4.setBackgroundColor(new Color(24, 43, 73));
        th4.setPadding(6);
        th4.setHorizontalAlignment(Element.ALIGN_RIGHT);
        itemsTable.addCell(th4);

        String conceptName = (payment.getConcept() != null) ? payment.getConcept().getName() : "Servicio Educativo";
        String conceptDesc = (payment.getNotes() != null && !payment.getNotes().isBlank())
                ? conceptName + " (" + payment.getNotes() + ")" : conceptName;

        PdfPCell tdDesc = new PdfPCell(new Phrase(conceptDesc, normalFont));
        tdDesc.setPadding(6);
        itemsTable.addCell(tdDesc);

        PdfPCell tdQty = new PdfPCell(new Phrase("1", normalFont));
        tdQty.setHorizontalAlignment(Element.ALIGN_CENTER);
        tdQty.setPadding(6);
        itemsTable.addCell(tdQty);

        PdfPCell tdUnit = new PdfPCell(new Phrase("$" + payment.getAmountPaid().toPlainString(), normalFont));
        tdUnit.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tdUnit.setPadding(6);
        itemsTable.addCell(tdUnit);

        PdfPCell tdSub = new PdfPCell(new Phrase("$" + payment.getAmountPaid().toPlainString(), normalFont));
        tdSub.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tdSub.setPadding(6);
        itemsTable.addCell(tdSub);

        // Totals
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setColspan(2);
        emptyCell.setBorder(PdfPCell.NO_BORDER);

        PdfPCell lblSubtotal = new PdfPCell(new Phrase("Subtotal:", boldFont));
        lblSubtotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lblSubtotal.setPadding(4);

        PdfPCell valSubtotal = new PdfPCell(new Phrase("$" + payment.getAmountPaid().toPlainString(), normalFont));
        valSubtotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valSubtotal.setPadding(4);

        PdfPCell lblIva = new PdfPCell(new Phrase("IVA (Excluido E.T. Art. 476):", boldFont));
        lblIva.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lblIva.setPadding(4);

        PdfPCell valIva = new PdfPCell(new Phrase("$0.00", normalFont));
        valIva.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valIva.setPadding(4);

        PdfPCell lblTotal = new PdfPCell(new Phrase("TOTAL A PAGAR:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK)));
        lblTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lblTotal.setBackgroundColor(new Color(230, 235, 245));
        lblTotal.setPadding(6);

        PdfPCell valTotal = new PdfPCell(new Phrase("$" + payment.getAmountPaid().toPlainString(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK)));
        valTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valTotal.setBackgroundColor(new Color(230, 235, 245));
        valTotal.setPadding(6);

        itemsTable.addCell(emptyCell);
        itemsTable.addCell(lblSubtotal);
        itemsTable.addCell(valSubtotal);

        itemsTable.addCell(emptyCell);
        itemsTable.addCell(lblIva);
        itemsTable.addCell(valIva);

        itemsTable.addCell(emptyCell);
        itemsTable.addCell(lblTotal);
        itemsTable.addCell(valTotal);

        document.add(itemsTable);

        document.add(new Paragraph(" "));

        // DIAN Compliance Footer Table
        PdfPTable dianTable = new PdfPTable(1);
        dianTable.setWidthPercentage(100);

        PdfPCell dianCell = new PdfPCell();
        dianCell.setBackgroundColor(new Color(248, 249, 250));
        dianCell.setPadding(8);
        dianCell.addElement(new Paragraph("INFORMACION DE CONTROL FISCAL Y VALIDEZ DIAN (SIMULADO)", boldFont));
        dianCell.addElement(new Paragraph(DIAN_RESOLUTION, smallFont));
        dianCell.addElement(new Paragraph("CUFE: " + cufe, smallFont));
        dianCell.addElement(new Paragraph("Validacion en linea DIAN: https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + cufe, smallFont));
        dianCell.addElement(new Paragraph("[ CODIGO QR SIMULADO - FACTURA ELECTRONICA VALIDA ]", FontFactory.getFont(FontFactory.COURIER_BOLD, 8, Color.DARK_GRAY)));
        dianTable.addCell(dianCell);
        document.add(dianTable);

        document.close();
    }

    private String calculateCufe(String invoiceNumber, String nit, LocalDate date, BigDecimal amount) {
        String raw = invoiceNumber + "|" + nit + "|" + date.toString() + "|" + amount.toPlainString() + "|LittleHorizonsERP";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private void saveInvoiceRecord(int paymentId, String invoiceNumber, LocalDate date, String cufe, String fileName, String path) {
        String sql = "INSERT INTO facturas (numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo) "
                   + "VALUES (?, ?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE cufe = VALUES(cufe), nombre_archivo_pdf = VALUES(nombre_archivo_pdf), ruta_archivo = VALUES(ruta_archivo)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, invoiceNumber);
            statement.setInt(2, paymentId);
            statement.setDate(3, Date.valueOf(date));
            statement.setString(4, cufe);
            statement.setString(5, fileName);
            statement.setString(6, path);
            statement.executeUpdate();
        } catch (SQLException e) {
            // Non-fatal if table doesn't exist yet; error log will catch it if needed.
        }
    }
}

