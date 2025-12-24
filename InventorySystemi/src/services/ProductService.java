package services;

import database.DBConnection;
import product.Product;
import gui.LocaleManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProductService {

    private final ObservableList<Product> productsData = FXCollections.observableArrayList();

    public ObservableList<Product> loadProducts() {
        productsData.clear();
        String sql = "SELECT * FROM products ORDER BY product_id";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                productsData.add(new Product(
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),
                    rs.getString("supplier")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productsData;
    }

    public void addProduct(Product product) {
        String sql = "INSERT INTO products (name, category, quantity, price, supplier) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getCategory());
            pstmt.setInt(3, product.getQuantity());
            pstmt.setDouble(4, product.getPrice());
            pstmt.setString(5, product.getSupplier());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateProduct(Product product) {
        String sql = "UPDATE products SET name = ?, category = ?, quantity = ?, price = ?, supplier = ? WHERE product_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getCategory());
            pstmt.setInt(3, product.getQuantity());
            pstmt.setDouble(4, product.getPrice());
            pstmt.setString(5, product.getSupplier());
            pstmt.setInt(6, product.getProductId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasSalesRecords(int productId) {
        String sql = "SELECT COUNT(*) FROM sales WHERE product_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void checkLowStock(ObservableList<Product> productsData, Runnable onLowStockFound) {
        List<Product> lowStockProducts = productsData.stream()
                .filter(p -> p.getQuantity() < 10)
                .sorted(Comparator.comparingInt(Product::getQuantity))
                .collect(Collectors.toList());

        if (!lowStockProducts.isEmpty()) {
            onLowStockFound.run();
        }
    }

    public ObservableList<Product> getProductsData() {
        return productsData;
    }
}