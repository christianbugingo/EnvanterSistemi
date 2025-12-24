package product;

import java.time.LocalDate;

public class Sale {
    private int saleId;
    private int productId;
    private int userId;
    private int quantitySold;
    private LocalDate saleDate;
    private String productName;
    private String soldBy;
    private double price;

    public Sale(int saleId, int productId, int userId, int quantitySold, LocalDate saleDate) {
        this.saleId = saleId;
        this.productId = productId;
        this.userId = userId;
        this.quantitySold = quantitySold;
        this.saleDate = saleDate;
    }

    // Getters and Setters
    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getQuantitySold() { return quantitySold; }
    public void setQuantitySold(int quantitySold) { this.quantitySold = quantitySold; }

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSoldBy() { return soldBy; }
    public void setSoldBy(String soldBy) { this.soldBy = soldBy; }
    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
    
}