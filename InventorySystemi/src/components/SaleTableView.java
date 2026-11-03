package components;
import gui.*;
import product.Sale;
import javafx.scene.control.*;
public class SaleTableView extends TableView<Sale> {
    public SaleTableView() {
        setPlaceholder(new Label(LocaleManager.get("table.empty")));
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getColumns().add(Ui.numberColumn("column.sale.id",s->java.math.BigDecimal.valueOf(s.getSaleId()),false));
        getColumns().add(Ui.column("column.product.name",Sale::getProductName));
        getColumns().add(Ui.numberColumn("column.quantity.sold",Sale::getQuantitySold,false));
        getColumns().add(Ui.column("column.unit",s->s.getUnit().label()));
        getColumns().add(Ui.numberColumn("column.price",Sale::getPrice,true));
        getColumns().add(Ui.numberColumn("column.total",Sale::getTotal,true));
        getColumns().add(Ui.column("column.sale.date",Sale::getSaleDate));
        getColumns().add(Ui.column("column.sold.by",Sale::getSoldBy));
        getColumns().add(Ui.column("column.status",s->LocaleManager.get(s.isVoided()?"sale.voided":"sale.completed")));
    }
}
