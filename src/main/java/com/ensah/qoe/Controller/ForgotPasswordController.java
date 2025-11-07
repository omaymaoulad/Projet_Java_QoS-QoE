package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.UUID;

public class ForgotPasswordController {

    @FXML private TextField emailField;

    @FXML
    private void handleSendResetLink() {
        String email = emailField.getText().trim();
        if(email.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez entrer votre email.").showAndWait();
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT * FROM utilisateurs WHERE email = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();

            if(rs.next()) {
                // Générer un token unique
                String token = UUID.randomUUID().toString();

                // Sauvegarder le token dans la base
                String updateToken = "UPDATE users SET reset_token = ? WHERE email = ?";
                PreparedStatement pst2 = conn.prepareStatement(updateToken);
                pst2.setString(1, token);
                pst2.setString(2, email);
                pst2.executeUpdate();

                // Envoyer l'email
                sendEmail(email, token);
                new Alert(Alert.AlertType.INFORMATION, "Un lien de réinitialisation a été envoyé à votre email.").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "Email non trouvé !").showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la réinitialisation du mot de passe.").showAndWait();
        }
    }

    private void sendEmail(String recipientEmail, String token) throws Exception {
        String from = "tonemail@gmail.com";
        String password = "tonmotdepasse"; // mot de passe application Gmail

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from, password);
                    }
                });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("Réinitialisation du mot de passe");

        // Lien pour réinitialiser le mot de passe
        String resetLink = "http://localhost:8080/resetPassword?token=" + token;
        message.setText("Cliquez sur ce lien pour réinitialiser votre mot de passe : " + resetLink);

        Transport.send(message);
    }
}
