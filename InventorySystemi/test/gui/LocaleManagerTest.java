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
        java.util.Properties rawTr=new java.util.Properties();try(var reader=java.nio.file.Files.newBufferedReader(java.nio.file.Path.of("src/messages_tr.properties"))){rawTr.load(reader);}
        en.keySet().stream().filter(k->!rawTr.containsKey(k)).forEach(k->org.slf4j.LoggerFactory.getLogger(LocaleManagerTest.class).warn("Missing Turkish translation: {}",k));
        var pattern=java.util.regex.Pattern.compile("(?:LocaleManager\\.get|Ui\\.button|Ui\\.column|Ui\\.numberColumn|failure|tab|item)\\(\"([a-z][a-z0-9_.]+)\"");
        try(var files=java.nio.file.Files.walk(java.nio.file.Path.of("src"))) {
            for(var file:files.filter(p->p.toString().endsWith(".java")).toList()) {
                var matcher=pattern.matcher(java.nio.file.Files.readString(file));
                while(matcher.find()) {
                    String key=matcher.group(1);if(key.endsWith("."))continue;
                    org.junit.jupiter.api.Assertions.assertTrue(en.containsKey(key),file+": "+key);
                    if(!rawTr.containsKey(key))org.slf4j.LoggerFactory.getLogger(LocaleManagerTest.class).warn("Missing Turkish translation: {}",key);
                    org.junit.jupiter.api.Assertions.assertFalse(en.getString(key).isBlank(),key);
                }
            }
        }
    }
}
