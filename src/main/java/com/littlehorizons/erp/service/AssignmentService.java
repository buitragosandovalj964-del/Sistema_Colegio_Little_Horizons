package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.ScheduleDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for teacher assignments and their class schedules.
 * User-facing messages are in Spanish.
 */
public class AssignmentService {

    public static final List<String> DAYS =
            List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");
    public static final String SECTION_PATTERN = "[A-Za-z]";
    public static final LocalTime FIRST_HOUR = LocalTime.of(6, 0);
    public static final LocalTime LAST_HOUR = LocalTime.of(18, 0);

    private static final String ACTIVE_TEACHER_STATUS = "Activo";

    private final AssignmentDao assignmentDao;
    private final ScheduleDao scheduleDao;
    private final TeacherDao teacherDao;
    private final SubjectDao subjectDao;

    public AssignmentService() {
        this(new AssignmentDao(), new ScheduleDao(), new TeacherDao(), new SubjectDao());
    }

    public AssignmentService(AssignmentDao assignmentDao, ScheduleDao scheduleDao,
            TeacherDao teacherDao, SubjectDao subjectDao) {
        this.assignmentDao = assignmentDao;
        this.scheduleDao = scheduleDao;
        this.teacherDao = teacherDao;
        this.subjectDao = subjectDao;
    }

    public static int minAcademicYear() {
        return LocalDate.now().getYear() - 1;
    }

    public static int maxAcademicYear() {
        return LocalDate.now().getYear() + 1;
    }

    public void assignSubject(Teacher teacher, Subject subject, int academicYear, String section)
            throws ValidationException, SQLException {
        if (!ACTIVE_TEACHER_STATUS.equals(teacher.getStatus())) {
            throw new ValidationException("El profesor " + teacher.getFullName()
                    + " está en estado " + teacher.getStatus()
                    + "; solo se pueden asignar materias a profesores activos.");
        }
        if (!subject.isActive()) {
            throw new ValidationException("La materia " + subject.getCode() + " está inactiva.");
        }
        if (academicYear < minAcademicYear() || academicYear > maxAcademicYear()) {
            throw new ValidationException("El año académico debe estar entre "
                    + minAcademicYear() + " y " + maxAcademicYear() + ".");
        }
        if (section == null || !section.matches(SECTION_PATTERN)) {
            throw new ValidationException("La sección debe ser una letra (A, B, C...).");
        }
        String normalizedSection = section.toUpperCase();

        Optional<AssignmentDao.AssignmentRow> existing = assignmentDao.findBySubjectYearSection(
                subject.getId(), academicYear, normalizedSection);
        if (existing.isPresent()) {
            if (teacher.getId().equals(existing.get().teacherId())) {
                throw new ValidationException("La materia " + subject.getCode()
                        + " ya está asignada a " + teacher.getFullName() + " en la sección "
                        + normalizedSection + " del año " + academicYear + ".");
            }
            String otherTeacher = teacherDao.findById(existing.get().teacherId())
                    .map(Teacher::getFullName)
                    .orElse("otro profesor");
            throw new ValidationException("La materia " + subject.getCode() + " en la sección "
                    + normalizedSection + " del año " + academicYear + " ya la dicta "
                    + otherTeacher + ".");
        }
        assignmentDao.insert(teacher.getId(), subject.getId(), academicYear, normalizedSection);
    }

    public List<Assignment> findAssignmentsOf(int teacherId) throws SQLException {
        List<Assignment> assignments = new ArrayList<>();
        for (AssignmentDao.AssignmentRow row : assignmentDao.findByTeacherId(teacherId)) {
            assignments.add(toAssignment(row));
        }
        return assignments;
    }

    public void removeAssignment(Teacher teacher, int assignmentId)
            throws ValidationException, SQLException {
        getAssignmentOf(teacher, assignmentId);
        assignmentDao.deleteById(assignmentId);
    }

    public void addSlot(Teacher teacher, int assignmentId, String day, LocalTime startTime,
            LocalTime endTime) throws ValidationException, SQLException {
        Assignment assignment = getAssignmentOf(teacher, assignmentId);
        FieldValidator.requireOneOf(day, DAYS, "El día");
        validateTimes(startTime, endTime);
        checkWeeklyHours(assignment, startTime, endTime);
        checkTeacherConflict(teacher, assignment, day, startTime, endTime);
        checkGroupConflict(assignment, day, startTime, endTime);
        scheduleDao.insert(assignmentId, day, startTime, endTime);
    }

