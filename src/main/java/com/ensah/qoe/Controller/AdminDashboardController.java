package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    private User currentUser;

    @FXML
    private Label usernameLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println(" Admin Dashboard chargé avec succès !");
    }

    /** 🔹 Appelée par MainAdminLayoutController après connexion */
    public void setUserData(User user) {
        this.currentUser = user;
        if (usernameLabel != null && user != null) {
            usernameLabel.setText("Bienvenue, " + user.getUsername() + " !");
        }
    }

    /** 🔹 Méthode optionnelle si tu veux rafraîchir le dashboard */
    public void refreshDashboard() {
        System.out.println("🔄 Rafraîchissement du dashboard...");
        if (currentUser != null) {
            System.out.println("Utilisateur actuel : " + currentUser.getUsername());
        }
    }
}
