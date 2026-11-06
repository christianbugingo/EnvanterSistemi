package product;

import java.math.BigDecimal;

public class Product {
    private final int productId;
    private final String name, category, supplier;
    private final long quantity, lowStock;
    private final BigDecimal price;
    private final Unit unit;
    private int version;
    private String barcode="";
    public Product(int id, String name, String category, long quantity, long price, String supplier) {
        this(id, name, category, BigDecimal.valueOf(quantity), BigDecimal.valueOf(price), supplier, Unit.PIECE, BigDecimal.TEN, 0);
    }
    public Product(int id, String name, String category, BigDecimal quantity, BigDecimal price, String supplier, Unit unit, BigDecimal lowStock, int version) {
        this.productId=id; this.name=name; this.category=category; this.quantity=unit.toStored(unit.validate(quantity,true)); this.price=price;
        this.supplier=supplier; this.unit=unit; this.lowStock=unit.toStored(unit.validate(lowStock,true)); this.version=version;
    }
    public String getBarcode() { return barcode; }
    public void setBarcode(String value) { barcode=value==null?"":value.trim(); }
    public int getProductId() { return productId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getQuantity() { return unit.fromStored(quantity); }
    public BigDecimal getPrice() { return price; }
    public String getSupplier() { return supplier; }
    public Unit getUnit() { return unit; }
    public BigDecimal getLowStock() { return unit.fromStored(lowStock); }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version=version; }
    public boolean isLowStock() { return quantity < lowStock; }
    @Override public String toString() { return name + " · " + Quantities.format(getQuantity(),unit); }
}
