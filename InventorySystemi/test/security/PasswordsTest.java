package security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PasswordsTest {
    @Test void uniqueSaltAndCorrectVerificationIncludingLegacyUpgradeInputs() {
        String first=Passwords.hash("PrivatePassword123!"),second=Passwords.hash("PrivatePassword123!");
        assertNotEquals(first,second);assertFalse(first.contains("PrivatePassword"));
        assertTrue(Passwords.verify("PrivatePassword123!",first));assertFalse(Passwords.verify("wrong",first));
        assertTrue(Passwords.verify("legacy","legacy"));assertFalse(Passwords.verify("wrong","legacy"));
        assertFalse(Passwords.verify("test","pbkdf2$corrupt"));assertFalse(Passwords.verify("test",null));
    }
}
