package com.ensah.qoe;
import com.ensah.qoe.Services.ClientCsvImporter;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            //ClientCsvImporter.importClients();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));

            Scene scene = new Scene(root, 1366, 700);
            scene.getStylesheets().add(getClass().getResource("/css/ml-dashboard.css").toExternalForm());
            primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo_red.jpg"))));

            // Configuration pour avoir les boutons système par défaut
            primaryStage.setTitle("QOS/QOE System - Login");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(500);

            // IMPORTANT: Ne pas utiliser StageStyle.UNDECORATED pour avoir les boutons système
            primaryStage.setResizable(true);

            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Application startup error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.setProperty("com.github.fommil.netlib.BLAS", "com.github.fommil.netlib.F2jBLAS");
        System.setProperty("com.github.fommil.netlib.LAPACK", "com.github.fommil.netlib.F2jLAPACK");
        System.setProperty("com.github.fommil.netlib.ARPACK", "com.github.fommil.netlib.F2jARPACK");
        launch(args);
    }
}