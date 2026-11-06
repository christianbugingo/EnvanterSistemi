package components;
import gui.*;
import product.Sale;
import javafx.scene.control.*;
public class SaleTableView extends TableView<Sale> {
    public SaleTableView() {
        setPlaceholder(new Label(LocaleManager.get("table.empty")));
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getColumns().add(Ui.numberColumn("column.sale.id",s->java.math.BigDecimal.valueOf(s.getSaleId()),false));
        getColumns().add(Ui.column("checkout.id",Sale::getCheckoutId));
        getColumns().add(Ui.column("column.product.name",Sale::getProductName));
        getColumns().add(Ui.column("column.quantity.sold",s->product.Quantities.format(s.getQuantitySold(),s.getUnit())));
        getColumns().add(Ui.column("column.unit",s->s.getUnit().label()));
        getColumns().add(Ui.column("column.price",Sale::getPriceLabel));
        getColumns().add(Ui.column("column.total",Sale::getTotalLabel));
        getColumns().add(Ui.column("column.sale.date",Sale::getSaleDate));
        getColumns().add(Ui.column("column.sold.by",Sale::getSoldBy));
        getColumns().add(Ui.column("payment.method",Sale::getPaymentLabel));
        getColumns().add(Ui.column("column.status",s->LocaleManager.get(s.isVoided()?"sale.voided":"sale.completed")));
    }
}
