package product;
import java.math.BigDecimal;
import java.time.LocalDate;
public record StockBatch(long id,int productId,Unit unit,long received,long remaining,BigDecimal totalCost,BigDecimal remainingCost,BigDecimal costPerUnit,LocalDate receivedDate,ShopCurrency currency,BigDecimal sellingPrice) {
    public String quantityLabel(){return Quantities.format(unit.fromStored(remaining),unit);}
    public String costLabel(){return totalCost==null?gui.LocaleManager.get("cost.unknown"):currency.format(totalCost);}
}
