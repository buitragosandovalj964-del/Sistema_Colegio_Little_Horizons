package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ScheduleDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Business rules for daily attendance and the absence report.
 * User-facing messages are in Spanish.
 */
public class AttendanceService {

    public static final String PRESENT = "Presente";
    public static final String ABSENT = "Ausente";
    public static final String LATE = "Tardanza";
    public static final String EXCUSED = "Excusa Médica";
    public static final List<String> STATUSES = List.of(PRESENT, ABSENT, LATE, EXCUSED);
    public static final int DEFAULT_MAX_ABSENCES_PER_WEEK = 3;

    private static final int MAX_NOTES_LENGTH = 500;
    private static final String TEACHER_ACTIVE = "Activo";

    /**
     * A student with more absences in one week than the allowed limit.
     */
    public record AbsencePattern(Student student, LocalDate weekStart, long absences) { }

    private record WeekKey(int studentId, LocalDate weekStart) { }

    private final AttendanceDao attendanceDao;
    private final SubjectEnrollmentDao inscriptionDao;
    private final AssignmentDao assignmentDao;
    private final ScheduleDao scheduleDao;
    private final TeacherDao teacherDao;
    private final StudentDao studentDao;
    private final SubjectDao subjectDao;

    public AttendanceService() {
        this(new AttendanceDao(), new SubjectEnrollmentDao(), new AssignmentDao(),
                new ScheduleDao(), new TeacherDao(), new StudentDao(), new SubjectDao());
    }

    public AttendanceService(AttendanceDao attendanceDao, SubjectEnrollmentDao inscriptionDao,
            AssignmentDao assignmentDao, ScheduleDao scheduleDao, TeacherDao teacherDao,
            StudentDao studentDao, SubjectDao subjectDao) {
        this.attendanceDao = attendanceDao;
        this.inscriptionDao = inscriptionDao;
        this.assignmentDao = assignmentDao;
        this.scheduleDao = scheduleDao;
        this.teacherDao = teacherDao;
        this.studentDao = studentDao;
        this.subjectDao = subjectDao;
    }

    /**
     * Counts the records of each status (used for the summary of a student).
     */
    public static Map<String, Long> countByStatus(List<Attendance> records) {
        return records.stream().collect(
                Collectors.groupingBy(Attendance::getStatus, Collectors.counting()));
    }

    /**
     * Validates the class and returns the students that still have no attendance
     * registered on that day, ordered by name.
     */
    public List<Student> findRollCall(Teacher teacher, Subject subject, String section,
            LocalDate date) throws ValidationException, SQLException {
        checkClass(teacher, subject, section, date);
        String normalizedSection = section.toUpperCase();

        Set<Integer> studentIds = new TreeSet<>();
        for (String period : SubjectEnrollmentService.PERIODS) {
            inscriptionDao.findRoster(subject.getId(), date.getYear(), period,
                            normalizedSection).stream()
                    .filter(row -> !EnrollmentService.STATUS_WITHDRAWN.equals(row.status()))
                    .forEach(row -> studentIds.add(row.studentId()));
        }
        List<Student> students = new ArrayList<>();
        for (int studentId : studentIds) {
            if (attendanceDao.findByKey(studentId, subject.getId(), date).isEmpty()) {
                studentDao.findById(studentId).ifPresent(students::add);
            }
        }
        if (students.isEmpty()) {
            throw new ValidationException("No hay estudiantes pendientes de asistencia en "
                    + subject.getCode() + ", sección " + normalizedSection + ", el " + date
                    + " (no hay inscritos o ya se registró la asistencia).");
        }
        students.sort(Comparator.comparing(Student::getFirstLastName)
                .thenComparing(Student::getFirstName));
        return students;
    }

