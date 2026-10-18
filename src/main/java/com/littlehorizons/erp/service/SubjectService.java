package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Business rules for subjects. The code is generated from a 3-letter prefix and
 * the grade level (for example MAT-5). User-facing messages are in Spanish.
 */
public class SubjectService {

    public static final int MIN_WEEKLY_HOURS = 1;
    public static final int MAX_WEEKLY_HOURS = 10;
    public static final int MAX_NAME_LENGTH = 100;
    public static final String CODE_PREFIX_PATTERN = "[A-Za-z]{3}";
    /** Accepts 2 to 5 letters so that older codes can still be found. */
    public static final String CODE_PATTERN = "[A-Za-z]{2,5}-\\d{1,2}";

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final SubjectDao subjectDao;
    private final GradeDao gradeDao;

    public SubjectService() {
        this(new SubjectDao(), new GradeDao());
    }

    public SubjectService(SubjectDao subjectDao, GradeDao gradeDao) {
        this.subjectDao = subjectDao;
        this.gradeDao = gradeDao;
    }

    public void registerSubject(Subject subject, String codePrefix)
            throws ValidationException, SQLException {
        if (codePrefix == null || !codePrefix.matches(CODE_PREFIX_PATTERN)) {
            throw new ValidationException("El prefijo del código debe tener exactamente 3 letras.");
        }
        Grade grade = findExistingGrade(subject.getGrade());
        subject.setGrade(grade);
        subject.setCode(codePrefix.toUpperCase() + "-" + grade.getNumericLevel());
        validate(subject);
        subject.setName(capitalize(subject.getName()));
        if (subjectDao.findByCode(subject.getCode()).isPresent()) {
            throw new ValidationException(
                    "Ya existe una materia con el código " + subject.getCode() + ".");
        }
        checkNameNotRepeated(subject);
        subjectDao.insert(subject);
    }

    public void updateSubject(Subject subject) throws ValidationException, SQLException {
        if (subject.getId() == null) {
            throw new ValidationException(
                    "La materia no tiene id, por lo que no se puede actualizar.");
        }
        validate(subject);
        subject.setName(capitalize(subject.getName()));
        checkNameNotRepeated(subject);
        if (!subjectDao.update(subject)) {
            throw new ValidationException(
                    "No se encontró la materia con id " + subject.getId() + ".");
        }
    }

    public void setActive(int id, boolean active) throws ValidationException, SQLException {
        Subject subject = subjectDao.findById(id).orElseThrow(
                () -> new ValidationException("No se encontró la materia con id " + id + "."));
        subject.setActive(active);
        subjectDao.update(subject);
    }

    public void deleteSubject(int id) throws ValidationException, SQLException {
        if (!subjectDao.deleteById(id)) {
            throw new ValidationException("No se encontró la materia con id " + id + ".");
        }
    }

    public Optional<Subject> findById(int id) throws SQLException {
        return subjectDao.findById(id);
    }

    public Optional<Subject> findByCode(String code) throws SQLException {
        return subjectDao.findByCode(code);
    }

    public List<Subject> listSubjects() throws SQLException {
        return subjectDao.findAll();
    }

    public List<Subject> listSubjectsOfGrade(int gradeId) throws SQLException {
        return subjectDao.findByGradeId(gradeId);
    }

    public List<Grade> listGrades() throws SQLException {
        return gradeDao.findAll();
    }

    private Grade findExistingGrade(Grade grade) throws ValidationException, SQLException {
        if (grade == null || grade.getId() == null) {
            throw new ValidationException("El grado es obligatorio.");
        }
        return gradeDao.findById(grade.getId()).orElseThrow(
                () -> new ValidationException("El grado seleccionado no existe."));
    }

    private void validate(Subject subject) throws ValidationException {
        if (subject.getName() == null || subject.getName().isBlank()) {
            throw new ValidationException("El nombre de la materia es obligatorio.");
        }
        FieldValidator.validateMaxLength(subject.getName(), "El nombre de la materia",
                MAX_NAME_LENGTH);
        FieldValidator.validateMaxLength(subject.getDescription(), "La descripción",
                MAX_DESCRIPTION_LENGTH);
        if (subject.getWeeklyHours() < MIN_WEEKLY_HOURS
                || subject.getWeeklyHours() > MAX_WEEKLY_HOURS) {
            throw new ValidationException("Las horas semanales deben estar entre "
                    + MIN_WEEKLY_HOURS + " y " + MAX_WEEKLY_HOURS + ".");
        }
    }

    private void checkNameNotRepeated(Subject subject) throws ValidationException, SQLException {
        boolean repeated = subjectDao.findByGradeId(subject.getGrade().getId()).stream()
                .anyMatch(other -> other.getName().equalsIgnoreCase(subject.getName())
                        && !Objects.equals(other.getId(), subject.getId()));
        if (repeated) {
            throw new ValidationException("Ya existe una materia llamada '" + subject.getName()
                    + "' en " + subject.getGrade().getName() + ".");
        }
    }

    /**
     * First letter in uppercase and the rest in lowercase ("educación FÍSICA" becomes
     * "Educación física"). The text is already validated as not blank.
     */
    private String capitalize(String text) {
        String trimmed = text.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }
}