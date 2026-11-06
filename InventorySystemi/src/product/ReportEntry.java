package product;
import java.time.LocalDate;
import java.math.BigDecimal;
public record ReportEntry(LocalDate date,long storedQuantity,BigDecimal subtotal,BigDecimal tax,Unit unit,PaymentMethod method,ShopCurrency currency,BigDecimal taxRate) {
    public BigDecimal totalQuantity(){return unit.fromStored(storedQuantity);}
    public LocalDate getDate(){return date;}
    public BigDecimal getTotalQuantity(){return totalQuantity();}
    public BigDecimal getTotalRevenue(){return currency.round(subtotal.add(tax));}
    public String getPaymentLabel(){return method==null?gui.LocaleManager.get("payment.legacy"):method.toString();}
    public String getQuantityLabel(){return Quantities.format(totalQuantity(),unit);}
    public String getUnit(){return unit.label();}
    public String getSubtotalLabel(){return taxRate.signum()==0?"":currency.format(subtotal);}
    public String getTaxLabel(){return taxRate.signum()==0?"":taxRate.stripTrailingZeros().toPlainString()+"% · "+currency.format(tax);}
    public String getTotalLabel(){return currency.format(getTotalRevenue());}
}
