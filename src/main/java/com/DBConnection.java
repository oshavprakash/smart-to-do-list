
package com;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    public static Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || url.isBlank()
                || username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new SQLException(
                "Database configuration is missing. Check DB_URL, DB_USERNAME, and DB_PASSWORD."
            );
        }

        return DriverManager.getConnection(url, username, password);
    }
}
