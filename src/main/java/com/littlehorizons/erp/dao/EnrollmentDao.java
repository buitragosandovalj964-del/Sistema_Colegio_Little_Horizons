package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the matriculas table.
 */
public class EnrollmentDao {

    /**
     * An enrollment as stored in the table, before loading student and grade.
     */
    public record EnrollmentRow(int id, int studentId, int gradeId, int academicYear,
            String status, LocalDate enrollmentDate, LocalDate withdrawalDate, String notes) { }

    private static final String SELECT_BASE_SQL =
            "SELECT id_matricula, id_estudiante, id_grado, año_academico AS academic_year, "
            + "estado_matricula, fecha_matricula, fecha_retiro, observaciones FROM matriculas";

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + " WHERE id_matricula = ?";

    private static final String SELECT_BY_STUDENT_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? ORDER BY año_academico DESC";

    private static final String SELECT_BY_STUDENT_YEAR_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? AND año_academico = ?";

    private static final String SELECT_BY_GRADE_YEAR_SQL =
            SELECT_BASE_SQL + " WHERE id_grado = ? AND año_academico = ? ORDER BY id_matricula";

    private static final String INSERT_SQL =
            "INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, "
            + "fecha_matricula, observaciones) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_STATUS_SQL =
            "UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ?, "
            + "observaciones = COALESCE(?, observaciones) WHERE id_matricula = ?";

    private static final String DELETE_SQL = "DELETE FROM matriculas WHERE id_matricula = ?";

    public int insert(int studentId, int gradeId, int academicYear, String status,
            LocalDate enrollmentDate, String notes) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, studentId);
            statement.setInt(2, gradeId);
            statement.setInt(3, academicYear);
            statement.setString(4, status);
            statement.setDate(5, Date.valueOf(enrollmentDate));
            statement.setString(6, notes);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                return generatedKeys.next() ? generatedKeys.getInt(1) : 0;
            }
        }
    }

    /**
     * Changes the status. The notes are replaced only when a new text is given.
     */
    public boolean updateStatus(int id, String status, LocalDate withdrawalDate, String notes)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return updateStatus(connection, id, status, withdrawalDate, notes);
        }
    }

    public boolean updateStatus(Connection connection, int id, String status,
            LocalDate withdrawalDate, String notes) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {
            statement.setString(1, status);
            statement.setDate(2, withdrawalDate == null ? null : Date.valueOf(withdrawalDate));
            statement.setString(3, notes);
            statement.setInt(4, id);
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

    public Optional<EnrollmentRow> findById(int id) throws SQLException {
        return query(SELECT_BY_ID_SQL, id).stream().findFirst();
    }

    public List<EnrollmentRow> findByStudentId(int studentId) throws SQLException {
        return query(SELECT_BY_STUDENT_SQL, studentId);
    }

    public Optional<EnrollmentRow> findByStudentAndYear(int studentId, int academicYear)
            throws SQLException {
        return query(SELECT_BY_STUDENT_YEAR_SQL, studentId, academicYear).stream().findFirst();
    }

    public List<EnrollmentRow> findByGradeAndYear(int gradeId, int academicYear)
            throws SQLException {
        return query(SELECT_BY_GRADE_YEAR_SQL, gradeId, academicYear);
    }

    /**
     * Runs a select with the given integer parameters and maps every row.
     */
    private List<EnrollmentRow> query(String sql, int... parameters) throws SQLException {
        List<EnrollmentRow> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int index = 0; index < parameters.length; index++) {
                statement.setInt(index + 1, parameters[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapRow(resultSet));
                }
            }
        }
        return rows;
    }

    private EnrollmentRow mapRow(ResultSet resultSet) throws SQLException {
        Date withdrawal = resultSet.getDate("fecha_retiro");
        return new EnrollmentRow(
                resultSet.getInt("id_matricula"),
                resultSet.getInt("id_estudiante"),
                resultSet.getInt("id_grado"),
                resultSet.getInt("academic_year"),
                resultSet.getString("estado_matricula"),
                resultSet.getDate("fecha_matricula").toLocalDate(),
                withdrawal == null ? null : withdrawal.toLocalDate(),
                resultSet.getString("observaciones"));
    }
}