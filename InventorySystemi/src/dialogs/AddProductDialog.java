package dialogs;

import gui.*;
import product.*;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import java.math.BigDecimal;

public class AddProductDialog extends Dialog<Product> {
    public AddProductDialog(Product existing) {
        Ui.style(getDialogPane());
        setTitle(LocaleManager.get(existing==null?"dialog.add.product":"dialog.update.product"));
        setHeaderText(LocaleManager.get("product.form.hint"));
        TextField name=new TextField(existing==null?"":existing.getName()); name.setId("nameField");
        TextField category=new TextField(existing==null?"":existing.getCategory()); category.setId("categoryField");
        TextField supplier=new TextField(existing==null?"":existing.getSupplier()); supplier.setId("supplierField");
        TextField quantity=new TextField(existing==null?"0":existing.getQuantity().toPlainString()); quantity.setId("quantityField");
        quantity.setDisable(existing!=null);
        TextField barcode=new TextField(existing==null?"":existing.getBarcode()); barcode.setId("barcodeField");
        TextField price=new TextField(existing==null?"":existing.getPrice().toPlainString()); price.setId("priceField");
        TextField threshold=new TextField(existing==null?"10":existing.getLowStock().toPlainString()); threshold.setId("lowStockField");
        ComboBox<Unit> unit=new ComboBox<>(); unit.getItems().addAll(Unit.values()); unit.setValue(existing==null?Unit.PIECE:existing.getUnit()); unit.setId("unitField");
        Label priceLabel=new Label(LocaleManager.get("product.price.unit",unit.getValue().label()));
        unit.valueProperty().addListener((o,a,b)->priceLabel.setText(LocaleManager.get("product.price.unit",b.label())));
        GridPane grid=new GridPane(); grid.setPadding(new Insets(20)); grid.setHgap(12); grid.setVgap(12);
        grid.addRow(0,new Label(LocaleManager.get("label.name")),name);
        grid.addRow(1,new Label(LocaleManager.get("label.category")),category);
        grid.addRow(2,new Label(LocaleManager.get("column.unit")),unit);
        grid.addRow(3,new Label(LocaleManager.get("label.quantity")),quantity);
        grid.addRow(4,priceLabel,price);
        grid.addRow(5,new Label(LocaleManager.get("product.threshold")),threshold);
        grid.addRow(6,new Label(LocaleManager.get("label.supplier")),supplier);
        grid.addRow(7,new Label(LocaleManager.get("column.barcode")),barcode);
        getDialogPane().setContent(grid);
        ButtonType save=new ButtonType(LocaleManager.get("button.save"),ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(save,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        final Product[] result={null};
        getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION,e->{
            try {
                for(TextField field:new TextField[]{name,category,supplier}) if(field.getText().isBlank() || field.getText().trim().length()>255) throw services.AuthService.failure("validation.product");
                BigDecimal qty=unit.getValue().validate(LocaleManager.decimal(quantity.getText()),true);
                BigDecimal low=unit.getValue().validate(LocaleManager.decimal(threshold.getText()),true);
                BigDecimal amount=LocaleManager.decimal(price.getText());
                if(amount.stripTrailingZeros().scale()>2 || amount.compareTo(new BigDecimal("9999999999.99"))>0) throw services.AuthService.failure("msg.invalid.price");
                if(!barcode.getText().isBlank() && !barcode.getText().trim().matches("[A-Za-z0-9._-]{1,64}")) throw services.AuthService.failure("barcode.invalid");
                result[0]=new Product(existing==null?0:existing.getProductId(),name.getText().trim(),category.getText().trim(),qty,amount,supplier.getText().trim(),unit.getValue(),low,existing==null?0:existing.getVersion());
                result[0].setBarcode(barcode.getText());
            } catch(RuntimeException ex) { e.consume(); Ui.error(ex.getMessage()); }
        });
        setResultConverter(b->b==save?result[0]:null);
    }
}
