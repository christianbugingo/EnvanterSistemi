package services;

import database.DBConnection;
import java.sql.*;
import java.util.*;

public final class AuditService {
    public record Entry(String time, String username, String action, String details, String workstation) { }
    static void record(Connection c, int userId, String username, String action, String details) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("INSERT INTO audit_log(user_id, username, action, details, workstation) VALUES(?,?,?,?,?)")) {
            ps.setInt(1,userId); ps.setString(2,username); ps.setString(3,action); ps.setString(4,details);
            ps.setString(5,workstation()); ps.executeUpdate();
        }
    }
    static void record(Connection c, AuthService.User actor, String action, String details) throws SQLException {
        record(c,actor.id(),actor.username(),action,details);
    }
    public List<Entry> load() {
        try (Connection c=DBConnection.connect()) {
            AuthService.require(c,true);
            try (PreparedStatement ps=c.prepareStatement("SELECT created_at, username, action, details, workstation FROM audit_log ORDER BY audit_id DESC LIMIT 500"); ResultSet rs=ps.executeQuery()) {
                List<Entry> rows=new ArrayList<>();
                while(rs.next()) rows.add(new Entry(rs.getTimestamp(1).toString(),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)));
                return rows;
            }
        } catch(SQLException e) { throw AuthService.databaseError(e); }
    }
    private static String workstation() {
        try { return java.net.InetAddress.getLocalHost().getHostName(); }
        catch(Exception e) { return System.getProperty("os.name", "Desktop"); }
    }
}
