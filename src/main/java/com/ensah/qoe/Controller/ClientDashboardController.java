package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientDashboardController implements Initializable {

    private User currentUser;

    @FXML
    private Label usernameLabel;

    public void setUserData(User user) {
        this.currentUser = user;
        if (usernameLabel != null && user != null) {
            usernameLabel.setText(user.getUsername());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("✅ ClientDashboardController initialisé");

        if (currentUser != null && usernameLabel != null) {
            usernameLabel.setText(currentUser.getUsername());
        }
    }

    // ==================== MÉTHODES DE NAVIGATION ====================

    @FXML
    private void showDashboard() {
        System.out.println("📊 Navigation vers Tableau de Bord");
    }

    @FXML
    private void showFeedbackForm() {
        System.out.println("📝 Navigation vers Saisie de Feedback");
    }

    @FXML
    private void showServiceTypes() {
        System.out.println("🎯 Navigation vers Types de Service");
    }

    @FXML
    private void showFeedbackHistory() {
        System.out.println("📋 Navigation vers Historique des Feedbacks");
    }

    @FXML
    private void showQoSMetrics() {
        System.out.println("📊 Navigation vers Métriques QoS");
    }

    @FXML
    private void showAuthentication() {
        System.out.println("🔐 Navigation vers Authentification");
    }

    @FXML
    private void handleLogout() {
        System.out.println("🚪 Déconnexion du client: " +
                (currentUser != null ? currentUser.getUsername() : "Unknown"));
        // Implémentez la logique de déconnexion
    }
}