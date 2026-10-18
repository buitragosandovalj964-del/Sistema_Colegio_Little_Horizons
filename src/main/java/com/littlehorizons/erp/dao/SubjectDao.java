package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the materias table. Queries join the grade so that each
 * subject comes with its grade data.
 */
public class SubjectDao {

    private static final String SELECT_BASE_SQL =
            "SELECT m.id_materia, m.codigo_materia, m.nombre_materia, m.descripcion, "
            + "m.horas_semanales, m.esta_activa, g.id_grado, g.nombre_grado, g.nivel_numerico "
            + "FROM materias m JOIN grados g ON g.id_grado = m.id_grado";

    private static final String SELECT_ALL_SQL =
            SELECT_BASE_SQL + " ORDER BY g.nivel_numerico, m.nombre_materia";

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + " WHERE m.id_materia = ?";

    private static final String SELECT_BY_CODE_SQL =
            SELECT_BASE_SQL + " WHERE m.codigo_materia = ?";

    private static final String SELECT_BY_GRADE_SQL =
            SELECT_BASE_SQL + " WHERE g.id_grado = ? ORDER BY m.nombre_materia";

    private static final String INSERT_SQL =
            "INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, "
            + "horas_semanales, esta_activa) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE materias SET nombre_materia = ?, descripcion = ?, horas_semanales = ?, "
            + "esta_activa = ? WHERE id_materia = ?";

    private static final String DELETE_SQL = "DELETE FROM materias WHERE id_materia = ?";

    public void insert(Subject subject) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, subject.getCode());
            statement.setString(2, subject.getName());
            statement.setString(3, subject.getDescription());
            statement.setInt(4, subject.getGrade().getId());
            statement.setInt(5, subject.getWeeklyHours());
            statement.setBoolean(6, subject.isActive());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    subject.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    /**
     * Updates name, description, weekly hours and active flag. The code and the
     * grade are not editable.
     */
    public boolean update(Subject subject) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            statement.setString(1, subject.getName());
            statement.setString(2, subject.getDescription());
            statement.setInt(3, subject.getWeeklyHours());
            statement.setBoolean(4, subject.isActive());
            statement.setInt(5, subject.getId());
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

    public Optional<Subject> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<Subject> findByCode(String code) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_CODE_SQL)) {

            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<Subject> findByGradeId(int gradeId) throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_GRADE_SQL)) {

            statement.setInt(1, gradeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    subjects.add(mapRow(resultSet));
                }
            }
        }
        return subjects;
    }

    public List<Subject> findAll() throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                subjects.add(mapRow(resultSet));
            }
        }
        return subjects;
    }

    private Subject mapRow(ResultSet resultSet) throws SQLException {
        Grade grade = new Grade();
        grade.setId(resultSet.getInt("id_grado"));
        grade.setName(resultSet.getString("nombre_grado"));
        grade.setNumericLevel(resultSet.getInt("nivel_numerico"));

        Subject subject = new Subject();
        subject.setId(resultSet.getInt("id_materia"));
        subject.setCode(resultSet.getString("codigo_materia"));
        subject.setName(resultSet.getString("nombre_materia"));
        subject.setDescription(resultSet.getString("descripcion"));
        subject.setWeeklyHours(resultSet.getInt("horas_semanales"));
        subject.setActive(resultSet.getBoolean("esta_activa"));
        subject.setGrade(grade);
        return subject;
    }
}