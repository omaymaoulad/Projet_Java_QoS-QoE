//package com.ensah.qoe.Controller;
//
//import com.ensah.qoe.Models.DBConnection;
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.scene.control.Alert;
//import javafx.scene.control.TextField;
//import javafx.stage.Modality;
//import javafx.stage.Stage;
//import oracle.jdbc.driver.Message;
//
//import javax.mail.*;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMessage;
//import java.io.IOException;
//import java.net.PasswordAuthentication;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLRecoverableException;
//import java.util.Properties;
//import java.util.UUID;
//
//public class ForgotPasswordController {
//
//    @FXML private TextField emailField;
//    private Stage currentStage;
//
//    public void setStage(Stage stage) {
//        this.currentStage = stage;
//    }
//
//    @FXML
//    private void handleSendResetLink() {
//        String email = emailField.getText().trim();
//        if(email.isEmpty()) {
//            new Alert(Alert.AlertType.WARNING, "Veuillez entrer votre email.").showAndWait();
//            return;
//        }
//
//        try (Connection conn = DBConnection.getConnection()) {
//            // Vérifier si la connexion est valide
//            if (conn == null || conn.isClosed()) {
//                new Alert(Alert.AlertType.ERROR, "Erreur de connexion à la base de données.").showAndWait();
//                return;
//            }
//
//            String query = "SELECT * FROM utilisateurs WHERE email = ?";
//            PreparedStatement pst = conn.prepareStatement(query);
//            pst.setString(1, email);
//            ResultSet rs = pst.executeQuery();
//
//            if(rs.next()) {
//                // Générer un token unique
//                String token = UUID.randomUUID().toString();
//
//                // Sauvegarder le token dans la base
//                String updateToken = "UPDATE utilisateurs SET reset_token = ? WHERE email = ?";
//                PreparedStatement pst2 = conn.prepareStatement(updateToken);
//                pst2.setString(1, token);
//                pst2.setString(2, email);
//                pst2.executeUpdate();
//
//                // Envoyer l'email
//                sendEmail(email, token);
//
//                // Ouvrir la fenêtre de réinitialisation
//                openResetPasswordWindow(token, email);
//
//                new Alert(Alert.AlertType.INFORMATION, "Un code de réinitialisation a été envoyé à votre email.").showAndWait();
//
//            } else {
//                new Alert(Alert.AlertType.ERROR, "Email non trouvé !").showAndWait();
//            }
//        } catch (SQLRecoverableException e) {
//            e.printStackTrace();
//            new Alert(Alert.AlertType.ERROR, "Erreur de connexion à la base de données. Veuillez réessayer.").showAndWait();
//        } catch (Exception e) {
//            e.printStackTrace();
//            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'envoi du lien de réinitialisation: " + e.getMessage()).showAndWait();
//        }
//    }
//    @FXML
//    private void handleBackToLogin() {
//        try {
//            // Fermer la fenêtre actuelle
//            Stage currentStage = (Stage) emailField.getScene().getWindow();
//            currentStage.close();
//
//            // Rouvrir la fenêtre de login
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ensah/qoe/login.fxml"));
//            Parent root = loader.load();
//
//            Stage loginStage = new Stage();
//            loginStage.setTitle("Connexion - QoE System");
//            loginStage.setScene(new Scene(root));
//            loginStage.show();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            new Alert(Alert.AlertType.ERROR, "Erreur lors du retour à la connexion.").showAndWait();
//        }
//    }
//    private void openResetPasswordWindow(String token, String email) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ensah/qoe/resetPassword.fxml"));
//            Parent root = loader.load();
//
//            ResetPasswordController controller = loader.getController();
//            controller.setToken(token);
//            controller.setEmail(email);
//
//            // DEBUG
//            System.out.println("🎯 Passage du token à ResetPasswordController: " + token);
//            System.out.println("📧 Email associé: " + email);
//
//            Stage resetStage = new Stage();
//            resetStage.setTitle("Réinitialisation du mot de passe");
//            resetStage.setScene(new Scene(root));
//            resetStage.initModality(Modality.APPLICATION_MODAL);
//            resetStage.show();
//
//            if (currentStage != null) {
//                currentStage.close();
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'ouverture de la fenêtre de réinitialisation.").showAndWait();
//        }
//    }
//    private void sendEmail(String recipientEmail, String token) throws Exception {
//        String from = "omaymaouladmoussa@gmail.com";
//        String password = "azertyuiop@04"; // Vérifiez que c'est un mot de passe d'application Gmail
//
//        Properties props = new Properties();
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
//        props.put("mail.smtp.host", "smtp.gmail.com");
//        props.put("mail.smtp.port", "587");
//
//        Session session = Session.getInstance(props,
//                new javax.mail.Authenticator() {
//                    protected PasswordAuthentication getPasswordAuthentication() {
//                        return new PasswordAuthentication(from, password);
//                    }
//                });
//
//        Message message = new MimeMessage(session);
//        message.setFrom(new InternetAddress(from));
//        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
//        message.setSubject("Réinitialisation du mot de passe - QoE System");
//
//        // Message avec code au lieu de lien HTTP
//        String emailContent = "Bonjour,\n\n"
//                + "Vous avez demandé la réinitialisation de votre mot de passe.\n"
//                + "Votre code de réinitialisation est : " + token + "\n\n"
//                + "Veuillez retourner dans l'application et saisir ce code dans le formulaire de réinitialisation.\n\n"
//                + "Cordialement,\nL'équipe QoE System";
//
//        message.setText(emailContent);
//
//        Transport.send(message);
//    }}
