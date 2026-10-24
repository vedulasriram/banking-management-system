
package banking.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            System.getenv("BANKING_DB_URL");

    private static final String USERNAME =
            System.getenv("BANKING_DB_USERNAME");

    private static final String PASSWORD =
            System.getenv("BANKING_DB_PASSWORD");

    public static Connection getConnection() throws SQLException {

        if (URL == null || URL.isBlank()
                || USERNAME == null || USERNAME.isBlank()
                || PASSWORD == null || PASSWORD.isBlank()) {

            throw new SQLException(
                    "Database configuration is missing. "
                    + "Please set BANKING_DB_URL, "
                    + "BANKING_DB_USERNAME and "
                    + "BANKING_DB_PASSWORD environment variables.");
        }

        return DriverManager.getConnection(
                URL, USERNAME, PASSWORD);
    }

    public static void main(String[] args) {

        try (Connection connection = getConnection()) {

            System.out.println("Oracle connection successful!");
            System.out.println("Connected: "
                    + !connection.isClosed());

        } catch (SQLException e) {

            System.out.println("Oracle connection failed.");
            System.err.println(e.getMessage());
        }
    }
}
