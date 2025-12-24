package services;

import database.DBConnection;
import product.Sale;
import gui.LocaleManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class SaleService {

    private final ObservableList<Sale> salesData = FXCollections.observableArrayList();

    public ObservableList<Sale> loadSales() {
        salesData.clear();
        String sql = "SELECT s.*, p.name as product_name, u.username as sold_by " +
                     "FROM sales s JOIN products p ON s.product_id = p.product_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "ORDER BY s.sale_date DESC, s.sale_id DESC";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Sale sale = new Sale(
                    rs.getInt("sale_id"),
                    rs.getInt("product_id"),
                    rs.getInt("user_id"),
                    rs.getInt("quantity_sold"),
                    rs.getDate("sale_date").toLocalDate()
                );
                sale.setProductName(rs.getString("product_name"));
                sale.setSoldBy(rs.getString("sold_by"));
                salesData.add(sale);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return salesData;
    }

    public void addMultipleSales(List<Sale> salesList) {
        String insertSaleSQL = "INSERT INTO sales (product_id, user_id, quantity_sold, sale_date) VALUES (?, ?, ?, ?)";
        String updateProductSQL = "UPDATE products SET quantity = quantity - ? WHERE product_id = ?";

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                for (Sale sale : salesList) {
                    // Insert sale
                    try (PreparedStatement ps1 = conn.prepareStatement(insertSaleSQL)) {
                        ps1.setInt(1, sale.getProductId());
                        ps1.setInt(2, sale.getUserId());
                        ps1.setInt(3, sale.getQuantitySold());
                        ps1.setDate(4, Date.valueOf(sale.getSaleDate()));
                        ps1.executeUpdate();
                    }

                    // Update stock
                    try (PreparedStatement ps2 = conn.prepareStatement(updateProductSQL)) {
                        ps2.setInt(1, sale.getQuantitySold());
                        ps2.setInt(2, sale.getProductId());
                        ps2.executeUpdate();
                    }
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteSale(int saleId, int productId, int quantitySold) {
        String deleteSaleSQL = "DELETE FROM sales WHERE sale_id = ?";
        String restoreStockSQL = "UPDATE products SET quantity = quantity + ? WHERE product_id = ?";

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(restoreStockSQL);
                 PreparedStatement ps2 = conn.prepareStatement(deleteSaleSQL)) {

                ps1.setInt(1, quantitySold);
                ps1.setInt(2, productId);
                ps1.executeUpdate();

                ps2.setInt(1, saleId);
                ps2.executeUpdate();

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ObservableList<Sale> getSalesData() {
        return salesData;
    }
}