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
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.*;
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

    private final QosZoneService zoneService = new QosZoneService();

    @FXML
    public void initialize() {
        rafraichirZones();
        zoneCombo.getSelectionModel().clearSelection();
        zoneCombo.setOnAction(event -> afficherInfosZone());
    }

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
            rafraichirZones();
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {

                System.out.println(">>> IMPORT CSV commencé");

                try {
                    // 1) Lancer Python
                    System.out.println(">>> Lancement script Python...");
                    ProcessBuilder pb = new ProcessBuilder(
                            "python",
                            "src/main/resources/python/geocode_dynamic.py",
                            file.getAbsolutePath()
                    );
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream())
                    );

                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[PY] " + line);
                    }

                    int exitCode = process.waitFor();
                    System.out.println(">>> Script Python terminé avec code: " + exitCode);

                } catch (Exception e) {
                    System.out.println(">>> ERREUR pendant Python:");
                    e.printStackTrace();
                }

                // 2) Analyse QoS
                System.out.println(">>> Analyse QoS en cours...");
                List<Qos> liste = QosAnalyzer.analyserQoSFichier(
                        file.getAbsolutePath(),
                        filename
                );
                System.out.println(">>> Analyse QoS terminée, nb groupes: " + liste.size());

                // 3) Insertion DB
                try {
                    System.out.println(">>> Insertion DB...");
                    QosInsertService.insertListe(liste);
                    System.out.println(">>> Insertion DB terminée !");
                } catch (Exception e) {
                    System.out.println(">>> ERREUR insertion DB:");
                    e.printStackTrace();
                }

                // 4) Retour UI
                Platform.runLater(() -> {
                    System.out.println(">>> Rafraîchissement UI...");
                    rafraichirZones();
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Fichier importé avec succès !");
                    a.show();
                    System.out.println(">>> UI OK !");
                });

                return null;
            }
        };


        new Thread(task).start();
    }

    @FXML
    private void afficherInfosZone() {

        String zone = zoneCombo.getValue();
        if (zone == null) return;

        List<Qos> historique = QosZoneService.getQosByZone(zone);
        if (historique.isEmpty()) return;

        Qos last = historique.get(historique.size() - 1);

        latenceLabel.setText(String.format("%.2f ms", last.getLatence()));
        jitterLabel.setText(String.format("%.2f ms", last.getJitter()));
        perteLabel.setText(String.format("%.2f %%", last.getPerte()));
        bandePassanteLabel.setText(String.format("%.2f Mbps", last.getBandePassante()));
        signalLabel.setText(String.format("%.2f", last.getSignalScore()));
        mosLabel.setText(String.format("%.2f / 5", last.getMos()));

        trancheLabel.setText(last.getTranche12h());

        String[] zoneParts = last.getZone().split(",");
        villeLabel.setText(zoneParts[0]);
        paysLabel.setText(zoneParts.length > 1 ? zoneParts[1].trim() : "");

        countLabel.setText(historique.size() + " mesures regroupées");

        tracerGraphique(historique);
    }

    private void tracerGraphique(List<Qos> historique) {
        qosLineChart.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("MOS");
        for (Qos q : historique) {
            serie.getData().add(new XYChart.Data<>(q.getTranche12h(), q.getMos()));
        }
        qosLineChart.getData().add(serie);
    }

    @FXML
    private void rafraichirZones() {
        zoneCombo.getItems().clear();
        zoneCombo.getItems().addAll(zoneService.getZones());
    }
}
