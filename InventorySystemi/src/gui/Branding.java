package gui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.util.Objects;

/** Loads branding from the app JAR, never from Downloads or the network. */
public final class Branding {
    private static final Image LOGO=new Image(Objects.requireNonNull(Branding.class.getResource("/branding/inventory-logo.png")).toExternalForm());
    private Branding() { }
    public static void apply(Stage stage) { stage.getIcons().setAll(LOGO); }
    public static ImageView logo(double size) {
        ImageView view=new ImageView(LOGO); view.setFitWidth(size);view.setFitHeight(size);
        view.setPreserveRatio(true);view.setSmooth(true);view.setAccessibleText("Inventory System");
        return view;
    }
}
