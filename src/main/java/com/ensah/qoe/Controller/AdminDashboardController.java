package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    private LoginController.User currentUser;
    @FXML
    private Label usernameLabel;

    public void setUserData(User user) {
        usernameLabel.setText("Welcome, " + user.getUsername() + "!");
    }

    public void setUserData(LoginController.User user) {
        this.currentUser = user;
        // Initialize admin dashboard with user data
        System.out.println("Admin logged in: " + user.getUsername());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize admin dashboard components
    }
}