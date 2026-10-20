package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.LoanService;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ResourceView;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Coordinates the resources menu (catalog and loans): asks the view for data,
 * calls the services and asks the view to show the result.
 */
public class ResourceController {

    private final ResourceService resourceService;
    private final LoanService loanService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final ResourceView view;

    public ResourceController(ResourceService resourceService, LoanService loanService,
            StudentService studentService, TeacherService teacherService, ResourceView view) {
        this.resourceService = resourceService;
        this.loanService = loanService;
        this.studentService = studentService;
        this.teacherService = teacherService;
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
                case 8 -> execute(this::lendToStudent);
                case 9 -> execute(this::lendToTeacher);
                case 10 -> execute(this::returnLoan);
                case 11 -> execute(this::showOpenLoans);
                case 12 -> execute(this::showBorrowerHistory);
                case 13 -> execute(this::showOverdueReport);
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

    // ---------- Catalog ----------

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

    // ---------- Loans ----------

    private void lendToStudent() throws ValidationException, SQLException {
        Resource resource = findResourceFromUser();
        Student student = askForStudent();
        LocalDate loanDate = view.askLoanDate();
        LocalDate dueDate = view.askDueDate(loanDate);
        String notes = view.askLoanNotes();
        loanService.lendToStudent(resource, student, loanDate, dueDate, notes);
        view.showMessage("Préstamo registrado: " + resource.getCode() + " para "
                + student.getFullName() + ", devolver el " + dueDate + ".");
    }

    private void lendToTeacher() throws ValidationException, SQLException {
        Resource resource = findResourceFromUser();
        Teacher teacher = askForTeacher();
        LocalDate loanDate = view.askLoanDate();
        LocalDate dueDate = view.askDueDate(loanDate);
        String notes = view.askLoanNotes();
        loanService.lendToTeacher(resource, teacher, loanDate, dueDate, notes);
        view.showMessage("Préstamo registrado: " + resource.getCode() + " para "
                + teacher.getFullName() + ", devolver el " + dueDate + ".");
    }

    private void returnLoan() throws ValidationException, SQLException {
        Resource resource = findResourceFromUser();
        Loan loan = loanService.findOpenLoanOf(resource).orElseThrow(
                () -> new ValidationException("El recurso " + resource.getCode()
                        + " no tiene un préstamo abierto."));
        view.showLoanDetail(loan);
        String notes = view.askReturnNotes();
        if (view.confirm("¿Registrar la devolución de " + resource.getCode() + "?")) {
            long daysLate = loanService.returnLoan(loan, notes);
            view.showMessage("Devolución registrada. El recurso quedó disponible.");
            if (daysLate > 0) {
                view.showMessage("Se devolvió con " + daysLate + " días de retraso.");
            }
        }
    }

    private void showOpenLoans() throws SQLException {
        view.showOpenLoans(loanService.listOpenLoans());
    }

    private void showBorrowerHistory() throws ValidationException, SQLException {
        if (view.askBorrowerType() == BorrowerType.STUDENT) {
            Student student = askForStudent();
            view.showBorrowerHistory(student.getFullName(), loanService.findLoansOf(student));
        } else {
            Teacher teacher = askForTeacher();
            view.showBorrowerHistory(teacher.getFullName(), loanService.findLoansOf(teacher));
        }
    }

    private void showOverdueReport() throws SQLException {
        view.showOverdueReport(loanService.listOverdueLoans());
    }

    // ---------- Lookups ----------

    private Resource findResourceFromUser() throws ValidationException, SQLException {
        String code = view.askResourceCode();
        return resourceService.findByCode(code).orElseThrow(
                () -> new ValidationException("No se encontró ningún recurso con ese código."));
    }

    private Student askForStudent() throws ValidationException, SQLException {
        ResourceView.DocumentId documentId = view.askStudentDocument();
        return studentService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún estudiante con ese documento."));
    }

    private Teacher askForTeacher() throws ValidationException, SQLException {
        ResourceView.DocumentId documentId = view.askTeacherDocument();
        return teacherService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún profesor con ese documento."));
    }
}