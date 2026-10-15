package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class LocaleManagerTest {
    @Test
    void supportsEnglishAndTurkishAndUtf8Text() {
        LocaleManager.setLanguage("en");
        assertEquals("Language", LocaleManager.get("language.label"));

        LocaleManager.setLanguage("tr");
        assertEquals("Dil", LocaleManager.get("language.label"));
        assertEquals("Kullanıcı adı:", LocaleManager.get("login.username"));
        assertEquals("Çıkış yap", LocaleManager.get("menu.logout"));

        LocaleManager.setLanguage("en");
    }
}
