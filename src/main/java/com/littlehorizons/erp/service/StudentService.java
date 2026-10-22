package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Student;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for students. Validates data before it reaches the database.
 * User-facing messages are in Spanish.
 */
public class StudentService {

    public static final List<String> DOCUMENT_TYPES = List.of("TI", "CC", "CE", "Pasaporte");
    public static final List<String> GENDERS = List.of("M", "F", "Otro");
    public static final int MAX_DOCUMENT_NUMBER_LENGTH = 10;
    public static final int PHONE_MIN_LENGTH = 7;
    public static final int PHONE_MAX_LENGTH = 10;

    private static final List<String> STATUSES =
            List.of("Activo", "Graduado", "Retirado", "Suspendido");
    private static final String STATUS_ACTIVE = "Activo";
    private static final String STATUS_WITHDRAWN = "Retirado";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final String DOCUMENT_NUMBER_PATTERN = "[A-Za-z0-9]+";
    private static final int MAX_NAME_LENGTH = 100;
    private static final String NAME_PATTERN = "\\p{L}+([ '\\-]\\p{L}+)*";

    private final StudentDao studentDao;

    public StudentService() {
        this(new StudentDao());
    }

    public StudentService(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    public void registerStudent(Student student) throws ValidationException, SQLException {
        validateForRegistration(student);
        studentDao.insert(student);
    }

    public void validateForRegistration(Student student)
            throws ValidationException, SQLException {
        if (student.getStatus() == null || student.getStatus().isBlank()) {
            student.setStatus(STATUS_ACTIVE);
        }
        validate(student);
        if (studentDao.findByDocumentNumber(student.getDocumentNumber()).isPresent()) {
            throw new ValidationException("Ya existe un estudiante con el número de documento "
                    + student.getDocumentNumber() + ".");
        }
    }

    public void updateStudent(Student student) throws ValidationException, SQLException {
        if (student.getId() == null) {
            throw new ValidationException(
                    "El estudiante no tiene id, por lo que no se puede actualizar.");
        }
        validate(student);
        Optional<Student> sameDocument = studentDao.findByDocumentNumber(
                student.getDocumentNumber());
        if (sameDocument.isPresent() && !sameDocument.get().getId().equals(student.getId())) {
            throw new ValidationException("Otro estudiante ya usa el número de documento "
                    + student.getDocumentNumber() + ".");
        }
        if (!studentDao.update(student)) {
            throw new ValidationException(
                    "No se encontró el estudiante con id " + student.getId() + ".");
        }
    }

    public void withdrawStudent(int id) throws ValidationException, SQLException {
        Student student = studentDao.findById(id).orElseThrow(
                () -> new ValidationException("No se encontró el estudiante con id " + id + "."));
        student.setStatus(STATUS_WITHDRAWN);
        studentDao.update(student);
    }

    public void deleteStudent(int id) throws ValidationException, SQLException {
        if (!studentDao.deleteById(id)) {
            throw new ValidationException("No se encontró el estudiante con id " + id + ".");
        }
    }

    public Optional<Student> findById(int id) throws SQLException {
        return studentDao.findById(id);
    }

    public Optional<Student> findByDocument(String documentType, String documentNumber)
            throws SQLException {
        return studentDao.findByDocument(documentType, documentNumber);
    }

    public List<Student> listStudents() throws SQLException {
        return studentDao.findAll();
    }

    private void validate(Student student) throws ValidationException {
        requireOneOf(student.getDocumentType(), DOCUMENT_TYPES, "El tipo de documento");
        requireText(student.getDocumentNumber(), "El número de documento",
                MAX_DOCUMENT_NUMBER_LENGTH);
        if (!student.getDocumentNumber().matches(DOCUMENT_NUMBER_PATTERN)) {
            throw new ValidationException(
                    "El número de documento solo puede contener letras y dígitos.");
        }
        validateName(student.getFirstName(), "El primer nombre", true);
        validateName(student.getMiddleName(), "El segundo nombre", false);
        validateName(student.getFirstLastName(), "El primer apellido", true);
        validateName(student.getSecondLastName(), "El segundo apellido", false);
        requireOneOf(student.getGender(), GENDERS, "El género");
        requireOneOf(student.getStatus(), STATUSES, "El estado");
        validateDates(student);
        validateEmail(student.getEmail());
        validatePhone(student.getPhone());
    }

    private void validateName(String value, String label, boolean required)
            throws ValidationException {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new ValidationException(label + " es obligatorio.");
            }
            return;
        }
        if (value.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(label + " debe tener máximo " + MAX_NAME_LENGTH
                    + " caracteres.");
        }
        if (!value.matches(NAME_PATTERN)) {
            throw new ValidationException(label
                    + " solo puede contener letras, espacios, guiones y apóstrofes.");
        }
    }

    private void validateDates(Student student) throws ValidationException {
        if (student.getBirthDate() == null) {
            throw new ValidationException("La fecha de nacimiento es obligatoria.");
        }
        if (student.getBirthDate().isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de nacimiento no puede ser futura.");
        }
        if (student.getAdmissionDate() == null) {
            throw new ValidationException("La fecha de ingreso es obligatoria.");
        }
        if (student.getAdmissionDate().isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de ingreso no puede ser futura.");
        }
        if (student.getAdmissionDate().isBefore(student.getBirthDate())) {
            throw new ValidationException(
                    "La fecha de ingreso no puede ser anterior a la fecha de nacimiento.");
        }
    }

    private void validateEmail(String email) throws ValidationException {
        if (email != null && !email.isBlank() && !email.matches(EMAIL_PATTERN)) {
            throw new ValidationException("El formato del correo electrónico no es válido.");
        }
    }

    private void validatePhone(String phone) throws ValidationException {
        if (phone == null || phone.isBlank()) {
            return;
        }
        boolean onlyDigits = phone.matches("\\d+");
        if (!onlyDigits || phone.length() < PHONE_MIN_LENGTH
                || phone.length() > PHONE_MAX_LENGTH) {
            throw new ValidationException("El teléfono debe tener solo números, entre "
                    + PHONE_MIN_LENGTH + " y " + PHONE_MAX_LENGTH + " dígitos.");
        }
    }

    private void requireText(String value, String label, int maxLength)
            throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(label + " es obligatorio.");
        }
        if (value.length() > maxLength) {
            throw new ValidationException(label + " debe tener máximo " + maxLength
                    + " caracteres.");
        }
    }

    private void requireOneOf(String value, List<String> allowed, String label)
            throws ValidationException {
        if (value == null || !allowed.contains(value)) {
            throw new ValidationException(label + " debe ser uno de: "
                    + String.join(", ", allowed) + ".");
        }
    }
}