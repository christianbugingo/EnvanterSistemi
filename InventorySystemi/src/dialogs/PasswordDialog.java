package dialogs;

import gui.*;
import services.AuthService;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;

public final class PasswordDialog {
    public static boolean change(boolean required) {return change(required,false);}
    public static boolean beforeLogin() {return change(false,true);}
    private static boolean change(boolean required,boolean beforeLogin) {
        Dialog<Boolean> dialog=new Dialog<>();
        Ui.style(dialog.getDialogPane());
        dialog.setTitle(LocaleManager.get("password.change"));
        dialog.setHeaderText(LocaleManager.get(required?"password.required":"password.change.hint"));
        PasswordField old=new PasswordField(), replacement=new PasswordField(), confirm=new PasswordField();
        old.setId("currentPassword"); replacement.setId("newPassword"); confirm.setId("confirmPassword");
        GridPane grid=new GridPane(); grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));
        TextField username=new TextField();username.setId("changeUsername");
        if(beforeLogin)grid.addRow(3,new Label(LocaleManager.get("login.username")),username);
        grid.addRow(0,new Label(LocaleManager.get("password.current")),old);
        grid.addRow(1,new Label(LocaleManager.get("password.new")),replacement);
        grid.addRow(2,new Label(LocaleManager.get("password.confirm")),confirm);
        dialog.getDialogPane().setContent(grid);
        ButtonType save=new ButtonType(LocaleManager.get("button.save"),ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save,new ButtonType(LocaleManager.get("button.cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().lookupButton(save).setId("savePassword");
        dialog.getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION,event->{
            try {
                if(!replacement.getText().equals(confirm.getText())) throw AuthService.failure("password.mismatch");
                if(beforeLogin)new AuthService().changePassword(username.getText(),old.getText(),replacement.getText());
                else new AuthService().changePassword(old.getText(),replacement.getText());
            } catch(RuntimeException ex) { event.consume(); Ui.error(ex.getMessage()); }
        });
        dialog.setResultConverter(b->b==save); return dialog.showAndWait().orElse(false);
    }
}
