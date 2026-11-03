package services;

import database.DBConnection;
import product.*;
import javafx.collections.*;
import java.math.BigDecimal;
import java.sql.*;

public class ProductService {
    public ObservableList<Product> loadProducts() {
        try(Connection c=DBConnection.connect()) {
            AuthService.require(c,false);
            ObservableList<Product> rows=FXCollections.observableArrayList();
            try(Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT * FROM products ORDER BY product_id")) { while(rs.next()) rows.add(read(rs)); }
            return rows;
        } catch(SQLException e) { throw AuthService.databaseError(e); }
    }
    static Product read(ResultSet rs) throws SQLException {
        Product p=new Product(rs.getInt("product_id"),rs.getString("name"),rs.getString("category"),rs.getBigDecimal("quantity"),rs.getBigDecimal("price"),rs.getString("supplier"),Unit.valueOf(rs.getString("unit")),rs.getBigDecimal("low_stock"),rs.getInt("version"));
        p.setBarcode(rs.getString("barcode")); return p;
    }
    public void addProduct(Product p) { save(p,false); }
    public void updateProduct(Product p) { save(p,true); }
    private void save(Product p, boolean update) {
        validate(p);
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                AuthService.User actor=AuthService.require(c,true);
                Product before=null;
                if(update) {
                    try(PreparedStatement ps=c.prepareStatement("SELECT * FROM products WHERE product_id=? FOR UPDATE")) {
                        ps.setInt(1,p.getProductId());
                        try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw AuthService.failure("error.stale"); before=read(rs); }
                    }
                    if(before.getVersion()!=p.getVersion()) throw AuthService.failure("error.stale");
                    if(before.getQuantity().compareTo(p.getQuantity())!=0) throw AuthService.failure("stock.use.adjustment");
                    if(before.getUnit()!=p.getUnit() && hasSalesRecords(c,p.getProductId())) throw AuthService.failure("unit.locked");
                }
                String sql=update ? "UPDATE products SET name=?, category=?, quantity=?, price=?, supplier=?, unit=?, low_stock=?, barcode=?, version=version+1 WHERE product_id=? AND version=?" : "INSERT INTO products(name,category,quantity,price,supplier,unit,low_stock,barcode) VALUES(?,?,?,?,?,?,?,?)";
                try(PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1,p.getName().trim()); ps.setString(2,p.getCategory().trim()); ps.setBigDecimal(3,p.getQuantity()); ps.setBigDecimal(4,p.getPrice()); ps.setString(5,p.getSupplier().trim()); ps.setString(6,p.getUnit().name()); ps.setBigDecimal(7,p.getLowStock());
                    ps.setString(8,p.getBarcode().isEmpty()?null:p.getBarcode());
                    if(update) { ps.setInt(9,p.getProductId()); ps.setInt(10,p.getVersion()); }
                    if(ps.executeUpdate()!=1) throw AuthService.failure("error.stale");
                }
                AuditService.record(c,actor,update?"PRODUCT_UPDATED":"PRODUCT_CREATED",p.getName()+" | "+(before==null?"":before.getQuantity()+" → ")+p.getQuantity()+" "+p.getUnit().name()+" | "+p.getPrice());
                c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        } catch(SQLException e) {
            if("23505".equals(e.getSQLState())) throw AuthService.failure("barcode.duplicate");
            throw AuthService.databaseError(e);
        }
    }
    public void deleteProduct(int id) {
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                AuthService.User actor=AuthService.require(c,true);
                if(hasSalesRecords(c,id)) throw AuthService.failure("msg.product.has.sales");
                try(PreparedStatement ps=c.prepareStatement("DELETE FROM products WHERE product_id=?")) { ps.setInt(1,id); if(ps.executeUpdate()!=1) throw AuthService.failure("error.stale"); }
                AuditService.record(c,actor,"PRODUCT_DELETED","#"+id); c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw AuthService.databaseError(e); }
    }
    public void adjustStock(int id,BigDecimal delta,String reason) {
        if(delta==null || delta.signum()==0 || reason==null || reason.isBlank() || reason.length()>500) throw AuthService.failure("stock.reason.required");
        try(Connection c=DBConnection.connect()) {
            c.setAutoCommit(false);
            try {
                AuthService.User actor=AuthService.require(c,true); Product before;
                try(PreparedStatement ps=c.prepareStatement("SELECT * FROM products WHERE product_id=? FOR UPDATE")) {
                    ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()) {if(!rs.next())throw AuthService.failure("error.stale");before=read(rs);}
                }
                before.getUnit().validate(delta.abs(),false);
                BigDecimal after=before.getUnit().validate(before.getQuantity().add(delta),true);
                try(PreparedStatement ps=c.prepareStatement("UPDATE products SET quantity=?,version=version+1 WHERE product_id=?")) {ps.setBigDecimal(1,after);ps.setInt(2,id);ps.executeUpdate();}
                AuditService.record(c,actor,"STOCK_ADJUSTED","#"+id+" | "+before.getQuantity()+" → "+after+" | "+reason.trim()); c.commit();
            } catch(Exception e) {c.rollback();throw e;}
        } catch(SQLException e) {throw AuthService.databaseError(e);}
    }
    private boolean hasSalesRecords(Connection c, int id) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT COUNT(*) FROM sales WHERE product_id=?")) {
            ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()) { rs.next(); return rs.getInt(1)>0; }
        }
    }
    private void validate(Product p) {
        if(p==null || p.getUnit()==null) throw AuthService.failure("validation.product");
        for(String text:new String[]{p.getName(),p.getCategory(),p.getSupplier()})
            if(text==null || text.isBlank() || text.length()>255) throw AuthService.failure("validation.product");
        if(!p.getBarcode().isEmpty() && !p.getBarcode().matches("[A-Za-z0-9._-]{1,64}")) throw AuthService.failure("barcode.invalid");
        p.getUnit().validate(p.getQuantity(),true); p.getUnit().validate(p.getLowStock(),true);
        if(p.getPrice()==null || p.getPrice().signum()<0 || p.getPrice().compareTo(new BigDecimal("9999999999.99"))>0 || p.getPrice().stripTrailingZeros().scale()>2) throw AuthService.failure("msg.invalid.price");
    }
}
