package com.ensah.qoe.Controller;

import com.ensah.qoe.Services.PredictionServiceMOS;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class MOSDashboardController {

    // ===== NAVIGATION =====
    @FXML private Button btnAccueilMOS;
    @FXML private Button btnPredictionMOS;
    @FXML private Button btnComparisonMOS;

    // ===== CONTAINER CENTRAL =====
    @FXML private StackPane contentPaneMOS;

    // ===== PAGE ACCUEIL MOS =====
    @FXML private VBox accueilMOSPane;
    @FXML private Label totalAudioLabel;
    @FXML private Label avgMOSLabel;
    @FXML private Label minMOSLabel;
    @FXML private Label maxMOSLabel;
    @FXML private Label rmseLabel;

    @FXML private LineChart<Number, Number> mosTrendChart;
    @FXML private BarChart<String, Number> qualityDistributionChart;
    @FXML private ScatterChart<Number, Number> featureMOSChart;

    @FXML private TableView<AudioQualityTableData> qualityTable;
    @FXML private TableColumn<AudioQualityTableData, String> colAudioId;
    @FXML private TableColumn<AudioQualityTableData, Double> colPredictedMOS;
    @FXML private TableColumn<AudioQualityTableData, Double> colActualMOS;
    @FXML private TableColumn<AudioQualityTableData, Double> colError;
    @FXML private TableColumn<AudioQualityTableData, String> colQualityLevel;

    // ===== PAGE PREDICTION MOS =====
    @FXML private VBox predictionMOSPane;
    @FXML private TextField spectralCentroidField;
    @FXML private TextField spectralBandwidthField;
    @FXML private TextField rmsField;
    @FXML private TextField zcrField;
    @FXML private TextField snrField;
    @FXML private TextField distortionField;
    @FXML private TextField noiseLevelField;

    @FXML private Label predictedMOSLabel;
    @FXML private Label qualityLevelLabel;
    @FXML private Label confidenceLabel;
    @FXML private ProgressIndicator loadingIndicatorMOS;

    @FXML private Slider volumeSlider;
    @FXML private Slider noiseSlider;
    @FXML private Slider compressionSlider;

    @FXML private ProgressBar mosBar;

    @FXML private ScrollPane accueilMOSScroll;
    @FXML private ScrollPane predictionMOSScroll;
    @FXML private ScrollPane comparisonMOSScroll;

    // ===== PAGE COMPARAISON =====
    @FXML private VBox comparisonMOSPane;
    @FXML private LineChart<Number, Number> comparisonChart;
    @FXML private TextArea comparisonTextArea;
    @FXML private ChoiceBox<String> comparisonMetricChoice;
    @FXML private CheckBox showActualMOSCheck;
    @FXML private CheckBox showPredictedMOSCheck;

    @FXML
    public void initialize() {
        // Configuration du tableau
        if (qualityTable != null) {
            colAudioId.setCellValueFactory(new PropertyValueFactory<>("audioId"));
            colPredictedMOS.setCellValueFactory(new PropertyValueFactory<>("predictedMOS"));
            colActualMOS.setCellValueFactory(new PropertyValueFactory<>("actualMOS"));
            colError.setCellValueFactory(new PropertyValueFactory<>("error"));
            colQualityLevel.setCellValueFactory(new PropertyValueFactory<>("qualityLevel"));
        }

        // Configuration des sliders
        setupSliders();

        // Configuration de la page de comparaison
        setupComparisonPage();

        showAccueilMOS();
    }

    private void setupSliders() {
        // Volume slider
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            rmsField.setText(String.format("%.3f", newVal.doubleValue() * 0.1));
            simulateMOSPrediction();
        });

        // Noise slider
        noiseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            noiseLevelField.setText(String.format("%.3f", newVal.doubleValue()));
            simulateMOSPrediction();
        });

        // Compression slider
        compressionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            distortionField.setText(String.format("%.3f", newVal.doubleValue() * 0.05));
            simulateMOSPrediction();
        });
    }

    private void setupComparisonPage() {
        comparisonMetricChoice.setItems(FXCollections.observableArrayList(
                "MOS vs Bande Passante",
                "MOS vs Bruit",
                "MOS vs Compression",
                "MOS vs SNR"
        ));
        comparisonMetricChoice.setValue("MOS vs Bande Passante");

        showActualMOSCheck.setSelected(true);
        showPredictedMOSCheck.setSelected(true);
    }

    // ================= NAVIGATION =================

    @FXML
    private void showAccueilMOS() {
        accueilMOSScroll.setVisible(true);
        accueilMOSScroll.setManaged(true);
        predictionMOSScroll.setVisible(false);
        predictionMOSScroll.setManaged(false);
        comparisonMOSScroll.setVisible(false);
        comparisonMOSScroll.setManaged(false);

        btnAccueilMOS.getStyleClass().setAll("nav-btn-active");
        btnPredictionMOS.getStyleClass().setAll("nav-btn");
        btnComparisonMOS.getStyleClass().setAll("nav-btn");

        updateMOSDashboard();
    }

    @FXML
    private void showPredictionMOS() {
        accueilMOSScroll.setVisible(false);
        accueilMOSScroll.setManaged(false);
        predictionMOSScroll.setVisible(true);
        predictionMOSScroll.setManaged(true);
        comparisonMOSScroll.setVisible(false);
        comparisonMOSScroll.setManaged(false);

        btnAccueilMOS.getStyleClass().setAll("nav-btn");
        btnPredictionMOS.getStyleClass().setAll("nav-btn-active");
        btnComparisonMOS.getStyleClass().setAll("nav-btn");

        // Réinitialiser les champs pour une nouvelle prédiction
        resetPredictionFields();
    }

    @FXML
    private void showComparisonMOS() {
        accueilMOSScroll.setVisible(false);
        accueilMOSScroll.setManaged(false);
        predictionMOSScroll.setVisible(false);
        predictionMOSScroll.setManaged(false);
        comparisonMOSScroll.setVisible(true);
        comparisonMOSScroll.setManaged(true);

        btnAccueilMOS.getStyleClass().setAll("nav-btn");
        btnPredictionMOS.getStyleClass().setAll("nav-btn");
        btnComparisonMOS.getStyleClass().setAll("nav-btn-active");

        updateComparisonChart();
    }

    // ================= ENTRAÎNEMENT =================

    @FXML
    private void handleTrainMOS() {
        System.out.println("✅ ENTRAÎNEMENT DU MODÈLE MOS CLIQUÉ");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Entraînement MOS");
        alert.setHeaderText("Lancement de l'entraînement du modèle MOS...");
        alert.setContentText("Le modèle de prédiction de qualité audio est en cours d'entraînement.");
        alert.show();

        new Thread(() -> {
            try {
                String report = PredictionServiceMOS.trainModel();

                Platform.runLater(() -> {
                    alert.close();
                    updateMOSDashboard();

                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("✅ Succès");
                    success.setHeaderText("Entraînement MOS terminé");
                    success.setContentText(report);
                    success.showAndWait();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    alert.close();
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("❌ Erreur");
                    error.setHeaderText("Erreur lors de l'entraînement MOS");
                    error.setContentText("Détails : " + e.getMessage());
                    error.showAndWait();
                    e.printStackTrace();
                });
            }
        }).start();
    }

    // ================= MISE À JOUR DASHBOARD MOS =================

    private void updateMOSDashboard() {
        if (!PredictionServiceMOS.isModelTrained()) {
            totalAudioLabel.setText("--");
            avgMOSLabel.setText("--");
            minMOSLabel.setText("--");
            maxMOSLabel.setText("--");
            rmseLabel.setText("--");
            return;
        }

        // Mise à jour des statistiques
        PredictionServiceMOS.ModelStats stats = PredictionServiceMOS.getModelStatistics();

        totalAudioLabel.setText(String.valueOf(stats.getTotalSamples()));
        avgMOSLabel.setText(String.format("%.2f", stats.getAverageMOS()));
        minMOSLabel.setText(String.format("%.2f", stats.getMinMOS()));
        maxMOSLabel.setText(String.format("%.2f", stats.getMaxMOS()));
        rmseLabel.setText(String.format("%.4f", stats.getRMSE()));

        // Mise à jour des graphiques
        updateMOSCharts();

        // Mise à jour du tableau
        updateQualityTable();
    }

    private void updateMOSCharts() {
        if (!PredictionServiceMOS.isModelTrained()) return;

        // Graphique 1: Tendances du MOS
        updateTrendChart();

        // Graphique 2: Distribution de la qualité
        updateQualityDistributionChart();

        // Graphique 3: Relation caractéristique-MOS
        updateFeatureMOSChart();
    }

    private void updateTrendChart() {
        if (mosTrendChart == null) return;

        mosTrendChart.getData().clear();
        mosTrendChart.setTitle("Évolution du MOS prédit");

        List<PredictionServiceMOS.MOSTrendData> trendData = PredictionServiceMOS.getMOSTrendData();

        if (trendData.isEmpty()) return;

        XYChart.Series<Number, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("MOS Réel");

        XYChart.Series<Number, Number> predictedSeries = new XYChart.Series<>();
        predictedSeries.setName("MOS Prédit");

        for (PredictionServiceMOS.MOSTrendData data : trendData) {
            actualSeries.getData().add(new XYChart.Data<>(data.getIndex(), data.getActualMOS()));
            predictedSeries.getData().add(new XYChart.Data<>(data.getIndex(), data.getPredictedMOS()));
        }

        mosTrendChart.getData().addAll(actualSeries, predictedSeries);
    }

    private void updateQualityDistributionChart() {
        if (qualityDistributionChart == null) return;

        qualityDistributionChart.getData().clear();
        qualityDistributionChart.setTitle("Distribution des niveaux de qualité");

        List<PredictionServiceMOS.QualityDistribution> distribution =
                PredictionServiceMOS.getQualityDistribution();

        if (distribution.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre d'échantillons");

        for (PredictionServiceMOS.QualityDistribution dist : distribution) {
            series.getData().add(new XYChart.Data<>(dist.getQualityLevel(), dist.getCount()));
        }

        qualityDistributionChart.getData().add(series);
    }

    private void updateFeatureMOSChart() {
        if (featureMOSChart == null) return;

        featureMOSChart.getData().clear();
        featureMOSChart.setTitle("SNR vs MOS");

        List<PredictionServiceMOS.FeatureMOSData> data =
                PredictionServiceMOS.getFeatureMOSData("snr");

        if (data.isEmpty()) return;

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Relation SNR-MOS");

        for (PredictionServiceMOS.FeatureMOSData point : data) {
            series.getData().add(new XYChart.Data<>(point.getFeatureValue(), point.getMOS()));
        }

        featureMOSChart.getData().add(series);
    }

    private void updateQualityTable() {
        if (qualityTable == null || !PredictionServiceMOS.isModelTrained()) return;

        List<PredictionServiceMOS.AudioQualityResult> results =
                PredictionServiceMOS.getAudioQualityResults();

        if (results.isEmpty()) return;

        var tableData = FXCollections.<AudioQualityTableData>observableArrayList();

        for (PredictionServiceMOS.AudioQualityResult result : results) {
            String qualityLevel = getQualityLevel(result.getPredictedMOS());

            tableData.add(new AudioQualityTableData(
                    result.getAudioId(),
                    result.getPredictedMOS(),
                    result.getActualMOS(),
                    result.getError(),
                    qualityLevel
            ));
        }

        qualityTable.setItems(tableData);
    }

    private String getQualityLevel(double mos) {
        if (mos >= 4.0) return "🔵 Excellente";
        if (mos >= 3.0) return "🟢 Bonne";
        if (mos >= 2.5) return "🟡 Acceptable";
        if (mos >= 2.0) return "🟠 Médiocre";
        return "🔴 Mauvaise";
    }

    // ================= PRÉDICTION MOS =================

    @FXML
    private void handlePredictMOS() {
        if (!PredictionServiceMOS.isModelReady()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("⚠️ Modèle non prêt");
            alert.setHeaderText("Le modèle MOS n'est pas encore entraîné");
            alert.setContentText("Veuillez d'abord entraîner le modèle depuis la page d'accueil.");
            alert.showAndWait();
            return;
        }

        try {
            double spectralCentroid = Double.parseDouble(spectralCentroidField.getText().trim());
            double spectralBandwidth = Double.parseDouble(spectralBandwidthField.getText().trim());
            double rms = Double.parseDouble(rmsField.getText().trim());
            double zcr = Double.parseDouble(zcrField.getText().trim());
            double snr = Double.parseDouble(snrField.getText().trim());
            double distortion = Double.parseDouble(distortionField.getText().trim());
            double noiseLevel = Double.parseDouble(noiseLevelField.getText().trim());

            // Validation des valeurs
            if (spectralCentroid < 0 || spectralBandwidth < 0 || rms < 0 ||
                    zcr < 0 || snr < 0 || distortion < 0 || noiseLevel < 0) {
                throw new IllegalArgumentException("Toutes les valeurs doivent être positives");
            }

            loadingIndicatorMOS.setVisible(true);
            predictedMOSLabel.setText("Calcul en cours...");
            qualityLevelLabel.setText("--");
            confidenceLabel.setText("--");

            new Thread(() -> {
                var result = PredictionServiceMOS.predictMOS(
                        spectralCentroid, spectralBandwidth, rms, zcr,
                        snr, distortion, noiseLevel
                );

                Platform.runLater(() -> {
                    loadingIndicatorMOS.setVisible(false);

                    double mos = result.getPredictedMOS();
                    predictedMOSLabel.setText(String.format("%.2f", mos));

                    String qualityLevel = getQualityLevel(mos);
                    qualityLevelLabel.setText(qualityLevel);

                    confidenceLabel.setText(String.format("±%.2f", result.getConfidenceInterval()));

                    // Mettre à jour la barre de progression
                    updateMOSBar(mos);

                    // Mettre à jour le style selon la qualité
                    updateMOSLabelStyle(mos);
                });
            }).start();

        } catch (NumberFormatException e) {
            showErrorAlert("Valeurs invalides",
                    "Veuillez entrer des valeurs numériques valides dans tous les champs.");
        } catch (IllegalArgumentException e) {
            showErrorAlert("Valeurs hors limites",
                    "Toutes les valeurs doivent être positives.");
        }
    }

    private void updateMOSBar(double mos) {
        mosBar.setProgress(mos / 5.0); // Normaliser entre 0 et 1

        if (mos >= 4.0) {
            mosBar.setStyle("-fx-accent: #3498db;"); // Bleu pour excellent
        } else if (mos >= 3.0) {
            mosBar.setStyle("-fx-accent: #2ecc71;"); // Vert pour bon
        } else if (mos >= 2.5) {
            mosBar.setStyle("-fx-accent: #f1c40f;"); // Jaune pour acceptable
        } else if (mos >= 2.0) {
            mosBar.setStyle("-fx-accent: #e67e22;"); // Orange pour médiocre
        } else {
            mosBar.setStyle("-fx-accent: #e74c3c;"); // Rouge pour mauvais
        }
    }

    private void updateMOSLabelStyle(double mos) {
        if (mos >= 4.0) {
            predictedMOSLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 32px;");
        } else if (mos >= 3.0) {
            predictedMOSLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 32px;");
        } else if (mos >= 2.5) {
            predictedMOSLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-size: 32px;");
        } else if (mos >= 2.0) {
            predictedMOSLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 32px;");
        } else {
            predictedMOSLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 32px;");
        }
    }

    private void simulateMOSPrediction() {
        if (!PredictionServiceMOS.isModelReady()) return;

        try {
            double rms = Double.parseDouble(rmsField.getText().trim());
            double noise = Double.parseDouble(noiseLevelField.getText().trim());
            double distortion = Double.parseDouble(distortionField.getText().trim());

            // Simulation simple (à remplacer par l'appel réel)
            double simulatedMOS = 4.5 - (noise * 2) - (distortion * 3);
            simulatedMOS = Math.max(1.0, Math.min(5.0, simulatedMOS));

            predictedMOSLabel.setText(String.format("%.2f", simulatedMOS));
            updateMOSBar(simulatedMOS);

        } catch (Exception e) {
            // Ignorer les erreurs pendant la simulation
        }
    }

    // ================= COMPARAISON =================

    @FXML
    private void updateComparisonChart() {
        if (comparisonChart == null || !PredictionServiceMOS.isModelTrained()) return;

        comparisonChart.getData().clear();
        String selectedMetric = comparisonMetricChoice.getValue();
        comparisonChart.setTitle(comparisonChartTitle(selectedMetric));

        if (showActualMOSCheck.isSelected()) {
            XYChart.Series<Number, Number> actualSeries = new XYChart.Series<>();
            actualSeries.setName("MOS Réel");
            // Ajouter les données de la série réelle
            comparisonChart.getData().add(actualSeries);
        }

        if (showPredictedMOSCheck.isSelected()) {
            XYChart.Series<Number, Number> predictedSeries = new XYChart.Series<>();
            predictedSeries.setName("MOS Prédit");
            // Ajouter les données de la série prédite
            comparisonChart.getData().add(predictedSeries);
        }

        updateComparisonText();
    }

    private String comparisonChartTitle(String metric) {
        switch (metric) {
            case "MOS vs Bande Passante": return "Influence de la bande passante sur le MOS";
            case "MOS vs Bruit": return "Influence du bruit sur le MOS";
            case "MOS vs Compression": return "Influence de la compression sur le MOS";
            case "MOS vs SNR": return "Relation SNR-MOS";
            default: return "Comparaison MOS";
        }
    }

    private void updateComparisonText() {
        if (!PredictionServiceMOS.isModelTrained()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("=== ANALYSE DE QUALITÉ AUDIO ===\n\n");

        PredictionServiceMOS.ModelStats stats = PredictionServiceMOS.getModelStatistics();
        sb.append("📊 Statistiques du modèle:\n");
        sb.append(String.format("• RMSE: %.4f\n", stats.getRMSE()));
        sb.append(String.format("• MAE: %.4f\n", stats.getMAE()));
        sb.append(String.format("• R² Score: %.4f\n\n", stats.getR2Score()));

        sb.append("🎯 Recommandations:\n");
        sb.append("• Pour améliorer le MOS (>4.0):\n");
        sb.append("  - SNR > 30 dB\n");
        sb.append("  - Bruit < 0.01\n");
        sb.append("  - Distorsion < 0.05\n\n");

        sb.append("⚠️ Facteurs dégradants:\n");
        sb.append("• Compression excessive\n");
        sb.append("• Bruit élevé\n");
        sb.append("• Faible bande passante\n");

        comparisonTextArea.setText(sb.toString());
    }

    // ================= UTILITAIRES =================

    private void resetPredictionFields() {
        spectralCentroidField.setText("0.5");
        spectralBandwidthField.setText("0.3");
        rmsField.setText("0.1");
        zcrField.setText("0.05");
        snrField.setText("25.0");
        distortionField.setText("0.02");
        noiseLevelField.setText("0.01");

        volumeSlider.setValue(50);
        noiseSlider.setValue(1);
        compressionSlider.setValue(40);

        predictedMOSLabel.setText("--");
        qualityLevelLabel.setText("--");
        confidenceLabel.setText("--");
        mosBar.setProgress(0);
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("❌ Erreur");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ================= CLASSE POUR LE TABLEAU =================

    public static class AudioQualityTableData {
        private final String audioId;
        private final Double predictedMOS;
        private final Double actualMOS;
        private final Double error;
        private final String qualityLevel;

        public AudioQualityTableData(String audioId, Double predictedMOS,
                                     Double actualMOS, Double error, String qualityLevel) {
            this.audioId = audioId;
            this.predictedMOS = predictedMOS;
            this.actualMOS = actualMOS;
            this.error = error;
            this.qualityLevel = qualityLevel;
        }

        public String getAudioId() { return audioId; }
        public Double getPredictedMOS() { return predictedMOS; }
        public Double getActualMOS() { return actualMOS; }
        public Double getError() { return error; }
        public String getQualityLevel() { return qualityLevel; }
    }
}