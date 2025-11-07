package com.ensah.qoe.Controller;

import javafx.fxml.FXML;
import com.ensah.qoe.Models.User;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientDashboardController implements Initializable {

    private User currentUser;
    @FXML
    private Label usernameLabel;

    public void setUserData(User user) {
        this.currentUser = user;
        // Initialize client dashboard with user data
        System.out.println("Client logged in: " + user.getUsername());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize client dashboard components
    }
}