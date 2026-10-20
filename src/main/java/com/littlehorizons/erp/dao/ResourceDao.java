package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the recursos table (and the read-only list of categories).
 */
public class ResourceDao {

    private static final String SELECT_BASE_SQL =
            "SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, r.descripcion, "
            + "r.fecha_adquisicion, r.ubicacion_fisica, r.estado_recurso, "
            + "c.id_categoria, c.nombre_categoria "
            + "FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria";

    private static final String SELECT_ALL_SQL =
            SELECT_BASE_SQL + " ORDER BY c.nombre_categoria, r.nombre_recurso";

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + " WHERE r.id_recurso = ?";

    private static final String SELECT_BY_CODE_SQL =
            SELECT_BASE_SQL + " WHERE r.codigo_recurso = ?";

    private static final String SELECT_CATEGORIES_SQL =
            "SELECT id_categoria, nombre_categoria FROM categorias_recurso "
            + "ORDER BY nombre_categoria";

    private static final String INSERT_SQL =
            "INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, "
            + "fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE recursos SET nombre_recurso = ?, id_categoria = ?, descripcion = ?, "
            + "fecha_adquisicion = ?, ubicacion_fisica = ? WHERE id_recurso = ?";

    private static final String UPDATE_STATUS_SQL =
            "UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?";

    private static final String COUNT_LOANS_SQL =
            "SELECT (SELECT COUNT(*) FROM prestamos_estudiante WHERE id_recurso = ?) "
            + "+ (SELECT COUNT(*) FROM prestamos_profesor WHERE id_recurso = ?)";

    private static final String DELETE_SQL = "DELETE FROM recursos WHERE id_recurso = ?";

    public void insert(Resource resource) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, resource.getCode());
            statement.setString(2, resource.getName());
            statement.setInt(3, resource.getCategory().id());
            statement.setString(4, resource.getDescription());
            setNullableDate(statement, 5, resource);
            statement.setString(6, resource.getLocation());
            statement.setString(7, resource.getStatus());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    resource.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    /**
     * Updates name, category, description, acquisition date and location.
     * The code and the status are not changed here.
     */
    public boolean update(Resource resource) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            statement.setString(1, resource.getName());
            statement.setInt(2, resource.getCategory().id());
            statement.setString(3, resource.getDescription());
            setNullableDate(statement, 4, resource);
            statement.setString(5, resource.getLocation());
            statement.setInt(6, resource.getId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(int id, String status) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {

            statement.setString(1, status);
            statement.setInt(2, id);
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

    /**
     * Counts the loans (to students and to teachers) a resource has ever had.
     */
    public int countLoans(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_LOANS_SQL)) {

            statement.setInt(1, id);
            statement.setInt(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    public Optional<Resource> findById(int id) throws SQLException {
        return queryOne(SELECT_BY_ID_SQL, String.valueOf(id));
    }

    public Optional<Resource> findByCode(String code) throws SQLException {
        return queryOne(SELECT_BY_CODE_SQL, code);
    }

    public List<Resource> findAll() throws SQLException {
        List<Resource> resources = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                resources.add(mapRow(resultSet));
            }
        }
        return resources;
    }

    public List<ResourceCategory> findCategories() throws SQLException {
        List<ResourceCategory> categories = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_CATEGORIES_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                categories.add(new ResourceCategory(resultSet.getInt("id_categoria"),
                        resultSet.getString("nombre_categoria")));
            }
        }
        return categories;
    }

    private Optional<Resource> queryOne(String sql, String parameter) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    private void setNullableDate(PreparedStatement statement, int index, Resource resource)
            throws SQLException {
        if (resource.getAcquisitionDate() == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(resource.getAcquisitionDate()));
        }
    }

    private Resource mapRow(ResultSet resultSet) throws SQLException {
        Resource resource = new Resource();
        resource.setId(resultSet.getInt("id_recurso"));
        resource.setCode(resultSet.getString("codigo_recurso"));
        resource.setName(resultSet.getString("nombre_recurso"));
        resource.setDescription(resultSet.getString("descripcion"));
        Date acquisition = resultSet.getDate("fecha_adquisicion");
        resource.setAcquisitionDate(acquisition == null ? null : acquisition.toLocalDate());
        resource.setLocation(resultSet.getString("ubicacion_fisica"));
        resource.setStatus(resultSet.getString("estado_recurso"));
        resource.setCategory(new ResourceCategory(resultSet.getInt("id_categoria"),
                resultSet.getString("nombre_categoria")));
        return resource;
    }
}