package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.LoanService;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.ConsoleFormat;
import com.littlehorizons.erp.util.ConsoleInput;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Console screens for resources and loans. Everything shown to the user is in Spanish.
 */
public class ResourceView {

    /**
     * Identifies a person by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private static final String SEPARATOR = "-".repeat(113);

    private final ConsoleInput input;

    public ResourceView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("     GESTIÓN DE RECURSOS");
        System.out.println("==============================");
        System.out.println("-- Catálogo --");
        System.out.println("1. Registrar recurso");
        System.out.println("2. Listar recursos (con filtros)");
        System.out.println("3. Buscar recurso por código");
        System.out.println("4. Actualizar recurso");
        System.out.println("5. Cambiar estado del recurso");
        System.out.println("6. Ver categorías");
        System.out.println("7. Eliminar recurso (registros creados por error)");
        System.out.println("-- Préstamos --");
        System.out.println("8. Prestar un recurso a un estudiante");
        System.out.println("9. Prestar un recurso a un profesor");
        System.out.println("10. Registrar devolución");
        System.out.println("11. Ver préstamos abiertos");
        System.out.println("12. Ver historial de préstamos de un estudiante o profesor");
        System.out.println("13. Reporte de préstamos vencidos");
        System.out.println("0. Volver al menú principal");
        return input.readMenuOption("Seleccione una opción: ");
    }

    // ---------- Catalog ----------

    public Resource askNewResource(List<ResourceCategory> categories) {
        showMessage("Escriba '" + ConsoleInput.CANCEL_WORD
                + "' en cualquier momento para volver al menú.");
        Resource resource = new Resource();
        resource.setCode(input.readRequiredPattern(
                "Código de inventario (3 a 50: letras, números o guiones; ej: LIB-001): ",
                ResourceService.CODE_PATTERN,
                "Código no válido. Use de 3 a 50 letras, números o guiones, sin espacios."));
        resource.setName(input.readRequiredText("Nombre: ", ResourceService.MAX_NAME_LENGTH));
        resource.setCategory(askCategory(categories));
        resource.setDescription(input.readOptionalText("Descripción"));
        if (input.readYesNo("¿Registrar la fecha de adquisición?")) {
            resource.setAcquisitionDate(input.readDate("Fecha de adquisición"));
        }
        resource.setLocation(input.readOptionalText("Ubicación física (ej: Biblioteca)"));
        return resource;
    }

    public void askResourceChanges(Resource resource, List<ResourceCategory> categories) {
        showMessage("Presione Enter para conservar el valor actual. Escriba '"
                + ConsoleInput.CANCEL_WORD + "' para volver al menú. "
                + "El código y el estado no se cambian aquí.");
        resource.setName(input.readTextKeepingCurrent("Nombre", resource.getName()));
        if (input.readYesNo("¿Cambiar la categoría (actual: "
                + resource.getCategory().name() + ")?")) {
            resource.setCategory(askCategory(categories));
        }
        resource.setDescription(input.readTextKeepingCurrent(
                "Descripción", resource.getDescription()));
        if (resource.getAcquisitionDate() != null) {
            resource.setAcquisitionDate(input.readDateKeepingCurrent(
                    "Fecha de adquisición", resource.getAcquisitionDate()));
        } else if (input.readYesNo("¿Registrar la fecha de adquisición?")) {
            resource.setAcquisitionDate(input.readDate("Fecha de adquisición"));
        }
        resource.setLocation(input.readTextKeepingCurrent(
                "Ubicación física", resource.getLocation()));
    }

    public String askResourceCode() {
        return input.readRequiredPattern("Código del recurso (ej: LIB-001): ",
                ResourceService.CODE_PATTERN, "Código no válido. Use letras, números o guiones.")
                .toUpperCase();
    }

    public ResourceCategory askCategory(List<ResourceCategory> categories) {
        List<String> names = categories.stream().map(ResourceCategory::name).toList();
        String selected = input.readChoice("Categoría", names);
        return categories.stream()
                .filter(category -> category.name().equals(selected))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Asks the optional category filter of the list. Returns null for "all categories".
     */
    public ResourceCategory askCategoryFilter(List<ResourceCategory> categories) {
        return input.readYesNo("¿Filtrar por categoría?") ? askCategory(categories) : null;
    }

    /**
     * Asks the optional status filter of the list. Returns null for "all statuses".
     */
    public String askStatusFilter() {
        return input.readYesNo("¿Filtrar por estado?")
                ? input.readChoice("Estado", ResourceService.STATUSES) : null;
    }

