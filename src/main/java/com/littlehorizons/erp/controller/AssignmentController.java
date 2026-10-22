package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.AssignmentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.AssignmentView;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;

/**
 * Coordinates teacher assignments and class schedules.
 */
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final TeacherService teacherService;
    private final SubjectService subjectService;
    private final AssignmentView view;

    public AssignmentController(AssignmentService assignmentService,
            TeacherService teacherService, SubjectService subjectService,
            AssignmentView view) {
        this.assignmentService = assignmentService;
        this.teacherService = teacherService;
        this.subjectService = subjectService;
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
                case 1 -> execute(this::assignSubject);
                case 2 -> execute(this::showAssignments);
                case 3 -> execute(this::removeAssignment);
                case 4 -> execute(this::addSlot);
                case 5 -> execute(this::removeSlot);
                case 6 -> execute(this::showWeeklySchedule);
                case 0 -> running = false;
                default -> view.showError("Opción inválida.");
            }
        }
    }

    private void execute(Operation operation) {
        try {
            operation.run();
        } catch (ConsoleInput.CancelledException e) {
            view.showMessage("Operación cancelada. Los cambios completados anteriormente se conservan.");
        } catch (ValidationException e) {
            view.showError(e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Assignment operation failed", e);
            view.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void assignSubject() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        Subject subject = askForSubject();
        int academicYear = view.askAcademicYear();
        String section = view.askSection();
        assignmentService.assignSubject(teacher, subject, academicYear, section);
        view.showMessage("Materia asignada.");
        showAssignmentsOf(teacher);
    }

    private void showAssignments() throws ValidationException, SQLException {
        showAssignmentsOf(askForTeacher());
    }

    private void removeAssignment() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        showAssignmentsOf(teacher);
        int assignmentId = view.askAssignmentId();
        view.showMessage("ADVERTENCIA: quitar la asignación también borra sus franjas horarias.");
        if (view.confirm("¿Quitar la asignación " + assignmentId + "?")) {
            assignmentService.removeAssignment(teacher, assignmentId);
            view.showMessage("Asignación eliminada.");
            showAssignmentsOf(teacher);
        }
    }

    private void addSlot() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        showAssignmentsOf(teacher);
        int assignmentId = view.askAssignmentId();
        String day = view.askDay();
        LocalTime startTime = view.askTime("Hora de inicio");
        LocalTime endTime = view.askTime("Hora de fin");
        assignmentService.addSlot(teacher, assignmentId, day, startTime, endTime);
        view.showMessage("Franja horaria agregada.");
        showAssignmentsOf(teacher);
    }

    private void removeSlot() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        showAssignmentsOf(teacher);
        int assignmentId = view.askAssignmentId();
        int slotId = view.askSlotId();
        if (view.confirm("¿Quitar la franja " + slotId + "?")) {
            assignmentService.removeSlot(teacher, assignmentId, slotId);
            view.showMessage("Franja eliminada.");
            showAssignmentsOf(teacher);
        }
    }

    private void showWeeklySchedule() throws ValidationException, SQLException {
        Teacher teacher = askForTeacher();
        int academicYear = view.askAcademicYear();
        List<Assignment> assignments = assignmentService.findAssignmentsOf(teacher.getId());
        view.showWeeklySchedule(teacher, academicYear, assignments);
    }

    private void showAssignmentsOf(Teacher teacher) throws SQLException {
        view.showAssignments(teacher, assignmentService.findAssignmentsOf(teacher.getId()));
    }

    private Teacher askForTeacher() throws ValidationException, SQLException {
        AssignmentView.DocumentId documentId = view.askTeacherDocument();
        return teacherService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún profesor con ese documento."));
    }

    private Subject askForSubject() throws ValidationException, SQLException {
        String code = view.askSubjectCode();
        return subjectService.findByCode(code).orElseThrow(
                () -> new ValidationException("No se encontró ninguna materia con ese código."));
    }
}