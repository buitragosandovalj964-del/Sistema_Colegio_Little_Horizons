package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the conceptos_pago table.
 */
public class PaymentConceptDao {

    private static final String SELECT_BASE_SQL =
            "SELECT id_concepto, codigo_concepto, nombre_concepto, descripcion, "
            + "monto_base, es_recurrente, esta_activo FROM conceptos_pago";

    private static final String SELECT_ALL_SQL =
            SELECT_BASE_SQL + " ORDER BY nombre_concepto";

    private static final String SELECT_ACTIVE_SQL =
            SELECT_BASE_SQL + " WHERE esta_activo = TRUE ORDER BY nombre_concepto";

    private static final String SELECT_BY_ID_SQL =
            SELECT_BASE_SQL + " WHERE id_concepto = ?";

    private static final String SELECT_BY_CODE_SQL =
            SELECT_BASE_SQL + " WHERE codigo_concepto = ?";

    private static final String INSERT_SQL =
            "INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, "
            + "monto_base, es_recurrente, esta_activo) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE conceptos_pago SET codigo_concepto = ?, nombre_concepto = ?, "
            + "descripcion = ?, monto_base = ?, es_recurrente = ?, esta_activo = ? "
            + "WHERE id_concepto = ?";

    public void insert(PaymentConcept concept) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setParameters(statement, concept);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    concept.setId(keys.getInt(1));
                }
            }
        }
    }

    public boolean update(PaymentConcept concept) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            setParameters(statement, concept);
            statement.setInt(7, concept.getId());
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<PaymentConcept> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<PaymentConcept> findByCode(String code) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_CODE_SQL)) {

            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<PaymentConcept> findAll() throws SQLException {
        return query(SELECT_ALL_SQL);
    }

    public List<PaymentConcept> findActive() throws SQLException {
        return query(SELECT_ACTIVE_SQL);
    }

    private List<PaymentConcept> query(String sql) throws SQLException {
        List<PaymentConcept> list = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                list.add(mapRow(resultSet));
            }
        }
        return list;
    }

    private void setParameters(PreparedStatement statement, PaymentConcept concept)
            throws SQLException {
        statement.setString(1, concept.getCode());
        statement.setString(2, concept.getName());
        statement.setString(3, concept.getDescription());
        statement.setBigDecimal(4, concept.getBaseAmount());
        statement.setBoolean(5, concept.isRecurring());
        statement.setBoolean(6, concept.isActive());
    }

    private PaymentConcept mapRow(ResultSet resultSet) throws SQLException {
        PaymentConcept concept = new PaymentConcept();
        concept.setId(resultSet.getInt("id_concepto"));
        concept.setCode(resultSet.getString("codigo_concepto"));
        concept.setName(resultSet.getString("nombre_concepto"));
        concept.setDescription(resultSet.getString("descripcion"));
        concept.setBaseAmount(resultSet.getBigDecimal("monto_base"));
        concept.setRecurring(resultSet.getBoolean("es_recurrente"));
        concept.setActive(resultSet.getBoolean("esta_activo"));
        return concept;
    }
}

