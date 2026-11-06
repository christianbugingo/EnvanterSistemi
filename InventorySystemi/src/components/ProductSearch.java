package components;
import gui.*;
import product.Product;
import javafx.collections.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.input.*;
import java.util.*;
import java.util.function.Consumer;

/** One text input with an inline, keyboard-accessible dropdown; filtering never rewrites typed text. */
public final class ProductSearch extends VBox {
    private final TextField input=new TextField();
    private final ListView<Product> results=new ListView<>();
    public ProductSearch(ObservableList<Product> products,Consumer<Product> select){
        setSpacing(0);input.setId("productSearch");input.setPromptText(LocaleManager.get("barcode.search"));results.setId("productSuggestions");results.getStyleClass().add("search-suggestions");results.setPrefHeight(150);results.setPlaceholder(new Label(LocaleManager.get("sale.no.products")));results.setVisible(false);results.managedProperty().bind(results.visibleProperty());
        Runnable choose=()->{Product p=results.getSelectionModel().getSelectedItem();if(p==null)throw services.AuthService.failure("msg.select.product");results.setVisible(false);select.accept(p);};
        input.textProperty().addListener((o,a,b)->{String key=b.trim().toLowerCase(Locale.ROOT);results.setItems(products.filtered(p->p.getName().toLowerCase(Locale.ROOT).contains(key)||p.getBarcode().toLowerCase(Locale.ROOT).contains(key)));results.setVisible(!key.isBlank());});
        input.setOnAction(e->{Ui.run(()->{String value=input.getText().trim();if(value.isBlank())return;Product exact=products.stream().filter(p->!p.getBarcode().isBlank()&&p.getBarcode().equalsIgnoreCase(value)).findFirst().orElse(null);if(exact!=null){results.setVisible(false);select.accept(exact);}else{if(results.getSelectionModel().getSelectedItem()==null&&!results.getItems().isEmpty())results.getSelectionModel().selectFirst();choose.run();}});e.consume();});
        input.setOnKeyPressed(e->{if(e.getCode()==KeyCode.DOWN&&!results.getItems().isEmpty()){results.setVisible(true);results.getSelectionModel().selectFirst();results.requestFocus();e.consume();}else if(e.getCode()==KeyCode.ESCAPE){results.setVisible(false);e.consume();}});
        results.setOnKeyPressed(e->{if(e.getCode()==KeyCode.ENTER){Ui.run(choose);e.consume();}else if(e.getCode()==KeyCode.ESCAPE){results.setVisible(false);input.requestFocus();e.consume();}});
        results.setOnMouseClicked(e->{if(e.getButton()==MouseButton.PRIMARY&&results.getSelectionModel().getSelectedItem()!=null)Ui.run(choose);});
        getChildren().addAll(input,results);
    }
    public TextField input(){return input;}
    public void ready(){input.clear();input.requestFocus();}
}
