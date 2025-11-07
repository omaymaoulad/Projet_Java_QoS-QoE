package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;
    @FXML private Button loginButton;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private ProgressIndicator progressIndicator;

    private boolean isPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEventHandlers();
        initializeConnectionStatus();
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
    }

    private void initializeConnectionStatus() {
        // Test database connection
        new Thread(() -> {
            try {
                Connection conn = DBConnection.getConnection();
                if (conn != null && !conn.isClosed()) {
                    javafx.application.Platform.runLater(() -> {
                        connectionStatusLabel.setText("🟢 Database Connected");
                    });
                } else {
                    javafx.application.Platform.runLater(() -> {
                        connectionStatusLabel.setText("🔴 Database Error");
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    connectionStatusLabel.setText("🔴 Connection Failed");
                });
            }
        }).start();
    }

    private void sendResetEmail(String toEmail, String token) throws Exception {
        String fromEmail = "salma.jaghoua@etu.uae.ac.ma"; // ton email
        String password = "e6cbfb0af1";      // mot de passe ou app password Gmail

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(fromEmail));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject("Réinitialisation de votre mot de passe");
        msg.setText("Cliquez sur ce lien pour réinitialiser votre mot de passe : " +
                "http://localhost:8080/resetPassword?token=" + token);

        Transport.send(msg);
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordButton.setText("🔒");
        } else {
            passwordField.setText(passwordVisibleField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            togglePasswordButton.setText("👁️");
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

        // Disable button and show loader
        loginButton.setDisable(true);
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
                        loginButton.setDisable(false);
                        progressIndicator.setVisible(false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    showError("Database error: " + e.getMessage());
                    loginButton.setDisable(false);
                    progressIndicator.setVisible(false);
                });
            }
        }).start();
    }

    private User authenticateUser(String username, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT id_user, username, email, role FROM utilisateurs WHERE username = ? AND password = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            System.out.println("Executing query: username=" + username + ", password=" + password);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id_user"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                System.out.println("User found: " + user);
                return user;
            } else {
                System.out.println("No user found with username=" + username);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) { e.printStackTrace(); }
            try { if (pstmt != null) pstmt.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }


    private void redirectToDashboard(User user) {
        try {
            String fxmlFile;
            String title;

            if ("admin".equals(user.getRole())) {
                fxmlFile = "/fxml/admin_dashboard.fxml";
                title = "QOS/QOE Admin Dashboard - Welcome " + user.getUsername();
            } else {
                fxmlFile = "/fxml/client_dashboard.fxml";
                title = "QOS/QOE Client Dashboard - Welcome " + user.getUsername();
            }

            // Load the appropriate dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));

            Parent dashboard = loader.load();

            // Pass user data to the dashboard controller
            Object controller = loader.getController();
            if (controller instanceof AdminDashboardController) {
                ((AdminDashboardController) controller).setUserData(user);
            } else if (controller instanceof ClientDashboardController) {
                ((ClientDashboardController) controller).setUserData(user);
            }

            // Switch scene
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(dashboard, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading dashboard: " + e.getMessage());
            loginButton.setDisable(false);
            progressIndicator.setVisible(false);
        }
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
    @FXML
    private void handleForgotPassword() {
        // Demander l'email à l'utilisateur
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Mot de passe oublié");
        dialog.setHeaderText("Entrez votre email pour réinitialiser le mot de passe");
        dialog.setContentText("Email : ");
        String email = dialog.showAndWait().orElse(null);

        if(email == null || email.isEmpty()) return;

        // Vérifier si l'email existe dans la DB et générer le token
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement pst = conn.prepareStatement("SELECT * FROM utilisateurs WHERE email = ?");
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();

            if(rs.next()) {
                // Générer un token unique
                String token = java.util.UUID.randomUUID().toString();

                // Mettre à jour la base avec le token
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE utilisateurs SET reset_token = ? WHERE email = ?");
                update.setString(1, token);
                update.setString(2, email);
                update.executeUpdate();

                // Ouvrir la fenêtre ResetPassword et passer le token
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resetPassword.fxml"));
                Parent root = loader.load();
                ResetPasswordController controller = loader.getController();
                controller.setToken(token); // maintenant le token est valide

                Stage stage = new Stage();
                stage.setTitle("Réinitialiser le mot de passe");
                stage.setScene(new Scene(root));
                stage.show();

            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Email non trouvé !");
                alert.showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors de la vérification de l'email.");
            alert.showAndWait();
        }
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

    // User class to hold user data
    public static class User {
        private int id;
        private String username;
        private String email;
        private String role;

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        @Override
        public String toString() {
            return "User{id=" + id + ", username='" + username + "', email='" + email + "', role='" + role + "'}";
        }
    }
}