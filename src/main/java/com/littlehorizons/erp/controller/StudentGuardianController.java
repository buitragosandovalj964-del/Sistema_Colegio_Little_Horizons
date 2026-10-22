package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentGuardian;
import com.littlehorizons.erp.service.GuardianService;
import com.littlehorizons.erp.service.StudentGuardianService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.GuardianView;
import com.littlehorizons.erp.view.StudentGuardianView;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

/**
 * Coordinates the links between students and guardians.
 */
public class StudentGuardianController {

    public record GuardianLinkPlan(Guardian guardian, boolean registerGuardian,
            boolean primary, boolean emergencyContact) { }

    private final StudentGuardianService linkService;
    private final StudentService studentService;
    private final GuardianService guardianService;
    private final StudentGuardianView view;
    private final GuardianView guardianView;

    public StudentGuardianController(StudentGuardianService linkService,
            StudentService studentService, GuardianService guardianService,
            StudentGuardianView view, GuardianView guardianView) {
        this.linkService = linkService;
        this.studentService = studentService;
        this.guardianService = guardianService;
        this.view = view;
        this.guardianView = guardianView;
    }

    private interface Operation {
        void run() throws ValidationException, SQLException;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = view.showMenuAndReadOption();
            switch (option) {
                case 1 -> execute(this::showGuardiansOfAskedStudent);
                case 2 -> execute(this::linkGuardianToAskedStudent);
                case 3 -> execute(this::unlinkGuardian);
                case 4 -> execute(this::changeFlags);
                case 5 -> execute(this::showStudentsOfAskedGuardian);
                case 0 -> running = false;
                default -> view.showError("Opción inválida.");
            }
        }
    }

    private void execute(Operation operation) {
        try {
            operation.run();
        } catch (ConsoleInput.CancelledException e) {
            view.showMessage(
                    "Operación actual cancelada. Los cambios completados anteriormente se conservan.");
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Student-guardian operation failed", e);
            view.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    /**
     * Used by the student module to show the guardians in a student's detail.
     */
    public void showGuardiansOf(Student student) throws SQLException {
        view.showGuardiansOfStudent(student, linkService.findGuardiansOf(student.getId()));
    }

    /**
     * Used by the student module to mark students that still have no guardian.
     */
    public Set<Integer> findStudentIdsWithoutGuardian() throws SQLException {
        return linkService.findStudentIdsWithoutGuardian();
    }

    /**
     * Links one guardian after another until the user says no.
     */
    public void linkGuardiansInteractively(Student student)
            throws ValidationException, SQLException {
        do {
            linkOneGuardian(student);
        } while (view.confirm("¿Desea vincular otro acudiente a este estudiante?"));
    }

    public GuardianLinkPlan prepareFirstGuardianLink() throws ValidationException, SQLException {
        Guardian guardian = prepareGuardian();
        boolean registerGuardian = guardian.getId() == null;
        boolean emergencyContact = view.askIsEmergencyContact();
        return new GuardianLinkPlan(guardian, registerGuardian, true, emergencyContact);
    }

    public void registerStudentWithFirstGuardian(Student student, GuardianLinkPlan plan)
            throws ValidationException, SQLException {
        studentService.validateForRegistration(student);
        if (plan.registerGuardian()) {
            guardianService.validateForRegistration(plan.guardian());
        }
        linkService.registerStudentWithGuardian(student, plan.guardian(),
                plan.registerGuardian(), plan.primary(), plan.emergencyContact());
    }

    private void showGuardiansOfAskedStudent() throws ValidationException, SQLException {
        showGuardiansOf(askForStudent());
    }

    private void linkGuardianToAskedStudent() throws ValidationException, SQLException {
        linkGuardiansInteractively(askForStudent());
    }

    private void linkOneGuardian(Student student) throws ValidationException, SQLException {
        Guardian guardian = prepareGuardian();
        boolean registerGuardian = guardian.getId() == null;
        boolean primary = view.askIsPrimary();
        boolean emergencyContact = view.askIsEmergencyContact();
        GuardianLinkPlan plan = new GuardianLinkPlan(guardian, registerGuardian,
                primary, emergencyContact);
        if (plan.registerGuardian()) {
            guardianService.validateForRegistration(plan.guardian());
            linkService.registerNewGuardianLink(student, plan.guardian(),
                    plan.primary(), plan.emergencyContact());
        } else {
            linkService.linkGuardian(student, plan.guardian(),
                    plan.primary(), plan.emergencyContact());
        }
        view.showMessage(guardian.getFullName() + " quedó vinculado(a) a "
                + student.getFullName() + ".");
        showGuardiansOf(student);
    }

    private void unlinkGuardian() throws ValidationException, SQLException {
        Student student = askForStudent();
        showGuardiansOf(student);
        Guardian guardian = askForGuardian();
        if (view.confirm("¿Desvincular a " + guardian.getFullName() + " de "
                + student.getFullName() + "?")) {
            linkService.unlinkGuardian(student, guardian);
            view.showMessage("Acudiente desvinculado.");
            showGuardiansOf(student);
        }
    }

    private void changeFlags() throws ValidationException, SQLException {
        Student student = askForStudent();
        showGuardiansOf(student);
        Guardian guardian = askForGuardian();
        StudentGuardian current = linkService.getLink(student, guardian);
        boolean primary = view.askPrimaryKeepingCurrent(current.primary());
        boolean emergencyContact = view.askEmergencyKeepingCurrent(current.emergencyContact());
        linkService.changeFlags(student, guardian, primary, emergencyContact);
        view.showMessage("Vínculo actualizado.");
        showGuardiansOf(student);
    }

    private void showStudentsOfAskedGuardian() throws ValidationException, SQLException {
        Guardian guardian = askForGuardian();
        view.showStudentsOfGuardian(guardian, linkService.findStudentsOf(guardian.getId()));
    }

    private Student askForStudent() throws ValidationException, SQLException {
        StudentGuardianView.DocumentId documentId = view.askStudentDocument();
        return studentService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún estudiante con ese documento."));
    }

    private Guardian askForGuardian() throws ValidationException, SQLException {
        StudentGuardianView.DocumentId documentId = view.askGuardianDocument();
        return guardianService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún acudiente con ese documento."));
    }

    private Guardian prepareGuardian() throws ValidationException, SQLException {
        StudentGuardianView.DocumentId documentId = view.askGuardianDocument();
        Optional<Guardian> existing =
                guardianService.findByDocumentNumber(documentId.number());
        if (existing.isPresent()) {
            return existing.get();
        }
        if (!view.confirm("No existe un acudiente con ese documento. "
                + "¿Desea registrarlo ahora?")) {
            throw new ValidationException("No se vinculó ningún acudiente.");
        }
        return guardianView.askNewGuardian(documentId.type(), documentId.number());
    }
}