package com.ensah.qoe.Controller;

import com.ensah.qoe.ML.DataPreparation;
import com.ensah.qoe.Services.PredictionService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import weka.core.Instances;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MLDashboardController {

    @FXML private Label statusLabel, datasetSizeLabel, modelTypeLabel, accuracyLabel, predictionsCountLabel;
    @FXML private LineChart<String, Number> performanceChart;
    @FXML private TableView<Map<String, String>> recentPredictionsTable;

    // Entraînement
    @FXML private ComboBox<String> modelTypeCombo, validationCombo;
    @FXML private Slider trainRatioSlider;
    @FXML private Button trainBtn;
    @FXML private ProgressBar trainingProgressBar;
    @FXML private Label trainingProgressLabel;
    @FXML private TextArea trainingResultsArea;
    @FXML private BarChart<String, Number> featureImportanceChart;

    // Prédiction
    @FXML private TextField latenceField, jitterField, perteField, bandePassanteField, signalField;
    @FXML private Slider latenceSlider, jitterSlider, perteSlider, bandePassanteSlider, signalSlider;
    @FXML private Button predictBtn;
    @FXML private Label predictedMOSLabel, qoeCategoryLabel;
    @FXML private ProgressBar qualityIndicator;
    @FXML private TableView<Map<String, String>> predictionHistoryTable;

    // Tests
    @FXML private Button quickTestBtn, evaluateBtn, predictMissingBtn, fullTestBtn;
    @FXML private TextArea testOutputArea;
    @FXML private Label maeLabel, rmseLabel, r2Label, correlationLabel;

    // Visualisation
    @FXML private BarChart<String, Number> mosDistributionChart;
    @FXML private ScatterChart<Number, Number> mosVsLatenceChart, mosVsPerteChart;

    // Footer
    @FXML private Label modelInfoLabel, dataInfoLabel, lastUpdateLabel;

    private ObservableList<Map<String, String>> predictionHistory = FXCollections.observableArrayList();
    private PrintStream customOut;
    private volatile boolean isTraining = false; // Flag pour éviter les doubles exécutions

    @FXML
    private void initialize() {
        modelTypeCombo.getItems().addAll("Random Forest", "Linear Regression", "Decision Tree");
        validationCombo.getItems().addAll("Validation croisée (10-fold)", "Hold-out");

        modelTypeCombo.setValue("Random Forest");
        validationCombo.setValue("Validation croisée (10-fold)");

        setupConsoleRedirect();
        setupUIComponents();
        loadInitialData();
        bindSlidersAndFields();

        // Initialiser le graphique d'importance
        initializeFeatureImportance();
    }

    private void setupConsoleRedirect() {
        customOut = new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                char c = (char) b;
                buffer.append(c);

                if (c == '\n') {
                    final String line = buffer.toString();
                    buffer.setLength(0);

                    Platform.runLater(() -> {
                        if (trainingResultsArea != null) {
                            trainingResultsArea.appendText(line);
                            trainingResultsArea.setScrollTop(Double.MAX_VALUE);
                        }
                        if (testOutputArea != null) {
                            testOutputArea.appendText(line);
                            testOutputArea.setScrollTop(Double.MAX_VALUE);
                        }
                    });
                }
            }
        });
    }

    private void setupUIComponents() {
        setupTables();
        initializeCharts();
        configureSliders();
    }

    private void configureSliders() {
        latenceSlider.setMin(0);
        latenceSlider.setMax(500);
        latenceSlider.setValue(50);

        jitterSlider.setMin(0);
        jitterSlider.setMax(100);
        jitterSlider.setValue(10);

        perteSlider.setMin(0);
        perteSlider.setMax(20);
        perteSlider.setValue(1);

        bandePassanteSlider.setMin(0);
        bandePassanteSlider.setMax(200);
        bandePassanteSlider.setValue(50);

        signalSlider.setMin(0);
        signalSlider.setMax(100);
        signalSlider.setValue(80);
    }

    private void setupTables() {
        recentPredictionsTable.setPlaceholder(new Label("Aucune donnée disponible"));
        setupPredictionTableColumns();

        predictionHistoryTable.setItems(predictionHistory);
        predictionHistoryTable.setPlaceholder(new Label("Aucune prédiction effectuée"));
        setupHistoryTableColumns();
    }

    private void setupPredictionTableColumns() {
        String[] columnNames = {"id", "latence", "jitter", "perte", "bande_passante", "signal", "mos", "category", "date"};
        String[] columnTitles = {"ID", "Latence", "Jitter", "Perte %", "Bande P.", "Signal", "MOS", "Catégorie", "Date"};

        for (int i = 0; i < columnNames.length; i++) {
            TableColumn<Map<String, String>, String> column = new TableColumn<>(columnTitles[i]);
            final String key = columnNames[i];
            column.setCellValueFactory(param ->
                    new SimpleStringProperty(param.getValue().get(key)));
            column.setPrefWidth(80);
            recentPredictionsTable.getColumns().add(column);
        }
    }

    private void setupHistoryTableColumns() {
        predictionHistoryTable.getColumns().clear();

        String[] columnNames = {"time", "latence", "jitter", "perte", "mos", "category"};
        String[] columnTitles = {"Heure", "Latence", "Jitter", "Perte %", "MOS", "Catégorie"};

        for (int i = 0; i < columnNames.length; i++) {
            TableColumn<Map<String, String>, String> column = new TableColumn<>(columnTitles[i]);
            final String key = columnNames[i];
            column.setCellValueFactory(param ->
                    new SimpleStringProperty(param.getValue().get(key)));
            column.setPrefWidth(100);
            predictionHistoryTable.getColumns().add(column);
        }
    }

    private void initializeCharts() {
        performanceChart.getData().clear();
        featureImportanceChart.getData().clear();
        mosDistributionChart.getData().clear();

        XYChart.Series<Number, Number> latenceSeries = new XYChart.Series<>();
        latenceSeries.setName("MOS vs Latence");
        mosVsLatenceChart.getData().add(latenceSeries);

        XYChart.Series<Number, Number> perteSeries = new XYChart.Series<>();
        perteSeries.setName("MOS vs Perte");
        mosVsPerteChart.getData().add(perteSeries);
    }

    private void initializeFeatureImportance() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Latence", 30));
        series.getData().add(new XYChart.Data<>("Perte", 25));
        series.getData().add(new XYChart.Data<>("Jitter", 20));
        series.getData().add(new XYChart.Data<>("Bande P.", 15));
        series.getData().add(new XYChart.Data<>("Signal", 10));

        Platform.runLater(() -> {
            featureImportanceChart.getData().clear();
            featureImportanceChart.getData().add(series);
        });
    }

    private void bindSlidersAndFields() {
        bindSliderToField(latenceSlider, latenceField);
        bindSliderToField(jitterSlider, jitterField);
        bindSliderToField(perteSlider, perteField);
        bindSliderToField(bandePassanteSlider, bandePassanteField);
        bindSliderToField(signalSlider, signalField);
    }

    private void bindSliderToField(Slider slider, TextField field) {
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            field.setText(String.format("%.1f", newVal.doubleValue()));
        });

        field.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double value = Double.parseDouble(newVal);
                if (value >= slider.getMin() && value <= slider.getMax()) {
                    slider.setValue(value);
                }
            } catch (NumberFormatException e) {
                // Ignorer
            }
        });
    }

    private void loadInitialData() {
        updateStatus("Chargement des données...", "info");

        Thread loadThread = new Thread(() -> {
            try {
                loadDatasetStats();
                loadRecentPredictions();
                loadPerformanceMetrics();
                loadMOSDistribution();
                loadScatterData();

                Platform.runLater(() -> {
                    updateStatus("Prêt", "success");
                    updateFooterInfo();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Erreur de chargement: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void loadDatasetStats() {
        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection()) {
            String countSQL = "SELECT COUNT(*) FROM MESURES_QOS WHERE MOS IS NOT NULL";
            try (PreparedStatement ps = conn.prepareStatement(countSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    Platform.runLater(() -> datasetSizeLabel.setText(String.valueOf(count)));
                }
            }

            String predSQL = "SELECT COUNT(*) FROM MESURES_QOS WHERE MOS IS NOT NULL AND MOS > 0";
            try (PreparedStatement ps = conn.prepareStatement(predSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    Platform.runLater(() -> predictionsCountLabel.setText(String.valueOf(count)));
                }
            }

        } catch (SQLException e) {
            System.err.println("Erreur chargement stats: " + e.getMessage());
        }
    }

    private void loadRecentPredictions() {
        ObservableList<Map<String, String>> data = FXCollections.observableArrayList();

        String sql = """
            SELECT 
                ID_MESURE, LATENCE, JITTER, PERTE, BANDE_PASSANTE, 
                SIGNAL_SCORE, MOS, TO_CHAR(SYSDATE, 'DD/MM HH24:MI') as PRED_DATE
            FROM MESURES_QOS
            WHERE MOS IS NOT NULL
            ORDER BY ID_MESURE DESC
            FETCH FIRST 10 ROWS ONLY
        """;

        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id", String.valueOf(rs.getInt("ID_MESURE")));
                row.put("latence", String.format("%.1f", rs.getDouble("LATENCE")));
                row.put("jitter", String.format("%.1f", rs.getDouble("JITTER")));
                row.put("perte", String.format("%.2f", rs.getDouble("PERTE")));
                row.put("bande_passante", String.format("%.1f", rs.getDouble("BANDE_PASSANTE")));
                row.put("signal", String.format("%.1f", rs.getDouble("SIGNAL_SCORE")));
                row.put("mos", String.format("%.2f", rs.getDouble("MOS")));
                row.put("date", rs.getString("PRED_DATE"));
                row.put("category", getQoECategory(rs.getDouble("MOS")));

                data.add(row);
            }

            Platform.runLater(() -> recentPredictionsTable.getItems().setAll(data));

        } catch (SQLException e) {
            System.err.println("Erreur chargement prédictions: " + e.getMessage());
        }
    }

    private void loadScatterData() {
        String sql = """
            SELECT LATENCE, PERTE, MOS 
            FROM MESURES_QOS 
            WHERE MOS IS NOT NULL AND LATENCE IS NOT NULL AND PERTE IS NOT NULL
            FETCH FIRST 50 ROWS ONLY
        """;

        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            XYChart.Series<Number, Number> latenceSeries = new XYChart.Series<>();
            XYChart.Series<Number, Number> perteSeries = new XYChart.Series<>();

            while (rs.next()) {
                double latence = rs.getDouble("LATENCE");
                double perte = rs.getDouble("PERTE");
                double mos = rs.getDouble("MOS");

                latenceSeries.getData().add(new XYChart.Data<>(latence, mos));
                perteSeries.getData().add(new XYChart.Data<>(perte, mos));
            }

            Platform.runLater(() -> {
                mosVsLatenceChart.getData().clear();
                mosVsLatenceChart.getData().add(latenceSeries);

                mosVsPerteChart.getData().clear();
                mosVsPerteChart.getData().add(perteSeries);
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement scatter: " + e.getMessage());
        }
    }

    private String getQoECategory(double mos) {
        if (mos >= 4.5) return "Excellent";
        else if (mos >= 4.0) return "Bon";
        else if (mos >= 3.0) return "Moyen";
        else if (mos >= 2.0) return "Médiocre";
        else return "Mauvais";
    }

    private void loadPerformanceMetrics() {
        Platform.runLater(() -> {
            modelTypeLabel.setText("Random Forest");
            accuracyLabel.setText("0.85");
            maeLabel.setText("0.42");
            rmseLabel.setText("0.65");
            r2Label.setText("0.78");
            correlationLabel.setText("0.88");

            performanceChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("MAE", 0.42));
            series.getData().add(new XYChart.Data<>("RMSE", 0.65));
            series.getData().add(new XYChart.Data<>("R²", 0.78));
            series.getData().add(new XYChart.Data<>("Corr", 0.88));
            performanceChart.getData().add(series);
        });
    }

    private void loadMOSDistribution() {
        String sql = """
            SELECT 
                CASE 
                    WHEN MOS >= 4.5 THEN 'Excellent'
                    WHEN MOS >= 4.0 THEN 'Bon'
                    WHEN MOS >= 3.0 THEN 'Moyen'
                    WHEN MOS >= 2.0 THEN 'Médiocre'
                    ELSE 'Mauvais'
                END as category,
                COUNT(*) as count
            FROM MESURES_QOS
            WHERE MOS IS NOT NULL
            GROUP BY 
                CASE 
                    WHEN MOS >= 4.5 THEN 'Excellent'
                    WHEN MOS >= 4.0 THEN 'Bon'
                    WHEN MOS >= 3.0 THEN 'Moyen'
                    WHEN MOS >= 2.0 THEN 'Médiocre'
                    ELSE 'Mauvais'
                END
            ORDER BY category
        """;

        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            XYChart.Series<String, Number> series = new XYChart.Series<>();

            while (rs.next()) {
                String category = rs.getString("category");
                int count = rs.getInt("count");
                series.getData().add(new XYChart.Data<>(category, count));
            }

            Platform.runLater(() -> {
                mosDistributionChart.getData().clear();
                mosDistributionChart.getData().add(series);
            });

        } catch (SQLException e) {
            System.err.println("Erreur distribution MOS: " + e.getMessage());
        }
    }

    @FXML
    private void trainModel() {
        if (isTraining) {
            updateStatus("Entraînement déjà en cours...", "info");
            return;
        }

        isTraining = true;
        updateStatus("Démarrage de l'entraînement...", "info");
        trainingResultsArea.clear();
        trainBtn.setDisable(true);

        Thread trainThread = new Thread(() -> {
            PrintStream originalOut = System.out;

            try {
                System.setOut(customOut);

                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(0.2);
                    trainingProgressLabel.setText("20% - Chargement données");
                });

                Instances data = DataPreparation.loadDataFromDatabase();

                if (data.numInstances() == 0) {
                    Platform.runLater(() -> {
                        updateStatus("Erreur: Aucune donnée chargée", "error");
                        trainingProgressBar.setProgress(0);
                        trainingProgressLabel.setText("Échec");
                    });
                    return;
                }

                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(0.5);
                    trainingProgressLabel.setText("50% - Entraînement modèle");
                });

                PredictionService.trainModel();

                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(1.0);
                    trainingProgressLabel.setText("100% - Terminé ✓");
                    updateStatus("Entraînement terminé avec succès!", "success");
                    trainBtn.setDisable(false);
                });

                loadInitialData();

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Erreur: " + e.getMessage(), "error");
                    trainingProgressBar.setProgress(0);
                    trainingProgressLabel.setText("Erreur");
                    trainBtn.setDisable(false);
                });
                e.printStackTrace(customOut);
            } finally {
                System.setOut(originalOut);
                isTraining = false;
            }
        });

        trainThread.setDaemon(true);
        trainThread.start();
    }

    @FXML
    private void predictManual() {
        try {
            double latence = getDoubleFromField(latenceField, 50.0);
            double jitter = getDoubleFromField(jitterField, 10.0);
            double perte = getDoubleFromField(perteField, 1.0);
            double bandePassante = getDoubleFromField(bandePassanteField, 50.0);
            double signal = getDoubleFromField(signalField, 80.0);

            double mos = PredictionService.predictMOS(latence, jitter, perte, bandePassante, signal);
            String category = getQoECategory(mos);

            Platform.runLater(() -> {
                predictedMOSLabel.setText(String.format("%.2f", mos));
                qoeCategoryLabel.setText(category);
                qualityIndicator.setProgress((mos - 1) / 4);

                Map<String, String> prediction = new HashMap<>();
                prediction.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                prediction.put("latence", String.format("%.1f ms", latence));
                prediction.put("jitter", String.format("%.1f ms", jitter));
                prediction.put("perte", String.format("%.2f%%", perte));
                prediction.put("mos", String.format("%.2f", mos));
                prediction.put("category", category);

                predictionHistory.add(0, prediction);
                if (predictionHistory.size() > 20) {
                    predictionHistory.remove(predictionHistory.size() - 1);
                }
            });

            updateStatus("Prédiction: MOS = " + String.format("%.2f", mos), "success");

        } catch (Exception e) {
            updateStatus("Erreur: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private double getDoubleFromField(TextField field, double defaultValue) {
        try {
            return Double.parseDouble(field.getText());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @FXML
    private void runQuickTest() {
        runTestInThread(() -> PredictionService.quickTest(), "Test rapide");
    }

    @FXML
    private void evaluateModel() {
        runTestInThread(() -> PredictionService.evaluatePredictions(), "Évaluation");
    }

    @FXML
    private void predictMissingMOS() {
        runTestInThread(() -> PredictionService.predictMissingMOS(), "Prédiction MOS manquants");
    }

    @FXML
    private void runAllTests() {
        if (isTraining) {
            updateStatus("Operation deja en cours...", "info");
            return;
        }

        runTestInThread(() -> {
            testOutputArea.appendText("========== SUITE COMPLETE DE TESTS ==========\n\n");

            PredictionService.trainModel();
            testOutputArea.appendText("\n");

            PredictionService.quickTest();
            testOutputArea.appendText("\n");

            PredictionService.evaluatePredictions();

            testOutputArea.appendText("\n[OK] TOUS LES TESTS TERMINES\n");
            testOutputArea.appendText("=============================================\n");

            loadInitialData();
        }, "Suite complete");
    }

    private void runTestInThread(Runnable task, String testName) {
        Thread thread = new Thread(() -> {
            PrintStream originalOut = System.out;
            try {
                System.setOut(customOut);
                testOutputArea.clear();
                updateStatus("Exécution: " + testName + "...", "info");
                task.run();
                updateStatus(testName + " terminé", "success");
            } catch (Exception e) {
                updateStatus("Erreur: " + e.getMessage(), "error");
                e.printStackTrace(customOut);
            } finally {
                System.setOut(originalOut);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML private void refreshData() { loadInitialData(); updateStatus("Rafraîchi", "success"); }
    @FXML private void clearTestOutput() { testOutputArea.clear(); }
    @FXML private void saveTestLog() { updateStatus("Fonction à implémenter", "info"); }
    @FXML private void exportResults() { updateStatus("Fonction à implémenter", "info"); }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/qoe.fxml"));
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 650));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatus(String message, String type) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            switch (type.toLowerCase()) {
                case "success" -> statusLabel.setStyle("-fx-text-fill: #27ae60;");
                case "error" -> statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                case "info" -> statusLabel.setStyle("-fx-text-fill: #3498db;");
            }
        });
    }

    private void updateFooterInfo() {
        Platform.runLater(() -> {
            modelInfoLabel.setText("Modèle: " + (PredictionService.isModelTrained() ? "Entraîné ✓" : "Non entraîné"));
            dataInfoLabel.setText("Données: " + datasetSizeLabel.getText() + " instances");
            lastUpdateLabel.setText("MAJ: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
        });
    }
}