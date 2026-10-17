package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the acudientes table.
 */
public class GuardianDao {

    private static final String SELECT_BASE_SQL =
            "SELECT id_acudiente, tipo_documento, numero_documento, primer_nombre, "
            + "segundo_nombre, primer_apellido, segundo_apellido, parentesco, "
            + "telefono_principal, telefono_secundario, correo_electronico, direccion, "
            + "ocupacion, es_responsable_financiero FROM acudientes";

    private static final String SELECT_ALL_SQL =
            SELECT_BASE_SQL + " ORDER BY primer_apellido, segundo_apellido, primer_nombre";

    private static final String SELECT_BY_ID_SQL =
            SELECT_BASE_SQL + " WHERE id_acudiente = ?";

    private static final String SELECT_BY_DOCUMENT_SQL =
            SELECT_BASE_SQL + " WHERE tipo_documento = ? AND numero_documento = ?";

    private static final String INSERT_SQL =
            "INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, "
            + "segundo_nombre, primer_apellido, segundo_apellido, parentesco, "
            + "telefono_principal, telefono_secundario, correo_electronico, direccion, "
            + "ocupacion, es_responsable_financiero) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE acudientes SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, "
            + "segundo_apellido = ?, parentesco = ?, telefono_principal = ?, "
            + "telefono_secundario = ?, correo_electronico = ?, direccion = ?, "
            + "ocupacion = ?, es_responsable_financiero = ? WHERE id_acudiente = ?";

    private static final String DELETE_SQL = "DELETE FROM acudientes WHERE id_acudiente = ?";

    public void insert(Guardian guardian) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setGuardianParameters(statement, guardian);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    guardian.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public boolean update(Guardian guardian) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            setGuardianParameters(statement, guardian);
            statement.setInt(14, guardian.getId());
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

    public Optional<Guardian> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<Guardian> findByDocument(String documentType, String documentNumber)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_DOCUMENT_SQL)) {

            statement.setString(1, documentType);
            statement.setString(2, documentNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public List<Guardian> findAll() throws SQLException {
        List<Guardian> guardians = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                guardians.add(mapRow(resultSet));
            }
        }
        return guardians;
    }

    private void setGuardianParameters(PreparedStatement statement, Guardian guardian)
            throws SQLException {
        statement.setString(1, guardian.getDocumentType());
        statement.setString(2, guardian.getDocumentNumber());
        statement.setString(3, guardian.getFirstName());
        statement.setString(4, guardian.getMiddleName());
        statement.setString(5, guardian.getFirstLastName());
        statement.setString(6, guardian.getSecondLastName());
        statement.setString(7, guardian.getRelationship());
        statement.setString(8, guardian.getPrimaryPhone());
        statement.setString(9, guardian.getSecondaryPhone());
        statement.setString(10, guardian.getEmail());
        statement.setString(11, guardian.getAddress());
        statement.setString(12, guardian.getOccupation());
        statement.setBoolean(13, guardian.isFinanciallyResponsible());
    }

    private Guardian mapRow(ResultSet resultSet) throws SQLException {
        Guardian guardian = new Guardian();
        guardian.setId(resultSet.getInt("id_acudiente"));
        guardian.setDocumentType(resultSet.getString("tipo_documento"));
        guardian.setDocumentNumber(resultSet.getString("numero_documento"));
        guardian.setFirstName(resultSet.getString("primer_nombre"));
        guardian.setMiddleName(resultSet.getString("segundo_nombre"));
        guardian.setFirstLastName(resultSet.getString("primer_apellido"));
        guardian.setSecondLastName(resultSet.getString("segundo_apellido"));
        guardian.setRelationship(resultSet.getString("parentesco"));
        guardian.setPrimaryPhone(resultSet.getString("telefono_principal"));
        guardian.setSecondaryPhone(resultSet.getString("telefono_secundario"));
        guardian.setEmail(resultSet.getString("correo_electronico"));
        guardian.setAddress(resultSet.getString("direccion"));
        guardian.setOccupation(resultSet.getString("ocupacion"));
        guardian.setFinanciallyResponsible(resultSet.getBoolean("es_responsable_financiero"));
        return guardian;
    }
}