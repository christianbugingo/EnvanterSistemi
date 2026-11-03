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
    @Test
    void bothLanguagesCoverEveryLiteralUiKey() throws Exception {
        var en=java.util.ResourceBundle.getBundle("messages",java.util.Locale.ENGLISH);
        var tr=java.util.ResourceBundle.getBundle("messages",java.util.Locale.forLanguageTag("tr-TR"));
        assertEquals(en.keySet(),tr.keySet());
        var pattern=java.util.regex.Pattern.compile("(?:LocaleManager\\.get|Ui\\.button|Ui\\.column|Ui\\.numberColumn|failure|tab|item)\\(\"([a-z][a-z0-9_.]+)\"");
        try(var files=java.nio.file.Files.walk(java.nio.file.Path.of("src"))) {
            for(var file:files.filter(p->p.toString().endsWith(".java")).toList()) {
                var matcher=pattern.matcher(java.nio.file.Files.readString(file));
                while(matcher.find()) {
                    String key=matcher.group(1);if(key.endsWith("."))continue;
                    org.junit.jupiter.api.Assertions.assertTrue(en.containsKey(key),file+": "+key);
                    org.junit.jupiter.api.Assertions.assertTrue(tr.containsKey(key),file+": "+key);
                    org.junit.jupiter.api.Assertions.assertFalse(en.getString(key).isBlank(),key);
                }
            }
        }
    }
}
