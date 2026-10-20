package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ResourceView;
import java.sql.SQLException;
import java.util.List;

/**
 * Coordinates the resources menu: asks the view for data, calls the service and
 * asks the view to show the result.
 */
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourceView view;

    public ResourceController(ResourceService resourceService, ResourceView view) {
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
                case 1 -> execute(this::registerResource);
                case 2 -> execute(this::listResources);
                case 3 -> execute(this::searchResource);
                case 4 -> execute(this::updateResource);
                case 5 -> execute(this::changeStatus);
                case 6 -> execute(this::showCategories);
                case 7 -> execute(this::deleteResource);
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
            ErrorLogger.log("Resource operation failed", e);
            view.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void registerResource() throws ValidationException, SQLException {
        Resource resource = view.askNewResource(resourceService.listCategories());
        resourceService.registerResource(resource);
        view.showMessage("Recurso registrado con el código " + resource.getCode() + ".");
    }

    private void listResources() throws SQLException {
        List<ResourceCategory> categories = resourceService.listCategories();
        ResourceCategory category = view.askCategoryFilter(categories);
        String status = view.askStatusFilter();
        view.showResourceList(resourceService.listResources(category, status));
    }

    private void searchResource() throws ValidationException, SQLException {
        view.showResourceDetail(findResourceFromUser());
    }

    private void updateResource() throws ValidationException, SQLException {
        Resource resource = findResourceFromUser();
        view.askResourceChanges(resource, resourceService.listCategories());
        resourceService.updateResource(resource);
        view.showMessage("Recurso actualizado.");
    }

    private void changeStatus() throws ValidationException, SQLException {
        Resource resource = findResourceFromUser();
        view.showMessage("Estado actual de " + resource.getCode() + ": " + resource.getStatus());
        String newStatus = view.askNewStatus();
        resourceService.changeStatus(resource.getId(), newStatus);
        view.showMessage("Estado actualizado a " + newStatus + ".");
    }

    private void showCategories() throws SQLException {
        view.showCategories(resourceService.listCategories());
    }

    private void deleteResource() throws ValidationException, SQLException {
        Resource resource = findResourceFromUser();
        view.showMessage("ADVERTENCIA: eliminar borra definitivamente el recurso del inventario. "
                + "Para recursos reales use 'Dado de Baja'.");
        if (view.confirm("¿Eliminar definitivamente " + resource.getCode() + " - "
                + resource.getName() + "?")) {
            resourceService.deleteResource(resource.getId());
            view.showMessage("Recurso eliminado.");
        }
    }

    private Resource findResourceFromUser() throws ValidationException, SQLException {
        String code = view.askResourceCode();
        return resourceService.findByCode(code).orElseThrow(
                () -> new ValidationException("No se encontró ningún recurso con ese código."));
    }
}