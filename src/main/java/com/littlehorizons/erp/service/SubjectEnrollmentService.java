package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Business rules for subject enrollments (a student in a subject for a year and period).
 * User-facing messages are in Spanish.
 */
public class SubjectEnrollmentService {

    public static final List<String> PERIODS = List.of("I", "II", "III", "IV");
    public static final String ALL_PERIODS = "Todos";
    public static final List<String> PERIOD_CHOICES = List.of("I", "II", "III", "IV", ALL_PERIODS);
    public static final List<String> CLOSING_STATUSES =
            List.of(EnrollmentService.STATUS_COMPLETED, EnrollmentService.STATUS_WITHDRAWN);

    private final SubjectEnrollmentDao inscriptionDao;
    private final EnrollmentDao enrollmentDao;
    private final AssignmentDao assignmentDao;
    private final SubjectDao subjectDao;
    private final StudentDao studentDao;
    private final GradeDao gradeDao;

    public SubjectEnrollmentService() {
        this(new SubjectEnrollmentDao(), new EnrollmentDao(), new AssignmentDao(),
                new SubjectDao(), new StudentDao(), new GradeDao());
    }

    public SubjectEnrollmentService(SubjectEnrollmentDao inscriptionDao,
            EnrollmentDao enrollmentDao, AssignmentDao assignmentDao, SubjectDao subjectDao,
            StudentDao studentDao, GradeDao gradeDao) {
        this.inscriptionDao = inscriptionDao;
        this.enrollmentDao = enrollmentDao;
        this.assignmentDao = assignmentDao;
        this.subjectDao = subjectDao;
        this.studentDao = studentDao;
        this.gradeDao = gradeDao;
    }

    public void enroll(Student student, Subject subject, int academicYear, String periodChoice,
            String section, LocalDate date) throws ValidationException, SQLException {
        if (academicYear < AssignmentService.minAcademicYear()
                || academicYear > AssignmentService.maxAcademicYear()) {
            throw new ValidationException("El año académico debe estar entre "
                    + AssignmentService.minAcademicYear() + " y "
                    + AssignmentService.maxAcademicYear() + ".");
        }
        if (section == null || !section.matches(AssignmentService.SECTION_PATTERN)) {
            throw new ValidationException("La sección debe ser una letra (A, B, C...).");
        }
        String normalizedSection = section.toUpperCase();
        FieldValidator.requireOneOf(periodChoice, PERIOD_CHOICES, "El período");
        List<String> periods = ALL_PERIODS.equals(periodChoice) ? PERIODS : List.of(periodChoice);
        validateDate(date);

        EnrollmentDao.EnrollmentRow enrollment = enrollmentDao
                .findByStudentAndYear(student.getId(), academicYear).orElseThrow(
                        () -> new ValidationException(student.getFullName()
                                + " no tiene matrícula en " + academicYear
                                + ". Matricúlelo primero (Gestión Académica > Matrículas)."));
        if (!EnrollmentService.STATUS_ACTIVE.equals(enrollment.status())) {
            throw new ValidationException("La matrícula de " + student.getFullName() + " en "
                    + academicYear + " está " + enrollment.status()
                    + "; solo se inscribe a estudiantes con matrícula activa.");
        }
        if (!subject.isActive()) {
            throw new ValidationException("La materia " + subject.getCode() + " está inactiva.");
        }
        if (!subject.getGrade().getId().equals(enrollment.gradeId())) {
            String enrolledGrade = gradeDao.findById(enrollment.gradeId())
                    .map(Grade::getName).orElse("otro grado");
            throw new ValidationException("La materia " + subject.getCode() + " es de "
                    + subject.getGrade().getName() + ", pero el estudiante está matriculado en "
                    + enrolledGrade + " en " + academicYear + ".");
        }
        if (assignmentDao.findBySubjectYearSection(
                subject.getId(), academicYear, normalizedSection).isEmpty()) {
            throw new ValidationException("No hay un profesor asignado a " + subject.getCode()
                    + ", sección " + normalizedSection + ", año " + academicYear
                    + ". Asígnelo primero (Gestión de Profesores > Asignaciones y horarios).");
        }

        List<SubjectEnrollmentDao.InscriptionRow> existing =
                inscriptionDao.findByStudentAndYear(student.getId(), academicYear);
        checkSameSection(existing, normalizedSection, academicYear);
        for (String period : periods) {
            boolean alreadyEnrolled = existing.stream().anyMatch(row ->
                    row.subjectId() == subject.getId() && row.period().equals(period));
            if (alreadyEnrolled) {
                throw new ValidationException(student.getFullName() + " ya está inscrito en "
                        + subject.getCode() + " en el período " + period + " de "
                        + academicYear + ".");
            }
        }
        inscriptionDao.insertForPeriods(student.getId(), subject.getId(), academicYear,
                periods, normalizedSection, date);
    }

