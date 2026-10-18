package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the grados table (read only: the catalog comes with the script).
 */
public class GradeDao {

    private static final String SELECT_BASE_SQL =
            "SELECT id_grado, nombre_grado, nivel_numerico FROM grados";

    private static final String SELECT_ALL_SQL = SELECT_BASE_SQL + " ORDER BY nivel_numerico";

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + " WHERE id_grado = ?";

    public List<Grade> findAll() throws SQLException {
        List<Grade> grades = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                grades.add(mapRow(resultSet));
            }
        }
        return grades;
    }

    public Optional<Grade> findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    private Grade mapRow(ResultSet resultSet) throws SQLException {
        Grade grade = new Grade();
        grade.setId(resultSet.getInt("id_grado"));
        grade.setName(resultSet.getString("nombre_grado"));
        grade.setNumericLevel(resultSet.getInt("nivel_numerico"));
        return grade;
    }
}