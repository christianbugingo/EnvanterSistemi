package dialogs;

import gui.LocaleManager;
import product.Product;
import product.Sale;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Callback;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AddSaleDialog extends Dialog<List<Sale>> {

    private final ObservableList<Product> productsData;
    private final int currentUserId;

    public AddSaleDialog(ObservableList<Product> productsData, int currentUserId) {
        this.productsData = productsData;
        this.currentUserId = currentUserId;
        initializeDialog();
    }

    private void initializeDialog() {
        setTitle(LocaleManager.get("dialog.add.sale"));
        setHeaderText(LocaleManager.get("dialog.add.sale.header"));

        ObservableList<Sale> saleItems = FXCollections.observableArrayList();
        TableView<Sale> saleItemsTable = createSaleItemsTable(saleItems);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField searchField = new TextField();
        searchField.setPromptText(LocaleManager.get("search.placeholder"));

        ComboBox<Product> productCombo = new ComboBox<>();
        productCombo.setItems(productsData);
        productCombo.setPrefWidth(300);
        productCombo.setCellFactory(lv -> new ListCell<Product>() {
            @Override protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getName() + " (Stok: " + item.getQuantity() + ", Fiyat: " + item.getPrice() + ")");
            }
        });

        TextField quantityField = new TextField();
        quantityField.setPromptText(LocaleManager.get("prompt.quantity"));

        Button addItemButton = new Button(LocaleManager.get("button.add.item"));
        addItemButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        Button removeItemButton = new Button(LocaleManager.get("button.remove.selected"));
        removeItemButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

        Label totalItemsLabel = new Label("Toplam Ürün: 0");
        Label totalAmountLabel = new Label("Toplam Tutar: ₺0,00");

        searchField.textProperty().addListener((obs, old, val) -> {
            String text = val == null ? "" : val.toLowerCase();
            if (text.isEmpty()) productCombo.setItems(productsData);
            else {
                ObservableList<Product> filtered = FXCollections.observableArrayList();
                for (Product p : productsData) {
                    if (p.getName().toLowerCase().contains(text)) filtered.add(p);
                }
                productCombo.setItems(filtered);
            }
            productCombo.show();
        });

        addItemButton.setOnAction(e -> {
            Product selectedProduct = productCombo.getValue();
            if (selectedProduct == null) {
                showError("alert.error", "msg.select.product");
                return;
            }

            try {
                int qty = Integer.parseInt(quantityField.getText());
                if (qty <= 0) {
                    showError("alert.error", "msg.invalid.quantity");
                    return;
                }

                int totalRequested = qty;
                for (Sale existing : saleItems) {
                    if (existing.getProductId() == selectedProduct.getProductId()) {
                        totalRequested += existing.getQuantitySold();
                    }
                }

                if (totalRequested > selectedProduct.getQuantity()) {
                    showError("alert.error", "msg.insufficient.stock", selectedProduct.getQuantity());
                    return;
                }

                Sale newSale = new Sale(0, selectedProduct.getProductId(), currentUserId, qty, LocalDate.now());
                newSale.setProductName(selectedProduct.getName());
                newSale.setPrice(selectedProduct.getPrice());
                saleItems.add(newSale);

                updateSaleSummary(saleItems, totalItemsLabel, totalAmountLabel);

                quantityField.clear();
                searchField.clear();
                productCombo.getSelectionModel().clearSelection();
            } catch (NumberFormatException ex) {
                showError("alert.error", "msg.invalid.quantity");
            }
        });

        removeItemButton.setOnAction(e -> {
            Sale selected = saleItemsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                saleItems.remove(selected);
                updateSaleSummary(saleItems, totalItemsLabel, totalAmountLabel);
            }
        });

        grid.add(new Label(LocaleManager.get("label.search") + ":"), 0, 0);
        grid.add(searchField, 1, 0);
        grid.add(new Label(LocaleManager.get("label.product") + ":"), 0, 1);
        grid.add(productCombo, 1, 1);
        grid.add(new Label(LocaleManager.get("label.quantity") + ":"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(addItemButton, 2, 2);

        grid.add(new Label("Satış Listesi:"), 0, 3, 3, 1);
        grid.add(saleItemsTable, 0, 4, 3, 1);

        grid.add(removeItemButton, 0, 5);
        grid.add(totalItemsLabel, 1, 5);
        grid.add(totalAmountLabel, 2, 5);

        VBox content = new VBox(10, grid);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);

        ButtonType finishSaleButton = new ButtonType("Satışı Tamamla", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("İptal", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(finishSaleButton, cancelButton);

        Node finishButton = getDialogPane().lookupButton(finishSaleButton);
        finishButton.setDisable(saleItems.isEmpty());

        saleItems.addListener((ListChangeListener<Sale>) change -> {
            finishButton.setDisable(saleItems.isEmpty());
        });

        setResultConverter(btn -> {
            if (btn == finishSaleButton) {
                return new ArrayList<>(saleItems);
            }
            return null;
        });
    }

    private TableView<Sale> createSaleItemsTable(ObservableList<Sale> saleItems) {
        TableView<Sale> table = new TableView<>(saleItems);
        table.setPrefHeight(200);

        TableColumn<Sale, String> productCol = new TableColumn<>("Ürün");
        productCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getProductName()));

        TableColumn<Sale, Integer> qtyCol = new TableColumn<>("Adet");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));

        TableColumn<Sale, Double> priceCol = new TableColumn<>("Birim Fiyat");
        priceCol.setCellValueFactory(cellData ->
            new SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());
        priceCol.setCellFactory(col -> new TableCell<Sale, Double>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("₺%.2f", item));
            }
        });

        TableColumn<Sale, Double> totalCol = new TableColumn<>("Toplam");
        totalCol.setCellValueFactory(cellData -> {
            double total = cellData.getValue().getPrice() * cellData.getValue().getQuantitySold();
            return new SimpleDoubleProperty(total).asObject();
        });
        totalCol.setCellFactory(col -> new TableCell<Sale, Double>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("₺%.2f", item));
            }
        });

        table.getColumns().addAll(productCol, qtyCol, priceCol, totalCol);
        return table;
    }

    private void updateSaleSummary(ObservableList<Sale> saleItems, Label totalItemsLabel, Label totalAmountLabel) {
        int totalItems = 0;
        double totalAmount = 0;

        for (Sale sale : saleItems) {
            totalItems += sale.getQuantitySold();
            totalAmount += sale.getPrice() * sale.getQuantitySold();
        }

        totalItemsLabel.setText("Toplam Ürün: " + totalItems);
        totalAmountLabel.setText(String.format("Toplam Tutar: ₺%.2f", totalAmount));
    }

    private void showError(String titleKey, String messageKey, Object... args) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(LocaleManager.get(titleKey));
        alert.setHeaderText(null);
        alert.setContentText(LocaleManager.get(messageKey, args));
        alert.showAndWait();
    }
}