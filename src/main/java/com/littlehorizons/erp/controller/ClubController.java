package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.ClubService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ClubView;
import java.sql.SQLException;

/**
 * Coordinates the clubs menu: asks the view for data, calls the service and
 * asks the view to show the result.
 */
public class ClubController {

    private final ClubService clubService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final ClubView view;

    public ClubController(ClubService clubService, StudentService studentService,
            TeacherService teacherService, ClubView view) {
        this.clubService = clubService;
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
                case 1 -> execute(this::registerClub);
                case 2 -> execute(this::listClubs);
                case 3 -> execute(this::showClubDetail);
                case 4 -> execute(this::updateClub);
                case 5 -> execute(this::changeActiveState);
                case 6 -> execute(this::enrollStudent);
                case 7 -> execute(this::withdrawStudent);
                case 8 -> execute(this::showClubsOfStudent);
                case 9 -> execute(this::deleteClub);
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
            ErrorLogger.log("Club operation failed", e);
            view.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void registerClub() throws ValidationException, SQLException {
        Club club = view.askNewClub();
        if (view.askWantsSupervisor()) {
            club.setSupervisor(askForTeacher());
        }
        clubService.registerClub(club);
        view.showMessage("Club registrado con id " + club.getId() + ".");
    }

    private void listClubs() throws SQLException {
        view.showClubList(clubService.listClubs());
    }

    private void showClubDetail() throws ValidationException, SQLException {
        Club club = findClubFromUser();
        view.showClubDetail(club, clubService.findMembers(club));
    }

    private void updateClub() throws ValidationException, SQLException {
        Club club = findClubFromUser();
        view.askClubChanges(club);
        if (view.askChangeSupervisor()) {
            club.setSupervisor(askForTeacher());
        }
        clubService.updateClub(club);
        view.showMessage("Club actualizado.");
    }

    private void changeActiveState() throws ValidationException, SQLException {
        Club club = findClubFromUser();
        view.showMessage("Estado actual de '" + club.getName() + "': "
                + (club.isActive() ? "activo" : "inactivo"));
        boolean active = view.askActiveState();
        clubService.setActive(club.getId(), active);
        view.showMessage(active ? "Club activado." : "Club desactivado.");
    }

    private void enrollStudent() throws ValidationException, SQLException {
        Club club = findClubFromUser();
        Student student = askForStudent();
        clubService.enrollStudent(club, student, view.askJoinDate());
        view.showMessage(student.getFullName() + " quedó inscrito en '" + club.getName() + "'.");
    }

    private void withdrawStudent() throws ValidationException, SQLException {
        Club club = findClubFromUser();
        Student student = askForStudent();
        String reason = view.askWithdrawalReason();
        clubService.withdrawStudent(club, student, reason);
        view.showMessage(student.getFullName() + " fue retirado de '" + club.getName() + "'.");
    }

    private void showClubsOfStudent() throws ValidationException, SQLException {
        Student student = askForStudent();
        view.showClubsOfStudent(student, clubService.findMembershipsOf(student));
    }

    private void deleteClub() throws ValidationException, SQLException {
        Club club = findClubFromUser();
        view.showMessage("ADVERTENCIA: eliminar también borra todas las membresías de este "
                + "club. Para clubes reales use 'Activar / desactivar'.");
        if (view.confirm("¿Eliminar definitivamente el club '" + club.getName() + "'?")) {
            clubService.deleteClub(club.getId());
            view.showMessage("Club eliminado.");
        }
    }

    private Club findClubFromUser() throws ValidationException, SQLException {
        String name = view.askClubName();
        return clubService.findByName(name).orElseThrow(
                () -> new ValidationException("No se encontró ningún club con ese nombre."));
    }

    private Teacher askForTeacher() throws ValidationException, SQLException {
        ClubView.DocumentId documentId = view.askTeacherDocument();
        return teacherService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún profesor con ese documento."));
    }

    private Student askForStudent() throws ValidationException, SQLException {
        ClubView.DocumentId documentId = view.askStudentDocument();
        return studentService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún estudiante con ese documento."));
    }
}