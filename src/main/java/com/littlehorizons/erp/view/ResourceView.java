package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.util.ConsoleFormat;
import com.littlehorizons.erp.util.ConsoleInput;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

/**
 * Console screens for the resources catalog. Everything shown to the user is in Spanish.
 */
public class ResourceView {

    private static final String SEPARATOR = "-".repeat(100);

    private final ConsoleInput input;

    public ResourceView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("     GESTIÓN DE RECURSOS");
        System.out.println("==============================");
        System.out.println("1. Registrar recurso");
        System.out.println("2. Listar recursos (con filtros)");
        System.out.println("3. Buscar recurso por código");
        System.out.println("4. Actualizar recurso");
        System.out.println("5. Cambiar estado del recurso");
        System.out.println("6. Ver categorías");
        System.out.println("7. Eliminar recurso (registros creados por error)");
        System.out.println("0. Volver al menú principal");
        return input.readMenuOption("Seleccione una opción: ");
    }

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

    public boolean confirm(String question) {
        return input.confirm(question);
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showError(String message) {
        input.showError(message);
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

    private void printField(String label, String value) {
        System.out.printf("%-22s%s%n", label + ":", value == null ? "-" : value);
    }

}