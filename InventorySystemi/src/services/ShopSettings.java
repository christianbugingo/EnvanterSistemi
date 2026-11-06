package services;
import database.DBConnection;
import product.ShopCurrency;
import java.sql.*;
import java.math.*;
public final class ShopSettings {
    public record Values(ShopCurrency currency,BigDecimal taxRate,String language) { }
    public static String get(Connection c,String key,String fallback) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT setting_value FROM app_settings WHERE setting_key=? FOR UPDATE")){p.setString(1,key);try(ResultSet r=p.executeQuery()){return r.next()?r.getString(1):fallback;}}
    }
    public static void put(Connection c,String key,String value) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("MERGE INTO app_settings KEY(setting_key) VALUES(?,?)")){p.setString(1,key);p.setString(2,value);p.executeUpdate();}
    }
    public static Values read(Connection c) throws SQLException {
        String code=get(c,"currency","");return new Values(code.isBlank()?null:Currencies.get(code),new BigDecimal(get(c,"tax_rate","0")),get(c,"language","en"));
    }
    public static Values current(){try(Connection c=DBConnection.connect()){return read(c);}catch(SQLException ex){throw AuthService.databaseError(ex);}}
    public static ShopCurrency currency(){ShopCurrency c=current().currency();if(c==null)throw AuthService.failure("currency.required");return c;}
    public static void validateTax(BigDecimal rate){if(rate==null||rate.signum()<0||rate.compareTo(new BigDecimal("100"))>0||rate.stripTrailingZeros().scale()>4)throw AuthService.failure("tax.invalid");}
    public void save(String code,BigDecimal tax,String language){
        ShopCurrency next=Currencies.get(code);if(next.minorUnits()<0)throw AuthService.failure("currency.invalid");validateTax(tax);if(!java.util.Set.of("en","tr").contains(language))throw AuthService.failure("language.invalid");
        try(Connection c=DBConnection.connect()){
            c.setAutoCommit(false);try{
                var actor=AuthService.require(c,true);Values previous=read(c);
                if(previous.currency()==null||!previous.currency().code().equals(next.code()))try(PreparedStatement p=c.prepareStatement("UPDATE products SET price=ROUND(price,?),version=version+1")){p.setInt(1,next.minorUnits());p.executeUpdate();}
                put(c,"currency",code);put(c,"tax_rate",tax.toPlainString());put(c,"language",language);
                AuditService.record(c,actor,"SETTINGS_CHANGED",code+" | "+tax+"%");c.commit();
            }catch(Exception ex){c.rollback();throw ex;}
        }catch(SQLException ex){throw AuthService.databaseError(ex);}
    }
}
