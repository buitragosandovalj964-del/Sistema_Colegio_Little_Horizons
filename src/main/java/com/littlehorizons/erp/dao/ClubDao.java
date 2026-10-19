package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the clubes table.
 */
public class ClubDao {

    /**
     * A club as stored in the table, before loading its supervisor.
     */
    public record ClubRow(int id, String name, String description, Integer supervisorId,
            int maxCapacity, String meetingSchedule, boolean active) { }

    private static final String SELECT_BASE_SQL =
            "SELECT id_club, nombre_club, descripcion, id_profesor_supervisor, "
            + "capacidad_maxima, horario_reunion, esta_activo FROM clubes";

    private static final String SELECT_ALL_SQL = SELECT_BASE_SQL + " ORDER BY nombre_club";

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + " WHERE id_club = ?";

    private static final String SELECT_BY_NAME_SQL = SELECT_BASE_SQL + " WHERE nombre_club = ?";

    private static final String INSERT_SQL =
            "INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, "
            + "capacidad_maxima, horario_reunion, esta_activo) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE clubes SET nombre_club = ?, descripcion = ?, id_profesor_supervisor = ?, "
            + "capacidad_maxima = ?, horario_reunion = ?, esta_activo = ? WHERE id_club = ?";

    private static final String DELETE_SQL = "DELETE FROM clubes WHERE id_club = ?";

    public void insert(Club club) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setClubParameters(statement, club);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    club.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public boolean update(Club club) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            setClubParameters(statement, club);
            statement.setInt(7, club.getId());
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

    public Optional<ClubRow> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<ClubRow> findByName(String name) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_NAME_SQL)) {

            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<ClubRow> findAll() throws SQLException {
        List<ClubRow> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(mapRow(resultSet));
            }
        }
        return rows;
    }

    private void setClubParameters(PreparedStatement statement, Club club) throws SQLException {
        statement.setString(1, club.getName());
        statement.setString(2, club.getDescription());
        if (club.getSupervisor() == null) {
            statement.setNull(3, Types.INTEGER);
        } else {
            statement.setInt(3, club.getSupervisor().getId());
        }
        statement.setInt(4, club.getMaxCapacity());
        statement.setString(5, club.getMeetingSchedule());
        statement.setBoolean(6, club.isActive());
    }

    private ClubRow mapRow(ResultSet resultSet) throws SQLException {
        int supervisor = resultSet.getInt("id_profesor_supervisor");
        Integer supervisorId = resultSet.wasNull() ? null : supervisor;
        return new ClubRow(
                resultSet.getInt("id_club"),
                resultSet.getString("nombre_club"),
                resultSet.getString("descripcion"),
                supervisorId,
                resultSet.getInt("capacidad_maxima"),
                resultSet.getString("horario_reunion"),
                resultSet.getBoolean("esta_activo"));
    }
}