package com.ensah.qoe.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {

    @FXML private TextField emailField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private VBox messagesContainer;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button resetButton;
    @FXML private Hyperlink backLink;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Masquer les messages au début
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        messagesContainer.setVisible(false);
        progressIndicator.setVisible(false);

        // Configuration des listeners
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Validation en temps réel
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateEmailField();
        });

        // Entrer pour soumettre
        emailField.setOnAction(e -> handleResetPassword());
    }

    private void validateEmailField() {
        String email = emailField.getText().trim();
        boolean isValid = !email.isEmpty() && isValidEmail(email);
        resetButton.setDisable(!isValid);
    }

    @FXML
    private void handleResetPassword() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Veuillez entrer votre adresse email");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Format d'email invalide");
            return;
        }

        // Désactiver le bouton et montrer le loader
        resetButton.setDisable(true);
        progressIndicator.setVisible(true);
        hideMessages();

        // Simuler l'envoi (à remplacer par la logique réelle)
        new Thread(() -> {
            try {
                Thread.sleep(1500); // Simulation de l'envoi

                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    showSuccess("Email envoyé ! Vérifiez votre boîte de réception.");
                    resetButton.setDisable(false);
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    showError("Erreur lors de l'envoi de l'email");
                    resetButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            // Fermer la fenêtre actuelle
            Stage currentStage = (Stage) backLink.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Impossible de revenir à la page de connexion");
            alert.showAndWait();
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
        messagesContainer.setVisible(true);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
        messagesContainer.setVisible(true);
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        messagesContainer.setVisible(false);
    }
}