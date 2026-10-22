package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.service.GuardianService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.GuardianView;
import java.sql.SQLException;

/**
 * Coordinates the guardian menu: asks the view for data, calls the service and
 * asks the view to show the result.
 */
public class GuardianController {

    private final GuardianService guardianService;
    private final GuardianView guardianView;

    public GuardianController(GuardianService guardianService, GuardianView guardianView) {
        this.guardianService = guardianService;
        this.guardianView = guardianView;
    }

    private interface Operation {
        void run() throws ValidationException, SQLException;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = guardianView.showMenuAndReadOption();
            switch (option) {
                case 1 -> execute(this::registerGuardian);
                case 2 -> execute(this::listGuardians);
                case 3 -> execute(this::searchGuardian);
                case 4 -> execute(this::updateGuardian);
                case 5 -> execute(this::deleteGuardian);
                case 0 -> running = false;
                default -> guardianView.showError("Opción inválida.");
            }
        }
    }

    private void execute(Operation operation) {
        try {
            operation.run();
        } catch (ConsoleInput.CancelledException e) {
            guardianView.showMessage("Operación cancelada. Los cambios completados anteriormente se conservan.");
        } catch (ValidationException e) {
            guardianView.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Guardian operation failed", e);
            guardianView.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void registerGuardian() throws ValidationException, SQLException {
        Guardian guardian = guardianView.askNewGuardian();
        guardianService.registerGuardian(guardian);
        guardianView.showMessage("Acudiente registrado con id " + guardian.getId() + ".");
    }

    private void listGuardians() throws SQLException {
        guardianView.showGuardianList(guardianService.listGuardians());
    }

    private void searchGuardian() throws ValidationException, SQLException {
        guardianView.showGuardianDetail(findGuardianFromUser());
    }

    private void updateGuardian() throws ValidationException, SQLException {
        Guardian guardian = findGuardianFromUser();
        guardianView.askGuardianChanges(guardian);
        guardianService.updateGuardian(guardian);
        guardianView.showMessage("Acudiente actualizado.");
    }

    private void deleteGuardian() throws ValidationException, SQLException {
        Guardian guardian = findGuardianFromUser();
        guardianView.showMessage("ADVERTENCIA: eliminar también borra los vínculos de este "
                + "acudiente con sus estudiantes.");
        if (guardianView.confirm("¿Eliminar definitivamente a " + guardian.getFullName() + "?")) {
            guardianService.deleteGuardian(guardian.getId());
            guardianView.showMessage("Acudiente eliminado.");
        }
    }

    private Guardian findGuardianFromUser() throws ValidationException, SQLException {
        GuardianView.DocumentId documentId = guardianView.askDocumentId();
        return guardianService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún acudiente con ese documento."));
    }
}