package services;
import database.*;
import product.*;
import org.junit.jupiter.api.*;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class GlobalShopTest {
    AuthService auth=new AuthService();
    @BeforeEach void setup() throws Exception {
        System.setProperty("DB_URL","jdbc:h2:mem:global_"+UUID.randomUUID()+";DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");System.setProperty("DB_USER","sa");System.setProperty("DB_PASSWORD","");DatabaseInitializer.initialize();
    }
    @AfterEach void cleanup(){AuthService.clearSession();for(String k:List.of("DB_URL","DB_USER","DB_PASSWORD"))System.clearProperty(k);}
    void owner(){LegalService.accept();auth.createFirstAdmin("owner","PrivateOwner123!");auth.login("owner","PrivateOwner123!");}
    @Test void currentCatalogueAndStableFormatting(){
        assertTrue(Currencies.all().size()>150);assertEquals(-1,Currencies.get("XAU").minorUnits());
        try(Connection c=DBConnection.connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM currencies")){r.next();assertEquals(Currencies.all().size(),r.getInt(1));}catch(SQLException ex){fail(ex);}
        assertEquals(0,Currencies.get("RWF").minorUnits());assertEquals(0,Currencies.get("JPY").minorUnits());assertEquals(2,Currencies.get("USD").minorUnits());assertEquals(3,Currencies.get("KWD").minorUnits());
        gui.LocaleManager.setLanguage("tr");try{assertEquals("1,250 RWF",Currencies.get("RWF").format(new BigDecimal("1250")));assertEquals("12.50 USD",Currencies.get("USD").format(new BigDecimal("12.5")));assertEquals("12.346 KWD",Currencies.get("KWD").format(new BigDecimal("12.3455")));}finally{gui.LocaleManager.setLanguage("en");}
    }
    @Test void roundingAndTaxAtCurrencyPrecision(){
        assertEquals(new BigDecimal("13"),Currencies.get("RWF").round(new BigDecimal("12.5")));assertEquals(new BigDecimal("12.35"),Currencies.get("USD").round(new BigDecimal("12.345")));assertEquals(new BigDecimal("12.346"),Currencies.get("KWD").round(new BigDecimal("12.3455")));
        assertEquals(new BigDecimal("12.35"),Amounts.calculate(new BigDecimal("12.35"),BigDecimal.ZERO,Currencies.get("USD")).total());
        var taxed=Amounts.calculate(new BigDecimal("12.35"),new BigDecimal("18"),Currencies.get("USD"));assertEquals(new BigDecimal("2.22"),taxed.tax());assertEquals(new BigDecimal("14.57"),taxed.total());
        for(String bad:List.of("-1","100.0001","0.00001"))assertThrows(IllegalStateException.class,()->ShopSettings.validateTax(new BigDecimal(bad)));
    }
    @Test void termsAreRequiredAndAcceptanceIsPersistent(){
        assertFalse(LegalService.accepted());assertThrows(IllegalStateException.class,()->auth.createFirstAdmin("owner","PrivateOwner123!"));
        owner();assertTrue(LegalService.accepted());assertFalse(LegalService.acceptedAt().isBlank());assertEquals("1.3.0",LegalService.VERSION);
        try(Connection c=DBConnection.connect()){assertEquals(LegalService.acceptedAt(),ShopSettings.get(c,"terms_accepted_at",""));}catch(SQLException e){fail(e);}
    }
    Sale line(Product p,BigDecimal qty){Sale s=new Sale(0,p.getProductId(),AuthService.current().id(),qty,LocalDate.now());s.setPrice(p.getPrice());s.setUnit(p.getUnit());return s;}
    @Test void originalCurrencyAndTaxSurviveSettingsChangeAndReprint(){
        owner();new ShopSettings().save("USD",new BigDecimal("18"),"en");ProductService products=new ProductService();
        products.addProduct(new Product(0,"Rice","Food",new BigDecimal("2"),new BigDecimal("49.99"),"Farm",Unit.KG,BigDecimal.ZERO,0));
        SaleService sales=new SaleService();Checkout old=sales.recordSale(List.of(line(products.loadProducts().getFirst(),new BigDecimal("0.230"))),PaymentMethod.CARD,UUID.randomUUID().toString());
        assertEquals(new BigDecimal("11.50"),old.subtotal());assertEquals(new BigDecimal("2.07"),old.taxAmount());assertEquals(new BigDecimal("13.57"),old.total());
        new ShopSettings().save("RWF",BigDecimal.ZERO,"tr");Checkout restored=sales.loadCheckout(old.id());assertEquals("USD",restored.currency().code());assertEquals(2,restored.currency().minorUnits());assertEquals(new BigDecimal("18.0000"),restored.taxRate());
        String receipt=utils.ReceiptGenerator.render(restored);assertTrue(receipt.contains("13.57 USD"));assertTrue(receipt.contains("2.07 USD"));assertFalse(receipt.contains("RWF"));
        Checkout newer=sales.recordSale(List.of(line(products.loadProducts().getFirst(),new BigDecimal("0.230"))),UUID.randomUUID().toString());assertEquals("RWF",newer.currency().code());assertEquals(new BigDecimal("12"),newer.total());
        var rows=new ReportService().load(LocalDate.now(),LocalDate.now());assertEquals(2,rows.size());assertThrows(IllegalStateException.class,()->new ReportService().revenue(rows));assertTrue(new ReportService().summary(rows).contains("USD"));
        String noTax=utils.ReceiptGenerator.render(newer);assertFalse(noTax.contains("Tax ("));
    }
    @Test void choosingInitialCurrencyRoundsExistingProductPricesWithoutInventingASaleCurrency() throws Exception {
        owner();try(Connection c=DBConnection.connect();Statement s=c.createStatement()){s.execute("INSERT INTO products(name,category,quantity,price,supplier,low_stock) VALUES('Legacy','Test',10,49.99,'Farm',0)");}
        new ShopSettings().save("RWF",BigDecimal.ZERO,"en");assertEquals(0,new ProductService().loadProducts().getFirst().getPrice().compareTo(new BigDecimal("50")));
    }
    @Test void staffCannotChangeFinancialSettings(){
        owner();new ShopSettings().save("USD",BigDecimal.ZERO,"en");auth.createStaff("cashier","TemporaryPassword123!");auth.login("cashier","TemporaryPassword123!");auth.changePassword("TemporaryPassword123!","PrivateCashier123!");
        assertThrows(IllegalStateException.class,()->new ShopSettings().save("RWF",BigDecimal.TEN,"en"));assertEquals("USD",ShopSettings.currency().code());
    }
    @Test void newWeightPricesApplyOnlyToFutureSales(){
        owner();new ShopSettings().save("RWF",BigDecimal.ZERO,"en");var products=new ProductService();products.addProduct(new Product(0,"Weighted product","Food",new BigDecimal("10"),new BigDecimal("100"),"Farm",Unit.KG,BigDecimal.ZERO,0));
        var sales=new SaleService();var p=products.loadProducts().getFirst();var old=sales.recordSale(List.of(line(p,new BigDecimal("0.2"))),UUID.randomUUID().toString());assertEquals(new BigDecimal("20"),old.total());
        p=products.loadProducts().getFirst();products.updateProduct(new Product(p.getProductId(),p.getName(),p.getCategory(),p.getQuantity(),new BigDecimal("200"),p.getSupplier(),p.getUnit(),p.getLowStock(),p.getVersion()));
        var next=sales.recordSale(List.of(line(products.loadProducts().getFirst(),new BigDecimal("0.23"))),UUID.randomUUID().toString());assertEquals(new BigDecimal("46"),next.total());assertEquals(new BigDecimal("20"),sales.loadCheckout(old.id()).total());assertEquals(0,sales.loadCheckout(old.id()).lines().getFirst().getPrice().compareTo(new BigDecimal("100")));
    }
    @Test void taxOncePerCheckoutAcrossUnitsAndVoids(){
        owner();new ShopSettings().save("KWD",new BigDecimal("5"),"en");var products=new ProductService();products.addProduct(new Product(0,"Piece","Test",10,1,"Farm"));products.addProduct(new Product(0,"Weight","Test",new BigDecimal("10"),new BigDecimal("1.234"),"Farm",Unit.KG,BigDecimal.ZERO,0));
        var p=products.loadProducts();var checkout=new SaleService().recordSale(List.of(line(p.get(0),BigDecimal.ONE),line(p.get(1),new BigDecimal("0.230"))),UUID.randomUUID().toString());
        assertEquals(new BigDecimal("1.284"),checkout.subtotal());assertEquals(new BigDecimal("0.064"),checkout.taxAmount());assertEquals(new BigDecimal("1.348"),checkout.total());
        var rows=new ReportService().load(LocalDate.now(),LocalDate.now());assertEquals(checkout.total(),new ReportService().revenue(rows));
        new SaleService().voidSale(checkout.lines().getFirst().getSaleId(),"Correction");rows=new ReportService().load(LocalDate.now(),LocalDate.now());assertEquals(new BigDecimal("0.298"),new ReportService().revenue(rows));
    }
}
