package dialogs;

import gui.*;
import product.*;
import components.ProductSearch;
import services.*;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.util.*;

public class AddSaleDialog extends Dialog<List<Sale>> {
    private final String checkoutId=UUID.randomUUID().toString();
    private final ComboBox<PaymentMethod> payment=new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
    public PaymentMethod getPaymentMethod(){return payment.getValue();}
    public String getCheckoutId(){return checkoutId;}
    public AddSaleDialog(ObservableList<Product> products,int userId){
        Ui.style(getDialogPane());setTitle(LocaleManager.get("dialog.add.sale"));setHeaderText(LocaleManager.get("sale.hint"));
        var settings=ShopSettings.current();var currency=ShopSettings.currency();var service=new SaleService();
        ObservableList<Sale> cart=FXCollections.observableArrayList();
        TableView<Sale> table=Ui.table();table.setId("saleCart");table.setItems(cart);table.setPrefHeight(220);
        table.getColumns().add(Ui.column("column.product.name",Sale::getProductName));table.getColumns().add(Ui.column("column.quantity",s->Quantities.format(s.getQuantitySold(),s.getUnit())));table.getColumns().add(Ui.column("column.price",Sale::getPriceLabel));table.getColumns().add(Ui.column("column.total",Sale::getTotalLabel));
        Product[] selected={null};ProductSearch[] search={null};
        TextField quantity=new TextField();quantity.setId("saleQuantity");quantity.setDisable(true);
        Label selectedName=new Label(),quantityLabel=new Label(LocaleManager.get("label.quantity")),available=new Label(),hint=new Label(),preview=new Label();preview.setId("weightPreview");hint.setWrapText(true);hint.setMaxWidth(700);
        java.util.function.Supplier<BigDecimal> reserved=()->selected[0]==null?BigDecimal.ZERO:cart.stream().filter(s->s.getProductId()==selected[0].getProductId()).map(Sale::getQuantitySold).reduce(BigDecimal.ZERO,BigDecimal::add);
        Button add=Ui.button("button.add.item",()->{
            Product product=selected[0];if(product==null)throw AuthService.failure("msg.select.product");
            var qty=product.getUnit().validate(Quantities.parse(quantity.getText(),product.getUnit()),false);var quotes=service.quoteProduct(product.getProductId(),userId,qty.add(reserved.get()));
            if(quotes.stream().anyMatch(s->!s.getCurrency().code().equals(currency.code())||s.getTaxRate().compareTo(settings.taxRate())!=0))throw AuthService.failure("settings.changed");
            cart.removeIf(s->s.getProductId()==product.getProductId());cart.addAll(quotes);quantity.clear();search[0].ready();
        });add.setId("addCartItem");
        search[0]=new ProductSearch(products,p->{
            selected[0]=p;quantity.setDisable(false);selectedName.setText(p.getName());quantityLabel.setText(LocaleManager.get(p.getUnit()==Unit.PIECE?"sale.quantity.pieces":"sale.quantity.kg"));
            quantity.setPromptText(LocaleManager.get(p.getUnit()==Unit.PIECE?"sale.pieces.example":"sale.kg.example"));available.setText(LocaleManager.get("sale.available",Quantities.format(p.getQuantity(),p.getUnit()),p.getUnit().label(),currency.format(p.getPrice())));hint.setText(LocaleManager.get(p.getUnit()==Unit.PIECE?"sale.pieces.hint":"sale.kg.hint"));
            if(p.getUnit()==Unit.PIECE){quantity.setText("1");add.fire();}else{quantity.clear();quantity.requestFocus();}
        });
        quantity.textProperty().addListener((o,a,b)->{
            Product p=selected[0];if(p==null)return;
            if(p.getUnit()==Unit.PIECE&&!b.matches("[0-9]*")){quantity.setText(a);return;}
            try{
                BigDecimal qty=p.getUnit().validate(Quantities.parse(b,p.getUnit()),false),already=reserved.get();
                BigDecimal full=service.quoteProduct(p.getProductId(),userId,already.add(qty)).stream().map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
                BigDecimal before=already.signum()==0?BigDecimal.ZERO:service.quoteProduct(p.getProductId(),userId,already).stream().map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
                preview.setText(Quantities.format(qty,p.getUnit())+" · "+currency.format(full.subtract(before)));
            }catch(RuntimeException ex){preview.setText(LocaleManager.get("quantity.enter"));}
        });quantity.setOnAction(e->{add.fire();e.consume();});
        Label total=new Label(LocaleManager.get("sale.cart.total",currency.format(BigDecimal.ZERO))),subtotal=new Label(),tax=new Label();subtotal.setVisible(settings.taxRate().signum()>0);subtotal.managedProperty().bind(subtotal.visibleProperty());tax.setVisible(settings.taxRate().signum()>0);tax.managedProperty().bind(tax.visibleProperty());
        payment.setValue(PaymentMethod.CASH);payment.setId("paymentMethod");
        GridPane fields=new GridPane();fields.setHgap(12);fields.setVgap(12);fields.addRow(0,new Label(LocaleManager.get("label.product")),search[0]);fields.add(selectedName,1,1);fields.addRow(2,quantityLabel,quantity,add);fields.add(available,1,3,2,1);fields.add(hint,0,4,3,1);fields.add(preview,1,5,2,1);
        VBox content=new VBox(15,fields,new Label(LocaleManager.get("sale.cart")),table,new HBox(16,Ui.button("button.remove.selected",()->cart.remove(table.getSelectionModel().getSelectedItem())),new VBox(8,subtotal,tax,total)),new HBox(12,new Label(LocaleManager.get("payment.method")),payment));content.setPadding(new Insets(20));getDialogPane().setContent(content);getDialogPane().setPrefWidth(900);
        ButtonType finish=new ButtonType(LocaleManager.get("button.finish.sale"),ButtonBar.ButtonData.OK_DONE);getDialogPane().getButtonTypes().addAll(finish,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));var finishButton=getDialogPane().lookupButton(finish);finishButton.setId("completeSale");finishButton.setDisable(true);
        cart.addListener((ListChangeListener<Sale>)change->{finishButton.setDisable(cart.isEmpty());var amounts=Amounts.calculate(cart.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add),settings.taxRate(),currency);subtotal.setText(LocaleManager.get("amount.subtotal",currency.format(amounts.subtotal())));tax.setText(LocaleManager.get("amount.tax",settings.taxRate().stripTrailingZeros().toPlainString(),currency.format(amounts.tax())));total.setText(LocaleManager.get("sale.cart.total",currency.format(amounts.total())));});
        setOnShown(e->search[0].ready());
        getDialogPane().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,e->{if(e.isShortcutDown()&&e.getCode()==javafx.scene.input.KeyCode.ENTER){((Button)finishButton).fire();e.consume();}});setResultConverter(b->b==finish?new ArrayList<>(cart):null);
    }
}
