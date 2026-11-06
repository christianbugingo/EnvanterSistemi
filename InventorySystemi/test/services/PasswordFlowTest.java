package services;
import database.*;
import services.LegalService;
import security.Passwords;
import org.junit.jupiter.api.*;
import java.time.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PasswordFlowTest {
    Instant now=Instant.parse("2026-01-01T00:00:00Z");AuthService auth;String recovery;
    @BeforeEach void setup() throws Exception {
        System.setProperty("DB_URL","jdbc:h2:mem:password_"+UUID.randomUUID()+";DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");System.setProperty("DB_USER","sa");System.setProperty("DB_PASSWORD","");DatabaseInitializer.initialize();
        auth=new AuthService(new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}});
        LegalService.accept();recovery=auth.createFirstAdmin("owner","OwnerPassword123!");
    }
    @AfterEach void cleanup(){AuthService.clearSession();for(String k:List.of("DB_URL","DB_USER","DB_PASSWORD"))System.clearProperty(k);}
    @Test void fifthFailureLocksForExactlyFiveMinutesAndAuditsEveryAttempt() throws Exception {
        for(int i=0;i<5;i++)assertThrows(IllegalStateException.class,()->auth.login("owner","WrongPassword1"));
        assertEquals(300,auth.lockSeconds("owner"));assertThrows(IllegalStateException.class,()->auth.login("owner","OwnerPassword123!"));
        now=now.plusSeconds(299);assertEquals(1,auth.lockSeconds("owner"));now=now.plusSeconds(1);assertEquals(0,auth.lockSeconds("owner"));
        auth.login("owner","OwnerPassword123!");assertEquals(6,new AuditService().load().stream().filter(a->a.action().equals("LOGIN_FAILED")).count());
    }
    @Test void recoveryIsOneTimeForcesChangeAndReplacementInvalidatesPreviousCode() throws Exception {
        String next=auth.recoverAdministrator("owner",recovery,"ResetPassword123!");
        assertThrows(IllegalStateException.class,()->auth.recoverAdministrator("owner",recovery,"AnotherPassword123!"));
        assertTrue(auth.login("owner","ResetPassword123!").mustChangePassword());assertThrows(IllegalStateException.class,()->new ProductService().loadProducts());
        auth.changePassword("ResetPassword123!","PrivatePassword456!");String newest=auth.generateRecoveryCode();
        assertThrows(IllegalStateException.class,()->auth.recoverAdministrator("owner",next,"AnotherPassword123!"));
        auth.recoverAdministrator("owner",newest,"AnotherPassword123!");
        try(Connection c=DBConnection.connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT recovery_hash FROM users")){r.next();assertFalse(r.getString(1).contains(newest));}
    }
    @Test void beforeLoginChangeAndLastThreePasswordsAndStaffReset() {
        auth.changePassword("owner","OwnerPassword123!","SecondPassword123!");assertNull(AuthService.current());
        auth.login("owner","SecondPassword123!");auth.changePassword("SecondPassword123!","ThirdPassword123!");auth.changePassword("ThirdPassword123!","FourthPassword123!");
        assertThrows(IllegalStateException.class,()->auth.changePassword("FourthPassword123!","OwnerPassword123!"));
        auth.createStaff("cashier","TemporaryPassword123!");int id=auth.users().stream().filter(u->!u.isAdmin()).findFirst().orElseThrow().id();
        assertThrows(IllegalStateException.class,()->auth.recoverAdministrator("cashier",recovery,"AnotherPassword123!"));
        auth.login("owner","FourthPassword123!");auth.resetStaffPassword(id,"NewTemporary123!");
        assertTrue(auth.login("cashier","NewTemporary123!").mustChangePassword());
    }
    @Test void resettingLegacyStaffNeverCopiesPlaintextIntoPasswordHistory() throws Exception {
        auth.login("owner","OwnerPassword123!");
        try(Connection c=DBConnection.connect();Statement s=c.createStatement()){s.execute("INSERT INTO users(username,password,role) VALUES('legacy','OldStaffPassword123!','staff')");}
        int id=auth.users().stream().filter(u->u.username().equals("legacy")).findFirst().orElseThrow().id();auth.resetStaffPassword(id,"NewStaffPassword123!");
        try(Connection c=DBConnection.connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT password_hash FROM password_history WHERE user_id="+id)){assertTrue(r.next());assertTrue(r.getString(1).startsWith("$2a$12$"));assertTrue(Passwords.verify("OldStaffPassword123!",r.getString(1)));}
    }
    @Test void pbkdf2HashUpgradesOnSuccessfulLogin() throws Exception {
        byte[] salt=new byte[16];java.util.Arrays.fill(salt,(byte)1);
        var spec=new javax.crypto.spec.PBEKeySpec("OldPassword123!".toCharArray(),salt,210000,256);
        String old="pbkdf2$210000$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());
        try(Connection c=DBConnection.connect();PreparedStatement p=c.prepareStatement("UPDATE users SET password=? WHERE username='owner'")){p.setString(1,old);p.executeUpdate();}
        auth.login("owner","OldPassword123!");try(Connection c=DBConnection.connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT password FROM users")){r.next();assertTrue(r.getString(1).startsWith("$2a$12$"));}
    }
}
