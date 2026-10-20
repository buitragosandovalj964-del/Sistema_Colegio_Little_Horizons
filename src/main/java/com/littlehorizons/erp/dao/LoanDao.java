package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BorrowerType;
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
 * Data access for the loans tables (prestamos_estudiante and prestamos_profesor).
 */
public class LoanDao {

    /**
     * A loan as stored in the table, before loading resource and borrower.
     */
    public record LoanRow(BorrowerType type, int id, int resourceId, int borrowerId,
            LocalDate loanDate, LocalDate dueDate, LocalDate returnDate, String status,
            String notes) { }

    private static final String OPEN_STATUSES = "('Activo', 'Vencido')";

    private static final String RESERVE_RESOURCE_SQL =
            "UPDATE recursos SET estado_recurso = 'Prestado' "
            + "WHERE id_recurso = ? AND estado_recurso = 'Disponible'";

    private static final String RELEASE_RESOURCE_SQL =
            "UPDATE recursos SET estado_recurso = 'Disponible' "
            + "WHERE id_recurso = ? AND estado_recurso = 'Prestado'";

    /**
     * Registers the loan and marks the resource as lent in one transaction.
     * Returns false (and saves nothing) if the resource is no longer available.
     */
    public boolean lend(BorrowerType type, int resourceId, int borrowerId, LocalDate loanDate,
            LocalDate dueDate, String notes) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean reserved;
                try (PreparedStatement statement = connection.prepareStatement(
                        RESERVE_RESOURCE_SQL)) {
                    statement.setInt(1, resourceId);
                    reserved = statement.executeUpdate() > 0;
                }
                if (!reserved) {
                    connection.rollback();
                    return false;
                }
                String insertSql = "INSERT INTO " + type.table() + " (id_recurso, "
                        + type.borrowerColumn() + ", fecha_prestamo, "
                        + "fecha_devolucion_esperada, observaciones) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                    statement.setInt(1, resourceId);
                    statement.setInt(2, borrowerId);
                    statement.setDate(3, Date.valueOf(loanDate));
                    statement.setDate(4, Date.valueOf(dueDate));
                    statement.setString(5, notes);
                    statement.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Registers the return and releases the resource in one transaction.
     * Returns false (and saves nothing) if the loan was already returned.
     */
    public boolean returnLoan(BorrowerType type, int loanId, int resourceId,
            LocalDate returnDate, String notes) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String updateSql = "UPDATE " + type.table() + " SET estado_prestamo = 'Devuelto', "
                        + "fecha_devolucion_real = ?, observaciones = COALESCE(?, observaciones) "
                        + "WHERE id_prestamo = ? AND estado_prestamo IN " + OPEN_STATUSES;
                boolean returned;
                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setDate(1, Date.valueOf(returnDate));
                    statement.setString(2, notes);
                    statement.setInt(3, loanId);
                    returned = statement.executeUpdate() > 0;
                }
                if (!returned) {
                    connection.rollback();
                    return false;
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        RELEASE_RESOURCE_SQL)) {
                    statement.setInt(1, resourceId);
                    statement.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Marks as "Vencido" the active loans whose due date is before today.
     */
    public int markOverdue(LocalDate today) throws SQLException {
        int updated = 0;
        for (BorrowerType type : BorrowerType.values()) {
            String sql = "UPDATE " + type.table() + " SET estado_prestamo = 'Vencido' "
                    + "WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < ?";
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setDate(1, Date.valueOf(today));
                updated += statement.executeUpdate();
            }
        }
        return updated;
    }

    public List<LoanRow> findOpen(BorrowerType type) throws SQLException {
        return query(type, " WHERE estado_prestamo IN " + OPEN_STATUSES
                + " ORDER BY fecha_devolucion_esperada");
    }

    public List<LoanRow> findOverdue(BorrowerType type) throws SQLException {
        return query(type, " WHERE estado_prestamo = 'Vencido' ORDER BY fecha_devolucion_esperada");
    }

    public List<LoanRow> findByBorrower(BorrowerType type, int borrowerId) throws SQLException {
        return query(type, " WHERE " + type.borrowerColumn()
                + " = ? ORDER BY fecha_prestamo DESC, id_prestamo DESC", borrowerId);
    }

    public Optional<LoanRow> findOpenByResource(BorrowerType type, int resourceId)
            throws SQLException {
        return query(type, " WHERE id_recurso = ? AND estado_prestamo IN " + OPEN_STATUSES,
                resourceId).stream().findFirst();
    }

    public int countOpenByBorrower(BorrowerType type, int borrowerId) throws SQLException {
        return count(type, borrowerId, " AND estado_prestamo IN " + OPEN_STATUSES);
    }

    public int countOverdueByBorrower(BorrowerType type, int borrowerId) throws SQLException {
        return count(type, borrowerId, " AND estado_prestamo = 'Vencido'");
    }

    private int count(BorrowerType type, int borrowerId, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + type.table() + " WHERE "
                + type.borrowerColumn() + " = ?" + condition;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, borrowerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private List<LoanRow> query(BorrowerType type, String whereAndOrder, Object... parameters)
            throws SQLException {
        String sql = "SELECT id_prestamo, id_recurso, " + type.borrowerColumn()
                + " AS borrower_id, fecha_prestamo, fecha_devolucion_esperada, "
                + "fecha_devolucion_real, estado_prestamo, observaciones FROM "
                + type.table() + whereAndOrder;
        List<LoanRow> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapRow(type, resultSet));
                }
            }
        }
        return rows;
    }

    private LoanRow mapRow(BorrowerType type, ResultSet resultSet) throws SQLException {
        Date returned = resultSet.getDate("fecha_devolucion_real");
        return new LoanRow(type,
                resultSet.getInt("id_prestamo"),
                resultSet.getInt("id_recurso"),
                resultSet.getInt("borrower_id"),
                resultSet.getDate("fecha_prestamo").toLocalDate(),
                resultSet.getDate("fecha_devolucion_esperada").toLocalDate(),
                returned == null ? null : returned.toLocalDate(),
                resultSet.getString("estado_prestamo"),
                resultSet.getString("observaciones"));
    }
}