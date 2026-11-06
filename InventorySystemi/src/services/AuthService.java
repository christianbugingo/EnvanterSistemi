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
    private final java.time.Clock clock;
    public AuthService(){this(java.time.Clock.systemUTC());}
    public AuthService(java.time.Clock clock){this.clock=clock;}
    public long lockSeconds(String username) {
        try(Connection c=DBConnection.connect();PreparedStatement p=c.prepareStatement("SELECT locked_until FROM users WHERE username=?")) {
            p.setString(1,username.trim());try(ResultSet r=p.executeQuery()){if(!r.next()||r.getTimestamp(1)==null)return 0;return Math.max(0,(java.time.Duration.between(clock.instant(),r.getTimestamp(1).toInstant()).toMillis()+999)/1000);}
        }catch(SQLException ex){throw databaseError(ex);}
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
    public String createFirstAdmin(String name, String password) {
        if(!LegalService.accepted())throw failure("legal.required");
        String recovery=Passwords.recoveryCode();
        validateName(name); Passwords.validate(name,password);
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement lock=c.prepareStatement("SELECT setting_value FROM app_settings WHERE setting_key='bootstrap' FOR UPDATE"); ResultSet rs=lock.executeQuery()) { rs.next(); }
                try(Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM users")) {
                    rs.next(); if(rs.getInt(1)>0) throw failure("login.setup.exists");
                }
                int id=insertUser(c,name.trim(),password,"admin",false);
                try(PreparedStatement p=c.prepareStatement("UPDATE users SET recovery_hash=? WHERE user_id=?")){p.setString(1,Passwords.hash(recovery));p.setInt(2,id);p.executeUpdate();}
                AuditService.record(c,id,name.trim(),"ADMIN_CREATED","");
                c.commit(); return recovery;
            } catch(Exception e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw databaseError(e); }
    }
    public User login(String username, String password) {
        clearSession();
        if(!LegalService.accepted())throw failure("legal.required");
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement ps=c.prepareStatement("SELECT * FROM users WHERE username=? FOR UPDATE")) {
                    ps.setString(1,username.trim());
                    try(ResultSet rs=ps.executeQuery()) {
                        if(!rs.next()) {AuditService.record(c,0,"unknown","LOGIN_FAILED","");c.commit();throw failure("login.failed");}
                        User candidate=readUser(rs);
                        Timestamp locked=rs.getTimestamp("locked_until");
                        if(locked!=null && locked.toInstant().isAfter(clock.instant())) {AuditService.record(c,candidate,"LOGIN_FAILED","Locked");c.commit();throw failure("login.locked");}
                        if(!rs.getBoolean("active") || !Passwords.verify(password,rs.getString("password"))) {
                            failed(c,candidate,locked==null?rs.getInt("failed_attempts"):0);c.commit();throw failure("login.failed");
                        }
                        try(PreparedStatement reset=c.prepareStatement("UPDATE users SET failed_attempts=0,locked_until=NULL WHERE user_id=?")){reset.setInt(1,candidate.id());reset.executeUpdate();}
                        User user=readUser(rs);
                        if(!Set.of("admin","staff").contains(user.role())) throw failure("msg.access.denied");
                        String stored=rs.getString("password");
                        if(Passwords.needsUpgrade(stored)) {
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

        if(Objects.equals(oldPassword,replacement)) throw failure("password.different");
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                User user=checkSession(c);
                if(!Passwords.verify(oldPassword,currentHash)) throw failure("login.failed");
                checkReplacement(c,user,replacement);
                remember(c,user.id(),currentHash);
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
        validateName(username); Passwords.validate(username,temporaryPassword);
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
        updateStaff(id,temporaryPassword,null);
    }
    public void setStaffActive(int id, boolean active) { updateStaff(id,null,active); }
    private void updateStaff(int id, String password, Boolean active) {
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                User admin=require(c,true);
                if(password!=null)try(PreparedStatement find=c.prepareStatement("SELECT * FROM users WHERE user_id=? FOR UPDATE")) {
                    find.setInt(1,id);try(ResultSet r=find.executeQuery()){if(!r.next())throw failure("user.staff.only");checkReplacement(c,readUser(r),password);remember(c,id,r.getString("password"));}
                }
                String sql=password!=null ? "UPDATE users SET password=?, must_change_password=TRUE,failed_attempts=0,locked_until=NULL WHERE user_id=? AND role='staff'" : "UPDATE users SET active=? WHERE user_id=? AND role='staff'";
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
    private void failed(Connection c,User user,int attempts) throws SQLException {
        int next=attempts+1;
        try(PreparedStatement p=c.prepareStatement("UPDATE users SET failed_attempts=?,locked_until=? WHERE user_id=?")) {
            p.setInt(1,next);if(next>=5)p.setTimestamp(2,Timestamp.from(clock.instant().plusSeconds(300)));else p.setNull(2,Types.TIMESTAMP);p.setInt(3,user.id());p.executeUpdate();
        }
        AuditService.record(c,user,"LOGIN_FAILED","");
    }
    private static void remember(Connection c,int userId,String hash) throws SQLException {
        if(!hash.startsWith("$2")&&!hash.startsWith("pbkdf2$"))hash=Passwords.hash(hash);
        try(PreparedStatement p=c.prepareStatement("INSERT INTO password_history(user_id,password_hash) VALUES(?,?)")){p.setInt(1,userId);p.setString(2,hash);p.executeUpdate();}
        try(PreparedStatement p=c.prepareStatement("DELETE FROM password_history WHERE user_id=? AND history_id NOT IN(SELECT history_id FROM password_history WHERE user_id=? ORDER BY history_id DESC LIMIT 3)")){p.setInt(1,userId);p.setInt(2,userId);p.executeUpdate();}
    }
    private static void checkReplacement(Connection c,User user,String replacement) throws SQLException {
        Passwords.validate(user.username(),replacement);
        try(PreparedStatement p=c.prepareStatement("SELECT password AS hash FROM users WHERE user_id=? UNION ALL SELECT password_hash AS hash FROM password_history WHERE user_id=?")) {
            p.setInt(1,user.id());p.setInt(2,user.id());try(ResultSet r=p.executeQuery()){while(r.next())if(Passwords.verify(replacement,r.getString(1)))throw failure("password.reused");}
        }
    }
    public void changePassword(String username,String oldPassword,String replacement) {
        try {login(username,oldPassword);changePassword(oldPassword,replacement);}finally{clearSession();}
    }
    public boolean isAdministrator(String username) {
        try(Connection c=DBConnection.connect();PreparedStatement p=c.prepareStatement("SELECT role FROM users WHERE username=?")){p.setString(1,username.trim());try(ResultSet r=p.executeQuery()){return r.next()&&"admin".equals(r.getString(1));}}
        catch(SQLException ex){throw databaseError(ex);}
    }
    /** Consumes recovery once; the returned replacement code must be saved immediately. */
    public String recoverAdministrator(String username,String code,String temporaryPassword) {
        clearSession();String replacement=Passwords.recoveryCode();
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try(PreparedStatement p=c.prepareStatement("SELECT * FROM users WHERE username=? FOR UPDATE")) {
                p.setString(1,username.trim());try(ResultSet r=p.executeQuery()) {
                    if(!r.next()||!"admin".equals(r.getString("role")))throw failure("recovery.staff");
                    User user=readUser(r);Timestamp locked=r.getTimestamp("locked_until");
                    if(locked!=null && locked.toInstant().isAfter(clock.instant())){AuditService.record(c,user,"LOGIN_FAILED","Locked recovery");c.commit();throw failure("login.locked");}
                    if(!Passwords.verify(code,r.getString("recovery_hash"))){failed(c,user,locked==null?r.getInt("failed_attempts"):0);c.commit();throw failure("recovery.invalid");}
                    checkReplacement(c,user,temporaryPassword);remember(c,user.id(),r.getString("password"));
                    try(PreparedStatement update=c.prepareStatement("UPDATE users SET password=?,recovery_hash=?,must_change_password=TRUE,failed_attempts=0,locked_until=NULL WHERE user_id=?")) {
                        update.setString(1,Passwords.hash(temporaryPassword));update.setString(2,Passwords.hash(replacement));update.setInt(3,user.id());update.executeUpdate();
                    }
                    AuditService.record(c,user,"ADMIN_RECOVERED","");c.commit();return replacement;
                }
            }catch(Exception ex){c.rollback();throw ex;}
        }catch(SQLException ex){throw databaseError(ex);}
    }
    public String generateRecoveryCode() {
        String code=Passwords.recoveryCode();
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);try{User user=require(c,true);
                try(PreparedStatement p=c.prepareStatement("UPDATE users SET recovery_hash=? WHERE user_id=?")){p.setString(1,Passwords.hash(code));p.setInt(2,user.id());p.executeUpdate();}
                AuditService.record(c,user,"RECOVERY_REPLACED","");c.commit();return code;
            }catch(Exception ex){c.rollback();throw ex;}
        }catch(SQLException ex){throw databaseError(ex);}
    }
    public static IllegalStateException failure(String key) { return new IllegalStateException(LocaleManager.get(key)); }
    static IllegalStateException databaseError(SQLException e) { return new IllegalStateException(LocaleManager.get("error.database"),e); }
}
