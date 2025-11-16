package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.Qos;
import com.ensah.qoe.Services.QosAnalyzer;
import com.ensah.qoe.Services.QosInsertService;
import com.ensah.qoe.Services.QosZoneService;
import com.ensah.qoe.Services.FichierService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class QoSController {

    @FXML private Button importButton;
    @FXML private ComboBox<String> zoneCombo;
    @FXML private Label latenceLabel;
    @FXML private Label jitterLabel;
    @FXML private Label perteLabel;
    @FXML private Label bandePassanteLabel;
    @FXML private Label signalLabel;
    @FXML private Label mosLabel;
    @FXML private Label trancheLabel;
    @FXML private Label villeLabel;
    @FXML private Label paysLabel;
    @FXML private Label countLabel;
    @FXML private LineChart<String, Number> qosLineChart;

    // Services
    private final QosZoneService zoneService = new QosZoneService();

    @FXML
    public void initialize() {
        rafraichirZones();
        zoneCombo.getSelectionModel().clearSelection(); // ne sélectionne rien au démarrage
        zoneCombo.setOnAction(event -> afficherInfosZone());

    }

    /**
     * Importation CSV
     */
    @FXML
    private void importerCsv() {

        FileChooser fc = new FileChooser();
        fc.setTitle("Importer fichier QoS CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fc.showOpenDialog(importButton.getScene().getWindow());
        if (file == null) return;

        String filename = file.getName();
        if (FichierService.fichierExiste(filename)) {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "Ce fichier a déjà été importé. Les données ne seront pas réinsérées.");
            a.show();
            rafraichirZones();   // on recharge juste les zones depuis la base
            return;
        }


        // Tâche en arrière-plan : Évite blocage UI
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {

                // Analyse
                List<Qos> liste = QosAnalyzer.analyserQoSFichier(file.getAbsolutePath(), filename);

                if (!liste.isEmpty()) {
                    QosInsertService.insertListe(liste);
                }

                // Mise à jour UI sur JavaFX Thread
                Platform.runLater(() -> {
                    rafraichirZones();
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Fichier importé avec succès !");
                    a.show();
                });

                return null;
            }
        };

        new Thread(task).start();
    }


    /**
     * Affiche les métriques moyennes de la zone sélectionnée
     */
    @FXML
    private void afficherInfosZone() {

        String zone = zoneCombo.getValue();
        if (zone == null) return;

        List<Qos> historique = QosZoneService.getQosByZone(zone);
        if (historique.isEmpty()) return;

        Qos last = historique.get(historique.size() - 1);

        // METRIQUES
        latenceLabel.setText(String.format("%.2f ms", last.getLatence()));
        jitterLabel.setText(String.format("%.2f ms", last.getJitter()));
        perteLabel.setText(String.format("%.2f %%", last.getPerte()));
        bandePassanteLabel.setText(String.format("%.2f Mbps", last.getBandePassante()));
        signalLabel.setText(String.format("%.2f", last.getSignalScore()));
        mosLabel.setText(String.format("%.2f / 5", last.getMos()));

        // NOUVELLES INFORMATIONS
        trancheLabel.setText(last.getTranche12h());

        String[] zoneParts = last.getZone().split(",");
        villeLabel.setText(zoneParts[0]);
        paysLabel.setText(zoneParts.length > 1 ? zoneParts[1] : "");

        countLabel.setText(historique.size() + " mesures regroupées");

        tracerGraphique(historique);
    }


    /**
     * Tracer l’évolution QoS
     */
    private void tracerGraphique(List<Qos> historique) {

        qosLineChart.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("MOS");

        for (Qos q : historique) {
            serie.getData().add(new XYChart.Data<>(q.getTranche12h(), q.getMos()));
        }

        qosLineChart.getData().add(serie);
    }


    /**
     * Recharger les zones dans la ComboBox
     */
    @FXML
    private void rafraichirZones() {
        zoneCombo.getItems().clear();
        zoneCombo.getItems().addAll(zoneService.getZones());
    }
}
