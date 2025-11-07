package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ResetPasswordController {

    @FXML private PasswordField passwordField;

    private String token; // à récupérer du lien

    public void setToken(String token) {
        this.token = token;
    }

    @FXML
    private void handleResetPassword() {
        String newPassword = passwordField.getText().trim();
        if(newPassword.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez entrer un nouveau mot de passe.").showAndWait();
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String query = "UPDATE utilisateurs SET password = ? , reset_token = NULL WHERE reset_token = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, newPassword);
            pst.setString(2, token);

            int updated = pst.executeUpdate();
            if(updated > 0) {
                new Alert(Alert.AlertType.INFORMATION, "Mot de passe réinitialisé avec succès !").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "Lien invalide ou expiré.").showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la réinitialisation du mot de passe.").showAndWait();
        }
    }
}
