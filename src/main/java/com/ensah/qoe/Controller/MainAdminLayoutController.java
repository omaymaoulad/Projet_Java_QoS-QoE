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

import javafx.scene.layout.AnchorPane;


public class MainAdminLayoutController {
    private User currentUser;

    @FXML
    private Label usernameLabel;
    @FXML
    private AnchorPane contentArea;

    // Charger le tableau de bord par défaut au démarrage
    @FXML
    public void initialize() {
        loadView("/fxml/admin_dashboard.fxml");
    }

    // Méthode générique pour charger une vue
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Liens entre le menu et les vues dynamiques
    @FXML private void loadAdminDashboardView() { loadView("/fxml/admin_dashboard.fxml"); }
    @FXML private void loadQoSView() { loadView("/fxml/qos.fxml"); }
    @FXML private void loadQoEView() { loadView("/fxml/qoe.fxml"); }
    @FXML private void showNetworkMonitor(){}
    @FXML private void showUserManagement(){}
    @FXML private void showReports(){}
    @FXML private void showSystemSettings(){}

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
    private void openLoginWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
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

}

