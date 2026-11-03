package product;

import java.math.BigDecimal;

public class Product {
    private final int productId;
    private final String name, category, supplier;
    private final BigDecimal quantity, price, lowStock;
    private final Unit unit;
    private int version;
    public Product(int id, String name, String category, double quantity, double price, String supplier) {
        this(id, name, category, BigDecimal.valueOf(quantity), BigDecimal.valueOf(price), supplier, Unit.PIECE, BigDecimal.TEN, 0);
    }
    public Product(int id, String name, String category, BigDecimal quantity, BigDecimal price, String supplier, Unit unit, BigDecimal lowStock, int version) {
        this.productId=id; this.name=name; this.category=category; this.quantity=quantity; this.price=price;
        this.supplier=supplier; this.unit=unit; this.lowStock=lowStock; this.version=version;
    }
    public int getProductId() { return productId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public String getSupplier() { return supplier; }
    public Unit getUnit() { return unit; }
    public BigDecimal getLowStock() { return lowStock; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version=version; }
    public boolean isLowStock() { return quantity.compareTo(lowStock) < 0; }
    @Override public String toString() { return name + " · " + quantity.stripTrailingZeros().toPlainString() + " " + unit.label(); }
}