    public void removeSlot(Teacher teacher, int assignmentId, int slotId)
            throws ValidationException, SQLException {
        getAssignmentOf(teacher, assignmentId);
        ScheduleSlot slot = scheduleDao.findById(slotId).orElseThrow(
                () -> new ValidationException("No existe la franja " + slotId + "."));
        if (!slot.getAssignmentId().equals(assignmentId)) {
            throw new ValidationException("La franja " + slotId
                    + " no pertenece a la asignación " + assignmentId + ".");
        }
        scheduleDao.deleteById(slotId);
    }

    private Assignment getAssignmentOf(Teacher teacher, int assignmentId)
            throws ValidationException, SQLException {
        AssignmentDao.AssignmentRow row = assignmentDao.findById(assignmentId).orElseThrow(
                () -> new ValidationException("No existe la asignación " + assignmentId + "."));
        if (!teacher.getId().equals(row.teacherId())) {
            throw new ValidationException("La asignación " + assignmentId
                    + " no pertenece a " + teacher.getFullName() + ".");
        }
        return toAssignment(row);
    }

    private Assignment toAssignment(AssignmentDao.AssignmentRow row) throws SQLException {
        Assignment assignment = new Assignment();
        assignment.setId(row.id());
        assignment.setAcademicYear(row.academicYear());
        assignment.setSection(row.section());
        teacherDao.findById(row.teacherId()).ifPresent(assignment::setTeacher);
        subjectDao.findById(row.subjectId()).ifPresent(assignment::setSubject);
        assignment.setSlots(scheduleDao.findByAssignmentId(row.id()));
        return assignment;
    }

    private void validateTimes(LocalTime startTime, LocalTime endTime) throws ValidationException {
        if (startTime == null || endTime == null) {
            throw new ValidationException("La hora de inicio y la de fin son obligatorias.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("La hora de fin debe ser posterior a la de inicio.");
        }
        if (startTime.isBefore(FIRST_HOUR) || endTime.isAfter(LAST_HOUR)) {
            throw new ValidationException(
                    "Las clases deben estar entre las " + FIRST_HOUR + " y las " + LAST_HOUR + ".");
        }
    }

    private void checkWeeklyHours(Assignment assignment, LocalTime startTime, LocalTime endTime)
            throws ValidationException {
        long newMinutes = Duration.between(startTime, endTime).toMinutes();
        long limitMinutes = assignment.getSubject().getWeeklyHours() * 60L;
        if (assignment.getScheduledMinutes() + newMinutes > limitMinutes) {
            throw new ValidationException("La materia " + assignment.getSubject().getCode()
                    + " tiene " + limitMinutes + " minutos semanales y ya hay "
                    + assignment.getScheduledMinutes() + " programados; esta franja ("
                    + newMinutes + " min) los superaría.");
        }
    }

    private void checkTeacherConflict(Teacher teacher, Assignment assignment, String day,
            LocalTime startTime, LocalTime endTime) throws ValidationException, SQLException {
        Optional<ScheduleSlot> conflict = scheduleDao
                .findTeacherSlots(teacher.getId(), assignment.getAcademicYear(), day).stream()
                .filter(slot -> slot.overlaps(startTime, endTime))
                .findFirst();
        if (conflict.isPresent()) {
            throw new ValidationException("El profesor ya tiene una clase el " + day + " de "
                    + conflict.get().getStartTime() + " a " + conflict.get().getEndTime()
                    + " que se cruza con esta franja.");
        }
    }

    private void checkGroupConflict(Assignment assignment, String day, LocalTime startTime,
            LocalTime endTime) throws ValidationException, SQLException {
        Subject subject = assignment.getSubject();
        Optional<ScheduleSlot> conflict = scheduleDao.findGroupSlots(subject.getGrade().getId(),
                assignment.getAcademicYear(), assignment.getSection(), day).stream()
                .filter(slot -> slot.overlaps(startTime, endTime))
                .findFirst();
        if (conflict.isPresent()) {
            throw new ValidationException("El grupo " + subject.getGrade().getName()
                    + " sección " + assignment.getSection() + " ya tiene otra clase el " + day
                    + " de " + conflict.get().getStartTime() + " a "
                    + conflict.get().getEndTime() + " que se cruza con esta franja.");
        }
    }
}