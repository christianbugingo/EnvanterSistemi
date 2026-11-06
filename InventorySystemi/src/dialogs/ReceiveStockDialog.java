package dialogs;
import gui.*;
import product.*;
import services.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
public final class ReceiveStockDialog {
    private ReceiveStockDialog() { }
    public static boolean show(Product product){
        Dialog<Boolean> d=new Dialog<>();Ui.style(d.getDialogPane());d.setTitle(LocaleManager.get("batch.receive"));d.setHeaderText(product.getName());
        TextField quantity=new TextField(),paid=new TextField(),price=new TextField();quantity.setId("receiveQuantity");paid.setId("receivePaid");price.setId("receivePrice");DatePicker date=new DatePicker(LocalDate.now());date.setId("receiveDate");
        Label preview=new Label();Runnable calculate=()->{try{BigDecimal qty=product.getUnit().validate(Quantities.parse(quantity.getText(),product.getUnit()),false);BigDecimal cost=LocaleManager.decimal(paid.getText());preview.setText(LocaleManager.get("batch.unit.cost",ShopSettings.currency().format(cost.divide(qty,ShopSettings.currency().minorUnits(),java.math.RoundingMode.HALF_UP)),product.getUnit().label()));}catch(RuntimeException ex){preview.setText("");}};quantity.textProperty().addListener((o,a,b)->calculate.run());paid.textProperty().addListener((o,a,b)->calculate.run());
        GridPane grid=new GridPane();grid.setPadding(new Insets(24));grid.setHgap(14);grid.setVgap(14);grid.addRow(0,new Label(LocaleManager.get("label.quantity")),quantity);grid.addRow(1,new Label(LocaleManager.get("batch.paid")),paid);grid.addRow(2,new Label(LocaleManager.get("batch.date")),date);grid.addRow(3,new Label(LocaleManager.get("batch.price.optional")),price);grid.add(preview,0,4,2,1);
        Label note=new Label(LocaleManager.get("batch.price.hint"));note.setWrapText(true);note.setMaxWidth(520);grid.add(note,0,5,2,1);d.getDialogPane().setContent(grid);
        ButtonType save=new ButtonType(LocaleManager.get("button.save"),ButtonBar.ButtonData.OK_DONE);d.getDialogPane().getButtonTypes().addAll(save,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));d.getDialogPane().lookupButton(save).setId("saveReceiveStock");
        d.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{new BatchService().receive(product.getProductId(),Quantities.parse(quantity.getText(),product.getUnit()),LocaleManager.decimal(paid.getText()),date.getValue(),price.getText().isBlank()?null:LocaleManager.decimal(price.getText()));}catch(RuntimeException ex){e.consume();Ui.error(ex.getMessage());}});d.setResultConverter(b->b==save);return d.showAndWait().orElse(false);
    }
}
