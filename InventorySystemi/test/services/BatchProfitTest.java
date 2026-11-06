package services;

import database.*;
import product.*;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BatchProfitTest {
    AuthService auth=new AuthService();ProductService products=new ProductService();BatchService batches=new BatchService();SaleService sales=new SaleService();
    @BeforeEach void setup() throws Exception {
        System.setProperty("DB_URL","jdbc:h2:mem:batches_"+UUID.randomUUID()+";DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");System.setProperty("DB_USER","sa");System.setProperty("DB_PASSWORD","");DatabaseInitializer.initialize();LegalService.accept();auth.createFirstAdmin("owner","OwnerPassword123!");auth.login("owner","OwnerPassword123!");new ShopSettings().save("RWF",BigDecimal.ZERO,"en");products.addProduct(new Product(0,"Biscuits","Food",0,150,"Farm"));
    }
    @AfterEach void cleanup(){AuthService.clearSession();for(String key:List.of("DB_URL","DB_USER","DB_PASSWORD"))System.clearProperty(key);}
    Checkout sell(String qty){return sales.recordSale(sales.quoteProduct(1,AuthService.current().id(),new BigDecimal(qty)),UUID.randomUUID().toString());}
    BigDecimal scalar(String sql) throws Exception {try(Connection c=DBConnection.connect();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getBigDecimal(1);}}
    @Test void fifoBiscuitCostsAndExactVoidRestoration() throws Exception {
        batches.receive(1,new BigDecimal("10"),new BigDecimal("1000"),LocalDate.now().minusDays(1),null);batches.receive(1,new BigDecimal("10"),new BigDecimal("1010"),LocalDate.now(),null);
        var first=sell("5");assertEquals(5,batches.load(1).getFirst().remaining());assertEquals(new BigDecimal("500.0000"),batches.load(1).getFirst().remainingCost());
        assertEquals(0,scalar("SELECT cost_of_goods FROM sales WHERE sale_id="+first.lines().getFirst().getSaleId()).compareTo(new BigDecimal("500")));
        var second=sell("10");assertEquals(0,batches.load(1).getFirst().remaining());assertEquals(5,batches.load(1).get(1).remaining());
        assertEquals(0,scalar("SELECT cost_of_goods FROM sales WHERE sale_id="+second.lines().getFirst().getSaleId()).compareTo(new BigDecimal("1005")));
        var profit=new DashboardService().profit(ProfitPeriod.TODAY,LocalDate.now(),null,null,Currencies.get("RWF"));assertEquals(new BigDecimal("2250"),profit.revenue());assertEquals(new BigDecimal("1505"),profit.cost());assertEquals(new BigDecimal("745"),profit.grossProfit());assertEquals(new BigDecimal("33.11"),profit.margin());assertFalse(profit.incomplete());
        sales.voidSale(second.lines().getFirst().getSaleId(),"Mistake");assertEquals(5,batches.load(1).getFirst().remaining());assertEquals(10,batches.load(1).get(1).remaining());assertThrows(IllegalStateException.class,()->sales.voidSale(second.lines().getFirst().getSaleId(),"Again"));
    }
    @Test void receiptFailureRollsBackFifoAllocationsAndCosts() throws Exception {
        batches.receive(1,new BigDecimal("10"),new BigDecimal("1000"),LocalDate.now(),null);
        try(Connection c=DBConnection.connect();Statement s=c.createStatement()){s.execute("ALTER TABLE receipt_records ADD CHECK(LENGTH(content)<1)");}
        assertThrows(IllegalStateException.class,()->sell("5"));assertEquals(10,batches.load(1).getFirst().remaining());assertEquals(0,batches.load(1).getFirst().remainingCost().compareTo(new BigDecimal("1000")));assertEquals(0,scalar("SELECT COUNT(*) FROM sale_batch_allocations").intValue());assertEquals(10,products.loadProducts().getFirst().getQuantity().intValue());
    }
    @Test void partialRoundingRetainsAllPurchaseCost() throws Exception {
        batches.receive(1,new BigDecimal("3"),BigDecimal.TEN,LocalDate.now(),null);sell("1");sell("1");sell("1");assertEquals(0,scalar("SELECT SUM(cost_of_goods) FROM sales").compareTo(BigDecimal.TEN));assertEquals(0,batches.load(1).getFirst().remainingCost().signum());
    }
    @Test void batchSpecificPricesProduceSeparateHistoricalLines(){
        batches.receive(1,new BigDecimal("2"),new BigDecimal("200"),LocalDate.now().minusDays(1),new BigDecimal("170"));batches.receive(1,new BigDecimal("2"),new BigDecimal("210"),LocalDate.now(),new BigDecimal("190"));
        var checkout=sell("3");assertEquals(2,checkout.lines().size());assertEquals(new BigDecimal("530"),checkout.total());assertEquals(new BigDecimal("170.0000"),checkout.lines().getFirst().getPrice());assertEquals(new BigDecimal("190.0000"),checkout.lines().get(1).getPrice());assertEquals(0,products.loadProducts().getFirst().getPrice().compareTo(new BigDecimal("150")));
        var p=products.loadProducts().getFirst();products.updateProduct(new Product(p.getProductId(),p.getName(),p.getCategory(),p.getQuantity(),new BigDecimal("250"),p.getSupplier(),p.getUnit(),p.getLowStock(),p.getVersion()));assertEquals(new BigDecimal("530"),sales.loadCheckout(checkout.id()).total());assertEquals(0,batches.load(1).getFirst().costPerUnit().compareTo(new BigDecimal("100")));
    }
    @Test void unknownCostsAreNotInventedAndCurrenciesAreNeverConverted(){
        products.adjustStock(1,new BigDecimal("2"),"Opening count");sell("1");var p=new DashboardService().profit(ProfitPeriod.TODAY,LocalDate.now(),null,null,Currencies.get("RWF"));assertTrue(p.incomplete());assertEquals(new BigDecimal("150"),p.revenue());assertEquals(new BigDecimal("150"),p.unknownRevenue());assertEquals(BigDecimal.ZERO,p.grossProfit());assertNull(p.margin());
        batches.receive(1,BigDecimal.ONE,new BigDecimal("100"),LocalDate.now(),new BigDecimal("170"));new ShopSettings().save("USD",BigDecimal.ZERO,"en");sell("2");var dollar=new DashboardService().profit(ProfitPeriod.TODAY,LocalDate.now(),null,null,Currencies.get("USD"));assertTrue(dollar.incomplete());assertEquals(new BigDecimal("300.00"),dollar.unknownRevenue());
    }
    @Test void duplicatesAreRejectedUntilRenamed(){
        assertNotNull(products.findDuplicate(" biscuits ",Unit.PIECE,0));assertThrows(IllegalStateException.class,()->products.addProduct(new Product(0,"BISCUITS","Food",0,150,"Other")));products.addProduct(new Product(0,"Other biscuits","Food",0,150,"Other"));assertEquals(2,products.loadProducts().size());
    }
    @Test void everyWholeKilogramFromOneToTwentyThreeCanBeSoldExactly(){
        products.addProduct(new Product(0,"Rice","Food",BigDecimal.ZERO,new BigDecimal("100"),"Farm",Unit.KG,BigDecimal.ZERO,0));batches.receive(2,new BigDecimal("300"),new BigDecimal("15000"),LocalDate.now(),null);
        for(int kg=1;kg<=23;kg++){var qty=Quantities.parse(kg+" kg",Unit.KG);assertEquals(kg+" kg",Quantities.format(qty,Unit.KG));var sold=sales.recordSale(sales.quoteProduct(2,AuthService.current().id(),qty),UUID.randomUUID().toString());assertEquals(new BigDecimal(kg*100),sold.total());}
        assertEquals(new BigDecimal("24.000"),products.loadProducts().get(1).getQuantity());
        assertEquals("19.5 kg",Quantities.format(new BigDecimal("19.500"),Unit.KG));assertEquals("1.25 kg",Quantities.format(new BigDecimal("1.250"),Unit.KG));assertEquals("230 g",Quantities.format(new BigDecimal("0.230"),Unit.KG));
    }
    @Test void taxDoubleZeroIsCanonicalAndStaffProfitIsScopedAndOptIn(){
        assertEquals(BigDecimal.ZERO,ShopSettings.parseTax("00"));new ShopSettings().save("RWF",ShopSettings.parseTax("00"),"en");assertEquals(BigDecimal.ZERO,ShopSettings.current().taxRate());
        batches.receive(1,new BigDecimal("10"),new BigDecimal("1000"),LocalDate.now(),null);sell("2");
        auth.createStaff("cashier","CashierPassword123!");auth.login("cashier","CashierPassword123!");auth.changePassword("CashierPassword123!","NewCashierPassword123!");sell("1");
        var service=new DashboardService();assertFalse(service.canViewProfit());assertThrows(IllegalStateException.class,()->new ShopSettings().setStaffProfitVisible(true));assertThrows(IllegalStateException.class,()->service.profit(ProfitPeriod.TODAY,LocalDate.now(),null,null,Currencies.get("RWF")));assertEquals(new BigDecimal("150"),service.metrics(LocalDate.now(),Currencies.get("RWF")).revenue());assertEquals(1,service.metrics(LocalDate.now(),Currencies.get("RWF")).transactions());
        auth.login("owner","OwnerPassword123!");new ShopSettings().setStaffProfitVisible(true);assertEquals(new BigDecimal("450"),service.metrics(LocalDate.now(),Currencies.get("RWF")).revenue());auth.login("cashier","NewCashierPassword123!");assertTrue(service.canViewProfit());assertEquals(new BigDecimal("50"),service.profit(ProfitPeriod.TODAY,LocalDate.now(),null,null,Currencies.get("RWF")).grossProfit());
    }
    @Test void periodBoundariesAndProfitAreCorrect() throws Exception {
        LocalDate today=LocalDate.of(2026,11,6);
        assertEquals(LocalDate.of(2026,11,2),ProfitPeriod.WEEK.range(today,null,null).start());assertEquals(LocalDate.of(2026,11,1),ProfitPeriod.MONTH.range(today,null,null).start());assertEquals(LocalDate.of(2026,1,1),ProfitPeriod.YEAR.range(today,null,null).start());
        batches.receive(1,new BigDecimal("10"),new BigDecimal("1000"),LocalDate.now(),null);
        for(LocalDate date:List.of(today,today.minusDays(3),today.minusDays(5),LocalDate.of(2026,3,1),LocalDate.of(2025,12,31))){var checkout=sell("1");try(Connection c=DBConnection.connect();PreparedStatement p=c.prepareStatement("UPDATE sales SET sale_date=? WHERE checkout_id=?")){p.setDate(1,java.sql.Date.valueOf(date));p.setString(2,checkout.id());p.executeUpdate();}}
        var s=new DashboardService();assertEquals(new BigDecimal("50"),s.profit(ProfitPeriod.TODAY,today,null,null,Currencies.get("RWF")).grossProfit());assertEquals(new BigDecimal("100"),s.profit(ProfitPeriod.WEEK,today,null,null,Currencies.get("RWF")).grossProfit());assertEquals(new BigDecimal("150"),s.profit(ProfitPeriod.MONTH,today,null,null,Currencies.get("RWF")).grossProfit());assertEquals(new BigDecimal("200"),s.profit(ProfitPeriod.YEAR,today,null,null,Currencies.get("RWF")).grossProfit());assertEquals(new BigDecimal("250"),s.profit(ProfitPeriod.CUSTOM,today,LocalDate.of(2025,1,1),today,Currencies.get("RWF")).grossProfit());
        for(var period:ProfitPeriod.values())assertEquals(new BigDecimal("33.33"),s.profit(period,today,LocalDate.of(2025,1,1),today,Currencies.get("RWF")).margin());
        assertThrows(IllegalStateException.class,()->ProfitPeriod.CUSTOM.range(today,today,today.minusDays(1)));
    }
}
