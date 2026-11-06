package gui;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.*;
/** Local JavaFX artwork and bundled branding; no web content or network assets. */
public final class Welcome {
    public void show(Stage stage){
        services.AuthService.clearSession();Branding.apply(stage);stage.setTitle("Inventory System · COODIC LABS");
        Label title=new Label(LocaleManager.get("home.title"));title.getStyleClass().add("home-title");
        Label subtitle=new Label(LocaleManager.get("home.subtitle"));subtitle.setWrapText(true);subtitle.setMaxWidth(650);
        HBox cards=new HBox(18,card("01","home.stock"),card("02","home.sales"),card("03","home.reports"));cards.setAlignment(Pos.CENTER);
        boolean fresh=!new services.AuthService().hasUsers();Button start=Ui.button(fresh?"home.create":"login.submit",()->{if(dialogs.LegalDialog.requireAcceptance()){Login login=new Login();login.showLogin(stage);}});start.setId("homeStart");start.setDefaultButton(true);
        Label support=new Label("COODIC LABS");support.getStyleClass().add("home-footer");
        VBox root=new VBox(22,Branding.logo(150),title,subtitle,cards,start,Ui.button("legal.full",dialogs.LegalDialog::view),support);root.setAlignment(Pos.CENTER);root.setPadding(new Insets(40));Ui.style(root);root.getStyleClass().add("welcome");
        stage.setScene(new Scene(root,1000,720));stage.setMinWidth(850);stage.setMinHeight(650);stage.show();
    }
    private VBox card(String number,String key){Label n=new Label(number);n.getStyleClass().add("step-number");Label text=new Label(LocaleManager.get(key));text.setWrapText(true);text.setMaxWidth(210);VBox card=new VBox(12,n,text);card.getStyleClass().add("step-card");card.setPadding(new Insets(22));card.setPrefWidth(250);return card;}
}
