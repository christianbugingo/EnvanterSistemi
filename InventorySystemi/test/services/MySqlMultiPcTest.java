package services;

import database.*;
import product.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.math.BigDecimal;
import java.sql.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="INVENTORY_MYSQL_TEST_URL",matches=".+")
class MySqlMultiPcTest {
    @Test void independentStaffProcessesShareStockAndAdminSeesTheirActions() throws Exception {
        String url=System.getenv("INVENTORY_MYSQL_TEST_URL");
        assertTrue(url.startsWith("jdbc:mysql:") && url.contains("/inventory_test"),"Use a disposable inventory_test schema only");
        System.setProperty("DB_URL",url);System.setProperty("DB_USER",System.getenv("INVENTORY_MYSQL_TEST_USER"));System.setProperty("DB_PASSWORD",System.getenv("INVENTORY_MYSQL_TEST_PASSWORD"));
        try {
            // Refuse to change an existing schema. This test never clears an existing shop.
            try(Connection c=DBConnection.connect();ResultSet tables=c.getMetaData().getTables(c.getCatalog(),null,"%",new String[]{"TABLE"})) {assertFalse(tables.next(),"MySQL integration test requires an empty disposable schema");}
            DatabaseInitializer.initialize();DatabaseInitializer.initialize();
            AuthService auth=new AuthService();auth.createFirstAdmin("admin","AdminTest123!");auth.login("admin","AdminTest123!");
            for(String name:List.of("cashier1","cashier2")) {
                auth.createStaff(name,"TemporaryTest123!");auth.login(name,"TemporaryTest123!");auth.changePassword("TemporaryTest123!","PrivateTest123!");auth.login("admin","AdminTest123!");
            }
            new ProductService().addProduct(new Product(0,"Apples","Fruit",new BigDecimal("5.000"),new BigDecimal("49.99"),"Farm",Unit.KG,new BigDecimal("2.000"),0));
            Process first=worker("cashier1",false),second=worker("cashier2",false);
            try {
                assertTrue(first.waitFor(25,TimeUnit.SECONDS));assertTrue(second.waitFor(25,TimeUnit.SECONDS));
                var codes=List.of(first.exitValue(),second.exitValue());
                String output=new String(first.getInputStream().readAllBytes())+new String(second.getInputStream().readAllBytes());
                assertTrue(codes.contains(0)&&codes.contains(2),output);
            } finally {if(first.isAlive())first.destroyForcibly();if(second.isAlive())second.destroyForcibly();}
            var products=new ProductService().loadProducts();assertEquals(new BigDecimal("2.000"),products.getFirst().getQuantity());
            var sales=new SaleService().loadSales();assertEquals(1,sales.size());assertTrue(sales.getFirst().getSoldBy().startsWith("cashier"));
            var revenue=new ReportService().revenue(new ReportService().load(LocalDate.now(),LocalDate.now()));assertEquals(0,revenue.compareTo(new BigDecimal("149.97")));
            assertEquals(1,new AuditService().load().stream().filter(a->a.action().equals("SALE_CREATED")&&a.username().startsWith("cashier")).count());
            new SaleService().voidSale(sales.getFirst().getSaleId());assertEquals(new BigDecimal("5.000"),new ProductService().loadProducts().getFirst().getQuantity());
            // A staff JVM stays signed in while a separate admin session disables it.
            Process openSession=worker("cashier1",true);
            try {
                var reader=new java.io.BufferedReader(new java.io.InputStreamReader(openSession.getInputStream()));
                ExecutorService timeout=Executors.newSingleThreadExecutor();
                try {assertEquals("READY",timeout.submit(reader::readLine).get(20,TimeUnit.SECONDS));} finally {timeout.shutdownNow();}
                int id=auth.users().stream().filter(u->u.username().equals("cashier1")).findFirst().orElseThrow().id();auth.setStaffActive(id,false);
                openSession.getOutputStream().write('\n');openSession.getOutputStream().flush();
                assertTrue(openSession.waitFor(20,TimeUnit.SECONDS));assertEquals(2,openSession.exitValue());
                assertEquals(new BigDecimal("5.000"),new ProductService().loadProducts().getFirst().getQuantity());
            } finally {if(openSession.isAlive())openSession.destroyForcibly();}
        } finally {AuthService.clearSession();System.clearProperty("DB_URL");System.clearProperty("DB_USER");System.clearProperty("DB_PASSWORD");}
    }
    private Process worker(String name,boolean wait) throws Exception {
        String java=Path.of(System.getProperty("java.home"),"bin","java").toString();
        String classpath=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
        ProcessBuilder builder=new ProcessBuilder(java,"-cp",classpath,"services.RemoteCheckoutWorker",name,wait?"wait":"sell").redirectErrorStream(true);
        builder.environment().put("DB_URL",System.getProperty("DB_URL"));builder.environment().put("DB_USER",System.getProperty("DB_USER"));builder.environment().put("DB_PASSWORD",System.getProperty("DB_PASSWORD"));
        return builder.start();
    }
}
