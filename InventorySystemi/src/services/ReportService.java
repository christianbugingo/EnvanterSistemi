package services;


import database.DBConnection;
import product.ReportEntry;
import product.Product;
import gui.LocaleManager;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

public class ReportService {
	

    public void generateSalesReport(ObservableList<ReportEntry> reportData, LocalDate start, LocalDate end, Label summaryLabel) {
        reportData.clear();
        String sql = "SELECT s.sale_date, " +
                     "SUM(s.quantity_sold) AS total_qty, " +
                     "SUM(s.quantity_sold * p.price) AS total_rev " +
                     "FROM sales s JOIN products p ON s.product_id = p.product_id " +
                     "WHERE s.sale_date BETWEEN ? AND ? " +
                     "GROUP BY DATE(s.sale_date) " +
                     "ORDER BY s.sale_date DESC";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(start));
            pstmt.setDate(2, Date.valueOf(end));
            ResultSet rs = pstmt.executeQuery();

            double totalRevenue = 0;
            long totalQty = 0;

            while (rs.next()) {
                LocalDate date = rs.getDate("sale_date").toLocalDate();
                int qty = rs.getInt("total_qty");
                double rev = rs.getDouble("total_rev");

                reportData.add(new ReportEntry(date, qty, rev));

                totalQty += qty;
                totalRevenue += rev;
            }

            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String periodText = String.format(
                LocaleManager.get("report.summary"),
                start.format(df),
                end.format(df),
                totalQty,
                NumberFormat.getCurrencyInstance(new Locale("tr", "TR")).format(totalRevenue)
            );
            summaryLabel.setText(periodText);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void showDailySummary() {
        String sql = "SELECT COALESCE(SUM(quantity_sold), 0) AS qty, " +
                     "COALESCE(SUM(quantity_sold * p.price), 0) AS rev " +
                     "FROM sales s " +
                     "JOIN products p ON s.product_id = p.product_id " +
                     "WHERE DATE(sale_date) = CURDATE()";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                int qty = rs.getInt("qty");
                double rev = rs.getDouble("rev");

                String formattedRev = NumberFormat.getCurrencyInstance(new Locale("tr", "TR")).format(rev);
                String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

                String message = LocaleManager.get("msg.daily.summary",
                        formattedDate, qty, formattedRev);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(LocaleManager.get("alert.information"));
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            } else {
                String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                String message = LocaleManager.get("msg.daily.summary",
                        formattedDate, 0, "₺0,00");

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(LocaleManager.get("alert.information"));
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(LocaleManager.get("alert.error"));
            alert.setHeaderText(null);
            alert.setContentText(LocaleManager.get("msg.daily.summary.failed", e.getMessage()));
            alert.showAndWait();
        }
    }

    public void showLowStockReport(ObservableList<Product> productsData) {
        Stage stage = new Stage();
        stage.setTitle(LocaleManager.get("menu.low.stock.report"));

        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        StringBuilder sb = new StringBuilder();
        sb.append(LocaleManager.get("report.low.stock.title")).append("\n\n");

        int count = 0;
        for (Product p : productsData) {
            if (p.getQuantity() < 10) {
                count++;
                sb.append("• ")
                  .append(p.getName())
                  .append(" (ID: ")
                  .append(p.getProductId())
                  .append(") - Stok: ")
                  .append(p.getQuantity())
                  .append(" adet - Fiyat: ")
                  .append(NumberFormat.getCurrencyInstance(new Locale("tr", "TR")).format(p.getPrice()))
                  .append(" - Tedarikçi: ")
                  .append(p.getSupplier())
                  .append("\n");
            }
        }

        if (count == 0) {
            sb.append(LocaleManager.get("msg.no.low.stock"));
        } else {
            String footer = String.format(LocaleManager.get("msg.low.stock.count"), count);
            sb.append("\n").append(footer);
        }

        ta.setText(sb.toString());

        ScrollPane scroll = new ScrollPane(ta);
        scroll.setFitToWidth(true);
        stage.setScene(new Scene(scroll, 700, 500));
        stage.show();
    }
}