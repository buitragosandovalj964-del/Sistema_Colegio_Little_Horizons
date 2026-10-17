package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Teacher;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for teachers. Validates data before it reaches the database.
 * User-facing messages are in Spanish.
 */
public class TeacherService {

    public static final List<String> DOCUMENT_TYPES = List.of("CC", "CE", "Pasaporte");
    public static final List<String> STATUSES = List.of("Activo", "Inactivo", "Licencia");

    private static final String STATUS_ACTIVE = "Activo";

    private final TeacherDao teacherDao;

    public TeacherService() {
        this(new TeacherDao());
    }

    public TeacherService(TeacherDao teacherDao) {
        this.teacherDao = teacherDao;
    }

    public void registerTeacher(Teacher teacher) throws ValidationException, SQLException {
        if (teacher.getStatus() == null || teacher.getStatus().isBlank()) {
            teacher.setStatus(STATUS_ACTIVE);
        }
        validate(teacher);
        if (teacherDao.findByDocument(teacher.getDocumentType(),
                teacher.getDocumentNumber()).isPresent()) {
            throw new ValidationException("Ya existe un profesor con el documento "
                    + teacher.getDocumentType() + " " + teacher.getDocumentNumber() + ".");
        }
        if (teacherDao.findByEmail(teacher.getEmail()).isPresent()) {
            throw new ValidationException(
                    "Ya existe un profesor con el correo " + teacher.getEmail() + ".");
        }
        teacherDao.insert(teacher);
    }

    public void updateTeacher(Teacher teacher) throws ValidationException, SQLException {
        if (teacher.getId() == null) {
            throw new ValidationException(
                    "El profesor no tiene id, por lo que no se puede actualizar.");
        }
        validate(teacher);
        Optional<Teacher> sameDocument = teacherDao.findByDocument(
                teacher.getDocumentType(), teacher.getDocumentNumber());
        if (sameDocument.isPresent() && !sameDocument.get().getId().equals(teacher.getId())) {
            throw new ValidationException("Otro profesor ya usa el documento "
                    + teacher.getDocumentType() + " " + teacher.getDocumentNumber() + ".");
        }
        Optional<Teacher> sameEmail = teacherDao.findByEmail(teacher.getEmail());
        if (sameEmail.isPresent() && !sameEmail.get().getId().equals(teacher.getId())) {
            throw new ValidationException(
                    "Otro profesor ya usa el correo " + teacher.getEmail() + ".");
        }
        if (!teacherDao.update(teacher)) {
            throw new ValidationException(
                    "No se encontró el profesor con id " + teacher.getId() + ".");
        }
    }

    public void changeStatus(int id, String status) throws ValidationException, SQLException {
        FieldValidator.requireOneOf(status, STATUSES, "El estado");
        Teacher teacher = teacherDao.findById(id).orElseThrow(
                () -> new ValidationException("No se encontró el profesor con id " + id + "."));
        teacher.setStatus(status);
        teacherDao.update(teacher);
    }

    public void deleteTeacher(int id) throws ValidationException, SQLException {
        boolean deleted;
        try {
            deleted = teacherDao.deleteById(id);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("No se puede eliminar al profesor porque tiene "
                    + "registros asociados (calificaciones, clubes o eventos). "
                    + "Cambie su estado a Inactivo.");
        }
        if (!deleted) {
            throw new ValidationException("No se encontró el profesor con id " + id + ".");
        }
    }

    public Optional<Teacher> findById(int id) throws SQLException {
        return teacherDao.findById(id);
    }

    public Optional<Teacher> findByDocument(String documentType, String documentNumber)
            throws SQLException {
        return teacherDao.findByDocument(documentType, documentNumber);
    }

    public List<Teacher> listTeachers() throws SQLException {
        return teacherDao.findAll();
    }

    private void validate(Teacher teacher) throws ValidationException {
        FieldValidator.requireOneOf(teacher.getDocumentType(), DOCUMENT_TYPES,
                "El tipo de documento");
        FieldValidator.validateDocumentNumber(teacher.getDocumentNumber());
        FieldValidator.validateName(teacher.getFirstName(), "El primer nombre", true);
        FieldValidator.validateName(teacher.getMiddleName(), "El segundo nombre", false);
        FieldValidator.validateName(teacher.getFirstLastName(), "El primer apellido", true);
        FieldValidator.validateName(teacher.getSecondLastName(), "El segundo apellido", false);
        if (teacher.getEmail() == null || teacher.getEmail().isBlank()) {
            throw new ValidationException("El correo electrónico es obligatorio.");
        }
        FieldValidator.validateEmail(teacher.getEmail());
        FieldValidator.validatePhone(teacher.getPhone(), "El teléfono", false);
        FieldValidator.validateMaxLength(teacher.getSpecialization(), "La especialización", 100);
        FieldValidator.requireOneOf(teacher.getStatus(), STATUSES, "El estado");
        validateHireDate(teacher.getHireDate());
    }

    private void validateHireDate(LocalDate hireDate) throws ValidationException {
        if (hireDate == null) {
            throw new ValidationException("La fecha de contratación es obligatoria.");
        }
        if (hireDate.isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de contratación no puede ser futura.");
        }
    }
}