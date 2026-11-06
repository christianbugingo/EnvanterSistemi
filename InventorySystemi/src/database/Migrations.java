package database;

import java.sql.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;

/** Restartable migration steps: committed H2 backup, archive, exact conversion, version marker last. */
final class Migrations {
    private Migrations() { }
    static boolean exists(Connection c,String table) throws SQLException {
        try(ResultSet rs=c.getMetaData().getTables(c.getCatalog(),null,table,null)){return rs.next();}
    }
    static void before(Connection c) throws SQLException {
        boolean applied=false;
        if(exists(c,"schema_migrations"))try(Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM schema_migrations WHERE version='1.3.0-batches'")){rs.next();applied=rs.getInt(1)>0;}
        if(applied)return;
        String url=c.getMetaData().getURL();
        if(url.startsWith("jdbc:h2:file:")) {
            try {
                String location=url.substring("jdbc:h2:file:".length()).split(";")[0];
                if(location.startsWith("~/"))location=System.getProperty("user.home")+location.substring(1);
                Path dir=Path.of(location).toAbsolutePath().getParent().resolve("migration-backups");Files.createDirectories(dir);
                Path backup=dir.resolve("before-1.3.0-"+UUID.randomUUID()+".zip");
                try(Statement s=c.createStatement()){s.execute("BACKUP TO '"+backup.toString().replace("'","''")+"'");}
                try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(backup.toFile())) {if(!zip.entries().hasMoreElements())throw new java.io.IOException("Empty migration backup");}
            }catch(java.io.IOException ex){throw new SQLException("Cannot back up database; upgrade stopped",ex);}
        }
    }
    static void apply(Connection c) throws SQLException {
        try(Statement s=c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS schema_migrations(version VARCHAR(30) PRIMARY KEY,applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            try(ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM schema_migrations WHERE version='1.3.0'")){rs.next();if(rs.getInt(1)>0){global(c);return;}}
            s.execute("CREATE TABLE IF NOT EXISTS migration_archive(source_table VARCHAR(50),row_id VARCHAR(50),payload CLOB,PRIMARY KEY(source_table,row_id))");
        }
        archive(c,"shifts","shift_id");
        if(column(c,"checkouts","shift_id"))archive(c,"checkouts","checkout_id");
        convert(c,"products","product_id","quantity");convert(c,"products","product_id","low_stock");convert(c,"sales","sale_id","quantity_sold");
        try(var stream=Migrations.class.getResourceAsStream("/migrations/V130__offline_shop.sql")) {
            String sql=new String(Objects.requireNonNull(stream).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).replaceAll("(?m)--.*$","");
            for(String statement:sql.split(";"))if(!statement.isBlank())try(Statement s=c.createStatement()){s.execute(statement);}
        }catch(java.io.IOException ex){throw new SQLException(ex);}
        try(Statement s=c.createStatement()){s.execute("INSERT INTO schema_migrations(version) VALUES('1.3.0')");}
        global(c);
    }
    private static void global(Connection c) throws SQLException {
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM schema_migrations WHERE version='1.3.0-global'")){r.next();if(r.getInt(1)>0){batches(c);return;}}
        try(var in=Migrations.class.getResourceAsStream("/migrations/V130_global__currency_tax_legal.sql")) {
            String text=new String(java.util.Objects.requireNonNull(in).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            for(String sql:text.split(";"))if(!sql.isBlank())try(Statement s=c.createStatement()){s.execute(sql);}
        }catch(java.io.IOException ex){throw new SQLException(ex);}
        for(var value:services.Currencies.all())try(PreparedStatement p=c.prepareStatement("MERGE INTO currencies KEY(code) VALUES(?,?,?,?)")){p.setString(1,value.code());p.setString(2,value.name());p.setString(3,value.symbol());p.setInt(4,value.minorUnits());p.executeUpdate();}
        try(Statement s=c.createStatement()){s.execute("INSERT INTO schema_migrations(version) VALUES('1.3.0-global')");}
        batches(c);
    }
    private static void batches(Connection c) throws SQLException {
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM schema_migrations WHERE version='1.3.0-batches'")){r.next();if(r.getInt(1)>0)return;}
        try(var in=Migrations.class.getResourceAsStream("/migrations/V130_batches__fifo.sql")){
            String sql=new String(Objects.requireNonNull(in).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            for(String part:sql.split(";"))if(!part.isBlank())try(Statement s=c.createStatement()){s.execute(part);}
        }catch(java.io.IOException ex){throw new SQLException(ex);}
        try(Statement s=c.createStatement()){s.execute("INSERT INTO schema_migrations(version) VALUES('1.3.0-batches')");}
    }
    private static boolean column(Connection c,String table,String col) throws SQLException {try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,table,col)){return r.next();}}
    private static void archive(Connection c,String table,String id) throws SQLException {
        if(!exists(c,table))return;
        try(Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT * FROM "+table)) {
            while(rs.next()) {
                StringBuilder payload=new StringBuilder();
                for(int i=1;i<=rs.getMetaData().getColumnCount();i++)payload.append(rs.getMetaData().getColumnName(i)).append('=').append(rs.getString(i)).append('\n');
                try(PreparedStatement p=c.prepareStatement("MERGE INTO migration_archive KEY(source_table,row_id) VALUES(?,?,?)")){p.setString(1,table);p.setString(2,rs.getString(id));p.setString(3,payload.toString());p.executeUpdate();}
            }
        }
    }
    private static void convert(Connection c,String table,String id,String col) throws SQLException {
        String temp=col+"_v130";
        if(!column(c,table,col) && column(c,table,temp)) {
            try(Statement s=c.createStatement()){s.execute("ALTER TABLE "+table+" RENAME COLUMN "+temp+" TO "+col);}
            constrain(c,table,col);return;
        }
        try(ResultSet info=c.getMetaData().getColumns(c.getCatalog(),null,table,col)) {
            if(info.next()&&info.getInt("DATA_TYPE")==Types.BIGINT){constrain(c,table,col);return;}
        }
        try(Statement s=c.createStatement()){s.execute("ALTER TABLE "+table+" ADD COLUMN IF NOT EXISTS "+temp+" BIGINT");}
        try(Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT "+id+","+col+",unit FROM "+table)) {
            while(rs.next()) {
                BigDecimal value=rs.getBigDecimal(2);long exact=("KG".equals(rs.getString(3))?value.movePointRight(3):value).longValueExact();
                try(PreparedStatement p=c.prepareStatement("UPDATE "+table+" SET "+temp+"=? WHERE "+id+"=?")){p.setLong(1,exact);p.setInt(2,rs.getInt(1));p.executeUpdate();}
            }
        }catch(ArithmeticException ex){throw new SQLException("Quantity cannot be converted exactly; restore migration backup",ex);}
        try(Statement s=c.createStatement()) {
            s.execute("ALTER TABLE "+table+" DROP COLUMN "+col);
            s.execute("ALTER TABLE "+table+" RENAME COLUMN "+temp+" TO "+col);
        }
        constrain(c,table,col);
    }
    private static void constrain(Connection c,String table,String col) throws SQLException {
        try(Statement s=c.createStatement()) {
            s.execute("ALTER TABLE "+table+" ALTER COLUMN "+col+" SET NOT NULL");
            s.execute("ALTER TABLE "+table+" ADD CONSTRAINT IF NOT EXISTS ck_"+table+"_"+col+" CHECK ("+col+(table.equals("sales")?">0":">=0")+")");
        }
    }
}
