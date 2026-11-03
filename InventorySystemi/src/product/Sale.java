package product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Sale {
    private int saleId;
    private final int productId, userId;
    private final BigDecimal quantitySold;
    private final LocalDate saleDate;
    private String productName="", soldBy="";
    private BigDecimal price=BigDecimal.ZERO;
    private Unit unit=Unit.PIECE;
    private boolean voided;
    public Sale(int id, int product, int user, double quantity, LocalDate date) {
        this(id, product, user, BigDecimal.valueOf(quantity), date);
    }
    public Sale(int id, int product, int user, BigDecimal quantity, LocalDate date) {
        saleId=id; productId=product; userId=user; quantitySold=quantity; saleDate=date;
    }
    public int getSaleId() { return saleId; }
    public void setSaleId(int id) { saleId=id; }
    public int getProductId() { return productId; }
    public int getUserId() { return userId; }
    public BigDecimal getQuantitySold() { return quantitySold; }
    public LocalDate getSaleDate() { return saleDate; }
    public String getProductName() { return productName; }
    public void setProductName(String value) { productName=value; }
    public String getSoldBy() { return soldBy; }
    public void setSoldBy(String value) { soldBy=value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(double value) { price=BigDecimal.valueOf(value); }
    public void setPrice(BigDecimal value) { price=value; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit value) { unit=value; }
    public boolean isVoided() { return voided; }
    public void setVoided(boolean value) { voided=value; }
    public BigDecimal getTotal() { return price.multiply(quantitySold).setScale(2, RoundingMode.HALF_UP); }
}
