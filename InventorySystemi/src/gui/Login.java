package gui;

import dialogs.PasswordDialog;
import services.AuthService;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class Login {
    private javafx.animation.Timeline countdown;
    private final AuthService auth=new AuthService();
    public void showLogin(Stage stage) {
        if(countdown!=null)countdown.stop();
        if(!dialogs.LegalDialog.requireAcceptance())return;
        AuthService.clearSession();
        if(!auth.hasUsers()){setup(stage);return;}
        Branding.apply(stage);
        stage.setTitle(LocaleManager.get("login.title")); stage.setMaximized(false);
        TextField username=new TextField(); username.setId("loginUsername");
        PasswordField password=new PasswordField(); password.setId("loginPassword");
        Label title=new Label(LocaleManager.get("login.heading")); title.setStyle("-fx-font-size:24; -fx-font-weight:bold;");
        title.setGraphic(Branding.logo(112));title.setGraphicTextGap(18);title.setWrapText(true);
        Button signIn=Ui.button("login.submit",()->{
            AuthService.User user=auth.login(username.getText(),password.getText()); password.clear();
            if(user.mustChangePassword() && !PasswordDialog.change(true)) { auth.logout(); return; }
            if(services.ShopSettings.current().currency()==null){if(!user.isAdmin())throw AuthService.failure("currency.required");dialogs.ShopSettingsDialog.show(true);}
            if(countdown!=null)countdown.stop();
            new Dashboard().showDashboard(stage,user.username(),user.role());
        }); signIn.setId("signIn"); signIn.setDefaultButton(true);
        Button setup=Ui.button("login.setup.admin",()->setup(stage));
        boolean firstRun=!auth.hasUsers();
        setup.setVisible(firstRun);setup.setManaged(firstRun);signIn.setDisable(firstRun);
        ComboBox<String> language=new ComboBox<>(); language.getItems().addAll("English","Türkçe");
        language.setValue("tr".equals(LocaleManager.getLanguage())?"Türkçe":"English");
        language.setOnAction(e->{LocaleManager.setLanguage("Türkçe".equals(language.getValue())?"tr":"en");showLogin(stage);});
        GridPane grid=new GridPane(); grid.setAlignment(Pos.CENTER); grid.setPadding(new Insets(30)); grid.setHgap(15); grid.setVgap(15);
        grid.add(title,0,0,2,1); grid.addRow(1,new Label(LocaleManager.get("login.username")),username);
        grid.addRow(2,new Label(LocaleManager.get("login.password")),password); grid.add(signIn,1,3);
        Button change=Ui.button("password.change",PasswordDialog::beforeLogin);change.setId("loginChangePassword");
        Button forgot=Ui.button("password.forgot",dialogs.RecoveryDialog::forgot);forgot.setId("forgotPassword");
        grid.add(new HBox(12,change,forgot),0,7,2,1);
        Label locked=new Label();locked.setId("lockCountdown");grid.add(locked,0,8,2,1);
        countdown=new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1),e->{
            long seconds=auth.lockSeconds(username.getText());locked.setText(seconds>0?LocaleManager.get("login.countdown",seconds):"");signIn.setDisable(firstRun||seconds>0);
        }));countdown.setCycleCount(javafx.animation.Timeline.INDEFINITE);countdown.play();
        grid.addRow(4,new Label(LocaleManager.get("language.label")),language); grid.add(setup,1,5);
        Label offline=new Label(LocaleManager.get(firstRun?"setup.first.run.hint":"offline.login.hint"));offline.setWrapText(true);offline.setMaxWidth(440);grid.add(offline,0,6,2,1);
        Ui.style(grid);
        stage.setScene(new Scene(grid,720,650)); stage.setMinWidth(600); stage.setMinHeight(500); stage.show();
        stage.sceneProperty().addListener((o,a,b)->{if(b!=grid.getScene()&&countdown!=null)countdown.stop();});
        stage.addEventHandler(javafx.stage.WindowEvent.WINDOW_HIDDEN,e->{if(countdown!=null)countdown.stop();});
        stage.setOnCloseRequest(e->{if(countdown!=null)countdown.stop();auth.logout();});
    }
    public void setup(Stage stage) {
        Dialog<Boolean> dialog=new Dialog<>(); dialog.initOwner(stage);
        Ui.style(dialog.getDialogPane());
        dialog.setTitle(LocaleManager.get("login.setup.title")); dialog.setHeaderText(LocaleManager.get("login.setup.header"));
        TextField username=new TextField();username.setId("setupUsername"); PasswordField password=new PasswordField(), confirm=new PasswordField();password.setId("setupPassword");confirm.setId("setupConfirm");
        GridPane grid=new GridPane(); grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));
        grid.addRow(0,new Label(LocaleManager.get("login.username")),username);
        grid.addRow(1,new Label(LocaleManager.get("login.password")),password);
        grid.addRow(2,new Label(LocaleManager.get("password.confirm")),confirm);
        dialog.getDialogPane().setContent(grid);
        ButtonType save=new ButtonType(LocaleManager.get("button.create"),ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().lookupButton(save).setId("createAdministrator");
        dialog.getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION,e->{
            try {
                if(!password.getText().equals(confirm.getText())) throw AuthService.failure("password.mismatch");
                String recovery=auth.createFirstAdmin(username.getText(),password.getText());
                dialogs.RecoveryDialog.saveCode(recovery);
                auth.login(username.getText(),password.getText());
                try{dialogs.ShopSettingsDialog.show(true);}finally{auth.logout();}
            } catch(RuntimeException ex) { e.consume(); Ui.error(ex.getMessage()); }
        });
        dialog.setResultConverter(b->b==save);
        if(dialog.showAndWait().orElse(false)) { Ui.info(LocaleManager.get("login.setup.created")); showLogin(stage); }
        else new Welcome().show(stage);
    }
}
