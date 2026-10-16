package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Student;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for students. Validates data before it reaches the database.
 */
public class StudentService {

    private static final List<String> DOCUMENT_TYPES = List.of("TI", "CC", "CE", "Pasaporte");
    private static final List<String> GENDERS = List.of("M", "F", "Otro");
    private static final List<String> STATUSES =
            List.of("Activo", "Graduado", "Retirado", "Suspendido");

    private static final String STATUS_ACTIVE = "Activo";
    private static final String STATUS_WITHDRAWN = "Retirado";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final String DOCUMENT_NUMBER_PATTERN = "[A-Za-z0-9]+";

    private final StudentDao studentDao;

    public StudentService() {
        this(new StudentDao());
    }

    public StudentService(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    public void registerStudent(Student student) throws ValidationException, SQLException {
        if (student.getStatus() == null || student.getStatus().isBlank()) {
            student.setStatus(STATUS_ACTIVE);
        }
        validate(student);
        if (studentDao.findByDocument(student.getDocumentType(),
                student.getDocumentNumber()).isPresent()) {
            throw new ValidationException("A student with document "
                    + student.getDocumentType() + " " + student.getDocumentNumber()
                    + " already exists.");
        }
        studentDao.insert(student);
    }

    public void updateStudent(Student student) throws ValidationException, SQLException {
        if (student.getId() == null) {
            throw new ValidationException("The student has no id, so it cannot be updated.");
        }
        validate(student);
        Optional<Student> sameDocument = studentDao.findByDocument(
                student.getDocumentType(), student.getDocumentNumber());
        if (sameDocument.isPresent() && !sameDocument.get().getId().equals(student.getId())) {
            throw new ValidationException("Another student already uses document "
                    + student.getDocumentType() + " " + student.getDocumentNumber() + ".");
        }
        if (!studentDao.update(student)) {
            throw new ValidationException("Student with id " + student.getId() + " not found.");
        }
    }

    public void withdrawStudent(int id) throws ValidationException, SQLException {
        Student student = studentDao.findById(id).orElseThrow(
                () -> new ValidationException("Student with id " + id + " not found."));
        student.setStatus(STATUS_WITHDRAWN);
        studentDao.update(student);
    }

    public void deleteStudent(int id) throws ValidationException, SQLException {
        if (!studentDao.deleteById(id)) {
            throw new ValidationException("Student with id " + id + " not found.");
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
        requireOneOf(student.getDocumentType(), DOCUMENT_TYPES, "Document type");
        requireText(student.getDocumentNumber(), "Document number", 20);
        if (!student.getDocumentNumber().matches(DOCUMENT_NUMBER_PATTERN)) {
            throw new ValidationException("Document number can only contain letters and digits.");
        }
        requireText(student.getFirstName(), "First name", 100);
        requireText(student.getFirstLastName(), "First last name", 100);
        requireOneOf(student.getGender(), GENDERS, "Gender");
        requireOneOf(student.getStatus(), STATUSES, "Status");
        validateDates(student);
        validateEmail(student.getEmail());
    }

    private void validateDates(Student student) throws ValidationException {
        if (student.getBirthDate() == null) {
            throw new ValidationException("Birth date is required.");
        }
        if (student.getBirthDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Birth date cannot be in the future.");
        }
        if (student.getAdmissionDate() == null) {
            throw new ValidationException("Admission date is required.");
        }
        if (student.getAdmissionDate().isBefore(student.getBirthDate())) {
            throw new ValidationException("Admission date cannot be before the birth date.");
        }
    }

    private void validateEmail(String email) throws ValidationException {
        if (email != null && !email.isBlank() && !email.matches(EMAIL_PATTERN)) {
            throw new ValidationException("Email format is not valid.");
        }
    }

    private void requireText(String value, String label, int maxLength)
            throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(label + " is required.");
        }
        if (value.length() > maxLength) {
            throw new ValidationException(label + " must have at most " + maxLength
                    + " characters.");
        }
    }

    private void requireOneOf(String value, List<String> allowed, String label)
            throws ValidationException {
        if (value == null || !allowed.contains(value)) {
            throw new ValidationException(label + " must be one of: "
                    + String.join(", ", allowed) + ".");
        }
    }
}