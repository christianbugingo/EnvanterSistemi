import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import product.*;
import services.*;
import database.*;

/** External verification agent only; never shipped in the app and never changes signed bundle contents. */
public class SignedOfflineSmoke {
    public static void premain(String ignored) throws Exception {
        Path home=Path.of(System.getenv("INVENTORY_SIGNED_TEST_HOME"));
        String db="jdbc:h2:file:"+home.resolve("shop/inventory")+";DATABASE_TO_LOWER=TRUE";
        System.setProperty("user.home",home.toString());
        System.setProperty("DB_URL",db);System.setProperty("DB_USER","sa");System.setProperty("DB_PASSWORD","");
        Files.createDirectories(home.resolve("shop"));
        DatabaseInitializer.initialize();LegalService.accept();AuthService auth=new AuthService();
        String password="A1"+UUID.randomUUID();
        String recovery=auth.createFirstAdmin("testowner",password);
        if(recovery.isBlank())throw new AssertionError("Recovery setup failed");
        auth.login("testowner",password);new ShopSettings().save("USD",BigDecimal.ZERO,"en");
        ProductService products=new ProductService();
        products.addProduct(new Product(0,"Signed-package apples","Fruit",BigDecimal.ZERO,new BigDecimal("49.99"),"Test farm",Unit.KG,new BigDecimal("0.100"),0));
        new BatchService().receive(1,BigDecimal.ONE,new BigDecimal("30.00"),java.time.LocalDate.now(),null);
        SaleService sales=new SaleService();
        Checkout checkout=sales.recordSale(sales.quoteProduct(1,AuthService.current().id(),new BigDecimal("0.230")),PaymentMethod.OTHER,UUID.randomUUID().toString());
        if(checkout.total().compareTo(new BigDecimal("11.50"))!=0)throw new AssertionError("Wrong weighted total");
        Path receipt=utils.ReceiptGenerator.generateAndSaveReceipt(checkout);
        if(!Files.readString(receipt).contains("230 g"))throw new AssertionError("Wrong receipt weight");
        var profit=new DashboardService().profit(ProfitPeriod.TODAY,java.time.LocalDate.now(),null,null,Currencies.get("USD"));
        if(profit.incomplete()||profit.grossProfit().compareTo(new BigDecimal("4.60"))!=0)throw new AssertionError("Wrong FIFO profit");
        Path backup=home.resolve("backup.zip");new BackupService().create(backup);
        org.h2.tools.Restore.execute(backup.toString(),home.resolve("restored").toString(),"inventory");
        System.setProperty("DB_URL","jdbc:h2:file:"+home.resolve("restored/inventory")+";DATABASE_TO_LOWER=TRUE");
        auth.login("testowner",password);
        if(products.loadProducts().getFirst().getQuantity().compareTo(new BigDecimal("0.770"))!=0||new BatchService().load(1).getFirst().remaining()!=770)throw new AssertionError("Restore failed");
        auth.logout();
        System.setProperty("DB_URL",db);
        Files.writeString(home.resolve("passed.txt"),"Signed native launcher/runtime: terms, admin/recovery setup, currency, login, stock receipt, 230 g sale, receipt, FIFO profit, backup and restore passed. No signed app files changed.");
    }
}
