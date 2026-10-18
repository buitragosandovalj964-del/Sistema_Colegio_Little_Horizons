package com.littlehorizons.erp.dao;

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
 * Data access for the asignaciones_profesor table.
 */
public class AssignmentDao {

    /**
     * An assignment as stored in the table, before loading teacher and subject.
     */
    public record AssignmentRow(int id, int teacherId, int subjectId, int academicYear,
            String section) { }

    private static final String SELECT_BASE_SQL =
            "SELECT id_asignacion, id_profesor, id_materia, año_academico AS academic_year, "
            + "seccion FROM asignaciones_profesor";

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + " WHERE id_asignacion = ?";

    private static final String SELECT_BY_TEACHER_SQL =
            SELECT_BASE_SQL + " WHERE id_profesor = ? ORDER BY año_academico DESC, id_asignacion";

    private static final String SELECT_BY_GROUP_SQL =
            SELECT_BASE_SQL + " WHERE id_materia = ? AND año_academico = ? AND seccion = ?";

    private static final String INSERT_SQL =
            "INSERT INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion) "
            + "VALUES (?, ?, ?, ?)";

    private static final String DELETE_SQL =
            "DELETE FROM asignaciones_profesor WHERE id_asignacion = ?";

    public int insert(int teacherId, int subjectId, int academicYear, String section)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, teacherId);
            statement.setInt(2, subjectId);
            statement.setInt(3, academicYear);
            statement.setString(4, section);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                return generatedKeys.next() ? generatedKeys.getInt(1) : 0;
            }
        }
    }

    public boolean deleteById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<AssignmentRow> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<AssignmentRow> findByTeacherId(int teacherId) throws SQLException {
        List<AssignmentRow> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_TEACHER_SQL)) {

            statement.setInt(1, teacherId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapRow(resultSet));
                }
            }
        }
        return rows;
    }

    /**
     * Finds who teaches a subject in a group (academic year and section), if anyone.
     */
    public Optional<AssignmentRow> findBySubjectYearSection(int subjectId, int academicYear,
            String section) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_GROUP_SQL)) {

            statement.setInt(1, subjectId);
            statement.setInt(2, academicYear);
            statement.setString(3, section);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    private AssignmentRow mapRow(ResultSet resultSet) throws SQLException {
        return new AssignmentRow(
                resultSet.getInt("id_asignacion"),
                resultSet.getInt("id_profesor"),
                resultSet.getInt("id_materia"),
                resultSet.getInt("academic_year"),
                resultSet.getString("seccion"));
    }
}