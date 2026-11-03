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
        DatabaseInitializer.initialize(); AuthService auth=new AuthService();auth.createFirstAdmin("admin","AdminTest123!");auth.login("admin","AdminTest123!");
        auth.createStaff("cashier","TempPass123!");
        new ProductService().addProduct(new Product(0,"Apples","Fruit",new BigDecimal("10"),new BigDecimal("40"),"Farm",Unit.KG,new BigDecimal("2"),0));
        Product milk=new Product(0,"Milk","Dairy",20,30,"Farm");milk.setBarcode("8691234567890");new ProductService().addProduct(milk);
        CompletableFuture<Void> done=new CompletableFuture<>();
        Platform.startup(()->{});
        Platform.runLater(()->{
            try {
                Platform.setImplicitExit(false);Stage stage=new Stage();
                for(String language:List.of("en","tr")) {
                    LocaleManager.setLanguage(language);
                    new Login().showLogin(stage);assertTrue(stage.isShowing());
                    assertFalse(stage.getIcons().isEmpty());assertFalse(stage.getIcons().getFirst().isError());
                    snapshot(stage.getScene().getRoot(),language+"-login");
                    ((TextField)stage.getScene().lookup("#loginUsername")).setText("admin");
                    ((PasswordField)stage.getScene().lookup("#loginPassword")).setText("AdminTest123!");
                    ((Button)stage.getScene().lookup("#signIn")).fire();
                    TabPane tabs=(TabPane)stage.getScene().lookup(".tab-pane");assertEquals(6,tabs.getTabs().size());
                    assertEquals(LocaleManager.get("tab.staff"),tabs.getTabs().get(4).getText());
                    stage.getScene().getRoot().applyCss(); assertTranslated(stage.getScene().getRoot());
                    snapshot(stage.getScene().getRoot(),language+"-admin");
                    AddProductDialog product=new AddProductDialog(null);product.show();product.getDialogPane().applyCss();assertTranslated(product.getDialogPane());product.close();
                    var rows=new ProductService().loadProducts();AddSaleDialog sale=new AddSaleDialog(rows,AuthService.current().id());sale.show();
                    sale.getDialogPane().applyCss();assertTranslated(sale.getDialogPane());
                    @SuppressWarnings("unchecked") ComboBox<Product> combo=(ComboBox<Product>)sale.getDialogPane().lookup("#saleProduct");combo.setValue(rows.getFirst());
                    ((TextField)sale.getDialogPane().lookup("#saleQuantity")).setText("1,250");
                    ((Button)sale.getDialogPane().lookup("#addCartItem")).fire();
                    @SuppressWarnings("unchecked") TableView<Sale> table=(TableView<Sale>)sale.getDialogPane().lookup(".table-view");assertEquals(1,table.getItems().size());
                    assertEquals(new BigDecimal("1.250"),table.getItems().getFirst().getQuantitySold());
                    TextField scan=(TextField)sale.getDialogPane().lookup("#barcodeSearch");scan.setText("8691234567890");scan.fireEvent(new javafx.event.ActionEvent());
                    assertEquals(2,table.getItems().size());assertEquals("Milk",table.getItems().get(1).getProductName());
                    ((TextField)sale.getDialogPane().lookup("#cashReceived")).setText("100");
                    assertEquals(new BigDecimal("100"),sale.getCashReceived());assertEquals(PaymentMethod.CASH,sale.getPaymentMethod());
                    snapshot(sale.getDialogPane(),language+"-sale-dialog");sale.close();
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
                assertEquals(3,((TabPane)stage.getScene().lookup(".tab-pane")).getTabs().size());
                snapshot(stage.getScene().getRoot(),"en-staff");
                menu(stage,LocaleManager.get("menu.logout")).fire();stage.close();done.complete(null);
            } catch(Throwable e) { done.completeExceptionally(e); }
        });
        try {done.get(45,TimeUnit.SECONDS);} finally {AuthService.clearSession();System.clearProperty("DB_URL");System.clearProperty("DB_USER");System.clearProperty("DB_PASSWORD");LocaleManager.setLanguage("en");}
    }
    private static void snapshot(Node root,String name) throws Exception {
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
