package product;
import java.math.*;
public record Amounts(BigDecimal subtotal,BigDecimal tax,BigDecimal total) {
    public static Amounts calculate(BigDecimal subtotal,BigDecimal rate,ShopCurrency currency){
        services.ShopSettings.validateTax(rate);BigDecimal net=currency.round(subtotal);BigDecimal tax=net.multiply(rate).divide(new BigDecimal("100"),currency.minorUnits(),RoundingMode.HALF_UP);
        return new Amounts(net,tax,net.add(tax));
    }
}
