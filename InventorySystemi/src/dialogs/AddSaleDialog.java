package dialogs;

import gui.*;
import product.*;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** A quantity-and-total sale, with no cash drawer or payment prerequisite. */
public class AddSaleDialog extends Dialog<List<Sale>> {
    private final String checkoutId=UUID.randomUUID().toString();
    public String getCheckoutId() {return checkoutId;}
    public AddSaleDialog(ObservableList<Product> products,int userId) {
        Ui.style(getDialogPane());setTitle(LocaleManager.get("dialog.add.sale"));setHeaderText(LocaleManager.get("sale.hint"));
        ObservableList<Sale> cart=FXCollections.observableArrayList();
        TableView<Sale> table=Ui.table();table.setId("saleCart");table.setItems(cart);table.setPrefHeight(260);
        table.getColumns().add(Ui.column("column.product.name",Sale::getProductName));
        table.getColumns().add(Ui.numberColumn("column.quantity",Sale::getQuantitySold,false));
        table.getColumns().add(Ui.column("column.unit",s->s.getUnit().label()));
        table.getColumns().add(Ui.numberColumn("column.price",Sale::getPrice,true));
        table.getColumns().add(Ui.numberColumn("column.total",Sale::getTotal,true));
        TextField search=new TextField();search.setPromptText(LocaleManager.get("barcode.search"));search.setId("barcodeSearch");
        ComboBox<Product> product=new ComboBox<>(products);product.setPrefWidth(400);product.setId("saleProduct");
        product.setPromptText(LocaleManager.get(products.isEmpty()?"sale.no.products":"msg.select.product"));
        product.setCellFactory(v->new ListCell<>() {
            @Override protected void updateItem(Product p,boolean empty){super.updateItem(p,empty);setText(empty||p==null?null:p.toString());}
        });
        product.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Product p,boolean empty){super.updateItem(p,empty);setText(empty||p==null?null:p.toString());}
        });
        search.textProperty().addListener((o,a,b)->product.setItems(products.filtered(p->p.getName().toLowerCase(Locale.ROOT).contains(b.toLowerCase(Locale.ROOT))||p.getBarcode().contains(b.trim()))));
        TextField quantity=new TextField();quantity.setId("saleQuantity");quantity.setDisable(true);
        Label quantityLabel=new Label(LocaleManager.get("label.quantity")),available=new Label(),hint=new Label();hint.setWrapText(true);hint.setMaxWidth(660);
        product.valueProperty().addListener((o,a,p)->{
            quantity.setDisable(p==null);
            if(p==null){available.setText("");hint.setText("");return;}
            boolean pieces=p.getUnit()==Unit.PIECE;
            quantityLabel.setText(LocaleManager.get(pieces?"sale.quantity.pieces":"sale.quantity.kg"));
            quantity.setText(pieces?"1":"");quantity.setPromptText(LocaleManager.get(pieces?"sale.pieces.example":"sale.kg.example"));
            available.setText(LocaleManager.get("sale.available",LocaleManager.quantity(p.getQuantity()),p.getUnit().label(),LocaleManager.money(p.getPrice())));
            hint.setText(LocaleManager.get(pieces?"sale.pieces.hint":"sale.kg.hint"));
        });
        Label total=new Label(LocaleManager.get("sale.cart.total",LocaleManager.money(BigDecimal.ZERO)));
        Button add=Ui.button("button.add.item",()->{
            Product selected=product.getValue();if(selected==null)throw services.AuthService.failure("msg.select.product");
            BigDecimal qty=selected.getUnit().validate(LocaleManager.decimal(quantity.getText()),false);
            int existing=-1;BigDecimal reserved=BigDecimal.ZERO;
            for(int i=0;i<cart.size();i++)if(cart.get(i).getProductId()==selected.getProductId()){existing=i;reserved=cart.get(i).getQuantitySold();}
            BigDecimal combined=qty.add(reserved);
            if(combined.compareTo(selected.getQuantity())>0)throw services.AuthService.failure("sale.stock.changed");
            Sale sale=new Sale(0,selected.getProductId(),userId,combined,LocalDate.now());sale.setProductName(selected.getName());sale.setPrice(selected.getPrice());sale.setUnit(selected.getUnit());
            if(existing<0)cart.add(sale);else cart.set(existing,sale);
            quantity.setText(selected.getUnit()==Unit.PIECE?"1":"");quantity.requestFocus();quantity.selectAll();
        });add.setId("addCartItem");
        quantity.setOnAction(e->{add.fire();e.consume();});
        search.setOnAction(e->{Ui.run(()->{
            Product match=products.stream().filter(p->!p.getBarcode().isEmpty()&&p.getBarcode().equals(search.getText().trim())).findFirst().orElseThrow(()->services.AuthService.failure("barcode.not.found"));
            product.setValue(match);quantity.setText(match.getUnit()==Unit.PIECE?"1":"");quantity.requestFocus();quantity.selectAll();
        });e.consume();});
        Button remove=Ui.button("button.remove.selected",()->cart.remove(table.getSelectionModel().getSelectedItem()));
        GridPane fields=new GridPane();fields.setHgap(12);fields.setVgap(12);
        fields.addRow(0,new Label(LocaleManager.get("label.search")),search);
        fields.addRow(1,new Label(LocaleManager.get("label.product")),product);
        fields.addRow(2,quantityLabel,quantity,add);fields.add(available,1,3,2,1);fields.add(hint,0,4,3,1);
        VBox box=new VBox(15,fields,new Label(LocaleManager.get("sale.cart")),table,new HBox(20,remove,total));box.setPadding(new Insets(20));
        getDialogPane().setContent(box);getDialogPane().setPrefWidth(880);
        ButtonType finish=new ButtonType(LocaleManager.get("button.finish.sale"),ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(finish,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        getDialogPane().lookupButton(finish).setId("completeSale");getDialogPane().lookupButton(finish).setDisable(true);
        cart.addListener((ListChangeListener<Sale>)change->{
            getDialogPane().lookupButton(finish).setDisable(cart.isEmpty());total.setText(LocaleManager.get("sale.cart.total",LocaleManager.money(cart.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add))));
        });
        setResultConverter(b->b==finish?new ArrayList<>(cart):null);
    }
}
