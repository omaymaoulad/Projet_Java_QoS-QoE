package com.ensah.qoe;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Charger le FXML de la page de login (doit être dans resources/fxml/login.fxml)
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Objects.requireNonNull(getClass().getResource("/fxml/login.fxml")));
            Parent root = loader.load();

            // Créer la scène
            Scene scene = new Scene(root);

            InputStream cssStream = getClass().getResourceAsStream("/css/style.css");
            if (cssStream != null) {
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
            }

            // Configuration de la fenêtre
            primaryStage.setTitle("Login - QoS/QoE Project");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(360);
            primaryStage.setMinHeight(260);
            primaryStage.setResizable(false); // ou true si tu veux permettre le redimensionnement
            primaryStage.centerOnScreen();

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() throws Exception {
        super.init();

    }

    @Override
    public void stop() throws Exception {
        super.stop();

    }

    public static void main(String[] args) {
        launch(args);
    }
}