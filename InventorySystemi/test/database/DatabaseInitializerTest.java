package database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseInitializerTest {
    @Test
    void defaultConnectionCreatesPersistentDatabaseInUserDirectory(@TempDir Path tempDirectory) throws Exception {
        assumeFalse(System.getenv("DB_URL") != null || System.getProperty("DB_URL") != null,
                "skip local database path test when an external DB_URL is configured");
        String oldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDirectory.toString());
            System.setProperty("DB_URL", "jdbc:h2:file:"+tempDirectory.resolve(".inventorysystem/inventory")+";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
            DatabaseInitializer.initialize();
            try (Connection connection = DBConnection.connect();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users")) {
                result.next();
                assertEquals(0, result.getInt(1));
            }
            assertTrue(Files.exists(tempDirectory.resolve(".inventorysystem/inventory.mv.db")));
        } finally {
            System.clearProperty("DB_URL");
            if (oldHome == null) System.clearProperty("user.home");
            else System.setProperty("user.home", oldHome);
        }
    }

    @Test
    void createsMissingSchemaAndKeepsExistingRowsOnRestart() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:inventory_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "")) {
            DatabaseInitializer.initialize(connection);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO users (username, password, role) VALUES ('tester', 'test-only', 'admin')");
            }

            DatabaseInitializer.initialize(connection);
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users WHERE username='tester'")) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
        }
    }
    @Test
    void upgradesLegacyIntegerStockAndPreservesAccountsAndSales() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:legacy_upgrade;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "")) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE users(user_id INT AUTO_INCREMENT PRIMARY KEY,username VARCHAR(100) UNIQUE,password VARCHAR(255),role VARCHAR(30))");
                st.execute("CREATE TABLE products(product_id INT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(255),category VARCHAR(255),quantity INT NOT NULL,price DECIMAL(12,2),supplier VARCHAR(255))");
                st.execute("CREATE TABLE sales(sale_id INT AUTO_INCREMENT PRIMARY KEY,product_id INT,user_id INT,quantity_sold INT NOT NULL,sale_date DATE)");
                st.execute("INSERT INTO users VALUES(1,'oldadmin','OldPassword!','admin')");
                st.execute("INSERT INTO products VALUES(1,'Legacy','Test',7,12.50,'Supplier')");
                st.execute("INSERT INTO sales VALUES(1,1,1,2,CURRENT_DATE)");
            }
            DatabaseInitializer.initialize(c);
            DatabaseInitializer.initialize(c);
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT p.quantity,p.unit,s.unit_price,s.product_name,u.password FROM products p JOIN sales s ON p.product_id=s.product_id JOIN users u ON u.user_id=s.user_id")) {
                rs.next(); assertEquals("7.000",rs.getBigDecimal(1).toPlainString());assertEquals("PIECE",rs.getString(2));
                assertEquals("12.50",rs.getBigDecimal(3).toPlainString());assertEquals("Legacy",rs.getString(4));assertEquals("OldPassword!",rs.getString(5));
            }
            try (Statement st=c.createStatement()) { st.execute("UPDATE products SET price=99.99 WHERE product_id=1"); }
            DatabaseInitializer.initialize(c);
            try (Statement st=c.createStatement(); ResultSet rs=st.executeQuery("SELECT unit_price FROM sales")) {rs.next();assertEquals("12.50",rs.getBigDecimal(1).toPlainString());}
        }
    }
}
