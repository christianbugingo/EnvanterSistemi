package services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import product.Sale;

class SaleServiceTest {
    private String url;
    private SaleService service;

    @BeforeEach
    void setUp() throws Exception {
        url = "jdbc:h2:mem:sales_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (user_id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL, role VARCHAR(30) NOT NULL)");
                statement.execute("CREATE TABLE products (product_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL, category VARCHAR(255) NOT NULL, quantity INT NOT NULL CHECK (quantity >= 0), price DECIMAL(12,2) NOT NULL, supplier VARCHAR(255) NOT NULL)");
                statement.execute("CREATE TABLE sales (sale_id INT AUTO_INCREMENT PRIMARY KEY, product_id INT NOT NULL, user_id INT NOT NULL, quantity_sold INT NOT NULL CHECK (quantity_sold > 0), sale_date DATE NOT NULL, FOREIGN KEY (product_id) REFERENCES products(product_id), FOREIGN KEY (user_id) REFERENCES users(user_id))");
                statement.executeUpdate("INSERT INTO users (username, password, role) VALUES ('tester', 'test', 'admin')");
                statement.executeUpdate("INSERT INTO products (name, category, quantity, price, supplier) "
                        + "VALUES ('Test product', 'Test', 5, 1.00, 'Test supplier')");
            }
        }
        service = new SaleService(() -> {
            try {
                return DriverManager.getConnection(url, "sa", "");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    void saleDeductsStockAndPersistsSale() throws Exception {
        service.addMultipleSales(List.of(new Sale(0, 1, 1, 3, LocalDate.now())));

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT quantity, (SELECT COUNT(*) FROM sales) AS sales_count "
                     + "FROM products WHERE product_id = 1")) {
            result.next();
            assertEquals(2, result.getInt("quantity"));
            assertEquals(1, result.getInt("sales_count"));
        }
    }

    @Test
    void oversellIsRejectedAndTransactionLeavesStockAndSalesUnchanged() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> service.addMultipleSales(List.of(new Sale(0, 1, 1, 6, LocalDate.now()))));

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT quantity, (SELECT COUNT(*) FROM sales) AS sales_count "
                     + "FROM products WHERE product_id = 1")) {
            result.next();
            assertEquals(5, result.getInt("quantity"));
            assertEquals(0, result.getInt("sales_count"));
        }
    }
}
