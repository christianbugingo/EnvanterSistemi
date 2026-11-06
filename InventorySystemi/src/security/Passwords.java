package security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class Passwords {
    public static String hash(String password) {
        if(password==null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException(gui.LocaleManager.get("password.invalid"));
        return org.mindrot.jbcrypt.BCrypt.hashpw(password,org.mindrot.jbcrypt.BCrypt.gensalt(12));
    }
    public static boolean needsUpgrade(String hash) {return !hash.startsWith("$2a$12$");}
    public static void validate(String username,String password) {
        if(password==null || password.length()<10 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72
            || !password.codePoints().anyMatch(Character::isLetter) || !password.codePoints().anyMatch(Character::isDigit)
            || password.equalsIgnoreCase(username)) throw new IllegalStateException(gui.LocaleManager.get("password.invalid"));
    }
    public static String recoveryCode() {
        byte[] bytes=new byte[24];new SecureRandom().nextBytes(bytes);
        return java.util.HexFormat.of().withUpperCase().formatHex(bytes);
    }
    private Passwords() { }
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        if(stored.startsWith("$2")) { try {return org.mindrot.jbcrypt.BCrypt.checkpw(password,stored);}catch(RuntimeException ex){return false;} }
        if (!stored.startsWith("pbkdf2$")) { // Upgrade legacy accounts after successful login.
            return MessageDigest.isEqual(password.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            String[] fields = stored.split("\\$");
            int iterations = Integer.parseInt(fields[1]);
            if (iterations < 10_000 || iterations > 1_000_000) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(fields[3]),
                    derive(password, Base64.getDecoder().decode(fields[2]), iterations));
        } catch (RuntimeException e) { return false; }
    }
    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
}
