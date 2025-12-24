package dialogs;

import gui.LocaleManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class ConfirmDialog {

    public static boolean show(String titleKey, String headerKey, String contentKey, Object... args) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(LocaleManager.get(titleKey));
        confirm.setHeaderText(LocaleManager.get(headerKey));
        confirm.setContentText(LocaleManager.get(contentKey, args));

        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // Overload for simple yes/no without header
    public static boolean showSimple(String titleKey, String contentKey, Object... args) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(LocaleManager.get(titleKey));
        confirm.setHeaderText(null);
        confirm.setContentText(LocaleManager.get(contentKey, args));

        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}