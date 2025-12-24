package components;

import java.time.LocalDate;

import gui.LocaleManager;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import product.Sale;

public class SaleTableView extends TableView<Sale> {

    public SaleTableView() {
        initializeColumns();
    }

    private void initializeColumns() {
    	TableColumn<Sale, Integer> saleIdCol = new TableColumn<>(LocaleManager.get("column.sale.id"));
        saleIdCol.setCellValueFactory(new PropertyValueFactory<>("saleId"));

        TableColumn<Sale, Integer> productIdCol = new TableColumn<>(LocaleManager.get("column.product.id"));
        productIdCol.setCellValueFactory(new PropertyValueFactory<>("productId"));

        TableColumn<Sale, String> productNameCol = new TableColumn<>(LocaleManager.get("column.product.name"));
        productNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<Sale, Integer> quantityCol = new TableColumn<>(LocaleManager.get("column.quantity.sold"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));

        TableColumn<Sale, LocalDate> dateCol = new TableColumn<>(LocaleManager.get("column.sale.date"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("saleDate"));

        TableColumn<Sale, String> soldByCol = new TableColumn<>(LocaleManager.get("column.sold.by"));
        soldByCol.setCellValueFactory(new PropertyValueFactory<>("soldBy"));

        getColumns().addAll(saleIdCol, productIdCol, productNameCol, quantityCol, dateCol, soldByCol);
    }
}