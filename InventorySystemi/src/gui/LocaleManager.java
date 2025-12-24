package gui;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class LocaleManager {
    public static final ResourceBundle bundle;
    
    static {
        // Force Turkish locale
        Locale.setDefault(new Locale("tr", "TR"));
        
        // Load Turkish properties file (messages_tr.properties)
        try {
            bundle = ResourceBundle.getBundle("messages_tr");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load properties file: messages_tr.properties", e);
        }
    }
    
    // For simple keys without parameters
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "[" + key + "]";
        }
    }
    
    // For keys with {0}, {1}, {2} parameters
    public static String get(String key, Object... params) {
        try {
            String pattern = bundle.getString(key);
            return MessageFormat.format(pattern, params);
        } catch (Exception e) {
            return "[" + key + "]";
        }
    }
}