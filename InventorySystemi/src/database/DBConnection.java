package database;

import java.nio.file.*;
import java.sql.*;

public class DBConnection {
    public static String location() {
        String path=Path.of(System.getProperty("user.home"),".inventorysystem","inventory").toAbsolutePath().toString().replace('\\','/');
        return AppConfig.get("DB_URL","jdbc:h2:file:"+path+";DATABASE_TO_LOWER=TRUE").split(";")[0];
    }
    public static Connection connect() {
        String path=Path.of(System.getProperty("user.home"),".inventorysystem","inventory").toAbsolutePath().toString().replace('\\','/');
        String url=AppConfig.get("DB_URL","jdbc:h2:file:"+path+";DATABASE_TO_LOWER=TRUE");
        if(!url.startsWith("jdbc:h2:file:") && !url.startsWith("jdbc:h2:mem:")) {
            throw new IllegalStateException(gui.LocaleManager.get("offline.database.only"));
        }
        try {
            if(url.startsWith("jdbc:h2:file:")) Files.createDirectories(Path.of(System.getProperty("user.home"),".inventorysystem"));
            DriverManager.setLoginTimeout(10);
            return DriverManager.getConnection(url,AppConfig.get("DB_USER","sa"),AppConfig.get("DB_PASSWORD",""));
        } catch(SQLException | java.io.IOException e) {
            throw new IllegalStateException(gui.LocaleManager.get("database.connection.failed"),e);
        }
    }
}
