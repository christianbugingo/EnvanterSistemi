package gui;

import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import product.*;
import services.*;
import java.math.*;
import java.time.*;
import java.util.*;

/** Native light-theme charts; chart values are fed by exact BigDecimal service totals. */
public final class AnalyticsPane extends VBox {
    private final DashboardService service=new DashboardService();
    private final ComboBox<ShopCurrency> currency=new ComboBox<>();
    private final Label revenue=new Label(),transactions=new Label(),items=new Label(),low=new Label(),grade=new Label();
    private final PieChart pie=new PieChart();
    private final Label pieEmpty=new Label(LocaleManager.get("chart.empty"));
    private final BarChart<String,Number> daily=new BarChart<>(new CategoryAxis(),new NumberAxis());
    private final LineChart<String,Number> trend=new LineChart<>(new CategoryAxis(),new NumberAxis());
    private final VBox profitBox=new VBox(16);
    private final ComboBox<ProfitPeriod> period=new ComboBox<>(FXCollections.observableArrayList(ProfitPeriod.values()));
    private final DatePicker start=new DatePicker(LocalDate.now()),end=new DatePicker(LocalDate.now());
    private final Label profitRevenue=new Label(),cost=new Label(),profit=new Label(),margin=new Label(),profitStatus=new Label();
    private final BarChart<String,Number> comparison=new BarChart<>(new CategoryAxis(),new NumberAxis());
    private final TableView<DashboardService.UnknownCost> unknown=Ui.table();
    public AnalyticsPane(){
        setSpacing(20);setPadding(new Insets(20));getStyleClass().add("analytics");setId("analyticsDashboard");
        currency.setId("dashboardCurrency");var preferred=ShopSettings.current().currency();currency.setItems(FXCollections.observableArrayList(service.currencies()));currency.setValue(preferred);if(currency.getValue()==null&&!currency.getItems().isEmpty())currency.setValue(currency.getItems().getFirst());
        HBox filters=new HBox(12,new Label(LocaleManager.get("column.currency")),currency,new Label(LocaleManager.get("dashboard.scope")));filters.setAlignment(Pos.CENTER_LEFT);
        HBox cards=new HBox(16,card("kpi.sales",revenue,new Label(LocaleManager.get("kpi.net"))),card("kpi.transactions",transactions,new Label(LocaleManager.get("kpi.completed"))),card("kpi.items",items,new Label(LocaleManager.get("kpi.units"))),card("kpi.low",low,grade));
        revenue.setId("kpiSales");transactions.setId("kpiTransactions");items.setId("kpiItems");low.setId("kpiLowStock");
        pie.setTitle(LocaleManager.get("chart.payment"));pie.setLabelsVisible(true);pie.setLegendVisible(true);pie.setAnimated(false);pie.setId("paymentChart");
        styleChart(daily,"chart.daily");daily.setId("dailyChart");styleChart(trend,"chart.trend");trend.setCreateSymbols(false);trend.setId("trendChart");
        StackPane piePanel=new StackPane(pie,pieEmpty);HBox charts=new HBox(18,piePanel,daily);HBox.setHgrow(piePanel,Priority.ALWAYS);HBox.setHgrow(daily,Priority.ALWAYS);pie.setPrefSize(430,300);daily.setPrefSize(650,300);trend.setPrefHeight(260);
        getChildren().addAll(filters,cards,charts,trend,profitBox);
        period.setValue(ProfitPeriod.TODAY);period.setId("profitPeriod");start.setId("profitStart");end.setId("profitEnd");
        HBox range=new HBox(12,new Label(LocaleManager.get("profit.period")),period,start,end,Ui.button("button.refresh",this::refreshProfit));range.setAlignment(Pos.CENTER_LEFT);
        profitStatus.setWrapText(true);profitStatus.setId("profitStatus");
        HBox totals=new HBox(16,card("profit.revenue",profitRevenue,new Label(LocaleManager.get("kpi.net"))),card("profit.cogs",cost,new Label(LocaleManager.get("profit.known.only"))),card("profit.gross",profit,new Label(LocaleManager.get("profit.known.only"))),card("profit.margin",margin,new Label(LocaleManager.get("profit.known.only"))));
        profit.setId("grossProfit");profitRevenue.setId("profitRevenue");
        styleChart(comparison,"profit.comparison");comparison.setPrefHeight(270);comparison.setId("profitChart");
        unknown.setId("unknownCosts");unknown.setPrefHeight(200);unknown.getColumns().add(Ui.column("column.product.name",DashboardService.UnknownCost::product));unknown.getColumns().add(Ui.column("column.quantity",DashboardService.UnknownCost::quantity));unknown.getColumns().add(Ui.column("column.date",DashboardService.UnknownCost::date));unknown.getColumns().add(Ui.column("column.sold.by",DashboardService.UnknownCost::seller));unknown.getColumns().add(Ui.column("column.total",r->currency.getValue().format(r.revenue())));
        Label heading=new Label(LocaleManager.get("profit.title"));heading.getStyleClass().add("section-title");profitBox.getChildren().addAll(heading,range,profitStatus,totals,comparison,new Label(LocaleManager.get("cost.unknown")),unknown);
        period.setOnAction(e->{updateDates();Ui.run(this::refreshProfit);});currency.setOnAction(e->Ui.run(this::refresh));updateDates();
    }
    private void updateDates(){boolean custom=period.getValue()==ProfitPeriod.CUSTOM;start.setDisable(!custom);end.setDisable(!custom);if(!custom){var r=period.getValue().range(LocalDate.now(),null,null);start.setValue(r.start());end.setValue(r.end());}}
    private VBox card(String key,Label value,Label status){value.getStyleClass().add("kpi-value");status.getStyleClass().add("kpi-status");VBox card=new VBox(8,new Label(LocaleManager.get(key)),value,status);card.getStyleClass().add("kpi-card");card.setPadding(new Insets(18));HBox.setHgrow(card,Priority.ALWAYS);card.setMaxWidth(Double.MAX_VALUE);return card;}
    private void styleChart(XYChart<String,Number> chart,String title){chart.setTitle(LocaleManager.get(title));chart.setLegendVisible(false);chart.setAnimated(false);chart.setMinWidth(300);}
    public void refresh(){
        if(currency.getValue()==null)return;var rows=service.metrics(LocalDate.now(),currency.getValue());revenue.setText(rows.currency().format(rows.revenue()));transactions.setText(Long.toString(rows.transactions()));
        items.setText(rows.pieces()+" "+Unit.PIECE.label()+" · "+Quantities.format(Unit.KG.fromStored(rows.grams()),Unit.KG));low.setText(Long.toString(rows.lowStock()));
        grade.setText(LocaleManager.get(rows.lowStock()==0?"grade.good":rows.lowStock()<=3?"grade.watch":"grade.action"));grade.getStyleClass().removeAll("status-good","status-watch","status-action");grade.getStyleClass().add(rows.lowStock()==0?"status-good":rows.lowStock()<=3?"status-watch":"status-action");
        pie.setData(FXCollections.observableArrayList(rows.byPayment().entrySet().stream().filter(e->e.getValue().signum()>0).map(e->new PieChart.Data(LocaleManager.get("LEGACY".equals(e.getKey())?"payment.legacy":"payment."+e.getKey().toLowerCase(Locale.ROOT))+" · "+rows.currency().format(e.getValue()),e.getValue().doubleValue())).toList()));
        pieEmpty.setVisible(pie.getData().isEmpty());
        XYChart.Series<String,Number> bars=new XYChart.Series<>(),line=new XYChart.Series<>();for(var e:rows.byDay().entrySet()){String label=e.getKey().toString().substring(5);bars.getData().add(new XYChart.Data<>(label,e.getValue()));line.getData().add(new XYChart.Data<>(label,e.getValue()));}daily.getData().setAll(List.of(bars));trend.getData().setAll(List.of(line));
        boolean allowed=service.canViewProfit();profitBox.setVisible(allowed);profitBox.setManaged(allowed);if(allowed)refreshProfit();
    }
    private void refreshProfit(){
        if(currency.getValue()==null||!service.canViewProfit())return;
        var result=service.profit(period.getValue(),LocalDate.now(),start.getValue(),end.getValue(),currency.getValue());profitRevenue.setText(result.currency().format(result.revenue()));cost.setText(result.currency().format(result.cost()));profit.setText(result.currency().format(result.grossProfit()));margin.setText(result.margin()==null?"—":result.margin().toPlainString()+"%");
        profitStatus.setText(result.incomplete()?LocaleManager.get("profit.incomplete",result.unknown().size(),result.currency().format(result.unknownRevenue())):LocaleManager.get("profit.complete"));profitStatus.getStyleClass().removeAll("status-good","status-watch");profitStatus.getStyleClass().add(result.incomplete()?"status-watch":"status-good");
        XYChart.Series<String,Number> series=new XYChart.Series<>();series.getData().add(new XYChart.Data<>(LocaleManager.get("profit.known.revenue"),result.knownRevenue()));series.getData().add(new XYChart.Data<>(LocaleManager.get("profit.cogs"),result.cost()));series.getData().add(new XYChart.Data<>(LocaleManager.get("profit.gross"),result.grossProfit()));comparison.getData().setAll(List.of(series));unknown.setItems(FXCollections.observableArrayList(result.unknown()));
    }
}
