package se.drachbar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteConnection {
    private static final Logger log = LoggerFactory.getLogger(SQLiteConnection.class);

    public static Connection connect() {
        String url = "jdbc:sqlite:chat_log.db";
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            log.error("Connection failed: ", e);
            return null;
        }
    }
}
