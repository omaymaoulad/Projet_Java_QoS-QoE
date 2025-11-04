package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
public class LoginController {
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField usernameField;
    @FXML
    private Label errorLabel;
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs !");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("✅ Connecté à : " + conn.getMetaData().getURL());
            System.out.println("👤 Utilisateur connecté Oracle : " + conn.getMetaData().getUserName());
            String query = "SELECT * FROM UTILISATEURS WHERE LOWER(username) = LOWER(?) AND password = ?";

            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username.trim());
            ps.setString(2, password.trim());
            System.out.println("🔍 Tentative de connexion avec : " + username + " / " + password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("✅ Utilisateur trouvé !");
                User user = new User(
                        rs.getInt("id_user"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("role")
                );

                // Afficher le rôle
                System.out.println("✅ Connecté en tant que : " + user.getRole());

                // Ouvrir la page correspondante
                openDashboard(user);
            } else {
                System.out.println("❌ Aucun utilisateur correspondant trouvé.");
                errorLabel.setText("Nom d'utilisateur ou mot de passe incorrect !");
            }

        } catch (Exception e) {
            errorLabel.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openDashboard(User user) {
        try {
            FXMLLoader loader;
            if (user.getRole().equals("admin")) {
                loader = new FXMLLoader(getClass().getResource("/fxml/admin.fxml"));
            } else {
                loader = new FXMLLoader(getClass().getResource("/fxml/client.fxml"));
            }

            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Bienvenue " + user.getUsername());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
