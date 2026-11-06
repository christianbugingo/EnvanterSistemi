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
        TextField quantity=new TextField(existing==null?"0":existing.getQuantity().stripTrailingZeros().toPlainString()); quantity.setId("quantityField");
        quantity.setDisable(existing!=null);
        TextField barcode=new TextField(existing==null?"":existing.getBarcode()); barcode.setId("barcodeField");
        TextField price=new TextField(existing==null?"":existing.getPrice().stripTrailingZeros().toPlainString()); price.setId("priceField");
        TextField threshold=new TextField(existing==null?"10":existing.getLowStock().stripTrailingZeros().toPlainString()); threshold.setId("lowStockField");
        ComboBox<Unit> unit=new ComboBox<>(); unit.getItems().addAll(Unit.values()); unit.setValue(existing==null?Unit.PIECE:existing.getUnit()); unit.setId("unitField");
        if(existing!=null){category.setDisable(true);supplier.setDisable(true);unit.setDisable(true);}
        Label priceLabel=new Label(LocaleManager.get("product.price.unit",unit.getValue().label()));
        unit.valueProperty().addListener((o,a,b)->priceLabel.setText(LocaleManager.get("product.price.unit",b.label())));
        GridPane grid=new GridPane(); grid.setPadding(new Insets(20)); grid.setHgap(12); grid.setVgap(12);
        ToggleGroup soldBy=new ToggleGroup();RadioButton piece=new RadioButton(LocaleManager.get("unit.piece")),weight=new RadioButton(LocaleManager.get("unit.weight"));
        piece.setToggleGroup(soldBy);weight.setToggleGroup(soldBy);piece.setSelected(unit.getValue()==Unit.PIECE);weight.setSelected(unit.getValue()==Unit.KG);
        piece.setId("soldByPiece");weight.setId("soldByWeight");piece.setDisable(existing!=null);weight.setDisable(existing!=null);
        soldBy.selectedToggleProperty().addListener((o,a,b)->{unit.setValue(b==weight?Unit.KG:Unit.PIECE);if(existing==null){quantity.setText("0");threshold.setText("0");}});
        unit.valueProperty().addListener((o,a,b)->{piece.setSelected(b==Unit.PIECE);weight.setSelected(b==Unit.KG);});
        quantity.textProperty().addListener((o,a,b)->{if(unit.getValue()==Unit.PIECE&&!b.matches("[0-9]*"))quantity.setText(a);});
        Label normalized=new Label();quantity.textProperty().addListener((o,a,b)->{try{normalized.setText(Quantities.format(Quantities.parse(b,unit.getValue()),unit.getValue()));}catch(RuntimeException ex){normalized.setText(LocaleManager.get("quantity.enter"));}});
        grid.addRow(0,new Label(LocaleManager.get("product.sold.by")),new javafx.scene.layout.HBox(15,piece,weight));
        grid.addRow(1,new Label(LocaleManager.get("label.name")),name);
        grid.addRow(2,new Label(LocaleManager.get("label.category")),category);
        grid.add(normalized,1,9);
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
                BigDecimal qty=unit.getValue().validate(Quantities.parse(quantity.getText(),unit.getValue()),true);
                BigDecimal low=unit.getValue().validate(Quantities.parse(threshold.getText(),unit.getValue()),true);
                BigDecimal amount=LocaleManager.decimal(price.getText());
                if(amount.stripTrailingZeros().scale()>services.ShopSettings.currency().minorUnits() || amount.compareTo(new BigDecimal("9999999999.99"))>0) throw services.AuthService.failure("msg.invalid.price");
                if(!barcode.getText().isBlank() && !barcode.getText().trim().matches("[A-Za-z0-9._-]{1,64}")) throw services.AuthService.failure("barcode.invalid");
                var duplicate=new services.ProductService().findDuplicate(name.getText(),unit.getValue(),existing==null?0:existing.getProductId());
                if(duplicate!=null){e.consume();if(Ui.confirm(LocaleManager.get("product.duplicate"))){if(ReceiveStockDialog.show(duplicate))close();}else{name.requestFocus();name.selectAll();}return;}
                result[0]=new Product(existing==null?0:existing.getProductId(),name.getText().trim(),category.getText().trim(),qty,amount,supplier.getText().trim(),unit.getValue(),low,existing==null?0:existing.getVersion());
                result[0].setBarcode(barcode.getText());
            } catch(RuntimeException ex) { e.consume(); Ui.error(ex.getMessage()); }
        });
        setResultConverter(b->b==save?result[0]:null);
    }
}
