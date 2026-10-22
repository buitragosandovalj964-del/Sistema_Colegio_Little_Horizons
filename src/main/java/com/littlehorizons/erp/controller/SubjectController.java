package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.SubjectView;
import java.sql.SQLException;

/**
 * Coordinates the academic menu (grades and subjects): asks the view for data,
 * calls the service and asks the view to show the result.
 */
public class SubjectController {

    private final SubjectService subjectService;
    private final SubjectView subjectView;

    public SubjectController(SubjectService subjectService, SubjectView subjectView) {
        this.subjectService = subjectService;
        this.subjectView = subjectView;
    }

    private interface Operation {
        void run() throws ValidationException, SQLException;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = subjectView.showMenuAndReadOption();
            switch (option) {
                case 1 -> execute(this::listGrades);
                case 2 -> execute(this::registerSubject);
                case 3 -> execute(this::listSubjects);
                case 4 -> execute(this::listSubjectsOfGrade);
                case 5 -> execute(this::searchSubject);
                case 6 -> execute(this::updateSubject);
                case 7 -> execute(this::changeActiveState);
                case 8 -> execute(this::deleteSubject);
                case 0 -> running = false;
                default -> subjectView.showError("Opción inválida.");
            }
        }
    }

    private void execute(Operation operation) {
        try {
            operation.run();
        } catch (ConsoleInput.CancelledException e) {
            subjectView.showMessage("Operación cancelada. Los cambios completados anteriormente se conservan.");
        } catch (ValidationException e) {
            subjectView.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Subject operation failed", e);
            subjectView.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void listGrades() throws SQLException {
        subjectView.showGradeList(subjectService.listGrades());
    }

    private void registerSubject() throws ValidationException, SQLException {
        SubjectView.NewSubject data = subjectView.askNewSubject(subjectService.listGrades());
        subjectService.registerSubject(data.subject(), data.codePrefix());
        subjectView.showMessage("Materia registrada con el código "
                + data.subject().getCode() + ".");
    }

    private void listSubjects() throws SQLException {
        subjectView.showSubjectList(subjectService.listSubjects());
    }

    private void listSubjectsOfGrade() throws SQLException {
        Grade grade = subjectView.askGrade(subjectService.listGrades());
        subjectView.showSubjectList(subjectService.listSubjectsOfGrade(grade.getId()));
    }

    private void searchSubject() throws ValidationException, SQLException {
        subjectView.showSubjectDetail(findSubjectFromUser());
    }

    private void updateSubject() throws ValidationException, SQLException {
        Subject subject = findSubjectFromUser();
        subjectView.askSubjectChanges(subject);
        subjectService.updateSubject(subject);
        subjectView.showMessage("Materia actualizada.");
    }

    private void changeActiveState() throws ValidationException, SQLException {
        Subject subject = findSubjectFromUser();
        subjectView.showMessage("Estado actual de " + subject.getName() + ": "
                + (subject.isActive() ? "activa" : "inactiva"));
        boolean active = subjectView.askActiveState();
        subjectService.setActive(subject.getId(), active);
        subjectView.showMessage(active ? "Materia activada." : "Materia desactivada.");
    }

    private void deleteSubject() throws ValidationException, SQLException {
        Subject subject = findSubjectFromUser();
        subjectView.showMessage("ADVERTENCIA: eliminar también borra las asignaciones, "
                + "inscripciones, calificaciones y asistencia de esta materia. "
                + "Para materias reales use 'Activar / desactivar'.");
        if (subjectView.confirm("¿Eliminar definitivamente la materia "
                + subject.getCode() + " - " + subject.getName() + "?")) {
            subjectService.deleteSubject(subject.getId());
            subjectView.showMessage("Materia eliminada.");
        }
    }

    private Subject findSubjectFromUser() throws ValidationException, SQLException {
        String code = subjectView.askSubjectCode();
        return subjectService.findByCode(code).orElseThrow(
                () -> new ValidationException("No se encontró ninguna materia con ese código."));
    }
}