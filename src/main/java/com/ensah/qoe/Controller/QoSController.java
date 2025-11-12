package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.Qos;
import com.ensah.qoe.Services.QosAnalyzer;
import com.ensah.qoe.Services.QosInsertService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import java.io.File;

public class QoSController {

    @FXML private Label latenceLabel;
    @FXML private Label jitterLabel;
    @FXML private Label perteLabel;
    @FXML private Label bandePassanteLabel;
    @FXML private Label signalLabel;
    @FXML private Label mosLabel;
    @FXML private Button analyserButton;

    @FXML
    private void analyserQoS() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choisir un fichier CSV de mesures QoS");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv")
            );

            File selectedFile = fileChooser.showOpenDialog(analyserButton.getScene().getWindow());
            if (selectedFile == null) {
                System.out.println("⚠️ Aucun fichier sélectionné.");
                return;
            }

            Qos qos = QosAnalyzer.analyserQoS(selectedFile.getAbsolutePath());
            if (qos == null) {
                afficherErreur();
                return;
            }

            // --- Affichage ---
            latenceLabel.setText(String.format("%.2f ms", qos.getLatence()));
            jitterLabel.setText(String.format("%.2f ms", qos.getJitter()));
            perteLabel.setText(String.format("%.2f %%", qos.getPerte()));
            bandePassanteLabel.setText(String.format("%.2f Mbps", qos.getBandePassante()));
            signalLabel.setText(String.format("%.2f", qos.getSignalScore()));
            mosLabel.setText(String.format("%.2f", qos.getMos()));

            // --- Insertion DB ---
            QosInsertService insertService = new QosInsertService();
            insertService.insert(
                    qos.getLatence(),
                    qos.getJitter(),
                    qos.getPerte(),
                    qos.getBandePassante(),
                    qos.getMos(),
                    qos.getSignalScore(),
                    qos.getCellid(),
                    qos.getZone(),
                    qos.getType_connexion()
            );

            System.out.println(" Données QoS enregistrées avec succès dans la base.");

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur();
        }
    }

    private void afficherErreur() {
        latenceLabel.setText("Erreur");
        jitterLabel.setText("Erreur");
        perteLabel.setText("Erreur");
        bandePassanteLabel.setText("Erreur");
        signalLabel.setText("Erreur");
        mosLabel.setText("Erreur");
    }
}
