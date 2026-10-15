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
            DatabaseInitializer.initialize();
            try (Connection connection = DBConnection.connect();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users")) {
                result.next();
                assertEquals(0, result.getInt(1));
            }
            assertTrue(Files.exists(tempDirectory.resolve(".inventorysystem/inventory.mv.db")));
        } finally {
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
}
