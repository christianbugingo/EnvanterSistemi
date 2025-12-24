package product;

import java.time.LocalDate;

public class ReportEntry {
    private LocalDate date;
    private int totalQuantity;
    private double totalRevenue;

    public ReportEntry(LocalDate date, int totalQuantity, double totalRevenue) {
        this.date = date;
        this.totalQuantity = totalQuantity;
        this.totalRevenue = totalRevenue;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public int getTotalQuantity() { return totalQuantity; }
    public double getTotalRevenue() { return totalRevenue; }
}