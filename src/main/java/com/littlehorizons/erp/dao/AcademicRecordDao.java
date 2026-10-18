package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.util.DatabaseConnection;
import java.math.BigDecimal;
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
 * Data access for the registros_academicos table.
 */
public class AcademicRecordDao {

    /**
     * A grade as stored in the table, before loading student, subject and teacher.
     */
    public record RecordRow(int id, int studentId, int subjectId, int teacherId,
            int academicYear, String period, BigDecimal value, String notes,
            LocalDate recordDate) { }

    private static final String SELECT_BASE_SQL =
            "SELECT id_registro, id_estudiante, id_materia, id_profesor, "
            + "año_academico AS academic_year, periodo_academico, valor_nota, observaciones, "
            + "fecha_registro FROM registros_academicos";

    private static final String SELECT_BY_KEY_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? AND id_materia = ? "
            + "AND año_academico = ? AND periodo_academico = ?";

    private static final String SELECT_BY_STUDENT_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? "
            + "ORDER BY año_academico DESC, id_materia, periodo_academico";

    private static final String SELECT_BY_STUDENT_YEAR_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? AND año_academico = ? "
            + "ORDER BY id_materia, periodo_academico";

    private static final String SELECT_BY_COURSE_SQL =
            SELECT_BASE_SQL + " WHERE id_materia = ? AND año_academico = ? AND EXISTS ("
            + "SELECT 1 FROM inscripciones_materia i "
            + "WHERE i.id_estudiante = registros_academicos.id_estudiante "
            + "AND i.id_materia = registros_academicos.id_materia "
            + "AND i.año_academico = registros_academicos.año_academico "
            + "AND i.seccion = ?) ORDER BY id_estudiante, periodo_academico";

    private static final String INSERT_SQL =
            "INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, "
            + "año_academico, periodo_academico, valor_nota, observaciones, fecha_registro) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE registros_academicos SET valor_nota = ?, observaciones = ?, "
            + "fecha_registro = ? WHERE id_registro = ?";

    private static final String DELETE_SQL =
            "DELETE FROM registros_academicos WHERE id_registro = ?";

    public int insert(int studentId, int subjectId, int teacherId, int academicYear,
            String period, BigDecimal value, String notes, LocalDate recordDate)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, studentId);
            statement.setInt(2, subjectId);
            statement.setInt(3, teacherId);
            statement.setInt(4, academicYear);
            statement.setString(5, period);
            statement.setBigDecimal(6, value);
            statement.setString(7, notes);
            statement.setDate(8, Date.valueOf(recordDate));
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                return generatedKeys.next() ? generatedKeys.getInt(1) : 0;
            }
        }
    }

    public boolean update(int id, BigDecimal value, String notes, LocalDate recordDate)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            statement.setBigDecimal(1, value);
            statement.setString(2, notes);
            statement.setDate(3, Date.valueOf(recordDate));
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

    public Optional<RecordRow> findByKey(int studentId, int subjectId, int academicYear,
            String period) throws SQLException {
        return query(SELECT_BY_KEY_SQL, studentId, subjectId, academicYear, period)
                .stream().findFirst();
    }

    public List<RecordRow> findByStudent(int studentId) throws SQLException {
        return query(SELECT_BY_STUDENT_SQL, studentId);
    }

    public List<RecordRow> findByStudentAndYear(int studentId, int academicYear)
            throws SQLException {
        return query(SELECT_BY_STUDENT_YEAR_SQL, studentId, academicYear);
    }

    /**
     * Grades of a course: a subject in a year for the students of one section.
     */
    public List<RecordRow> findByCourse(int subjectId, int academicYear, String section)
            throws SQLException {
        return query(SELECT_BY_COURSE_SQL, subjectId, academicYear, section);
    }

    /**
     * Runs a select with the given parameters (integers or strings) and maps every row.
     */
    private List<RecordRow> query(String sql, Object... parameters) throws SQLException {
        List<RecordRow> rows = new ArrayList<>();
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

    private RecordRow mapRow(ResultSet resultSet) throws SQLException {
        return new RecordRow(
                resultSet.getInt("id_registro"),
                resultSet.getInt("id_estudiante"),
                resultSet.getInt("id_materia"),
                resultSet.getInt("id_profesor"),
                resultSet.getInt("academic_year"),
                resultSet.getString("periodo_academico"),
                resultSet.getBigDecimal("valor_nota"),
                resultSet.getString("observaciones"),
                resultSet.getDate("fecha_registro").toLocalDate());
    }
}