package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.User;
import com.ensah.qoe.Models.DBConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
import java.util.Properties;

public class LoginController implements Initializable {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordVisibleField;
    @FXML
    private Button togglePasswordButton;
    @FXML
    private Button loginButton;
    @FXML
    private CheckBox rememberMeCheckbox;
    @FXML
    private Label errorLabel;
    @FXML
    private Label successLabel;
    @FXML
    private Label connectionStatusLabel;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Hyperlink forgotPasswordLink; // Ajout du lien pour mot de passe oublié

    private boolean isPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEventHandlers();
        initializeConnectionStatus();
        setupPasswordToggle();
        loadSavedCredentials();
    }

    private void setupEventHandlers() {
        // Real-time field validation
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
        passwordVisibleField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());

        // Enter key to submit form
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        passwordVisibleField.setOnAction(e -> handleLogin());

        // Forgot password link
        if (forgotPasswordLink != null) {
            forgotPasswordLink.setOnAction(e -> handleForgotPassword());
        }
    }

    private void initializeConnectionStatus() {
        // Test database connection
        new Thread(() -> {
            try {
                Connection conn = DBConnection.getConnection();
                if (conn != null && !conn.isClosed()) {
                    javafx.application.Platform.runLater(() -> {
                        connectionStatusLabel.setText("🟢 Database Connected");
                        connectionStatusLabel.setStyle("-fx-text-fill: green;");
                    });
                } else {
                    javafx.application.Platform.runLater(() -> {
                        connectionStatusLabel.setText("🔴 Database Error");
                        connectionStatusLabel.setStyle("-fx-text-fill: red;");
                    });
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    connectionStatusLabel.setText("🔴 Connection Failed");
                    connectionStatusLabel.setStyle("-fx-text-fill: red;");
                });
            }
        }).start();
    }

    private void setupPasswordToggle() {
        // Initial setup
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        togglePasswordButton.setText("👁️");

        togglePasswordButton.setOnAction(e -> togglePasswordVisibility());
    }

    private void loadSavedCredentials() {
        // Charger les identifiants sauvegardés si "Remember Me" était coché précédemment
        // À implémenter selon votre système de préférences
        // Exemple avec Preferences API:
        /*
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        String savedUsername = prefs.get("savedUsername", "");
        boolean rememberMe = prefs.getBoolean("rememberMe", false);

        if (rememberMe && !savedUsername.isEmpty()) {
            usernameField.setText(savedUsername);
            rememberMeCheckbox.setSelected(true);
        }
        */
    }

    private void saveCredentials() {
        // Sauvegarder les identifiants si "Remember Me" est coché
        if (rememberMeCheckbox.isSelected()) {
            /*
            Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
            prefs.put("savedUsername", usernameField.getText().trim());
            prefs.putBoolean("rememberMe", true);
            */
        } else {
            /*
            Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
            prefs.remove("savedUsername");
            prefs.putBoolean("rememberMe", false);
            */
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            // Montrer le mot de passe en clair
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordButton.setText("🔒");

            // Transférer le focus
            passwordVisibleField.requestFocus();
            passwordVisibleField.end();
        } else {
            // Cacher le mot de passe
            passwordField.setText(passwordVisibleField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            togglePasswordButton.setText("👁️");

            // Transférer le focus
            passwordField.requestFocus();
            passwordField.end();
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = isPasswordVisible ? passwordVisibleField.getText() : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        // Sauvegarder les identifiants
        saveCredentials();

        // Disable button and show loader
        loginButton.setDisable(true);
        togglePasswordButton.setDisable(true);
        progressIndicator.setVisible(true);
        hideMessages();

        // Use thread for database operation to avoid freezing UI
        new Thread(() -> {
            try {
                User authenticatedUser = authenticateUser(username, password);
                javafx.application.Platform.runLater(() -> {
                    if (authenticatedUser != null) {
                        showSuccess("Login successful! Redirecting...");
                        redirectToDashboard(authenticatedUser);
                    } else {
                        showError("Invalid username or password");
                        resetLoginButton();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    showError("Authentication error: " + e.getMessage());
                    resetLoginButton();
                });
            }
        }).start();
    }

    private void resetLoginButton() {
        loginButton.setDisable(false);
        togglePasswordButton.setDisable(false);
        progressIndicator.setVisible(false);
    }

    private User authenticateUser(String username, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();

            // Récupérer TOUTES les colonnes nécessaires
            String sql = "SELECT id_user, username, email, role, password, date_creation " +
                    "FROM utilisateurs " +
                    "WHERE username = ? AND TRIM(password) = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            System.out.println("🔍 Executing query for username: " + username);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setIdUser(rs.getInt("id_user"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setPassword(rs.getString("password"));

                // Récupérer la date de création
                String dateCreation = rs.getString("date_creation");
                user.setDateCreation(dateCreation != null ? dateCreation : "");

                System.out.println("✅ User authenticated: " + user.getUsername() +
                        " (Role: " + user.getRole() + ")");
                return user;
            } else {
                System.out.println("❌ Authentication failed for username: " + username);
            }

            return null;

        } catch (Exception e) {
            System.err.println("❌ Error during authentication: " + e.getMessage());
            return null;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.err.println("❌ Error closing resources: " + e.getMessage());
            }
        }
    }

    private void redirectToDashboard(User user) {
        try {
            String fxmlFile;
            String title;

            // Sélectionner le dashboard selon le rôle
            if ("admin".equalsIgnoreCase(user.getRole())) {
                fxmlFile = "/fxml/main_layout_admin.fxml";
                title = "QOS/QOE - Admin Panel (" + user.getUsername() + ")";
            } else {
                fxmlFile = "/fxml/client_dashboard.fxml";
                title = "QOS/QOE Client Dashboard - Welcome " + user.getUsername();
            }

            System.out.println("🔄 Loading FXML: " + fxmlFile);

            URL fxmlUrl = getClass().getResource(fxmlFile);
            if (fxmlUrl == null) {
                throw new IOException("FXML file not found: " + fxmlFile);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent dashboard = loader.load();

            System.out.println("✅ FXML loaded successfully");

            // Passer l'utilisateur au contrôleur approprié
            Object controller = loader.getController();
            if (controller instanceof MainAdminLayoutController) {
                MainAdminLayoutController mainController = (MainAdminLayoutController) controller;
                mainController.setCurrentUser(user);
                System.out.println("✅ User passed to MainAdminLayoutController: " + user.getUsername());
            } else if (controller instanceof ClientDashboardController) {
                ((ClientDashboardController) controller).setUserData(user);
                System.out.println("✅ User data passed to ClientDashboardController");
            }

            // Créer une nouvelle scène
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(dashboard, 1366, 700);

            // Appliquer le CSS si disponible
            try {
                URL cssUrl = getClass().getResource("/css/style.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception e) {
                System.out.println("ℹ️ CSS not found, continuing without styles");
            }

            // Configurer et afficher la nouvelle scène
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();

            System.out.println("✅ Dashboard displayed successfully");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading dashboard: " + e.getMessage());
            resetLoginButton();
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            // Charger le fichier FXML depuis le bon chemin
            FXMLLoader loader = new FXMLLoader();

            // Option 1: Chemin relatif depuis la racine de resources
            URL fxmlUrl = getClass().getResource("/com/ensah/qoe/fxml/forgotPassword.fxml");

            // Option 2: Si le fichier est dans resources/fxml/
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/fxml/forgotPassword.fxml");
            }

            // Option 3: Si le fichier est dans le même package
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("forgotPassword.fxml");
            }

            if (fxmlUrl == null) {
                throw new IOException("Fichier FXML non trouvé : forgotPassword.fxml");
            }

            loader.setLocation(fxmlUrl);
            Parent root = loader.load();

            Stage forgotStage = new Stage();
            forgotStage.setTitle("Mot de passe oublié - QoE System");
            forgotStage.setScene(new Scene(root, 500, 600));
            forgotStage.setResizable(false);

            // Optionnel : fermer la fenêtre de login
            // Stage currentStage = (Stage) loginButton.getScene().getWindow();
            // currentStage.close();

            forgotStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Erreur lors de l'ouverture de la fenêtre : " + e.getMessage());
            alert.showAndWait();
        }
    }
    private void sendResetEmail(String toEmail, String token) throws Exception {
        String fromEmail = "salma.jaghoua@etu.uae.ac.ma";
        String password = "e6cbfb0af1";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

//        Session session = Session.getInstance(props, new Authenticator() {
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(fromEmail, password);
//            }
//        });
//
//        Message msg = new MimeMessage(session);
//        msg.setFrom(new InternetAddress(fromEmail));
//        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
//        msg.setSubject("Réinitialisation de votre mot de passe - QoE System");

        // Message HTML plus professionnel
        String htmlContent = "<html><body>"
                + "<h3>Réinitialisation de mot de passe</h3>"
                + "<p>Cliquez sur le lien suivant pour réinitialiser votre mot de passe :</p>"
                + "<p><a href=\"http://localhost:8080/resetPassword?token=" + token + "\">"
                + "Réinitialiser mon mot de passe</a></p>"
                + "<p>Ce lien expirera dans 24 heures.</p>"
                + "<br/><p>Cordialement,<br/>L'équipe QoE System</p>"
                + "</body></html>";
//
//        msg.setContent(htmlContent, "text/html; charset=utf-8");
//        Transport.send(msg);
    }

    private void validateFields() {
        String username = usernameField.getText().trim();
        String password = isPasswordVisible ? passwordVisibleField.getText() : passwordField.getText();

        boolean isValid = !username.isEmpty() && !password.isEmpty();
        loginButton.setDisable(!isValid);

        if (isValid) {
            hideMessages();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}