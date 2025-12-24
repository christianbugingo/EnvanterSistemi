package components;

import javafx.scene.control.*;
import javafx.stage.Stage;
import gui.LocaleManager;
import gui.Login;
import gui.Dashboard;

public class MenuBarBuilder {

	public static MenuBar createMenuBar(Stage primaryStage, Dashboard dashboard) {
	    MenuBar menuBar = new MenuBar();

	    Menu fileMenu = new Menu(LocaleManager.get("menu.file"));
	    MenuItem exportProducts = new MenuItem(LocaleManager.get("menu.export.products"));
	    MenuItem exportSales = new MenuItem(LocaleManager.get("menu.export.sales"));
	    MenuItem exportReport = new MenuItem(LocaleManager.get("menu.export.report"));
	    MenuItem logout = new MenuItem(LocaleManager.get("menu.logout"));

	    exportProducts.setOnAction(e -> dashboard.exportProductsCSV());
	    exportSales.setOnAction(e -> dashboard.exportSalesCSV());
	    exportReport.setOnAction(e -> dashboard.exportReportCSV());
	    logout.setOnAction(e -> new Login().showLogin(primaryStage));

	    fileMenu.getItems().addAll(exportProducts, exportSales, exportReport, new SeparatorMenuItem(), logout);

	    Menu reportMenu = new Menu(LocaleManager.get("menu.reports"));
	    MenuItem salesReport = new MenuItem(LocaleManager.get("menu.sales.report"));
	    MenuItem dailySummary = new MenuItem(LocaleManager.get("menu.daily.summary"));
	    MenuItem lowStockReport = new MenuItem(LocaleManager.get("menu.low.stock.report"));

	    salesReport.setOnAction(e -> dashboard.showSalesReport());
	    dailySummary.setOnAction(e -> dashboard.showDailySummary());
	    lowStockReport.setOnAction(e -> dashboard.showLowStockReport());

	    reportMenu.getItems().addAll(salesReport, dailySummary, lowStockReport);

	    Menu viewMenu = new Menu(LocaleManager.get("menu.view"));
	    MenuItem refreshAll = new MenuItem(LocaleManager.get("menu.refresh.all"));
	    refreshAll.setOnAction(e -> dashboard.refreshAllData());

	    viewMenu.getItems().add(refreshAll);

	    menuBar.getMenus().addAll(fileMenu, reportMenu, viewMenu);

	    return menuBar;
	}
}