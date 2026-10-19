package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.EventParticipationDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Event;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Business rules for events, participants and competition results.
 * User-facing messages are in Spanish.
 */
public class EventService {

    public static final List<String> TYPES =
            List.of("Académico", "Cultural", "Deportivo", "Social", "Científico");
    public static final String ROLE_PARTICIPANT = "Participante";
    public static final String ROLE_COMPETITOR = "Competidor";
    public static final List<String> ROLES =
            List.of(ROLE_PARTICIPANT, ROLE_COMPETITOR, "Organizador", "Asistente");
    public static final List<String> POSITIONS =
            List.of("1er Lugar", "2do Lugar", "3er Lugar", "Participante");
    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_PLACE_LENGTH = 150;

    private static final int MAX_TEXT_LENGTH = 500;
    private static final String STUDENT_ACTIVE = "Activo";
    private static final String TEACHER_ACTIVE = "Activo";

    private final EventDao eventDao;
    private final EventParticipationDao participationDao;
    private final TeacherDao teacherDao;
    private final StudentDao studentDao;

    public EventService() {
        this(new EventDao(), new EventParticipationDao(), new TeacherDao(), new StudentDao());
    }

    public EventService(EventDao eventDao, EventParticipationDao participationDao,
            TeacherDao teacherDao, StudentDao studentDao) {
        this.eventDao = eventDao;
        this.participationDao = participationDao;
        this.teacherDao = teacherDao;
        this.studentDao = studentDao;
    }

    // ---------- Events ----------

    public void registerEvent(Event event) throws ValidationException, SQLException {
        validate(event);
        event.setName(capitalizeFirst(event.getName()));
        if (eventDao.findByNameAndDate(event.getName(), event.getDate()).isPresent()) {
            throw new ValidationException("Ya existe un evento llamado '" + event.getName()
                    + "' el " + event.getDate() + ".");
        }
        eventDao.insert(event);
    }

    public void updateEvent(Event event) throws ValidationException, SQLException {
        if (event.getId() == null) {
            throw new ValidationException(
                    "El evento no tiene id, por lo que no se puede actualizar.");
        }
        validate(event);
        event.setName(capitalizeFirst(event.getName()));
        Optional<EventDao.EventRow> sameEvent =
                eventDao.findByNameAndDate(event.getName(), event.getDate());
        if (sameEvent.isPresent() && sameEvent.get().id() != event.getId()) {
            throw new ValidationException("Ya existe otro evento llamado '" + event.getName()
                    + "' el " + event.getDate() + ".");
        }
        if (!event.isHasCompetition() && !findCompetitorsOf(event.getId()).isEmpty()) {
            throw new ValidationException("No se puede quitar la competencia: el evento ya "
                    + "tiene competidores registrados.");
        }
        if (!eventDao.update(event)) {
            throw new ValidationException("No se encontró el evento con id " + event.getId() + ".");
        }
    }

    public void setActive(int eventId, boolean active) throws ValidationException, SQLException {
        Event event = findById(eventId);
        event.setActive(active);
        eventDao.update(event);
    }

    public void deleteEvent(int eventId) throws ValidationException, SQLException {
        if (!eventDao.deleteById(eventId)) {
            throw new ValidationException("No se encontró el evento con id " + eventId + ".");
        }
    }

    public Event findById(int eventId) throws ValidationException, SQLException {
        EventDao.EventRow row = eventDao.findById(eventId).orElseThrow(
                () -> new ValidationException("No se encontró el evento con id " + eventId + "."));
        return toEvent(row);
    }

    public List<Event> listEvents() throws SQLException {
        List<Event> events = new ArrayList<>();
        for (EventDao.EventRow row : eventDao.findAll()) {
            events.add(toEvent(row));
        }
        return events;
    }

    // ---------- Participants ----------

    public void registerParticipant(Event event, Student student, String role, String notes)
            throws ValidationException, SQLException {
        Event current = findById(event.getId());
        if (!current.isActive()) {
            throw new ValidationException("El evento '" + current.getName() + "' está inactivo.");
        }
        if (!STUDENT_ACTIVE.equals(student.getStatus())) {
            throw new ValidationException("El estudiante " + student.getFullName()
                    + " está en estado " + student.getStatus()
                    + "; solo participan estudiantes activos.");
        }
        FieldValidator.requireOneOf(role, ROLES, "El rol");
        FieldValidator.validateMaxLength(notes, "Las observaciones", MAX_TEXT_LENGTH);
        if (ROLE_COMPETITOR.equals(role) && !current.isHasCompetition()) {
            throw new ValidationException("El evento '" + current.getName()
                    + "' no tiene competencia; use otro rol.");
        }
        if (participationDao.find(current.getId(), student.getId()).isPresent()) {
            throw new ValidationException(student.getFullName()
                    + " ya está registrado en el evento '" + current.getName() + "'.");
        }
        participationDao.insert(current.getId(), student.getId(), role, notes);
    }

    public void removeParticipant(Event event, Student student)
            throws ValidationException, SQLException {
        if (!participationDao.delete(event.getId(), student.getId())) {
            throw new ValidationException(student.getFullName()
                    + " no está registrado en el evento '" + event.getName() + "'.");
        }
    }

