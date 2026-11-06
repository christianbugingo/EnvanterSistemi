package gui;

import java.math.BigDecimal;
import java.text.*;
import java.util.*;

public final class LocaleManager {
    private static Locale locale=Locale.ENGLISH;
    private static ResourceBundle bundle=ResourceBundle.getBundle("messages",locale);
    public static void setLanguage(String language) {
        locale="tr".equalsIgnoreCase(language)?Locale.forLanguageTag("tr-TR"):Locale.ENGLISH;
        bundle=ResourceBundle.getBundle("messages",locale);
    }
    public static String getLanguage() { return locale.getLanguage(); }
    public static String get(String key, Object... args) {
        String pattern=bundle.containsKey(key)?bundle.getString(key):ResourceBundle.getBundle("messages",Locale.ENGLISH).getString(key);
        return args.length==0?pattern:new MessageFormat(pattern,locale).format(args);
    }
    public static String quantity(BigDecimal value) {
        NumberFormat format=NumberFormat.getNumberInstance(locale); format.setMaximumFractionDigits(3); format.setGroupingUsed(false);
        return format.format(value);
    }
    public static String money(BigDecimal value) {
        var currency=services.ShopSettings.current().currency();
        return currency==null?services.Currencies.snapshot(null,2).format(value):currency.format(value);
    }
    public static BigDecimal decimal(String text) {
        if(text==null || !text.trim().matches("[+]?(?:[0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)")) throw new IllegalArgumentException(get("validation.number"));
        return new BigDecimal(text.trim().replace(',','.'));
    }
}
