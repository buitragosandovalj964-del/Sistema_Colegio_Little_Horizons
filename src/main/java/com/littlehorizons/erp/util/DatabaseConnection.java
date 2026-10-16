package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Provides JDBC connections to the ERP database.
 * Credentials are read from db.properties, which is not stored in Git.
 */
public final class DatabaseConnection {

    private static final String CONFIG_FILE = "db.properties";

    private DatabaseConnection() {
        // Utility class: it should not be instantiated.
    }

    public static Connection getConnection() throws SQLException {
        Properties properties = loadProperties();
        return DriverManager.getConnection(
                properties.getProperty("db.url"),
                properties.getProperty("db.user"),
                properties.getProperty("db.password"));
    }

    private static Properties loadProperties() throws SQLException {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConnection.class
                .getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new SQLException("Configuration file not found: " + CONFIG_FILE);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new SQLException("Could not read " + CONFIG_FILE, e);
        }
        return properties;
    }
}