    public List<EventParticipation> findParticipants(Event event) throws SQLException {
        List<EventParticipation> participations = new ArrayList<>();
        for (EventParticipationDao.ParticipationRow row
                : participationDao.findByEvent(event.getId())) {
            participations.add(toParticipation(row, event, null));
        }
        return participations;
    }

    public List<EventParticipation> findParticipationsOf(Student student)
            throws SQLException {
        List<EventParticipation> participations = new ArrayList<>();
        for (EventParticipationDao.ParticipationRow row
                : participationDao.findByStudent(student.getId())) {
            Optional<EventDao.EventRow> eventRow = eventDao.findById(row.eventId());
            Event event = eventRow.isPresent() ? toEvent(eventRow.get()) : null;
            participations.add(toParticipation(row, event, student));
        }
        return participations;
    }

    /**
     * Returns the competitors of an event with competition, so that their positions
     * can be recorded.
     */
    public List<EventParticipation> findCompetitors(Event event)
            throws ValidationException, SQLException {
        if (!event.isHasCompetition()) {
            throw new ValidationException("El evento '" + event.getName()
                    + "' no tiene competencia.");
        }
        List<EventParticipation> competitors = findCompetitorsOf(event.getId());
        if (competitors.isEmpty()) {
            throw new ValidationException("El evento '" + event.getName()
                    + "' todavía no tiene competidores registrados.");
        }
        return competitors;
    }

    /**
     * Saves the positions of the competitors of an event (participation id to position).
     */
    public void saveResults(Event event, Map<Integer, String> positionsByParticipationId)
            throws ValidationException, SQLException {
        List<EventParticipation> competitors = findCompetitors(event);
        for (Map.Entry<Integer, String> entry : positionsByParticipationId.entrySet()) {
            boolean isCompetitorOfEvent = competitors.stream()
                    .anyMatch(competitor -> competitor.getId().equals(entry.getKey()));
            if (!isCompetitorOfEvent) {
                throw new ValidationException("La participación " + entry.getKey()
                        + " no es de un competidor de este evento.");
            }
            FieldValidator.requireOneOf(entry.getValue(), POSITIONS, "La posición");
        }
        participationDao.updatePositions(positionsByParticipationId);
    }

    // ---------- Helpers ----------

    private List<EventParticipation> findCompetitorsOf(int eventId) throws SQLException {
        List<EventParticipation> competitors = new ArrayList<>();
        for (EventParticipationDao.ParticipationRow row : participationDao.findByEvent(eventId)) {
            if (ROLE_COMPETITOR.equals(row.role())) {
                competitors.add(toParticipation(row, null, null));
            }
        }
        return competitors;
    }

    private Event toEvent(EventDao.EventRow row) throws SQLException {
        Event event = new Event();
        event.setId(row.id());
        event.setName(row.name());
        event.setDescription(row.description());
        event.setType(row.type());
        event.setDate(row.date());
        event.setPlace(row.place());
        event.setHasCompetition(row.hasCompetition());
        event.setActive(row.active());
        event.setParticipantCount(participationDao.countByEvent(row.id()));
        if (row.organizerId() != null) {
            teacherDao.findById(row.organizerId()).ifPresent(event::setOrganizer);
        }
        return event;
    }

    private EventParticipation toParticipation(EventParticipationDao.ParticipationRow row,
            Event event, Student knownStudent) throws SQLException {
        EventParticipation participation = new EventParticipation();
        participation.setId(row.id());
        participation.setEvent(event);
        participation.setRole(row.role());
        participation.setPosition(row.position());
        participation.setNotes(row.notes());
        participation.setStudent(knownStudent != null ? knownStudent
                : studentDao.findById(row.studentId()).orElse(null));
        return participation;
    }

    private void validate(Event event) throws ValidationException {
        if (event.getName() == null || event.getName().isBlank()) {
            throw new ValidationException("El nombre del evento es obligatorio.");
        }
        FieldValidator.validateMaxLength(event.getName(), "El nombre del evento",
                MAX_NAME_LENGTH);
        FieldValidator.requireOneOf(event.getType(), TYPES, "El tipo de evento");
        validateDate(event.getDate());
        FieldValidator.validateMaxLength(event.getPlace(), "El lugar", MAX_PLACE_LENGTH);
        FieldValidator.validateMaxLength(event.getDescription(), "La descripción",
                MAX_TEXT_LENGTH);
        Teacher organizer = event.getOrganizer();
        if (organizer != null && !TEACHER_ACTIVE.equals(organizer.getStatus())) {
            throw new ValidationException("El profesor " + organizer.getFullName()
                    + " está en estado " + organizer.getStatus()
                    + "; el organizador debe estar activo.");
        }
    }

    private void validateDate(LocalDate date) throws ValidationException {
        if (date == null) {
            throw new ValidationException("La fecha del evento es obligatoria.");
        }
        if (date.getYear() < AssignmentService.minAcademicYear()
                || date.getYear() > AssignmentService.maxAcademicYear()) {
            throw new ValidationException("El año del evento debe estar entre "
                    + AssignmentService.minAcademicYear() + " y "
                    + AssignmentService.maxAcademicYear() + ".");
        }
    }

    private String capitalizeFirst(String text) {
        String trimmed = text.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1);
    }
}