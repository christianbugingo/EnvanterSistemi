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
        primaryStage.setTitle("Envanter Sistemi - Giriş");
    

        Label titleLabel = new Label("Envanter Sistemi Giriş");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label userLabel = new Label("Kullanıcı Adı:");
        TextField userField = new TextField();
        userField.setPromptText("Kullanıcı adınızı girin");

        Label passLabel = new Label("Şifre:");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Şifrenizi girin");

        Button loginButton = new Button("Giriş Yap");
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

        loginButton.setOnAction(e -> {
            String username = userField.getText().trim();
            String password = passField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen kullanıcı adı ve şifreyi girin.");
                return;
            }

            String[] userInfo = authenticate(username, password);
            if (userInfo != null) {
                // userInfo[0] = username, userInfo[1] = role
                Dashboard dashboard = new Dashboard();
                dashboard.showDashboard(primaryStage, userInfo[0], userInfo[1]);
                userField.clear();
                passField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Giriş Başarısız", "Kullanıcı adı veya şifre yanlış!");
            }
        });

        Scene scene = new Scene(grid, 420, 320);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
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