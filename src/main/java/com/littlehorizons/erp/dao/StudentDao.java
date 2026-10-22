package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the estudiantes table.
 */
public class StudentDao {

    private static final String SELECT_BASE_SQL =
            "SELECT id_estudiante, tipo_documento, numero_documento, primer_nombre, "
            + "segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, "
            + "direccion, telefono, correo_electronico, tipo_sangre, alergias, "
            + "condiciones_medicas, fecha_ingreso, estado FROM estudiantes";

    private static final String SELECT_ALL_SQL =
            SELECT_BASE_SQL + " ORDER BY primer_apellido, segundo_apellido, primer_nombre";

    private static final String SELECT_BY_ID_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ?";

    private static final String SELECT_BY_DOCUMENT_SQL =
            SELECT_BASE_SQL + " WHERE tipo_documento = ? AND numero_documento = ?";

    private static final String SELECT_BY_DOCUMENT_NUMBER_SQL =
            SELECT_BASE_SQL + " WHERE numero_documento = ?";

    private static final String INSERT_SQL =
            "INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, "
            + "segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, "
            + "direccion, telefono, correo_electronico, tipo_sangre, alergias, "
            + "condiciones_medicas, fecha_ingreso, estado) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE estudiantes SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, "
            + "segundo_apellido = ?, fecha_nacimiento = ?, genero = ?, direccion = ?, "
            + "telefono = ?, correo_electronico = ?, tipo_sangre = ?, alergias = ?, "
            + "condiciones_medicas = ?, fecha_ingreso = ?, estado = ? "
            + "WHERE id_estudiante = ?";

    private static final String DELETE_SQL =
            "DELETE FROM estudiantes WHERE id_estudiante = ?";

    public void insert(Student student) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            insert(connection, student);
        }
    }

    public void insert(Connection connection, Student student) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setStudentParameters(statement, student);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    student.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public boolean update(Student student) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return update(connection, student);
        }
    }

    public boolean update(Connection connection, Student student) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            setStudentParameters(statement, student);
            statement.setInt(17, student.getId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<Student> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<Student> findByDocument(String documentType, String documentNumber)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_DOCUMENT_SQL)) {

            statement.setString(1, documentType);
            statement.setString(2, documentNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<Student> findByDocumentNumber(String documentNumber) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_BY_DOCUMENT_NUMBER_SQL)) {
            statement.setString(1, documentNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<Student> findAll() throws SQLException {
        List<Student> students = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                students.add(mapRow(resultSet));
            }
        }
        return students;
    }

    private void setStudentParameters(PreparedStatement statement, Student student)
            throws SQLException {
        statement.setString(1, student.getDocumentType());
        statement.setString(2, student.getDocumentNumber());
        statement.setString(3, student.getFirstName());
        statement.setString(4, student.getMiddleName());
        statement.setString(5, student.getFirstLastName());
        statement.setString(6, student.getSecondLastName());
        statement.setDate(7, Date.valueOf(student.getBirthDate()));
        statement.setString(8, student.getGender());
        statement.setString(9, student.getAddress());
        statement.setString(10, student.getPhone());
        statement.setString(11, student.getEmail());
        statement.setString(12, student.getBloodType());
        statement.setString(13, student.getAllergies());
        statement.setString(14, student.getMedicalConditions());
        statement.setDate(15, Date.valueOf(student.getAdmissionDate()));
        statement.setString(16, student.getStatus());
    }

    private Student mapRow(ResultSet resultSet) throws SQLException {
        Student student = new Student();
        student.setId(resultSet.getInt("id_estudiante"));
        student.setDocumentType(resultSet.getString("tipo_documento"));
        student.setDocumentNumber(resultSet.getString("numero_documento"));
        student.setFirstName(resultSet.getString("primer_nombre"));
        student.setMiddleName(resultSet.getString("segundo_nombre"));
        student.setFirstLastName(resultSet.getString("primer_apellido"));
        student.setSecondLastName(resultSet.getString("segundo_apellido"));
        student.setBirthDate(resultSet.getDate("fecha_nacimiento").toLocalDate());
        student.setGender(resultSet.getString("genero"));
        student.setAddress(resultSet.getString("direccion"));
        student.setPhone(resultSet.getString("telefono"));
        student.setEmail(resultSet.getString("correo_electronico"));
        student.setBloodType(resultSet.getString("tipo_sangre"));
        student.setAllergies(resultSet.getString("alergias"));
        student.setMedicalConditions(resultSet.getString("condiciones_medicas"));
        student.setAdmissionDate(resultSet.getDate("fecha_ingreso").toLocalDate());
        student.setStatus(resultSet.getString("estado"));
        return student;
    }
}