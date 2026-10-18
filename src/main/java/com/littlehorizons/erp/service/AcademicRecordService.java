package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Business rules for grades and the automatic averages.
 * User-facing messages are in Spanish.
 */
public class AcademicRecordService {

    public static final String FINAL_PERIOD = "Final";
    public static final List<String> PERIODS_WITH_FINAL =
            List.of("I", "II", "III", "IV", FINAL_PERIOD);

    private static final BigDecimal MAX_VALUE = new BigDecimal("5.0");
    private static final int MAX_NOTES_LENGTH = 500;
    private static final String TEACHER_ACTIVE = "Activo";

    private final AcademicRecordDao recordDao;
    private final SubjectEnrollmentDao inscriptionDao;
    private final AssignmentDao assignmentDao;
    private final TeacherDao teacherDao;
    private final SubjectDao subjectDao;
    private final StudentDao studentDao;

    public AcademicRecordService() {
        this(new AcademicRecordDao(), new SubjectEnrollmentDao(), new AssignmentDao(),
                new TeacherDao(), new SubjectDao(), new StudentDao());
    }

    public AcademicRecordService(AcademicRecordDao recordDao,
            SubjectEnrollmentDao inscriptionDao, AssignmentDao assignmentDao,
            TeacherDao teacherDao, SubjectDao subjectDao, StudentDao studentDao) {
        this.recordDao = recordDao;
        this.inscriptionDao = inscriptionDao;
        this.assignmentDao = assignmentDao;
        this.teacherDao = teacherDao;
        this.subjectDao = subjectDao;
        this.studentDao = studentDao;
    }

    // ---------- Averages (pure functions, also used by the view) ----------

    /**
     * Average of all the grades of each period (the Final period is its own entry).
     */
    public static Map<String, Double> averagesByPeriod(List<AcademicRecord> records) {
        return records.stream().collect(Collectors.groupingBy(
                AcademicRecord::getPeriod,
                Collectors.averagingDouble(record -> record.getValue().doubleValue())));
    }

    /**
     * Average of the grades of periods I to IV (the Final grade is not included).
     */
    public static OptionalDouble overallAverage(List<AcademicRecord> records) {
        return records.stream()
                .filter(record -> !FINAL_PERIOD.equals(record.getPeriod()))
                .mapToDouble(record -> record.getValue().doubleValue())
                .average();
    }

    /**
     * Average of the grades of periods I to IV of one subject, given period to grade.
     */
    public static OptionalDouble averageOfPeriods(Map<String, BigDecimal> gradesByPeriod) {
        return gradesByPeriod.entrySet().stream()
                .filter(entry -> !FINAL_PERIOD.equals(entry.getKey()))
                .mapToDouble(entry -> entry.getValue().doubleValue())
                .average();
    }

    // ---------- Operations ----------

    public void registerGrade(Teacher teacher, Student student, Subject subject,
            int academicYear, String period, BigDecimal value, String notes)
            throws ValidationException, SQLException {
        checkTeacherActive(teacher);
        validateYearAndPeriod(academicYear, period);
        validateValue(value);
        FieldValidator.validateMaxLength(notes, "Las observaciones", MAX_NOTES_LENGTH);

        String section = findSectionForGrading(student, subject, academicYear, period);
        checkTeacherTeaches(teacher, subject, academicYear, section);
        if (recordDao.findByKey(student.getId(), subject.getId(), academicYear, period)
                .isPresent()) {
            throw new ValidationException("Ya existe la nota de " + student.getFullName()
                    + " en " + subject.getCode() + ", período " + period + " de " + academicYear
                    + ". Use la opción 'Corregir nota'.");
        }
        recordDao.insert(student.getId(), subject.getId(), teacher.getId(), academicYear,
                period, value.setScale(1, RoundingMode.UNNECESSARY), notes, LocalDate.now());
    }

    public void correctGrade(Teacher teacher, Student student, Subject subject,
            int academicYear, String period, BigDecimal value, String notes)
            throws ValidationException, SQLException {
        checkTeacherActive(teacher);
        validateValue(value);
        FieldValidator.validateMaxLength(notes, "Las observaciones", MAX_NOTES_LENGTH);
        AcademicRecordDao.RecordRow row = findOwnedRow(teacher, student, subject,
                academicYear, period);
        recordDao.update(row.id(), value.setScale(1, RoundingMode.UNNECESSARY), notes,
                LocalDate.now());
    }

    public void deleteGrade(Teacher teacher, Student student, Subject subject,
            int academicYear, String period) throws ValidationException, SQLException {
        AcademicRecordDao.RecordRow row = findOwnedRow(teacher, student, subject,
                academicYear, period);
        recordDao.deleteById(row.id());
    }

    public Optional<AcademicRecord> findRecord(Student student, Subject subject,
            int academicYear, String period) throws SQLException {
        Optional<AcademicRecordDao.RecordRow> row = recordDao.findByKey(
                student.getId(), subject.getId(), academicYear, period);
        if (row.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toRecords(List.of(row.get())).get(0));
    }

    public List<AcademicRecord> findHistoryOf(int studentId) throws SQLException {
        return toRecords(recordDao.findByStudent(studentId));
    }

    public List<AcademicRecord> findRecordsOfYear(int studentId, int academicYear)
            throws SQLException {
        return toRecords(recordDao.findByStudentAndYear(studentId, academicYear));
    }

    public List<AcademicRecord> findCourseRecords(Subject subject, int academicYear,
            String section) throws SQLException {
        return toRecords(recordDao.findByCourse(subject.getId(), academicYear,
                section.toUpperCase()));
    }

    // ---------- Rules ----------

