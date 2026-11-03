package product;

import java.time.LocalDate;
import java.math.BigDecimal;
public record ReportEntry(LocalDate date, BigDecimal totalQuantity, BigDecimal totalRevenue, Unit unit) {
    public LocalDate getDate() { return date; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public String getUnit() { return unit.label(); }
}
