package product;

import gui.LocaleManager;
import java.math.*;
import java.util.Locale;

/** Weight input is converted exactly to integer grams; unsuffixed input uses the selected field unit. */
public final class Quantities {
    private Quantities() { }
    public static long parseGrams(String text, boolean fieldInGrams) {
        if(text==null)throw new IllegalArgumentException(LocaleManager.get("validation.weight"));
        String value=text.trim().toLowerCase(Locale.ROOT);boolean grams=fieldInGrams;
        if(value.endsWith("kg")){grams=false;value=value.substring(0,value.length()-2).trim();}
        else if(value.endsWith("g")){grams=true;value=value.substring(0,value.length()-1).trim();}
        try {long result=LocaleManager.decimal(value).multiply(grams?BigDecimal.ONE:new BigDecimal("1000")).longValueExact();
            if(result<0 || result>99_999_999_999_000L)throw new ArithmeticException();return result;
        }catch(RuntimeException ex){throw new IllegalArgumentException(LocaleManager.get("validation.weight"));}
    }
    public static BigDecimal parse(String text,Unit unit) {
        return unit==Unit.KG?BigDecimal.valueOf(parseGrams(text,false),3):unit.validate(LocaleManager.decimal(text),true);
    }
    public static String format(BigDecimal value,Unit unit) {
        if(unit==Unit.PIECE)return LocaleManager.quantity(value)+" "+unit.label();
        long grams=unit.toStored(value);
        return Math.abs(grams)<1000?grams+" g":BigDecimal.valueOf(grams,3).stripTrailingZeros().toPlainString()+" kg";
    }
    public static BigDecimal total(long grams,BigDecimal perKg,ShopCurrency currency){return currency.round(perKg.multiply(BigDecimal.valueOf(grams)).movePointLeft(3));}
    public static BigDecimal total(long grams,BigDecimal perKg) {return perKg.multiply(BigDecimal.valueOf(grams)).divide(new BigDecimal("1000"),2,RoundingMode.HALF_UP);}
}
