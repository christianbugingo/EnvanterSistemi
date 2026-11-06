package dialogs;

import gui.*;
import services.AuthService;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;

public final class RecoveryDialog {
    private RecoveryDialog() { }
    public static void saveCode(String code) {
        Dialog<Void> dialog=new Dialog<>();Ui.style(dialog.getDialogPane());dialog.setTitle(LocaleManager.get("recovery.title"));
        dialog.setHeaderText(LocaleManager.get("recovery.save"));
        TextArea value=new TextArea(code);value.setEditable(false);value.setWrapText(true);value.setPrefRowCount(3);value.setId("recoveryCode");
        CheckBox saved=new CheckBox(LocaleManager.get("recovery.saved"));saved.setId("recoverySaved");
        Button print=Ui.button("recovery.print",()->{javafx.print.PrinterJob job=javafx.print.PrinterJob.createPrinterJob();if(job!=null&&job.showPrintDialog(dialog.getDialogPane().getScene().getWindow())){if(job.printPage(new Label(code)))job.endJob();}});
        VBox box=new VBox(15,value,print,saved);box.setPadding(new Insets(20));dialog.getDialogPane().setContent(box);
        ButtonType done=new ButtonType(LocaleManager.get("button.continue"),ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().add(done);
        dialog.getDialogPane().lookupButton(done).disableProperty().bind(saved.selectedProperty().not());
        dialog.setOnCloseRequest(e->{if(!saved.isSelected())e.consume();});dialog.showAndWait();value.clear();
    }
    public static void forgot() {
        Dialog<Boolean> dialog=new Dialog<>();Ui.style(dialog.getDialogPane());dialog.setTitle(LocaleManager.get("password.forgot"));
        dialog.setHeaderText(LocaleManager.get("recovery.help"));
        TextField username=new TextField();username.setId("recoveryUsername");PasswordField code=new PasswordField(),replacement=new PasswordField(),confirm=new PasswordField();
        code.setId("recoveryInput");replacement.setId("recoveryTemporary");confirm.setId("recoveryConfirm");
        GridPane grid=new GridPane();grid.setHgap(12);grid.setVgap(12);grid.setPadding(new Insets(20));
        grid.addRow(0,new Label(LocaleManager.get("login.username")),username);grid.addRow(1,new Label(LocaleManager.get("recovery.code")),code);
        grid.addRow(2,new Label(LocaleManager.get("password.temporary")),replacement);grid.addRow(3,new Label(LocaleManager.get("password.confirm")),confirm);
        Label status=new Label();status.setWrapText(true);status.setMaxWidth(500);grid.add(status,0,4,2,1);
        dialog.getDialogPane().setContent(grid);ButtonType reset=new ButtonType(LocaleManager.get("recovery.reset"),ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(reset,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().lookupButton(reset).setId("recoverAccount");
        dialog.getDialogPane().lookupButton(reset).addEventFilter(ActionEvent.ACTION,e->{
            try {
                AuthService auth=new AuthService();if(!auth.isAdministrator(username.getText()))throw AuthService.failure("recovery.staff");
                if(!replacement.getText().equals(confirm.getText()))throw AuthService.failure("password.mismatch");
                String next=auth.recoverAdministrator(username.getText(),code.getText(),replacement.getText());saveCode(next);
            }catch(RuntimeException ex){e.consume();status.setText(ex.getMessage());}
        });dialog.showAndWait();
    }
}
