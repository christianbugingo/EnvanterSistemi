package dialogs;
import gui.*;
import services.*;
import product.ShopCurrency;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import java.math.BigDecimal;
public final class ShopSettingsDialog {
    private ShopSettingsDialog() { }
    public static boolean show(boolean initial){
        var previous=ShopSettings.current();Dialog<Boolean> d=new Dialog<>();Ui.style(d.getDialogPane());d.setTitle(LocaleManager.get(initial?"currency.choose":"settings.title"));
        var all=javafx.collections.FXCollections.observableArrayList(Currencies.all().stream().filter(c->c.minorUnits()>=0).toList());
        ComboBox<ShopCurrency> currencies=new ComboBox<>(all);currencies.setEditable(true);currencies.setId("shopCurrency");currencies.setPrefWidth(560);
        currencies.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ShopCurrency c){return c==null?"":c.toString();}
            @Override public ShopCurrency fromString(String text){return all.stream().filter(c->c.code().equalsIgnoreCase(text.trim())||c.toString().equalsIgnoreCase(text.trim())).findFirst().orElse(null);}
        });
        currencies.setValue(previous.currency());currencies.getEditor().setId("currencySearch");currencies.getEditor().setPromptText(LocaleManager.get("currency.search"));
        boolean[] filtering={false};
        currencies.getEditor().textProperty().addListener((o,a,b)->{
            if(filtering[0]||(currencies.getValue()!=null&&b.equals(currencies.getValue().toString())))return;
            filtering[0]=true;try{
            var matches=all.filtered(c->c.toString().toLowerCase(java.util.Locale.ROOT).contains(b.toLowerCase(java.util.Locale.ROOT)));
            currencies.setItems(matches);currencies.getEditor().setText(b);currencies.getEditor().positionCaret(b.length());
            if(currencies.getEditor().isFocused()&&!matches.isEmpty()&&!currencies.isShowing())currencies.show();
            }finally{filtering[0]=false;}
        });
        TextField tax=new TextField(previous.taxRate().toPlainString());tax.setId("shopTaxRate");
        ComboBox<String> language=new ComboBox<>(javafx.collections.FXCollections.observableArrayList("en","tr"));language.setValue(previous.language());language.setId("shopLanguage");
        CheckBox staffProfit=new CheckBox(LocaleManager.get("settings.staff.profit"));staffProfit.setId("staffProfitVisible");staffProfit.setSelected(new ShopSettings().staffProfitVisible());
        Label taxHint=new Label(LocaleManager.get("tax.zero.hint"));taxHint.setId("taxHint");
        Label warning=new Label(LocaleManager.get("currency.warning"));warning.setWrapText(true);warning.setMaxWidth(560);
        VBox box=new VBox(12,new Label(LocaleManager.get("currency.choose")),currencies,new Label(LocaleManager.get("tax.rate")),tax,taxHint,new Label(LocaleManager.get("language.label")),language,staffProfit,warning);box.setPadding(new Insets(24));d.getDialogPane().setContent(box);
        ButtonType save=new ButtonType(LocaleManager.get("button.save"),ButtonBar.ButtonData.OK_DONE);d.getDialogPane().getButtonTypes().add(save);if(!initial)d.getDialogPane().getButtonTypes().add(new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        var button=d.getDialogPane().lookupButton(save);button.setId("saveShopSettings");button.addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{
            ShopCurrency selected=currencies.getConverter().fromString(currencies.getEditor().getText());if(selected==null)throw AuthService.failure("currency.required");BigDecimal rate;try{rate=ShopSettings.parseTax(tax.getText());}catch(RuntimeException ex){throw AuthService.failure("tax.invalid");}
            ShopSettings.validateTax(rate);if(previous.currency()!=null&&!previous.currency().code().equals(selected.code())&&!Ui.confirm(LocaleManager.get("currency.warning"))){e.consume();return;}
            new ShopSettings().save(selected.code(),rate,language.getValue());new ShopSettings().setStaffProfitVisible(staffProfit.isSelected());LocaleManager.setLanguage(language.getValue());
        }catch(RuntimeException ex){e.consume();Ui.error(ex.getMessage());}});
        if(initial)d.setOnCloseRequest(e->{if(ShopSettings.current().currency()==null)e.consume();});d.setResultConverter(b->b==save);return d.showAndWait().orElse(false);
    }
}
