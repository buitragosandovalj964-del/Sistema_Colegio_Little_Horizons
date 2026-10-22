package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the inscripciones_materia table.
 */
public class SubjectEnrollmentDao {

    /**
     * An enrollment as stored in the table, before loading student and subject.
     */
    public record InscriptionRow(int id, int studentId, int subjectId, int academicYear,
            String period, String section, LocalDate enrollmentDate, String status) { }

    private static final String SELECT_BASE_SQL =
            "SELECT id_inscripcion, id_estudiante, id_materia, año_academico AS academic_year, "
            + "periodo_academico, seccion, fecha_inscripcion, estado FROM inscripciones_materia";

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + " WHERE id_inscripcion = ?";

    private static final String SELECT_BY_STUDENT_YEAR_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? AND año_academico = ? "
            + "ORDER BY periodo_academico, id_materia";

    private static final String SELECT_ROSTER_SQL =
            SELECT_BASE_SQL + " WHERE id_materia = ? AND año_academico = ? "
            + "AND periodo_academico = ? AND seccion = ? ORDER BY id_estudiante";

    private static final String COUNT_BY_STUDENT_YEAR_SQL =
            "SELECT COUNT(*) FROM inscripciones_materia "
            + "WHERE id_estudiante = ? AND año_academico = ?";

    private static final String INSERT_SQL =
            "INSERT INTO inscripciones_materia (id_estudiante, id_materia, año_academico, "
            + "periodo_academico, seccion, fecha_inscripcion) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_STATUS_SQL =
            "UPDATE inscripciones_materia SET estado = ? WHERE id_inscripcion = ?";

    private static final String UPDATE_STATUS_FOR_YEAR_SQL =
            "UPDATE inscripciones_materia SET estado = ? "
            + "WHERE id_estudiante = ? AND año_academico = ? AND estado = 'Activa'";

    private static final String DELETE_SQL =
            "DELETE FROM inscripciones_materia WHERE id_inscripcion = ?";

    /**
     * Inserts one row per period in a single transaction: either all are saved or none.
     */
    public void insertForPeriods(int studentId, int subjectId, int academicYear,
            List<String> periods, String section, LocalDate enrollmentDate)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
                for (String period : periods) {
                    statement.setInt(1, studentId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, academicYear);
                    statement.setString(4, period);
                    statement.setString(5, section);
                    statement.setDate(6, Date.valueOf(enrollmentDate));
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public boolean updateStatus(int id, String status) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {

            statement.setString(1, status);
            statement.setInt(2, id);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Changes the status of every active enrollment of a student in a year.
     */
    public int updateStatusForYear(int studentId, int academicYear, String status)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return updateStatusForYear(connection, studentId, academicYear, status);
        }
    }

    public int updateStatusForYear(Connection connection, int studentId, int academicYear,
            String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                UPDATE_STATUS_FOR_YEAR_SQL)) {
            statement.setString(1, status);
            statement.setInt(2, studentId);
            statement.setInt(3, academicYear);
            return statement.executeUpdate();
        }
    }

    public boolean deleteById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public int countByStudentAndYear(int studentId, int academicYear) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     COUNT_BY_STUDENT_YEAR_SQL)) {

            statement.setInt(1, studentId);
            statement.setInt(2, academicYear);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    public Optional<InscriptionRow> findById(int id) throws SQLException {
        return query(SELECT_BY_ID_SQL, id).stream().findFirst();
    }

    public List<InscriptionRow> findByStudentAndYear(int studentId, int academicYear)
            throws SQLException {
        return query(SELECT_BY_STUDENT_YEAR_SQL, studentId, academicYear);
    }

    public List<InscriptionRow> findRoster(int subjectId, int academicYear, String period,
            String section) throws SQLException {
        return query(SELECT_ROSTER_SQL, subjectId, academicYear, period, section);
    }

    /**
     * Runs a select with the given parameters (integers or strings) and maps every row.
     */
    private List<InscriptionRow> query(String sql, Object... parameters) throws SQLException {
        List<InscriptionRow> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapRow(resultSet));
                }
            }
        }
        return rows;
    }

    private InscriptionRow mapRow(ResultSet resultSet) throws SQLException {
        return new InscriptionRow(
                resultSet.getInt("id_inscripcion"),
                resultSet.getInt("id_estudiante"),
                resultSet.getInt("id_materia"),
                resultSet.getInt("academic_year"),
                resultSet.getString("periodo_academico"),
                resultSet.getString("seccion"),
                resultSet.getDate("fecha_inscripcion").toLocalDate(),
                resultSet.getString("estado"));
    }
}