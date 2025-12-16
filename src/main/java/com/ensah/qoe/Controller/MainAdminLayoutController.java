package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.User;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.io.IOException;

public class MainAdminLayoutController {
    private User currentUser;

    @FXML
    private Label usernameLabel;

    @FXML
    private AnchorPane contentArea;

    @FXML
    private VBox predictionsSubmenu;

    @FXML
    private Label submenuIndicator;

    @FXML
    private Button dashboardBtn;

    @FXML
    private ScrollPane menuScrollPane;

    @FXML
    public void initialize() {
        System.out.println("🔄 MainAdminLayoutController - initialize() called");
        loadView("/fxml/admin_dashboard.fxml");

        if (menuScrollPane != null) {
            menuScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            menuScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            menuScrollPane.setFitToWidth(true);
            menuScrollPane.setPannable(false);

            menuScrollPane.setOnScroll(event -> {
                double deltaY = event.getDeltaY() * 0.01;
                menuScrollPane.setVvalue(menuScrollPane.getVvalue() - deltaY);
            });
        }
    }

    public void setCurrentUser(User user) {
        System.out.println("═══════════════════════════════════════");
        System.out.println("🔥 setCurrentUser() called");

        if (user != null) {
            this.currentUser = user;
            System.out.println("✅ User set: " + user.getUsername());
            System.out.println("   - ID: " + user.getIdUser());
            System.out.println("   - Email: " + user.getEmail());

            if (usernameLabel != null) {
                usernameLabel.setText(user.getUsername());
                System.out.println("✅ Username label updated");
            }
        } else {
            System.err.println("❌ User is NULL in setCurrentUser!");
        }
        System.out.println("═══════════════════════════════════════");
    }

    public User getCurrentUser() {
        return currentUser;
    }

