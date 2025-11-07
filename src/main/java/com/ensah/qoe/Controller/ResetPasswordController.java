package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class ResetPasswordController {

    @FXML private PasswordField passwordField;
    @FXML private TextField tokenField; // Ajoutez ce champ dans votre FXML
    private String token;
    private String email;

    public void setToken(String token) {
        this.token = token;
        if (tokenField != null) tokenField.setText(token); // Afficher le token (optionnel)
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @FXML
    private void handleResetPassword() {
        String newPassword = passwordField.getText().trim();

        // DEBUG
        System.out.println("🔍 DEBUG ResetPasswordController:");
        System.out.println("Token reçu: " + token);
        System.out.println("Nouveau mot de passe: " + newPassword);

        if(newPassword.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez entrer un nouveau mot de passe.").showAndWait();
            return;
        }

        Connection conn = null;
        PreparedStatement pst = null;

        try {
            conn = DBConnection.getConnection();

            if (conn == null || conn.isClosed()) {
                new Alert(Alert.AlertType.ERROR, "Erreur de connexion à la base de données.").showAndWait();
                return;
            }

            // DEBUG: Vérifier si le token existe dans la base
            String checkTokenQuery = "SELECT email FROM utilisateurs WHERE reset_token = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkTokenQuery);
            checkStmt.setString(1, token);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String email = rs.getString("email");
                System.out.println("✅ Token trouvé pour l'email: " + email);
            } else {
                System.out.println("❌ Token NON trouvé dans la base: " + token);
            }
            checkStmt.close();

            // Requête de mise à jour
            String query = "UPDATE utilisateurs SET password = ?, reset_token = NULL WHERE reset_token = ?";
            pst = conn.prepareStatement(query);
            pst.setString(1, newPassword);
            pst.setString(2, token);

            int updated = pst.executeUpdate();

            // DEBUG
            System.out.println("Lignes mises à jour: " + updated);

            if(updated > 0) {
                new Alert(Alert.AlertType.INFORMATION, "Mot de passe réinitialisé avec succès !").showAndWait();
                openLoginWindow();
            } else {
                new Alert(Alert.AlertType.ERROR, "Token invalide ou expiré.").showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la réinitialisation: " + e.getMessage()).showAndWait();
        } finally {
            try {
                if (pst != null) pst.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void openLoginWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ensah/qoe/login.fxml"));
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Connexion");
            loginStage.setScene(new Scene(root));
            loginStage.show();

            // Fermer la fenêtre actuelle
            ((Stage) passwordField.getScene().getWindow()).close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}