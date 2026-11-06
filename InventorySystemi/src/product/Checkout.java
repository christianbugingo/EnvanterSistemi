package product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record Checkout(String id,String username,PaymentMethod method,BigDecimal total,LocalDateTime createdAt,List<Sale> lines,ShopCurrency currency,BigDecimal taxRate,BigDecimal subtotal,BigDecimal taxAmount) { }
