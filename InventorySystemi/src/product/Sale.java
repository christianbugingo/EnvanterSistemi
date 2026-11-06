package product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Sale {
    private int saleId;
    private String checkoutId="";
    private final int productId, userId;
    private final long milliQuantity; // Integer grams for weight; integer thousandths for piece validation.
    private final LocalDate saleDate;
    private String productName="", soldBy="";
    private BigDecimal price=BigDecimal.ZERO;
    private Unit unit=Unit.PIECE;
    private boolean voided;
    private ShopCurrency currency;
    private BigDecimal taxRate=BigDecimal.ZERO;
    public ShopCurrency getCurrency(){return currency;}
    public BigDecimal getTaxRate(){return taxRate;}
    public void setFinancialSnapshot(ShopCurrency value,BigDecimal rate){currency=value;taxRate=rate;}
    public String getPriceLabel(){return effectiveCurrency().format(price);}
    public String getTotalLabel(){return effectiveCurrency().format(getTotal());}
    private ShopCurrency effectiveCurrency(){return currency==null?services.Currencies.snapshot(null,2):currency;}
    private PaymentMethod method;
    public void setPaymentMethod(PaymentMethod value){method=value;}
    public String getPaymentLabel(){return method==null?gui.LocaleManager.get("payment.legacy"):method.toString();}
    public Sale(int id, int product, int user, long quantity, LocalDate date) {
        this(id, product, user, BigDecimal.valueOf(quantity), date);
    }
    public Sale(int id, int product, int user, BigDecimal quantity, LocalDate date) {
        saleId=id; productId=product; userId=user; milliQuantity=quantity.movePointRight(3).longValueExact(); saleDate=date;
    }
    public String getCheckoutId() { return checkoutId; }
    public void setCheckoutId(String value) { checkoutId=value==null?"":value; }
    public int getSaleId() { return saleId; }
    public void setSaleId(int id) { saleId=id; }
    public int getProductId() { return productId; }
    public int getUserId() { return userId; }
    public BigDecimal getQuantitySold() { return BigDecimal.valueOf(milliQuantity,3); }
    public LocalDate getSaleDate() { return saleDate; }
    public String getProductName() { return productName; }
    public void setProductName(String value) { productName=value; }
    public String getSoldBy() { return soldBy; }
    public void setSoldBy(String value) { soldBy=value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(long value) { price=BigDecimal.valueOf(value); }
    public void setPrice(BigDecimal value) { price=value; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit value) { unit=value; }
    public boolean isVoided() { return voided; }
    public void setVoided(boolean value) { voided=value; }
    public long getStoredQuantity() {return unit.toStored(unit.validate(getQuantitySold(),false));}
    public BigDecimal getTotal() { return unit==Unit.KG?Quantities.total(getStoredQuantity(),price,effectiveCurrency()):effectiveCurrency().round(price.multiply(getQuantitySold())); }
}
