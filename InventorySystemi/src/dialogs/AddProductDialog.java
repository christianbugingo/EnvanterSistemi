package dialogs;

import gui.LocaleManager;
import product.Product;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

public class AddProductDialog extends Dialog<Product> {

    private final Product existingProduct;

    public AddProductDialog(Product existing) {
        this.existingProduct = existing;
        initializeDialog();
    }

    private void initializeDialog() {
        setTitle(existingProduct == null ? 
                 LocaleManager.get("dialog.add.product") : 
                 LocaleManager.get("dialog.update.product"));

        setHeaderText(existingProduct == null ? 
                      LocaleManager.get("dialog.add.product.header") : 
                      LocaleManager.get("dialog.update.product.update.header"));

        GridPane grid = createProductForm(existingProduct);
        getDialogPane().setContent(grid);

        ButtonType actionButton = new ButtonType(
            existingProduct == null ? LocaleManager.get("button.add") : LocaleManager.get("button.update"),
            ButtonBar.ButtonData.OK_DONE
        );
        getDialogPane().getButtonTypes().addAll(actionButton, ButtonType.CANCEL);

        setResultConverter(new Callback<ButtonType, Product>() {
            @Override
            public Product call(ButtonType btn) {
                if (btn == actionButton) {
                    try {
                        TextField name = (TextField) grid.lookup("#nameField");
                        TextField category = (TextField) grid.lookup("#categoryField");
                        TextField quantity = (TextField) grid.lookup("#quantityField");
                        TextField price = (TextField) grid.lookup("#priceField");
                        TextField supplier = (TextField) grid.lookup("#supplierField");

                        return validateAndCreateProduct(
                            name.getText(), category.getText(), quantity.getText(),
                            price.getText(), supplier.getText(),
                            existingProduct == null ? null : existingProduct.getProductId()
                        );
                    } catch (IllegalArgumentException ex) {
                        // Show error inside dialog
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(LocaleManager.get("alert.error"));
                        alert.setHeaderText(null);
                        alert.setContentText(ex.getMessage());
                        alert.showAndWait();
                        return null; // Keep dialog open
                    }
                }
                return null;
            }
        });
    }

    private GridPane createProductForm(Product existing) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        nameField.setId("nameField");
        TextField catField = new TextField(existing != null ? existing.getCategory() : "");
        catField.setId("categoryField");
        TextField qtyField = new TextField(existing != null ? String.valueOf(existing.getQuantity()) : "");
        qtyField.setId("quantityField");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "");
        priceField.setId("priceField");
        TextField suppField = new TextField(existing != null ? existing.getSupplier() : "");
        suppField.setId("supplierField");

        grid.add(new Label(LocaleManager.get("label.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(LocaleManager.get("label.category") + ":"), 0, 1);
        grid.add(catField, 1, 1);
        grid.add(new Label(LocaleManager.get("label.quantity") + ":"), 0, 2);
        grid.add(qtyField, 1, 2);
        grid.add(new Label(LocaleManager.get("label.price") + ":"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label(LocaleManager.get("label.supplier") + ":"), 0, 4);
        grid.add(suppField, 1, 4);

        return grid;
    }

    private Product validateAndCreateProduct(String name, String category, String qtyStr,
                                            String priceStr, String supplier, Integer id) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException(LocaleManager.get("msg.name.required"));
        if (category == null || category.trim().isEmpty()) throw new IllegalArgumentException(LocaleManager.get("msg.category.required"));
        if (supplier == null || supplier.trim().isEmpty()) throw new IllegalArgumentException(LocaleManager.get("msg.supplier.required"));

        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
            if (qty < 0) throw new IllegalArgumentException(LocaleManager.get("msg.negative.quantity"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(LocaleManager.get("msg.invalid.quantity"));
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) throw new IllegalArgumentException(LocaleManager.get("msg.negative.price"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(LocaleManager.get("msg.invalid.price"));
        }

        return new Product(id == null ? 0 : id, name.trim(), category.trim(), qty, price, supplier.trim());
    }
}