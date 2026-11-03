package main;

import database.DatabaseInitializer;
import gui.*;
import services.AuthService;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override public void start(Stage stage) {
        try {
            DatabaseInitializer.initialize();
            new Login().showLogin(stage);
        } catch(Exception e) { Ui.error(LocaleManager.get("database.connection.failed")); }
    }
    @Override public void stop() { new AuthService().logout(); }
    public static void main(String[] args) { launch(args); }
}
