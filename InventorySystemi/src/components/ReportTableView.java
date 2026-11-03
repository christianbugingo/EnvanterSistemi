package components;
import gui.*;
import product.ReportEntry;
import javafx.scene.control.*;
public class ReportTableView extends TableView<ReportEntry> {
    public ReportTableView() {
        setPlaceholder(new Label(LocaleManager.get("table.empty")));
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getColumns().add(Ui.column("column.date",ReportEntry::getDate));
        getColumns().add(Ui.column("column.unit",ReportEntry::getUnit));
        getColumns().add(Ui.numberColumn("column.total.quantity",ReportEntry::getTotalQuantity,false));
        getColumns().add(Ui.numberColumn("column.total.revenue",ReportEntry::getTotalRevenue,true));
    }
}
