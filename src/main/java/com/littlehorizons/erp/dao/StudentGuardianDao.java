package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data access for the estudiante_acudiente table (links between students and guardians).
 */
public class StudentGuardianDao {

    /**
     * A link as stored in the table, before loading the guardian data.
     */
    public record LinkRow(int guardianId, boolean primary, boolean emergencyContact) { }

    private interface TransactionBody {
        void run(Connection connection) throws SQLException;
    }

    private static final String INSERT_SQL =
            "INSERT INTO estudiante_acudiente "
            + "(id_estudiante, id_acudiente, es_principal, es_contacto_emergencia) "
            + "VALUES (?, ?, ?, ?)";

    private static final String CLEAR_PRIMARY_SQL =
            "UPDATE estudiante_acudiente SET es_principal = FALSE WHERE id_estudiante = ?";

    private static final String SET_PRIMARY_SQL =
            "UPDATE estudiante_acudiente SET es_principal = TRUE "
            + "WHERE id_estudiante = ? AND id_acudiente = ?";

    private static final String UPDATE_FLAGS_SQL =
            "UPDATE estudiante_acudiente SET es_principal = ?, es_contacto_emergencia = ? "
            + "WHERE id_estudiante = ? AND id_acudiente = ?";

    private static final String DELETE_SQL =
            "DELETE FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?";

    private static final String SELECT_LINKS_OF_STUDENT_SQL =
            "SELECT id_acudiente, es_principal, es_contacto_emergencia "
            + "FROM estudiante_acudiente WHERE id_estudiante = ? "
            + "ORDER BY es_principal DESC, id_acudiente";

    private static final String SELECT_STUDENTS_OF_GUARDIAN_SQL =
            "SELECT id_estudiante FROM estudiante_acudiente "
            + "WHERE id_acudiente = ? ORDER BY id_estudiante";

    private static final String SELECT_STUDENTS_WITHOUT_GUARDIAN_SQL =
            "SELECT id_estudiante FROM estudiantes "
            + "WHERE id_estudiante NOT IN (SELECT id_estudiante FROM estudiante_acudiente)";

    public void link(int studentId, int guardianId, boolean primary, boolean emergencyContact)
            throws SQLException {
        inTransaction(connection -> {
            if (primary) {
                clearPrimary(connection, studentId);
            }
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
                statement.setInt(1, studentId);
                statement.setInt(2, guardianId);
                statement.setBoolean(3, primary);
                statement.setBoolean(4, emergencyContact);
                statement.executeUpdate();
            }
        });
    }

    public void updateFlags(int studentId, int guardianId, boolean primary,
            boolean emergencyContact) throws SQLException {
        inTransaction(connection -> {
            if (primary) {
                clearPrimary(connection, studentId);
            }
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_FLAGS_SQL)) {
                statement.setBoolean(1, primary);
                statement.setBoolean(2, emergencyContact);
                statement.setInt(3, studentId);
                statement.setInt(4, guardianId);
                statement.executeUpdate();
            }
        });
    }

    /**
     * Removes the link. When newPrimaryGuardianId is not null, that guardian becomes
     * the primary one in the same transaction.
     */
    public void unlink(int studentId, int guardianId, Integer newPrimaryGuardianId)
            throws SQLException {
        inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setInt(1, studentId);
                statement.setInt(2, guardianId);
                statement.executeUpdate();
            }
            if (newPrimaryGuardianId != null) {
                clearPrimary(connection, studentId);
                try (PreparedStatement statement = connection.prepareStatement(SET_PRIMARY_SQL)) {
                    statement.setInt(1, studentId);
                    statement.setInt(2, newPrimaryGuardianId);
                    statement.executeUpdate();
                }
            }
        });
    }

    public List<LinkRow> findLinksOfStudent(int studentId) throws SQLException {
        List<LinkRow> links = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_LINKS_OF_STUDENT_SQL)) {

            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    links.add(new LinkRow(resultSet.getInt("id_acudiente"),
                            resultSet.getBoolean("es_principal"),
                            resultSet.getBoolean("es_contacto_emergencia")));
                }
            }
        }
        return links;
    }

    public List<Integer> findStudentIdsOfGuardian(int guardianId) throws SQLException {
        List<Integer> studentIds = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_STUDENTS_OF_GUARDIAN_SQL)) {

            statement.setInt(1, guardianId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    studentIds.add(resultSet.getInt("id_estudiante"));
                }
            }
        }
        return studentIds;
    }

    public Set<Integer> findStudentIdsWithoutGuardian() throws SQLException {
        Set<Integer> studentIds = new HashSet<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_STUDENTS_WITHOUT_GUARDIAN_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                studentIds.add(resultSet.getInt("id_estudiante"));
            }
        }
        return studentIds;
    }

    private void clearPrimary(Connection connection, int studentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(CLEAR_PRIMARY_SQL)) {
            statement.setInt(1, studentId);
            statement.executeUpdate();
        }
    }

    /**
     * Runs several statements as one unit: if any fails, none of them is kept.
     */
    private void inTransaction(TransactionBody body) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                body.run(connection);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}