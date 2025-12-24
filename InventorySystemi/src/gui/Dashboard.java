package gui;

import product.Product;
import product.ReportEntry;
import product.Sale;
import database.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;

import java.util.ArrayList;
import java.awt.Desktop;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Locale;

import components.ProductTableView;
import components.SaleTableView;
import components.ReportTableView;
import components.MenuBarBuilder;

import dialogs.AddProductDialog;
import dialogs.AddSaleDialog;
import dialogs.ConfirmDialog;

import services.ProductService;
import services.SaleService;
import services.ReportService;

import utils.ReceiptGenerator;

public class Dashboard {
    private String currentUser;
    private String currentRole;
    private int currentUserId;
    private ProductTableView productsTable;
    private TableView<Sale> salesTable;
    private ObservableList<Product> productsData;
    private ObservableList<Sale> salesData;
    private Label statusLabel;
    private Alert lowStockAlert;
    private final ProductService productService = new ProductService();
    private final SaleService saleService = new SaleService();
    private final ReportService reportService = new ReportService();
    

    public void showDashboard(Stage primaryStage, String username, String role) {
    	
    	
        this.currentUser = username;
        this.currentRole = role;
        this.currentUserId = getCurrentUserId();
        

        primaryStage.setTitle(LocaleManager.get("title.dashboard", username, role));
     // ========== FIX: Enable window features ==========
        primaryStage.setResizable(true);  // Allow resizing
        primaryStage.setMaximized(true);  // Start maximized

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(createMenuBar(primaryStage));

        TabPane tabPane = new TabPane();

        Tab productsTab = new Tab(LocaleManager.get("tab.products"));
        productsTab.setContent(createProductsSection());
        productsTab.setClosable(false);

        Tab salesTab = new Tab(LocaleManager.get("tab.sales"));
        salesTab.setContent(createSalesSection());
        salesTab.setClosable(false);

        Tab reportsTab = new Tab(LocaleManager.get("tab.reports"));
        reportsTab.setContent(createReportsSection());
        reportsTab.setClosable(false);

        tabPane.getTabs().addAll(productsTab, salesTab, reportsTab);
        borderPane.setCenter(tabPane);

        statusLabel = new Label(LocaleManager.get("status.showing.all"));
        statusLabel.setPadding(new Insets(5));
        borderPane.setBottom(statusLabel);

        loadProducts();
        loadSales();
        checkLowStock();

        Scene scene = new Scene(borderPane, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.show();

        if ("admin".equals(role)) {
            showDailySummary();
        }
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        return MenuBarBuilder.createMenuBar(primaryStage, this);  // ← NEW: use the builder class
    }

    private VBox createProductsSection() {
        productsTable = new ProductTableView();

        HBox searchBox = new HBox(10);
        searchBox.setPadding(new Insets(10, 10, 0, 10));
        ComboBox<String> searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll(
            LocaleManager.get("search.all"),
            LocaleManager.get("search.name"),
            LocaleManager.get("search.category"),
            LocaleManager.get("search.supplier"),
            LocaleManager.get("search.id")
        );
        searchTypeCombo.setValue(LocaleManager.get("search.all"));
        TextField searchField = new TextField();
        searchField.setPromptText(LocaleManager.get("search.placeholder"));
        searchField.setPrefWidth(250);
        Button searchButton = new Button(LocaleManager.get("button.search"));
        Button clearButton = new Button(LocaleManager.get("button.clear"));
        searchBox.getChildren().addAll(
            new Label(LocaleManager.get("search.in") + ":"), searchTypeCombo,
            new Label(LocaleManager.get("search.for") + ":"), searchField, searchButton, clearButton
        );
        searchButton.setOnAction(e -> filterProductsEnhanced(searchField.getText().trim().toLowerCase(), searchTypeCombo.getValue()));
        clearButton.setOnAction(e -> {
            searchField.clear();
            productsTable.setItems(productsData);
            updateStatus(LocaleManager.get("status.search.cleared"));
        });
        searchField.textProperty().addListener((obs, old, newVal) -> {
            String text = newVal == null ? "" : newVal.trim().toLowerCase();
            if (text.isEmpty()) productsTable.setItems(productsData);
            else filterProductsEnhanced(text, searchTypeCombo.getValue());
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        Button addBtn = new Button(LocaleManager.get("button.add"));
        Button updateBtn = new Button(LocaleManager.get("button.update"));
        Button deleteBtn = new Button(LocaleManager.get("button.delete"));
        Button refreshBtn = new Button(LocaleManager.get("button.refresh"));
        addBtn.setOnAction(e -> addProduct());
        updateBtn.setOnAction(e -> updateProduct());
        deleteBtn.setOnAction(e -> deleteProduct());
        refreshBtn.setOnAction(e -> loadProducts());
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        updateBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        refreshBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        if ("staff".equals(currentRole)) {
            addBtn.setDisable(true);
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
        }
        buttonBox.getChildren().addAll(addBtn, updateBtn, deleteBtn, refreshBtn);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(searchBox, productsTable, buttonBox);
        return vbox;
    }
    
    private void filterProductsEnhanced(String searchText, String searchType) {
        if (searchText.isEmpty()) {
            productsTable.setItems(productsData);
            updateStatus(LocaleManager.get("status.showing.all"));
            return;
        }

        ObservableList<Product> filtered = FXCollections.observableArrayList();
        for (Product p : productsData) {
            boolean matches = switch (searchType) {
                case "All Fields", "Tüm Alanlar" -> p.getName().toLowerCase().contains(searchText) ||
                        p.getCategory().toLowerCase().contains(searchText) ||
                        p.getSupplier().toLowerCase().contains(searchText) ||
                        String.valueOf(p.getProductId()).contains(searchText);
                case "Name", "Ürün Adı" -> p.getName().toLowerCase().contains(searchText);
                case "Category", "Kategori" -> p.getCategory().toLowerCase().contains(searchText);
                case "Supplier", "Tedarikçi" -> p.getSupplier().toLowerCase().contains(searchText);
                case "ID" -> String.valueOf(p.getProductId()).contains(searchText);
                default -> false;
            };
            if (matches) filtered.add(p);
        }
        productsTable.setItems(filtered);
        updateStatus(LocaleManager.get("status.found", filtered.size()));
    }

    private VBox createSalesSection() {
        salesTable = new SaleTableView();

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        Button addSaleBtn = new Button(LocaleManager.get("button.add.sale"));
        Button deleteSaleBtn = new Button(LocaleManager.get("button.delete.sale"));
        Button refreshBtn = new Button(LocaleManager.get("button.refresh"));
        addSaleBtn.setOnAction(e -> addSale());
        deleteSaleBtn.setOnAction(e -> deleteSale());
        refreshBtn.setOnAction(e -> loadSales());
        addSaleBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        deleteSaleBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        refreshBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        buttonBox.getChildren().addAll(addSaleBtn, deleteSaleBtn, refreshBtn);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(salesTable, buttonBox);
        return vbox;
    }

    private VBox createReportsSection() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        HBox dateRangeBox = new HBox(10);
        dateRangeBox.setPadding(new Insets(10));
        DatePicker startDatePicker = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker endDatePicker = new DatePicker(LocalDate.now());
        Button generateReportBtn = new Button(LocaleManager.get("button.generate.report"));
        Button exportCsvBtn = new Button(LocaleManager.get("button.export.csv"));
        generateReportBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        exportCsvBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        dateRangeBox.getChildren().addAll(
            new Label(LocaleManager.get("label.start.date") + ":"), startDatePicker,
            new Label(LocaleManager.get("label.end.date") + ":"), endDatePicker,
            generateReportBtn, exportCsvBtn
        );

        TableView<ReportEntry> reportTable = new ReportTableView();  // ← NEW: use the extracted class

        Label summaryLabel = new Label();
        summaryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        generateReportBtn.setOnAction(e1 -> {
            LocalDate s = startDatePicker.getValue();
            LocalDate e = endDatePicker.getValue();
            if (s == null || e == null) {
                showAlert(Alert.AlertType.ERROR, "alert.error", "msg.select.dates");
                return;
            }
            if (s.isAfter(e)) {
                showAlert(Alert.AlertType.ERROR, "alert.error", "msg.start.after.end");
                return;
            }
            generateSalesReport(reportTable, summaryLabel, s, e);
        });

        exportCsvBtn.setOnAction(e2 -> {
            LocalDate s = startDatePicker.getValue();
            LocalDate e = endDatePicker.getValue();
            if (s == null || e == null) {
                showAlert(Alert.AlertType.ERROR, "alert.error", "msg.select.dates");
                return;
            }
            exportReportCSV(s, e);
        });

        vbox.getChildren().addAll(dateRangeBox, summaryLabel, reportTable);
        return vbox;
    }
    // ====================== DATABASE & LOGIC ======================

    private void loadProducts() {
        updateStatus(LocaleManager.get("status.loading.products"));
        productsData = productService.loadProducts();
        productsTable.setItems(productsData);
        updateStatus(LocaleManager.get("status.products.loaded", productsData.size()));
        productService.checkLowStock(productsData, () -> checkLowStock());
    }

    private void loadSales() {
        updateStatus(LocaleManager.get("status.loading.sales"));
        salesData = saleService.loadSales();
        salesTable.setItems(salesData);
        updateStatus(LocaleManager.get("status.sales.loaded", salesData.size()));
    }

    private void checkLowStock() {
        if (productsData == null) return;

        productService.checkLowStock(productsData, () -> {
            List<Product> lowStockProducts = productsData.stream()
                    .filter(p -> p.getQuantity() < 10)
                    .sorted(Comparator.comparingInt(Product::getQuantity))
                    .collect(Collectors.toList());

            if (lowStockProducts.isEmpty()) return;

            int count = lowStockProducts.size();

            String headerLine = LocaleManager.get("alert.low.stock.header", count);

            StringBuilder details = new StringBuilder();
            for (Product p : lowStockProducts) {
                details.append("• ")
                       .append(p.getName())
                       .append(" (ID: ")
                       .append(p.getProductId())
                       .append(", Kalan: ")
                       .append(p.getQuantity())
                       .append(" adet)\n");
            }

            String fullMessage = LocaleManager.get("alert.low.stock") + "\n\n" + headerLine + "\n\n" + details.toString();

            showAlert(Alert.AlertType.WARNING, "alert.warning", fullMessage);
        });
    }
    // ====================== CRUD OPERATIONS ======================

    private void addProduct() {
        if (!hasAdminAccess()) {
            showAlert(Alert.AlertType.ERROR, "alert.error", "msg.access.denied");
            return;
        }

        AddProductDialog dialog = new AddProductDialog(null);
        dialog.showAndWait().ifPresent(product -> {
            productService.addProduct(product);
            loadProducts();
            showAlert(Alert.AlertType.INFORMATION, "alert.success", "msg.product.added");
        });
    }
    private void updateProduct() {
        if (!hasAdminAccess()) {
            showAlert(Alert.AlertType.ERROR, "alert.error", "msg.access.denied");
            return;
        }

        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "alert.warning", "msg.no.selection");
            return;
        }

        AddProductDialog dialog = new AddProductDialog(selected);
        dialog.showAndWait().ifPresent(product -> {
            productService.updateProduct(product);
            loadProducts();
            showAlert(Alert.AlertType.INFORMATION, "alert.success", "msg.product.updated");
        });
    }

