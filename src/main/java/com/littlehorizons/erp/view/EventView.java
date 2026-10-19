package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Event;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.EventService;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Console screens for events and participants. Everything shown to the user is in Spanish.
 */
public class EventView {

    /**
     * Identifies a person by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private static final String POSITIVE_NUMBER_PATTERN = "[1-9]\\d{0,8}";
    private static final String SEPARATOR = "-".repeat(78);

    private final ConsoleInput input;

    public EventView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("           EVENTOS");
        System.out.println("==============================");
        System.out.println("1. Registrar evento");
        System.out.println("2. Listar eventos (calendario)");
        System.out.println("3. Ver detalle de un evento y sus participantes");
        System.out.println("4. Actualizar evento");
        System.out.println("5. Activar / desactivar (cancelar) evento");
        System.out.println("6. Registrar participante en un evento");
        System.out.println("7. Quitar participante de un evento");
        System.out.println("8. Registrar resultados de la competencia");
        System.out.println("9. Ver eventos de un estudiante");
        System.out.println("10. Eliminar evento (registros creados por error)");
        System.out.println("0. Volver");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public Event askNewEvent() {
        showMessage("Escriba '" + ConsoleInput.CANCEL_WORD
                + "' en cualquier momento para volver al menú.");
        Event event = new Event();
        event.setName(input.readRequiredText("Nombre del evento: ", EventService.MAX_NAME_LENGTH));
        event.setType(input.readChoice("Tipo de evento", EventService.TYPES));
        event.setDate(input.readDate("Fecha del evento"));
        event.setPlace(input.readOptionalText("Lugar"));
        event.setDescription(input.readOptionalText("Descripción"));
        event.setHasCompetition(input.readYesNo("¿Tiene competencia con posiciones?"));
        return event;
    }

    public void askEventChanges(Event event) {
        showMessage("Presione Enter para conservar el valor actual. Escriba '"
                + ConsoleInput.CANCEL_WORD + "' para volver al menú.");
        event.setName(input.readTextKeepingCurrent("Nombre", event.getName()));
        event.setType(input.readChoiceKeepingCurrent("Tipo", event.getType(), EventService.TYPES));
        event.setDate(input.readDateKeepingCurrent("Fecha", event.getDate()));
        event.setPlace(input.readTextKeepingCurrent("Lugar", event.getPlace()));
        event.setDescription(input.readTextKeepingCurrent("Descripción", event.getDescription()));
        event.setHasCompetition(input.readYesNoKeepingCurrent(
                "¿Tiene competencia con posiciones?", event.isHasCompetition()));
    }

    public int askEventId() {
        String text = input.readRequiredPattern("Número del evento (columna ID de la lista): ",
                POSITIVE_NUMBER_PATTERN, "Ingrese solo el número del evento, por ejemplo 3.");
        return Integer.parseInt(text);
    }

    public boolean askWantsOrganizer() {
        return input.readYesNo("¿Asignar un profesor organizador?");
    }

    public boolean askChangeOrganizer() {
        return input.readYesNo("¿Cambiar el profesor organizador?");
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

    /**
     * Role of a participant. Pressing Enter keeps "Participante".
     */
    public String askRole() {
        return input.readChoiceKeepingCurrent("Rol", EventService.ROLE_PARTICIPANT,
                EventService.ROLES);
    }

    public String askNotes() {
        return input.readOptionalText("Observaciones");
    }

    public boolean askActiveState() {
        return input.readYesNo("¿El evento debe quedar activo?");
    }

    /**
     * Asks the position of each competitor. Pressing Enter keeps the current position,
     * or "Participante" if none was recorded yet.
     */
    public Map<Integer, String> askResults(Event event, List<EventParticipation> competitors) {
        System.out.println(SEPARATOR);
        System.out.println("Resultados de '" + event.getName() + "' - " + competitors.size()
                + " competidores");
        System.out.println(SEPARATOR);
        Map<Integer, String> positions = new LinkedHashMap<>();
        for (int index = 0; index < competitors.size(); index++) {
            EventParticipation competitor = competitors.get(index);
            String current = competitor.getPosition() == null
                    ? EventService.ROLE_PARTICIPANT : competitor.getPosition();
            String position = input.readChoiceKeepingCurrent(
                    (index + 1) + "/" + competitors.size() + " "
                            + competitor.getStudent().getFullName(),
                    current, EventService.POSITIONS);
            positions.put(competitor.getId(), position);
        }
        return positions;
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

    public void showEventList(List<Event> events) {
        if (events.isEmpty()) {
            showMessage("No hay eventos registrados.");
            return;
        }
        System.out.printf("%-5s %-12s %-30s %-12s %-12s %-8s %-8s%n",
                "ID", "FECHA", "NOMBRE", "TIPO", "COMPETENCIA", "PARTIC.", "ACTIVO");
        events.forEach(event -> System.out.printf("%-5d %-12s %-30s %-12s %-12s %-8d %-8s%n",
                event.getId(), event.getDate(), event.getName(), event.getType(),
                event.isHasCompetition() ? "Sí" : "No", event.getParticipantCount(),
                event.isActive() ? "Sí" : "No"));
    }

    public void showEventDetail(Event event, List<EventParticipation> participants) {
        System.out.println(SEPARATOR);
        System.out.println("Evento: " + event.getName() + (event.isActive() ? "" : " (INACTIVO)"));
        System.out.println("Tipo: " + event.getType() + " | Fecha: " + event.getDate()
                + " | Lugar: " + valueOrDash(event.getPlace()));
        System.out.println("Descripción: " + valueOrDash(event.getDescription()));
        System.out.println("Organizador: " + (event.getOrganizer() == null ? "Sin asignar"
                : event.getOrganizer().getFullName()));
        System.out.println("Competencia: " + (event.isHasCompetition() ? "Sí" : "No"));
        System.out.println(SEPARATOR);
        if (participants.isEmpty()) {
            showMessage("Este evento todavía no tiene participantes.");
            return;
        }
        System.out.printf("%-38s %-14s %-12s %-20s%n",
                "ESTUDIANTE", "ROL", "POSICIÓN", "OBSERVACIONES");
        participants.forEach(participation -> System.out.printf("%-38s %-14s %-12s %-20s%n",
                participation.getStudent().getFullName(), participation.getRole(),
                valueOrDash(participation.getPosition()), valueOrDash(participation.getNotes())));
        System.out.println(SEPARATOR);
        System.out.println("Total de participantes: " + participants.size());
    }

    public void showEventsOfStudent(Student student, List<EventParticipation> participations) {
        System.out.println(SEPARATOR);
        System.out.println("Eventos de " + student.getFullName());
        System.out.println(SEPARATOR);
        if (participations.isEmpty()) {
            showMessage("Este estudiante no ha participado en ningún evento.");
            return;
        }
        System.out.printf("%-12s %-30s %-12s %-14s %-12s%n",
                "FECHA", "EVENTO", "TIPO", "ROL", "POSICIÓN");
        participations.forEach(participation -> System.out.printf(
                "%-12s %-30s %-12s %-14s %-12s%n",
                participation.getEvent() == null ? "-" : participation.getEvent().getDate(),
                participation.getEvent() == null ? "-" : participation.getEvent().getName(),
                participation.getEvent() == null ? "-" : participation.getEvent().getType(),
                participation.getRole(), valueOrDash(participation.getPosition())));
        System.out.println(SEPARATOR);
    }

    private String valueOrDash(String value) {
        return value == null ? "-" : value;
    }
}