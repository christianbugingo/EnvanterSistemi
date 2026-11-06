package gui;

import components.*;
import database.DBConnection;
import dialogs.*;
import services.*;
import product.*;
import utils.*;
import javafx.animation.*;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class Dashboard {
    private final ProductService products=new ProductService();
    private final SaleService sales=new SaleService();
    private final ReportService reports=new ReportService();
    private final AuthService auth=new AuthService();
    private final AuditService audits=new AuditService();
    private ProductTableView productTable;
    private SaleTableView saleTable;
    private ReportTableView reportTable;
    private TableView<AuthService.User> usersTable;
    private TableView<AuditService.Entry> activityTable;
    private ObservableList<Product> productRows=FXCollections.observableArrayList();
    private DatePicker start,end;
    private Label status,overview,reportSummary;
    private TextField search;
    private Stage stage;
    private Timeline refresh;
    private AuthService.User user;
    public String getCurrentUser() { return user.username(); }
    public String getCurrentRole() { return user.role(); }
    public void showDashboard(Stage primaryStage,String username,String role) {
        stopRefresh(); user=AuthService.current();
        if(user==null || user.mustChangePassword()) throw AuthService.failure("msg.access.denied");
        stage=primaryStage; Branding.apply(stage); stage.setTitle(LocaleManager.get("title.dashboard",user.username(),LocaleManager.get("role."+user.role())));
        BorderPane root=new BorderPane(); Ui.style(root); root.setPadding(new Insets(12));
        overview=new Label(); overview.setStyle("-fx-font-size:18; -fx-font-weight:bold; -fx-padding:12;");
        HBox brandHeader=new HBox(12,Branding.logo(48),overview);brandHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);brandHeader.setPadding(new Insets(8));
        root.setTop(new VBox(MenuBarBuilder.createMenuBar(stage,this),brandHeader));
        TabPane tabs=new TabPane(); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(tab("tab.products",productSection())); tabs.getTabs().add(tab("tab.sales",salesSection()));
        if(user.isAdmin()) {
            tabs.getTabs().add(tab("tab.reports",reportsSection()));
            tabs.getTabs().add(tab("tab.staff",staffSection()));
            tabs.getTabs().add(tab("tab.activity",activitySection()));
        }
        root.setCenter(tabs); status=new Label(); status.setPadding(new Insets(10)); root.setBottom(status);
        stage.setScene(new Scene(root,1280,760)); stage.setMinWidth(1050); stage.setMinHeight(620); stage.setMaximized(true); stage.show();
        stage.setOnCloseRequest(e->{stopRefresh();auth.logout();});
        stage.getScene().getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.N,javafx.scene.input.KeyCombination.SHORTCUT_DOWN),()->Ui.run(this::addSale));
        stage.getScene().getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F,javafx.scene.input.KeyCombination.SHORTCUT_DOWN),()->{tabs.getSelectionModel().select(0);search.requestFocus();});
        refreshAllData();
        refresh=new Timeline(new KeyFrame(Duration.seconds(10),e->{
            try { refreshAllData(); }
            catch(RuntimeException ex) { status.setText(LocaleManager.get("refresh.failed",ex.getMessage())); }
        })); refresh.setCycleCount(Timeline.INDEFINITE); refresh.play();
    }
    private Tab tab(String key,javafx.scene.Node content) { return new Tab(LocaleManager.get(key),content); }
    private VBox section(javafx.scene.Node... nodes) { VBox v=new VBox(12,nodes); v.setPadding(new Insets(15)); for(var n:nodes) if(n instanceof TableView<?>) VBox.setVgrow(n,Priority.ALWAYS); return v; }
    private HBox buttons(Button... buttons) { return new HBox(12,buttons); }
    private VBox productSection() {
        productTable=new ProductTableView(); productTable.setId("productsTable");
        search=new TextField(); search.setPromptText(LocaleManager.get("search.placeholder")); search.textProperty().addListener((o,a,b)->filterProducts());
        HBox actions=buttons(Ui.button("button.refresh",this::refreshAllData));
        if(user.isAdmin()) {
            actions.getChildren().addAll(Ui.button("button.add",()->new AddProductDialog(null).showAndWait().ifPresent(p->{products.addProduct(p);refreshAllData();})),
                Ui.button("button.update",()->new AddProductDialog(selected(productTable)).showAndWait().ifPresent(p->{products.updateProduct(p);refreshAllData();})),
                Ui.button("stock.adjust",this::adjustStock),
                Ui.button("button.delete",()->{Product p=selected(productTable);if(Ui.confirm(LocaleManager.get("confirm.product.delete",p.getName()))) {products.deleteProduct(p.getProductId());refreshAllData();}}));
        }
        return section(search,productTable,actions);
    }
    private void filterProducts() {
        String text=search.getText().trim().toLowerCase(Locale.ROOT);
        productTable.setItems(productRows.filtered(p->(p.getProductId()+" "+p.getName()+" "+p.getCategory()+" "+p.getSupplier()+" "+p.getBarcode()).toLowerCase(Locale.ROOT).contains(text)));
    }
    private VBox salesSection() {
        saleTable=new SaleTableView(); saleTable.setId("salesTable");
        HBox actions=buttons(Ui.button("button.add.sale",this::addSale),Ui.button("button.refresh",this::refreshAllData),Ui.button("receipt.reprint",()->{
            Sale selected=selected(saleTable);var file=ReceiptGenerator.generateAndSaveReceipt(sales.loadCheckout(selected.getCheckoutId()));Ui.info(LocaleManager.get("receipt.reprinted",file.toAbsolutePath().toString()));
        }));
        if(user.isAdmin()) actions.getChildren().add(Ui.button("button.void.sale",()->{
            Sale s=selected(saleTable); if(Ui.confirm(LocaleManager.get("confirm.sale.void",s.getSaleId()))) { TextInputDialog reason=new TextInputDialog();reason.setHeaderText(LocaleManager.get("void.reason.required"));reason.getDialogPane().getButtonTypes().setAll(new ButtonType(LocaleManager.get("button.confirm"),ButtonBar.ButtonData.OK_DONE),new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));reason.showAndWait().ifPresent(text->{sales.voidSale(s.getSaleId(),text);refreshAllData();}); }
        }));
        return section(new Label(LocaleManager.get(user.isAdmin()?"sales.admin.hint":"sales.staff.hint")),saleTable,actions);
    }
    public void addSale() {
        refreshAllData();
        AddSaleDialog dialog=new AddSaleDialog(productRows,user.id());
        dialog.showAndWait().ifPresent(items->{
            Checkout checkout=sales.recordSale(items,dialog.getPaymentMethod(),dialog.getCheckoutId());
            try {
                var file=ReceiptGenerator.generateAndSaveReceipt(checkout);
                Ui.info(LocaleManager.get("sale.saved",file.toAbsolutePath().toString()));
            } catch(java.io.UncheckedIOException ex) { Ui.alert(Alert.AlertType.WARNING,"alert.warning",LocaleManager.get("msg.receipt.failed",ex.getCause().getMessage())); }
            refreshAllData();
        });
    }
    private void adjustStock() {
        Product product=selected(productTable);Dialog<Boolean> dialog=new Dialog<>();dialog.initOwner(stage);Ui.style(dialog.getDialogPane());dialog.setTitle(LocaleManager.get("stock.adjust"));dialog.setHeaderText(product.toString());
        TextField delta=new TextField(),reason=new TextField();GridPane grid=new GridPane();grid.setPadding(new Insets(20));grid.setHgap(12);grid.setVgap(12);
        grid.addRow(0,new Label(LocaleManager.get("stock.delta")),delta);grid.addRow(1,new Label(LocaleManager.get("stock.reason")),reason);dialog.getDialogPane().setContent(grid);
        ButtonType save=new ButtonType(LocaleManager.get("button.save"),ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION,e->{
            try {String text=delta.getText().trim();BigDecimal value=text.startsWith("-")?Quantities.parse(text.substring(1),product.getUnit()).negate():Quantities.parse(text,product.getUnit());products.adjustStock(product.getProductId(),value,reason.getText());}
            catch(RuntimeException ex){e.consume();Ui.error(ex.getMessage());}
        });dialog.setResultConverter(b->b==save);if(dialog.showAndWait().orElse(false))refreshAllData();
    }
    public void backupDatabase() {
        FileChooser chooser=new FileChooser();chooser.setTitle(LocaleManager.get("backup.create"));chooser.setInitialFileName("inventory-backup-"+LocalDate.now()+".zip");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP","*.zip"));
        File file=chooser.showSaveDialog(stage);if(file!=null){new BackupService().create(file.toPath());Ui.info(LocaleManager.get("backup.saved",file.toPath().toAbsolutePath()));}
    }
    private VBox reportsSection() {
        start=new DatePicker(LocalDate.now().minusDays(30)); end=new DatePicker(LocalDate.now());
        reportTable=new ReportTableView(); reportTable.setId("reportsTable"); reportSummary=new Label();
        HBox dates=new HBox(12,new Label(LocaleManager.get("label.start.date")),start,new Label(LocaleManager.get("label.end.date")),end,Ui.button("button.generate.report",this::generateReport),Ui.button("button.export.csv",this::exportReportCSV));
        return section(dates,new Label(LocaleManager.get("report.units.hint")),reportSummary,reportTable);
    }
    private void generateReport() {
        var rows=reports.load(start.getValue(),end.getValue()); reportTable.setItems(FXCollections.observableArrayList(rows));
        reportSummary.setText(LocaleManager.get("report.period.total",start.getValue(),end.getValue(),reports.summary(rows)));
    }
    private VBox staffSection() {
        usersTable=Ui.table(); usersTable.setId("staffTable");
        usersTable.getColumns().add(Ui.column("login.username",AuthService.User::username));
        usersTable.getColumns().add(Ui.column("column.role",u->LocaleManager.get("role."+u.role())));
        usersTable.getColumns().add(Ui.column("column.status",u->LocaleManager.get(u.active()?"user.active":"user.disabled")));
        usersTable.getColumns().add(Ui.column("password.change",u->LocaleManager.get(u.mustChangePassword()?"user.pending":"user.ready")));
        return section(new Label(LocaleManager.get("staff.hint")),usersTable,buttons(
            Ui.button("staff.create",()->staffPasswordDialog(null)),
            Ui.button("staff.reset",()->staffPasswordDialog(selected(usersTable))),
            Ui.button("staff.toggle",()->{AuthService.User target=selected(usersTable);if(Ui.confirm(LocaleManager.get("staff.toggle.confirm",target.username()))) {auth.setStaffActive(target.id(),!target.active());refreshAllData();}}),
            Ui.button("button.refresh",this::refreshAllData)));
    }
    private void staffPasswordDialog(AuthService.User target) {
        if(target!=null && target.isAdmin()) throw AuthService.failure("user.staff.only");
        Dialog<Boolean> dialog=new Dialog<>(); dialog.initOwner(stage);
        Ui.style(dialog.getDialogPane());
        dialog.setTitle(LocaleManager.get(target==null?"staff.create":"staff.reset")); dialog.setHeaderText(LocaleManager.get("staff.password.hint"));
        TextField username=new TextField(target==null?"":target.username()); username.setDisable(target!=null);
        PasswordField password=new PasswordField(), confirm=new PasswordField();
        GridPane grid=new GridPane(); grid.setPadding(new Insets(20)); grid.setHgap(12); grid.setVgap(12);
        grid.addRow(0,new Label(LocaleManager.get("login.username")),username);
        grid.addRow(1,new Label(LocaleManager.get("password.temporary")),password);
        grid.addRow(2,new Label(LocaleManager.get("password.confirm")),confirm);
        dialog.getDialogPane().setContent(grid);
        ButtonType save=new ButtonType(LocaleManager.get("button.save"),ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION,e->{
            try {
                if(!password.getText().equals(confirm.getText())) throw AuthService.failure("password.mismatch");
                if(target!=null&&!Ui.confirm(LocaleManager.get("staff.reset.confirm",target.username()))){e.consume();return;}
                if(target==null) auth.createStaff(username.getText(),password.getText()); else auth.resetStaffPassword(target.id(),password.getText());
            } catch(RuntimeException ex) { e.consume(); Ui.error(ex.getMessage()); }
        });
        dialog.setResultConverter(b->b==save); if(dialog.showAndWait().orElse(false)) refreshAllData();
    }
    private VBox activitySection() {
        activityTable=Ui.table(); activityTable.setId("activityTable");
        activityTable.getColumns().add(Ui.column("activity.time",AuditService.Entry::time));
        activityTable.getColumns().add(Ui.column("login.username",AuditService.Entry::username));
        activityTable.getColumns().add(Ui.column("activity.action",a->LocaleManager.get("action."+a.action())));
        activityTable.getColumns().add(Ui.column("activity.details",a->a.details().replace(" PIECE"," "+Unit.PIECE.label()).replace(" KG"," "+Unit.KG.label())));
        activityTable.getColumns().add(Ui.column("activity.pc",AuditService.Entry::workstation));
        return section(new Label(LocaleManager.get("activity.hint")),activityTable,Ui.button("button.refresh",this::refreshAllData));
    }
    public void refreshAllData() {
        productRows=products.loadProducts(); filterProducts(); saleTable.setItems(sales.loadSales());
        long low=productRows.stream().filter(Product::isLowStock).count();
        if(user.isAdmin()) {
            usersTable.setItems(FXCollections.observableArrayList(auth.users())); activityTable.setItems(FXCollections.observableArrayList(audits.load()));
            String revenue=reports.summary(reports.load(LocalDate.now(),LocalDate.now()));
            overview.setText(LocaleManager.get("overview.admin",revenue,productRows.size(),low));
            generateReport();
        } else overview.setText(LocaleManager.get("overview.staff",user.username(),low));
        status.setText(LocaleManager.get("status.updated",java.time.LocalTime.now().withNano(0),LocaleManager.get("role."+user.role())));
    }
    public void showDailySummary() {
        var rows=reports.load(LocalDate.now(),LocalDate.now()); Ui.info(LocaleManager.get("daily.total",reports.summary(rows)));
    }
    public void showSalesReport() { if(!user.isAdmin()) throw AuthService.failure("msg.access.denied"); generateReport(); }
    public void showLowStockReport() {
        // Reload stock rather than using a terminal's stale cache.
        StringBuilder text=new StringBuilder(LocaleManager.get("report.low.stock.title")).append("\n\n");
        var low=products.loadProducts().stream().filter(Product::isLowStock).toList();
        if(low.isEmpty()) text.append(LocaleManager.get("msg.no.low.stock"));
        for(Product p:low) text.append(p.getName()).append(" | ").append(Quantities.format(p.getQuantity(),p.getUnit())).append(" | ").append(LocaleManager.money(p.getPrice())).append("\n");
        TextArea area=new TextArea(text.toString()); area.setEditable(false); area.setWrapText(true);
        Stage window=new Stage(); window.initOwner(stage); window.setTitle(LocaleManager.get("menu.low.stock.report")); window.setScene(new Scene(area,700,450)); window.show();
    }
    public void exportProductsCSV() {
        List<List<?>> rows=new ArrayList<>(); rows.add(headers("column.id","column.name","column.category","column.quantity","column.unit","column.price","column.supplier"));
        for(Product p:products.loadProducts()) rows.add(List.of(p.getProductId(),p.getName(),p.getCategory(),p.getQuantity(),p.getUnit().name(),p.getPrice(),p.getSupplier())); export("products",rows);
    }
    public void exportSalesCSV() {
        List<List<?>> rows=new ArrayList<>(); rows.add(headers("column.sale.id","column.product.name","column.quantity","column.unit","column.price","column.total","column.date","column.sold.by","column.status","checkout.id","payment.method"));
        for(Sale s:sales.loadSales()) rows.add(List.of(s.getSaleId(),s.getProductName(),s.getQuantitySold(),s.getUnit().name(),s.getPriceLabel(),s.getTotalLabel(),s.getSaleDate(),s.getSoldBy(),LocaleManager.get(s.isVoided()?"sale.voided":"sale.completed"),s.getCheckoutId(),s.getPaymentLabel())); export("sales",rows);
    }
    public void exportReportCSV() {
        List<List<?>> rows=new ArrayList<>(); rows.add(headers("column.date","column.unit","column.quantity","column.subtotal","column.tax","column.total.revenue","payment.method","column.currency"));
        for(ReportEntry r:reports.load(start.getValue(),end.getValue())) rows.add(List.of(r.date(),r.unit().name(),r.totalQuantity(),r.getSubtotalLabel(),r.getTaxLabel(),r.getTotalLabel(),r.getPaymentLabel(),r.currency().code())); export("report",rows);
    }
    private List<?> headers(String... keys) { return Arrays.stream(keys).map(LocaleManager::get).toList(); }
    private void export(String name,List<List<?>> rows) {
        FileChooser chooser=new FileChooser(); chooser.setTitle(LocaleManager.get("button.export.csv")); chooser.setInitialFileName(name+"_"+LocalDate.now()+".csv"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv"));
        File file=chooser.showSaveDialog(stage); if(file==null)return;
        try { Csv.write(file.toPath(),rows); Ui.info(LocaleManager.get("msg.export.success",file.getName())); }
        catch(java.io.IOException e) { throw new IllegalStateException(LocaleManager.get("msg.export.failed",e.getMessage()),e); }
    }
    private <T> T selected(TableView<T> table) { T selected=table.getSelectionModel().getSelectedItem(); if(selected==null)throw AuthService.failure("msg.no.selection");return selected; }
    public void settings(){if(dialogs.ShopSettingsDialog.show(false))showDashboard(stage,user.username(),user.role());}
    public void about(){
        Dialog<Void> d=new Dialog<>();d.setTitle(LocaleManager.get("about.title"));Ui.style(d.getDialogPane());
        Label text=new Label(LocaleManager.get("about.global",database.BuildInfo.version(),database.BuildInfo.date(),services.LegalService.acceptedAt(),DBConnection.location()));text.setWrapText(true);text.setMaxWidth(650);
        javafx.scene.layout.VBox box=new javafx.scene.layout.VBox(20,text,Ui.button("legal.full",dialogs.LegalDialog::view));box.setPadding(new Insets(25));d.getDialogPane().setContent(box);d.getDialogPane().getButtonTypes().add(new ButtonType(LocaleManager.get("button.close"),ButtonBar.ButtonData.CANCEL_CLOSE));d.showAndWait();
    }
    public void recoveryCode() {if(Ui.confirm(LocaleManager.get("recovery.replace.confirm")))dialogs.RecoveryDialog.saveCode(auth.generateRecoveryCode());}
    public void changePassword() { PasswordDialog.change(false); }
    public void logout() { stopRefresh(); auth.logout(); new Login().showLogin(stage); }
    public void stopRefresh() { if(refresh!=null) {refresh.stop();refresh=null;} }
}
