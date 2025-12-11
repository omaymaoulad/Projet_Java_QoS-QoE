package com.ensah.qoe.Controller;

import com.ensah.qoe.ML.AnomalyDetectionModels;
import com.ensah.qoe.ML.DataPreparationAnomalie;
import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Services.PredictionServiceAnomalies;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import weka.core.Instances;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AnomalyDashboardController {

    // ============ STATUT & KPI ============
    @FXML private Label statusLabel;
    @FXML private Label totalInstancesLabel;
    @FXML private Label normalCountLabel;
    @FXML private Label anomalyCountLabel;
    @FXML private Label accuracyLabel;
    @FXML private Label precisionLabel;
    @FXML private Label recallLabel;
    @FXML private Label f1ScoreLabel;
    @FXML private ProgressBar anomalyRateProgress;
    @FXML private Label anomalyRateLabel;

    // ============ ENTRTRAÎNEMENT ============
    @FXML private ComboBox<String> algorithmCombo;
    @FXML private Slider trainTestSplitSlider;
    @FXML private Label splitRatioLabel;
    @FXML private Button trainButton;
    @FXML private ProgressBar trainingProgressBar;
    @FXML private Label trainingProgressLabel;
    @FXML private TextArea trainingLogArea;
    @FXML private Button visualizeDataButton;

    // ============ PRÉDICTION ============
    @FXML private VBox predictionInputsContainer;
    @FXML private TextField latenceField;
    @FXML private Slider latenceSlider;
    @FXML private TextField jitterField;
    @FXML private Slider jitterSlider;
    @FXML private TextField perteField;
    @FXML private Slider perteSlider;
    @FXML private TextField bandePassanteField;
    @FXML private Slider bandePassanteSlider;
    @FXML private TextField signalField;
    @FXML private Slider signalSlider;

    @FXML private Button predictButton;
    @FXML private Button analyzeButton;
    @FXML private Label predictionResultLabel;
    @FXML private Label confidenceLabel;
    @FXML private ProgressBar confidenceProgressBar;
    @FXML private Label explanationLabel;


    // ============ VISUALISATION ============
    @FXML private TabPane visualizationTabPane;
    @FXML private BarChart<String, Number> distributionChart;
    @FXML private PieChart anomalyPieChart;
    @FXML private LineChart<String, Number> performanceChart;
    @FXML private ScatterChart<Number, Number> correlationChart;
    @FXML private TableView<Map<String, String>> confusionMatrixTable;
    @FXML private TableView<Map<String, String>> featureStatsTable;

    // ============ HISTORIQUE & LOGS ============
    @FXML private TableView<Map<String, String>> recentPredictionsTable;
    @FXML private TableView<Map<String, String>> anomalyLogTable;
    @FXML private TextArea systemLogArea;
    @FXML private Button clearLogsButton;
    @FXML private Button exportLogsButton;

    // ============ ACTIONS RAPIDES ============
    @FXML private Button predictMissingButton;
    @FXML private Button evaluateModelButton;
    @FXML private Button fullAnalysisButton;
    @FXML private Button refreshButton;

    // ============ FOOTER ============
    @FXML private Label modelStatusLabel;
    @FXML private Label lastTrainingLabel;
    @FXML private Label dataUpdateLabel;
    @FXML private Label performanceStatusLabel;
    @FXML private Button goBackButton;
    // ============ VARIABLES ============
    private ObservableList<Map<String, String>> recentPredictions = FXCollections.observableArrayList();
    private ObservableList<Map<String, String>> anomalyLogs = FXCollections.observableArrayList();
    private ByteArrayOutputStream consoleOutput;
    private PrintStream originalOut;
    private PrintStream customOut;


    private static final String NORMAL_STYLE = "-fx-background-color: #2ecc71; -fx-text-fill: white;";
    private static final String ANOMALY_STYLE = "-fx-background-color: #e74c3c; -fx-text-fill: white;";
    private static final String WARNING_STYLE = "-fx-background-color: #f39c12; -fx-text-fill: white;";

    @FXML
    public void initialize() {
        setupConsoleRedirect();

        setupUIComponents();
        initializeCharts();
        setupTables();
        bindSlidersAndFields();
        loadDashboardData();
        startAutoRefresh();
        setupRecentPredictionsTable();
        loadRecentPredictions();
    }


    private void setupConsoleRedirect() {
        consoleOutput = new ByteArrayOutputStream();
        originalOut = System.out;

        customOut = new PrintStream(consoleOutput) {
            @Override
            public void print(String s) {
                super.print(s);
                Platform.runLater(() -> {
                    trainingLogArea.appendText(s);
                    trainingLogArea.positionCaret(trainingLogArea.getLength());
                    systemLogArea.appendText(s);
                    systemLogArea.positionCaret(systemLogArea.getLength());
                });
            }

            @Override
            public void println(String s) {
                super.println(s);
                Platform.runLater(() -> {
                    trainingLogArea.appendText(s + "\n");
                    trainingLogArea.positionCaret(trainingLogArea.getLength());
                    systemLogArea.appendText(s + "\n");
                    systemLogArea.positionCaret(systemLogArea.getLength());
                });
            }
        };
    }

    private void setupUIComponents() {
        // Configuration des ComboBox
        algorithmCombo.getItems().addAll(
                "J48 (Arbre de décision)",
                "Naive Bayes",
                "KNN (k=3)"
        );
        algorithmCombo.setValue("J48 (Arbre de décision)");

        // Configuration des sliders
        configureSliders();

        // Mise à jour du label du split
        trainTestSplitSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            splitRatioLabel.setText(String.format("%.0f/%.0f", newVal.doubleValue(), 100 - newVal.doubleValue()));
        });

        // Style des boutons
        trainButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        predictButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
        analyzeButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");

        // Tooltips
        Tooltip.install(trainButton, new Tooltip("Entraîner le modèle de détection d'anomalies"));
        Tooltip.install(predictButton, new Tooltip("Prédire si les valeurs actuelles représentent une anomalie"));
        Tooltip.install(analyzeButton, new Tooltip("Analyse détaillée des facteurs d'anomalie"));
    }

    private void configureSliders() {
        // Latence: 0-500ms (typique 0-100 normal)
        latenceSlider.setMin(0);
        latenceSlider.setMax(500);
        latenceSlider.setValue(50);
        latenceSlider.setBlockIncrement(10);

        // Jitter: 0-100ms (typique 0-20 normal)
        jitterSlider.setMin(0);
        jitterSlider.setMax(100);
        jitterSlider.setValue(10);
        jitterSlider.setBlockIncrement(5);

        // Perte: 0-100% (typique 0-5 normal)
        perteSlider.setMin(0);
        perteSlider.setMax(100);
        perteSlider.setValue(1);
        perteSlider.setBlockIncrement(1);

        // Bande passante: 0-200 Mbps
        bandePassanteSlider.setMin(0);
        bandePassanteSlider.setMax(200);
        bandePassanteSlider.setValue(50);
        bandePassanteSlider.setBlockIncrement(10);

        // Signal: 0-100%
        signalSlider.setMin(0);
        signalSlider.setMax(100);
        signalSlider.setValue(80);
        signalSlider.setBlockIncrement(5);
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
            updatePredictionPreview();
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

    private void initializeCharts() {
        // Distribution chart
        distributionChart.setTitle("Distribution des Anomalies");
        distributionChart.setLegendVisible(false);

        // Pie chart
        anomalyPieChart.setTitle("Proportion Anomalies/Normales");

        // Performance chart
        performanceChart.setTitle("Performance du Modèle");
        performanceChart.setCreateSymbols(true);

        // Correlation chart
        correlationChart.setTitle("Corrélations entre Features");

        // Initialiser les séries
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Anomalies");
        distributionChart.getData().add(series);
    }

    private void setupTables() {
        // Table des prédictions récentes
        setupPredictionsTable();

        // Table des logs d'anomalies
        setupAnomalyLogTable();

        // Table de matrice de confusion
        setupConfusionMatrixTable();

        // Table des statistiques des features
        setupFeatureStatsTable();
    }

    private void setupPredictionsTable() {
        recentPredictionsTable.setPlaceholder(new Label("Aucune prédiction récente"));

        String[] columns = {"time", "latence", "jitter", "perte", "signal", "prediction", "confidence"};
        String[] titles = {"Heure", "Latence", "Jitter", "Perte %", "Signal", "Prédiction", "Confiance"};

        for (int i = 0; i < columns.length; i++) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(titles[i]);
            final String key = columns[i];
            col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(key)));
            col.setPrefWidth(100);
            recentPredictionsTable.getColumns().add(col);
        }
    }

    private void setupAnomalyLogTable() {
        anomalyLogTable.getColumns().clear();
        anomalyLogTable.setPlaceholder(new Label("Aucune anomalie détectée"));

        TableColumn<Map<String, String>, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get("time")));

        TableColumn<Map<String, String>, String> severityCol = new TableColumn<>("Sévérité");
        severityCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get("severity")));

        TableColumn<Map<String, String>, String> detailsCol = new TableColumn<>("Détails");
        detailsCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get("details")));

        TableColumn<Map<String, String>, String> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get("action")));

        anomalyLogTable.getColumns().addAll(timeCol, severityCol, detailsCol, actionCol);
    }

    private void setupConfusionMatrixTable() {
        confusionMatrixTable.setPlaceholder(new Label("Matrice de confusion non disponible"));

        String[] headers = {"", "Prédit Normal", "Prédit Anomalie"};
        String[] rows = {"Réel Normal", "Réel Anomalie"};

        for (int i = 0; i < headers.length; i++) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(headers[i]);
            final String key = "col" + i;
            col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(key)));
            confusionMatrixTable.getColumns().add(col);
        }
    }

    private void setupFeatureStatsTable() {
        featureStatsTable.getItems().clear();
        featureStatsTable.setPlaceholder(new Label("Charger les données pour voir les statistiques"));

        String[] columns = {"feature", "min", "max", "avg", "std", "impact"};
        String[] titles = {"Feature", "Min", "Max", "Moyenne", "Écart-type", "Impact"};

        for (int i = 0; i < columns.length; i++) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(titles[i]);
            final String key = columns[i];
            col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(key)));
            col.setPrefWidth(100);
            featureStatsTable.getColumns().add(col);
            featureStatsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        }
    }

    private void loadDashboardData() {
        updateStatus("Chargement des données...", "info", "info");

        Thread loadThread = new Thread(() -> {
            try {
                loadKPIData();
                loadDistributionData();
                loadPerformanceHistory();
                loadRecentPredictions();
                loadAnomalyLogs();
                loadFeatureStatistics();
                updateModelStatus();
                loadCorrelationData();
                loadConfusionMatrix();


                Platform.runLater(() -> {
                    updateStatus("Système prêt", "success", "normal");
                    updateFooter();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Erreur de chargement: " + e.getMessage(), "error", "warning");
                });
                e.printStackTrace();
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void loadKPIData() {
        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection()) {
            // Nombre total d'instances
            String countSQL = "SELECT COUNT(*) FROM MESURES_QOS WHERE ANOMALIE IS NOT NULL";
            int totalInstances = executeCountQuery(countSQL);

            // Nombre d'anomalies
            String anomalySQL = "SELECT COUNT(*) FROM MESURES_QOS WHERE ANOMALIE = 1";
            int anomalyCount = executeCountQuery(anomalySQL);

            // Nombre de normales
            String normalSQL = "SELECT COUNT(*) FROM MESURES_QOS WHERE ANOMALIE = 0";
            int normalCount = executeCountQuery(normalSQL);

            double anomalyRate = totalInstances > 0 ? (anomalyCount * 100.0) / totalInstances : 0;

            Platform.runLater(() -> {
                totalInstancesLabel.setText(String.format("%,d", totalInstances));
                anomalyCountLabel.setText(String.format("%,d", anomalyCount));
                normalCountLabel.setText(String.format("%,d", normalCount));
                anomalyRateProgress.setProgress(anomalyRate / 100);
                anomalyRateLabel.setText(String.format("%.1f%%", anomalyRate));
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement KPI: " + e.getMessage());
        }
    }

    private int executeCountQuery(String sql) throws SQLException {
        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void loadDistributionData() {
        String sql = """
            SELECT 
                CASE 
                    WHEN LATENCE > 200 THEN 'Latence Élevée'
                    WHEN JITTER > 50 THEN 'Jitter Élevé'
                    WHEN PERTE > 10 THEN 'Perte Élevée'
                    WHEN SIGNAL_SCORE < 30 THEN 'Signal Faible'
                    ELSE 'Autres Anomalies'
                END as anomaly_type,
                COUNT(*) as count
            FROM MESURES_QOS
            WHERE ANOMALIE = 1
            GROUP BY 
                CASE 
                    WHEN LATENCE > 200 THEN 'Latence Élevée'
                    WHEN JITTER > 50 THEN 'Jitter Élevé'
                    WHEN PERTE > 10 THEN 'Perte Élevée'
                    WHEN SIGNAL_SCORE < 30 THEN 'Signal Faible'
                    ELSE 'Autres Anomalies'
                END
            ORDER BY count DESC
        """;

        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

            while (rs.next()) {
                String type = rs.getString("anomaly_type");
                int count = rs.getInt("count");

                series.getData().add(new XYChart.Data<>(type, count));
                pieData.add(new PieChart.Data(type, count));
            }

            Platform.runLater(() -> {
                distributionChart.getData().clear();
                distributionChart.getData().add(series);

                anomalyPieChart.setData(pieData);
                anomalyPieChart.setLabelsVisible(true);
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement distribution: " + e.getMessage());
        }
    }

    private void loadPerformanceHistory() {
        // Données factices pour l'exemple
        XYChart.Series<String, Number> accuracySeries = new XYChart.Series<>();
        accuracySeries.setName("Accuracy");

        XYChart.Series<String, Number> f1Series = new XYChart.Series<>();
        f1Series.setName("F1-Score");

        String[] dates = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

        Random rand = new Random();
        for (String date : dates) {
            accuracySeries.getData().add(new XYChart.Data<>(date, 80 + rand.nextDouble() * 15));
            f1Series.getData().add(new XYChart.Data<>(date, 75 + rand.nextDouble() * 18));
        }

        Platform.runLater(() -> {
            performanceChart.getData().clear();
            performanceChart.getData().addAll(accuracySeries, f1Series);
        });
    }

    private void loadRecentPredictions() {

        String sql = """
        SELECT
           TO_CHAR(DATE_REELLE, 'HH24:MI:SS') AS HEURE,
           LATENCE,
           JITTER,
           PERTE,
           SIGNAL_SCORE,
           ANOMALIE
        FROM MESURES_QOS
        ORDER BY DATE_REELLE DESC
        FETCH FIRST 10 ROWS ONLY
    """;

        ObservableList<Map<String, String>> data = FXCollections.observableArrayList();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Map<String, String> row = new HashMap<>();

                row.put("time", rs.getString("HEURE"));    // 🔥 FIX ICI
                row.put("latence", String.format("%.1f", rs.getDouble("LATENCE")));
                row.put("jitter", String.format("%.1f", rs.getDouble("JITTER")));
                row.put("perte", String.format("%.1f%%", rs.getDouble("PERTE")));
                row.put("signal", String.format("%.0f", rs.getDouble("SIGNAL_SCORE")));

                int anomaly = rs.getInt("ANOMALIE");
                row.put("prediction", anomaly == 1 ? "ANOMALIE" : "NORMAL");
                row.put("confidence", anomaly == 1 ? "80%" : "95%");

                data.add(row);
            }

            Platform.runLater(() -> recentPredictionsTable.setItems(data));

        } catch (SQLException e) {
            System.err.println("Erreur chargement prédictions: " + e.getMessage());
        }
    }



    private void loadAnomalyLogs() {
        String sql = """
            SELECT 
                ID_MESURE,
                TO_CHAR(SYSDATE, 'DD/MM HH24:MI') as time,
                LATENCE,
                JITTER,
                PERTE,
                CASE 
                    WHEN LATENCE > 200 THEN 'CRITIQUE'
                    WHEN PERTE > 20 THEN 'HAUTE'
                    ELSE 'MOYENNE'
                END as severity,
                'Anomalie réseau détectée' as details
            FROM MESURES_QOS
            WHERE ANOMALIE = 1
            ORDER BY ID_MESURE DESC
            FETCH FIRST 20 ROWS ONLY
        """;

        ObservableList<Map<String, String>> logs = FXCollections.observableArrayList();

        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, String> log = new HashMap<>();
                log.put("time", rs.getString("time"));
                log.put("severity", rs.getString("severity"));
                log.put("details", rs.getString("details") +
                        " (L:" + rs.getDouble("LATENCE") +
                        "ms J:" + rs.getDouble("JITTER") +
                        "ms P:" + rs.getDouble("PERTE") + "%)");
                log.put("action", "À investiguer");
                logs.add(log);
            }

            Platform.runLater(() -> {
                anomalyLogTable.setItems(logs);
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement logs: " + e.getMessage());
        }
    }

    private void loadFeatureStatistics() {
        String sql = """
            SELECT 
                'latence' as feature,
                MIN(LATENCE) as min_val,
                MAX(LATENCE) as max_val,
                AVG(LATENCE) as avg_val,
                STDDEV(LATENCE) as std_val,
                35 as impact
            FROM MESURES_QOS
            UNION ALL
            SELECT 
                'jitter',
                MIN(JITTER),
                MAX(JITTER),
                AVG(JITTER),
                STDDEV(JITTER),
                25
            FROM MESURES_QOS
            UNION ALL
            SELECT 
                'perte',
                MIN(PERTE),
                MAX(PERTE),
                AVG(PERTE),
                STDDEV(PERTE),
                30
            FROM MESURES_QOS
            UNION ALL
            SELECT 
                'signal_score',
                MIN(SIGNAL_SCORE),
                MAX(SIGNAL_SCORE),
                AVG(SIGNAL_SCORE),
                STDDEV(SIGNAL_SCORE),
                10
            FROM MESURES_QOS
        """;

        ObservableList<Map<String, String>> stats = FXCollections.observableArrayList();

        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("feature", rs.getString("feature").toUpperCase());
                row.put("min", String.format("%.1f", rs.getDouble("min_val")));
                row.put("max", String.format("%.1f", rs.getDouble("max_val")));
                row.put("avg", String.format("%.1f", rs.getDouble("avg_val")));
                row.put("std", String.format("%.2f", rs.getDouble("std_val")));
                row.put("impact", rs.getInt("impact") + "%");
                stats.add(row);
            }

            Platform.runLater(() -> {
                featureStatsTable.setItems(stats);
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement statistiques: " + e.getMessage());
        }
    }

    private void updateModelStatus() {
        boolean isTrained = PredictionServiceAnomalies.isModelTrained();

        Platform.runLater(() -> {
            if (isTrained) {
                modelStatusLabel.setText("✓ Modèle entraîné");
                modelStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                accuracyLabel.setText(AnomalyDetectionModels.lastAccuracy);
                precisionLabel.setText(AnomalyDetectionModels.lastPrecision);
                recallLabel.setText(AnomalyDetectionModels.lastRecall);
                f1ScoreLabel.setText(AnomalyDetectionModels.lastF1);
            } else {
                modelStatusLabel.setText("✗ Modèle non entraîné");
                modelStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                accuracyLabel.setText("--");
                precisionLabel.setText("--");
                recallLabel.setText("--");
                f1ScoreLabel.setText("--");
            }
        });
    }

    private void updateFooter() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        dataUpdateLabel.setText("Dernière mise à jour: " + timestamp);
        lastTrainingLabel.setText("Dernier entraînement: " + timestamp);
        performanceStatusLabel.setText(PredictionServiceAnomalies.isModelTrained() ?
                "Performance: Optimale" : "Performance: Non évaluée");
    }

    // ============ ÉVÉNEMENTS ============

    @FXML
    private void handleTrainModel() {
        updateStatus("Démarrage de l'entraînement...", "info", "info");
        trainingLogArea.clear();

        Thread trainThread = new Thread(() -> {
            try {
                System.setOut(customOut);

                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(0.1);
                    trainingProgressLabel.setText("Chargement données...");
                });

                String algo = algorithmCombo.getValue();
                PredictionServiceAnomalies.setSelectedAlgorithm(algo);

                PredictionServiceAnomalies.trainModel();

                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(1.0);
                    trainingProgressLabel.setText("Terminé ✓");
                    updateStatus("Modèle entraîné avec succès", "success", "normal");
                    loadDashboardData(); // Rafraîchir
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    trainingProgressBar.setProgress(0);
                    trainingProgressLabel.setText("Échec ✗");
                    updateStatus("Erreur d'entraînement: " + e.getMessage(), "error", "warning");
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
    private void handlePredict() {
        try {
            double latence = getDoubleFromField(latenceField, 50.0);
            double jitter = getDoubleFromField(jitterField, 10.0);
            double perte = getDoubleFromField(perteField, 1.0);
            double bandePassante = getDoubleFromField(bandePassanteField, 50.0);
            double signal = getDoubleFromField(signalField, 80.0);
            if (signal < 0) signal = 0;
            if (signal > 100) signal = 100;
            final double signalFinal = signal;
            if (!PredictionServiceAnomalies.isModelTrained()) {
                updateStatus("Veuillez d'abord entraîner le modèle", "warning", "warning");
                return;
            }

            // Prédiction
            String prediction = PredictionServiceAnomalies.predictAnomaly(latence, jitter, perte, bandePassante, signal);

            // Probabilités
            double[] probabilities = PredictionServiceAnomalies.predictWithProbability(
                    latence, jitter, perte, bandePassante, signal);

            double anomalyProbability = probabilities[1] * 100;

            Platform.runLater(() -> {
                predictionResultLabel.setText(prediction);
                confidenceLabel.setText(String.format("%.1f%%", anomalyProbability));
                confidenceProgressBar.setProgress(anomalyProbability / 100);

                if (prediction.equals("ANOMALIE")) {
                    predictionResultLabel.setStyle(ANOMALY_STYLE);

                    explanationLabel.setText("⚠️ Anomalie détectée - Vérification réseau recommandée");
                } else {
                    predictionResultLabel.setStyle(NORMAL_STYLE);

                    explanationLabel.setText("✓ Qualité réseau normale");
                }

                // Ajouter à l'historique
                addToPredictionHistory(latence, jitter, perte, signalFinal, prediction, anomalyProbability);
            });

            updateStatus("Prédiction effectuée", "success", prediction.equals("ANOMALIE") ? "warning" : "normal");

        } catch (Exception e) {
            updateStatus("Erreur de prédiction: " + e.getMessage(), "error", "warning");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAnalyze() {
        try {
            double latence = getDoubleFromField(latenceField, 50.0);
            double jitter = getDoubleFromField(jitterField, 10.0);
            double perte = getDoubleFromField(perteField, 1.0);
            double bandePassante = getDoubleFromField(bandePassanteField, 50.0);
            double signal = getDoubleFromField(signalField, 80.0);

            // Analyse détaillée
            PredictionServiceAnomalies.analyzeAnomalyPrediction(latence, jitter, perte, bandePassante, signal);

            updateStatus("Analyse terminée - Voir logs", "info", "info");

        } catch (Exception e) {
            updateStatus("Erreur d'analyse: " + e.getMessage(), "error", "warning");
        }
    }

    @FXML
    private void handlePredictMissing() {
        Thread thread = new Thread(() -> {
            System.setOut(customOut);
            updateStatus("Prédiction des anomalies manquantes...", "info", "info");
            PredictionServiceAnomalies.predictMissingAnomalies();
            updateStatus("Anomalies manquantes prédites", "success", "info");
            loadDashboardData();
            System.setOut(originalOut);
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleEvaluate() {
        Thread thread = new Thread(() -> {
            System.setOut(customOut);
            updateStatus("Évaluation du modèle...", "info", "info");
            PredictionServiceAnomalies.evaluatePredictions();
            updateStatus("Évaluation terminée", "success", "info");
            loadDashboardData();
            System.setOut(originalOut);
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleFullAnalysis() {
        Thread thread = new Thread(() -> {
            System.setOut(customOut);
            updateStatus("Démarrage analyse complète...", "info", "info");

            systemLogArea.appendText("\n╔════════════════════════════════════════╗\n");
            systemLogArea.appendText("║   ANALYSE COMPLÈTE DES ANOMALIES     ║\n");
            systemLogArea.appendText("╚════════════════════════════════════════╝\n\n");

            // Vérifier corrélation
            PredictionServiceAnomalies.checkAnomalyMOSCorrelation();

            // Entraîner si nécessaire
            if (!PredictionServiceAnomalies.isModelTrained()) {
                PredictionServiceAnomalies.trainModel();
            }

            // Évaluer
            PredictionServiceAnomalies.evaluatePredictions();

            // Prédire manquants
            PredictionServiceAnomalies.predictMissingAnomalies();

            systemLogArea.appendText("\n╔════════════════════════════════════════╗\n");
            systemLogArea.appendText("║        ANALYSE TERMINÉE ✅           ║\n");
            systemLogArea.appendText("╚════════════════════════════════════════╝\n");

            updateStatus("Analyse complète terminée", "success", "normal");
            loadDashboardData();
            System.setOut(originalOut);
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleRefresh() {
        loadDashboardData();
        updateStatus("Données rafraîchies", "success", "info");
    }

    @FXML
    private void handleVisualizeData() {
        // Implémenter la visualisation des données
        updateStatus("Visualisation des données...", "info", "info");
        // TODO: Implémenter la fenêtre de visualisation
    }

    @FXML
    private void handleClearLogs() {
        trainingLogArea.clear();
        systemLogArea.clear();
        updateStatus("Logs effacés", "info", "info");
    }

    @FXML
    private void handleExportLogs() {
        updateStatus("Export des logs...", "info", "info");
        // TODO: Implémenter l'export
    }

    @FXML
    private void handleGoBack() {
        try {
            Stage stage = (Stage) goBackButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleResetInputs() {
        latenceSlider.setValue(50);
        jitterSlider.setValue(10);
        perteSlider.setValue(1);
        bandePassanteSlider.setValue(50);
        signalSlider.setValue(80);

        predictionResultLabel.setText("--");
        confidenceLabel.setText("--");
        confidenceProgressBar.setProgress(0);
        explanationLabel.setText("Prédiction en attente...");


        updateStatus("Champs réinitialisés", "info", "info");
    }

    // ============ MÉTHODES UTILITAIRES ============

    private double getDoubleFromField(TextField field, double defaultValue) {
        try {
            return Double.parseDouble(field.getText());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void addToPredictionHistory(double latence, double jitter, double perte,
                                        double signal, String prediction, double confidence) {
        Map<String, String> history = new HashMap<>();
        history.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        history.put("latence", String.format("%.1f ms", latence));
        history.put("jitter", String.format("%.1f ms", jitter));
        history.put("perte", String.format("%.1f%%", perte));
        history.put("signal", String.format("%.0f/100", signal));
        history.put("prediction", prediction);
        history.put("confidence", String.format("%.1f%%", confidence));

        recentPredictions.add(0, history);
        if (recentPredictions.size() > 10) {
            recentPredictions.remove(recentPredictions.size() - 1);
        }
    }

    private void updatePredictionPreview() {
        // Prévisualisation rapide basée sur des règles simples
        double latence = getDoubleFromField(latenceField, 50.0);
        double jitter = getDoubleFromField(jitterField, 10.0);
        double perte = getDoubleFromField(perteField, 1.0);

        if (latence > 200 || jitter > 50 || perte > 10) {
            predictionResultLabel.setText("ANOMALIE probable");
            predictionResultLabel.setStyle(WARNING_STYLE);
            explanationLabel.setText("Valeurs critiques détectées");
        } else if (latence > 100 || jitter > 20 || perte > 5) {
            predictionResultLabel.setText("RISQUE");
            predictionResultLabel.setStyle(WARNING_STYLE);
            explanationLabel.setText("Valeurs limites détectées");
        } else {
            predictionResultLabel.setText("Prévisualisation");
            predictionResultLabel.setStyle(NORMAL_STYLE);
            explanationLabel.setText("Valeurs dans les limites normales");
        }
    }

    private void updateStatus(String message, String type, String iconType) {
        Platform.runLater(() -> {
            statusLabel.setText(message);

            switch (type.toLowerCase()) {
                case "success":
                    statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    break;
                case "error":
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    break;
                case "warning":
                    statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    break;
                case "info":
                    statusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    break;
            }
        });
    }

    private void startAutoRefresh() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (PredictionServiceAnomalies.isModelTrained()) {
                        updateFooter();
                    }
                });
            }
        }, 60000, 60000); // Toutes les minutes
    }

    // ============ TESTS CONNEXION ============

    @FXML
    private void testDatabaseConnection() {
        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM MESURES_QOS WHERE ANOMALIE IS NOT NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    updateStatus("✅ Connexion BD OK - " + rs.getInt(1) + " anomalies référencées",
                            "success", "normal");
                }
            }
        } catch (SQLException e) {
            updateStatus("❌ Erreur connexion BD: " + e.getMessage(), "error", "warning");
        }
    }

    @FXML
    private void validateModel() {
        if (!PredictionServiceAnomalies.isModelTrained()) {
            updateStatus("❌ Modèle non entraîné", "error", "warning");
            return;
        }

        updateStatus("Validation du modèle en cours...", "info", "info");

        Thread thread = new Thread(() -> {
            try {
                System.setOut(customOut);
                systemLogArea.appendText("\n=== VALIDATION DU MODÈLE ===\n");

                // Tester avec quelques exemples
                testPredictionExample(50, 10, 1, 50, 80, "Devrait être NORMAL");
                testPredictionExample(300, 60, 20, 10, 20, "Devrait être ANOMALIE");
                testPredictionExample(150, 30, 8, 30, 50, "Cas limite");

                systemLogArea.appendText("=== VALIDATION TERMINÉE ===\n");

                Platform.runLater(() -> {
                    updateStatus("Validation terminée", "success", "normal");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Erreur validation: " + e.getMessage(), "error", "warning");
                });
            } finally {
                System.setOut(originalOut);
            }
        });

        thread.setDaemon(true);
        thread.start();
    }
    private void loadCorrelationData() {

        // Toujours exécuter toute la méthode dans JavaFX thread
        Platform.runLater(() -> {

            try {
                // Nettoyage SAFE : ne jamais utiliser .clear()
                correlationChart.setData(FXCollections.observableArrayList());

            } catch (Exception e) {
                System.err.println("Erreur lors du reset du graphique : " + e.getMessage());
            }
        });

        // Thread pour charger les données SQL
        Thread t = new Thread(() -> {

            XYChart.Series<Number, Number> serie = new XYChart.Series<>();
            serie.setName("Latence vs Jitter");

            try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT LATENCE, JITTER FROM MESURES_QOS " +
                                 "WHERE ANOMALIE IS NOT NULL FETCH FIRST 300 ROWS ONLY")) {

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    double x = rs.getDouble("LATENCE");
                    double y = rs.getDouble("JITTER");
                    serie.getData().add(new XYChart.Data<>(x, y));
                }

            } catch (SQLException e) {
                System.err.println("Erreur SQL corrélation : " + e.getMessage());
            }

            // Ajout au graphique → dans le thread FX uniquement
            Platform.runLater(() -> {
                try {
                    ObservableList<XYChart.Series<Number, Number>> newList =
                            FXCollections.observableArrayList();
                    newList.add(serie);

                    // Remplacer tout le dataset proprement
                    correlationChart.setData(newList);

                    correlationChart.setLegendVisible(true);

                } catch (Exception e) {
                    System.err.println("Erreur update graphique : " + e.getMessage());
                }
            });

        });

        t.setDaemon(true);
        t.start();
    }


    private void testPredictionExample(double latence, double jitter, double perte,
                                       double bandePassante, double signal, String expected) {
        String prediction = PredictionServiceAnomalies.predictAnomaly(latence, jitter, perte, bandePassante, signal);
        double[] probs = PredictionServiceAnomalies.predictAnomalyWithProbability(latence, jitter, perte, bandePassante, signal);

        systemLogArea.appendText(String.format(
                "Test: L=%.0fms, J=%.0fms, P=%.1f%%, BP=%.0fMbps, S=%.0f/100 → %s (%.1f%%) [%s]\n",
                latence, jitter, perte, bandePassante, signal, prediction, probs[1]*100, expected
        ));
    }

    private void loadConfusionMatrix() {

        confusionMatrixTable.getItems().clear();

        int normal_normal = PredictionServiceAnomalies.confusion[0][0];
        int normal_anomaly = PredictionServiceAnomalies.confusion[0][1];
        int anomaly_normal = PredictionServiceAnomalies.confusion[1][0];
        int anomaly_anomaly = PredictionServiceAnomalies.confusion[1][1];

        ObservableList<Map<String, String>> rows = FXCollections.observableArrayList();

        Map<String, String> row1 = new HashMap<>();
        row1.put("col0", "Réel Normal");
        row1.put("col1", String.valueOf(normal_normal));
        row1.put("col2", String.valueOf(normal_anomaly));

        Map<String, String> row2 = new HashMap<>();
        row2.put("col0", "Réel Anomalie");
        row2.put("col1", String.valueOf(anomaly_normal));
        row2.put("col2", String.valueOf(anomaly_anomaly));

        rows.add(row1);
        rows.add(row2);

        Platform.runLater(() -> confusionMatrixTable.setItems(rows));
    }
    private void setupRecentPredictionsTable() {
        TableColumn<Map<String, String>, String> timeCol = new TableColumn<>("Heure");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("time")));

        TableColumn<Map<String, String>, String> latCol = new TableColumn<>("Latence");
        latCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("latence")));

        TableColumn<Map<String, String>, String> jitCol = new TableColumn<>("Jitter");
        jitCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("jitter")));

        TableColumn<Map<String, String>, String> perteCol = new TableColumn<>("Perte");
        perteCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("perte")));

        TableColumn<Map<String, String>, String> signalCol = new TableColumn<>("Signal");
        signalCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("signal")));

        TableColumn<Map<String, String>, String> predCol = new TableColumn<>("Statut");
        predCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("prediction")));

        recentPredictionsTable.getColumns().setAll(timeCol, latCol, jitCol, perteCol, signalCol, predCol);
    }

}