    // 🔥 MÉTHODE CORRIGÉE : Utilise contentArea au lieu de mainContainer
    @FXML
    public void loadProfileView() {
        System.out.println("═══════════════════════════════════════");
        System.out.println("🔥 loadProfileView() called");
        System.out.println("   - currentUser: " + (currentUser != null ? currentUser.getUsername() : "NULL"));

        if (currentUser == null) {
            System.err.println("❌ CRITICAL ERROR: currentUser is NULL!");
            showAlert("Erreur de session", "Utilisateur non connecté",
                    "Veuillez vous reconnecter.");
            return;
        }

        try {
            System.out.println("🔄 Loading profile FXML...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminProfileView.fxml"));
            Parent profileView = loader.load();
            System.out.println("✅ FXML loaded successfully");

            AdminProfileController profileController = loader.getController();
            System.out.println("✅ AdminProfileController obtained");

            System.out.println("🔄 Passing user to profile controller...");
            profileController.setMainController(this);
            profileController.setCurrentUser(this.currentUser);
            System.out.println("✅ User data passed to profile controller");

            // 🔥 CORRECTION : Utiliser contentArea au lieu de mainContainer
            if (contentArea != null) {
                contentArea.getChildren().setAll(profileView);
                AnchorPane.setTopAnchor(profileView, 0.0);
                AnchorPane.setBottomAnchor(profileView, 0.0);
                AnchorPane.setLeftAnchor(profileView, 0.0);
                AnchorPane.setRightAnchor(profileView, 0.0);
                System.out.println("✅ Profile view displayed in contentArea");
            } else {
                System.err.println("❌ contentArea is NULL!");
            }

        } catch (Exception e) {
            System.err.println("❌ ERROR loading profile:");
            e.printStackTrace();
            showAlert("Erreur", "Erreur de chargement",
                    "Impossible de charger le profil: " + e.getMessage());
        }
        System.out.println("═══════════════════════════════════════");
    }

    public void handleMenuHover(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-background-color: rgba(52, 152, 219, 0.2); " +
                "-fx-background-radius: 10; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14; " +
                "-fx-font-weight: 600; " +
                "-fx-alignment: center-left; " +
                "-fx-padding: 15 20; " +
                "-fx-border-color: rgba(52, 152, 219, 0.4); " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.3), 10, 0.5, 0, 2);");
    }

    public void handleMenuExit(MouseEvent event) {
        Button btn = (Button) event.getSource();
        if (btn.getId() != null && btn.getId().equals("dashboardBtn")) {
            btn.setStyle("-fx-background-color: rgba(52, 152, 219, 0.15); " +
                    "-fx-background-radius: 10; " +
                    "-fx-text-fill: #ecf0f1; " +
                    "-fx-font-size: 14; " +
                    "-fx-font-weight: 600; " +
                    "-fx-alignment: center-left; " +
                    "-fx-padding: 15 20; " +
                    "-fx-border-color: rgba(52, 152, 219, 0.3); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 10; " +
                    "-fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; " +
                    "-fx-background-radius: 10; " +
                    "-fx-text-fill: #ecf0f1; " +
                    "-fx-font-size: 14; " +
                    "-fx-font-weight: 500; " +
                    "-fx-alignment: center-left; " +
                    "-fx-padding: 15 20; " +
                    "-fx-cursor: hand;");
        }
    }

    public void handleSubmenuHover(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-background-color: rgba(52, 152, 219, 0.15); " +
                "-fx-background-radius: 8; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 13; " +
                "-fx-font-weight: 600; " +
                "-fx-alignment: center-left; " +
                "-fx-padding: 12 15; " +
                "-fx-cursor: hand;");
    }

    public void handleSubmenuExit(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-background-color: transparent; " +
                "-fx-background-radius: 8; " +
                "-fx-text-fill: #bdc3c7; " +
                "-fx-font-size: 13; " +
                "-fx-font-weight: 500; " +
                "-fx-alignment: center-left; " +
                "-fx-padding: 12 15; " +
                "-fx-cursor: hand;");
    }

    public void handleLogoutHover(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-background-color: linear-gradient(to right, #c0392b, #e74c3c); " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 10; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 13; " +
                "-fx-padding: 12; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(231, 76, 60, 0.6), 15, 0.4, 0, 3);");
    }

    public void handleLogoutExit(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-background-color: linear-gradient(to right, #e74c3c, #c0392b); " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 10; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 13; " +
                "-fx-padding: 12; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(231, 76, 60, 0.4), 10, 0.3, 0, 2);");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            FadeTransition fadeTransition = new FadeTransition(Duration.millis(300), view);
            fadeTransition.setFromValue(0.0);
            fadeTransition.setToValue(1.0);

            contentArea.getChildren().setAll(view);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);

            fadeTransition.play();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur de chargement",
                    "Impossible de charger la vue: " + e.getMessage());
        }
    }

    @FXML
    private void togglePredictionsSubmenu() {
        boolean isVisible = predictionsSubmenu.isVisible();

        RotateTransition rotateTransition = new RotateTransition(Duration.millis(300), submenuIndicator);
        rotateTransition.setToAngle(isVisible ? 0 : 90);

        FadeTransition fadeTransition = new FadeTransition(Duration.millis(300), predictionsSubmenu);

        if (!isVisible) {
            predictionsSubmenu.setVisible(true);
            predictionsSubmenu.setManaged(true);
            fadeTransition.setFromValue(0.0);
            fadeTransition.setToValue(1.0);
        } else {
            fadeTransition.setFromValue(1.0);
            fadeTransition.setToValue(0.0);
            fadeTransition.setOnFinished(e -> {
                predictionsSubmenu.setVisible(false);
                predictionsSubmenu.setManaged(false);
            });
        }

        ParallelTransition parallelTransition = new ParallelTransition(rotateTransition, fadeTransition);
        parallelTransition.play();
    }

    @FXML
    private void loadAdminDashboardView() {
        loadView("/fxml/admin_dashboard.fxml");
    }

    @FXML
    private void loadQoSView() {
        loadView("/fxml/qos.fxml");
    }

    @FXML
    private void loadQoEView() {
        loadView("/fxml/qoe.fxml");
    }

    @FXML
    private void showNetworkMonitor() {
        loadView("/fxml/NetworkMonitorView.fxml");
    }

    @FXML
    private void showUserManagement() {
        loadView("/fxml/UserManagementView.fxml");
    }

    @FXML
    private void showReports() {
        loadView("/fxml/ReportsView.fxml");
    }

    @FXML
    private void showSystemSettings() {
        System.out.println("🔄 showSystemSettings() called");
        System.out.println("   - Loading: /fxml/SystemSettingsView.fxml");

        try {
            loadView("/fxml/SystemSettingsView.fxml");
            System.out.println("✅ SystemSettingsView loaded successfully");
        } catch (Exception e) {
            System.err.println("❌ ERROR loading SystemSettingsView:");
            e.printStackTrace();
            showAlert("Erreur", "Erreur de chargement",
                    "Impossible de charger les paramètres système: " + e.getMessage());
        }
    }
    @FXML
    private void showAdminProfile() {
        loadProfileView();
    }

    @FXML
    private void openMLDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ml-dashboard.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Dashboard Machine Learning QoE/QoS");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur d'ouverture",
                    "Impossible d'ouvrir le dashboard ML: " + e.getMessage());
        }
    }

    @FXML
    private void openMLDashboardAnomalies() {
        loadView("/fxml/ml-dashboard-anomalies.fxml");
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

                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    stage.close();
                    openLoginWindow();
                });
                fadeOut.play();
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

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            loginStage.show();
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur de connexion",
                    "Impossible d'ouvrir la fenêtre de connexion: " + e.getMessage());
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}