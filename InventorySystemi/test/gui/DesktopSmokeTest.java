package gui;

import database.*;
import dialogs.*;
import services.*;
import product.*;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="inventory.guiTest",matches="true")
class DesktopSmokeTest {
    @Test void englishTurkishAdminStaffAndForcedPasswordChange() throws Exception {
        System.setProperty("DB_URL","jdbc:h2:mem:desktop;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        System.setProperty("DB_USER","sa");System.setProperty("DB_PASSWORD","");
        DatabaseInitializer.initialize(); AuthService auth=new AuthService();LegalService.accept();String recovery=auth.createFirstAdmin("admin","AdminTest123!");auth.login("admin","AdminTest123!");new ShopSettings().save("USD",BigDecimal.ZERO,"en");
        auth.createStaff("cashier","TempPass123!");
        new ProductService().addProduct(new Product(0,"Apples","Fruit",new BigDecimal("10"),new BigDecimal("40"),"Farm",Unit.KG,new BigDecimal("2"),0));
        Product milk=new Product(0,"Milk","Dairy",20,30,"Farm");milk.setBarcode("8691234567890");new ProductService().addProduct(milk);
        CompletableFuture<Void> done=new CompletableFuture<>();
        var previousHandler=Thread.getDefaultUncaughtExceptionHandler();Thread.setDefaultUncaughtExceptionHandler((thread,error)->done.completeExceptionally(error));
        Platform.startup(()->{});
        Platform.runLater(()->{
            try {
                Platform.setImplicitExit(false);Stage stage=new Stage();
                firstRun(stage,done);
                for(String language:List.of("en","tr")) {
                    LocaleManager.setLanguage(language);
                    new Login().showLogin(stage);assertTrue(stage.isShowing());
                    assertFalse(stage.getIcons().isEmpty());assertFalse(stage.getIcons().getFirst().isError());
                    snapshot(stage.getScene().getRoot(),language+"-login");
                    ((TextField)stage.getScene().lookup("#loginUsername")).setText("admin");
                    ((PasswordField)stage.getScene().lookup("#loginPassword")).setText("AdminTest123!");
                    ((Button)stage.getScene().lookup("#signIn")).fire();
                    TabPane tabs=(TabPane)stage.getScene().lookup(".tab-pane");assertEquals(5,tabs.getTabs().size());
                    assertEquals(LocaleManager.get("tab.staff"),tabs.getTabs().get(3).getText());
                    stage.getScene().getRoot().applyCss(); assertTranslated(stage.getScene().getRoot());
                    snapshot(stage.getScene().getRoot(),language+"-admin");
                    Platform.runLater(()->guard(done,()->{DialogPane about=dialog(stage);about.applyCss();assertTrue(about.lookupAll(".label").stream().anyMatch(n->n instanceof Label l&&l.getText().contains(LegalService.acceptedAt())&&l.getText().contains("COODIC LABS")));((Button)about.lookupButton(about.getButtonTypes().getFirst())).fire();}));
                    menu(stage,LocaleManager.get("about.title")).fire();
                    AddProductDialog product=new AddProductDialog(null);product.show();product.getDialogPane().applyCss();assertTranslated(product.getDialogPane());
                    ((RadioButton)product.getDialogPane().lookup("#soldByWeight")).fire();
                    ((TextField)product.getDialogPane().lookup("#nameField")).setText("Bulk rice");
                    ((TextField)product.getDialogPane().lookup("#categoryField")).setText("Food");
                    ((TextField)product.getDialogPane().lookup("#supplierField")).setText("Farm");
                    ((TextField)product.getDialogPane().lookup("#quantityField")).setText("230 g");
                    ((TextField)product.getDialogPane().lookup("#priceField")).setText("49.99");
                    ((Button)product.getDialogPane().lookupButton(product.getDialogPane().getButtonTypes().getFirst())).fire();
                    assertNotNull(product.getResult());assertEquals(Unit.KG,product.getResult().getUnit());
                    assertEquals(new BigDecimal("0.230"),product.getResult().getQuantity());new ProductService().addProduct(product.getResult());
                    var rows=new ProductService().loadProducts();AddSaleDialog sale=new AddSaleDialog(rows,AuthService.current().id());sale.show();
                    sale.getDialogPane().applyCss();assertTranslated(sale.getDialogPane());
                    @SuppressWarnings("unchecked") ComboBox<Product> combo=(ComboBox<Product>)sale.getDialogPane().lookup("#saleProduct");combo.setValue(rows.getFirst());
                    ((TextField)sale.getDialogPane().lookup("#saleQuantity")).setText("0.23 kg");
                    ((Button)sale.getDialogPane().lookup("#addCartItem")).fire();
                    @SuppressWarnings("unchecked") TableView<Sale> table=(TableView<Sale>)sale.getDialogPane().lookup(".table-view");assertEquals(1,table.getItems().size());
                    assertEquals(new BigDecimal("0.230"),table.getItems().getFirst().getQuantitySold());
                    TextField scan=(TextField)sale.getDialogPane().lookup("#barcodeSearch");scan.setText("8691234567890");scan.fireEvent(new javafx.event.ActionEvent());
                    assertEquals(2,table.getItems().size());assertEquals("Milk",table.getItems().get(1).getProductName());
                    combo.setValue(rows.get(1));((TextField)sale.getDialogPane().lookup("#saleQuantity")).setText("4");((Button)sale.getDialogPane().lookup("#addCartItem")).fire();
                    assertEquals(2,table.getItems().size());assertEquals(new BigDecimal("5.000"),table.getItems().get(1).getQuantitySold());
                    assertNull(sale.getDialogPane().lookup("#cashReceived"));assertEquals(PaymentMethod.CASH,sale.getPaymentMethod());
                    snapshot(sale.getDialogPane(),language+"-sale-dialog");
                    ((Button)sale.getDialogPane().lookup("#completeSale")).fire();
                    assertNotNull(sale.getResult());new SaleService().recordSale(sale.getResult(),sale.getPaymentMethod(),sale.getCheckoutId());
                    sale.close();
                    // Logout through the menu so its refresh timer is stopped.
                    menu(stage,LocaleManager.get("menu.logout")).fire();
                }
                LocaleManager.setLanguage("en");new Login().showLogin(stage);
                ((TextField)stage.getScene().lookup("#loginUsername")).setText("cashier");
                ((PasswordField)stage.getScene().lookup("#loginPassword")).setText("TempPass123!");
                Platform.runLater(()->{
                    try {
                        DialogPane pane=(DialogPane)Window.getWindows().stream().filter(w->w!=stage&&w.isShowing()).findFirst().orElseThrow().getScene().getRoot();
                        ((PasswordField)pane.lookup("#currentPassword")).setText("TempPass123!");
                        ((PasswordField)pane.lookup("#newPassword")).setText("PrivatePass123!");
                        ((PasswordField)pane.lookup("#confirmPassword")).setText("PrivatePass123!");
                        ((Button)pane.lookup("#savePassword")).fire();
                    } catch(Throwable e) {done.completeExceptionally(e);List.copyOf(Window.getWindows()).stream().filter(w->w!=stage).forEach(Window::hide);}
                });
                ((Button)stage.getScene().lookup("#signIn")).fire();
                assertFalse(AuthService.current().mustChangePassword());
                assertEquals(2,((TabPane)stage.getScene().lookup(".tab-pane")).getTabs().size());
                snapshot(stage.getScene().getRoot(),"en-staff");
                menu(stage,LocaleManager.get("menu.logout")).fire();
                // Login-screen password change without opening the dashboard.
                Platform.runLater(()->guard(done,()->{
                    DialogPane pane=dialog(stage);((TextField)pane.lookup("#changeUsername")).setText("admin");
                    ((PasswordField)pane.lookup("#currentPassword")).setText("AdminTest123!");
                    ((PasswordField)pane.lookup("#newPassword")).setText("ChangedAdmin123!");
                    ((PasswordField)pane.lookup("#confirmPassword")).setText("ChangedAdmin123!");((Button)pane.lookup("#savePassword")).fire();
                }));
                ((Button)stage.getScene().lookup("#loginChangePassword")).fire();assertNull(AuthService.current());
                // Staff receives instructions; admin uses a recovery code and must save its replacement.
                Platform.runLater(()->guard(done,()->{
                    DialogPane pane=dialog(stage);((TextField)pane.lookup("#recoveryUsername")).setText("cashier");
                    ((Button)pane.lookup("#recoverAccount")).fire();assertTrue(pane.getContent().lookupAll(".label").stream().anyMatch(n->n instanceof Label l&&LocaleManager.get("recovery.staff").equals(l.getText())));
                    ((TextField)pane.lookup("#recoveryUsername")).setText("admin");((PasswordField)pane.lookup("#recoveryInput")).setText(recovery);
                    ((PasswordField)pane.lookup("#recoveryTemporary")).setText("RecoveryTemp123!");((PasswordField)pane.lookup("#recoveryConfirm")).setText("RecoveryTemp123!");
                    Platform.runLater(()->guard(done,()->{
                        DialogPane saved=dialog(stage);CheckBox check=(CheckBox)saved.lookup("#recoverySaved");
                        Button finish=(Button)saved.lookupButton(saved.getButtonTypes().getFirst());assertTrue(finish.isDisabled());check.setSelected(true);assertFalse(finish.isDisabled());finish.fire();
                    }));((Button)pane.lookup("#recoverAccount")).fire();
                }));
                ((Button)stage.getScene().lookup("#forgotPassword")).fire();
                assertTrue(auth.login("admin","RecoveryTemp123!").mustChangePassword());auth.changePassword("RecoveryTemp123!","RecoveredPrivate123!");auth.logout();
                stage.close();done.complete(null);
            } catch(Throwable e) { done.completeExceptionally(e); }
        });
        try {done.get(90,TimeUnit.SECONDS);} finally {Thread.setDefaultUncaughtExceptionHandler(previousHandler);AuthService.clearSession();System.clearProperty("DB_URL");System.clearProperty("DB_USER");System.clearProperty("DB_PASSWORD");LocaleManager.setLanguage("en");}
    }
    private static void firstRun(Stage stage,CompletableFuture<Void> done) throws Exception {
        String previous=System.getProperty("DB_URL");System.setProperty("DB_URL","jdbc:h2:mem:first_run;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");DatabaseInitializer.initialize();
        new Welcome().show(stage);stage.getScene().getRoot().applyCss();assertFalse(stage.getScene().getRoot().lookupAll(".label").stream().anyMatch(n->n instanceof Label l&&(l.getText().contains("@")||l.getText().contains("coodic.org"))));snapshot(stage.getScene().getRoot(),"welcome");
        Platform.runLater(()->guard(done,()->{DialogPane terms=dialog(stage);assertTrue(terms.lookup("#acceptTerms").isDisabled());((Button)terms.lookupButton(terms.getButtonTypes().get(1))).fire();}));
        ((Button)stage.getScene().lookup("#homeStart")).fire();assertFalse(LegalService.accepted());assertFalse(new AuthService().hasUsers());
        Platform.runLater(()->guard(done,()->{
            DialogPane terms=dialog(stage);((CheckBox)terms.lookup("#termsAgree")).setSelected(true);
            Platform.runLater(()->guard(done,()->{
                DialogPane setup=dialog(stage);((TextField)setup.lookup("#setupUsername")).setText("firstowner");((PasswordField)setup.lookup("#setupPassword")).setText("FirstOwnerPassword123!");((PasswordField)setup.lookup("#setupConfirm")).setText("FirstOwnerPassword123!");
                Platform.runLater(()->guard(done,()->{
                    DialogPane code=dialog(stage);assertTrue(code.lookupButton(code.getButtonTypes().getFirst()).isDisabled());assertFalse(((TextArea)code.lookup("#recoveryCode")).getText().isBlank());((CheckBox)code.lookup("#recoverySaved")).setSelected(true);
                    Platform.runLater(()->guard(done,()->{
                        DialogPane settings=dialog(stage);@SuppressWarnings("unchecked") ComboBox<ShopCurrency> currencies=(ComboBox<ShopCurrency>)settings.lookup("#shopCurrency");assertNull(currencies.getValue());
                        ((TextField)settings.lookup("#currencySearch")).setText("RWF");assertEquals(1,currencies.getItems().size());assertEquals("RWF",currencies.getItems().getFirst().code());
                        Platform.runLater(()->guard(done,()->{DialogPane information=dialog(stage);((Button)information.lookupButton(information.getButtonTypes().getFirst())).fire();}));
                        ((Button)settings.lookup("#saveShopSettings")).fire();
                    }));((Button)code.lookupButton(code.getButtonTypes().getFirst())).fire();
                }));((Button)setup.lookup("#createAdministrator")).fire();
            }));((Button)terms.lookup("#acceptTerms")).fire();
        }));
        ((Button)stage.getScene().lookup("#homeStart")).fire();assertTrue(LegalService.accepted());assertEquals("RWF",ShopSettings.currency().code());assertFalse(LegalService.acceptedAt().isBlank());
        AuthService owner=new AuthService();owner.login("firstowner","FirstOwnerPassword123!");
        Product weight=new Product(0,"Test weight","Food",new BigDecimal("2"),new BigDecimal("100"),"Farm",Unit.KG,BigDecimal.ZERO,0);weight.setBarcode("WEIGHT100");new ProductService().addProduct(weight);
        AddSaleDialog weighted=new AddSaleDialog(new ProductService().loadProducts(),AuthService.current().id());weighted.show();
        TextField barcode=(TextField)weighted.getDialogPane().lookup("#barcodeSearch");barcode.setText("WEIGHT100");barcode.fireEvent(new javafx.event.ActionEvent());
        TextField quantity=(TextField)weighted.getDialogPane().lookup("#saleQuantity");quantity.setText("0.2 kg");assertEquals("200 g · 20 RWF",((Label)weighted.getDialogPane().lookup("#weightPreview")).getText());
        quantity.setText("0.23 kg");assertEquals("230 g · 23 RWF",((Label)weighted.getDialogPane().lookup("#weightPreview")).getText());quantity.setText("230 g");assertEquals("230 g · 23 RWF",((Label)weighted.getDialogPane().lookup("#weightPreview")).getText());
        quantity.setText("5 g");assertEquals("5 g · 1 RWF",((Label)weighted.getDialogPane().lookup("#weightPreview")).getText());weighted.close();owner.logout();
        var legal=LegalText.render(LegalService.text("TERMS.md"));String text=((javafx.scene.layout.VBox)legal.getContent()).getChildren().stream().filter(n->n instanceof javafx.scene.text.TextFlow).map(n->((javafx.scene.text.TextFlow)n).getChildren().stream().map(t->((javafx.scene.text.Text)t).getText()).collect(java.util.stream.Collectors.joining())).collect(java.util.stream.Collectors.joining("\n"));assertFalse(text.contains("##"));assertFalse(text.contains("**"));
        System.setProperty("DB_URL",previous);AuthService.clearSession();
    }
    private static DialogPane dialog(Stage owner) {
        return (DialogPane)Window.getWindows().stream().filter(w->w!=owner&&w.isShowing()&&w.getScene().getRoot() instanceof DialogPane).reduce((a,b)->b).orElseThrow().getScene().getRoot();
    }
    private static void guard(CompletableFuture<Void> done,Runnable work) {
        try{work.run();}catch(Throwable ex){done.completeExceptionally(ex);List.copyOf(Window.getWindows()).forEach(Window::hide);}
    }
    private static void snapshot(Node root,String name) throws Exception {
        if(root instanceof Parent parent){parent.applyCss();parent.layout();}
        var image=root.snapshot(null,null);var pixels=image.getPixelReader();
        var file=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<file.getHeight();y++)for(int x=0;x<file.getWidth();x++)file.setRGB(x,y,pixels.getArgb(x,y));
        var directory=java.nio.file.Path.of("target","ui-previews");java.nio.file.Files.createDirectories(directory);
        javax.imageio.ImageIO.write(file,"png",directory.resolve(name+".png").toFile());
    }
    private static MenuItem menu(Stage stage,String text) {
        MenuBar bar=(MenuBar)stage.getScene().lookup(".menu-bar");
        return bar.getMenus().stream().flatMap(m->m.getItems().stream()).filter(i->text.equals(i.getText())).findFirst().orElseThrow();
    }
    private static void assertTranslated(Node node) {
        if(node instanceof Labeled label && label.getText()!=null) {
            assertFalse(label.getText().matches("(button|report|label|column|msg)\\.[a-z.]+"),label.getText());
            if(LocaleManager.getLanguage().equals("en")) for(String turkish:List.of("Satış Listesi","Toplam Ürün","Satışı Tamamla","İptal","Birim Fiyat")) assertFalse(label.getText().contains(turkish),label.getText());
        }
        if(node instanceof Parent parent) for(Node child:parent.getChildrenUnmodifiable()) assertTranslated(child);
    }
}
