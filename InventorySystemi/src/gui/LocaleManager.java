package gui;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Locale;

public class LocaleManager {
    private static Locale locale = Locale.ENGLISH;
    private static ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);

    public static void setLanguage(String language) {
        locale = "tr".equalsIgnoreCase(language) ? Locale.forLanguageTag("tr-TR") : Locale.ENGLISH;
        bundle = ResourceBundle.getBundle("messages", locale);
    }

    public static String getLanguage() { return locale.getLanguage(); }
    
    // For simple keys without parameters
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }
    
    // For keys with {0}, {1}, {2} parameters
    public static String get(String key, Object... params) {
        try {
            String pattern = bundle.getString(key);
            return MessageFormat.format(pattern, params);
        } catch (Exception e) {
            return key;
        }
    }
}