    private void deleteProduct() {
        if (!hasAdminAccess()) {
            showAlert(Alert.AlertType.ERROR, "alert.error", "msg.access.denied");
            return;
        }

        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "alert.warning", "msg.no.selection");
            return;
        }

        if (productService.hasSalesRecords(selected.getProductId())) {
            showAlert(Alert.AlertType.ERROR, "alert.error", "msg.product.has.sales");
            return;
        }

        if (ConfirmDialog.show("alert.confirm", "alert.confirm.delete.header", "alert.confirm.delete", selected.getName())) {
            productService.deleteProduct(selected.getProductId());
            loadProducts();
            showAlert(Alert.AlertType.INFORMATION, "alert.success", "msg.product.deleted");
        }
    }
    
    private void addSale() {
        AddSaleDialog dialog = new AddSaleDialog(productsData, currentUserId);
        dialog.showAndWait().ifPresent(salesList -> {
            if (salesList != null && !salesList.isEmpty()) {
                processMultipleSales(salesList);
            }
        });
    }

    // Helper method to create sale items table
    private TableView<Sale> createSaleItemsTable(ObservableList<Sale> saleItems) {
        TableView<Sale> table = new TableView<>(saleItems);
        table.setPrefHeight(200);
        
        TableColumn<Sale, String> productCol = new TableColumn<>("Ürün");
        productCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getProductName()));
        
        TableColumn<Sale, Integer> qtyCol = new TableColumn<>("Adet");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));
        
        TableColumn<Sale, Double> priceCol = new TableColumn<>("Birim Fiyat");
        priceCol.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());
        priceCol.setCellFactory(col -> new TableCell<Sale, Double>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("₺%.2f", item));
            }
        });
        
        TableColumn<Sale, Double> totalCol = new TableColumn<>("Toplam");
        totalCol.setCellValueFactory(cellData -> {
            double total = cellData.getValue().getPrice() * cellData.getValue().getQuantitySold();
            return new SimpleDoubleProperty(total).asObject();
        });
        totalCol.setCellFactory(col -> new TableCell<Sale, Double>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("₺%.2f", item));
            }
        });
        
        table.getColumns().addAll(productCol, qtyCol, priceCol, totalCol);
        return table;
    }

    // Helper method to update sale summary
    private void updateSaleSummary(ObservableList<Sale> saleItems, Label totalItemsLabel, Label totalAmountLabel) {
        int totalItems = 0;
        double totalAmount = 0;
        
        for (Sale sale : saleItems) {
            totalItems += sale.getQuantitySold();
            totalAmount += sale.getPrice() * sale.getQuantitySold();
        }
        
        totalItemsLabel.setText("Toplam Ürün: " + totalItems);
        totalAmountLabel.setText(String.format("Toplam Tutar: ₺%.2f", totalAmount));
    }

 // New method to process multiple sales
    private void processMultipleSales(List<Sale> salesList) {
        saleService.addMultipleSales(salesList);

        // Generate and save receipt using ReceiptGenerator
        ReceiptGenerator.generateAndSaveReceipt(salesList, currentUser);

        // Show success message
        int totalItems = salesList.stream().mapToInt(Sale::getQuantitySold).sum();
        double totalAmount = salesList.stream()
                .mapToDouble(s -> s.getPrice() * s.getQuantitySold())
                .sum();

        String successMessage = String.format(
            "Satış başarıyla tamamlandı!\n\n" +
            "Satılan Ürün Sayısı: %d\n" +
            "Toplam Adet: %d\n" +
            "Toplam Tutar: ₺%.2f\n\n" +
            "Fiş/fatura 'receipts' klasörüne kaydedildi.",
            salesList.size(), totalItems, totalAmount
        );

        showAlert(Alert.AlertType.INFORMATION, "alert.success", successMessage);

        // Refresh data
        loadProducts();
        loadSales();
        checkLowStock();
    }

    
    
    private void deleteSale() {
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "alert.warning", "msg.no.selection");
            return;
        }

        if (ConfirmDialog.show("alert.confirm", "alert.confirm.delete.sale", "alert.confirm.delete.sale.details",
                selected.getSaleId(), selected.getProductName(), selected.getQuantitySold(), selected.getSaleDate())) {
            saleService.deleteSale(selected.getSaleId(), selected.getProductId(), selected.getQuantitySold());
            loadProducts();
            loadSales();
            checkLowStock();
            showAlert(Alert.AlertType.INFORMATION, "alert.success", "msg.sale.deleted");
        }
    }

    // ====================== REPORTS & EXPORT ======================

    private void generateSalesReport(TableView<ReportEntry> table, Label summaryLabel, LocalDate start, LocalDate end) {
        reportService.generateSalesReport(table.getItems(), start, end, summaryLabel);
    }
    
    private void exportReportCSV(LocalDate start, LocalDate end) {
        FileChooser fc = new FileChooser();
        fc.setTitle(LocaleManager.get("menu.export.report"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("sales_report_" + LocalDate.now() + ".csv");

        File file = fc.showSaveDialog(null);
        if (file == null) return;

        String sql = "SELECT s.sale_date, SUM(s.quantity_sold) as total_qty, " +
                    "SUM(s.quantity_sold * p.price) as total_rev " +
                    "FROM sales s JOIN products p ON s.product_id = p.product_id " +
                    "WHERE s.sale_date BETWEEN ? AND ? GROUP BY s.sale_date";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             FileWriter writer = new FileWriter(file)) {

            pstmt.setDate(1, Date.valueOf(start));
            pstmt.setDate(2, Date.valueOf(end));
            ResultSet rs = pstmt.executeQuery();

            writer.write("Date,Total Quantity Sold,Total Revenue\n");
            while (rs.next()) {
                writer.write(String.format("%s,%d,%.2f\n",
                    rs.getDate("sale_date"),
                    rs.getInt("total_qty"),
                    rs.getDouble("total_rev")
                ));
            }

            showAlert(Alert.AlertType.INFORMATION, "alert.success", "msg.export.success", file.getName());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "alert.error", "msg.export.failed", e.getMessage());
        }
    }

    public void showDailySummary() {
        reportService.showDailySummary();
    }

    public void showSalesReport() {
        // Same logic as generateSalesReport but in new window
        Stage stage = new Stage();
        stage.setTitle(LocaleManager.get("menu.sales.report"));

        TableView<ReportEntry> table = new TableView<>();
        TableColumn<ReportEntry, LocalDate> dateCol = new TableColumn<>(LocaleManager.get("column.date"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<ReportEntry, Integer> qtyCol = new TableColumn<>(LocaleManager.get("column.total.quantity"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));

        TableColumn<ReportEntry, Double> revCol = new TableColumn<>(LocaleManager.get("column.total.revenue"));
        revCol.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));
        revCol.setCellFactory(col -> new TableCell<ReportEntry, Double>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
            }
        });

        table.getColumns().addAll(dateCol, qtyCol, revCol);

        String sql = "SELECT s.sale_date, SUM(s.quantity_sold) as total_qty, " +
                    "SUM(s.quantity_sold * p.price) as total_rev " +
                    "FROM sales s JOIN products p ON s.product_id = p.product_id " +
                    "GROUP BY s.sale_date ORDER BY s.sale_date DESC";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                table.getItems().add(new ReportEntry(
                    rs.getDate("sale_date").toLocalDate(),
                    rs.getInt("total_qty"),
                    rs.getDouble("total_rev")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        VBox root = new VBox(10, table);
        root.setPadding(new Insets(10));
        stage.setScene(new Scene(root, 600, 500));
        stage.show();
    }

    public void showLowStockReport() {
        reportService.showLowStockReport(productsData);
    }
    // ====================== EXPORT CSV METHODS ======================

    public void exportProductsCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(LocaleManager.get("menu.export.products"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));

        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        try (FileWriter w = new FileWriter(file)) {
            w.write("ID,Name,Category,Quantity,Price,Supplier\n");
            for (Product p : productsData) {
                w.write(String.format("%d,%s,%s,%d,%.2f,%s\n",
                    p.getProductId(),
                    p.getName().replace(",", ""),
                    p.getCategory().replace(",", ""),
                    p.getQuantity(),
                    p.getPrice(),
                    p.getSupplier().replace(",", "")
                ));
            }
            showAlert(Alert.AlertType.INFORMATION, "alert.success", "msg.export.success", file.getName());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "alert.error", "msg.export.failed", e.getMessage());
        }
    }

    public void exportSalesCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(LocaleManager.get("menu.export.sales"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));

        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        try (FileWriter w = new FileWriter(file)) {
            w.write("SaleID,ProductID,ProductName,QuantitySold,Date,SoldBy\n");
            for (Sale s : salesData) {
                w.write(String.format("%d,%d,%s,%d,%s,%s\n",
                    s.getSaleId(),
                    s.getProductId(),
                    s.getProductName().replace(",", ""),
                    s.getQuantitySold(),
                    s.getSaleDate(),
                    s.getSoldBy()
                ));
            }
            showAlert(Alert.AlertType.INFORMATION, "alert.success", "msg.export.success", file.getName());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "alert.error", "msg.export.failed", e.getMessage());
        }
    }

    public void exportReportCSV() {
        exportReportCSV(LocalDate.now().minusDays(30), LocalDate.now());
    }

    // ====================== HELPER METHODS ======================

    private GridPane createProductForm(Product existing) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        nameField.setId("nameField");
        TextField catField = new TextField(existing != null ? existing.getCategory() : "");
        catField.setId("categoryField");
        TextField qtyField = new TextField(existing != null ? String.valueOf(existing.getQuantity()) : "");
        qtyField.setId("quantityField");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "");
        priceField.setId("priceField");
        TextField suppField = new TextField(existing != null ? existing.getSupplier() : "");
        suppField.setId("supplierField");

        grid.add(new Label(LocaleManager.get("label.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(LocaleManager.get("label.category") + ":"), 0, 1);
        grid.add(catField, 1, 1);
        grid.add(new Label(LocaleManager.get("label.quantity") + ":"), 0, 2);
        grid.add(qtyField, 1, 2);
        grid.add(new Label(LocaleManager.get("label.price") + ":"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label(LocaleManager.get("label.supplier") + ":"), 0, 4);
        grid.add(suppField, 1, 4);

        return grid;
    }

    private Product validateAndCreateProduct(String name, String category, String qtyStr,
                                           String priceStr, String supplier, Integer id) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException(LocaleManager.get("msg.name.required"));
        if (category == null || category.trim().isEmpty()) throw new IllegalArgumentException(LocaleManager.get("msg.category.required"));
        if (supplier == null || supplier.trim().isEmpty()) throw new IllegalArgumentException(LocaleManager.get("msg.supplier.required"));

        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
            if (qty < 0) throw new IllegalArgumentException(LocaleManager.get("msg.negative.quantity"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(LocaleManager.get("msg.invalid.quantity"));
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) throw new IllegalArgumentException(LocaleManager.get("msg.negative.price"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(LocaleManager.get("msg.invalid.price"));
        }

        return new Product(id == null ? 0 : id, name.trim(), category.trim(), qty, price, supplier.trim());
    }

    private boolean hasSalesRecords(int productId) {
        String sql = "SELECT COUNT(*) FROM sales WHERE product_id = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean hasAdminAccess() {
        return "admin".equals(currentRole);
    }

    private void updateStatus(String message) {
        statusLabel.setText(message + " | " + LocaleManager.get("status.user") + " " + currentUser +
                " | " + LocaleManager.get("status.role") + " " + currentRole);
    }

    public void refreshAllData() {
        loadProducts();
        loadSales();
        checkLowStock();
    }

    private int getCurrentUserId() {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentUser);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("user_id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

 // ====================== CENTRALIZED ALERT METHODS ======================
    

    // Then modify your showAlert() method to add tracing:
    private void showAlert(Alert.AlertType type, String titleKey, String messageKey, Object... args) {
        System.out.println("\n=== ALERT CALLED ===");
        System.out.println("Caller: " + getCallerMethodName());
        System.out.println("Type: " + type);
        System.out.println("Title Key: '" + titleKey + "'");
        System.out.println("Message Key: '" + messageKey + "'");
        System.out.println("Args: " + Arrays.toString(args));
        
        Alert alert = new Alert(type);
        
        // Get title
        String title = LocaleManager.get(titleKey);
        System.out.println("Title resolved: '" + title + "'");
        alert.setTitle(title);
        alert.setHeaderText(null);
        
        // Get message
        String message;
        if (args.length > 0) {
            message = LocaleManager.get(messageKey, args);
        } else {
            message = LocaleManager.get(messageKey);
        }
        
        System.out.println("Message resolved: '" + message + "'");
        
        // Check if it's showing raw key
        if (message.startsWith("[") && message.endsWith("]")) {
            System.out.println("ERROR: Raw key displayed! Key '" + messageKey + "' not found!");
        }
        
        alert.setContentText(message);
        alert.showAndWait();
        System.out.println("=== ALERT END ===\n");
    }

    // Helper method to get caller method name
    private String getCallerMethodName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // stackTrace[0] = getStackTrace
        // stackTrace[1] = getCallerMethodName
        // stackTrace[2] = showAlert
        // stackTrace[3] = The method that called showAlert
        if (stackTrace.length > 3) {
            return stackTrace[3].getMethodName() + "() at line " + stackTrace[3].getLineNumber();
        }
        return "Unknown";
    }

    // Overloaded version for when you already have the full translated message
  //  private void showAlert(Alert.AlertType type, String titleKey, String readyMessage) {
      //  Alert alert = new Alert(type);
     //   alert.setTitle(LocaleManager.get(titleKey));
      //  alert.setHeaderText(null);
     //   alert.setContentText(readyMessage);
      //  alert.showAndWait();
   // }
   
    
}