    public void changeStatus(Student student, int inscriptionId, String newStatus)
            throws ValidationException, SQLException {
        SubjectEnrollmentDao.InscriptionRow row = getInscriptionOf(student, inscriptionId);
        if (!EnrollmentService.STATUS_ACTIVE.equals(row.status())) {
            throw new ValidationException("Solo se puede cambiar el estado de una inscripción "
                    + "activa (esta está " + row.status() + ").");
        }
        FieldValidator.requireOneOf(newStatus, CLOSING_STATUSES, "El nuevo estado");
        inscriptionDao.updateStatus(inscriptionId, newStatus);
    }

    public void deleteInscription(Student student, int inscriptionId)
            throws ValidationException, SQLException {
        getInscriptionOf(student, inscriptionId);
        inscriptionDao.deleteById(inscriptionId);
    }

    public List<SubjectEnrollment> findInscriptionsOf(int studentId, int academicYear)
            throws SQLException {
        List<SubjectEnrollment> inscriptions = new ArrayList<>();
        for (SubjectEnrollmentDao.InscriptionRow row
                : inscriptionDao.findByStudentAndYear(studentId, academicYear)) {
            inscriptions.add(toInscription(row));
        }
        return inscriptions;
    }

    public List<SubjectEnrollment> findRoster(Subject subject, int academicYear, String period,
            String section) throws SQLException {
        List<SubjectEnrollment> inscriptions = new ArrayList<>();
        for (SubjectEnrollmentDao.InscriptionRow row : inscriptionDao.findRoster(
                subject.getId(), academicYear, period, section.toUpperCase())) {
            inscriptions.add(toInscription(row));
        }
        return inscriptions;
    }

    private void checkSameSection(List<SubjectEnrollmentDao.InscriptionRow> existing,
            String section, int academicYear) throws ValidationException {
        for (SubjectEnrollmentDao.InscriptionRow row : existing) {
            if (!row.section().equalsIgnoreCase(section)) {
                throw new ValidationException("El estudiante ya está inscrito en la sección "
                        + row.section() + " en " + academicYear
                        + "; todas sus materias deben ser de la misma sección.");
            }
        }
    }

    private SubjectEnrollmentDao.InscriptionRow getInscriptionOf(Student student,
            int inscriptionId) throws ValidationException, SQLException {
        SubjectEnrollmentDao.InscriptionRow row = inscriptionDao.findById(inscriptionId)
                .orElseThrow(() -> new ValidationException(
                        "No existe la inscripción " + inscriptionId + "."));
        if (!student.getId().equals(row.studentId())) {
            throw new ValidationException("La inscripción " + inscriptionId
                    + " no pertenece a " + student.getFullName() + ".");
        }
        return row;
    }

    private SubjectEnrollment toInscription(SubjectEnrollmentDao.InscriptionRow row)
            throws SQLException {
        SubjectEnrollment inscription = new SubjectEnrollment();
        inscription.setId(row.id());
        inscription.setAcademicYear(row.academicYear());
        inscription.setPeriod(row.period());
        inscription.setSection(row.section());
        inscription.setEnrollmentDate(row.enrollmentDate());
        inscription.setStatus(row.status());
        studentDao.findById(row.studentId()).ifPresent(inscription::setStudent);
        subjectDao.findById(row.subjectId()).ifPresent(inscription::setSubject);
        return inscription;
    }

    private void validateDate(LocalDate date) throws ValidationException {
        if (date == null) {
            throw new ValidationException("La fecha de inscripción es obligatoria.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de inscripción no puede ser futura.");
        }
    }
}