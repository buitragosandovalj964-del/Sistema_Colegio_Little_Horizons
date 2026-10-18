package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for enrollments (a student in a grade for an academic year).
 * User-facing messages are in Spanish.
 */
public class EnrollmentService {

    public static final String STATUS_ACTIVE = "Activa";
    public static final String STATUS_COMPLETED = "Completada";
    public static final String STATUS_WITHDRAWN = "Retirada";
    public static final List<String> CLOSING_STATUSES = List.of(STATUS_COMPLETED, STATUS_WITHDRAWN);

    private static final int LAST_GRADE_LEVEL = 11;
    private static final int MAX_NOTES_LENGTH = 500;
    private static final String STUDENT_ACTIVE = "Activo";
    private static final String STUDENT_GRADUATED = "Graduado";

    private final EnrollmentDao enrollmentDao;
    private final StudentDao studentDao;
    private final GradeDao gradeDao;
    private final StudentGuardianService guardianLinkService;

    public EnrollmentService() {
        this(new EnrollmentDao(), new StudentDao(), new GradeDao(),
                new StudentGuardianService());
    }

    public EnrollmentService(EnrollmentDao enrollmentDao, StudentDao studentDao,
            GradeDao gradeDao, StudentGuardianService guardianLinkService) {
        this.enrollmentDao = enrollmentDao;
        this.studentDao = studentDao;
        this.gradeDao = gradeDao;
        this.guardianLinkService = guardianLinkService;
    }

    public void registerEnrollment(Student student, Grade grade, int academicYear,
            LocalDate enrollmentDate, String notes) throws ValidationException, SQLException {
        if (!STUDENT_ACTIVE.equals(student.getStatus())) {
            throw new ValidationException("El estudiante " + student.getFullName()
                    + " está en estado " + student.getStatus()
                    + "; solo se puede matricular a estudiantes activos.");
        }
        if (guardianLinkService.findGuardiansOf(student.getId()).isEmpty()) {
            throw new ValidationException("El estudiante " + student.getFullName()
                    + " no tiene ningún acudiente. Vincule al menos uno "
                    + "(Gestión de Estudiantes > Vínculos estudiante-acudiente) "
                    + "antes de matricularlo.");
        }
        if (grade == null || grade.getId() == null) {
            throw new ValidationException("El grado es obligatorio.");
        }
        if (academicYear < AssignmentService.minAcademicYear()
                || academicYear > AssignmentService.maxAcademicYear()) {
            throw new ValidationException("El año académico debe estar entre "
                    + AssignmentService.minAcademicYear() + " y "
                    + AssignmentService.maxAcademicYear() + ".");
        }
        validateDate(enrollmentDate);
        FieldValidator.validateMaxLength(notes, "Las observaciones", MAX_NOTES_LENGTH);
        if (enrollmentDao.findByStudentAndYear(student.getId(), academicYear).isPresent()) {
            throw new ValidationException("El estudiante " + student.getFullName()
                    + " ya tiene una matrícula en el año " + academicYear + ".");
        }
        checkGradeProgression(student, grade, academicYear);
        enrollmentDao.insert(student.getId(), grade.getId(), academicYear, STATUS_ACTIVE,
                enrollmentDate, notes);
    }

    /**
     * Completes or withdraws an active enrollment. Completing the last grade
     * graduates the student.
     */
    public void changeStatus(Student student, int enrollmentId, String newStatus, String notes)
            throws ValidationException, SQLException {
        Enrollment enrollment = getEnrollmentOf(student, enrollmentId);
        if (!STATUS_ACTIVE.equals(enrollment.getStatus())) {
            throw new ValidationException("Solo se puede cambiar el estado de una matrícula "
                    + "activa (esta está " + enrollment.getStatus() + ").");
        }
        FieldValidator.requireOneOf(newStatus, CLOSING_STATUSES, "El nuevo estado");
        FieldValidator.validateMaxLength(notes, "Las observaciones", MAX_NOTES_LENGTH);

        LocalDate withdrawalDate = STATUS_WITHDRAWN.equals(newStatus) ? LocalDate.now() : null;
        enrollmentDao.updateStatus(enrollmentId, newStatus, withdrawalDate, notes);

        boolean finishedLastGrade = STATUS_COMPLETED.equals(newStatus)
                && enrollment.getGrade().getNumericLevel() == LAST_GRADE_LEVEL;
        if (finishedLastGrade) {
            student.setStatus(STUDENT_GRADUATED);
            studentDao.update(student);
        }
    }

