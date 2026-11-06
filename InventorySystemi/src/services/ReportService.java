package services;
import database.DBConnection;
import product.*;
import java.math.*;
import java.time.LocalDate;
import java.sql.*;
import java.util.*;
/** Tax is calculated once per checkout. For unit-separated rows the final unit receives rounding residue. */
public class ReportService {
    public List<ReportEntry> load(LocalDate start,LocalDate end){
        if(start==null||end==null||start.isAfter(end))throw AuthService.failure("msg.start.after.end");
        try(Connection c=DBConnection.connect()){
            AuthService.require(c,true);Map<String,List<ReportEntry>> checkouts=new LinkedHashMap<>();
            try(PreparedStatement p=c.prepareStatement("SELECT s.*,ch.payment_method FROM sales s LEFT JOIN checkouts ch ON s.checkout_id=ch.checkout_id WHERE s.voided=FALSE AND s.sale_date BETWEEN ? AND ? ORDER BY s.sale_date DESC,s.checkout_id,s.unit,s.sale_id")){
                p.setDate(1,java.sql.Date.valueOf(start));p.setDate(2,java.sql.Date.valueOf(end));try(ResultSet r=p.executeQuery()){
                    while(r.next()){
                        String id=r.getString("checkout_id");if(id==null)id="legacy-"+r.getInt("sale_id");
                        String method=r.getString("payment_method");
                        var row=new ReportEntry(r.getDate("sale_date").toLocalDate(),r.getLong("quantity_sold"),r.getBigDecimal("net_amount"),BigDecimal.ZERO,Unit.valueOf(r.getString("unit")),method==null?null:PaymentMethod.valueOf(method),Currencies.snapshot(r.getString("currency_code"),r.getInt("currency_minor")),r.getBigDecimal("tax_rate"));
                        checkouts.computeIfAbsent(id,k->new ArrayList<>()).add(row);
                    }
                }
            }
            Map<String,ReportEntry> grouped=new LinkedHashMap<>();
            for(var lines:checkouts.values()){
                var first=lines.getFirst();BigDecimal subtotal=lines.stream().map(ReportEntry::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add);
                BigDecimal tax=Amounts.calculate(subtotal,first.taxRate(),first.currency()).tax(),remaining=tax;
                Map<Unit,List<ReportEntry>> units=new TreeMap<>();for(var line:lines)units.computeIfAbsent(line.unit(),k->new ArrayList<>()).add(line);
                int index=0;
                for(var entry:units.entrySet()){
                    BigDecimal net=entry.getValue().stream().map(ReportEntry::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add);
                    BigDecimal allocated=++index==units.size()?remaining:subtotal.signum()==0?BigDecimal.ZERO:tax.multiply(net).divide(subtotal,first.currency().minorUnits(),RoundingMode.HALF_UP);remaining=remaining.subtract(allocated);
                    long qty=entry.getValue().stream().mapToLong(ReportEntry::storedQuantity).sum();
                    String key=first.date()+"|"+entry.getKey()+"|"+first.method()+"|"+first.currency().code()+"|"+first.taxRate().stripTrailingZeros();
                    var old=grouped.get(key);
                    grouped.put(key,new ReportEntry(first.date(),qty+(old==null?0:old.storedQuantity()),net.add(old==null?BigDecimal.ZERO:old.subtotal()),allocated.add(old==null?BigDecimal.ZERO:old.tax()),entry.getKey(),first.method(),first.currency(),first.taxRate()));
                }
            }
            return List.copyOf(grouped.values());
        }catch(SQLException ex){throw AuthService.databaseError(ex);}
    }
    /** Deliberately refuses to add different currencies. */
    public BigDecimal revenue(List<ReportEntry> rows){if(rows.stream().map(r->r.currency().code()).distinct().count()>1)throw AuthService.failure("currency.mixed");return rows.stream().map(ReportEntry::getTotalRevenue).reduce(BigDecimal.ZERO,BigDecimal::add);}
    public String summary(List<ReportEntry> rows){Map<String,BigDecimal> values=new TreeMap<>();Map<String,ShopCurrency> currencies=new HashMap<>();for(var r:rows){values.merge(r.currency().code(),r.getTotalRevenue(),BigDecimal::add);currencies.put(r.currency().code(),r.currency());}return values.entrySet().stream().map(e->currencies.get(e.getKey()).format(e.getValue())).collect(java.util.stream.Collectors.joining(" · "));}
}
