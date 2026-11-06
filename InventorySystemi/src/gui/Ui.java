package gui;

import javafx.scene.control.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import java.util.function.Function;

public final class Ui {
    private Ui() { }
    public static Button button(String key, Runnable action) {
        Button b=new Button(LocaleManager.get(key)); b.setOnAction(e->run(action));
        if(key.contains("delete") || key.contains("void")) b.getStyleClass().add("danger");
        return b;
    }
    public static void run(Runnable action) { try { action.run(); } catch(RuntimeException ex) { if(ex.getCause()!=null)org.slf4j.LoggerFactory.getLogger(Ui.class).error("Operation failed",ex);error(ex.getMessage()); } }
    public static void error(String message) { alert(Alert.AlertType.ERROR,"alert.error",message); }
    public static void info(String message) { alert(Alert.AlertType.INFORMATION,"alert.information",message); }
    public static void alert(Alert.AlertType type,String title,String message) {
        Alert a=new Alert(type); a.setTitle(LocaleManager.get(title)); a.setHeaderText(null); a.setContentText(message);
        a.getButtonTypes().setAll(new ButtonType(LocaleManager.get("button.ok"),ButtonBar.ButtonData.OK_DONE)); a.showAndWait();
    }
    public static boolean confirm(String message) {
        ButtonType yes=new ButtonType(LocaleManager.get("button.confirm"),ButtonBar.ButtonData.OK_DONE);
        Alert a=new Alert(Alert.AlertType.CONFIRMATION,message,yes,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        a.setTitle(LocaleManager.get("alert.confirm")); a.setHeaderText(null); return a.showAndWait().orElse(null)==yes;
    }
    public static <T> TableColumn<T,String> column(String key, Function<T,Object> value) {
        TableColumn<T,String> col=new TableColumn<>(LocaleManager.get(key));
        col.setCellValueFactory(c->new ReadOnlyStringWrapper(String.valueOf(value.apply(c.getValue()))));
        col.setPrefWidth(150); col.setMinWidth(90); return col;
    }
    public static void style(javafx.scene.Parent root) {
        root.getStylesheets().add(Ui.class.getResource("/styles.css").toExternalForm());
    }
    public static <T> TableColumn<T,java.math.BigDecimal> numberColumn(String key, Function<T,java.math.BigDecimal> value, boolean currency) {
        TableColumn<T,java.math.BigDecimal> col=new TableColumn<>(LocaleManager.get(key));
        col.setCellValueFactory(c->new javafx.beans.property.ReadOnlyObjectWrapper<>(value.apply(c.getValue())));
        col.setCellFactory(c->new TableCell<>() {
            @Override protected void updateItem(java.math.BigDecimal amount,boolean empty) {
                super.updateItem(amount,empty); setText(empty||amount==null?null:currency?LocaleManager.money(amount):LocaleManager.quantity(amount));
                setStyle("-fx-alignment:CENTER-RIGHT;");
            }
        });
        col.setPrefWidth(140);col.setMinWidth(90);return col;
    }
    public static <T> TableView<T> table() {
        TableView<T> table=new TableView<>(); table.setPlaceholder(new Label(LocaleManager.get("table.empty")));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN); return table;
    }
}
