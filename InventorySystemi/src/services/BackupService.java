package services;
import database.DBConnection;
import java.nio.file.*;
import java.sql.*;
public final class BackupService {
    public void create(Path destination) {
        try(Connection c=DBConnection.connect()) {
            var actor=AuthService.require(c,true);
            if(Files.exists(destination))throw AuthService.failure("backup.exists");
            if(!c.getMetaData().getURL().startsWith("jdbc:h2:file:"))throw AuthService.failure("backup.file.only");
            try(Statement st=c.createStatement()) {st.execute("BACKUP TO '"+destination.toAbsolutePath().toString().replace("'","''")+"'");}
            AuditService.record(c,actor,"BACKUP_CREATED",destination.getFileName().toString());
        } catch(SQLException e){throw AuthService.databaseError(e);}
    }
}
