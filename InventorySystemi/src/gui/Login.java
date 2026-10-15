package gui;

import database.DBConnection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Login {
    // These fields are no longer needed here — we pass them directly!
    //private String currentUser;
   // private String currentRole;

    public void showLogin(Stage primaryStage) {
        primaryStage.setTitle(LocaleManager.get("login.title"));
    

        Label titleLabel = new Label(LocaleManager.get("login.heading"));
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label userLabel = new Label(LocaleManager.get("login.username"));
        TextField userField = new TextField();
        userField.setPromptText(LocaleManager.get("login.username.prompt"));

        Label passLabel = new Label(LocaleManager.get("login.password"));
        PasswordField passField = new PasswordField();
        passField.setPromptText(LocaleManager.get("login.password.prompt"));

        Button loginButton = new Button(LocaleManager.get("login.submit"));
        ComboBox<String> language = new ComboBox<>();
        language.getItems().addAll("English", "Türkçe");
        language.setValue("tr".equals(LocaleManager.getLanguage()) ? "Türkçe" : "English");
        language.setOnAction(e -> {
            LocaleManager.setLanguage("Türkçe".equals(language.getValue()) ? "tr" : "en");
            showLogin(primaryStage);
        });
        loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14;");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(30));

        grid.add(titleLabel, 0, 0, 2, 1);
        grid.add(userLabel, 0, 1);
        grid.add(userField, 1, 1);
        grid.add(passLabel, 0, 2);
        grid.add(passField, 1, 2);
        grid.add(loginButton, 1, 3);
        grid.add(new Label(LocaleManager.get("language.label")), 0, 4);
        grid.add(language, 1, 4);
        Button setupAdminButton = new Button(LocaleManager.get("login.setup.admin"));
        setupAdminButton.setOnAction(e -> setupFirstAdmin());
        grid.add(setupAdminButton, 1, 5);

        loginButton.setOnAction(e -> {
            String username = userField.getText().trim();
            String password = passField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, LocaleManager.get("alert.warning"), LocaleManager.get("login.required"));
                return;
            }

            String[] userInfo = authenticate(username, password);
            if (userInfo != null && "DB_ERROR".equals(userInfo[1])) return;
            if (userInfo != null) {
                // userInfo[0] = username, userInfo[1] = role
                Dashboard dashboard = new Dashboard();
                dashboard.showDashboard(primaryStage, userInfo[0], userInfo[1]);
                userField.clear();
                passField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, LocaleManager.get("alert.error"), LocaleManager.get("login.failed"));
            }
        });

        Scene scene = new Scene(grid, 420, 320);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    private void setupFirstAdmin() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(LocaleManager.get("login.setup.title"));
        dialog.setHeaderText(LocaleManager.get("login.setup.header"));
        ButtonType create = new ButtonType(LocaleManager.get("login.setup.submit"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);

        TextField username = new TextField();
        PasswordField password = new PasswordField();
        PasswordField confirm = new PasswordField();
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.addRow(0, new Label(LocaleManager.get("login.username")), username);
        fields.addRow(1, new Label(LocaleManager.get("login.password")), password);
        fields.addRow(2, new Label(LocaleManager.get("login.setup.confirm")), confirm);
        dialog.getDialogPane().setContent(fields);
        dialog.setResultConverter(button -> button);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != create) return;
        if (username.getText().isBlank() || password.getText().length() < 8
                || !password.getText().equals(confirm.getText())) {
            showAlert(Alert.AlertType.WARNING, LocaleManager.get("alert.warning"), LocaleManager.get("login.setup.invalid"));
            return;
        }

        String countSql = "SELECT COUNT(*) FROM users";
        String insertSql = "INSERT INTO users (username, password, role) VALUES (?, ?, 'admin')";
        try (Connection connection = DBConnection.connect();
             PreparedStatement count = connection.prepareStatement(countSql);
             ResultSet result = count.executeQuery()) {
            result.next();
            if (result.getInt(1) != 0) {
                showAlert(Alert.AlertType.INFORMATION, LocaleManager.get("alert.information"), LocaleManager.get("login.setup.exists"));
                return;
            }
            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                insert.setString(1, username.getText().trim());
                insert.setString(2, password.getText());
                insert.executeUpdate();
            }
            showAlert(Alert.AlertType.INFORMATION, LocaleManager.get("alert.success"), LocaleManager.get("login.setup.created"));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, LocaleManager.get("alert.error"), LocaleManager.get("login.setup.failed"));
        }
    }

    // Now returns both username and role as String array
    private String[] authenticate(String username, String password) {
        String sql = "SELECT username, role FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String foundUsername = rs.getString("username");
                String role = rs.getString("role");
                return new String[]{foundUsername, role != null ? role : "Kullanıcı"};
            }
        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.ERROR, LocaleManager.get("alert.error"), LocaleManager.get("database.connection.failed"));
            return new String[]{"", "DB_ERROR"};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Login failed
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
