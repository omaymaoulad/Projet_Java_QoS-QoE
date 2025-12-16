package com.ensah.qoe.Controller;

import com.ensah.qoe.Services.PredictionServiceAnomalies;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class AnomalyDashboardController {

    // ===== NAVIGATION =====
    @FXML private Button btnAccueil;
    @FXML private Button btnPrediction;

    // ===== CONTAINER CENTRAL =====
    @FXML private StackPane contentPane;

    // ===== PAGE ACCUEIL =====
    @FXML private VBox accueilPane;
    @FXML private Label totalInstancesLabel;
    @FXML private Label normalCountLabel;
    @FXML private Label anomalyCountLabel;
    @FXML private Label accuracyLabel;

    @FXML private ScatterChart<Number, Number> chart1;
    @FXML private ScatterChart<Number, Number> chart2;
    @FXML private ScatterChart<Number, Number> chart3;

    @FXML private TableView<ZoneTableData> zoneTable;
    @FXML private TableColumn<ZoneTableData, String> colZone;
    @FXML private TableColumn<ZoneTableData, Integer> colNormal;
    @FXML private TableColumn<ZoneTableData, Integer> colAnomaly;
    @FXML private TableColumn<ZoneTableData, Integer> colTotal;
    @FXML private TableColumn<ZoneTableData, String> colStatus;

    // ===== PAGE PREDICTION =====
    @FXML private VBox predictionPane;
    @FXML private TextField latField, jitField, lossField, bwField, sigField;
    @FXML private Label predictionLabel, confidenceLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private ScrollPane accueilScroll;
    @FXML private ScrollPane predictionScroll;
    @FXML
    public void initialize() {
        // Configuration du tableau
        if (zoneTable != null) {
            colZone.setCellValueFactory(new PropertyValueFactory<>("zoneName"));
            colNormal.setCellValueFactory(new PropertyValueFactory<>("normalCount"));
            colAnomaly.setCellValueFactory(new PropertyValueFactory<>("anomalyCount"));
            colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        }

        showAccueil();
    }

    // ================= NAVIGATION =================

    @FXML
    private void showAccueil() {
        accueilScroll.setVisible(true);
        accueilScroll.setManaged(true);

        predictionScroll.setVisible(false);
        predictionScroll.setManaged(false);

        btnAccueil.getStyleClass().setAll("nav-btn-active");
        btnPrediction.getStyleClass().setAll("nav-btn");

        updateDashboard();
    }

    @FXML
    private void showPrediction() {
        accueilScroll.setVisible(false);
        accueilScroll.setManaged(false);

        predictionScroll.setVisible(true);
        predictionScroll.setManaged(true);

        btnAccueil.getStyleClass().setAll("nav-btn");
        btnPrediction.getStyleClass().setAll("nav-btn-active");
    }

    // ================= ENTRAÎNEMENT =================

    @FXML
    private void handleTrain() {
        System.out.println("✅ BOUTON ENTRAÎNEMENT CLIQUÉ");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Entraînement");
        alert.setHeaderText("Lancement de l'entraînement...");
        alert.setContentText("Le modèle Random Forest est en cours d'entraînement. Veuillez patienter...");
        alert.show();

        new Thread(() -> {
            try {
                String report = PredictionServiceAnomalies.trainModel();

                Platform.runLater(() -> {
                    alert.close();
                    updateDashboard();

                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("✅ Succès");
                    success.setHeaderText("Entraînement terminé avec succès");
                    success.setContentText(report);
                    success.showAndWait();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    alert.close();
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("❌ Erreur");
                    error.setHeaderText("Erreur lors de l'entraînement");
                    error.setContentText("Détails : " + e.getMessage());
                    error.showAndWait();
                    e.printStackTrace();
                });
            }
        }).start();
    }

    // ================= MISE À JOUR DASHBOARD =================

    private void updateDashboard() {
        if (!PredictionServiceAnomalies.isModelTrained()) {
            totalInstancesLabel.setText("--");
            normalCountLabel.setText("--");
            anomalyCountLabel.setText("--");
            accuracyLabel.setText("--");
            return;
        }

        // Mise à jour des statistiques
        int total = PredictionServiceAnomalies.getTotalInstances();
        int normal = PredictionServiceAnomalies.getNormalCount();
        int anomaly = PredictionServiceAnomalies.getAnomalyCount();
        double accuracy = PredictionServiceAnomalies.getLastAccuracy();

        totalInstancesLabel.setText(String.valueOf(total));
        normalCountLabel.setText(String.valueOf(normal));
        anomalyCountLabel.setText(String.valueOf(anomaly));
        accuracyLabel.setText(String.format("%.2f%%", accuracy));

        // Mise à jour des graphiques
        updateCharts();

        // Mise à jour du tableau
        updateZoneTable();
    }

    private void updateCharts() {
        if (!PredictionServiceAnomalies.isModelTrained()) return;

        // Graphique 1: Latence vs Jitter
        updateScatterChart(chart1, "latence", "jitter", "Latence vs Jitter");

        // Graphique 2: Bande passante vs Loss Rate
        updateScatterChart(chart2, "bande_passante", "loss_rate", "Bande passante vs Perte");

        // Graphique 3: Signal vs Latence
        updateScatterChart(chart3, "signal_score", "latence", "Signal vs Latence");
    }

    private void updateScatterChart(ScatterChart<Number, Number> chart,
                                    String feature1, String feature2, String title) {
        if (chart == null) return;

        chart.getData().clear();
        chart.setTitle(title);

        List<PredictionServiceAnomalies.DataPoint> data =
                PredictionServiceAnomalies.getScatterData(feature1, feature2);

        if (data.isEmpty()) {
            System.out.println("⚠️ Aucune donnée pour " + title);
            return;
        }

        XYChart.Series<Number, Number> normalSeries = new XYChart.Series<>();
        normalSeries.setName("Normal");

        XYChart.Series<Number, Number> anomalySeries = new XYChart.Series<>();
        anomalySeries.setName("Anomalie");

        for (PredictionServiceAnomalies.DataPoint point : data) {
            XYChart.Data<Number, Number> dataPoint =
                    new XYChart.Data<>(point.x, point.y);

            if (point.isAnomaly) {
                anomalySeries.getData().add(dataPoint);
            } else {
                normalSeries.getData().add(dataPoint);
            }
        }

        chart.getData().addAll(normalSeries, anomalySeries);
    }

    private void updateZoneTable() {
        if (zoneTable == null || !PredictionServiceAnomalies.isModelTrained()) return;

        List<PredictionServiceAnomalies.ZoneStats> zones =
                PredictionServiceAnomalies.getZoneStatistics();

        if (zones.isEmpty()) {
            System.out.println("⚠️ Aucune donnée de zone disponible");
            return;
        }

        var tableData = FXCollections.<ZoneTableData>observableArrayList();

        for (PredictionServiceAnomalies.ZoneStats zone : zones) {
            double anomalyPct = zone.getAnomalyPercentage();
            String status;

            if (anomalyPct > 20) {
                status = "🔴 Critique";
            } else if (anomalyPct > 10) {
                status = "⚠️ À surveiller";
            } else {
                status = "✅ Normal";
            }

            tableData.add(new ZoneTableData(
                    zone.zoneName,
                    zone.normalCount,
                    zone.anomalyCount,
                    zone.getTotal(),
                    status
            ));
        }

        zoneTable.setItems(tableData);
    }

    // ================= PRÉDICTION =================

    @FXML
    private void handlePredict() {
        if (!PredictionServiceAnomalies.isModelReady()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("⚠️ Modèle non prêt");
            alert.setHeaderText("Le modèle n'est pas encore entraîné");
            alert.setContentText("Veuillez d'abord entraîner le modèle depuis la page d'accueil.");
            alert.showAndWait();
            return;
        }

        try {
            double lat  = Double.parseDouble(latField.getText().trim());
            double jit  = Double.parseDouble(jitField.getText().trim());
            double loss = Double.parseDouble(lossField.getText().trim());
            double bw   = Double.parseDouble(bwField.getText().trim());
            double sig  = Double.parseDouble(sigField.getText().trim());

            // Validation des valeurs
            if (lat < 0 || jit < 0 || loss < 0 || bw < 0 || sig < 0 || sig > 100) {
                throw new IllegalArgumentException("Valeurs hors limites");
            }

            loadingIndicator.setVisible(true);
            predictionLabel.setText("Analyse en cours...");
            confidenceLabel.setText("--");

            new Thread(() -> {
                var res = PredictionServiceAnomalies.predictAnomaly(lat, jit, loss, bw, sig);

                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);

                    predictionLabel.setText(res.getPrediction());
                    confidenceLabel.setText(
                            String.format("%.1f%%", res.getAnomalyProbability() * 100)
                    );

                    if (res.getPrediction().equals("ANOMALIE")) {
                        predictionLabel.setStyle(
                                "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 28px;"
                        );
                    } else {
                        predictionLabel.setStyle(
                                "-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 28px;"
                        );
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("❌ Erreur");
            alert.setHeaderText("Valeurs invalides");
            alert.setContentText("Veuillez entrer des valeurs numériques valides dans tous les champs.");
            alert.showAndWait();
        } catch (IllegalArgumentException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("❌ Erreur");
            alert.setHeaderText("Valeurs hors limites");
            alert.setContentText("Vérifications :\n• Toutes les valeurs doivent être positives\n• Le signal doit être entre 0 et 100");
            alert.showAndWait();
        }
    }

    // ================= CLASSE POUR LE TABLEAU =================

    public static class ZoneTableData {
        private final String zoneName;
        private final Integer normalCount;
        private final Integer anomalyCount;
        private final Integer total;
        private final String status;

        public ZoneTableData(String zoneName, Integer normalCount,
                             Integer anomalyCount, Integer total, String status) {
            this.zoneName = zoneName;
            this.normalCount = normalCount;
            this.anomalyCount = anomalyCount;
            this.total = total;
            this.status = status;
        }

        public String getZoneName() { return zoneName; }
        public Integer getNormalCount() { return normalCount; }
        public Integer getAnomalyCount() { return anomalyCount; }
        public Integer getTotal() { return total; }
        public String getStatus() { return status; }
    }
}