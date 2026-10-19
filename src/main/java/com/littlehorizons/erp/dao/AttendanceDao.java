package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;
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
 * Data access for the asistencias table.
 */
public class AttendanceDao {

    /**
     * An attendance record as stored in the table, before loading student, subject and teacher.
     */
    public record AttendanceRow(int id, int studentId, int subjectId, Integer teacherId,
            LocalDate date, String status, String notes) { }

    private static final String SELECT_BASE_SQL =
            "SELECT id_asistencia, id_estudiante, id_materia, fecha_asistencia, "
            + "estado_asistencia, observaciones, registrado_por FROM asistencias";

    private static final String SELECT_BY_KEY_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? AND id_materia = ? "
            + "AND fecha_asistencia = ?";

    private static final String SELECT_BY_STUDENT_YEAR_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? AND YEAR(fecha_asistencia) = ? "
            + "ORDER BY fecha_asistencia DESC, id_materia";

    private static final String SELECT_BY_COURSE_DATE_SQL =
            SELECT_BASE_SQL + " WHERE id_materia = ? AND fecha_asistencia = ? AND EXISTS ("
            + "SELECT 1 FROM inscripciones_materia i "
            + "WHERE i.id_estudiante = asistencias.id_estudiante "
            + "AND i.id_materia = asistencias.id_materia "
            + "AND i.año_academico = YEAR(asistencias.fecha_asistencia) "
            + "AND i.seccion = ?) ORDER BY id_estudiante";

    private static final String SELECT_ABSENCES_SQL =
            SELECT_BASE_SQL + " WHERE estado_asistencia = 'Ausente' "
            + "AND fecha_asistencia BETWEEN ? AND ? ORDER BY fecha_asistencia";

    private static final String INSERT_SQL =
            "INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, "
            + "estado_asistencia, observaciones, registrado_por) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE asistencias SET estado_asistencia = ?, observaciones = ? "
            + "WHERE id_asistencia = ?";

    private static final String DELETE_SQL = "DELETE FROM asistencias WHERE id_asistencia = ?";

    /**
     * Inserts all the records in one transaction: either all are saved or none.
     */
    public void insertAll(List<Attendance> records) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
                for (Attendance record : records) {
                    statement.setInt(1, record.getStudent().getId());
                    statement.setInt(2, record.getSubject().getId());
                    statement.setDate(3, Date.valueOf(record.getDate()));
                    statement.setString(4, record.getStatus());
                    statement.setString(5, record.getNotes());
                    statement.setInt(6, record.getRegisteredBy().getId());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public boolean update(int id, String status, String notes) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            statement.setString(1, status);
            statement.setString(2, notes);
            statement.setInt(3, id);
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

    public Optional<AttendanceRow> findByKey(int studentId, int subjectId, LocalDate date)
            throws SQLException {
        return query(SELECT_BY_KEY_SQL, studentId, subjectId, Date.valueOf(date))
                .stream().findFirst();
    }

    public List<AttendanceRow> findByStudentAndYear(int studentId, int academicYear)
            throws SQLException {
        return query(SELECT_BY_STUDENT_YEAR_SQL, studentId, academicYear);
    }

    public List<AttendanceRow> findByCourseAndDate(int subjectId, LocalDate date,
            String section) throws SQLException {
        return query(SELECT_BY_COURSE_DATE_SQL, subjectId, Date.valueOf(date), section);
    }

    public List<AttendanceRow> findAbsencesBetween(LocalDate from, LocalDate to)
            throws SQLException {
        return query(SELECT_ABSENCES_SQL, Date.valueOf(from), Date.valueOf(to));
    }

    /**
     * Runs a select with the given parameters (integers, strings or dates) and maps every row.
     */
    private List<AttendanceRow> query(String sql, Object... parameters) throws SQLException {
        List<AttendanceRow> rows = new ArrayList<>();
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

    private AttendanceRow mapRow(ResultSet resultSet) throws SQLException {
        int teacher = resultSet.getInt("registrado_por");
        Integer teacherId = resultSet.wasNull() ? null : teacher;
        return new AttendanceRow(
                resultSet.getInt("id_asistencia"),
                resultSet.getInt("id_estudiante"),
                resultSet.getInt("id_materia"),
                teacherId,
                resultSet.getDate("fecha_asistencia").toLocalDate(),
                resultSet.getString("estado_asistencia"),
                resultSet.getString("observaciones"));
    }
}