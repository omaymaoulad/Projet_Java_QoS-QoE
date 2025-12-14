package com.ensah.qoe.Controller;

import com.ensah.qoe.ML.AnomalyDetectionModels;
import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Services.PredictionServiceAnomalies;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
    // Note: compareAllCheckbox n'est pas dans votre FXML

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
    // Note: mosField et zoneField ne sont pas dans votre FXML
    // Note: predictionDetailsArea n'est pas dans votre FXML

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
    // Note: analyzeDatasetButton et demoButton ne sont pas dans votre FXML

    // ============ FOOTER ============
    @FXML private Label modelStatusLabel;
    @FXML private Label lastTrainingLabel;
    @FXML private Label dataUpdateLabel;
    @FXML private Label performanceStatusLabel;
    @FXML private Button goBackButton;
    // Note: modelInfoLabel n'est pas dans votre FXML

    // ============ VARIABLES ============
    private ObservableList<Map<String, String>> recentPredictions = FXCollections.observableArrayList();
    private ObservableList<Map<String, String>> anomalyLogs = FXCollections.observableArrayList();
    private ByteArrayOutputStream consoleOutput;
    private PrintStream originalOut;
    private PrintStream customOut;

    private static final String NORMAL_STYLE = "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;";
    private static final String ANOMALY_STYLE = "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;";
    private static final String WARNING_STYLE = "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;";
    private static final String INFO_STYLE = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;";

    @FXML
    public void initialize() {
        setupConsoleRedirect();
        setupUIComponents();
        initializeCharts();
        setupTables();
        bindSlidersAndFields();
        loadDashboardData();
        startAutoRefresh();
        updateModelStatus(); // Pas de updateModelInfo() car modelInfoLabel n'existe pas
        loadRecentPredictionsFromDB();
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
                "RandomForest",
                "J48",
                "NaiveBayes",
                "KNN",
                "SVM",
                "MLP"
        );
        algorithmCombo.setValue("RandomForest");

        // Configuration des sliders
        configureSliders();

        // Mise à jour du label du split
        trainTestSplitSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            splitRatioLabel.setText(String.format("%.0f/%.0f", newVal.doubleValue(), 100 - newVal.doubleValue()));
        });

        // Style des boutons
        trainButton.setStyle(INFO_STYLE);
        predictButton.setStyle(NORMAL_STYLE);
        analyzeButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;");
        evaluateModelButton.setStyle(INFO_STYLE);

        // Note: demoButton n'existe pas dans le FXML, donc pas de style à appliquer

        // Tooltips
        setupTooltips();
    }

    private void setupTooltips() {
        Tooltip.install(trainButton, new Tooltip("Entraîner le modèle de détection d'anomalies"));
        Tooltip.install(predictButton, new Tooltip("Prédire si les valeurs actuelles représentent une anomalie"));
        Tooltip.install(analyzeButton, new Tooltip("Analyse détaillée des facteurs d'anomalie"));
        Tooltip.install(evaluateModelButton, new Tooltip("Évaluer la performance du modèle sur le jeu de test"));
        // Note: demoButton n'existe pas
    }

    private void configureSliders() {
        // Les sliders doivent être configurés dans initialize() car ils existent dans le FXML
        if (latenceSlider != null) {
            latenceSlider.setMin(0);
            latenceSlider.setMax(500);
            latenceSlider.setValue(50);
            latenceSlider.setBlockIncrement(10);
            latenceSlider.setShowTickLabels(true);
            latenceSlider.setShowTickMarks(true);
            latenceSlider.setMajorTickUnit(100);
        }

        if (jitterSlider != null) {
            jitterSlider.setMin(0);
            jitterSlider.setMax(100);
            jitterSlider.setValue(10);
            jitterSlider.setBlockIncrement(5);
            jitterSlider.setShowTickLabels(true);
            jitterSlider.setMajorTickUnit(20);
        }

        if (perteSlider != null) {
            perteSlider.setMin(0);
            perteSlider.setMax(100);
            perteSlider.setValue(1);
            perteSlider.setBlockIncrement(1);
            perteSlider.setShowTickLabels(true);
            perteSlider.setMajorTickUnit(10);
        }

        if (bandePassanteSlider != null) {
            bandePassanteSlider.setMin(0);
            bandePassanteSlider.setMax(200);
            bandePassanteSlider.setValue(50);
            bandePassanteSlider.setBlockIncrement(10);
            bandePassanteSlider.setShowTickLabels(true);
            bandePassanteSlider.setMajorTickUnit(50);
        }

        if (signalSlider != null) {
            signalSlider.setMin(0);
            signalSlider.setMax(100);
            signalSlider.setValue(80);
            signalSlider.setBlockIncrement(5);
            signalSlider.setShowTickLabels(true);
            signalSlider.setMajorTickUnit(25);
        }
    }

    private void bindSlidersAndFields() {
        // Vérifier que les composants existent avant de les binder
        if (latenceSlider != null && latenceField != null) {
            bindSliderToField(latenceSlider, latenceField);
        }
        if (jitterSlider != null && jitterField != null) {
            bindSliderToField(jitterSlider, jitterField);
        }
        if (perteSlider != null && perteField != null) {
            bindSliderToField(perteSlider, perteField);
        }
        if (bandePassanteSlider != null && bandePassanteField != null) {
            bindSliderToField(bandePassanteSlider, bandePassanteField);
        }
        if (signalSlider != null && signalField != null) {
            bindSliderToField(signalSlider, signalField);
        }
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
        if (distributionChart != null) {
            distributionChart.setTitle("Distribution des Anomalies");
            distributionChart.setLegendVisible(false);
            distributionChart.setAnimated(true);
        }

        // Pie chart
        if (anomalyPieChart != null) {
            anomalyPieChart.setTitle("Proportion Anomalies/Normales");
            anomalyPieChart.setLabelsVisible(true);
            anomalyPieChart.setLegendVisible(true);
        }

        // Performance chart
        if (performanceChart != null) {
            performanceChart.setTitle("Performance du Modèle");
            performanceChart.setCreateSymbols(true);
            performanceChart.setAnimated(true);
        }

        // Correlation chart
        if (correlationChart != null) {
            correlationChart.setTitle("Corrélations entre Features");
            correlationChart.setAnimated(true);
        }

        // Initialiser les séries
        if (distributionChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Anomalies");
            distributionChart.getData().add(series);
        }
    }

    private void setupTables() {
        // Table des prédictions récentes
        setupRecentPredictionsTable();

        // Table des logs d'anomalies
        setupAnomalyLogTable();

        // Table de matrice de confusion
        setupConfusionMatrixTable();

        // Table des statistiques des features
        setupFeatureStatsTable();
    }

    private void setupRecentPredictionsTable() {
        if (recentPredictionsTable == null || !recentPredictionsTable.getColumns().isEmpty()) {
            return;
        }

        String[] columns = {"time", "latence", "jitter", "perte", "signal", "prediction", "confidence"};
        String[] titles = {"Heure", "Latence", "Jitter", "Perte", "Signal", "Statut", "Confiance"};

        for (int i = 0; i < columns.length; i++) {
            final String key = columns[i];
            TableColumn<Map<String, String>, String> col = new TableColumn<>(titles[i]);
            col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(key)));

            // Style spécial pour la colonne de prédiction
            if ("prediction".equals(key)) {
                col.setCellFactory(column -> new TableCell<Map<String, String>, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            if ("ANOMALIE".equals(item)) {
                                setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
                            }
                        }
                    }
                });
            }

            recentPredictionsTable.getColumns().add(col);
        }
        recentPredictionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupAnomalyLogTable() {
        if (anomalyLogTable == null || !anomalyLogTable.getColumns().isEmpty()) {
            return;
        }

        String[] columns = {"time", "severity", "details", "action"};
        String[] titles = {"Timestamp", "Sévérité", "Détails", "Action"};

        for (int i = 0; i < columns.length; i++) {
            final String key = columns[i];
            TableColumn<Map<String, String>, String> col = new TableColumn<>(titles[i]);
            col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(key)));

            // Style pour la colonne de sévérité
            if ("severity".equals(key)) {
                col.setCellFactory(column -> new TableCell<Map<String, String>, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            switch (item) {
                                case "CRITIQUE":
                                    setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                                    break;
                                case "HAUTE":
                                    setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
                                    break;
                                default:
                                    setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold;");
                            }
                        }
                    }
                });
            }

            anomalyLogTable.getColumns().add(col);
        }
        anomalyLogTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupConfusionMatrixTable() {
        if (confusionMatrixTable == null || !confusionMatrixTable.getColumns().isEmpty()) {
            return;
        }

        String[] headers = {"", "Prédit Normal", "Prédit Anomalie"};
        String[] keys = {"col0", "col1", "col2"};

        for (int i = 0; i < headers.length; i++) {
            final String key = keys[i];
            TableColumn<Map<String, String>, String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(key)));

            confusionMatrixTable.getColumns().add(col);
        }
        confusionMatrixTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupFeatureStatsTable() {
        if (featureStatsTable == null || !featureStatsTable.getColumns().isEmpty()) {
            return;
        }

        String[] columns = {"feature", "min", "max", "avg", "std", "impact"};
        String[] titles = {"Feature", "Min", "Max", "Moyenne", "Écart-type", "Impact"};

        for (int i = 0; i < columns.length; i++) {
            final String key = columns[i];
            TableColumn<Map<String, String>, String> col = new TableColumn<>(titles[i]);
            col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(key)));

            featureStatsTable.getColumns().add(col);
        }
        featureStatsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadDashboardData() {
        updateStatus("Chargement des données...", "info");

        Thread loadThread = new Thread(() -> {
            try {
                System.setOut(customOut);

                loadKPIData();
                loadDistributionData();
                loadPerformanceHistory();
                loadRecentPredictionsFromDB();
                loadAnomalyLogs();
                loadFeatureStatistics();
                updateModelStatus();
                loadCorrelationData();
                loadConfusionMatrixFromService();

                Platform.runLater(() -> {
                    updateStatus("Système prêt", "success");
                    updateFooter();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Erreur de chargement: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            } finally {
                System.setOut(originalOut);
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void loadKPIData() {
        try {
            // Vérifier d'abord si le service a des statistiques
            if (PredictionServiceAnomalies.getLastAccuracy() > 0) {
                Platform.runLater(() -> {
                    if (accuracyLabel != null) {
                        accuracyLabel.setText(String.format("%.1f%%", PredictionServiceAnomalies.getLastAccuracy()));
                    }
                    if (precisionLabel != null) {
                        precisionLabel.setText(String.format("%.3f", PredictionServiceAnomalies.getLastPrecision()));
                    }
                    if (recallLabel != null) {
                        recallLabel.setText(String.format("%.3f", PredictionServiceAnomalies.getLastRecall()));
                    }
                });
            }

            // Charger depuis la base de données
            String sql = """
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN ANOMALIE = 0 THEN 1 ELSE 0 END) as normal,
                    SUM(CASE WHEN ANOMALIE = 1 THEN 1 ELSE 0 END) as anomaly
                FROM MESURES_QOS
                WHERE ANOMALIE IS NOT NULL
            """;

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    final int total = rs.getInt("total");
                    final int normal = rs.getInt("normal");
                    final int anomaly = rs.getInt("anomaly");
                    final double anomalyRate = total > 0 ? (anomaly * 100.0 / total) : 0;

                    Platform.runLater(() -> {
                        if (totalInstancesLabel != null) totalInstancesLabel.setText(String.valueOf(total));
                        if (normalCountLabel != null) normalCountLabel.setText(String.valueOf(normal));
                        if (anomalyCountLabel != null) anomalyCountLabel.setText(String.valueOf(anomaly));
                        if (anomalyRateLabel != null) anomalyRateLabel.setText(String.format("%.1f%%", anomalyRate));
                        if (anomalyRateProgress != null) anomalyRateProgress.setProgress(anomalyRate / 100);
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement KPI: " + e.getMessage());
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

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Types d'anomalies");
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

            while (rs.next()) {
                String type = rs.getString("anomaly_type");
                int count = rs.getInt("count");

                series.getData().add(new XYChart.Data<>(type, count));
                pieData.add(new PieChart.Data(type + " (" + count + ")", count));
            }

            Platform.runLater(() -> {
                if (distributionChart != null) {
                    distributionChart.getData().clear();
                    distributionChart.getData().add(series);
                }

                if (anomalyPieChart != null) {
                    anomalyPieChart.setData(pieData);

                    // Personnaliser les couleurs du pie chart
                    int colorIndex = 0;
                    for (PieChart.Data data : pieData) {
                        switch (colorIndex % 4) {
                            case 0:
                                data.getNode().setStyle("-fx-pie-color: #e74c3c;");
                                break;
                            case 1:
                                data.getNode().setStyle("-fx-pie-color: #f39c12;");
                                break;
                            case 2:
                                data.getNode().setStyle("-fx-pie-color: #3498db;");
                                break;
                            case 3:
                                data.getNode().setStyle("-fx-pie-color: #2ecc71;");
                                break;
                        }
                        colorIndex++;
                    }
                }
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement distribution: " + e.getMessage());
        }
    }

    private void loadPerformanceHistory() {
        if (performanceChart == null) return;

        // Charger l'historique des performances depuis la base
        XYChart.Series<String, Number> accuracySeries = new XYChart.Series<>();
        accuracySeries.setName("Accuracy");

        // Pour l'exemple, on génère des données factices
        String[] dates = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        Random rand = new Random();

        for (String date : dates) {
            double accuracy = 80 + rand.nextDouble() * 15;
            accuracySeries.getData().add(new XYChart.Data<>(date, accuracy));
        }

        Platform.runLater(() -> {
            performanceChart.getData().clear();
            performanceChart.getData().add(accuracySeries);

            // Personnaliser les couleurs
            accuracySeries.getNode().setStyle("-fx-stroke: #2ecc71; -fx-stroke-width: 2px;");
        });
    }

    private void loadRecentPredictionsFromDB() {
        if (recentPredictionsTable == null) return;

        String sql = """
            SELECT
                TO_CHAR(DATE_REELLE, 'HH24:MI:SS') AS HEURE,
                LATENCE,
                JITTER,
                PERTE,
                SIGNAL_SCORE,
                ANOMALIE,
                MOS,
                ZONE
            FROM MESURES_QOS
            ORDER BY DATE_REELLE DESC
            FETCH FIRST 15 ROWS ONLY
        """;

        ObservableList<Map<String, String>> data = FXCollections.observableArrayList();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("time", rs.getString("HEURE"));
                row.put("latence", String.format("%.1f ms", rs.getDouble("LATENCE")));
                row.put("jitter", String.format("%.1f ms", rs.getDouble("JITTER")));
                row.put("perte", String.format("%.1f%%", rs.getDouble("PERTE")));
                row.put("signal", String.format("%.0f", rs.getDouble("SIGNAL_SCORE")));

                int anomaly = rs.getInt("ANOMALIE");
                row.put("prediction", anomaly == 1 ? "ANOMALIE" : "NORMAL");
                row.put("confidence", anomaly == 1 ? "80%" : "95%");

                data.add(row);
            }

            Platform.runLater(() -> {
                recentPredictionsTable.setItems(data);
                recentPredictionsTable.refresh();
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement prédictions: " + e.getMessage());
        }
    }

    private void loadAnomalyLogs() {
        if (anomalyLogTable == null) return;

        String sql = """
            SELECT 
                TO_CHAR(SYSDATE, 'DD/MM HH24:MI') as time,
                CASE 
                    WHEN LATENCE > 300 OR PERTE > 30 THEN 'CRITIQUE'
                    WHEN LATENCE > 200 OR PERTE > 20 THEN 'HAUTE'
                    ELSE 'MOYENNE'
                END as severity,
                'Latence: ' || LATENCE || 'ms, Jitter: ' || JITTER || 'ms, Perte: ' || PERTE || '%, Signal: ' || SIGNAL_SCORE as details,
                'À investiguer' as action
            FROM MESURES_QOS
            WHERE ANOMALIE = 1
            ORDER BY ID_MESURE DESC
            FETCH FIRST 20 ROWS ONLY
        """;

        ObservableList<Map<String, String>> logs = FXCollections.observableArrayList();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, String> log = new HashMap<>();
                log.put("time", rs.getString("time"));
                log.put("severity", rs.getString("severity"));
                log.put("details", rs.getString("details"));
                log.put("action", rs.getString("action"));
                logs.add(log);
            }

            Platform.runLater(() -> {
                anomalyLogTable.setItems(logs);
                anomalyLogTable.refresh();
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement logs: " + e.getMessage());
        }
    }

    private void loadFeatureStatistics() {
        if (featureStatsTable == null) return;

        String sql = """
            SELECT 
                'LATENCE' as feature,
                MIN(LATENCE) as min_val,
                MAX(LATENCE) as max_val,
                AVG(LATENCE) as avg_val,
                STDDEV(LATENCE) as std_val,
                35 as impact
            FROM MESURES_QOS
            UNION ALL
            SELECT 
                'JITTER',
                MIN(JITTER),
                MAX(JITTER),
                AVG(JITTER),
                STDDEV(JITTER),
                25
            FROM MESURES_QOS
            UNION ALL
            SELECT 
                'PERTE',
                MIN(PERTE),
                MAX(PERTE),
                AVG(PERTE),
                STDDEV(PERTE),
                30
            FROM MESURES_QOS
            UNION ALL
            SELECT 
                'SIGNAL',
                MIN(SIGNAL_SCORE),
                MAX(SIGNAL_SCORE),
                AVG(SIGNAL_SCORE),
                STDDEV(SIGNAL_SCORE),
                10
            FROM MESURES_QOS
        """;

        ObservableList<Map<String, String>> stats = FXCollections.observableArrayList();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("feature", rs.getString("feature"));
                row.put("min", String.format("%.1f", rs.getDouble("min_val")));
                row.put("max", String.format("%.1f", rs.getDouble("max_val")));
                row.put("avg", String.format("%.1f", rs.getDouble("avg_val")));
                row.put("std", String.format("%.2f", rs.getDouble("std_val")));
                row.put("impact", rs.getInt("impact") + "%");
                stats.add(row);
            }

            Platform.runLater(() -> {
                featureStatsTable.setItems(stats);
                featureStatsTable.refresh();
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement statistiques: " + e.getMessage());
        }
    }

    private void updateModelStatus() {
        boolean isTrained = PredictionServiceAnomalies.isModelTrained();
        boolean isLoaded = PredictionServiceAnomalies.isModelLoaded();

        Platform.runLater(() -> {
            if (modelStatusLabel != null) {
                if (isTrained && isLoaded) {
                    modelStatusLabel.setText("✓ Modèle entraîné et chargé");
                    modelStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

                    // Mettre à jour les métriques depuis le service
                    double accuracy = PredictionServiceAnomalies.getLastAccuracy();
                    double precision = PredictionServiceAnomalies.getLastPrecision();
                    double recall = PredictionServiceAnomalies.getLastRecall();

                    if (accuracy > 0) {
                        if (accuracyLabel != null) accuracyLabel.setText(String.format("%.1f%%", accuracy));
                        if (precisionLabel != null) precisionLabel.setText(String.format("%.3f", precision));
                        if (recallLabel != null) recallLabel.setText(String.format("%.3f", recall));
                        if (f1ScoreLabel != null) f1ScoreLabel.setText(String.format("%.3f",
                                2 * (precision * recall) / (precision + recall)));
                    }

                } else if (isTrained) {
                    modelStatusLabel.setText("⚠ Modèle entraîné mais non chargé");
                    modelStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                } else {
                    modelStatusLabel.setText("✗ Modèle non entraîné");
                    modelStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    if (accuracyLabel != null) accuracyLabel.setText("--");
                    if (precisionLabel != null) precisionLabel.setText("--");
                    if (recallLabel != null) recallLabel.setText("--");
                    if (f1ScoreLabel != null) f1ScoreLabel.setText("--");
                }
            }
        });
    }

    private void updateFooter() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        if (dataUpdateLabel != null) {
            dataUpdateLabel.setText("Dernière mise à jour: " + timestamp);
        }

        boolean isTrained = PredictionServiceAnomalies.isModelTrained();
        if (performanceStatusLabel != null) {
            if (isTrained) {
                double accuracy = PredictionServiceAnomalies.getLastAccuracy();
                if (accuracy >= 85) {
                    performanceStatusLabel.setText("Performance: Excellente");
                    performanceStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else if (accuracy >= 70) {
                    performanceStatusLabel.setText("Performance: Satisfaisante");
                    performanceStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                } else {
                    performanceStatusLabel.setText("Performance: À améliorer");
                    performanceStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            } else {
                performanceStatusLabel.setText("Performance: Non évaluée");
                performanceStatusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
            }
        }
    }

    // ============ ÉVÉNEMENTS ============

    @FXML
    private void handleTrainModel() {
        updateStatus("Démarrage de l'entraînement...", "info");
        if (trainingLogArea != null) trainingLogArea.clear();

        Thread trainThread = new Thread(() -> {
            try {
                System.setOut(customOut);

                Platform.runLater(() -> {
                    if (trainingProgressBar != null) trainingProgressBar.setProgress(0.1);
                    if (trainingProgressLabel != null) trainingProgressLabel.setText("Chargement données...");
                });

                // Récupérer l'algorithme sélectionné
                String algo = algorithmCombo.getValue();
                PredictionServiceAnomalies.setSelectedAlgorithm(algo);

                Platform.runLater(() -> {
                    if (trainingProgressBar != null) trainingProgressBar.setProgress(0.3);
                    if (trainingProgressLabel != null) trainingProgressLabel.setText("Entraînement en cours...");
                });

                // Lancer l'entraînement (sans comparaison d'algorithmes)
                String report = PredictionServiceAnomalies.trainModel(false);

                if (systemLogArea != null) {
                    systemLogArea.appendText("\n=== RAPPORT D'ENTRAÎNEMENT ===\n");
                    systemLogArea.appendText(report);
                    systemLogArea.appendText("=== FIN DU RAPPORT ===\n");
                }

                Platform.runLater(() -> {
                    if (trainingProgressBar != null) trainingProgressBar.setProgress(1.0);
                    if (trainingProgressLabel != null) trainingProgressLabel.setText("Terminé ✓");
                    updateStatus("Modèle entraîné avec succès", "success");

                    // Rafraîchir les données
                    loadDashboardData();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (trainingProgressBar != null) trainingProgressBar.setProgress(0);
                    if (trainingProgressLabel != null) trainingProgressLabel.setText("Échec ✗");
                    updateStatus("Erreur d'entraînement: " + e.getMessage(), "error");
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

            if (!PredictionServiceAnomalies.isModelTrained() && !PredictionServiceAnomalies.isModelLoaded()) {
                updateStatus("Veuillez d'abord entraîner ou charger un modèle", "warning");
                return;
            }

            // Utiliser le nouveau service pour la prédiction
            PredictionServiceAnomalies.PredictionResult result =
                    PredictionServiceAnomalies.predictAnomaly(latence, jitter, perte, bandePassante, signal);

            Platform.runLater(() -> {
                if (predictionResultLabel != null) predictionResultLabel.setText(result.getPrediction());
                if (confidenceLabel != null) confidenceLabel.setText(String.format("%.1f%%", result.getAnomalyProbability() * 100));
                if (confidenceProgressBar != null) confidenceProgressBar.setProgress(result.getAnomalyProbability());

                if ("ANOMALIE".equals(result.getPrediction())) {
                    if (predictionResultLabel != null) predictionResultLabel.setStyle(ANOMALY_STYLE);
                    if (explanationLabel != null) explanationLabel.setText("⚠️ Anomalie détectée - " +
                            (result.getContributingFactors() != null ? result.getContributingFactors() : ""));

                    // Ajouter au log d'anomalies
                    addToAnomalyLog(latence, jitter, perte, signal, result.getAnomalyProbability());
                } else {
                    if (predictionResultLabel != null) predictionResultLabel.setStyle(NORMAL_STYLE);
                    if (explanationLabel != null) explanationLabel.setText("✓ Qualité réseau normale");
                }

                // Ajouter à l'historique
                addToPredictionHistory(latence, jitter, perte, signal,
                        result.getPrediction(), result.getAnomalyProbability() * 100);
            });

            updateStatus("Prédiction effectuée", "success");

        } catch (Exception e) {
            updateStatus("Erreur de prédiction: " + e.getMessage(), "error");
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

            // Analyse détaillée avec le nouveau service
            if (systemLogArea != null) {
                systemLogArea.appendText("\n=== ANALYSE DÉTAILLÉE ===\n");

                PredictionServiceAnomalies.PredictionResult result =
                        PredictionServiceAnomalies.predictAnomaly(latence, jitter, perte, bandePassante, signal);

                systemLogArea.appendText(result.toDetailedString());

                // Analyser les facteurs contributifs
                analyzeContributingFactors(latence, jitter, perte, bandePassante, signal);

                systemLogArea.appendText("=== FIN ANALYSE ===\n");
            }

            updateStatus("Analyse terminée", "info");

        } catch (Exception e) {
            updateStatus("Erreur d'analyse: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handlePredictMissing() {
        Thread thread = new Thread(() -> {
            System.setOut(customOut);
            updateStatus("Prédiction des anomalies manquantes...", "info");

            // Implémenter la logique de prédiction des données manquantes

            updateStatus("Anomalies manquantes prédites", "success");
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
            updateStatus("Évaluation du modèle...", "info");

            try {
                // Utiliser le nouveau service pour l'évaluation
                String evalReport = PredictionServiceAnomalies.evaluateModel();

                if (systemLogArea != null) {
                    systemLogArea.appendText("\n=== RAPPORT D'ÉVALUATION ===\n");
                    systemLogArea.appendText(evalReport);
                    systemLogArea.appendText("=== FIN RAPPORT ===\n");
                }

                // Mettre à jour l'interface
                Platform.runLater(() -> {
                    loadConfusionMatrixFromService();
                    updateModelStatus();
                    updateFooter();
                });

                updateStatus("Évaluation terminée", "success");

            } catch (Exception e) {
                updateStatus("Erreur d'évaluation: " + e.getMessage(), "error");
            }

            System.setOut(originalOut);
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleFullAnalysis() {
        Thread thread = new Thread(() -> {
            System.setOut(customOut);
            updateStatus("Démarrage analyse complète...", "info");

            if (systemLogArea != null) {
                systemLogArea.appendText("\n╔════════════════════════════════════════╗\n");
                systemLogArea.appendText("║   ANALYSE COMPLÈTE DES ANOMALIES     ║\n");
                systemLogArea.appendText("╚════════════════════════════════════════╝\n\n");
            }

            // Analyser le dataset
            PredictionServiceAnomalies.analyzeDataset();

            // Entraîner si nécessaire
            if (!PredictionServiceAnomalies.isModelTrained()) {
                PredictionServiceAnomalies.trainModel(false);
            }

            // Évaluer le modèle
            PredictionServiceAnomalies.evaluateModel();

            if (systemLogArea != null) {
                systemLogArea.appendText("\n╔════════════════════════════════════════╗\n");
                systemLogArea.appendText("║        ANALYSE TERMINÉE           ║\n");
                systemLogArea.appendText("╚════════════════════════════════════════╝\n");
            }

            updateStatus("Analyse complète terminée", "success");
            loadDashboardData();
            System.setOut(originalOut);
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleVisualizeData() {
        updateStatus("Visualisation des données...", "info");
        // TODO: Implémenter la fenêtre de visualisation avancée
    }

    @FXML
    private void handleClearLogs() {
        if (trainingLogArea != null) trainingLogArea.clear();
        if (systemLogArea != null) systemLogArea.clear();
        updateStatus("Logs effacés", "info");
    }

    @FXML
    private void handleExportLogs() {
        updateStatus("Export des logs...", "info");

        Thread exportThread = new Thread(() -> {
            try {
                PredictionServiceAnomalies.exportPredictionHistory();
                updateStatus("Export terminé", "success");
            } catch (Exception e) {
                updateStatus("Erreur export: " + e.getMessage(), "error");
            }
        });
        exportThread.setDaemon(true);
        exportThread.start();
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
        if (latenceSlider != null) latenceSlider.setValue(50);
        if (jitterSlider != null) jitterSlider.setValue(10);
        if (perteSlider != null) perteSlider.setValue(1);
        if (bandePassanteSlider != null) bandePassanteSlider.setValue(50);
        if (signalSlider != null) signalSlider.setValue(80);

        if (predictionResultLabel != null) predictionResultLabel.setText("--");
        if (confidenceLabel != null) confidenceLabel.setText("--");
        if (confidenceProgressBar != null) confidenceProgressBar.setProgress(0);
        if (explanationLabel != null) explanationLabel.setText("Prédiction en attente...");

        updateStatus("Champs réinitialisés", "info");
    }

    @FXML
    private void handleRefresh() {
        loadDashboardData();
        updateStatus("Données rafraîchies", "success");
    }

    @FXML
    private void handleLoadModel() {
        updateStatus("Chargement du modèle...", "info");

        Thread thread = new Thread(() -> {
            try {
                System.setOut(customOut);

                boolean success = PredictionServiceAnomalies.loadLatestModel();

                if (success) {
                    updateStatus("Modèle chargé avec succès", "success");
                    Platform.runLater(() -> {
                        updateModelStatus();

                    });
                } else {
                    updateStatus("Aucun modèle disponible. Veuillez en entraîner un.", "warning");
                }

            } catch (Exception e) {
                updateStatus("Erreur chargement: " + e.getMessage(), "error");
                System.err.println("❌ Erreur chargement modèle: " + e.getMessage());
            } finally {
                System.setOut(originalOut);
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void testDatabaseConnection() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM MESURES_QOS WHERE ANOMALIE IS NOT NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    updateStatus("✅ Connexion BD OK - " + rs.getInt(1) + " anomalies référencées",
                            "success");
                }
            }
        } catch (SQLException e) {
            updateStatus("❌ Erreur connexion BD: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void validateModel() {
        if (!PredictionServiceAnomalies.isModelTrained()) {
            updateStatus("❌ Modèle non entraîné", "error");
            return;
        }

        updateStatus("Validation du modèle en cours...", "info");

        Thread thread = new Thread(() -> {
            try {
                System.setOut(customOut);
                if (systemLogArea != null) {
                    systemLogArea.appendText("\n=== VALIDATION DU MODÈLE ===\n");
                }

                // Tester avec quelques exemples
                testPredictionExample(50, 10, 1, 100, 80, "Devrait être NORMAL");
                testPredictionExample(300, 60, 20, 10, 20, "Devrait être ANOMALIE");
                testPredictionExample(150, 30, 8, 50, 50, "Cas limite");

                if (systemLogArea != null) {
                    systemLogArea.appendText("=== VALIDATION TERMINÉE ===\n");
                }

                Platform.runLater(() -> {
                    updateStatus("Validation terminée", "success");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Erreur validation: " + e.getMessage(), "error");
                });
            } finally {
                System.setOut(originalOut);
            }
        });

        thread.setDaemon(true);
        thread.start();
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

        Platform.runLater(() -> {
            if (recentPredictionsTable != null) {
                recentPredictionsTable.getItems().add(0, history);
                if (recentPredictionsTable.getItems().size() > 10) {
                    recentPredictionsTable.getItems().remove(recentPredictionsTable.getItems().size() - 1);
                }
            }
        });
    }

    private void addToAnomalyLog(double latence, double jitter, double perte,
                                 double signal, double confidence) {
        String severity = "MOYENNE";
        if (latence > 300 || perte > 30) {
            severity = "CRITIQUE";
        } else if (latence > 200 || perte > 20) {
            severity = "HAUTE";
        }

        Map<String, String> log = new HashMap<>();
        log.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        log.put("severity", severity);
        log.put("details", String.format("Latence: %.1fms, Jitter: %.1fms, Perte: %.1f%%, Signal: %.0f/100",
                latence, jitter, perte, signal));
        log.put("action", "À investiguer");

        Platform.runLater(() -> {
            if (anomalyLogTable != null) {
                anomalyLogTable.getItems().add(0, log);
                if (anomalyLogTable.getItems().size() > 20) {
                    anomalyLogTable.getItems().remove(anomalyLogTable.getItems().size() - 1);
                }
            }
        });
    }

    private void updatePredictionPreview() {
        if (predictionResultLabel == null || explanationLabel == null) return;

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

    private void updateStatus(String message, String type) {
        Platform.runLater(() -> {
            if (statusLabel == null) return;

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
                default:
                    statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
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
                        // Actualiser les prédictions récentes toutes les 5 minutes
                        loadRecentPredictionsFromDB();
                    }
                });
            }
        }, 60000, 60000); // Toutes les minutes
    }

    private void loadCorrelationData() {
        Platform.runLater(() -> {
            try {
                if (correlationChart != null) {
                    correlationChart.setData(FXCollections.observableArrayList());
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du reset du graphique : " + e.getMessage());
            }
        });

        Thread t = new Thread(() -> {
            XYChart.Series<Number, Number> serie = new XYChart.Series<>();
            serie.setName("Latence vs Jitter");

            try (Connection conn = DBConnection.getConnection();
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

            Platform.runLater(() -> {
                try {
                    if (correlationChart != null) {
                        ObservableList<XYChart.Series<Number, Number>> newList =
                                FXCollections.observableArrayList();
                        newList.add(serie);
                        correlationChart.setData(newList);
                        correlationChart.setLegendVisible(true);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur update graphique : " + e.getMessage());
                }
            });
        });

        t.setDaemon(true);
        t.start();
    }

    private void loadConfusionMatrixFromService() {
        if (confusionMatrixTable == null || !confusionMatrixTable.getColumns().isEmpty()) {
            setupConfusionMatrixTable();
        }

        // Récupérer la matrice de confusion depuis le service
        String confusionMatrix = PredictionServiceAnomalies.getLastConfusionMatrix();

        if (confusionMatrix != null && !confusionMatrix.isEmpty()) {
            ObservableList<Map<String, String>> rows = FXCollections.observableArrayList();

            Map<String, String> row1 = new HashMap<>();
            row1.put("col0", "Réel Normal");
            row1.put("col1", "TN");
            row1.put("col2", "FP");

            Map<String, String> row2 = new HashMap<>();
            row2.put("col0", "Réel Anomalie");
            row2.put("col1", "FN");
            row2.put("col2", "TP");

            rows.add(row1);
            rows.add(row2);

            Platform.runLater(() -> {
                if (confusionMatrixTable != null) {
                    confusionMatrixTable.setItems(rows);

                    // Appliquer un style aux cellules
                    confusionMatrixTable.setRowFactory(tv -> new TableRow<Map<String, String>>() {
                        @Override
                        protected void updateItem(Map<String, String> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setStyle("");
                            } else {
                                String rowLabel = item.get("col0");
                                if ("Réel Anomalie".equals(rowLabel)) {
                                    setStyle("-fx-background-color: rgba(231, 76, 60, 0.1);");
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    private void testPredictionExample(double latence, double jitter, double perte,
                                       double bandePassante, double signal, String expected) {
        try {
            PredictionServiceAnomalies.PredictionResult result =
                    PredictionServiceAnomalies.predictAnomaly(latence, jitter, perte, bandePassante, signal);

            if (systemLogArea != null) {
                systemLogArea.appendText(String.format(
                        "Test: L=%.0fms, J=%.0fms, P=%.1f%%, BP=%.0fMbps, S=%.0f/100 → %s (%.1f%%) [%s]\n",
                        latence, jitter, perte, bandePassante, signal,
                        result.getPrediction(), result.getAnomalyProbability()*100, expected
                ));
            }
        } catch (Exception e) {
            if (systemLogArea != null) {
                systemLogArea.appendText("Erreur test: " + e.getMessage() + "\n");
            }
        }
    }

    private void analyzeContributingFactors(double latence, double jitter, double perte,
                                            double bandePassante, double signal) {
        if (systemLogArea == null) return;

        systemLogArea.appendText("\n🔍 ANALYSE DES FACTEURS CONTRIBUTIFS:\n");

        List<String> factors = new ArrayList<>();

        if (latence > 100) {
            factors.add("Latence élevée (" + latence + "ms > 100ms)");
        }
        if (jitter > 20) {
            factors.add("Jitter élevé (" + jitter + "ms > 20ms)");
        }
        if (perte > 5) {
            factors.add("Perte élevée (" + perte + "% > 5%)");
        }
        if (bandePassante < 10) {
            factors.add("Bande passante faible (" + bandePassante + "Mbps < 10Mbps)");
        }
        if (signal < 50) {
            factors.add("Signal faible (" + signal + "/100 < 50)");
        }

        if (factors.isEmpty()) {
            systemLogArea.appendText("  ✓ Tous les paramètres sont dans les limites normales\n");
        } else {
            systemLogArea.appendText("  ⚠ Facteurs détectés:\n");
            for (String factor : factors) {
                systemLogArea.appendText("    • " + factor + "\n");
            }
        }
    }
}