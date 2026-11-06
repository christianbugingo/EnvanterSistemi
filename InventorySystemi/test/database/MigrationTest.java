package database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MigrationTest {
    @Test void real121FixtureCopyMigratesWithoutLosingSalesAndBackupRestores(@TempDir Path dir) throws Exception {
        String original="jdbc:h2:file:"+dir.resolve("old/inventory")+";DATABASE_TO_LOWER=TRUE";Files.createDirectories(dir.resolve("old"));
        try(Connection c=DriverManager.getConnection(original,"sa","");var input=Files.newBufferedReader(Path.of("test-fixtures/v121.sql"))){org.h2.tools.RunScript.execute(c,input);try(Statement data=c.createStatement()){data.execute("UPDATE sales SET quantity_sold=0.230");data.execute("UPDATE checkouts SET total=11.50");}try(Statement shutdown=c.createStatement()){shutdown.execute("SHUTDOWN");}}
        Files.createDirectories(dir.resolve("copy"));Files.copy(dir.resolve("old/inventory.mv.db"),dir.resolve("copy/inventory.mv.db"));
        String copy="jdbc:h2:file:"+dir.resolve("copy/inventory")+";DATABASE_TO_LOWER=TRUE";
        try(Connection c=DriverManager.getConnection(copy,"sa","")) {
            DatabaseInitializer.initialize(c);DatabaseInitializer.initialize(c);
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT quantity,low_stock FROM products")){r.next();assertEquals(8750,r.getLong(1));assertEquals(230,r.getLong(2));}
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT quantity_sold,unit_price,product_name FROM sales")){r.next();assertEquals(230,r.getLong(1));assertEquals("49.99",r.getBigDecimal(2).setScale(2).toPlainString());assertEquals("Apples",r.getString(3));}
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT total,payment_method FROM checkouts")){r.next();assertEquals("11.50",r.getBigDecimal(1).setScale(2).toPlainString());assertEquals("CASH",r.getString(2));}
            assertFalse(Migrations.exists(c,"shifts"));
            try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,"checkouts","cash_received")){assertFalse(r.next());}
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM migration_archive")){r.next();assertEquals(2,r.getInt(1));}
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM users")){r.next();assertEquals(1,r.getInt(1));}
        }
        List<Path> backups;try(var files=Files.list(dir.resolve("copy/migration-backups"))){backups=files.toList();}assertEquals(1,backups.size());
        org.h2.tools.Restore.execute(backups.getFirst().toString(),dir.resolve("restored").toString(),"inventory");
        try(Connection c=DriverManager.getConnection("jdbc:h2:file:"+dir.resolve("restored/inventory")+";DATABASE_TO_LOWER=TRUE","sa","")) {
            assertTrue(Migrations.exists(c,"shifts"));try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT quantity FROM products")){r.next();assertEquals("8.750",r.getBigDecimal(1).toPlainString());}
        }
    }
    @Test void interruptedColumnRenameResumesWithoutMultiplyingGramsAgain() throws Exception {
        try(Connection c=DriverManager.getConnection("jdbc:h2:mem:resume_"+UUID.randomUUID()+";DATABASE_TO_LOWER=TRUE","sa","")) {
            DatabaseInitializer.initialize(c);
            try(Statement s=c.createStatement()) {
                s.execute("INSERT INTO products(name,category,quantity,price,supplier,unit,low_stock) VALUES('Rice','Food',230,49.99,'Farm','KG',50)");
                s.execute("DELETE FROM schema_migrations");s.execute("ALTER TABLE products RENAME COLUMN quantity TO quantity_v130");
            }
            DatabaseInitializer.initialize(c);
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT quantity FROM products")){r.next();assertEquals(230,r.getLong(1));}
        }
    }
    @Test void backupFailureStopsBeforeChangingLegacySchema(@TempDir Path dir) throws Exception {
        String url="jdbc:h2:file:"+dir.resolve("inventory")+";DATABASE_TO_LOWER=TRUE";
        try(Connection c=DriverManager.getConnection(url,"sa","");var input=Files.newBufferedReader(Path.of("test-fixtures/v121.sql"))) {
            org.h2.tools.RunScript.execute(c,input);Files.writeString(dir.resolve("migration-backups"),"blocked");
            assertThrows(SQLException.class,()->DatabaseInitializer.initialize(c));assertTrue(Migrations.exists(c,"shifts"));
            try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,"products","quantity")){r.next();assertEquals(Types.DECIMAL,r.getInt("DATA_TYPE"));}
        }
    }
    @Test void freshSchemaHasIntegerQuantitiesAndNoDefaultUsers() throws Exception {
        try(Connection c=DriverManager.getConnection("jdbc:h2:mem:empty_"+UUID.randomUUID()+";DATABASE_TO_LOWER=TRUE","sa","")) {
            DatabaseInitializer.initialize(c);assertFalse(Migrations.exists(c,"shifts"));
            try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,"products","quantity")){assertTrue(r.next());assertEquals(Types.BIGINT,r.getInt("DATA_TYPE"));}
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM users")){r.next();assertEquals(0,r.getInt(1));}
        }
    }
}