    private void checkTeacherActive(Teacher teacher) throws ValidationException {
        if (!TEACHER_ACTIVE.equals(teacher.getStatus())) {
            throw new ValidationException("El profesor " + teacher.getFullName()
                    + " está en estado " + teacher.getStatus()
                    + "; solo un profesor activo puede calificar.");
        }
    }

    private void validateYearAndPeriod(int academicYear, String period)
            throws ValidationException {
        if (academicYear < AssignmentService.minAcademicYear()
                || academicYear > AssignmentService.maxAcademicYear()) {
            throw new ValidationException("El año académico debe estar entre "
                    + AssignmentService.minAcademicYear() + " y "
                    + AssignmentService.maxAcademicYear() + ".");
        }
        FieldValidator.requireOneOf(period, PERIODS_WITH_FINAL, "El período");
    }

    private void validateValue(BigDecimal value) throws ValidationException {
        if (value == null) {
            throw new ValidationException("La nota es obligatoria.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(MAX_VALUE) > 0) {
            throw new ValidationException("La nota debe estar entre 0.0 y 5.0.");
        }
        if (value.stripTrailingZeros().scale() > 1) {
            throw new ValidationException("La nota debe tener máximo un decimal.");
        }
    }

    /**
     * Finds the section in which the student takes the subject. A period from I to IV
     * needs an active enrollment of that period; the Final grade needs any enrollment
     * of the subject in the year that was not withdrawn.
     */
    private String findSectionForGrading(Student student, Subject subject, int academicYear,
            String period) throws ValidationException, SQLException {
        List<SubjectEnrollmentDao.InscriptionRow> inscriptions = inscriptionDao
                .findByStudentAndYear(student.getId(), academicYear).stream()
                .filter(row -> row.subjectId() == subject.getId())
                .toList();
        if (FINAL_PERIOD.equals(period)) {
            return inscriptions.stream()
                    .filter(row -> !EnrollmentService.STATUS_WITHDRAWN.equals(row.status()))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException(student.getFullName()
                            + " no tiene inscripción vigente en " + subject.getCode()
                            + " en " + academicYear + "."))
                    .section();
        }
        SubjectEnrollmentDao.InscriptionRow inscription = inscriptions.stream()
                .filter(row -> row.period().equals(period))
                .findFirst()
                .orElseThrow(() -> new ValidationException(student.getFullName()
                        + " no está inscrito en " + subject.getCode() + " en el período "
                        + period + " de " + academicYear + "."));
        if (!EnrollmentService.STATUS_ACTIVE.equals(inscription.status())) {
            throw new ValidationException("La inscripción de " + student.getFullName() + " en "
                    + subject.getCode() + " (período " + period + ") está "
                    + inscription.status() + "; solo se califica con inscripción activa.");
        }
        return inscription.section();
    }

    private void checkTeacherTeaches(Teacher teacher, Subject subject, int academicYear,
            String section) throws ValidationException, SQLException {
        AssignmentDao.AssignmentRow assignment = assignmentDao
                .findBySubjectYearSection(subject.getId(), academicYear, section)
                .orElseThrow(() -> new ValidationException("No hay profesor asignado a "
                        + subject.getCode() + ", sección " + section + ", año "
                        + academicYear + "."));
        if (!teacher.getId().equals(assignment.teacherId())) {
            String otherTeacher = teacherDao.findById(assignment.teacherId())
                    .map(Teacher::getFullName).orElse("otro profesor");
            throw new ValidationException("El profesor " + teacher.getFullName()
                    + " no dicta " + subject.getCode() + " en la sección " + section
                    + " en " + academicYear + "; la dicta " + otherTeacher + ".");
        }
    }

    private AcademicRecordDao.RecordRow findOwnedRow(Teacher teacher, Student student,
            Subject subject, int academicYear, String period)
            throws ValidationException, SQLException {
        AcademicRecordDao.RecordRow row = recordDao
                .findByKey(student.getId(), subject.getId(), academicYear, period)
                .orElseThrow(() -> new ValidationException("No hay una nota registrada de "
                        + student.getFullName() + " en " + subject.getCode() + ", período "
                        + period + " de " + academicYear + "."));
        if (!teacher.getId().equals(row.teacherId())) {
            String author = teacherDao.findById(row.teacherId())
                    .map(Teacher::getFullName).orElse("otro profesor");
            throw new ValidationException("Solo el profesor que registró la nota puede "
                    + "modificarla (la registró " + author + ").");
        }
        return row;
    }

    // ---------- Loading ----------

    private List<AcademicRecord> toRecords(List<AcademicRecordDao.RecordRow> rows)
            throws SQLException {
        Map<Integer, Student> students = new HashMap<>();
        Map<Integer, Subject> subjects = new HashMap<>();
        Map<Integer, Teacher> teachers = new HashMap<>();
        List<AcademicRecord> records = new ArrayList<>();
        for (AcademicRecordDao.RecordRow row : rows) {
            if (!students.containsKey(row.studentId())) {
                students.put(row.studentId(), studentDao.findById(row.studentId()).orElse(null));
            }
            if (!subjects.containsKey(row.subjectId())) {
                subjects.put(row.subjectId(), subjectDao.findById(row.subjectId()).orElse(null));
            }
            if (!teachers.containsKey(row.teacherId())) {
                teachers.put(row.teacherId(), teacherDao.findById(row.teacherId()).orElse(null));
            }
            AcademicRecord record = new AcademicRecord();
            record.setId(row.id());
            record.setStudent(students.get(row.studentId()));
            record.setSubject(subjects.get(row.subjectId()));
            record.setTeacher(teachers.get(row.teacherId()));
            record.setAcademicYear(row.academicYear());
            record.setPeriod(row.period());
            record.setValue(row.value());
            record.setNotes(row.notes());
            record.setRecordDate(row.recordDate());
            records.add(record);
        }
        return records;
    }
}