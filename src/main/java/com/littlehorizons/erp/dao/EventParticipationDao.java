package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data access for the participantes_evento table.
 */
public class EventParticipationDao {

    /**
     * A participation as stored in the table, before loading event and student.
     */
    public record ParticipationRow(int id, int eventId, int studentId, String role,
            String position, String notes) { }

    private static final String SELECT_BASE_SQL =
            "SELECT id_participacion, id_evento, id_estudiante, rol_participacion, "
            + "posicion_obtenida, observaciones FROM participantes_evento";

    private static final String SELECT_ONE_SQL =
            SELECT_BASE_SQL + " WHERE id_evento = ? AND id_estudiante = ?";

    private static final String SELECT_BY_EVENT_SQL =
            SELECT_BASE_SQL + " WHERE id_evento = ? ORDER BY rol_participacion, id_participacion";

    private static final String SELECT_BY_STUDENT_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? ORDER BY id_participacion DESC";

    private static final String COUNT_BY_EVENT_SQL =
            "SELECT COUNT(*) FROM participantes_evento WHERE id_evento = ?";

    private static final String INSERT_SQL =
            "INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, "
            + "observaciones) VALUES (?, ?, ?, ?)";

    private static final String UPDATE_POSITION_SQL =
            "UPDATE participantes_evento SET posicion_obtenida = ? WHERE id_participacion = ?";

    private static final String DELETE_SQL =
            "DELETE FROM participantes_evento WHERE id_evento = ? AND id_estudiante = ?";

    public void insert(int eventId, int studentId, String role, String notes)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {

            statement.setInt(1, eventId);
            statement.setInt(2, studentId);
            statement.setString(3, role);
            statement.setString(4, notes);
            statement.executeUpdate();
        }
    }

    public boolean delete(int eventId, int studentId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {

            statement.setInt(1, eventId);
            statement.setInt(2, studentId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Saves the position of several participations in one transaction.
     */
    public void updatePositions(Map<Integer, String> positionsByParticipationId)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_POSITION_SQL)) {
                for (Map.Entry<Integer, String> entry : positionsByParticipationId.entrySet()) {
                    statement.setString(1, entry.getValue());
                    statement.setInt(2, entry.getKey());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public int countByEvent(int eventId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_BY_EVENT_SQL)) {

            statement.setInt(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    public Optional<ParticipationRow> find(int eventId, int studentId) throws SQLException {
        return query(SELECT_ONE_SQL, eventId, studentId).stream().findFirst();
    }

    public List<ParticipationRow> findByEvent(int eventId) throws SQLException {
        return query(SELECT_BY_EVENT_SQL, eventId);
    }

    public List<ParticipationRow> findByStudent(int studentId) throws SQLException {
        return query(SELECT_BY_STUDENT_SQL, studentId);
    }

    /**
     * Runs a select with the given integer parameters and maps every row.
     */
    private List<ParticipationRow> query(String sql, int... parameters) throws SQLException {
        List<ParticipationRow> rows = new ArrayList<>();
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

    private ParticipationRow mapRow(ResultSet resultSet) throws SQLException {
        return new ParticipationRow(
                resultSet.getInt("id_participacion"),
                resultSet.getInt("id_evento"),
                resultSet.getInt("id_estudiante"),
                resultSet.getString("rol_participacion"),
                resultSet.getString("posicion_obtenida"),
                resultSet.getString("observaciones"));
    }
}