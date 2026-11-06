package main;

import database.DatabaseInitializer;
import gui.*;
import services.AuthService;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(Main.class);
    @Override public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            LOG.error("Unhandled application error on {}",thread.getName(),error);
            Runnable alert=()->Ui.error(LocaleManager.get("error.unexpected"));
            if(javafx.application.Platform.isFxApplicationThread())alert.run();else javafx.application.Platform.runLater(alert);
        });
        try {
            DatabaseInitializer.initialize();
            LocaleManager.setLanguage(services.ShopSettings.current().language());
            new Welcome().show(stage);
        } catch(Exception e) { LOG.error("Application startup failed",e); Ui.error(LocaleManager.get("database.connection.failed")); }
    }
    @Override public void stop() { new AuthService().logout(); }
    public static void main(String[] args) { launch(args); }
}
