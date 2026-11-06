package services;

import database.*;
import services.LegalService;
import product.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OfflineWorkflowTest {
    AuthService auth;ProductService products;SaleService sales;
    @BeforeEach void setup() throws Exception {
        System.setProperty("DB_URL","jdbc:h2:mem:offline_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");System.setProperty("DB_USER","sa");System.setProperty("DB_PASSWORD","");
        DatabaseInitializer.initialize();auth=new AuthService();LegalService.accept();LegalService.accept();auth.createFirstAdmin("admin","AdminTest123!");auth.login("admin","AdminTest123!");new ShopSettings().save("USD",BigDecimal.ZERO,"en");
        products=new ProductService();sales=new SaleService();
        Product p=new Product(0,"Milk","Dairy",10,100,"Farm");p.setBarcode("869000000001");products.addProduct(p);
    }
    @AfterEach void cleanup() {AuthService.clearSession();for(String key:List.of("DB_URL","DB_USER","DB_PASSWORD"))System.clearProperty(key);}
    Sale item(int quantity) {Sale s=new Sale(0,1,AuthService.current().id(),quantity,LocalDate.now());s.setPrice(100);return s;}
    BigDecimal scalar(String sql) throws Exception {try(Connection c=DBConnection.connect();Statement st=c.createStatement();ResultSet rs=st.executeQuery(sql)){rs.next();return rs.getBigDecimal(1);}}
    @Test void paymentTypesAreReportingOnlyAndRetryIsIdempotent() throws Exception {
        String id=UUID.randomUUID().toString();Checkout saved=sales.recordSale(List.of(item(1)),PaymentMethod.CARD,id);
        assertEquals(PaymentMethod.CARD,saved.method());sales.recordSale(List.of(item(1)),PaymentMethod.CARD,id);
        assertEquals(9,scalar("SELECT quantity FROM products WHERE product_id=1").intValue());
        assertEquals(1,scalar("SELECT COUNT(*) FROM checkouts").intValue());assertEquals(1,scalar("SELECT COUNT(*) FROM receipt_records").intValue());
        assertThrows(IllegalStateException.class,()->sales.recordSale(List.of(item(2)),PaymentMethod.CARD,id));
        assertThrows(IllegalStateException.class,()->sales.recordSale(List.of(item(1)),PaymentMethod.CASH,id));
    }
    @Test void oversellRollsBackReceiptAndCheckoutToo() throws Exception {
        assertThrows(IllegalStateException.class,()->sales.addMultipleSales(List.of(item(1),item(11))));
        assertEquals(0,scalar("SELECT COUNT(*) FROM checkouts").intValue());assertEquals(0,scalar("SELECT COUNT(*) FROM receipt_records").intValue());
        assertEquals(10,scalar("SELECT quantity FROM products WHERE product_id=1").intValue());
    }
    @Test void voidRestoresOnceWithActorAndReason() throws Exception {
        Checkout sale=sales.recordSale(List.of(item(1)),UUID.randomUUID().toString());int id=sale.lines().getFirst().getSaleId();
        assertThrows(IllegalStateException.class,()->sales.voidSale(id,""));sales.voidSale(id,"Wrong item");
        assertEquals(10,scalar("SELECT quantity FROM products WHERE product_id=1").intValue());
        assertTrue(sales.loadCheckout(sale.id()).lines().getFirst().isVoided());assertThrows(IllegalStateException.class,()->sales.voidSale(id,"Again"));
        assertEquals(AuthService.current().id(),scalar("SELECT voided_by FROM sales WHERE sale_id="+id).intValue());
    }
    @Test void receiptInsertFailureRollsBackTheWholeCheckout() throws Exception {
        try(Connection c=DBConnection.connect();Statement s=c.createStatement()){s.execute("ALTER TABLE receipt_records ADD CHECK (LENGTH(content)<1)");}
        assertThrows(IllegalStateException.class,()->sales.recordSale(List.of(item(1)),UUID.randomUUID().toString()));
        assertEquals(10,scalar("SELECT quantity FROM products WHERE product_id=1").intValue());
        for(String table:List.of("sales","checkouts","receipt_records"))assertEquals(0,scalar("SELECT COUNT(*) FROM "+table).intValue());
        assertEquals(0,scalar("SELECT COUNT(*) FROM audit_log WHERE action='SALE_CREATED'").intValue());
    }
    @Test void unitCannotChangeAfterSaleAndPaymentReportsStaySeparate() {
        for(PaymentMethod method:PaymentMethod.values())sales.recordSale(List.of(item(1)),method,UUID.randomUUID().toString());
        var rows=new ReportService().load(LocalDate.now(),LocalDate.now());assertEquals(3,rows.size());assertEquals(new BigDecimal("300.00"),new ReportService().revenue(rows));
        Product p=products.loadProducts().getFirst();
        assertThrows(IllegalStateException.class,()->products.updateProduct(new Product(p.getProductId(),p.getName(),p.getCategory(),p.getQuantity(),p.getPrice(),p.getSupplier(),Unit.KG,p.getLowStock(),p.getVersion())));
    }
    @Test void barcodesAreUniqueAndStockAdjustmentsRequireAdminAndReason() {
        assertEquals("869000000001",products.loadProducts().getFirst().getBarcode());
        Product duplicate=new Product(0,"Other","Dairy",1,1,"Farm");duplicate.setBarcode("869000000001");assertThrows(IllegalStateException.class,()->products.addProduct(duplicate));
        assertThrows(IllegalStateException.class,()->products.adjustStock(1,BigDecimal.ONE,""));
        products.adjustStock(1,new BigDecimal("5"),"Delivery received");assertEquals(new BigDecimal("15.000"),products.loadProducts().getFirst().getQuantity());
        assertThrows(IllegalArgumentException.class,()->products.adjustStock(1,new BigDecimal("-16"),"Too much removal"));
        auth.createStaff("cashier","Temporary123!");auth.login("cashier","Temporary123!");auth.changePassword("Temporary123!","PrivatePass123!");
        assertThrows(IllegalStateException.class,()->products.adjustStock(1,BigDecimal.ONE,"Unauthorized"));
    }
    @Test void separateLoginsShareStockButKeepPermissions() {
        auth.createStaff("cashier","Temporary123!");auth.logout();auth.login("cashier","Temporary123!");auth.changePassword("Temporary123!","PrivatePass123!");
        Checkout own=sales.recordSale(List.of(item(1)),PaymentMethod.OTHER,UUID.randomUUID().toString());
        auth.logout();auth.login("admin","AdminTest123!");new ShopSettings().save("USD",BigDecimal.ZERO,"en");
        assertEquals(own.id(),sales.loadSales().getFirst().getCheckoutId());
        assertTrue(new AuditService().load().stream().anyMatch(a->a.username().equals("cashier")&&a.action().equals("SALE_CREATED")));
    }
    @Test void backupsRestoreAccountsAndStockWithoutNetwork(@TempDir Path dir) throws Exception {
        System.setProperty("DB_URL","jdbc:h2:file:"+dir.resolve("live/inventory")+";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        Files.createDirectories(dir.resolve("live"));DatabaseInitializer.initialize();LegalService.accept();auth.createFirstAdmin("admin","AdminTest123!");auth.login("admin","AdminTest123!");new ShopSettings().save("USD",BigDecimal.ZERO,"en");products.addProduct(new Product(0,"Restored product","Test",7,10,"Supplier"));
        Path backup=dir.resolve("shop.zip");new BackupService().create(backup);assertTrue(Files.size(backup)>0);
        assertThrows(IllegalStateException.class,()->new BackupService().create(backup));
        org.h2.tools.Restore.execute(backup.toString(),dir.resolve("restore").toString(),"inventory");
        System.setProperty("DB_URL","jdbc:h2:file:"+dir.resolve("restore/inventory")+";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        auth.login("admin","AdminTest123!");new ShopSettings().save("USD",BigDecimal.ZERO,"en");assertEquals("Restored product",products.loadProducts().getFirst().getName());assertEquals(new BigDecimal("7.000"),products.loadProducts().getFirst().getQuantity());
        auth.createStaff("cashier","Temporary123!");auth.login("cashier","Temporary123!");auth.changePassword("Temporary123!","PrivatePass123!");assertThrows(IllegalStateException.class,()->new BackupService().create(dir.resolve("staff.zip")));
    }
    @Test void remoteDatabaseSettingsAreRejectedBeforeAnyNetworkConnection() {
        String previous=System.getProperty("DB_URL");try {System.setProperty("DB_URL","jdbc:mysql://example.invalid/shop");assertThrows(IllegalStateException.class,DBConnection::connect);}finally {System.setProperty("DB_URL",previous);}
    }
}
