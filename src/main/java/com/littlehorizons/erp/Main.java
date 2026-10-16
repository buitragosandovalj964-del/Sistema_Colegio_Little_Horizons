package com.littlehorizons.erp;

import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Connection OK");
            System.out.println("Database: " + connection.getCatalog());
            System.out.println("User: " + connection.getMetaData().getUserName());
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}