    public String askNewStatus() {
        return input.readChoice("Nuevo estado", ResourceService.MANUAL_STATUSES);
    }

    public void showResourceList(List<Resource> resources) {
        if (resources.isEmpty()) {
            showMessage("No hay recursos que coincidan.");
            return;
        }
        System.out.println(SEPARATOR);
        System.out.printf("%-5s %-14s %-34s %-24s %-18s %-20s%n",
                "ID", "CÓDIGO", "NOMBRE", "CATEGORÍA", "ESTADO", "UBICACIÓN");
        System.out.println(SEPARATOR);
        resources.forEach(resource -> System.out.printf("%-5d %-14s %-34s %-24s %-18s %-20s%n",
                resource.getId(), ConsoleFormat.fit(resource.getCode(), 14),
                ConsoleFormat.fit(resource.getName(), 34),
                ConsoleFormat.fit(resource.getCategory().name(), 24), resource.getStatus(),
                ConsoleFormat.fit(resource.getLocation(), 20)));
        System.out.println(SEPARATOR);
        Map<String, Long> byStatus = resources.stream().collect(
                Collectors.groupingBy(Resource::getStatus, Collectors.counting()));
        StringBuilder summary = new StringBuilder("Total: " + resources.size() + " recursos");
        for (String status : ResourceService.STATUSES) {
            summary.append(" | ").append(status).append(": ").append(byStatus.getOrDefault(status, 0L));
        }
        System.out.println(summary);
    }

    public void showResourceDetail(Resource resource) {
        System.out.println(SEPARATOR);
        printField("Id", String.valueOf(resource.getId()));
        printField("Código", resource.getCode());
        printField("Nombre", resource.getName());
        printField("Categoría", resource.getCategory().name());
        printField("Estado", resource.getStatus());
        printField("Ubicación física", resource.getLocation());
        printField("Fecha de adquisición", resource.getAcquisitionDate() == null ? null
                : resource.getAcquisitionDate().toString());
        printField("Descripción", resource.getDescription());
        System.out.println(SEPARATOR);
    }

    public void showCategories(List<ResourceCategory> categories) {
        System.out.printf("%-5s %-30s%n", "ID", "CATEGORÍA");
        categories.forEach(category -> System.out.printf("%-5d %-30s%n",
                category.id(), category.name()));
        System.out.println("Total: " + categories.size() + " categorías");
    }

    // ---------- Loans ----------

