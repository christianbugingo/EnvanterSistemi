package services;

import database.*;
import product.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SaleServiceTest {
    AuthService auth;
    ProductService products;
    SaleService sales;
    int adminId;
    @BeforeEach void setup() throws Exception {
        System.setProperty("DB_URL","jdbc:h2:mem:test_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        System.setProperty("DB_USER","sa"); System.setProperty("DB_PASSWORD","");
        DatabaseInitializer.initialize(); auth=new AuthService(); auth.createFirstAdmin("admin","AdminTest123!");
        adminId=auth.login("admin","AdminTest123!").id(); products=new ProductService(); sales=new SaleService();
        products.addProduct(new Product(0,"Keyboard","Electronics",5,100,"Supplier"));
    }
    @AfterEach void cleanup() {
        AuthService.clearSession(); System.clearProperty("DB_URL");System.clearProperty("DB_USER");System.clearProperty("DB_PASSWORD");
    }
    Sale item(double quantity) { return item(new BigDecimal(Double.toString(quantity)),Unit.PIECE,new BigDecimal("100")); }
    Sale item(BigDecimal quantity,Unit unit,BigDecimal price) {
        Sale s=new Sale(0,1,AuthService.current().id(),quantity,LocalDate.now());s.setPrice(price);s.setUnit(unit);return s;
    }
    BigDecimal value(String sql) throws Exception {
        try(Connection c=DBConnection.connect();Statement st=c.createStatement();ResultSet rs=st.executeQuery(sql)) { rs.next();return rs.getBigDecimal(1); }
    }
    @Test void saleDeductsStockAndUsesPriceSnapshot() throws Exception {
        sales.addMultipleSales(List.of(item(3)));
        assertEquals(0,value("SELECT quantity FROM products WHERE product_id=1").compareTo(new BigDecimal("2")));
        Product p=products.loadProducts().getFirst();
        products.updateProduct(new Product(1,p.getName(),p.getCategory(),p.getQuantity(),new BigDecimal("200"),p.getSupplier(),p.getUnit(),p.getLowStock(),p.getVersion()));
        var rows=new ReportService().load(LocalDate.now(),LocalDate.now());
        assertEquals(0,new ReportService().revenue(rows).compareTo(new BigDecimal("300")));
        assertEquals("Keyboard",sales.loadSales().getFirst().getProductName());
    }
    @Test void oversellAndLaterCartFailureRollBackStockSalesAndAudit() throws Exception {
        assertThrows(IllegalStateException.class,()->sales.addMultipleSales(List.of(item(6))));
        assertThrows(IllegalStateException.class,()->sales.addMultipleSales(List.of(item(3),item(3))));
        assertEquals(0,value("SELECT quantity FROM products WHERE product_id=1").compareTo(new BigDecimal("5")));
        assertEquals(0,value("SELECT COUNT(*) FROM sales").intValue());
        assertEquals(0,value("SELECT COUNT(*) FROM audit_log WHERE action='SALE_CREATED'").intValue());
    }
    @Test void voidRestoresStoredQuantityExactlyOnceAndRetainsHistory() throws Exception {
        sales.addMultipleSales(List.of(item(3)));int id=sales.loadSales().getFirst().getSaleId();
        sales.voidSale(id);assertThrows(IllegalStateException.class,()->sales.voidSale(id));
        assertEquals(5,value("SELECT quantity FROM products WHERE product_id=1").intValue());
        assertTrue(sales.loadSales().getFirst().isVoided());
        assertTrue(new ReportService().load(LocalDate.now(),LocalDate.now()).isEmpty());
        assertThrows(IllegalStateException.class,()->products.deleteProduct(1));
    }
    @Test void kilosAreExactAndReportsSeparateUnits() throws Exception {
        products.addProduct(new Product(0,"Apples","Fruit",new BigDecimal("10.000"),new BigDecimal("49.99"),"Farm",Unit.KG,new BigDecimal("2.000"),0));
        Sale kg=new Sale(0,2,adminId,new BigDecimal("1.250"),LocalDate.now());kg.setUnit(Unit.KG);kg.setPrice(new BigDecimal("49.99"));
        sales.addMultipleSales(List.of(item(1),kg));
        assertEquals(0,value("SELECT quantity FROM products WHERE product_id=2").compareTo(new BigDecimal("8.750")));
        assertEquals(new BigDecimal("62.49"),kg.getTotal());
        var report=new ReportService().load(LocalDate.now(),LocalDate.now());assertEquals(2,report.size());
        assertEquals(0,new ReportService().revenue(report).compareTo(new BigDecimal("162.49")));
        assertThrows(IllegalArgumentException.class,()->sales.addMultipleSales(List.of(item(0.5))));
        assertThrows(IllegalArgumentException.class,()->Unit.KG.validate(new BigDecimal("0.0001"),false));
        assertThrows(IllegalArgumentException.class,()->Unit.KG.validate(BigDecimal.ZERO,false));
    }
    @Test void rejectsStalePriceAndStockEdits() throws Exception {
        Product stale=products.loadProducts().getFirst(); sales.addMultipleSales(List.of(item(1)));
        assertThrows(IllegalStateException.class,()->products.updateProduct(stale));
        Sale wrong=item(1);wrong.setPrice(1);assertThrows(IllegalStateException.class,()->sales.addMultipleSales(List.of(wrong)));
        assertThrows(IllegalStateException.class,()->new ReportService().load(LocalDate.now(),LocalDate.now().minusDays(1)));
    }
    @Test void concurrentCheckoutCannotOversell() throws Exception {
        CyclicBarrier ready=new CyclicBarrier(2);ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> sell=()->{ready.await();try {sales.addMultipleSales(List.of(item(3)));return true;}catch(IllegalStateException ex){return false;}};
            Future<Boolean> a=pool.submit(sell),b=pool.submit(sell);
            assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            assertEquals(2,value("SELECT quantity FROM products WHERE product_id=1").intValue());
            assertEquals(1,value("SELECT COUNT(*) FROM sales").intValue());
        } finally {pool.shutdownNow();}
    }
    @Test void staffMustChangePasswordAndCannotAccessAdminFunctions() throws Exception {
        auth.createStaff("cashier","TempPass123!");
        AuthService.User staff=auth.login("cashier","TempPass123!");assertTrue(staff.mustChangePassword());
        assertThrows(IllegalStateException.class,()->products.loadProducts());
        assertThrows(IllegalStateException.class,()->auth.changePassword("TempPass123!","TempPass123!"));
        auth.changePassword("TempPass123!","MyPrivatePass123!");
        assertFalse(AuthService.current().mustChangePassword()); assertEquals(1,products.loadProducts().size());
        assertThrows(IllegalStateException.class,()->auth.createStaff("other","TempPass123!"));
        assertThrows(IllegalStateException.class,()->products.addProduct(new Product(0,"Bad","Test",1,1,"Test")));
        assertThrows(IllegalStateException.class,()->products.updateProduct(products.loadProducts().getFirst()));
        assertThrows(IllegalStateException.class,()->products.deleteProduct(1));
        assertThrows(IllegalStateException.class,()->new AuditService().load());
        assertThrows(IllegalStateException.class,()->new ReportService().load(LocalDate.now(),LocalDate.now()));
        sales.addMultipleSales(List.of(item(1)));int saleId=sales.loadSales().getFirst().getSaleId();
        assertThrows(IllegalStateException.class,()->sales.voidSale(saleId));
        Sale forged=new Sale(0,1,adminId,1,LocalDate.now());forged.setPrice(100);
        assertThrows(IllegalStateException.class,()->sales.addMultipleSales(List.of(forged)));
        auth.login("admin","AdminTest123!");sales.addMultipleSales(List.of(item(1)));
        assertTrue(new AuditService().load().stream().anyMatch(a->a.username().equals("cashier")&&a.action().equals("SALE_CREATED")));
        auth.login("cashier","MyPrivatePass123!");assertEquals(1,sales.loadSales().size());
        assertEquals(staff.id(),sales.loadSales().getFirst().getUserId());
    }
    @Test void adminResetAndDisableRevokeAlreadyOpenStaffSessions() throws Exception {
        auth.createStaff("cashier","TempPass123!"); int staffId=auth.users().stream().filter(u->u.username().equals("cashier")).findFirst().orElseThrow().id();
        auth.resetStaffPassword(staffId,"AnotherTemp123!");
        assertThrows(IllegalStateException.class,()->auth.login("cashier","TempPass123!"));
        auth.login("cashier","AnotherTemp123!");auth.changePassword("AnotherTemp123!","PrivatePass123!");
        // Simulate a reset/disable by another running PC while this session remains open.
        try(Connection c=DBConnection.connect();Statement s=c.createStatement()) {s.executeUpdate("UPDATE users SET active=FALSE WHERE user_id="+staffId);}
        assertThrows(IllegalStateException.class,()->sales.addMultipleSales(List.of(item(1))));
        auth.login("admin","AdminTest123!");auth.setStaffActive(staffId,true);auth.resetStaffPassword(staffId,"ResetAgain123!");
        assertThrows(IllegalStateException.class,()->auth.login("cashier","PrivatePass123!"));
        assertTrue(auth.login("cashier","ResetAgain123!").mustChangePassword());
    }
    @Test void legacyPlaintextLoginIsUpgradedAndPasswordsNeverEnterAudit() throws Exception {
        try(Connection c=DBConnection.connect();Statement s=c.createStatement()) {s.executeUpdate("INSERT INTO users(username,password,role) VALUES('legacy','LegacyPass123!','staff')");}
        auth.login("legacy","LegacyPass123!");
        try(Connection c=DBConnection.connect();Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT password FROM users WHERE username='legacy'")) {rs.next();assertTrue(rs.getString(1).startsWith("pbkdf2$"));}
        auth.login("admin","AdminTest123!");
        assertTrue(new AuditService().load().stream().noneMatch(a->a.details().contains("Pass123")));
        assertThrows(IllegalStateException.class,()->auth.createFirstAdmin("second","AdminTest123!"));
        assertThrows(IllegalStateException.class,()->auth.createStaff("admin","TempPass123!"));
    }
}
