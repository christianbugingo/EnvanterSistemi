package services;

import database.DBConnection;
import product.*;
import javafx.collections.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class SaleService {
    public ObservableList<Sale> loadSales() {
        try(Connection c=DBConnection.connect()) {
            AuthService.User actor=AuthService.require(c,false);
            ObservableList<Sale> rows=FXCollections.observableArrayList();
            String sql="SELECT s.*, u.username AS sold_by FROM sales s JOIN users u ON s.user_id=u.user_id"+(actor.isAdmin()?"":" WHERE s.user_id=?")+" ORDER BY s.sale_id DESC LIMIT 1000";
            try(PreparedStatement ps=c.prepareStatement(sql)) {
                if(!actor.isAdmin()) ps.setInt(1,actor.id());
                try(ResultSet rs=ps.executeQuery()) {
                    while(rs.next()) {
                        Sale sale=new Sale(rs.getInt("sale_id"),rs.getInt("product_id"),rs.getInt("user_id"),rs.getBigDecimal("quantity_sold"),rs.getDate("sale_date").toLocalDate());
                        sale.setProductName(rs.getString("product_name")); sale.setPrice(rs.getBigDecimal("unit_price")); sale.setUnit(Unit.valueOf(rs.getString("unit"))); sale.setSoldBy(rs.getString("sold_by")); sale.setVoided(rs.getBoolean("voided")); rows.add(sale);
                    }
                }
            }
            return rows;
        } catch(SQLException e) { throw AuthService.databaseError(e); }
    }
    public void addMultipleSales(List<Sale> items) {
        if(items==null || items.isEmpty() || items.stream().anyMatch(Objects::isNull)) throw AuthService.failure("sale.empty");
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                AuthService.User actor=AuthService.require(c,false);
                // Consistent product lock order prevents carts taking locks in opposite order.
                for(Sale sale:items.stream().sorted(Comparator.comparingInt(Sale::getProductId)).toList()) {
                    if(sale.getUserId()!=actor.id()) throw AuthService.failure("msg.access.denied");
                    Product product;
                    try(PreparedStatement ps=c.prepareStatement("SELECT * FROM products WHERE product_id=? FOR UPDATE")) {
                        ps.setInt(1,sale.getProductId());
                        try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw AuthService.failure("error.stale"); product=ProductService.read(rs); }
                    }
                    BigDecimal quantity=product.getUnit().validate(sale.getQuantitySold(),false);
                    // Never silently sell at a changed price or unit from a stale terminal/cart.
                    if(sale.getPrice().compareTo(product.getPrice())!=0 || sale.getUnit()!=product.getUnit()) throw AuthService.failure("sale.price.changed");
                    try(PreparedStatement ps=c.prepareStatement("UPDATE products SET quantity=quantity-?, version=version+1 WHERE product_id=? AND quantity>=?")) {
                        ps.setBigDecimal(1,quantity); ps.setInt(2,product.getProductId()); ps.setBigDecimal(3,quantity);
                        if(ps.executeUpdate()!=1) throw AuthService.failure("sale.stock.changed");
                    }
                    try(PreparedStatement ps=c.prepareStatement("INSERT INTO sales(product_id,user_id,quantity_sold,sale_date,unit_price,unit,product_name) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1,product.getProductId()); ps.setInt(2,actor.id()); ps.setBigDecimal(3,quantity); ps.setDate(4,java.sql.Date.valueOf(LocalDate.now())); ps.setBigDecimal(5,product.getPrice()); ps.setString(6,product.getUnit().name()); ps.setString(7,product.getName()); ps.executeUpdate();
                        try(ResultSet rs=ps.getGeneratedKeys()) { rs.next(); sale.setSaleId(rs.getInt(1)); }
                    }
                    sale.setProductName(product.getName()); sale.setSoldBy(actor.username());
                    AuditService.record(c,actor,"SALE_CREATED","#"+sale.getSaleId()+" | "+product.getName()+" | "+quantity+" "+product.getUnit().name()+" | "+sale.getTotal());
                }
                c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw AuthService.databaseError(e); }
    }
    /** Keep the sale and its audit trail; restore stock exactly once from the stored record. */
    public void voidSale(int id) {
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                AuthService.User actor=AuthService.require(c,true);
                int productId; BigDecimal quantity;
                try(PreparedStatement ps=c.prepareStatement("SELECT product_id,quantity_sold,voided FROM sales WHERE sale_id=? FOR UPDATE")) {
                    ps.setInt(1,id);
                    try(ResultSet rs=ps.executeQuery()) {
                        if(!rs.next() || rs.getBoolean("voided")) throw AuthService.failure("sale.already.void");
                        productId=rs.getInt(1); quantity=rs.getBigDecimal(2);
                    }
                }
                try(PreparedStatement ps=c.prepareStatement("UPDATE products SET quantity=quantity+?, version=version+1 WHERE product_id=?")) { ps.setBigDecimal(1,quantity); ps.setInt(2,productId); if(ps.executeUpdate()!=1) throw AuthService.failure("error.stale"); }
                try(PreparedStatement ps=c.prepareStatement("UPDATE sales SET voided=TRUE WHERE sale_id=?")) { ps.setInt(1,id); ps.executeUpdate(); }
                AuditService.record(c,actor,"SALE_VOIDED","#"+id+" | "+quantity); c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw AuthService.databaseError(e); }
    }
}
