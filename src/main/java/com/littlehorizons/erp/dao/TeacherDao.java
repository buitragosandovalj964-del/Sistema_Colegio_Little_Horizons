package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Teacher;
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
 * Data access for the profesores table.
 */
public class TeacherDao {

    private static final String SELECT_BASE_SQL =
            "SELECT id_profesor, tipo_documento, numero_documento, primer_nombre, "
            + "segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, "
            + "telefono, especializacion, fecha_contratacion, estado FROM profesores";

    private static final String SELECT_ALL_SQL =
            SELECT_BASE_SQL + " ORDER BY primer_apellido, segundo_apellido, primer_nombre";

    private static final String SELECT_BY_ID_SQL =
            SELECT_BASE_SQL + " WHERE id_profesor = ?";

    private static final String SELECT_BY_DOCUMENT_SQL =
            SELECT_BASE_SQL + " WHERE tipo_documento = ? AND numero_documento = ?";

    private static final String SELECT_BY_EMAIL_SQL =
            SELECT_BASE_SQL + " WHERE correo_electronico = ?";

    private static final String INSERT_SQL =
            "INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, "
            + "segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, "
            + "telefono, especializacion, fecha_contratacion, estado) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE profesores SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, "
            + "segundo_apellido = ?, correo_electronico = ?, telefono = ?, "
            + "especializacion = ?, fecha_contratacion = ?, estado = ? "
            + "WHERE id_profesor = ?";

    private static final String DELETE_SQL = "DELETE FROM profesores WHERE id_profesor = ?";

    public void insert(Teacher teacher) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setTeacherParameters(statement, teacher);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    teacher.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public boolean update(Teacher teacher) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            setTeacherParameters(statement, teacher);
            statement.setInt(12, teacher.getId());
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

    public Optional<Teacher> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<Teacher> findByDocument(String documentType, String documentNumber)
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

    public Optional<Teacher> findByEmail(String email) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_EMAIL_SQL)) {

            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<Teacher> findAll() throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                teachers.add(mapRow(resultSet));
            }
        }
        return teachers;
    }

    private void setTeacherParameters(PreparedStatement statement, Teacher teacher)
            throws SQLException {
        statement.setString(1, teacher.getDocumentType());
        statement.setString(2, teacher.getDocumentNumber());
        statement.setString(3, teacher.getFirstName());
        statement.setString(4, teacher.getMiddleName());
        statement.setString(5, teacher.getFirstLastName());
        statement.setString(6, teacher.getSecondLastName());
        statement.setString(7, teacher.getEmail());
        statement.setString(8, teacher.getPhone());
        statement.setString(9, teacher.getSpecialization());
        statement.setDate(10, Date.valueOf(teacher.getHireDate()));
        statement.setString(11, teacher.getStatus());
    }

    private Teacher mapRow(ResultSet resultSet) throws SQLException {
        Teacher teacher = new Teacher();
        teacher.setId(resultSet.getInt("id_profesor"));
        teacher.setDocumentType(resultSet.getString("tipo_documento"));
        teacher.setDocumentNumber(resultSet.getString("numero_documento"));
        teacher.setFirstName(resultSet.getString("primer_nombre"));
        teacher.setMiddleName(resultSet.getString("segundo_nombre"));
        teacher.setFirstLastName(resultSet.getString("primer_apellido"));
        teacher.setSecondLastName(resultSet.getString("segundo_apellido"));
        teacher.setEmail(resultSet.getString("correo_electronico"));
        teacher.setPhone(resultSet.getString("telefono"));
        teacher.setSpecialization(resultSet.getString("especializacion"));
        teacher.setHireDate(resultSet.getDate("fecha_contratacion").toLocalDate());
        teacher.setStatus(resultSet.getString("estado"));
        return teacher;
    }
}