package components;

import java.time.LocalDate;

import gui.LocaleManager;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import product.ReportEntry;

public class ReportTableView extends TableView<ReportEntry> {

    public ReportTableView() {
        initializeColumns();
    }

    private void initializeColumns() {
    	TableColumn<ReportEntry, LocalDate> dateCol = new TableColumn<>(LocaleManager.get("column.date"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<ReportEntry, Integer> quantityCol = new TableColumn<>(LocaleManager.get("column.total.quantity"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));

        TableColumn<ReportEntry, Double> revenueCol = new TableColumn<>(LocaleManager.get("column.total.revenue"));
        revenueCol.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));
        revenueCol.setCellFactory(col -> new TableCell<ReportEntry, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
            }
        });

        getColumns().addAll(dateCol, quantityCol, revenueCol);
    }
}