package com.ensah.qoe.Controller;

import com.ensah.qoe.Services.PredictionServiceMOS;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.util.*;

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

    private Random random = new Random();

    @FXML
    public void initialize() {
        System.out.println("🚀 Initialisation du contrôleur MOS Dashboard...");

        // Configuration du tableau
        if (qualityTable != null) {
            colAudioId.setCellValueFactory(new PropertyValueFactory<>("audioId"));
            colPredictedMOS.setCellValueFactory(new PropertyValueFactory<>("predictedMOS"));
            colActualMOS.setCellValueFactory(new PropertyValueFactory<>("actualMOS"));
            colError.setCellValueFactory(new PropertyValueFactory<>("error"));
            colQualityLevel.setCellValueFactory(new PropertyValueFactory<>("qualityLevel"));
            System.out.println("✅ Tableau qualité configuré");
        } else {
            System.out.println("⚠️ Tableau qualité non trouvé");
        }

        // Configuration des sliders
        setupSliders();
        System.out.println("✅ Sliders configurés");

        // Configuration de la page de comparaison
        setupComparisonPage();
        System.out.println("✅ Page comparaison configurée");

        // Initialisation des graphiques
        initializeCharts();
        System.out.println("✅ Graphiques initialisés");

        // Afficher la page d'accueil par défaut
        Platform.runLater(() -> {
            showAccueilMOS();
            System.out.println("✅ Page d'accueil affichée");
        });
    }

    private void initializeCharts() {
        System.out.println("📊 Initialisation des graphiques...");

        // Graphique de tendance MOS
        if (mosTrendChart != null) {
            NumberAxis xAxis = (NumberAxis) mosTrendChart.getXAxis();
            NumberAxis yAxis = (NumberAxis) mosTrendChart.getYAxis();

            xAxis.setLabel("Échantillon");
            xAxis.setAutoRanging(true);

            yAxis.setLabel("MOS");
            yAxis.setAutoRanging(true);
            yAxis.setLowerBound(1.0);
            yAxis.setUpperBound(5.0);
            yAxis.setTickUnit(0.5);

            mosTrendChart.setTitle("Évolution du MOS");
            mosTrendChart.setAnimated(false);
            mosTrendChart.setCreateSymbols(true);
            System.out.println("✅ Graphique de tendance initialisé");
        }

        // Graphique de distribution
        if (qualityDistributionChart != null) {
            CategoryAxis xAxis = (CategoryAxis) qualityDistributionChart.getXAxis();
            NumberAxis yAxis = (NumberAxis) qualityDistributionChart.getYAxis();

            xAxis.setLabel("Niveau de Qualité");
            yAxis.setLabel("Nombre d'Échantillons");

            qualityDistributionChart.setTitle("Distribution de la Qualité Audio");
            qualityDistributionChart.setLegendVisible(true);
            qualityDistributionChart.setAnimated(true);
            System.out.println("✅ Graphique de distribution initialisé");
        }

        // Graphique feature vs MOS
        if (featureMOSChart != null) {
            NumberAxis xAxis = (NumberAxis) featureMOSChart.getXAxis();
            NumberAxis yAxis = (NumberAxis) featureMOSChart.getYAxis();

            xAxis.setLabel("SNR (dB)");
            xAxis.setAutoRanging(true);

            yAxis.setLabel("MOS");
            yAxis.setAutoRanging(true);
            yAxis.setLowerBound(1.0);
            yAxis.setUpperBound(5.0);
            yAxis.setTickUnit(0.5);

            featureMOSChart.setTitle("Relation SNR vs MOS");
            featureMOSChart.setLegendVisible(true);
            featureMOSChart.setAnimated(true);
            System.out.println("✅ Graphique feature-MOS initialisé");
        }
    }

    private void setupSliders() {
        // Volume slider → RMS
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double rmsValue = newVal.doubleValue() / 100.0 * 0.5;
            rmsField.setText(String.format("%.3f", rmsValue));
            simulateMOSPrediction();
        });

        // Noise slider → Noise Level
        noiseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double noiseValue = newVal.doubleValue() / 100.0;
            noiseLevelField.setText(String.format("%.3f", noiseValue));
            simulateMOSPrediction();
        });

        // Compression slider → Distortion
        compressionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double distortionValue = newVal.doubleValue() / 100.0 * 0.1;
            distortionField.setText(String.format("%.3f", distortionValue));
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
        comparisonMetricChoice.setValue("MOS vs SNR");
        comparisonMetricChoice.setOnAction(e -> updateComparisonChart());

        showActualMOSCheck.setSelected(true);
        showPredictedMOSCheck.setSelected(true);

        showActualMOSCheck.setOnAction(e -> updateComparisonChart());
        showPredictedMOSCheck.setOnAction(e -> updateComparisonChart());
    }

    // ================= NAVIGATION =================

    @FXML
    private void showAccueilMOS() {
        System.out.println("📱 Navigation vers page d'accueil MOS");

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
        System.out.println("📱 Navigation vers prédiction MOS");

        accueilMOSScroll.setVisible(false);
        accueilMOSScroll.setManaged(false);
        predictionMOSScroll.setVisible(true);
        predictionMOSScroll.setManaged(true);
        comparisonMOSScroll.setVisible(false);
        comparisonMOSScroll.setManaged(false);

        btnAccueilMOS.getStyleClass().setAll("nav-btn");
        btnPredictionMOS.getStyleClass().setAll("nav-btn-active");
        btnComparisonMOS.getStyleClass().setAll("nav-btn");

        resetPredictionFields();
    }

    @FXML
    private void showComparisonMOS() {
        System.out.println("📱 Navigation vers comparaison MOS");

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
        System.out.println("🎯 Lancement entraînement du modèle MOS");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Entraînement MOS");
        alert.setHeaderText("Lancement de l'entraînement du modèle MOS...");
        alert.setContentText("Veuillez patienter pendant l'entraînement du modèle de prédiction de qualité audio.");
        alert.show();

        new Thread(() -> {
            try {
                String report = PredictionServiceMOS.trainModel();
                System.out.println("✅ Entraînement terminé avec succès");

                Platform.runLater(() -> {
                    alert.close();

                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("✅ Succès");
                    success.setHeaderText("Entraînement MOS terminé");
                    success.setContentText(report);
                    success.showAndWait();

                    updateMOSDashboard();
                });

            } catch (Exception e) {
                System.err.println("❌ Erreur lors de l'entraînement: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    alert.close();

                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("❌ Erreur");
                    error.setHeaderText("Erreur lors de l'entraînement MOS");
                    error.setContentText("Détails : " + e.getMessage());
                    error.showAndWait();
                });
            }
        }).start();
    }

    // ================= MISE À JOUR DASHBOARD MOS =================

    private void updateMOSDashboard() {
        System.out.println("🔄 Mise à jour du dashboard MOS...");

        if (!PredictionServiceMOS.isModelTrained()) {
            System.out.println("⚠️ Modèle non entraîné - affichage valeurs par défaut");

            totalAudioLabel.setText("0");
            avgMOSLabel.setText("--");
            minMOSLabel.setText("--");
            maxMOSLabel.setText("--");
            rmseLabel.setText("--");

            // Afficher des données de démonstration
            displayDemoCharts();
            displayDemoTable();
            return;
        }

        try {
            // Récupérer les statistiques
            PredictionServiceMOS.ModelStats stats = PredictionServiceMOS.getModelStatistics();

            totalAudioLabel.setText(String.valueOf(stats.getTotalSamples()));
            avgMOSLabel.setText(String.format("%.2f", stats.getAverageMOS()));
            minMOSLabel.setText(String.format("%.2f", stats.getMinMOS()));
            maxMOSLabel.setText(String.format("%.2f", stats.getMaxMOS()));
            rmseLabel.setText(String.format("%.4f", stats.getRMSE()));

            System.out.println("📊 Statistiques chargées: " + stats.getTotalSamples() + " échantillons");

            // Mettre à jour les graphiques
            updateMOSCharts();

            // Mettre à jour le tableau
            updateQualityTable();

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour du dashboard: " + e.getMessage());
            showErrorAlert("Erreur Dashboard", "Impossible de charger les données: " + e.getMessage());
        }
    }

    private void updateMOSCharts() {
        System.out.println("📈 Mise à jour des graphiques...");

        // Graphique 1: Tendances du MOS
        updateTrendChart();

        // Graphique 2: Distribution de la qualité
        updateQualityDistributionChart();

        // Graphique 3: Relation caractéristique-MOS
        updateFeatureMOSChart();
    }

    private void updateTrendChart() {
        if (mosTrendChart == null) {
            System.out.println("⚠️ mosTrendChart est null");
            return;
        }

        try {
            mosTrendChart.getData().clear();

            List<PredictionServiceMOS.MOSTrendData> trendData = PredictionServiceMOS.getMOSTrendData();

            if (trendData.isEmpty()) {
                System.out.println("⚠️ Aucune donnée de tendance - génération données de démo");
                createDemoTrendChart();
                return;
            }

            XYChart.Series<Number, Number> actualSeries = new XYChart.Series<>();
            actualSeries.setName("MOS Réel");

            XYChart.Series<Number, Number> predictedSeries = new XYChart.Series<>();
            predictedSeries.setName("MOS Prédit");

            for (PredictionServiceMOS.MOSTrendData data : trendData) {
                actualSeries.getData().add(new XYChart.Data<>(data.getIndex(), data.getActualMOS()));
                predictedSeries.getData().add(new XYChart.Data<>(data.getIndex(), data.getPredictedMOS()));
            }

            mosTrendChart.getData().addAll(actualSeries, predictedSeries);
            System.out.println("✅ Graphique de tendance mis à jour avec " + trendData.size() + " points");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour du graphique de tendance: " + e.getMessage());
            createDemoTrendChart();
        }
    }

    private void updateQualityDistributionChart() {
        if (qualityDistributionChart == null) {
            System.out.println("⚠️ qualityDistributionChart est null");
            return;
        }

        try {
            qualityDistributionChart.getData().clear();

            List<PredictionServiceMOS.QualityDistribution> distribution =
                    PredictionServiceMOS.getQualityDistribution();

            if (distribution.isEmpty()) {
                System.out.println("⚠️ Aucune donnée de distribution - génération données de démo");
                createDemoDistributionChart();
                return;
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Distribution");

            for (PredictionServiceMOS.QualityDistribution dist : distribution) {
                series.getData().add(new XYChart.Data<>(dist.getQualityLevel(), dist.getCount()));
            }

            qualityDistributionChart.getData().add(series);
            System.out.println("✅ Graphique de distribution mis à jour avec " + distribution.size() + " catégories");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour du graphique de distribution: " + e.getMessage());
            createDemoDistributionChart();
        }
    }

    private void updateFeatureMOSChart() {
        if (featureMOSChart == null) {
            System.out.println("⚠️ featureMOSChart est null");
            return;
        }

        try {
            featureMOSChart.getData().clear();

            List<PredictionServiceMOS.FeatureMOSData> data =
                    PredictionServiceMOS.getFeatureMOSData("snr");

            if (data.isEmpty()) {
                System.out.println("⚠️ Aucune donnée feature-MOS - génération données de démo");
                createDemoFeatureChart();
                return;
            }

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("SNR vs MOS");

            for (PredictionServiceMOS.FeatureMOSData point : data) {
                series.getData().add(new XYChart.Data<>(point.getFeatureValue(), point.getMOS()));
            }

            featureMOSChart.getData().add(series);
            System.out.println("✅ Graphique feature-MOS mis à jour avec " + data.size() + " points");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour du graphique feature-MOS: " + e.getMessage());
            createDemoFeatureChart();
        }
    }

    private void updateQualityTable() {
        if (qualityTable == null) {
            System.out.println("⚠️ qualityTable est null");
            return;
        }

        if (!PredictionServiceMOS.isModelTrained()) {
            displayDemoTable();
            return;
        }

        try {
            List<PredictionServiceMOS.AudioQualityResult> results =
                    PredictionServiceMOS.getAudioQualityResults();

            if (results.isEmpty()) {
                displayDemoTable();
                return;
            }

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
            System.out.println("✅ Tableau mis à jour avec " + results.size() + " lignes");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour du tableau: " + e.getMessage());
            displayDemoTable();
        }
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
        System.out.println("🎯 Lancement prédiction MOS");

        if (!PredictionServiceMOS.isModelReady()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("⚠️ Modèle non prêt");
            alert.setHeaderText("Le modèle MOS n'est pas encore entraîné");
            alert.setContentText("Veuillez d'abord entraîner le modèle depuis la page d'accueil.");
            alert.showAndWait();
            return;
        }

        try {
            // Récupérer et valider les valeurs
            double spectralCentroid = parseDoubleField(spectralCentroidField, "Centroïde spectral");
            double spectralBandwidth = parseDoubleField(spectralBandwidthField, "Bande passante spectrale");
            double rms = parseDoubleField(rmsField, "RMS");
            double zcr = parseDoubleField(zcrField, "ZCR");
            double snr = parseDoubleField(snrField, "SNR");
            double distortion = parseDoubleField(distortionField, "Distorsion");
            double noiseLevel = parseDoubleField(noiseLevelField, "Niveau de bruit");

            // Validation des valeurs
            validateValues(spectralCentroid, spectralBandwidth, rms, zcr, snr, distortion, noiseLevel);

            // Afficher l'indicateur de chargement
            loadingIndicatorMOS.setVisible(true);
            predictedMOSLabel.setText("Calcul...");
            qualityLevelLabel.setText("--");
            confidenceLabel.setText("--");

            // Lancer la prédiction dans un thread séparé
            new Thread(() -> {
                try {
                    PredictionServiceMOS.MOSResult result = PredictionServiceMOS.predictMOS(
                            spectralCentroid, spectralBandwidth, rms, zcr,
                            snr, distortion, noiseLevel
                    );

                    Platform.runLater(() -> {
                        loadingIndicatorMOS.setVisible(false);

                        double mos = result.getPredictedMOS();
                        predictedMOSLabel.setText(String.format("%.2f", mos));
                        qualityLevelLabel.setText(result.getQualityLevel());
                        confidenceLabel.setText(String.format("±%.2f", result.getConfidenceInterval()));

                        updateMOSBar(mos);
                        updateMOSLabelStyle(mos);

                        System.out.println("✅ Prédiction réussie: MOS = " + mos);
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        loadingIndicatorMOS.setVisible(false);
                        showErrorAlert("Erreur Prédiction", "Erreur lors de la prédiction: " + e.getMessage());
                        e.printStackTrace();
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            showErrorAlert("Format invalide", "Veuillez entrer des nombres valides dans tous les champs.");
        } catch (IllegalArgumentException e) {
            showErrorAlert("Valeurs invalides", e.getMessage());
        }
    }

    private double parseDoubleField(TextField field, String fieldName) throws NumberFormatException {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            throw new NumberFormatException(fieldName + " est vide");
        }
        return Double.parseDouble(text);
    }

    private void validateValues(double spectralCentroid, double spectralBandwidth,
                                double rms, double zcr, double snr,
                                double distortion, double noiseLevel) {
        if (spectralCentroid < 0 || spectralCentroid > 1)
            throw new IllegalArgumentException("Centroïde spectral doit être entre 0 et 1");
        if (spectralBandwidth < 0 || spectralBandwidth > 1)
            throw new IllegalArgumentException("Bande passante spectrale doit être entre 0 et 1");
        if (rms < 0 || rms > 1)
            throw new IllegalArgumentException("RMS doit être entre 0 et 1");
        if (zcr < 0 || zcr > 1)
            throw new IllegalArgumentException("ZCR doit être entre 0 et 1");
        if (snr < 0 || snr > 100)
            throw new IllegalArgumentException("SNR doit être entre 0 et 100 dB");
        if (distortion < 0 || distortion > 1)
            throw new IllegalArgumentException("Distorsion doit être entre 0 et 1");
        if (noiseLevel < 0 || noiseLevel > 1)
            throw new IllegalArgumentException("Niveau de bruit doit être entre 0 et 1");
    }

    private void updateMOSBar(double mos) {
        double progress = mos / 5.0;
        mosBar.setProgress(progress);

        // Changer la couleur selon la qualité
        if (mos >= 4.0) {
            mosBar.setStyle("-fx-accent: #3498db; -fx-control-inner-background: #ecf0f1;");
        } else if (mos >= 3.0) {
            mosBar.setStyle("-fx-accent: #2ecc71; -fx-control-inner-background: #ecf0f1;");
        } else if (mos >= 2.5) {
            mosBar.setStyle("-fx-accent: #f1c40f; -fx-control-inner-background: #ecf0f1;");
        } else if (mos >= 2.0) {
            mosBar.setStyle("-fx-accent: #e67e22; -fx-control-inner-background: #ecf0f1;");
        } else {
            mosBar.setStyle("-fx-accent: #e74c3c; -fx-control-inner-background: #ecf0f1;");
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
            // Récupérer les valeurs des sliders
            double rms = volumeSlider.getValue() / 100.0 * 0.5;
            double noise = noiseSlider.getValue() / 100.0;
            double distortion = compressionSlider.getValue() / 100.0 * 0.1;
            double snr = 25.0 - (noise * 20) - (distortion * 30);

            // Simulation simple du MOS
            double simulatedMOS = 4.5 - (noise * 2.5) - (distortion * 3.0) + (rms * 1.5);
            simulatedMOS = Math.max(1.0, Math.min(5.0, simulatedMOS));

            predictedMOSLabel.setText(String.format("%.2f", simulatedMOS));
            updateMOSBar(simulatedMOS);
            qualityLevelLabel.setText(getQualityLevel(simulatedMOS));
            confidenceLabel.setText(String.format("±%.2f", 0.3 + (noise * 0.5)));

        } catch (Exception e) {
            // Ignorer les erreurs pendant la simulation
        }
    }

    // ================= COMPARAISON =================

    @FXML
    private void updateComparisonChart() {
        if (comparisonChart == null) return;

        comparisonChart.getData().clear();
        String selectedMetric = comparisonMetricChoice.getValue();
        comparisonChart.setTitle(getComparisonTitle(selectedMetric));

        // Configurer les axes
        NumberAxis xAxis = (NumberAxis) comparisonChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) comparisonChart.getYAxis();

        xAxis.setLabel(getXAxisLabel(selectedMetric));
        yAxis.setLabel("MOS");
        yAxis.setLowerBound(1.0);
        yAxis.setUpperBound(5.0);
        yAxis.setTickUnit(0.5);

        if (showActualMOSCheck.isSelected()) {
            XYChart.Series<Number, Number> actualSeries = new XYChart.Series<>();
            actualSeries.setName("MOS Réel");

            // Générer des données de démonstration
            for (int i = 0; i < 20; i++) {
                double x = i * 2.5;
                double y = 2.5 + Math.sin(i * 0.5) * 1.5 + random.nextDouble() * 0.5;
                y = Math.max(1.0, Math.min(5.0, y));
                actualSeries.getData().add(new XYChart.Data<>(x, y));
            }

            comparisonChart.getData().add(actualSeries);
        }

        if (showPredictedMOSCheck.isSelected()) {
            XYChart.Series<Number, Number> predictedSeries = new XYChart.Series<>();
            predictedSeries.setName("MOS Prédit");

            // Générer des données de démonstration avec bruit
            for (int i = 0; i < 20; i++) {
                double x = i * 2.5;
                double baseY = 2.5 + Math.sin(i * 0.5) * 1.5;
                double y = baseY + (random.nextDouble() - 0.5) * 0.8;
                y = Math.max(1.0, Math.min(5.0, y));
                predictedSeries.getData().add(new XYChart.Data<>(x, y));
            }

            comparisonChart.getData().add(predictedSeries);
        }

        updateComparisonText();
    }

    private String getComparisonTitle(String metric) {
        switch (metric) {
            case "MOS vs Bande Passante": return "Influence de la Bande Passante sur le MOS";
            case "MOS vs Bruit": return "Influence du Bruit sur le MOS";
            case "MOS vs Compression": return "Influence de la Compression sur le MOS";
            case "MOS vs SNR": return "Relation SNR vs MOS";
            default: return "Comparaison MOS";
        }
    }

    private String getXAxisLabel(String metric) {
        switch (metric) {
            case "MOS vs Bande Passante": return "Bande Passante (kHz)";
            case "MOS vs Bruit": return "Niveau de Bruit";
            case "MOS vs Compression": return "Taux de Compression";
            case "MOS vs SNR": return "SNR (dB)";
            default: return "Paramètre";
        }
    }

    private void updateComparisonText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ANALYSE COMPARATIVE DE QUALITÉ AUDIO ===\n\n");

        sb.append("📊 PERFORMANCE DU MODÈLE\n");
        sb.append("-----------------------------\n");

        if (PredictionServiceMOS.isModelTrained()) {
            PredictionServiceMOS.ModelStats stats = PredictionServiceMOS.getModelStatistics();
            sb.append(String.format("• RMSE        : %.4f\n", stats.getRMSE()));
            sb.append(String.format("• MAE         : %.4f\n", stats.getMAE()));
            sb.append(String.format("• Score R²    : %.4f\n", stats.getR2Score()));
            sb.append(String.format("• Échantillons : %d\n\n", stats.getTotalSamples()));
        } else {
            sb.append("• Modèle non entraîné\n\n");
        }

        sb.append("🎯 RECOMMANDATIONS D'AMÉLIORATION\n");
        sb.append("-----------------------------\n");
        sb.append("Pour un MOS > 4.0 (Excellente qualité) :\n");
        sb.append("  ✓ SNR > 30 dB\n");
        sb.append("  ✓ Bruit < 0.05\n");
        sb.append("  ✓ Distorsion < 0.03\n");
        sb.append("  ✓ Bande passante > 8 kHz\n\n");

        sb.append("⚠️ FACTEURS DE DÉGRADATION\n");
        sb.append("-----------------------------\n");
        sb.append("• Compression excessive (bitrate < 64 kbps)\n");
        sb.append("• Bruit de fond élevé\n");
        sb.append("• Distorsion harmonique\n");
        sb.append("• Faible rapport signal/bruit\n");

        comparisonTextArea.setText(sb.toString());
    }

    // ================= DONNÉES DE DÉMONSTRATION =================

    private void displayDemoCharts() {
        System.out.println("📊 Affichage graphiques de démonstration");
        createDemoTrendChart();
        createDemoDistributionChart();
        createDemoFeatureChart();
    }

    private void createDemoTrendChart() {
        if (mosTrendChart == null) return;

        mosTrendChart.getData().clear();

        XYChart.Series<Number, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("MOS Réel (Démo)");

        XYChart.Series<Number, Number> predictedSeries = new XYChart.Series<>();
        predictedSeries.setName("MOS Prédit (Démo)");

        for (int i = 0; i < 15; i++) {
            double base = 3.0 + Math.sin(i * 0.4) * 0.8;
            double actual = base + random.nextDouble() * 0.3;
            double predicted = base + (random.nextDouble() - 0.5) * 0.4;

            actual = Math.max(1.0, Math.min(5.0, actual));
            predicted = Math.max(1.0, Math.min(5.0, predicted));

            actualSeries.getData().add(new XYChart.Data<>(i, actual));
            predictedSeries.getData().add(new XYChart.Data<>(i, predicted));
        }

        mosTrendChart.getData().addAll(actualSeries, predictedSeries);
    }

    private void createDemoDistributionChart() {
        if (qualityDistributionChart == null) return;

        qualityDistributionChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Distribution (Démo)");

        String[] levels = {"Mauvaise", "Médiocre", "Acceptable", "Bonne", "Excellente"};
        int[] counts = {5, 8, 12, 15, 10};

        for (int i = 0; i < levels.length; i++) {
            series.getData().add(new XYChart.Data<>(levels[i], counts[i]));
        }

        qualityDistributionChart.getData().add(series);
    }

    private void createDemoFeatureChart() {
        if (featureMOSChart == null) return;

        featureMOSChart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("SNR vs MOS (Démo)");

        for (int i = 0; i < 30; i++) {
            double snr = 10 + random.nextDouble() * 30;
            double mos = 2.0 + (snr / 40.0) * 2.5 + (random.nextDouble() - 0.5) * 0.6;
            mos = Math.max(1.0, Math.min(5.0, mos));

            series.getData().add(new XYChart.Data<>(snr, mos));
        }

        featureMOSChart.getData().add(series);
    }

    private void displayDemoTable() {
        if (qualityTable == null) return;

        var tableData = FXCollections.<AudioQualityTableData>observableArrayList();

        for (int i = 1; i <= 10; i++) {
            double actualMOS = 2.5 + random.nextDouble() * 2.0;
            double predictedMOS = actualMOS + (random.nextDouble() - 0.5) * 0.3;
            double error = Math.abs(actualMOS - predictedMOS);
            String qualityLevel = getQualityLevel(predictedMOS);

            tableData.add(new AudioQualityTableData(
                    "audio_demo_" + i,
                    predictedMOS,
                    actualMOS,
                    error,
                    qualityLevel
            ));
        }

        qualityTable.setItems(tableData);
    }

    // ================= UTILITAIRES =================

    private void resetPredictionFields() {
        // Valeurs par défaut réalistes
        spectralCentroidField.setText("0.45");
        spectralBandwidthField.setText("0.35");
        rmsField.setText("0.18");
        zcrField.setText("0.12");
        snrField.setText("28.5");
        distortionField.setText("0.03");
        noiseLevelField.setText("0.08");

        // Positionner les sliders
        volumeSlider.setValue(36);   // 0.18 * 100 / 0.5
        noiseSlider.setValue(8);     // 0.08 * 100
        compressionSlider.setValue(30); // 0.03 * 100 / 0.1

        // Réinitialiser les résultats
        predictedMOSLabel.setText("--");
        qualityLevelLabel.setText("--");
        confidenceLabel.setText("--");
        mosBar.setProgress(0);

        // Réinitialiser les styles
        predictedMOSLabel.setStyle("-fx-text-fill: black; -fx-font-size: 32px;");
        mosBar.setStyle("");
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