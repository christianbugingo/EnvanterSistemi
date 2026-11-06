package components;
import gui.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
public class MenuBarBuilder {
    private static MenuItem item(String key,Runnable action) { MenuItem item=new MenuItem(LocaleManager.get(key));item.setOnAction(e->Ui.run(action));return item; }
    public static MenuBar createMenuBar(Stage stage,Dashboard dashboard) {
        Menu file=new Menu(LocaleManager.get("menu.file"));
        file.getItems().addAll(item("menu.export.products",dashboard::exportProductsCSV),item("menu.export.sales",dashboard::exportSalesCSV),new SeparatorMenuItem(),item("password.change",dashboard::changePassword),item("menu.logout",dashboard::logout));
        if("admin".equals(dashboard.getCurrentRole()))file.getItems().addAll(item("settings.title",dashboard::settings),item("backup.create",dashboard::backupDatabase),item("recovery.replace",dashboard::recoveryCode));
        Menu reports=new Menu(LocaleManager.get("menu.reports"));
        reports.getItems().add(item("menu.low.stock.report",dashboard::showLowStockReport));
        if("admin".equals(dashboard.getCurrentRole())) reports.getItems().addAll(item("menu.daily.summary",dashboard::showDailySummary),item("menu.export.report",dashboard::exportReportCSV));
        Menu view=new Menu(LocaleManager.get("menu.view")); view.getItems().add(item("menu.refresh.all",dashboard::refreshAllData));
        Menu language=new Menu(LocaleManager.get("language.label")); ToggleGroup group=new ToggleGroup();
        for(String code:new String[]{"en","tr"}) {
            RadioMenuItem entry=new RadioMenuItem("en".equals(code)?"English":"Türkçe");entry.setToggleGroup(group);entry.setSelected(LocaleManager.getLanguage().equals(code));
            entry.setOnAction(e->Ui.run(()->{LocaleManager.setLanguage(code);dashboard.showDashboard(stage,dashboard.getCurrentUser(),dashboard.getCurrentRole());}));language.getItems().add(entry);
        }
        view.getItems().add(item("about.title",dashboard::about));
        return new MenuBar(file,reports,view,language);
    }
}
