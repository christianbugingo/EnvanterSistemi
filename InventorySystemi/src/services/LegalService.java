package services;
import database.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
public final class LegalService {
    public static final String VERSION="1.3.0";
    public static String text(String file){try(var in=LegalService.class.getResourceAsStream("/legal/"+file)){return new String(java.util.Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);}catch(java.io.IOException ex){throw new IllegalStateException(ex);}}
    private static String digest(){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((text("TERMS.md")+text("PRIVACY.md")).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
    public static boolean accepted(){try(Connection c=DBConnection.connect()){return VERSION.equals(ShopSettings.get(c,"terms_version",""))&&digest().equals(ShopSettings.get(c,"terms_digest",""));}catch(SQLException ex){throw AuthService.databaseError(ex);}}
    public static void accept(){try(Connection c=DBConnection.connect()){c.setAutoCommit(false);try{ShopSettings.put(c,"terms_version",VERSION);ShopSettings.put(c,"terms_digest",digest());ShopSettings.put(c,"terms_accepted_at",java.time.Instant.now().toString());c.commit();}catch(Exception ex){c.rollback();throw ex;}}catch(SQLException ex){throw AuthService.databaseError(ex);}}
    public static String acceptedAt(){try(Connection c=DBConnection.connect()){return ShopSettings.get(c,"terms_accepted_at","");}catch(SQLException ex){throw AuthService.databaseError(ex);}}
}
