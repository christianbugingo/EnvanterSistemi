package components;

import gui.LocaleManager;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import product.Product;

public class ProductTableView extends TableView<Product> {

    public ProductTableView() {
    	super();
        initializeColumns();
        
    }

    private void initializeColumns() {
    	TableColumn<Product, Integer> idCol = new TableColumn<>(LocaleManager.get("column.id"));
        idCol.setCellValueFactory(new PropertyValueFactory<>("productId"));

        TableColumn<Product, String> nameCol = new TableColumn<>(LocaleManager.get("column.name"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, String> categoryCol = new TableColumn<>(LocaleManager.get("column.category"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Product, Integer> quantityCol = new TableColumn<>(LocaleManager.get("column.quantity"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setCellFactory(col -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    if (item < 5) setStyle("-fx-background-color: #ffcccc; -fx-font-weight: bold;");
                    else if (item < 10) setStyle("-fx-background-color: #fff0cc;");
                    else setStyle("");
                }
            }
        });

        TableColumn<Product, Double> priceCol = new TableColumn<>(LocaleManager.get("column.price"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setCellFactory(col -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
            }
        });

        TableColumn<Product, String> supplierCol = new TableColumn<>(LocaleManager.get("column.supplier"));
        supplierCol.setCellValueFactory(new PropertyValueFactory<>("supplier"));

        getColumns().addAll(idCol, nameCol, categoryCol, quantityCol, priceCol, supplierCol);
    }
}