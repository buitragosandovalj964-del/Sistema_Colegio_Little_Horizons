package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.ClubService;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.time.LocalDate;
import java.util.List;

/**
 * Console screens for clubs and memberships. Everything shown to the user is in Spanish.
 */
public class ClubView {

    /**
     * Identifies a person by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private static final String SEPARATOR = "-".repeat(78);

    private final ConsoleInput input;

    public ClubView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("           CLUBES");
        System.out.println("==============================");
        System.out.println("1. Registrar club");
        System.out.println("2. Listar clubes (con cupos)");
        System.out.println("3. Ver detalle de un club y sus miembros");
        System.out.println("4. Actualizar club");
        System.out.println("5. Activar / desactivar club");
        System.out.println("6. Inscribir estudiante en un club");
        System.out.println("7. Retirar estudiante de un club");
        System.out.println("8. Ver clubes de un estudiante");
        System.out.println("9. Eliminar club (registros creados por error)");
        System.out.println("0. Volver al menú principal");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public Club askNewClub() {
        showMessage("Escriba '" + ConsoleInput.CANCEL_WORD
                + "' en cualquier momento para volver al menú.");
        Club club = new Club();
        club.setName(input.readRequiredText("Nombre del club: ", ClubService.MAX_NAME_LENGTH));
        club.setDescription(input.readOptionalText("Descripción"));
        club.setMaxCapacity(input.readIntKeepingCurrent("Cupo máximo",
                ClubService.DEFAULT_CAPACITY, ClubService.MIN_CAPACITY,
                ClubService.MAX_CAPACITY));
        club.setMeetingSchedule(input.readOptionalText("Horario de reuniones (ej: Martes 3-5 PM)"));
        return club;
    }

    public void askClubChanges(Club club) {
        showMessage("Presione Enter para conservar el valor actual. Escriba '"
                + ConsoleInput.CANCEL_WORD + "' para volver al menú.");
        club.setName(input.readTextKeepingCurrent("Nombre", club.getName()));
        club.setDescription(input.readTextKeepingCurrent("Descripción", club.getDescription()));
        club.setMaxCapacity(input.readIntKeepingCurrent("Cupo máximo", club.getMaxCapacity(),
                ClubService.MIN_CAPACITY, ClubService.MAX_CAPACITY));
        club.setMeetingSchedule(input.readTextKeepingCurrent(
                "Horario de reuniones", club.getMeetingSchedule()));
    }

    public String askClubName() {
        return input.readRequiredText("Nombre del club: ", ClubService.MAX_NAME_LENGTH);
    }

    public boolean askWantsSupervisor() {
        return input.readYesNo("¿Asignar un profesor encargado?");
    }

    public boolean askChangeSupervisor() {
        return input.readYesNo("¿Cambiar el profesor encargado?");
    }

    public DocumentId askTeacherDocument() {
        String type = input.readChoice("Documento del profesor - tipo",
                TeacherService.DOCUMENT_TYPES);
        String number = input.readRequiredText("Documento del profesor - número: ",
                FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public DocumentId askStudentDocument() {
        String type = input.readChoice("Documento del estudiante - tipo",
                StudentService.DOCUMENT_TYPES);
        String number = input.readRequiredText("Documento del estudiante - número: ",
                FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public LocalDate askJoinDate() {
        return input.readDateKeepingCurrent("Fecha de ingreso", LocalDate.now());
    }

    public String askWithdrawalReason() {
        return input.readOptionalText("Motivo del retiro");
    }

    public boolean askActiveState() {
        return input.readYesNo("¿El club debe quedar activo?");
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

    public void showClubList(List<Club> clubs) {
        if (clubs.isEmpty()) {
            showMessage("No hay clubes registrados.");
            return;
        }
        System.out.printf("%-5s %-28s %-8s %-8s %-24s %-22s%n",
                "ID", "NOMBRE", "CUPO", "ACTIVO", "ENCARGADO", "HORARIO");
        clubs.forEach(club -> System.out.printf("%-5d %-28s %-8s %-8s %-24s %-22s%n",
                club.getId(), club.getName(),
                club.getActiveMembers() + "/" + club.getMaxCapacity(),
                club.isActive() ? "Sí" : "No", supervisorName(club),
                club.getMeetingSchedule() == null ? "-" : club.getMeetingSchedule()));
    }

    public void showClubDetail(Club club, List<ClubMembership> members) {
        System.out.println(SEPARATOR);
        System.out.println("Club: " + club.getName() + (club.isActive() ? "" : " (INACTIVO)"));
        System.out.println("Descripción: " + (club.getDescription() == null ? "-" : club.getDescription()));
        System.out.println("Encargado: " + supervisorName(club));
        System.out.println("Horario: " + (club.getMeetingSchedule() == null ? "-" : club.getMeetingSchedule()));
        System.out.println("Cupo: " + club.getActiveMembers() + " de " + club.getMaxCapacity()
                + " ocupados");
        System.out.println(SEPARATOR);
        if (members.isEmpty()) {
            showMessage("Este club todavía no tiene miembros.");
            return;
        }
        System.out.printf("%-38s %-12s %-12s %-12s%n",
                "ESTUDIANTE", "INGRESO", "ESTADO", "RETIRO");
        members.forEach(member -> System.out.printf("%-38s %-12s %-12s %-12s%n",
                member.getStudent().getFullName(), member.getJoinDate(), member.getStatus(),
                member.getWithdrawalDate() == null ? "-" : member.getWithdrawalDate()));
        System.out.println(SEPARATOR);
    }

    public void showClubsOfStudent(Student student, List<ClubMembership> memberships) {
        System.out.println(SEPARATOR);
        System.out.println("Clubes de " + student.getFullName());
        System.out.println(SEPARATOR);
        if (memberships.isEmpty()) {
            showMessage("Este estudiante no pertenece ni perteneció a ningún club.");
            return;
        }
        System.out.printf("%-28s %-12s %-12s %-12s%n", "CLUB", "INGRESO", "ESTADO", "RETIRO");
        memberships.forEach(membership -> System.out.printf("%-28s %-12s %-12s %-12s%n",
                membership.getClub() == null ? "-" : membership.getClub().getName(),
                membership.getJoinDate(), membership.getStatus(),
                membership.getWithdrawalDate() == null ? "-" : membership.getWithdrawalDate()));
        System.out.println(SEPARATOR);
    }

    private String supervisorName(Club club) {
        return club.getSupervisor() == null ? "Sin asignar" : club.getSupervisor().getFullName();
    }
}