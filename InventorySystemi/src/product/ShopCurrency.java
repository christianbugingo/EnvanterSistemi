package product;
import java.math.*;
import java.text.*;
import java.util.*;
public record ShopCurrency(String code,String name,String symbol,int minorUnits) {
    public ShopCurrency {if(minorUnits< -1||minorUnits>4)throw new IllegalArgumentException("Invalid currency precision");}
    public BigDecimal round(BigDecimal value){if(minorUnits<0)throw services.AuthService.failure("currency.invalid");return value.setScale(minorUnits,RoundingMode.HALF_UP);}
    public String format(BigDecimal amount){
        if(minorUnits<0)throw services.AuthService.failure("currency.invalid");
        DecimalFormat format=new DecimalFormat("#,##0"+(minorUnits==0?"":"."+"0".repeat(minorUnits)),DecimalFormatSymbols.getInstance(Locale.ROOT));
        format.setRoundingMode(RoundingMode.HALF_UP);return format.format(amount)+" "+code;
    }
    @Override public String toString(){return code+" – "+name+" – "+symbol;}
}
