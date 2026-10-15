package main;

import gui.Login;
import javafx.application.Application;
import javafx.stage.Stage;
import database.DatabaseInitializer;
import javafx.scene.control.Alert;
import gui.LocaleManager;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(LocaleManager.get("alert.error"));
            alert.setHeaderText(null);
            alert.setContentText(LocaleManager.get("database.connection.failed"));
            alert.showAndWait();
            return;
        }
        Login login = new Login();
        login.showLogin(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
