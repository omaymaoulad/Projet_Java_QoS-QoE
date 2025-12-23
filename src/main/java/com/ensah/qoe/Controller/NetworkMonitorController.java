package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.Qos;
import com.ensah.qoe.Services.QosZoneService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

public class NetworkMonitorController implements Initializable {

    @FXML private Circle statusIndicator;
    @FXML private Label networkStatusLabel;
    @FXML private Label activeConnectionsLabel;
    @FXML private Label bandwidthLabel;
    @FXML private ProgressBar bandwidthProgress;
    @FXML private Label packetLossLabel;
    @FXML private Label latenceLabel;
    @FXML private Label jitterLabel;
    @FXML private Label perteLabel;
    @FXML private Label bandePassanteLabel;
    @FXML private Label signalLabel;
    @FXML private Label mosLabel;
    @FXML private Label villeLabel;
    @FXML private Label paysLabel;
    @FXML private Label countLabel;

    @FXML private LineChart<String, Number> qosLineChart;
    @FXML private BarChart<String, Number> latencyChart;

    @FXML private ComboBox<String> zoneCombo;
    @FXML private TextField searchField;
    @FXML private PieChart qosPieChart;
    private Timeline updateTimeline;
    private final QosZoneService zoneService = new QosZoneService();
    private List<String> zones = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupZoneCombo();
        setupCharts();
        startAutoRefresh();
        updateNetworkStatus();
    }

    private void setupZoneCombo() {
        try {
            zones = zoneService.getZones();
            zoneCombo.getItems().clear();
            zoneCombo.getItems().addAll(zones);

            zoneCombo.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    updateZoneInfo(newValue);
                }
            });

            if (!zones.isEmpty()) {
                zoneCombo.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            System.err.println("Error loading zones: " + e.getMessage());
        }
    }

    private void updateZoneInfo(String zone) {
        try {
            List<Qos> qosList = QosZoneService.getQosByZone(zone);
            if (!qosList.isEmpty()) {
                Qos latest = qosList.get(qosList.size() - 1);

                // 🔁 graphique circulaire
                updateQoSPieChart(latest);

                updateQoSChart(qosList); // chart existant
            }
        } catch (Exception e) {
            System.err.println("Error updating zone info: " + e.getMessage());
        }
    }

    private void updateQoSChart(List<Qos> qosList) {
        if (qosLineChart == null || qosList.isEmpty()) return;

        qosLineChart.getData().clear();

        XYChart.Series<String, Number> mosSeries = new XYChart.Series<>();
        mosSeries.setName("MOS");

        for (Qos qos : qosList) {
            mosSeries.getData().add(new XYChart.Data<>(qos.getTranche12h(), qos.getMos()));
        }

        qosLineChart.getData().add(mosSeries);
    }



    private void setupCharts() {
        // Setup latency chart
        if (latencyChart != null) {
            XYChart.Series<String, Number> latencySeries = new XYChart.Series<>();
            latencySeries.setName("Connections");

            String[] ranges = {"0-10ms", "10-20ms", "20-50ms", "50-100ms", "100+ms"};
            int[] counts = {45, 32, 18, 8, 3};

            for (int i = 0; i < ranges.length; i++) {
                latencySeries.getData().add(new XYChart.Data<>(ranges[i], counts[i]));
            }

            latencyChart.getData().add(latencySeries);
        }
    }
    private void startAutoRefresh() {
        updateTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            updateNetworkStatus();
        }));
        updateTimeline.setCycleCount(Animation.INDEFINITE);
        updateTimeline.play();
    }

    private void updateNetworkStatus() {
        Random random = new Random();

        int connections = 150 + random.nextInt(20);
        activeConnectionsLabel.setText(String.valueOf(connections));

        double bandwidth = 2.0 + random.nextDouble() * 1.0;
        bandwidthLabel.setText(String.format("%.1f Gbps", bandwidth));
        bandwidthProgress.setProgress(bandwidth / 3.5);

        double packetLoss = random.nextDouble() * 0.1;
        packetLossLabel.setText(String.format("%.2f%%", packetLoss));

        if (packetLoss < 0.05) {
            statusIndicator.setFill(Color.web("#10b981"));
            networkStatusLabel.setText("Operational");
            networkStatusLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #10b981;");
        } else {
            statusIndicator.setFill(Color.web("#f59e0b"));
            networkStatusLabel.setText("Warning");
            networkStatusLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        }
    }

    @FXML
    private void refreshData() {
        updateNetworkStatus();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Refresh");
        alert.setHeaderText(null);
        alert.setContentText("Network data refreshed successfully!");
        alert.showAndWait();
    }
    private void updateQoSPieChart(Qos qos) {

        // ⚠ Normalisation (important pour un PieChart)
        double latency = Math.min(qos.getLatence() / 200.0, 1.0);      // max 200 ms
        double jitter  = Math.min(qos.getJitter() / 100.0, 1.0);       // max 100 ms
        double loss    = Math.min(qos.getPerte() / 5.0, 1.0);          // max 5 %
        double mos     = qos.getMos() / 5.0;                            // sur 5
        double signal  = Math.min((qos.getSignalScore() + 5) / 10.0, 1.0);
        double bw      = Math.min(qos.getBandePassante() / 3000.0, 1.0);

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Latency", latency),
                new PieChart.Data("Jitter", jitter),
                new PieChart.Data("Packet Loss", loss),
                new PieChart.Data("Bandwidth", bw),
                new PieChart.Data("Signal", signal),
                new PieChart.Data("MOS", mos)
        );

        qosPieChart.setData(data);
    }

}