package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.Qos;
import com.ensah.qoe.Services.QosAnalyzer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.application.Platform;
import javafx.concurrent.Task;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class QoSController {

    @FXML private Label latenceLabel;
    @FXML private Label jitterLabel;
    @FXML private Label perteLabel;
    @FXML private Label bandePassanteLabel;
    @FXML private Label signalLabel;
    @FXML private Label mosLabel;
    @FXML private Button analyserButton;

    @FXML
    public void initialize() {
        System.out.println("🟢 QoSController initialisé");

        // Test du fichier CSV
        InputStream test = getClass().getResourceAsStream("/data/QoS_data.csv");
        if (test != null) {
            System.out.println("✅ QoS_data.csv trouvé dans les resources");
            try { test.close(); } catch (Exception e) {}
        } else {
            System.err.println("❌ QoS_data.csv INTROUVABLE");
        }
    }

    @FXML
    private void analyserQoS() {
        System.out.println("🔵 ========== BOUTON CLIQUÉ ==========");

        // Désactiver le bouton pendant l'analyse
        analyserButton.setDisable(true);

        // Afficher un message temporaire
        Platform.runLater(() -> {
            latenceLabel.setText("Analyse en cours...");
            jitterLabel.setText("⏳");
            perteLabel.setText("⏳");
            bandePassanteLabel.setText("⏳");
            signalLabel.setText("⏳");
            mosLabel.setText("⏳");
        });

        // Créer une tâche en arrière-plan
        Task<Qos> analysisTask = new Task<Qos>() {
            @Override
            protected Qos call() throws Exception {
                System.out.println("📂 Extraction du fichier CSV...");
                String csvPath = "/data/qos_data.csv";
                System.out.println("✅  Fichier extrait vers : " + csvPath);

                System.out.println("🔄  Appel de QosAnalyzer.analyserQoS()...");
                Qos result = QosAnalyzer.analyserQoS(csvPath);
                System.out.println("📊  Résultat de l'analyse : " + (result != null ? "OK" : "NULL"));

                return result;
            }
        };

        // Quand l'analyse réussit
        analysisTask.setOnSucceeded(event -> {
            Qos qos = analysisTask.getValue();
            System.out.println("✅ Analyse terminée avec succès");

            if (qos != null) {
                System.out.println("📈 Valeurs reçues :");
                System.out.println("   - Latence: " + qos.getLatence());
                System.out.println("   - Jitter: " + qos.getJitter());
                System.out.println("   - Perte: " + qos.getPerte());
                System.out.println("   - Bande passante: " + qos.getBandePassante());
                System.out.println("   - Signal Score: " + qos.getSignalScore());
                System.out.println("   - MOS: " + qos.getMos());

                // Mettre à jour l'interface
                latenceLabel.setText(String.format("%.2f ms", qos.getLatence()));
                jitterLabel.setText(String.format("%.2f ms", qos.getJitter()));
                perteLabel.setText(String.format("%.2f %%", qos.getPerte()));
                bandePassanteLabel.setText(String.format("%.2f Mbps", qos.getBandePassante()));
                signalLabel.setText(String.format("%.2f", qos.getSignalScore()));
                mosLabel.setText(String.format("%.2f", qos.getMos()));

            } else {
                System.err.println("❌ QoS est NULL - L'analyse a échoué");
                latenceLabel.setText("Erreur : Analyse échouée");
                afficherTirets();
            }

            // Réactiver le bouton
            analyserButton.setDisable(false);
        });

        // Quand l'analyse échoue
        analysisTask.setOnFailed(event -> {
            Throwable exception = analysisTask.getException();
            System.err.println("❌ ERREUR lors de l'analyse :");
            exception.printStackTrace();

            latenceLabel.setText("Erreur : " + exception.getMessage());
            afficherTirets();

            // Réactiver le bouton
            analyserButton.setDisable(false);
        });

        // Lancer la tâche dans un nouveau thread
        Thread thread = new Thread(analysisTask);
        thread.setDaemon(true); // Le thread se fermera avec l'application
        thread.start();

        System.out.println("🔵 ========== ANALYSE LANCÉE EN ARRIÈRE-PLAN ==========");
    }

    private String extractResourceToTempFile(String resourcePath) throws Exception {
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new Exception("❌ Fichier introuvable dans les resources : " + resourcePath);
        }

        File tempFile = File.createTempFile("QoS_data_", ".csv");
        tempFile.deleteOnExit();

        Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        inputStream.close();

        return tempFile.getAbsolutePath();
    }

    private void afficherTirets() {
        jitterLabel.setText("-");
        perteLabel.setText("-");
        bandePassanteLabel.setText("-");
        signalLabel.setText("-");
        mosLabel.setText("-");
    }
}