    public void saveAttendance(Teacher teacher, Subject subject, String section,
            LocalDate date, List<Attendance> entries) throws ValidationException, SQLException {
        checkClass(teacher, subject, section, date);
        for (Attendance entry : entries) {
            FieldValidator.requireOneOf(entry.getStatus(), STATUSES, "El estado de asistencia");
            FieldValidator.validateMaxLength(entry.getNotes(), "Las observaciones",
                    MAX_NOTES_LENGTH);
            entry.setSubject(subject);
            entry.setRegisteredBy(teacher);
            entry.setDate(date);
        }
        try {
            attendanceDao.insertAll(entries);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Alguno de los estudiantes ya tiene la asistencia "
                    + "de ese día registrada.");
        }
    }

    public void correctAttendance(Teacher teacher, Student student, Subject subject,
            LocalDate date, String status, String notes)
            throws ValidationException, SQLException {
        FieldValidator.requireOneOf(status, STATUSES, "El estado de asistencia");
        FieldValidator.validateMaxLength(notes, "Las observaciones", MAX_NOTES_LENGTH);
        AttendanceDao.AttendanceRow row = findOwnedRow(teacher, student, subject, date);
        attendanceDao.update(row.id(), status, notes);
    }

    public void deleteAttendance(Teacher teacher, Student student, Subject subject,
            LocalDate date) throws ValidationException, SQLException {
        AttendanceDao.AttendanceRow row = findOwnedRow(teacher, student, subject, date);
        attendanceDao.deleteById(row.id());
    }

    public List<Attendance> findAttendanceOf(int studentId, int academicYear)
            throws SQLException {
        return toAttendances(attendanceDao.findByStudentAndYear(studentId, academicYear));
    }

    public List<Attendance> findCourseAttendance(Subject subject, String section,
            LocalDate date) throws SQLException {
        return toAttendances(attendanceDao.findByCourseAndDate(
                subject.getId(), date, section.toUpperCase()));
    }

    /**
     * Finds the students with more absences in one week (Monday to Sunday, all subjects
     * together) than the allowed limit, within a date range.
     */
    public List<AbsencePattern> findAbsencePatterns(LocalDate from, LocalDate to,
            int maxAbsencesPerWeek) throws ValidationException, SQLException {
        if (from == null || to == null || to.isBefore(from)) {
            throw new ValidationException(
                    "El rango de fechas no es válido: 'Hasta' no puede ser anterior a 'Desde'.");
        }
        if (maxAbsencesPerWeek < 1) {
            throw new ValidationException("El límite de ausencias debe ser al menos 1.");
        }
        Map<WeekKey, Long> absencesPerWeek = attendanceDao.findAbsencesBetween(from, to).stream()
                .collect(Collectors.groupingBy(
                        row -> new WeekKey(row.studentId(),
                                row.date().with(TemporalAdjusters.previousOrSame(
                                        DayOfWeek.MONDAY))),
                        Collectors.counting()));

        List<AbsencePattern> patterns = new ArrayList<>();
        for (Map.Entry<WeekKey, Long> entry : absencesPerWeek.entrySet()) {
            if (entry.getValue() > maxAbsencesPerWeek) {
                studentDao.findById(entry.getKey().studentId()).ifPresent(student ->
                        patterns.add(new AbsencePattern(student, entry.getKey().weekStart(),
                                entry.getValue())));
            }
        }
        patterns.sort(Comparator.comparing(AbsencePattern::weekStart)
                .thenComparing(Comparator.comparingLong(AbsencePattern::absences).reversed()));
        return patterns;
    }

    // ---------- Rules ----------

    /**
     * Checks that the teacher can take attendance of that subject and section on that date.
     */
    private void checkClass(Teacher teacher, Subject subject, String section, LocalDate date)
            throws ValidationException, SQLException {
        if (!TEACHER_ACTIVE.equals(teacher.getStatus())) {
            throw new ValidationException("El profesor " + teacher.getFullName()
                    + " está en estado " + teacher.getStatus()
                    + "; solo un profesor activo puede tomar asistencia.");
        }
        validateDate(date);
        if (date.getYear() < AssignmentService.minAcademicYear()
                || date.getYear() > AssignmentService.maxAcademicYear()) {
            throw new ValidationException("El año de la fecha debe estar entre "
                    + AssignmentService.minAcademicYear() + " y "
                    + AssignmentService.maxAcademicYear() + ".");
        }
        if (section == null || !section.matches(AssignmentService.SECTION_PATTERN)) {
            throw new ValidationException("La sección debe ser una letra (A, B, C...).");
        }
        String normalizedSection = section.toUpperCase();
        AssignmentDao.AssignmentRow assignment = assignmentDao
                .findBySubjectYearSection(subject.getId(), date.getYear(), normalizedSection)
                .orElseThrow(() -> new ValidationException("No hay profesor asignado a "
                        + subject.getCode() + ", sección " + normalizedSection + ", año "
                        + date.getYear() + "."));
        if (!teacher.getId().equals(assignment.teacherId())) {
            String otherTeacher = teacherDao.findById(assignment.teacherId())
                    .map(Teacher::getFullName).orElse("otro profesor");
            throw new ValidationException("El profesor " + teacher.getFullName()
                    + " no dicta " + subject.getCode() + " en la sección " + normalizedSection
                    + " en " + date.getYear() + "; la dicta " + otherTeacher + ".");
        }
        String dayName = AssignmentService.DAYS.get(date.getDayOfWeek().getValue() - 1);
        List<ScheduleSlot> slots = scheduleDao.findByAssignmentId(assignment.id());
        if (!slots.isEmpty() && slots.stream().noneMatch(slot -> slot.getDay().equals(dayName))) {
            throw new ValidationException("Según el horario, " + subject.getCode()
                    + " no tiene clase los " + dayName.toLowerCase() + ".");
        }
    }

    private void validateDate(LocalDate date) throws ValidationException {
        if (date == null) {
            throw new ValidationException("La fecha es obligatoria.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("No se puede registrar asistencia de una fecha futura.");
        }
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            throw new ValidationException(
                    "La asistencia solo se registra de lunes a viernes.");
        }
    }

    private AttendanceDao.AttendanceRow findOwnedRow(Teacher teacher, Student student,
            Subject subject, LocalDate date) throws ValidationException, SQLException {
        AttendanceDao.AttendanceRow row = attendanceDao
                .findByKey(student.getId(), subject.getId(), date)
                .orElseThrow(() -> new ValidationException("No hay asistencia registrada de "
                        + student.getFullName() + " en " + subject.getCode() + " el "
                        + date + "."));
        if (row.teacherId() == null || !teacher.getId().equals(row.teacherId())) {
            String author = row.teacherId() == null ? "otro profesor"
                    : teacherDao.findById(row.teacherId())
                            .map(Teacher::getFullName).orElse("otro profesor");
            throw new ValidationException("Solo el profesor que registró la asistencia puede "
                    + "modificarla (la registró " + author + ").");
        }
        return row;
    }

    // ---------- Loading ----------

    private List<Attendance> toAttendances(List<AttendanceDao.AttendanceRow> rows)
            throws SQLException {
        Map<Integer, Student> students = new HashMap<>();
        Map<Integer, Subject> subjects = new HashMap<>();
        Map<Integer, Teacher> teachers = new HashMap<>();
        List<Attendance> records = new ArrayList<>();
        for (AttendanceDao.AttendanceRow row : rows) {
            if (!students.containsKey(row.studentId())) {
                students.put(row.studentId(), studentDao.findById(row.studentId()).orElse(null));
            }
            if (!subjects.containsKey(row.subjectId())) {
                subjects.put(row.subjectId(), subjectDao.findById(row.subjectId()).orElse(null));
            }
            if (row.teacherId() != null && !teachers.containsKey(row.teacherId())) {
                teachers.put(row.teacherId(), teacherDao.findById(row.teacherId()).orElse(null));
            }
            Attendance record = new Attendance();
            record.setId(row.id());
            record.setStudent(students.get(row.studentId()));
            record.setSubject(subjects.get(row.subjectId()));
            record.setRegisteredBy(row.teacherId() == null ? null : teachers.get(row.teacherId()));
            record.setDate(row.date());
            record.setStatus(row.status());
            record.setNotes(row.notes());
            records.add(record);
        }
        return records;
    }
}