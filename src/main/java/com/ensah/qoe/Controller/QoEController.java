package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.QoE;
import com.ensah.qoe.Services.QoeAnalyzer;
import com.ensah.qoe.Services.QoeInsertService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import java.io.File;

public class QoEController {

    // Labels pour les métriques subjectives
    @FXML private Label satisfactionLabel;
    @FXML private Label videoQualityLabel;
    @FXML private Label audioQualityLabel;
    @FXML private Label interactivityLabel;
    @FXML private Label reliabilityLabel;
    @FXML private Label overallQoeLabel;

    // Labels pour les métriques objectives
    @FXML private Label bufferingLabel;
    @FXML private Label loadingTimeLabel;
    @FXML private Label failureRateLabel;
    @FXML private Label streamingQualityLabel;

    // Labels pour les informations contextuelles
    @FXML private Label serviceTypeLabel;
    @FXML private Label deviceTypeLabel;

    @FXML private Button analyserButton;

    @FXML
    private void analyserQoE() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choisir un fichier CSV de données QoE");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv")
            );

            File selectedFile = fileChooser.showOpenDialog(analyserButton.getScene().getWindow());
            if (selectedFile == null) {
                System.out.println("⚠️ Aucun fichier sélectionné.");
                return;
            }

            QoE qoe = QoeAnalyzer.analyserQoE(selectedFile.getAbsolutePath());
            if (qoe == null) {
                afficherErreur();
                return;
            }

            // --- Affichage des métriques subjectives ---
            satisfactionLabel.setText(String.format("%.2f / 5", qoe.getSatisfactionScore()));
            videoQualityLabel.setText(String.format("%.2f / 5", qoe.getVideoQuality()));
            audioQualityLabel.setText(String.format("%.2f / 5", qoe.getAudioQuality()));
            interactivityLabel.setText(String.format("%.2f / 5", qoe.getInteractivity()));
            reliabilityLabel.setText(String.format("%.2f / 5", qoe.getReliability()));
            overallQoeLabel.setText(String.format("%.2f / 5", qoe.getOverallQoe()));

            // --- Affichage des métriques objectives ---
            bufferingLabel.setText(String.format("%.2f s", qoe.getBuffering()));
            loadingTimeLabel.setText(String.format("%.2f s", qoe.getLoadingTime()));
            failureRateLabel.setText(String.format("%.2f %%", qoe.getFailureRate()));
            streamingQualityLabel.setText(String.format("%.2f / 5", qoe.getStreamingQuality()));

            // --- Affichage des informations contextuelles ---
            serviceTypeLabel.setText(qoe.getServiceType());
            deviceTypeLabel.setText(qoe.getDeviceType());

            // Appliquer les couleurs selon le score
            appliquerCouleurQoe(overallQoeLabel, qoe.getOverallQoe());
            appliquerCouleurQoe(satisfactionLabel, qoe.getSatisfactionScore());
            appliquerCouleurQoe(videoQualityLabel, qoe.getVideoQuality());
            appliquerCouleurQoe(audioQualityLabel, qoe.getAudioQuality());
            appliquerCouleurQoe(interactivityLabel, qoe.getInteractivity());
            appliquerCouleurQoe(reliabilityLabel, qoe.getReliability());
            appliquerCouleurQoe(streamingQualityLabel, qoe.getStreamingQuality());

            // --- Insertion en base de données ---
            QoeInsertService insertService = new QoeInsertService();
            insertService.insert(
                    qoe.getSatisfactionScore(),
                    qoe.getVideoQuality(),
                    qoe.getAudioQuality(),
                    qoe.getInteractivity(),
                    qoe.getReliability(),
                    qoe.getOverallQoe(),
                    qoe.getBuffering(),
                    qoe.getLoadingTime(),
                    qoe.getFailureRate(),
                    qoe.getStreamingQuality(),
                    qoe.getServiceType(),
                    qoe.getDeviceType(),
                    qoe.getUserId()
            );

            System.out.println("✅ Données QoE enregistrées avec succès dans la base.");

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur();
        }
    }

    /**
     * Applique une couleur au label en fonction du score QoE (1-5)
     */
    private void appliquerCouleurQoe(Label label, double score) {
        String style = "-fx-font-weight: bold; -fx-font-size: 24;";

        if (score >= 4.0) {
            // Excellent - Vert
            style += " -fx-text-fill: #059669;";
        } else if (score >= 3.0) {
            // Bon - Bleu
            style += " -fx-text-fill: #2563eb;";
        } else if (score >= 2.0) {
            // Moyen - Orange
            style += " -fx-text-fill: #d97706;";
        } else {
            // Mauvais - Rouge
            style += " -fx-text-fill: #dc2626;";
        }

        label.setStyle(style);
    }

    /**
     * Affiche un message d'erreur dans tous les labels
     */
    private void afficherErreur() {
        String erreur = "Erreur";

        satisfactionLabel.setText(erreur);
        videoQualityLabel.setText(erreur);
        audioQualityLabel.setText(erreur);
        interactivityLabel.setText(erreur);
        reliabilityLabel.setText(erreur);
        overallQoeLabel.setText(erreur);
        bufferingLabel.setText(erreur);
        loadingTimeLabel.setText(erreur);
        failureRateLabel.setText(erreur);
        streamingQualityLabel.setText(erreur);
        serviceTypeLabel.setText(erreur);
        deviceTypeLabel.setText(erreur);
    }
}