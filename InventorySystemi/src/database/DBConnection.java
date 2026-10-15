

package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;


public class DBConnection {
    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? System.getProperty(name, fallback) : value;
    }

    public static Connection connect() {
        try {
            String url = setting("DB_URL", defaultUrl());
            String user = setting("DB_USER", "sa");
            String password = setting("DB_PASSWORD", "");
            if (url.startsWith("jdbc:h2:file:")) {
                Files.createDirectories(Path.of(System.getProperty("user.home"), ".inventorysystem"));
            }
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open the application database. Check database settings and file permissions.", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not create the application database directory.", e);
        }
    }

    private static String defaultUrl() {
        String path = Path.of(System.getProperty("user.home"), ".inventorysystem", "inventory")
                .toAbsolutePath().toString().replace('\\', '/');
        return "jdbc:h2:file:" + path + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
    }
    
}
