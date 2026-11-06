package components;
import gui.*;
import product.ReportEntry;
import javafx.scene.control.*;
public class ReportTableView extends TableView<ReportEntry> {
    public ReportTableView() {
        setPlaceholder(new Label(LocaleManager.get("table.empty")));
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getColumns().add(Ui.column("payment.method",ReportEntry::getPaymentLabel));
        getColumns().add(Ui.column("column.date",ReportEntry::getDate));
        getColumns().add(Ui.column("column.unit",ReportEntry::getUnit));
        getColumns().add(Ui.column("column.total.quantity",ReportEntry::getQuantityLabel));
        getColumns().add(Ui.column("column.subtotal",ReportEntry::getSubtotalLabel));
        getColumns().add(Ui.column("column.tax",ReportEntry::getTaxLabel));
        getColumns().add(Ui.column("column.total.revenue",ReportEntry::getTotalLabel));
    }
}
