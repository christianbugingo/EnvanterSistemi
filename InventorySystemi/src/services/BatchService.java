package services;

import database.DBConnection;
import product.*;
import java.math.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Stock quantities and their FIFO cost allocations always share the caller's transaction. */
public final class BatchService {
    public record Allocation(long batchId,long quantity,BigDecimal cost,BigDecimal unitCost,ShopCurrency currency,BigDecimal sellingPrice) { }
    public record Consumption(List<Allocation> allocations,BigDecimal cost,boolean complete) { }
    static void opening(Connection c,int id,Unit unit,long quantity) throws SQLException {
        if(quantity>0)insert(c,id,unit,quantity,null,LocalDate.now(),null,null,"new-"+id);
    }
    static void insert(Connection c,int id,Unit unit,long quantity,BigDecimal paid,LocalDate date,ShopCurrency currency,BigDecimal price,String key) throws SQLException {
        BigDecimal perUnit=paid==null?null:paid.multiply(unit==Unit.KG?new BigDecimal("1000"):BigDecimal.ONE).divide(BigDecimal.valueOf(quantity),12,RoundingMode.HALF_UP);
        try(PreparedStatement p=c.prepareStatement("INSERT INTO stock_batches(product_id,unit,quantity_received,remaining_quantity,total_cost,remaining_cost,cost_per_unit,received_date,currency_code,currency_minor,selling_price,opening_key) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")){
            p.setInt(1,id);p.setString(2,unit.name());p.setLong(3,quantity);p.setLong(4,quantity);p.setBigDecimal(5,paid);p.setBigDecimal(6,paid);p.setBigDecimal(7,perUnit);p.setDate(8,java.sql.Date.valueOf(date));p.setString(9,currency==null?null:currency.code());p.setInt(10,currency==null?2:currency.minorUnits());p.setBigDecimal(11,price);p.setString(12,key);p.executeUpdate();
        }
    }
    public void receive(int productId,BigDecimal quantity,BigDecimal totalPaid,LocalDate date,BigDecimal sellingPrice){
        if(date==null||date.isAfter(LocalDate.now()))throw AuthService.failure("batch.date.invalid");
        try(Connection c=DBConnection.connect()){
            c.setAutoCommit(false);try{
                var actor=AuthService.require(c,true);var currency=ShopSettings.read(c).currency();Product product=lockProduct(c,productId);if(currency==null)throw AuthService.failure("currency.required");
                long qty=product.getUnit().toStored(product.getUnit().validate(quantity,false));validateMoney(totalPaid,currency);
                if(sellingPrice!=null)validateMoney(sellingPrice,currency);
                product.getUnit().validate(product.getQuantity().add(quantity),true);
                insert(c,productId,product.getUnit(),qty,currency.round(totalPaid),date,currency,sellingPrice,null);
                try(PreparedStatement p=c.prepareStatement("UPDATE products SET quantity=quantity+?,version=version+1 WHERE product_id=?")){p.setLong(1,qty);p.setInt(2,productId);p.executeUpdate();}
                AuditService.record(c,actor,"STOCK_RECEIVED",product.getName()+" | "+qty+" | "+currency.format(totalPaid));c.commit();
            }catch(Exception ex){c.rollback();throw ex;}
        }catch(SQLException ex){throw AuthService.databaseError(ex);}
    }
    private static void validateMoney(BigDecimal value,ShopCurrency currency){if(value==null||value.signum()<0||value.compareTo(new BigDecimal("999999999999.9999"))>0||value.stripTrailingZeros().scale()>currency.minorUnits())throw AuthService.failure("msg.invalid.price");}
    static Product lockProduct(Connection c,int id) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT * FROM products WHERE product_id=? FOR UPDATE")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw AuthService.failure("error.stale");return ProductService.read(r);}}
    }
    static Consumption consume(Connection c,int productId,long requested,ShopCurrency saleCurrency) throws SQLException {
        long left=requested;BigDecimal total=BigDecimal.ZERO;boolean complete=true;List<Allocation> rows=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement("SELECT * FROM stock_batches WHERE product_id=? AND remaining_quantity>0 ORDER BY received_date,batch_id FOR UPDATE")){
            p.setInt(1,productId);try(ResultSet r=p.executeQuery()){
                while(left>0&&r.next()){
                    long remaining=r.getLong("remaining_quantity"),take=Math.min(left,remaining);BigDecimal cost=r.getBigDecimal("remaining_cost");
                    var currency=Currencies.snapshot(r.getString("currency_code"),r.getInt("currency_minor"));
                    BigDecimal used=cost==null?null:take==remaining?cost:cost.multiply(BigDecimal.valueOf(take)).divide(BigDecimal.valueOf(remaining),currency.minorUnits(),RoundingMode.HALF_UP);
                    try(PreparedStatement update=c.prepareStatement("UPDATE stock_batches SET remaining_quantity=remaining_quantity-?,remaining_cost=? WHERE batch_id=?")){update.setLong(1,take);update.setBigDecimal(2,cost==null?null:cost.subtract(used));update.setLong(3,r.getLong("batch_id"));update.executeUpdate();}
                    rows.add(new Allocation(r.getLong("batch_id"),take,used,r.getBigDecimal("cost_per_unit"),currency,r.getBigDecimal("selling_price")));
                    if(used==null||saleCurrency==null||!currency.code().equals(saleCurrency.code()))complete=false;else total=total.add(used);
                    left-=take;
                }
            }
        }
        if(left!=0)throw AuthService.failure("batch.stock.inconsistent");
        return new Consumption(List.copyOf(rows),complete?saleCurrency.round(total):null,complete);
    }
    static void allocate(Connection c,int saleId,Consumption used) throws SQLException {
        for(var a:used.allocations())try(PreparedStatement p=c.prepareStatement("INSERT INTO sale_batch_allocations(sale_id,batch_id,quantity_used,cost_amount,unit_cost_snapshot,currency_code,currency_minor) VALUES(?,?,?,?,?,?,?)")){
            p.setInt(1,saleId);p.setLong(2,a.batchId());p.setLong(3,a.quantity());p.setBigDecimal(4,a.cost());p.setBigDecimal(5,a.unitCost());p.setString(6,a.currency().code());p.setInt(7,a.currency().minorUnits());p.executeUpdate();
        }
    }
    static void restore(Connection c,int saleId,int productId,Unit unit,long quantity) throws SQLException {
        boolean found=false;
        try(PreparedStatement p=c.prepareStatement("SELECT * FROM sale_batch_allocations WHERE sale_id=? ORDER BY batch_id")){
            p.setInt(1,saleId);try(ResultSet r=p.executeQuery()){while(r.next()){
                found=true;try(PreparedStatement u=c.prepareStatement("UPDATE stock_batches SET remaining_quantity=remaining_quantity+?,remaining_cost=CASE WHEN remaining_cost IS NULL THEN NULL ELSE remaining_cost+? END WHERE batch_id=?")){u.setLong(1,r.getLong("quantity_used"));u.setBigDecimal(2,r.getBigDecimal("cost_amount"));u.setLong(3,r.getLong("batch_id"));u.executeUpdate();}
            }}
        }
        if(!found)insert(c,productId,unit,quantity,null,LocalDate.now(),null,null,null);
    }
    public List<StockBatch> load(int productId){
        try(Connection c=DBConnection.connect()){
            AuthService.require(c,true);List<StockBatch> result=new ArrayList<>();
            try(PreparedStatement p=c.prepareStatement("SELECT * FROM stock_batches WHERE product_id=? ORDER BY received_date,batch_id")){p.setInt(1,productId);try(ResultSet r=p.executeQuery()){while(r.next())result.add(new StockBatch(r.getLong("batch_id"),productId,Unit.valueOf(r.getString("unit")),r.getLong("quantity_received"),r.getLong("remaining_quantity"),r.getBigDecimal("total_cost"),r.getBigDecimal("remaining_cost"),r.getBigDecimal("cost_per_unit"),r.getDate("received_date").toLocalDate(),Currencies.snapshot(r.getString("currency_code"),r.getInt("currency_minor")),r.getBigDecimal("selling_price")));}}return result;
        }catch(SQLException ex){throw AuthService.databaseError(ex);}
    }
}
