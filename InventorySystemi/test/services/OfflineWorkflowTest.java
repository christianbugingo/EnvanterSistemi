package services;

import database.*;
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
    AuthService auth;ProductService products;SaleService sales;ShiftService shifts;
    @BeforeEach void setup() throws Exception {
        System.setProperty("DB_URL","jdbc:h2:mem:offline_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");System.setProperty("DB_USER","sa");System.setProperty("DB_PASSWORD","");
        DatabaseInitializer.initialize();auth=new AuthService();auth.createFirstAdmin("admin","AdminTest123!");auth.login("admin","AdminTest123!");
        products=new ProductService();sales=new SaleService();shifts=new ShiftService();
        Product p=new Product(0,"Milk","Dairy",10,100,"Farm");p.setBarcode("869000000001");products.addProduct(p);
    }
    @AfterEach void cleanup() {AuthService.clearSession();for(String key:List.of("DB_URL","DB_USER","DB_PASSWORD"))System.clearProperty(key);}
    Sale item(int quantity) {Sale s=new Sale(0,1,AuthService.current().id(),quantity,LocalDate.now());s.setPrice(100);return s;}
    BigDecimal scalar(String sql) throws Exception {try(Connection c=DBConnection.connect();Statement st=c.createStatement();ResultSet rs=st.executeQuery(sql)){rs.next();return rs.getBigDecimal(1);}}
    @Test void cashAndCardCheckoutIdsPreventDuplicateStockChangesAndReconcileCash() throws Exception {
        shifts.open(new BigDecimal("50"));String id=UUID.randomUUID().toString();
        Checkout cash=sales.checkout(List.of(item(1)),PaymentMethod.CASH,new BigDecimal("120"),id);
        assertEquals(new BigDecimal("20.00"),cash.change());assertEquals(1,cash.lines().size());
        sales.checkout(List.of(item(1)),PaymentMethod.CASH,new BigDecimal("120.00"),id);
        assertEquals(9,scalar("SELECT quantity FROM products WHERE product_id=1").intValue());
        assertEquals(1,scalar("SELECT COUNT(*) FROM checkouts").intValue());
        assertThrows(IllegalStateException.class,()->sales.checkout(List.of(item(2)),PaymentMethod.CASH,new BigDecimal("220"),id));
        Checkout card=sales.checkout(List.of(item(1)),PaymentMethod.CARD,BigDecimal.ZERO,UUID.randomUUID().toString());
        assertEquals(BigDecimal.ZERO.setScale(2),card.cashReceived());
        var shift=shifts.load().getFirst();assertEquals(new BigDecimal("150.00"),shift.expected());
        assertThrows(IllegalStateException.class,()->shifts.close(shift.id(),new BigDecimal("149"),""));
        shifts.close(shift.id(),new BigDecimal("149"),"One lira discrepancy counted");
        assertEquals(new BigDecimal("-1.00"),shifts.load().getFirst().difference());
        assertThrows(IllegalStateException.class,()->sales.addMultipleSales(List.of(item(1))));
        assertThrows(IllegalStateException.class,()->sales.voidSale(cash.lines().getFirst().getSaleId()));
        assertEquals(id,sales.loadCheckout(id).id());
    }
    @Test void noShiftOrInsufficientCashCannotSaveAnything() throws Exception {
        assertThrows(IllegalStateException.class,()->sales.addMultipleSales(List.of(item(1))));
        shifts.open(BigDecimal.ZERO);assertThrows(IllegalStateException.class,()->shifts.open(BigDecimal.ZERO));
        assertThrows(IllegalStateException.class,()->sales.checkout(List.of(item(1)),PaymentMethod.CASH,new BigDecimal("99"),UUID.randomUUID().toString()));
        assertThrows(IllegalStateException.class,()->sales.checkout(List.of(item(1),item(11)),PaymentMethod.CASH,new BigDecimal("1200"),UUID.randomUUID().toString()));
        assertEquals(0,scalar("SELECT COUNT(*) FROM checkouts").intValue());assertEquals(10,scalar("SELECT quantity FROM products WHERE product_id=1").intValue());
    }
    @Test void voidInOpenShiftRestoresCashExpectationOnce() {
        shifts.open(new BigDecimal("50"));Checkout result=sales.checkout(List.of(item(1)),PaymentMethod.CASH,new BigDecimal("100"),UUID.randomUUID().toString());
        sales.voidSale(result.lines().getFirst().getSaleId());assertEquals(new BigDecimal("50.00"),shifts.load().getFirst().expected());
        assertTrue(sales.loadCheckout(result.id()).lines().getFirst().isVoided());assertThrows(IllegalStateException.class,()->sales.voidSale(result.lines().getFirst().getSaleId()));
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
    @Test void rolesAndShiftsSurviveSwitchingUsersOnTheSameLaptop() {
        shifts.open(BigDecimal.ZERO);long adminShift=shifts.load().getFirst().id();
        auth.createStaff("cashier","Temporary123!");auth.logout();auth.login("cashier","Temporary123!");auth.changePassword("Temporary123!","PrivatePass123!");
        assertTrue(shifts.load().isEmpty());assertThrows(IllegalStateException.class,()->shifts.open(BigDecimal.ZERO));
        assertThrows(IllegalStateException.class,()->shifts.close(adminShift,BigDecimal.ZERO,""));
        auth.login("admin","AdminTest123!");shifts.close(adminShift,BigDecimal.ZERO,"");auth.login("cashier","PrivatePass123!");shifts.open(BigDecimal.ZERO);
        Checkout own=sales.checkout(List.of(item(1)),PaymentMethod.CASH,new BigDecimal("100"),UUID.randomUUID().toString());
        shifts.close(shifts.load().getFirst().id(),new BigDecimal("100"),"");auth.logout();auth.login("admin","AdminTest123!");
        assertEquals(2,shifts.load().size());assertEquals(own.id(),sales.loadSales().getFirst().getCheckoutId());
        assertTrue(new AuditService().load().stream().anyMatch(a->a.username().equals("cashier")&&a.action().equals("SHIFT_CLOSED")));
    }
    @Test void backupsRestoreAccountsAndStockWithoutNetwork(@TempDir Path dir) throws Exception {
        System.setProperty("DB_URL","jdbc:h2:file:"+dir.resolve("live/inventory")+";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        Files.createDirectories(dir.resolve("live"));DatabaseInitializer.initialize();auth.createFirstAdmin("admin","AdminTest123!");auth.login("admin","AdminTest123!");products.addProduct(new Product(0,"Restored product","Test",7,10,"Supplier"));
        Path backup=dir.resolve("shop.zip");new BackupService().create(backup);assertTrue(Files.size(backup)>0);
        assertThrows(IllegalStateException.class,()->new BackupService().create(backup));
        org.h2.tools.Restore.execute(backup.toString(),dir.resolve("restore").toString(),"inventory");
        System.setProperty("DB_URL","jdbc:h2:file:"+dir.resolve("restore/inventory")+";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        auth.login("admin","AdminTest123!");assertEquals("Restored product",products.loadProducts().getFirst().getName());assertEquals(new BigDecimal("7.000"),products.loadProducts().getFirst().getQuantity());
        auth.createStaff("cashier","Temporary123!");auth.login("cashier","Temporary123!");auth.changePassword("Temporary123!","PrivatePass123!");assertThrows(IllegalStateException.class,()->new BackupService().create(dir.resolve("staff.zip")));
    }
    @Test void remoteDatabaseSettingsAreRejectedBeforeAnyNetworkConnection() {
        String previous=System.getProperty("DB_URL");try {System.setProperty("DB_URL","jdbc:mysql://example.invalid/shop");assertThrows(IllegalStateException.class,DBConnection::connect);}finally {System.setProperty("DB_URL",previous);}
    }
}
