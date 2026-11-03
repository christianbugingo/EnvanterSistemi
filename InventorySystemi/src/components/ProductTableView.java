package components;
import gui.*;
import product.Product;
import javafx.scene.control.*;
public class ProductTableView extends TableView<Product> {
    public ProductTableView() {
        setPlaceholder(new Label(LocaleManager.get("table.empty")));
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getColumns().add(Ui.numberColumn("column.id",p->java.math.BigDecimal.valueOf(p.getProductId()),false));
        getColumns().add(Ui.column("column.name",Product::getName));
        getColumns().add(Ui.column("column.category",Product::getCategory));
        getColumns().add(Ui.numberColumn("column.quantity",Product::getQuantity,false));
        getColumns().add(Ui.column("column.unit",p->p.getUnit().label()));
        getColumns().add(Ui.numberColumn("column.price",Product::getPrice,true));
        getColumns().add(Ui.numberColumn("product.threshold",Product::getLowStock,false));
        getColumns().add(Ui.column("column.supplier",Product::getSupplier));
        setRowFactory(t->new TableRow<>() {
            @Override protected void updateItem(Product p,boolean empty) { super.updateItem(p,empty); setStyle(!empty && p!=null && p.isLowStock()?"-fx-background-color:#fff0cc;":""); }
        });
    }
}
