package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Event;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.EventService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.EventView;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Coordinates the events menu: asks the view for data, calls the service and
 * asks the view to show the result.
 */
public class EventController {

    private final EventService eventService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final EventView view;

    public EventController(EventService eventService, StudentService studentService,
            TeacherService teacherService, EventView view) {
        this.eventService = eventService;
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
                case 1 -> execute(this::registerEvent);
                case 2 -> execute(this::listEvents);
                case 3 -> execute(this::showEventDetail);
                case 4 -> execute(this::updateEvent);
                case 5 -> execute(this::changeActiveState);
                case 6 -> execute(this::registerParticipant);
                case 7 -> execute(this::removeParticipant);
                case 8 -> execute(this::recordResults);
                case 9 -> execute(this::showEventsOfStudent);
                case 10 -> execute(this::deleteEvent);
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
            ErrorLogger.log("Event operation failed", e);
            view.showError(
                    "Ocurrió un error en la base de datos. Revise error_log.txt para más detalles.");
        }
    }

    private void registerEvent() throws ValidationException, SQLException {
        Event event = view.askNewEvent();
        if (view.askWantsOrganizer()) {
            event.setOrganizer(askForTeacher());
        }
        eventService.registerEvent(event);
        view.showMessage("Evento registrado con id " + event.getId() + ".");
    }

    private void listEvents() throws SQLException {
        view.showEventList(eventService.listEvents());
    }

    private void showEventDetail() throws ValidationException, SQLException {
        Event event = findEventFromUser();
        view.showEventDetail(event, eventService.findParticipants(event));
    }

    private void updateEvent() throws ValidationException, SQLException {
        Event event = findEventFromUser();
        view.askEventChanges(event);
        if (view.askChangeOrganizer()) {
            event.setOrganizer(askForTeacher());
        }
        eventService.updateEvent(event);
        view.showMessage("Evento actualizado.");
    }

    private void changeActiveState() throws ValidationException, SQLException {
        Event event = findEventFromUser();
        view.showMessage("Estado actual de '" + event.getName() + "': "
                + (event.isActive() ? "activo" : "inactivo"));
        boolean active = view.askActiveState();
        eventService.setActive(event.getId(), active);
        view.showMessage(active ? "Evento activado." : "Evento desactivado.");
    }

    private void registerParticipant() throws ValidationException, SQLException {
        Event event = findEventFromUser();
        Student student = askForStudent();
        String role = view.askRole();
        String notes = view.askNotes();
        eventService.registerParticipant(event, student, role, notes);
        view.showMessage(student.getFullName() + " quedó registrado en '" + event.getName() + "'.");
    }

    private void removeParticipant() throws ValidationException, SQLException {
        Event event = findEventFromUser();
        Student student = askForStudent();
        if (view.confirm("¿Quitar a " + student.getFullName() + " de '" + event.getName() + "'?")) {
            eventService.removeParticipant(event, student);
            view.showMessage("Participante retirado del evento.");
        }
    }

    private void recordResults() throws ValidationException, SQLException {
        Event event = findEventFromUser();
        List<EventParticipation> competitors = eventService.findCompetitors(event);
        Map<Integer, String> positions = view.askResults(event, competitors);
        eventService.saveResults(event, positions);
        view.showMessage("Resultados guardados.");
        view.showEventDetail(event, eventService.findParticipants(event));
    }

    private void showEventsOfStudent() throws ValidationException, SQLException {
        Student student = askForStudent();
        view.showEventsOfStudent(student, eventService.findParticipationsOf(student));
    }

    private void deleteEvent() throws ValidationException, SQLException {
        Event event = findEventFromUser();
        view.showMessage("ADVERTENCIA: eliminar también borra todos los participantes de este "
                + "evento. Para eventos cancelados use 'Activar / desactivar'.");
        if (view.confirm("¿Eliminar definitivamente el evento '" + event.getName() + "'?")) {
            eventService.deleteEvent(event.getId());
            view.showMessage("Evento eliminado.");
        }
    }

    private Event findEventFromUser() throws ValidationException, SQLException {
        return eventService.findById(view.askEventId());
    }

    private Teacher askForTeacher() throws ValidationException, SQLException {
        EventView.DocumentId documentId = view.askTeacherDocument();
        return teacherService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún profesor con ese documento."));
    }

    private Student askForStudent() throws ValidationException, SQLException {
        EventView.DocumentId documentId = view.askStudentDocument();
        return studentService.findByDocument(documentId.type(), documentId.number()).orElseThrow(
                () -> new ValidationException("No se encontró ningún estudiante con ese documento."));
    }
}