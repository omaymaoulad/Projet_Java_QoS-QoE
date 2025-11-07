package com.ensah.qoe.Controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientDashboardController implements Initializable {

    private LoginController.User currentUser;
    @FXML
    private Label usernameLabel;

    public void setUserData(LoginController.User user) {
        this.currentUser = user;
        // Initialize client dashboard with user data
        System.out.println("Client logged in: " + user.getUsername());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize client dashboard components
    }
}