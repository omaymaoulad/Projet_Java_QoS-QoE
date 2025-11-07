package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.Qos;
import com.ensah.qoe.Services.QosAnalyzer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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
        // Choix du fichier CSV
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(analyserButton.getScene().getWindow());
        if (file != null) {
            Qos qos = QosAnalyzer.analyserQoS(file.getAbsolutePath());

            // Mise à jour des labels
            latenceLabel.setText(String.format("%.2f ms", qos.getLatence()));
            jitterLabel.setText(String.format("%.2f ms", qos.getJitter()));
            perteLabel.setText(String.format("%.2f %%", qos.getPerte()));
            bandePassanteLabel.setText(String.format("%.2f Mbps", qos.getBandePassante()));
            signalLabel.setText(String.format("%.2f", qos.getSignalScore()));
            mosLabel.setText(String.format("%.2f", qos.getMos()));
        }
    }
}
