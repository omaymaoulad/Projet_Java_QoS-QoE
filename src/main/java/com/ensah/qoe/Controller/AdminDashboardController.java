package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    private User currentUser;

    @FXML
    private Label usernameLabel;

    public void setUserData(User user) {
        this.currentUser = user;
        if (usernameLabel != null && user != null) {
            usernameLabel.setText("Welcome, " + user.getUsername() + "!");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("✅ AdminDashboardController initialisé");

        if (currentUser != null && usernameLabel != null) {
            usernameLabel.setText("Welcome, " + currentUser.getUsername() + "!");
        }
    }

    // ==================== MÉTHODES DE NAVIGATION ====================

    @FXML
    private void showDashboard() {
        System.out.println("📊 Navigation vers Dashboard");
        showAlert("Info", "Dashboard", "Affichage du tableau de bord principal.");
    }

    @FXML
    private void showQoEAnalysis() {
        System.out.println("🎯 Navigation vers QoE Analysis");
        showAlert("Info", "QoE Analysis", "Ouverture de l'analyse Quality of Experience.");
    }

    @FXML
    private void showNetworkMonitor() {
        System.out.println("🌐 Navigation vers Network Monitor");
        showAlert("Info", "Network Monitor", "Ouverture du moniteur réseau.");
    }

    @FXML
    private void showUserManagement() {
        System.out.println("👥 Navigation vers User Management");
        showAlert("Info", "User Management", "Ouverture de la gestion des utilisateurs.");
    }

    @FXML
    private void showReports() {
        System.out.println("📈 Navigation vers Reports & Analytics");
        showAlert("Info", "Reports", "Ouverture des rapports et analyses.");
    }

    @FXML
    private void showSystemSettings() {
        System.out.println("⚙️ Navigation vers System Settings");
        showAlert("Info", "System Settings", "Ouverture des paramètres système.");
    }

    @FXML
    private void handleLogout() {
        System.out.println("🚪 Déconnexion de l'administrateur: " +
                (currentUser != null ? currentUser.getUsername() : "Unknown"));

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de déconnexion");
        confirmation.setHeaderText("Déconnexion");
        confirmation.setContentText("Êtes-vous sûr de vouloir vous déconnecter ?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                Stage stage = (Stage) usernameLabel.getScene().getWindow();
                stage.close();
                openLoginWindow();
            }
        });
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private void openLoginWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ensah/qoe/views/login.fxml"));
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Connexion - QoE System");
            loginStage.setScene(new Scene(root));
            loginStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Login Error", "Cannot open login window: " + e.getMessage());
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        if (usernameLabel != null && currentUser != null) {
            usernameLabel.setText("Welcome, " + currentUser.getUsername() + "!");
        }
    }

    public void refreshDashboard() {
        System.out.println("🔄 Rafraîchissement du dashboard...");
        if (currentUser != null) {
            System.out.println("Utilisateur actuel: " + currentUser.getUsername());
        }
    }
    @FXML
    private void openQosWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/qos.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Analyse QoS");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void showQoSMetrics() {
        try {
            // Charger le fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/qos.fxml"));
            Parent root = loader.load();

            // Récupérer le contrôleur associé
            QoSController qosController = loader.getController();

            // Créer la scène et la fenêtre
            Stage stage = new Stage();
            stage.setTitle("Analyse QoS");
            stage.setScene(new Scene(root, 600, 400));
            stage.show();

            System.out.println("✅ Fenêtre QoS ouverte avec succès !");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Erreur lors de l'ouverture de QoS.fxml : " + e.getMessage());
        }
    }


    public void onWindowClosing() {
        System.out.println("🔒 Fermeture du dashboard admin");
        if (currentUser != null) {
            System.out.println("Session fermée pour: " + currentUser.getUsername());
        }
    }
}