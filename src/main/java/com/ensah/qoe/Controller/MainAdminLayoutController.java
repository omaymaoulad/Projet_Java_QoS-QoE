package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.User;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ParallelTransition;
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

    // Charger le tableau de bord par défaut au démarrage
    @FXML
    public void initialize() {
        loadView("/fxml/admin_dashboard.fxml");

        // Configuration du ScrollPane
        if (menuScrollPane != null) {
            // Rendre le scroll plus fluide
            menuScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            menuScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            menuScrollPane.setFitToWidth(true);
            menuScrollPane.setPannable(false);

            // Ajuster la vitesse de défilement avec la molette de la souris
            menuScrollPane.setOnScroll(event -> {
                double deltaY = event.getDeltaY() * 0.01; // Ajuster le facteur de vitesse
                menuScrollPane.setVvalue(menuScrollPane.getVvalue() - deltaY);
            });
        }
    }

    // Effet hover pour les boutons du menu principal
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
        // Vérifier si c'est le bouton Dashboard (avec effet actif)
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

    // Effet hover pour les sous-menus
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

    // Effet hover pour le bouton logout
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

    // Méthode générique pour charger une vue
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Animation de transition
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
        }
    }

    // Toggle sous-menu avec animation
    @FXML
    private void togglePredictionsSubmenu() {
        boolean isVisible = predictionsSubmenu.isVisible();

        // Animation de rotation de l'indicateur
        RotateTransition rotateTransition = new RotateTransition(Duration.millis(300), submenuIndicator);
        rotateTransition.setToAngle(isVisible ? 0 : 90);

        // Animation de fade pour le sous-menu
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

        // Jouer les animations en parallèle
        ParallelTransition parallelTransition = new ParallelTransition(rotateTransition, fadeTransition);
        parallelTransition.play();
    }

    // Liens entre le menu et les vues dynamiques
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
        loadView("/fxml/SystemSettingView.fxml");
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
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ml-dashboard-anomalies.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Dashboard Détection d'Anomalies QoS");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Ouverture échouée",
                    "Impossible d'ouvrir le dashboard anomalies: " + e.getMessage());
        }
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

                // Animation de fermeture
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

            // Animation d'ouverture
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

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        if (usernameLabel != null && currentUser != null) {
            usernameLabel.setText(currentUser.getUsername());
        }
    }
}
