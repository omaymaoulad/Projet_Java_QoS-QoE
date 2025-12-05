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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MLDashboardController {

    // ============ COMPOSANTS FXML ============

    // Vue d'ensemble
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
    @FXML private TableView<Map<String, String>> correlationMatrixTable;

    // Footer
    @FXML private Label modelInfoLabel, dataInfoLabel, lastUpdateLabel;

    // ============ VARIABLES ============
    private ObservableList<Map<String, String>> predictionHistory = FXCollections.observableArrayList();
    private ByteArrayOutputStream consoleOutput;
    private PrintStream originalOut;
    private PrintStream customOut;

    @FXML
    public void initialize() {
        setupConsoleRedirect();
        setupUIComponents();
        loadInitialData();
        bindSlidersAndFields();
    }

    private void setupConsoleRedirect() {
        consoleOutput = new ByteArrayOutputStream();
        originalOut = System.out;

        customOut = new PrintStream(consoleOutput) {
            @Override
            public void print(String s) {
                super.print(s);
                Platform.runLater(() -> {
                    testOutputArea.appendText(s);
                    testOutputArea.positionCaret(testOutputArea.getLength());
                });
            }

            @Override
            public void println(String s) {
                super.println(s);
                Platform.runLater(() -> {
                    testOutputArea.appendText(s + "\n");
                    testOutputArea.positionCaret(testOutputArea.getLength());
                });
            }
        };
    }

    private void setupUIComponents() {
        // Configurer les ComboBox
        modelTypeCombo.getItems().addAll("Random Forest", "Régression Linéaire", "SVM");
        modelTypeCombo.setValue("Random Forest");

        validationCombo.getItems().addAll("Validation croisée (10-fold)", "Split simple");
        validationCombo.setValue("Validation croisée (10-fold)");

        // Configurer les tables
        setupTables();

        // Initialiser les charts
        initializeCharts();

        // Configurer les sliders
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
        // Table des prédictions récentes
        recentPredictionsTable.setPlaceholder(new Label("Aucune donnée disponible"));
        setupPredictionTableColumns();

        // Table d'historique des prédictions
        predictionHistoryTable.setItems(predictionHistory);
        predictionHistoryTable.setPlaceholder(new Label("Aucune prédiction effectuée"));
        setupHistoryTableColumns();
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
        // Performance chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Performance");
        performanceChart.getData().add(series);
        performanceChart.setLegendVisible(false);

        // Feature importance chart
        featureImportanceChart.setLegendVisible(false);

        // MOS distribution chart
        mosDistributionChart.setLegendVisible(false);

        // Scatter charts
        mosVsLatenceChart.setLegendVisible(false);
        mosVsPerteChart.setLegendVisible(false);

        // Initialiser les séries pour scatter charts
        XYChart.Series<Number, Number> latenceSeries = new XYChart.Series<>();
        latenceSeries.setName("MOS vs Latence");
        mosVsLatenceChart.getData().add(latenceSeries);

        XYChart.Series<Number, Number> perteSeries = new XYChart.Series<>();
        perteSeries.setName("MOS vs Perte");
        mosVsPerteChart.getData().add(perteSeries);
    }

    private void bindSlidersAndFields() {
        // Lier les sliders aux champs texte
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
                // Ignorer les entrées invalides
            }
        });
    }

    private void loadInitialData() {
        updateStatus("Chargement des données...", "info");

        Thread loadThread = new Thread(() -> {
            try {
                // Charger les statistiques de base
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
            // Taille du dataset
            String countSQL = "SELECT COUNT(*) FROM MESURES_QOS WHERE MOS IS NOT NULL";
            try (PreparedStatement ps = conn.prepareStatement(countSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    Platform.runLater(() -> {
                        datasetSizeLabel.setText(String.valueOf(count));
                    });
                }
            }

            // Nombre de prédictions
            String predSQL = "SELECT COUNT(*) FROM MESURES_QOS WHERE MOS IS NOT NULL AND MOS > 0";
            try (PreparedStatement ps = conn.prepareStatement(predSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    Platform.runLater(() -> {
                        predictionsCountLabel.setText(String.valueOf(count));
                    });
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
                ID_MESURE,
                LATENCE,
                JITTER,
                PERTE,
                BANDE_PASSANTE,
                SIGNAL_SCORE,
                MOS,
                TO_CHAR(SYSDATE, 'DD/MM HH24:MI') as PRED_DATE
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

                // Catégorie QoE
                double mos = rs.getDouble("MOS");
                String category = getQoECategory(mos);
                row.put("category", category);

                data.add(row);
            }

            Platform.runLater(() -> {
                recentPredictionsTable.getItems().setAll(data);
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement prédictions: " + e.getMessage());
        }
    }

    private void loadScatterData() {
        String sql = """
            SELECT LATENCE, PERTE, MOS 
            FROM MESURES_QOS 
            WHERE MOS IS NOT NULL 
            AND LATENCE IS NOT NULL 
            AND PERTE IS NOT NULL
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
            System.err.println("Erreur chargement données scatter: " + e.getMessage());
        }
    }

    private void setupPredictionTableColumns() {
        // Créer les colonnes pour la table des prédictions récentes
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

    private String getQoECategory(double mos) {
        if (mos >= 4.5) return "Excellent";
        else if (mos >= 4.0) return "Bon";
        else if (mos >= 3.0) return "Moyen";
        else if (mos >= 2.0) return "Médiocre";
        else return "Mauvais";
    }

    private void loadPerformanceMetrics() {
        // Ici, vous pourriez charger les métriques depuis un fichier de log
        // ou depuis la base de données

        Platform.runLater(() -> {
            // Valeurs par défaut / exemple
            modelTypeLabel.setText("Random Forest");
            accuracyLabel.setText("0.85");
            maeLabel.setText("0.42");
            rmseLabel.setText("0.65");
            r2Label.setText("0.78");
            correlationLabel.setText("0.88");

            // Mettre à jour le chart de performance
            performanceChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("MAE", 0.42));
            series.getData().add(new XYChart.Data<>("RMSE", 0.65));
            series.getData().add(new XYChart.Data<>("R²", 0.78));
            series.getData().add(new XYChart.Data<>("Corrélation", 0.88));
            performanceChart.getData().add(series);
        });
    }

    private void loadMOSDistribution() {
        String sql = """
            SELECT 
                CASE 
                    WHEN MOS >= 4.5 THEN 'Excellent (4.5-5)'
                    WHEN MOS >= 4.0 THEN 'Bon (4-4.5)'
                    WHEN MOS >= 3.0 THEN 'Moyen (3-4)'
                    WHEN MOS >= 2.0 THEN 'Médiocre (2-3)'
                    ELSE 'Mauvais (1-2)'
                END as category,
                COUNT(*) as count
            FROM MESURES_QOS
            WHERE MOS IS NOT NULL
            GROUP BY 
                CASE 
                    WHEN MOS >= 4.5 THEN 'Excellent (4.5-5)'
                    WHEN MOS >= 4.0 THEN 'Bon (4-4.5)'
                    WHEN MOS >= 3.0 THEN 'Moyen (3-4)'
                    WHEN MOS >= 2.0 THEN 'Médiocre (2-3)'
                    ELSE 'Mauvais (1-2)'
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
            System.err.println("Erreur chargement distribution MOS: " + e.getMessage());
        }
    }

    @FXML
    private void trainModel() {
        updateStatus("Démarrage de l'entraînement...", "info");
        trainingResultsArea.clear();

        Thread trainThread = new Thread(() -> {
            try {
                System.setOut(customOut);

                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(0.1);
                    trainingProgressLabel.setText("10% - Chargement données");
                });

                // Charger les données
                Instances data = DataPreparation.loadDataFromDatabase();

                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(0.3);
                    trainingProgressLabel.setText("30% - Préparation données");
                });

                if (data.numInstances() == 0) {
                    Platform.runLater(() -> {
                        updateStatus("Erreur: Aucune donnée chargée", "error");
                        trainingProgressBar.setProgress(0);
                        trainingProgressLabel.setText("Échec");
                    });
                    return;
                }

                // Diviser les données
                Instances[] split = DataPreparation.splitData(data, trainRatioSlider.getValue());

                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(0.5);
                    trainingProgressLabel.setText("50% - Entraînement modèle");
                });

                // Entraîner le modèle
                PredictionService.trainModel();

                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(0.8);
                    trainingProgressLabel.setText("80% - Évaluation");
                });

                // Évaluer
                PredictionService.evaluatePredictions();

                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(1.0);
                    trainingProgressLabel.setText("100% - Terminé");
                    updateStatus("Entraînement terminé avec succès!", "success");
                    loadInitialData(); // Rafraîchir les données
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Erreur pendant l'entraînement: " + e.getMessage(), "error");
                    trainingProgressBar.setProgress(0);
                    trainingProgressLabel.setText("Erreur");
                });
                e.printStackTrace(customOut);
            } finally {
                System.setOut(originalOut);
            }
        });

        trainThread.setDaemon(true);
        trainThread.start();
    }

    @FXML
    private void predictManual() {
        try {
            // Récupérer les valeurs des champs
            double latence = getDoubleFromField(latenceField, 50.0);
            double jitter = getDoubleFromField(jitterField, 10.0);
            double perte = getDoubleFromField(perteField, 1.0);
            double bandePassante = getDoubleFromField(bandePassanteField, 50.0);
            double signal = getDoubleFromField(signalField, 80.0);

            // Prédire le MOS
            double mos = PredictionService.predictMOS(latence, jitter, perte, bandePassante, signal);
            String category = getQoECategory(mos);

            // Mettre à jour l'interface
            Platform.runLater(() -> {
                predictedMOSLabel.setText(String.format("%.2f", mos));
                qoeCategoryLabel.setText(category);
                qualityIndicator.setProgress((mos - 1) / 4); // Normaliser entre 0 et 1

                // Ajouter à l'historique
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

            updateStatus("Prédiction terminée: MOS = " + String.format("%.2f", mos), "success");

        } catch (Exception e) {
            updateStatus("Erreur de prédiction: " + e.getMessage(), "error");
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
        runInThread(() -> {
            System.setOut(customOut);
            testOutputArea.clear();
            updateStatus("Exécution du test rapide...", "info");
            PredictionService.quickTest();
            updateStatus("Test rapide terminé", "success");
            System.setOut(originalOut);
        });
    }

    @FXML
    private void evaluateModel() {
        runInThread(() -> {
            System.setOut(customOut);
            testOutputArea.clear();
            updateStatus("Évaluation du modèle...", "info");
            PredictionService.evaluatePredictions();
            updateStatus("Évaluation terminée", "success");
            System.setOut(originalOut);
        });
    }

    @FXML
    private void predictMissingMOS() {
        runInThread(() -> {
            System.setOut(customOut);
            testOutputArea.clear();
            updateStatus("Prédiction des MOS manquants...", "info");
            PredictionService.predictMissingMOS();
            updateStatus("Prédictions terminées", "success");
            loadInitialData(); // Rafraîchir
            System.setOut(originalOut);
        });
    }

    @FXML
    private void runAllTests() {
        runInThread(() -> {
            System.setOut(customOut);
            testOutputArea.clear();
            updateStatus("Exécution de tous les tests...", "info");

            testOutputArea.appendText("╔════════════════════════════════════════╗\n");
            testOutputArea.appendText("║   DÉMARRAGE DE TOUS LES TESTS ML      ║\n");
            testOutputArea.appendText("╚════════════════════════════════════════╝\n\n");

            PredictionService.trainModel();
            PredictionService.quickTest();
            PredictionService.evaluatePredictions();
            PredictionService.predictMissingMOS();

            testOutputArea.appendText("\n╔════════════════════════════════════════╗\n");
            testOutputArea.appendText("║        PROCESSUS TERMINÉ ! ✅        ║\n");
            testOutputArea.appendText("╚════════════════════════════════════════╝\n");

            updateStatus("Tous les tests terminés", "success");
            loadInitialData();
            System.setOut(originalOut);
        });
    }

    @FXML
    private void refreshData() {
        loadInitialData();
        updateStatus("Données rafraîchies", "success");
    }

    @FXML
    private void clearTestOutput() {
        testOutputArea.clear();
        updateStatus("Sortie effacée", "info");
    }

    @FXML
    private void saveTestLog() {
        // Implémenter la sauvegarde du log
        updateStatus("Fonctionnalité de sauvegarde à implémenter", "info");
    }

    @FXML
    private void exportResults() {
        // Implémenter l'export des résultats
        updateStatus("Fonctionnalité d'export à implémenter", "info");
    }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/qoe.fxml"));
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 650));
            stage.setTitle("QOS/QOE System - Main Application");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void testConnection() {
        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection()) {
            String testSQL = "SELECT COUNT(*) FROM MESURES_QOS";
            try (PreparedStatement ps = conn.prepareStatement(testSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    updateStatus("✅ Table MESURES_QOS trouvée: " + rs.getInt(1) + " lignes", "success");
                }
            }
        } catch (SQLException e) {
            updateStatus("❌ Erreur: " + e.getMessage(), "error");
        }
    }

    // ============ MÉTHODES UTILITAIRES ============

    private void runInThread(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateStatus(String message, String type) {
        Platform.runLater(() -> {
            statusLabel.setText(message);

            switch (type.toLowerCase()) {
                case "success":
                    statusLabel.setStyle("-fx-text-fill: #27ae60;");
                    break;
                case "error":
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    break;
                case "info":
                    statusLabel.setStyle("-fx-text-fill: #3498db;");
                    break;
                default:
                    statusLabel.setStyle("-fx-text-fill: #2c3e50;");
            }
        });
    }

    private void updateFooterInfo() {
        Platform.runLater(() -> {
            modelInfoLabel.setText("Modèle: " + (PredictionService.isModelTrained() ? "Entraîné" : "Non entraîné"));
            dataInfoLabel.setText("Données: " + datasetSizeLabel.getText() + " instances");
            lastUpdateLabel.setText("Dernière mise à jour: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
        });
    }
}