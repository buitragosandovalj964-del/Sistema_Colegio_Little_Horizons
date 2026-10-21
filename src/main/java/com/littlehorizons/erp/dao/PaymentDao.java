package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.util.DatabaseConnection;
import java.math.BigDecimal;
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
 * Data access for the pagos table.
 */
public class PaymentDao {

    /**
     * A payment row as stored in the database, before joining student/guardian/concept objects.
     */
    public record PaymentRow(int id, int studentId, Integer guardianId, int conceptId,
            BigDecimal amountPaid, LocalDate paymentDate, String paymentMethod,
            String receiptNumber, Integer academicYear, String academicPeriod, String notes) { }

    private static final String SELECT_BASE_SQL =
            "SELECT id_pago, id_estudiante, id_acudiente, id_concepto, "
            + "monto_pagado, fecha_pago, metodo_pago, numero_recibo, "
            + "año_academico AS academic_year, periodo_academico, notas FROM pagos";

    private static final String SELECT_BY_STUDENT_SQL =
            SELECT_BASE_SQL + " WHERE id_estudiante = ? ORDER BY fecha_pago DESC";

    private static final String SELECT_BY_GUARDIAN_SQL =
            SELECT_BASE_SQL + " WHERE id_acudiente = ? ORDER BY fecha_pago DESC";

    private static final String SELECT_ALL_SQL =
            SELECT_BASE_SQL + " ORDER BY fecha_pago DESC";

    private static final String SELECT_BY_ID_SQL =
            SELECT_BASE_SQL + " WHERE id_pago = ?";

    private static final String INSERT_SQL =
            "INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, "
            + "fecha_pago, metodo_pago, numero_recibo, año_academico, periodo_academico, notas) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public int insert(int studentId, Integer guardianId, int conceptId, BigDecimal amountPaid,
            LocalDate paymentDate, String paymentMethod, String receiptNumber,
            Integer academicYear, String academicPeriod, String notes) throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, studentId);
            if (guardianId != null) {
                statement.setInt(2, guardianId);
            } else {
                statement.setNull(2, Types.INTEGER);
            }
            statement.setInt(3, conceptId);
            statement.setBigDecimal(4, amountPaid);
            statement.setDate(5, Date.valueOf(paymentDate));
            statement.setString(6, paymentMethod);
            statement.setString(7, receiptNumber);
            if (academicYear != null) {
                statement.setInt(8, academicYear);
            } else {
                statement.setNull(8, Types.INTEGER);
            }
            statement.setString(9, academicPeriod);
            statement.setString(10, notes);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public Optional<PaymentRow> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<PaymentRow> findByStudent(int studentId) throws SQLException {
        return queryById(SELECT_BY_STUDENT_SQL, studentId);
    }

    public List<PaymentRow> findByGuardian(int guardianId) throws SQLException {
        return queryById(SELECT_BY_GUARDIAN_SQL, guardianId);
    }

    public List<PaymentRow> findAll() throws SQLException {
        List<PaymentRow> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(mapRow(resultSet));
            }
        }
        return rows;
    }

    private List<PaymentRow> queryById(String sql, int id) throws SQLException {
        List<PaymentRow> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapRow(resultSet));
                }
            }
        }
        return rows;
    }

    private PaymentRow mapRow(ResultSet resultSet) throws SQLException {
        int guardianId = resultSet.getInt("id_acudiente");
        Integer guardianIdObj = resultSet.wasNull() ? null : guardianId;

        int year = resultSet.getInt("academic_year");
        Integer yearObj = resultSet.wasNull() ? null : year;

        return new PaymentRow(
                resultSet.getInt("id_pago"),
                resultSet.getInt("id_estudiante"),
                guardianIdObj,
                resultSet.getInt("id_concepto"),
                resultSet.getBigDecimal("monto_pagado"),
                resultSet.getDate("fecha_pago").toLocalDate(),
                resultSet.getString("metodo_pago"),
                resultSet.getString("numero_recibo"),
                yearObj,
                resultSet.getString("periodo_academico"),
                resultSet.getString("notas"));
    }
}
