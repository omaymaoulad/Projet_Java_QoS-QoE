package com.ensah.qoe;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("🚀 Démarrage de l'application QoS/QoE System...");

            // ✅ Vérifier et charger le FXML
            URL fxmlUrl = getClass().getResource("/fxml/admin_dashboard.fxml");
            if (fxmlUrl == null) {
                System.err.println("❌ ERREUR : /fxml/admin_dashboard.fxml introuvable !");
                throw new Exception("Fichier FXML introuvable");
            }
            System.out.println("✅ FXML trouvé : " + fxmlUrl);

            Parent root = FXMLLoader.load(fxmlUrl);

            // ✅ Créer la scène
            Scene scene = new Scene(root, 1200, 650);

            // ✅ Charger le CSS (optionnel)
            URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("✅ CSS chargé : " + cssUrl);
            } else {
                System.out.println("⚠️ CSS non trouvé : /css/style.css (optionnel)");
            }

            // ✅ Charger l'icône (optionnel)
            InputStream iconStream = getClass().getResourceAsStream("/images/logo_red.jpg");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
                System.out.println("✅ Icône chargée");
            } else {
                System.out.println("⚠️ Icône non trouvée : /images/logo_red.jpg (optionnel)");
            }

            // Configuration de la fenêtre
            primaryStage.setTitle("QOS/QOE System - Login");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(500);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("✅ Application démarrée avec succès !\n");

        } catch (Exception e) {
            System.err.println("\n❌ ERREUR CRITIQUE au démarrage :");
            System.err.println("Message : " + e.getMessage());
            e.printStackTrace();

            System.err.println("\n📋 Vérifiez la structure de votre projet :");
            System.err.println("   src/main/resources/");
            System.err.println("   ├── fxml/");
            System.err.println("   │   └── admin_dashboard.fxml  ← REQUIS");
            System.err.println("   ├── css/");
            System.err.println("   │   └── style.css            ← Optionnel");
            System.err.println("   ├── images/");
            System.err.println("   │   └── logo_red.jpg         ← Optionnel");
            System.err.println("   └── data/");
            System.err.println("       └── QoS_data.csv         ← Pour l'analyse");

            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}