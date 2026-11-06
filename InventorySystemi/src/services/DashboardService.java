package services;

import database.DBConnection;
import product.*;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Every dashboard and profit query enforces the current user's scope in SQL. */
public final class DashboardService {
    public record Metrics(ShopCurrency currency,BigDecimal revenue,long transactions,long pieces,long grams,long lowStock,Map<String,BigDecimal> byPayment,Map<LocalDate,BigDecimal> byDay){ }
    public record UnknownCost(int saleId,String product,String quantity,BigDecimal revenue,String seller,LocalDate date) { }
    public record Profit(ShopCurrency currency,BigDecimal revenue,BigDecimal knownRevenue,BigDecimal cost,BigDecimal grossProfit,BigDecimal margin,List<UnknownCost> unknown){
        public boolean incomplete(){return !unknown.isEmpty();}
        public BigDecimal unknownRevenue(){return revenue.subtract(knownRevenue);}
    }
    public List<ShopCurrency> currencies(){
        try(Connection c=DBConnection.connect()){
            var actor=AuthService.require(c,false);Map<String,ShopCurrency> values=new TreeMap<>();var current=ShopSettings.read(c).currency();if(current!=null)values.put(current.code(),current);
            try(PreparedStatement p=c.prepareStatement("SELECT DISTINCT currency_code,currency_minor FROM sales WHERE voided=FALSE"+(actor.isAdmin()?"":" AND user_id=?"))){if(!actor.isAdmin())p.setInt(1,actor.id());try(ResultSet r=p.executeQuery()){while(r.next()){var value=Currencies.snapshot(r.getString(1),r.getInt(2));values.putIfAbsent(value.code(),value);}}}return List.copyOf(values.values());
        }catch(SQLException ex){throw AuthService.databaseError(ex);}
    }
    public boolean canViewProfit(){try(Connection c=DBConnection.connect()){var user=AuthService.require(c,false);return user.isAdmin()||Boolean.parseBoolean(ShopSettings.get(c,"staff_profit_visible","false"));}catch(SQLException ex){throw AuthService.databaseError(ex);}}
    private String scope(AuthService.User user){return user.isAdmin()?"":" AND s.user_id=?";}
    private void bind(PreparedStatement p,LocalDate start,LocalDate end,String code,AuthService.User user) throws SQLException {
        p.setDate(1,java.sql.Date.valueOf(start));p.setDate(2,java.sql.Date.valueOf(end));p.setString(3,code);if(!user.isAdmin())p.setInt(4,user.id());
    }
    public Metrics metrics(LocalDate today,ShopCurrency currency){
        try(Connection c=DBConnection.connect()){
            var user=AuthService.require(c,false);Map<String,BigDecimal> payments=new LinkedHashMap<>();Map<LocalDate,BigDecimal> days=new TreeMap<>();for(int n=13;n>=0;n--)days.put(today.minusDays(n),BigDecimal.ZERO);
            BigDecimal revenue=BigDecimal.ZERO;long pieces=0,grams=0;Set<String> checkouts=new HashSet<>();
            try(PreparedStatement p=c.prepareStatement("SELECT s.*,ch.payment_method FROM sales s LEFT JOIN checkouts ch ON s.checkout_id=ch.checkout_id WHERE s.voided=FALSE AND s.sale_date BETWEEN ? AND ? AND COALESCE(s.currency_code,'?')=?"+scope(user))){
                bind(p,today.minusDays(13),today,currency.code(),user);try(ResultSet r=p.executeQuery()){while(r.next()){
                    LocalDate date=r.getDate("sale_date").toLocalDate();BigDecimal value=r.getBigDecimal("net_amount");days.merge(date,value,BigDecimal::add);
                    if(date.equals(today)){
                        revenue=revenue.add(value);if("KG".equals(r.getString("unit")))grams+=r.getLong("quantity_sold");else pieces+=r.getLong("quantity_sold");
                        String id=r.getString("checkout_id");checkouts.add(id==null?"legacy-"+r.getInt("sale_id"):id);String method=r.getString("payment_method");payments.merge(method==null?"LEGACY":method,value,BigDecimal::add);
                    }
                }}
            }
            long low;try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM products WHERE quantity<low_stock")){r.next();low=r.getLong(1);}
            return new Metrics(currency,currency.round(revenue),checkouts.size(),pieces,grams,low,Collections.unmodifiableMap(payments),Collections.unmodifiableMap(days));
        }catch(SQLException ex){throw AuthService.databaseError(ex);}
    }
    public Profit profit(ProfitPeriod period,LocalDate today,LocalDate start,LocalDate end,ShopCurrency currency){
        var range=period.range(today,start,end);
        try(Connection c=DBConnection.connect()){
            var user=AuthService.require(c,false);if(!user.isAdmin()&&!Boolean.parseBoolean(ShopSettings.get(c,"staff_profit_visible","false")))throw AuthService.failure("msg.access.denied");
            BigDecimal revenue=BigDecimal.ZERO,known=BigDecimal.ZERO,cost=BigDecimal.ZERO;List<UnknownCost> unknown=new ArrayList<>();
            try(PreparedStatement p=c.prepareStatement("SELECT s.*,u.username FROM sales s JOIN users u ON s.user_id=u.user_id WHERE s.voided=FALSE AND s.sale_date BETWEEN ? AND ? AND COALESCE(s.currency_code,'?')=?"+scope(user)+" ORDER BY s.sale_date DESC,s.sale_id DESC")){
                bind(p,range.start(),range.end(),currency.code(),user);try(ResultSet r=p.executeQuery()){while(r.next()){
                    var value=r.getBigDecimal("net_amount");revenue=revenue.add(value);var purchaseCost=r.getBigDecimal("cost_of_goods");
                    if(r.getBoolean("cost_complete")&&purchaseCost!=null){known=known.add(value);cost=cost.add(purchaseCost);}
                    else{Unit unit=Unit.valueOf(r.getString("unit"));unknown.add(new UnknownCost(r.getInt("sale_id"),r.getString("product_name"),Quantities.format(unit.fromStored(r.getLong("quantity_sold")),unit),currency.round(value),r.getString("username"),r.getDate("sale_date").toLocalDate()));}
                }}
            }
            BigDecimal gross=known.subtract(cost);BigDecimal margin=known.signum()==0?null:gross.multiply(new BigDecimal("100")).divide(known,2,RoundingMode.HALF_UP);
            return new Profit(currency,currency.round(revenue),currency.round(known),currency.round(cost),currency.round(gross),margin,List.copyOf(unknown));
        }catch(SQLException ex){throw AuthService.databaseError(ex);}
    }
}
