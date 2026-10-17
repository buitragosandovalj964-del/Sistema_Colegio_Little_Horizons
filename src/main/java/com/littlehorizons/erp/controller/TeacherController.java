package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.TeacherView;
import java.sql.SQLException;

/**
 * Coordinates the teacher menu: asks the view for data, calls the service and
 * asks the view to show the result.
 */
public class TeacherController {

    private final TeacherService teacherService;
    private final TeacherView teacherView;

    public TeacherController(TeacherService teacherService, TeacherView teacherView) {
        this.teacherService = teacherService;
        this.teacherView = teacherView;
    }

    private interface Operation {
        void run() throws ValidationException, SQLException;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = teacherView.showMenuAndReadOption();
            switch (option) {
                case 1 -> execute(this::registerTeacher);
                case 2 -> execute(this::listTeachers);
                case 3 -> execute(this::searchTeacher);
                case 4 -> execute(this::updateTeacher);
                case 5 -> execute(this::changeStatus);
                case 6 -> execute(this::deleteTeacher);
                case 0 -> running = false;
                default -> teacherView.showError("Opción inválida.");
            }
        }
    }

    private void execute(Operation operation) {
        try {
            operation.run();
        } catch (ConsoleInput.CancelledException e) {
            teacherView.showMessage("Operación cancelada. No se guardó ningún cambio.");
        } catch (ValidationException e) {
            teacherView.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Teacher operation failed", e);
            teacherView.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void registerTeacher() throws ValidationException, SQLException {
        Teacher teacher = teacherView.askNewTeacher();
        teacherService.registerTeacher(teacher);
        teacherView.showMessage("Profesor registrado con id " + teacher.getId() + ".");
    }

    private void listTeachers() throws SQLException {
        teacherView.showTeacherList(teacherService.listTeachers());
    }

    private void searchTeacher() throws ValidationException, SQLException {
        teacherView.showTeacherDetail(findTeacherFromUser());
    }

    private void updateTeacher() throws ValidationException, SQLException {
        Teacher teacher = findTeacherFromUser();
        teacherView.askTeacherChanges(teacher);
        teacherService.updateTeacher(teacher);
        teacherView.showMessage("Profesor actualizado.");
    }

    private void changeStatus() throws ValidationException, SQLException {
        Teacher teacher = findTeacherFromUser();
        teacherView.showMessage("Estado actual de " + teacher.getFullName() + ": "
                + teacher.getStatus());
        String newStatus = teacherView.askNewStatus();
        teacherService.changeStatus(teacher.getId(), newStatus);
        teacherView.showMessage("Estado actualizado a " + newStatus + ".");
    }

    private void deleteTeacher() throws ValidationException, SQLException {
        Teacher teacher = findTeacherFromUser();
        teacherView.showMessage("ADVERTENCIA: eliminar también borra las asignaciones de "
                + "materias de este profesor. Para profesores reales use 'Cambiar estado'.");
        if (teacherView.confirm("¿Eliminar definitivamente a " + teacher.getFullName() + "?")) {
            teacherService.deleteTeacher(teacher.getId());
            teacherView.showMessage("Profesor eliminado.");
        }
    }

    private Teacher findTeacherFromUser() throws ValidationException, SQLException {
        TeacherView.DocumentId documentId = teacherView.askDocumentId();
        return teacherService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún profesor con ese documento."));
    }
}