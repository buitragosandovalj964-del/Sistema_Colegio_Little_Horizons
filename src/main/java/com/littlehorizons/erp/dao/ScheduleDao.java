package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the horarios_clase table.
 */
public class ScheduleDao {

    private static final String SELECT_BASE_SQL =
            "SELECT h.id_horario, h.id_asignacion, h.dia_semana, h.hora_inicio, h.hora_fin "
            + "FROM horarios_clase h";

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + " WHERE h.id_horario = ?";

    private static final String SELECT_BY_ASSIGNMENT_SQL =
            SELECT_BASE_SQL + " WHERE h.id_asignacion = ? ORDER BY h.dia_semana, h.hora_inicio";

    private static final String SELECT_TEACHER_SLOTS_SQL =
            SELECT_BASE_SQL + " JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion "
            + "WHERE a.id_profesor = ? AND a.año_academico = ? AND h.dia_semana = ?";

    private static final String SELECT_GROUP_SLOTS_SQL =
            SELECT_BASE_SQL + " JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion "
            + "JOIN materias m ON m.id_materia = a.id_materia "
            + "WHERE m.id_grado = ? AND a.año_academico = ? AND a.seccion = ? "
            + "AND h.dia_semana = ?";

    private static final String INSERT_SQL =
            "INSERT INTO horarios_clase (id_asignacion, dia_semana, hora_inicio, hora_fin) "
            + "VALUES (?, ?, ?, ?)";

    private static final String DELETE_SQL = "DELETE FROM horarios_clase WHERE id_horario = ?";

    public void insert(int assignmentId, String day, LocalTime startTime, LocalTime endTime)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {

            statement.setInt(1, assignmentId);
            statement.setString(2, day);
            statement.setTime(3, Time.valueOf(startTime));
            statement.setTime(4, Time.valueOf(endTime));
            statement.executeUpdate();
        }
    }

    public boolean deleteById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<ScheduleSlot> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<ScheduleSlot> findByAssignmentId(int assignmentId) throws SQLException {
        return query(SELECT_BY_ASSIGNMENT_SQL, assignmentId);
    }

    public List<ScheduleSlot> findTeacherSlots(int teacherId, int academicYear, String day)
            throws SQLException {
        return query(SELECT_TEACHER_SLOTS_SQL, teacherId, academicYear, day);
    }

    public List<ScheduleSlot> findGroupSlots(int gradeId, int academicYear, String section,
            String day) throws SQLException {
        return query(SELECT_GROUP_SLOTS_SQL, gradeId, academicYear, section, day);
    }

    /**
     * Runs a select with the given parameters (integers or strings) and maps every row.
     */
    private List<ScheduleSlot> query(String sql, Object... parameters) throws SQLException {
        List<ScheduleSlot> slots = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    slots.add(mapRow(resultSet));
                }
            }
        }
        return slots;
    }

    private ScheduleSlot mapRow(ResultSet resultSet) throws SQLException {
        ScheduleSlot slot = new ScheduleSlot();
        slot.setId(resultSet.getInt("id_horario"));
        slot.setAssignmentId(resultSet.getInt("id_asignacion"));
        slot.setDay(resultSet.getString("dia_semana"));
        slot.setStartTime(resultSet.getTime("hora_inicio").toLocalTime());
        slot.setEndTime(resultSet.getTime("hora_fin").toLocalTime());
        return slot;
    }
}