    public void deleteEnrollment(Student student, int enrollmentId)
            throws ValidationException, SQLException {
        getEnrollmentOf(student, enrollmentId);
        enrollmentDao.deleteById(enrollmentId);
    }

    public List<Enrollment> findEnrollmentsOf(int studentId) throws SQLException {
        List<Enrollment> enrollments = new ArrayList<>();
        for (EnrollmentDao.EnrollmentRow row : enrollmentDao.findByStudentId(studentId)) {
            enrollments.add(toEnrollment(row));
        }
        return enrollments;
    }

    public List<Enrollment> findRoster(int gradeId, int academicYear) throws SQLException {
        List<Enrollment> enrollments = new ArrayList<>();
        for (EnrollmentDao.EnrollmentRow row
                : enrollmentDao.findByGradeAndYear(gradeId, academicYear)) {
            enrollments.add(toEnrollment(row));
        }
        return enrollments;
    }

    public List<Grade> listGrades() throws SQLException {
        return gradeDao.findAll();
    }

    private Enrollment getEnrollmentOf(Student student, int enrollmentId)
            throws ValidationException, SQLException {
        EnrollmentDao.EnrollmentRow row = enrollmentDao.findById(enrollmentId).orElseThrow(
                () -> new ValidationException("No existe la matrícula " + enrollmentId + "."));
        if (!student.getId().equals(row.studentId())) {
            throw new ValidationException("La matrícula " + enrollmentId
                    + " no pertenece a " + student.getFullName() + ".");
        }
        return toEnrollment(row);
    }

    private Enrollment toEnrollment(EnrollmentDao.EnrollmentRow row) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(row.id());
        enrollment.setAcademicYear(row.academicYear());
        enrollment.setStatus(row.status());
        enrollment.setEnrollmentDate(row.enrollmentDate());
        enrollment.setWithdrawalDate(row.withdrawalDate());
        enrollment.setNotes(row.notes());
        studentDao.findById(row.studentId()).ifPresent(enrollment::setStudent);
        gradeDao.findById(row.gradeId()).ifPresent(enrollment::setGrade);
        return enrollment;
    }

    /**
     * A student can repeat a grade or move to the next one, but not go back or skip grades.
     * The comparison is made with the most recent enrollment of an earlier year.
     */
    private void checkGradeProgression(Student student, Grade grade, int academicYear)
            throws ValidationException, SQLException {
        Optional<EnrollmentDao.EnrollmentRow> previous =
                enrollmentDao.findByStudentId(student.getId()).stream()
                        .filter(row -> row.academicYear() < academicYear)
                        .max(Comparator.comparingInt(EnrollmentDao.EnrollmentRow::academicYear));
        if (previous.isEmpty()) {
            return;
        }
        Optional<Grade> previousGrade = gradeDao.findById(previous.get().gradeId());
        if (previousGrade.isEmpty()) {
            return;
        }
        int difference = grade.getNumericLevel() - previousGrade.get().getNumericLevel();
        String lastGradeText = "En " + previous.get().academicYear() + " el estudiante estuvo en "
                + previousGrade.get().getName() + ". ";
        if (difference < 0) {
            throw new ValidationException(lastGradeText + "No puede retroceder a "
                    + grade.getName() + ".");
        }
        if (difference > 1) {
            throw new ValidationException(lastGradeText + "No puede saltar a "
                    + grade.getName() + "; solo puede repetir ese grado o pasar al siguiente.");
        }
    }

    private void validateDate(LocalDate date) throws ValidationException {
        if (date == null) {
            throw new ValidationException("La fecha de matrícula es obligatoria.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de matrícula no puede ser futura.");
        }
    }
}