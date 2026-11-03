package services;

import database.DBConnection;
import product.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.*;
import java.util.*;

/** Quantities are grouped by unit; revenue uses immutable price snapshots and line rounding. */
public class ReportService {
    public List<ReportEntry> load(LocalDate start, LocalDate end) {
        if(start==null || end==null || start.isAfter(end)) throw AuthService.failure("msg.start.after.end");
        try(Connection c=DBConnection.connect()) {
            AuthService.require(c,true);
            List<ReportEntry> rows=new ArrayList<>();
            try(PreparedStatement ps=c.prepareStatement("SELECT s.sale_date,s.unit,SUM(s.quantity_sold) AS qty,SUM(ROUND(s.quantity_sold*s.unit_price,2)) AS revenue FROM sales s WHERE s.voided=FALSE AND s.sale_date BETWEEN ? AND ? GROUP BY s.sale_date,s.unit ORDER BY s.sale_date DESC,s.unit")) {
                ps.setDate(1,java.sql.Date.valueOf(start)); ps.setDate(2,java.sql.Date.valueOf(end));
                try(ResultSet rs=ps.executeQuery()) {
                    while(rs.next()) rows.add(new ReportEntry(rs.getDate(1).toLocalDate(),rs.getBigDecimal(3),rs.getBigDecimal(4),Unit.valueOf(rs.getString(2))));
                }
            }
            return rows;
        } catch(SQLException e) { throw AuthService.databaseError(e); }
    }
    public BigDecimal revenue(List<ReportEntry> rows) { return rows.stream().map(ReportEntry::getTotalRevenue).reduce(BigDecimal.ZERO,BigDecimal::add); }
}
