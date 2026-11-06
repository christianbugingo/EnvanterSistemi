package database;

import java.sql.*;

/** Additive, restartable upgrades. Back up an existing database before deploying an upgrade. */
public final class DatabaseInitializer {
    private DatabaseInitializer() { }
    public static void initialize() throws SQLException {
        try (Connection c = DBConnection.connect()) { initialize(c); }
    }
    static void initialize(Connection c) throws SQLException {
        Migrations.before(c);
        execute(c, "CREATE TABLE IF NOT EXISTS users (user_id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL, role VARCHAR(30) NOT NULL)");
        execute(c, "CREATE TABLE IF NOT EXISTS products (product_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL, category VARCHAR(255) NOT NULL, quantity DECIMAL(14,3) NOT NULL DEFAULT 0 CHECK(quantity >= 0), price DECIMAL(12,2) NOT NULL CHECK(price >= 0), supplier VARCHAR(255) NOT NULL)");
        execute(c, "CREATE TABLE IF NOT EXISTS sales (sale_id INT AUTO_INCREMENT PRIMARY KEY, product_id INT NOT NULL, user_id INT NOT NULL, quantity_sold DECIMAL(14,3) NOT NULL CHECK(quantity_sold > 0), sale_date DATE NOT NULL, CONSTRAINT fk_sales_product FOREIGN KEY(product_id) REFERENCES products(product_id), CONSTRAINT fk_sales_user FOREIGN KEY(user_id) REFERENCES users(user_id))");
        add(c, "users", "must_change_password", "BOOLEAN NOT NULL DEFAULT FALSE");
        add(c, "users", "active", "BOOLEAN NOT NULL DEFAULT TRUE");
        add(c, "products", "unit", "VARCHAR(10) NOT NULL DEFAULT 'PIECE'");
        add(c, "products", "low_stock", "DECIMAL(14,3) NOT NULL DEFAULT 10");
        add(c, "products", "version", "INT NOT NULL DEFAULT 0");
        add(c, "sales", "unit", "VARCHAR(10) NOT NULL DEFAULT 'PIECE'");
        add(c, "sales", "unit_price", "DECIMAL(12,2)");
        add(c, "sales", "product_name", "VARCHAR(255)");
        add(c, "sales", "voided", "BOOLEAN NOT NULL DEFAULT FALSE");
        add(c, "sales", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        add(c, "products", "barcode", "VARCHAR(64)");
        execute(c, "CREATE UNIQUE INDEX IF NOT EXISTS ux_product_barcode ON products(barcode)");
        execute(c, "CREATE TABLE IF NOT EXISTS checkouts (checkout_id VARCHAR(36) PRIMARY KEY, user_id INT NOT NULL REFERENCES users(user_id), payment_method VARCHAR(10) NOT NULL, total DECIMAL(14,2) NOT NULL, fingerprint VARCHAR(64) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        add(c, "sales", "checkout_id", "VARCHAR(36) REFERENCES checkouts(checkout_id)");
        add(c, "sales", "line_no", "INT");
        // Legacy sales have no original price snapshot: use the current price once, then freeze it.
        execute(c, "UPDATE sales SET unit_price = (SELECT price FROM products WHERE products.product_id = sales.product_id) WHERE unit_price IS NULL");
        execute(c, "UPDATE sales SET product_name = (SELECT name FROM products WHERE products.product_id = sales.product_id) WHERE product_name IS NULL");

        execute(c, "CREATE TABLE IF NOT EXISTS audit_log (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, username VARCHAR(100) NOT NULL, action VARCHAR(40) NOT NULL, details VARCHAR(2000) NOT NULL, workstation VARCHAR(255) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        execute(c, "CREATE TABLE IF NOT EXISTS app_settings (setting_key VARCHAR(100) PRIMARY KEY, setting_value VARCHAR(255) NOT NULL)");
        // Serialize first-administrator creation.
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO app_settings(setting_key, setting_value) SELECT 'bootstrap', '1' WHERE NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key='bootstrap')")) {
            try { ps.executeUpdate(); } catch (SQLException ex) { if (!"23505".equals(ex.getSQLState()) && ex.getErrorCode() != 1062) throw ex; }
        }
        Migrations.apply(c);
    }
    private static void add(Connection c, String table, String column, String definition) throws SQLException {
        try (ResultSet rs = c.getMetaData().getColumns(c.getCatalog(), null, table, column)) { if (rs.next()) return; }
        execute(c, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }
    private static void execute(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) { s.execute(sql); }
    }
}
