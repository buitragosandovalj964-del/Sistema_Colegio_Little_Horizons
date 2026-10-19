package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Event;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the eventos table.
 */
public class EventDao {

    /**
     * An event as stored in the table, before loading its organizer.
     */
    public record EventRow(int id, String name, String description, String type,
            LocalDate date, String place, boolean hasCompetition, Integer organizerId,
            boolean active) { }

    private static final String SELECT_BASE_SQL =
            "SELECT id_evento, nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, "
            + "tiene_competencia, id_organizador, esta_activo FROM eventos";

    private static final String SELECT_ALL_SQL = SELECT_BASE_SQL + " ORDER BY fecha_evento, id_evento";

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + " WHERE id_evento = ?";

    private static final String SELECT_BY_NAME_DATE_SQL =
            SELECT_BASE_SQL + " WHERE nombre_evento = ? AND fecha_evento = ?";

    private static final String INSERT_SQL =
            "INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, "
            + "tiene_competencia, id_organizador, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE eventos SET nombre_evento = ?, descripcion = ?, tipo_evento = ?, "
            + "fecha_evento = ?, lugar = ?, tiene_competencia = ?, id_organizador = ?, "
            + "esta_activo = ? WHERE id_evento = ?";

    private static final String DELETE_SQL = "DELETE FROM eventos WHERE id_evento = ?";

    public void insert(Event event) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setEventParameters(statement, event);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    event.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public boolean update(Event event) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            setEventParameters(statement, event);
            statement.setInt(9, event.getId());
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

    public Optional<EventRow> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<EventRow> findByNameAndDate(String name, LocalDate date)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_NAME_DATE_SQL)) {

            statement.setString(1, name);
            statement.setDate(2, Date.valueOf(date));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<EventRow> findAll() throws SQLException {
        List<EventRow> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(mapRow(resultSet));
            }
        }
        return rows;
    }

    private void setEventParameters(PreparedStatement statement, Event event)
            throws SQLException {
        statement.setString(1, event.getName());
        statement.setString(2, event.getDescription());
        statement.setString(3, event.getType());
        statement.setDate(4, Date.valueOf(event.getDate()));
        statement.setString(5, event.getPlace());
        statement.setBoolean(6, event.isHasCompetition());
        if (event.getOrganizer() == null) {
            statement.setNull(7, Types.INTEGER);
        } else {
            statement.setInt(7, event.getOrganizer().getId());
        }
        statement.setBoolean(8, event.isActive());
    }

    private EventRow mapRow(ResultSet resultSet) throws SQLException {
        int organizer = resultSet.getInt("id_organizador");
        Integer organizerId = resultSet.wasNull() ? null : organizer;
        return new EventRow(
                resultSet.getInt("id_evento"),
                resultSet.getString("nombre_evento"),
                resultSet.getString("descripcion"),
                resultSet.getString("tipo_evento"),
                resultSet.getDate("fecha_evento").toLocalDate(),
                resultSet.getString("lugar"),
                resultSet.getBoolean("tiene_competencia"),
                organizerId,
                resultSet.getBoolean("esta_activo"));
    }
}