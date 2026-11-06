package services;

import database.DBConnection;
import product.*;
import javafx.collections.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class SaleService {
    private static Sale read(ResultSet rs) throws SQLException {
        Sale sale=new Sale(rs.getInt("sale_id"),rs.getInt("product_id"),rs.getInt("user_id"),Unit.valueOf(rs.getString("unit")).fromStored(rs.getLong("quantity_sold")),rs.getDate("sale_date").toLocalDate());
        sale.setFinancialSnapshot(Currencies.snapshot(rs.getString("currency_code"),rs.getInt("currency_minor")),rs.getBigDecimal("tax_rate"));sale.setProductName(rs.getString("product_name"));sale.setPrice(rs.getBigDecimal("unit_price"));sale.setUnit(Unit.valueOf(rs.getString("unit")));sale.setSoldBy(rs.getString("sold_by"));sale.setVoided(rs.getBoolean("voided"));sale.setCheckoutId(rs.getString("checkout_id"));sale.setPaymentMethod(rs.getString("payment_method")==null?null:PaymentMethod.valueOf(rs.getString("payment_method")));return sale;
    }
    public ObservableList<Sale> loadSales() {
        try(Connection c=DBConnection.connect()) {
            var actor=AuthService.require(c,false);ObservableList<Sale> rows=FXCollections.observableArrayList();
            try(PreparedStatement ps=c.prepareStatement("SELECT s.*,u.username AS sold_by,ch.payment_method FROM sales s JOIN users u ON s.user_id=u.user_id LEFT JOIN checkouts ch ON s.checkout_id=ch.checkout_id"+(actor.isAdmin()?"":" WHERE s.user_id=?")+" ORDER BY s.sale_id DESC LIMIT 1000")) {
                if(!actor.isAdmin())ps.setInt(1,actor.id());try(ResultSet rs=ps.executeQuery()) {while(rs.next())rows.add(read(rs));}
            }
            return rows;
        } catch(SQLException e) {throw AuthService.databaseError(e);}
    }
    public void addMultipleSales(List<Sale> items) {recordSale(items,UUID.randomUUID().toString());}
    /** Record a checkout with a reporting-only payment type. */
    public Checkout recordSale(List<Sale> items,String checkoutId) {return recordSale(items,PaymentMethod.CASH,checkoutId);}
    public Checkout recordSale(List<Sale> items,PaymentMethod method,String checkoutId) {
        Objects.requireNonNull(method);
        if(items==null || items.isEmpty() || items.stream().anyMatch(Objects::isNull))throw AuthService.failure("sale.empty");
        try {UUID.fromString(checkoutId);}catch(RuntimeException e){throw AuthService.failure("checkout.invalid");}

        String fingerprint=fingerprint(items,method);
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                var actor=AuthService.require(c,false);
                // A stable checkout ID makes repeating the same checkout harmless.
                try(PreparedStatement ps=c.prepareStatement("SELECT user_id,fingerprint FROM checkouts WHERE checkout_id=?")) {
                    ps.setString(1,checkoutId);try(ResultSet rs=ps.executeQuery()) {
                        if(rs.next()) {
                            if(rs.getInt(1)!=actor.id() || !fingerprint.equals(rs.getString(2)))throw AuthService.failure("checkout.conflict");
                            Checkout existing=load(c,checkoutId,actor);c.commit();return existing;
                        }
                    }
                }
                var settings=ShopSettings.read(c);if(settings.currency()==null)throw AuthService.failure("currency.required");
                for(Sale sale:items){if(sale.getCurrency()!=null&&(!sale.getCurrency().code().equals(settings.currency().code())||sale.getTaxRate().compareTo(settings.taxRate())!=0))throw AuthService.failure("settings.changed");sale.setFinancialSnapshot(settings.currency(),settings.taxRate());}
                BigDecimal subtotal=items.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
                var amounts=Amounts.calculate(subtotal,settings.taxRate(),settings.currency());BigDecimal total=amounts.total();
                if(total.signum()<0||total.compareTo(new BigDecimal("999999999999.9999"))>0)throw AuthService.failure("payment.invalid.amount");
                try(PreparedStatement ps=c.prepareStatement("INSERT INTO checkouts(checkout_id,user_id,payment_method,total,fingerprint,currency_code,currency_minor,tax_rate,subtotal,tax_amount) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1,checkoutId);ps.setInt(2,actor.id());ps.setString(3,method.name());ps.setBigDecimal(4,total);ps.setString(5,fingerprint);ps.setString(6,settings.currency().code());ps.setInt(7,settings.currency().minorUnits());ps.setBigDecimal(8,settings.taxRate());ps.setBigDecimal(9,amounts.subtotal());ps.setBigDecimal(10,amounts.tax());ps.executeUpdate();
                }
                var indices=java.util.stream.IntStream.range(0,items.size()).boxed().sorted(Comparator.comparingInt(i->items.get(i).getProductId())).toList();
                for(int index:indices) {
                    Sale sale=items.get(index);if(sale.getUserId()!=actor.id())throw AuthService.failure("msg.access.denied");
                    Product product;
                    try(PreparedStatement ps=c.prepareStatement("SELECT * FROM products WHERE product_id=? FOR UPDATE")) {
                        ps.setInt(1,sale.getProductId());try(ResultSet rs=ps.executeQuery()) {if(!rs.next())throw AuthService.failure("error.stale");product=ProductService.read(rs);}
                    }
                    BigDecimal quantity=product.getUnit().validate(sale.getQuantitySold(),false);
                    if(sale.getUnit()!=product.getUnit())throw AuthService.failure("sale.price.changed");
                    var batchUse=BatchService.consume(c,product.getProductId(),product.getUnit().toStored(quantity),settings.currency());
                    for(var allocation:batchUse.allocations())if(sale.getPrice().compareTo(allocation.sellingPrice()==null||!allocation.currency().code().equals(settings.currency().code())?product.getPrice():allocation.sellingPrice())!=0)throw AuthService.failure("sale.price.changed");
                    try(PreparedStatement ps=c.prepareStatement("UPDATE products SET quantity=quantity-?,version=version+1 WHERE product_id=? AND quantity>=?")) {
                        ps.setLong(1,product.getUnit().toStored(quantity));ps.setInt(2,product.getProductId());ps.setLong(3,product.getUnit().toStored(quantity));if(ps.executeUpdate()!=1)throw AuthService.failure("sale.stock.changed");
                    }
                    try(PreparedStatement ps=c.prepareStatement("INSERT INTO sales(product_id,user_id,quantity_sold,sale_date,unit_price,unit,product_name,checkout_id,line_no,net_amount,currency_code,currency_minor,tax_rate) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1,product.getProductId());ps.setInt(2,actor.id());ps.setLong(3,product.getUnit().toStored(quantity));ps.setDate(4,java.sql.Date.valueOf(LocalDate.now()));ps.setBigDecimal(5,sale.getPrice());ps.setString(6,product.getUnit().name());ps.setString(7,product.getName());ps.setString(8,checkoutId);ps.setInt(9,index);ps.setBigDecimal(10,sale.getTotal());ps.setString(11,settings.currency().code());ps.setInt(12,settings.currency().minorUnits());ps.setBigDecimal(13,settings.taxRate());ps.executeUpdate();
                        try(ResultSet rs=ps.getGeneratedKeys()) {rs.next();sale.setSaleId(rs.getInt(1));}
                    }
                    BatchService.allocate(c,sale.getSaleId(),batchUse);
                    try(PreparedStatement cost=c.prepareStatement("UPDATE sales SET cost_of_goods=?,cost_complete=? WHERE sale_id=?")){cost.setBigDecimal(1,batchUse.cost());cost.setBoolean(2,batchUse.complete());cost.setInt(3,sale.getSaleId());cost.executeUpdate();}
                    sale.setCheckoutId(checkoutId);sale.setProductName(product.getName());sale.setSoldBy(actor.username());
                    AuditService.record(c,actor,"SALE_CREATED","#"+sale.getSaleId()+" | "+product.getName()+" | "+quantity+" "+product.getUnit().name()+" | "+sale.getTotal()+" | "+checkoutId);
                }
                AuditService.record(c,actor,"CHECKOUT_CREATED",checkoutId+" | "+method.name()+" | "+total);
                Checkout result=load(c,checkoutId,actor);
                try(PreparedStatement receipt=c.prepareStatement("INSERT INTO receipt_records(checkout_id,content) VALUES(?,?)")){receipt.setString(1,checkoutId);receipt.setString(2,utils.ReceiptGenerator.render(result));receipt.executeUpdate();}
                c.commit();return result;
            } catch(Exception e) {c.rollback();throw e;}
        } catch(SQLException e) {throw AuthService.databaseError(e);}
    }
    /** Split a requested quantity at FIFO price boundaries without consuming stock. */
    public List<Sale> quoteProduct(int productId,int userId,java.math.BigDecimal quantity){
        try(Connection c=DBConnection.connect()){
            c.setAutoCommit(false);var actor=AuthService.require(c,false);if(actor.id()!=userId)throw AuthService.failure("msg.access.denied");
            var settings=ShopSettings.read(c);Product product=BatchService.lockProduct(c,productId);long left=product.getUnit().toStored(product.getUnit().validate(quantity,false));
            List<Sale> result=new ArrayList<>();
            try(PreparedStatement p=c.prepareStatement("SELECT remaining_quantity,selling_price,currency_code FROM stock_batches WHERE product_id=? AND remaining_quantity>0 ORDER BY received_date,batch_id")){
                p.setInt(1,productId);try(ResultSet r=p.executeQuery()){while(left>0&&r.next()){
                    long take=Math.min(left,r.getLong(1));var price=r.getBigDecimal(2);if(price==null||!Objects.equals(r.getString(3),settings.currency().code()))price=product.getPrice();
                    if(!result.isEmpty()&&result.getLast().getPrice().compareTo(price)==0){var previous=result.removeLast();take+=product.getUnit().toStored(previous.getQuantitySold());}
                    Sale line=new Sale(0,productId,userId,product.getUnit().fromStored(take),java.time.LocalDate.now());line.setUnit(product.getUnit());line.setPrice(price);line.setProductName(product.getName());line.setFinancialSnapshot(settings.currency(),settings.taxRate());result.add(line);
                    // Only this batch's portion is removed from the outstanding request.
                    left-=Math.min(left,r.getLong(1));
                }}
            }
            if(left!=0)throw AuthService.failure("sale.stock.changed");c.commit();return result;
        }catch(SQLException ex){throw AuthService.databaseError(ex);}
    }
    public Checkout loadCheckout(String id) {
        try(Connection c=DBConnection.connect()) {return load(c,id,AuthService.require(c,false));}
        catch(SQLException e) {throw AuthService.databaseError(e);}
    }
    private Checkout load(Connection c,String id,AuthService.User actor) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT ch.*,u.username FROM checkouts ch JOIN users u ON ch.user_id=u.user_id WHERE checkout_id=?")) {
            ps.setString(1,id);try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next())throw AuthService.failure("checkout.legacy");
                if(rs.getInt("user_id")!=actor.id()&&!actor.isAdmin())throw AuthService.failure("msg.access.denied");
                List<Sale> lines=new ArrayList<>();
                try(PreparedStatement line=c.prepareStatement("SELECT s.*,u.username AS sold_by,ch.payment_method FROM sales s JOIN users u ON s.user_id=u.user_id LEFT JOIN checkouts ch ON s.checkout_id=ch.checkout_id WHERE s.checkout_id=? ORDER BY line_no,sale_id")) {
                    line.setString(1,id);try(ResultSet rows=line.executeQuery()) {while(rows.next())lines.add(read(rows));}
                }
                return new Checkout(id,rs.getString("username"),PaymentMethod.valueOf(rs.getString("payment_method")),Currencies.snapshot(rs.getString("currency_code"),rs.getInt("currency_minor")).round(rs.getBigDecimal("total")),rs.getTimestamp("created_at").toLocalDateTime(),List.copyOf(lines),Currencies.snapshot(rs.getString("currency_code"),rs.getInt("currency_minor")),rs.getBigDecimal("tax_rate"),Currencies.snapshot(rs.getString("currency_code"),rs.getInt("currency_minor")).round(rs.getBigDecimal("subtotal")),Currencies.snapshot(rs.getString("currency_code"),rs.getInt("currency_minor")).round(rs.getBigDecimal("tax_amount")));
            }
        }
    }
    public void voidSale(int id) {voidSale(id,gui.LocaleManager.get("void.default.reason"));}
    public void voidSale(int id,String reason) {
        if(reason==null || reason.isBlank() || reason.length()>500)throw AuthService.failure("void.reason.required");
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                var actor=AuthService.require(c,true);int productId;long quantity;String checkoutId;
                try(PreparedStatement ps=c.prepareStatement("SELECT product_id,quantity_sold,voided,checkout_id FROM sales WHERE sale_id=? FOR UPDATE")) {
                    ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()) {if(!rs.next()||rs.getBoolean("voided"))throw AuthService.failure("sale.already.void");productId=rs.getInt(1);quantity=rs.getLong(2);checkoutId=rs.getString(4);}
                }
                Product current=BatchService.lockProduct(c,productId);BatchService.restore(c,id,productId,current.getUnit(),quantity);
                try(PreparedStatement ps=c.prepareStatement("UPDATE products SET quantity=quantity+?,version=version+1 WHERE product_id=?")) {ps.setLong(1,quantity);ps.setInt(2,productId);if(ps.executeUpdate()!=1)throw AuthService.failure("error.stale");}
                try(PreparedStatement ps=c.prepareStatement("UPDATE sales SET voided=TRUE,voided_by=?,void_reason=? WHERE sale_id=?")) {ps.setInt(1,actor.id());ps.setString(2,reason.trim());ps.setInt(3,id);ps.executeUpdate();}
                AuditService.record(c,actor,"SALE_VOIDED","#"+id+" | "+quantity+" | "+reason.trim());c.commit();
            } catch(Exception e) {c.rollback();throw e;}
        } catch(SQLException e) {throw AuthService.databaseError(e);}
    }
    private static String fingerprint(List<Sale> items,PaymentMethod method) {
        StringBuilder text=new StringBuilder(method.name());
        for(Sale s:items)text.append('|').append(s.getProductId()).append(':').append(s.getQuantitySold().stripTrailingZeros().toPlainString()).append(':').append(s.getPrice().stripTrailingZeros().toPlainString()).append(':').append(s.getUnit().name());
        try {return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
