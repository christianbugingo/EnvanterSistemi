package services;

import database.DBConnection;
import gui.LocaleManager;
import security.Passwords;
import java.sql.*;
import java.util.*;

public final class AuthService {
    public record User(int id, String username, String role, boolean mustChangePassword, boolean active) {
        public boolean isAdmin() { return "admin".equals(role); }
    }
    private static User current;
    private static String currentHash;
    public static User current() { return current; }
    public static void clearSession() { current=null; currentHash=null; }
    public boolean hasUsers() {
        try(Connection c=DBConnection.connect(); Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next(); return rs.getInt(1)>0;
        } catch(SQLException e) { throw databaseError(e); }
    }
    public void createFirstAdmin(String name, String password) {
        validateName(name); validatePassword(password);
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement lock=c.prepareStatement("SELECT setting_value FROM app_settings WHERE setting_key='bootstrap' FOR UPDATE"); ResultSet rs=lock.executeQuery()) { rs.next(); }
                try(Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM users")) {
                    rs.next(); if(rs.getInt(1)>0) throw failure("login.setup.exists");
                }
                int id=insertUser(c,name.trim(),password,"admin",false);
                AuditService.record(c,id,name.trim(),"ADMIN_CREATED","");
                c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw databaseError(e); }
    }
    public User login(String username, String password) {
        clearSession();
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement ps=c.prepareStatement("SELECT * FROM users WHERE username=? FOR UPDATE")) {
                    ps.setString(1,username.trim());
                    try(ResultSet rs=ps.executeQuery()) {
                        if(!rs.next() || !rs.getBoolean("active") || !Passwords.verify(password,rs.getString("password"))) throw failure("login.failed");
                        User user=readUser(rs);
                        if(!Set.of("admin","staff").contains(user.role())) throw failure("msg.access.denied");
                        String stored=rs.getString("password");
                        if(!stored.startsWith("pbkdf2$")) {
                            stored=Passwords.hash(password);
                            try(PreparedStatement update=c.prepareStatement("UPDATE users SET password=? WHERE user_id=?")) {
                                update.setString(1,stored); update.setInt(2,user.id()); update.executeUpdate();
                            }
                        }
                        AuditService.record(c,user,"LOGIN",""); c.commit();
                        current=user; currentHash=stored; return user;
                    }
                }
            } catch(Exception e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw databaseError(e); }
    }
    /** Each operation rechecks role, active flag, password reset and pending password change. */
    static User require(Connection c, boolean admin) throws SQLException {
        User user=checkSession(c);
        if(user.mustChangePassword() || (admin && !user.isAdmin())) throw failure("msg.access.denied");
        return user;
    }
    private static User checkSession(Connection c) throws SQLException {
        if(current==null) throw failure("session.expired");
        try(PreparedStatement ps=c.prepareStatement("SELECT * FROM users WHERE user_id=? FOR UPDATE")) {
            ps.setInt(1,current.id());
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next() || !rs.getBoolean("active") || !Objects.equals(currentHash,rs.getString("password"))) throw failure("session.expired");
                User user=readUser(rs);
                if(!Set.of("admin","staff").contains(user.role())) throw failure("msg.access.denied");
                return user;
            }
        }
    }
    public void changePassword(String oldPassword, String replacement) {
        validatePassword(replacement);
        if(Objects.equals(oldPassword,replacement)) throw failure("password.different");
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                User user=checkSession(c);
                if(!Passwords.verify(oldPassword,currentHash)) throw failure("login.failed");
                String hash=Passwords.hash(replacement);
                try(PreparedStatement ps=c.prepareStatement("UPDATE users SET password=?, must_change_password=FALSE WHERE user_id=?")) {
                    ps.setString(1,hash); ps.setInt(2,user.id()); ps.executeUpdate();
                }
                AuditService.record(c,user,"PASSWORD_CHANGED",""); c.commit();
                current=new User(user.id(),user.username(),user.role(),false,true); currentHash=hash;
            } catch(Exception e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw databaseError(e); }
    }
    public void createStaff(String username, String temporaryPassword) {
        validateName(username); validatePassword(temporaryPassword);
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                User admin=require(c,true);
                insertUser(c,username.trim(),temporaryPassword,"staff",true);
                AuditService.record(c,admin,"STAFF_CREATED",username.trim()); c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        } catch(SQLException e) {
            if("23505".equals(e.getSQLState()) || e.getErrorCode()==1062) throw failure("user.exists");
            throw databaseError(e);
        }
    }
    public List<User> users() {
        try(Connection c=DBConnection.connect()) {
            require(c,true); List<User> users=new ArrayList<>();
            try(Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT * FROM users ORDER BY user_id")) { while(rs.next()) users.add(readUser(rs)); }
            return users;
        } catch(SQLException e) { throw databaseError(e); }
    }
    public void resetStaffPassword(int id, String temporaryPassword) {
        validatePassword(temporaryPassword);
        updateStaff(id,temporaryPassword,null);
    }
    public void setStaffActive(int id, boolean active) { updateStaff(id,null,active); }
    private void updateStaff(int id, String password, Boolean active) {
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                User admin=require(c,true);
                String sql=password!=null ? "UPDATE users SET password=?, must_change_password=TRUE WHERE user_id=? AND role='staff'" : "UPDATE users SET active=? WHERE user_id=? AND role='staff'";
                try(PreparedStatement ps=c.prepareStatement(sql)) {
                    if(password!=null) ps.setString(1,Passwords.hash(password)); else ps.setBoolean(1,active);
                    ps.setInt(2,id); if(ps.executeUpdate()!=1) throw failure("user.staff.only");
                }
                AuditService.record(c,admin,password!=null?"PASSWORD_RESET":Boolean.TRUE.equals(active)?"STAFF_ENABLED":"STAFF_DISABLED", "#"+id); c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw databaseError(e); }
    }
    public void logout() {
        try {
            if(current!=null) try(Connection c=DBConnection.connect()) {
                AuditService.record(c,current,"LOGOUT","");
            }
        } catch(RuntimeException | SQLException e) { /* Always allow a local session to close. */ }
        finally { clearSession(); }
    }
    private static int insertUser(Connection c, String name, String password, String role, boolean change) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO users(username,password,role,must_change_password) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,name); ps.setString(2,Passwords.hash(password)); ps.setString(3,role); ps.setBoolean(4,change); ps.executeUpdate();
            try(ResultSet rs=ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }
    private static User readUser(ResultSet rs) throws SQLException { return new User(rs.getInt("user_id"),rs.getString("username"),rs.getString("role"),rs.getBoolean("must_change_password"),rs.getBoolean("active")); }
    private static void validateName(String value) { if(value==null || !value.trim().matches("[\\p{L}\\p{N}_.-]{3,100}")) throw failure("user.invalid"); }
    private static void validatePassword(String value) { if(value==null || value.length()<8 || value.length()>128 || value.isBlank()) throw failure("password.invalid"); }
    public static IllegalStateException failure(String key) { return new IllegalStateException(LocaleManager.get(key)); }
    static IllegalStateException databaseError(SQLException e) { return new IllegalStateException(LocaleManager.get("error.database"),e); }
}