    public DocumentId askStudentDocument() {
        String type = input.readChoice("Documento del estudiante - tipo",
                StudentService.DOCUMENT_TYPES);
        String number = input.readRequiredText("Documento del estudiante - número: ",
                FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public DocumentId askTeacherDocument() {
        String type = input.readChoice("Documento del profesor - tipo",
                TeacherService.DOCUMENT_TYPES);
        String number = input.readRequiredText("Documento del profesor - número: ",
                FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public LocalDate askLoanDate() {
        return input.readDateKeepingCurrent("Fecha del préstamo", LocalDate.now());
    }

    public LocalDate askDueDate(LocalDate loanDate) {
        return input.readDateKeepingCurrent("Fecha de devolución programada (máx. "
                + LoanService.MAX_LOAN_DAYS + " días)",
                loanDate.plusDays(LoanService.DEFAULT_LOAN_DAYS));
    }

    public String askLoanNotes() {
        return input.readOptionalText("Observaciones");
    }

    public String askReturnNotes() {
        return input.readOptionalText("Estado en que se devuelve / observaciones");
    }

    public BorrowerType askBorrowerType() {
        List<String> labels = List.of(BorrowerType.STUDENT.label(), BorrowerType.TEACHER.label());
        String selected = input.readChoice("¿De quién?", labels);
        return BorrowerType.STUDENT.label().equals(selected)
                ? BorrowerType.STUDENT : BorrowerType.TEACHER;
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

    public void showLoanDetail(Loan loan) {
        System.out.println(SEPARATOR);
        printField("Recurso", loan.getResource().getCode() + " - " + loan.getResource().getName());
        printField(loan.getBorrowerType().label(),
                loan.getBorrowerName() + " (" + loan.getBorrowerDocument() + ")");
        printField("Fecha del préstamo", loan.getLoanDate().toString());
        printField("Devolución programada", loan.getDueDate().toString());
        printField("Estado", loan.getStatus());
        long days = loan.getDaysOverdue(LocalDate.now());
        if (days > 0) {
            printField("Días de atraso", String.valueOf(days));
        }
        System.out.println(SEPARATOR);
    }

    public void showOpenLoans(List<Loan> loans) {
        System.out.println(SEPARATOR);
        System.out.println("PRÉSTAMOS ABIERTOS");
        if (loans.isEmpty()) {
            System.out.println(SEPARATOR);
            showMessage("No hay préstamos abiertos.");
            return;
        }
        System.out.println(SEPARATOR);
        System.out.printf("%-12s %-26s %-28s %-10s %-11s %-11s %-9s%n",
                "CÓDIGO", "RECURSO", "PRESTATARIO", "TIPO", "PRESTADO", "VENCE", "ESTADO");
        System.out.println(SEPARATOR);
        loans.forEach(loan -> System.out.printf("%-12s %-26s %-28s %-10s %-11s %-11s %-9s%n",
                ConsoleFormat.fit(loan.getResource().getCode(), 12),
                ConsoleFormat.fit(loan.getResource().getName(), 26),
                ConsoleFormat.fit(loan.getBorrowerName(), 28), loan.getBorrowerType().label(),
                loan.getLoanDate(), loan.getDueDate(), loan.getStatus()));
        System.out.println(SEPARATOR);
        long overdue = loans.stream()
                .filter(loan -> LoanService.STATUS_OVERDUE.equals(loan.getStatus())).count();
        System.out.println("Total: " + loans.size() + " abiertos | Al día: "
                + (loans.size() - overdue) + " | Vencidos: " + overdue);
    }

    public void showOverdueReport(List<Loan> loans) {
        LocalDate today = LocalDate.now();
        System.out.println(SEPARATOR);
        System.out.println("REPORTE DE PRÉSTAMOS VENCIDOS al " + today);
        System.out.println(SEPARATOR);
        if (loans.isEmpty()) {
            showMessage("No hay préstamos vencidos. Todo está al día.");
            return;
        }
        System.out.printf("%-12s %-26s %-28s %-10s %-11s %-6s%n",
                "CÓDIGO", "RECURSO", "PRESTATARIO", "TIPO", "VENCÍA", "ATRASO");
        System.out.println(SEPARATOR);
        loans.forEach(loan -> System.out.printf("%-12s %-26s %-28s %-10s %-11s %-6s%n",
                ConsoleFormat.fit(loan.getResource().getCode(), 12),
                ConsoleFormat.fit(loan.getResource().getName(), 26),
                ConsoleFormat.fit(loan.getBorrowerName(), 28), loan.getBorrowerType().label(),
                loan.getDueDate(), loan.getDaysOverdue(today) + " d"));
        System.out.println(SEPARATOR);
        long students = loans.stream()
                .filter(loan -> loan.getBorrowerType() == BorrowerType.STUDENT).count();
        long maxDays = loans.stream().mapToLong(loan -> loan.getDaysOverdue(today)).max().orElse(0);
        System.out.println("Total: " + loans.size() + " vencidos | Estudiantes: " + students
                + " | Profesores: " + (loans.size() - students)
                + " | Mayor atraso: " + maxDays + " días");
    }

    public void showBorrowerHistory(String borrowerName, List<Loan> loans) {
        System.out.println(SEPARATOR);
        System.out.println("Historial de préstamos de " + borrowerName);
        System.out.println(SEPARATOR);
        if (loans.isEmpty()) {
            showMessage("No tiene préstamos registrados.");
            return;
        }
        System.out.printf("%-12s %-30s %-11s %-11s %-11s %-9s%n",
                "CÓDIGO", "RECURSO", "PRESTADO", "VENCE", "DEVUELTO", "ESTADO");
        System.out.println(SEPARATOR);
        loans.forEach(loan -> System.out.printf("%-12s %-30s %-11s %-11s %-11s %-9s%n",
                ConsoleFormat.fit(loan.getResource().getCode(), 12),
                ConsoleFormat.fit(loan.getResource().getName(), 30),
                loan.getLoanDate(), loan.getDueDate(),
                loan.getReturnDate() == null ? "-" : loan.getReturnDate(), loan.getStatus()));
        System.out.println(SEPARATOR);
        long open = loans.stream().filter(loan -> loan.getReturnDate() == null).count();
        System.out.println("Total: " + loans.size() + " préstamos | Abiertos: " + open
                + " | Devueltos: " + (loans.size() - open));
    }

    private void printField(String label, String value) {
        System.out.printf("%-24s%s%n", label + ":", value == null ? "-" : value);
    }
}