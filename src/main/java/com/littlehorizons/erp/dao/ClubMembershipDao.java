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
 * Data access for the membresias_club table.
 */
public class ClubMembershipDao {

    /**
     * A membership as stored in the table, before loading student and club.
     */
    public record MembershipRow(int id, int studentId, int clubId, LocalDate joinDate,
            String status, LocalDate withdrawalDate, String withdrawalReason) { }

    private static final String SELECT_BASE_SQL =
            "SELECT id_membresia, id_estudiante, id_club, fecha_ingreso, estado_membresia, "
            + "fecha_retiro, motivo_retiro FROM membresias_club";

    private static final String SELECT_ACTIVE_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? AND id_club = ? "
            + "AND estado_membresia = 'Activa'";

    private static final String SELECT_BY_CLUB_SQL =
            SELECT_BASE_SQL + " WHERE id_club = ? ORDER BY estado_membresia, fecha_ingreso";

    private static final String SELECT_BY_STUDENT_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? ORDER BY estado_membresia, fecha_ingreso";

    private static final String COUNT_ACTIVE_SQL =
            "SELECT COUNT(*) FROM membresias_club "
            + "WHERE id_club = ? AND estado_membresia = 'Activa'";

    private static final String INSERT_SQL =
            "INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso) "
            + "VALUES (?, ?, ?)";

    private static final String WITHDRAW_SQL =
            "UPDATE membresias_club SET estado_membresia = 'Retirada', fecha_retiro = ?, "
            + "motivo_retiro = ? WHERE id_membresia = ?";

    public void insert(int studentId, int clubId, LocalDate joinDate) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {

            statement.setInt(1, studentId);
            statement.setInt(2, clubId);
            statement.setDate(3, Date.valueOf(joinDate));
            statement.executeUpdate();
        }
    }

    public boolean withdraw(int id, LocalDate withdrawalDate, String reason)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(WITHDRAW_SQL)) {

            statement.setDate(1, Date.valueOf(withdrawalDate));
            statement.setString(2, reason);
            statement.setInt(3, id);
            return statement.executeUpdate() > 0;
        }
    }

    public int countActive(int clubId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_ACTIVE_SQL)) {

            statement.setInt(1, clubId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    public Optional<MembershipRow> findActive(int studentId, int clubId) throws SQLException {
        return query(SELECT_ACTIVE_SQL, studentId, clubId).stream().findFirst();
    }

    public List<MembershipRow> findByClub(int clubId) throws SQLException {
        return query(SELECT_BY_CLUB_SQL, clubId);
    }

    public List<MembershipRow> findByStudent(int studentId) throws SQLException {
        return query(SELECT_BY_STUDENT_SQL, studentId);
    }

    /**
     * Runs a select with the given integer parameters and maps every row.
     */
    private List<MembershipRow> query(String sql, int... parameters) throws SQLException {
        List<MembershipRow> rows = new ArrayList<>();
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

    private MembershipRow mapRow(ResultSet resultSet) throws SQLException {
        Date withdrawal = resultSet.getDate("fecha_retiro");
        return new MembershipRow(
                resultSet.getInt("id_membresia"),
                resultSet.getInt("id_estudiante"),
                resultSet.getInt("id_club"),
                resultSet.getDate("fecha_ingreso").toLocalDate(),
                resultSet.getString("estado_membresia"),
                withdrawal == null ? null : withdrawal.toLocalDate(),
                resultSet.getString("motivo_retiro"));
    }
}