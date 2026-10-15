package database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates missing application tables only; existing rows and tables are never reset. */
public final class DatabaseInitializer {
    private static final String[] CREATE_TABLES = {
        "CREATE TABLE IF NOT EXISTS users (" +
            "user_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            "username VARCHAR(100) NOT NULL UNIQUE," +
            "password VARCHAR(255) NOT NULL," +
            "role VARCHAR(30) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS products (" +
            "product_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            "name VARCHAR(255) NOT NULL," +
            "category VARCHAR(255) NOT NULL," +
            "quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0)," +
            "price DECIMAL(12,2) NOT NULL CHECK (price >= 0)," +
            "supplier VARCHAR(255) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS sales (" +
            "sale_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            "product_id INT NOT NULL," +
            "user_id INT NOT NULL," +
            "quantity_sold INT NOT NULL CHECK (quantity_sold > 0)," +
            "sale_date DATE NOT NULL," +
            "CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES products(product_id)," +
            "CONSTRAINT fk_sales_user FOREIGN KEY (user_id) REFERENCES users(user_id))"
    };

    private DatabaseInitializer() { }

    public static void initialize() throws SQLException {
        try (Connection connection = DBConnection.connect()) {
            initialize(connection);
        }
    }

    static void initialize(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : CREATE_TABLES) statement.execute(sql);
        }
    }
}
