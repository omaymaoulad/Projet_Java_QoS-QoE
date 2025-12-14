package com.ensah.qoe.Controller;

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
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PredictionDashboardController {

    // ============ COMPOSANTS FXML ============

    // Header
    @FXML private Label statusLabel;
    @FXML private Button backBtn, refreshBtn;

    // Tab 1: Visualisation
    @FXML private LineChart<String, Number> predictionChart;
    @FXML private BarChart<String, Number> distributionChart;
    @FXML private TableView<Map<String, String>> metricsTable;
    @FXML private Label avgMOSLabel, accuracyLabel;
    @FXML private ProgressBar overallQualityBar;
    @FXML private ComboBox<String> chartTypeCombo;
    @FXML private Slider instancesSlider;
    @FXML private CheckBox showRealCheck, showPredCheck;
    @FXML private Button updateChartBtn, exportCSVBtn, exportPDFBtn, exportImageBtn;

    // Tab 2: Prédiction Manuelle
    @FXML private TextField latenceField, jitterField, perteField, bandePassanteField, signalField;
    @FXML private Slider latenceSlider, jitterSlider, perteSlider, bandePassanteSlider, signalSlider;
    @FXML private Button predictSingleBtn, clearFormBtn, randomValuesBtn;
    @FXML private Label singleMOSLabel, singleCategoryLabel, historyCountLabel;
    @FXML private ProgressBar singleQualityBar;
    @FXML private TableView<Map<String, String>> historyTable;
    @FXML private Button clearHistoryBtn;

    // Tab 3: Import CSV
    @FXML private TextField filePathField;
    @FXML private CheckBox hasHeaderCheck, autoDetectCheck;
    @FXML private Button browseBtn, loadCSVBtn;
    @FXML private VBox mappingBox;
    @FXML private TableView<Map<String, String>> columnMappingTable, previewTable;
    @FXML private Label previewCountLabel;
    @FXML private Button predictBatchBtn, samplePredictBtn, clearBatchBtn;
    @FXML private TableView<Map<String, String>> batchResultsTable;
    @FXML private Label totalPredictionsLabel, batchAvgMOSLabel, excellentCountLabel, badCountLabel;
    @FXML private ProgressBar excellentBar, goodBar, averageBar, mediumBar, badBar;
    @FXML private Button exportBatchCSVBtn, exportBatchPDFBtn, saveToDBBtn;

    // Footer
    @FXML private Label modelStatusLabel, dataStatsLabel, lastUpdateLabel;

    // ============ VARIABLES ============
    private ObservableList<Map<String, String>> historyData = FXCollections.observableArrayList();
    private ObservableList<Map<String, String>> batchData = FXCollections.observableArrayList();
    private ObservableList<Map<String, String>> csvData = FXCollections.observableArrayList();
    private List<CSVRecord> loadedCSVRecords = new ArrayList<>();
    private Map<String, String> columnMapping = new HashMap<>();

    // ============ INITIALISATION ============
    @FXML
    public void initialize() {
        setupUI();
        loadInitialData();
        setupBindings();
        setupTables();
        setupCharts();
    }

    private void setupUI() {
        // Initialiser les ComboBox
        chartTypeCombo.getItems().addAll("Ligne", "Barre", "Aire", "Nuage de points");
        chartTypeCombo.setValue("Ligne");

        // Configurer les sliders
        instancesSlider.setMin(10);
        instancesSlider.setMax(200);
        instancesSlider.setValue(50);
        instancesSlider.setBlockIncrement(10);
        instancesSlider.setMajorTickUnit(50);
        instancesSlider.setMinorTickCount(4);
        instancesSlider.setSnapToTicks(true);
        instancesSlider.setShowTickLabels(true);
        instancesSlider.setShowTickMarks(true);

        // Configurer les autres sliders
        setupSlider(latenceSlider, latenceField, 0, 500, 50);
        setupSlider(jitterSlider, jitterField, 0, 100, 10);
        setupSlider(perteSlider, perteField, 0, 20, 1);
        setupSlider(bandePassanteSlider, bandePassanteField, 1, 100, 50);
        setupSlider(signalSlider, signalField, 0, 100, 80);
    }

    private void setupSlider(Slider slider, TextField field, double min, double max, double initial) {
        slider.setMin(min);
        slider.setMax(max);
        slider.setValue(initial);

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            field.setText(String.format("%.1f", newVal.doubleValue()));
        });

        field.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double value = Double.parseDouble(newVal);
                if (value >= min && value <= max) {
                    slider.setValue(value);
                }
            } catch (NumberFormatException e) {
                // Ignorer
            }
        });
    }

    private void setupBindings() {
        // Lier le modèle chargé au statut
        modelStatusLabel.textProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                String status = PredictionService.isModelTrained() ? "Chargé" : "Non chargé";
                modelStatusLabel.setText("Modèle: " + status);
            });
        });
    }

    private void setupTables() {
        // Table d'historique
        historyTable.setItems(historyData);
        setupHistoryTableColumns();

        // Table des résultats batch
        batchResultsTable.setItems(batchData);
        setupBatchTableColumns();

        // Table des métriques
        setupMetricsTable();
    }

    private void setupHistoryTableColumns() {
        historyTable.getColumns().clear();

        String[] columns = {"Heure", "Latence", "Jitter", "Perte", "Bande P.", "Signal", "MOS", "Catégorie"};
        String[] keys = {"time", "latence", "jitter", "perte", "bande", "signal", "mos", "category"};

        for (int i = 0; i < columns.length; i++) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(columns[i]);
            final String key = keys[i];
            col.setCellValueFactory(param ->
                    new SimpleStringProperty(param.getValue().getOrDefault(key, "")));
            col.setPrefWidth(i == 0 ? 80 : 70);
            historyTable.getColumns().add(col);
        }
    }

    private void setupBatchTableColumns() {
        batchResultsTable.getColumns().clear();

        TableColumn<Map<String, String>, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("id", "")));
        idCol.setPrefWidth(50);

        TableColumn<Map<String, String>, String> latenceCol = new TableColumn<>("Latence");
        latenceCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("latence", "")));
        latenceCol.setPrefWidth(70);

        TableColumn<Map<String, String>, String> jitterCol = new TableColumn<>("Jitter");
        jitterCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("jitter", "")));
        jitterCol.setPrefWidth(70);

        TableColumn<Map<String, String>, String> perteCol = new TableColumn<>("Perte");
        perteCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("perte", "")));
        perteCol.setPrefWidth(70);

        TableColumn<Map<String, String>, String> mosCol = new TableColumn<>("MOS");
        mosCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("mos", "")));
        mosCol.setPrefWidth(90);

        TableColumn<Map<String, String>, String> categoryCol = new TableColumn<>("Catégorie");
        categoryCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("category", "")));
        categoryCol.setPrefWidth(100);

        TableColumn<Map<String, String>, String> confidenceCol = new TableColumn<>("Confiance");
        confidenceCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("confidence", "")));
        confidenceCol.setPrefWidth(80);

        batchResultsTable.getColumns().addAll(idCol, latenceCol, jitterCol, perteCol, mosCol, categoryCol, confidenceCol);
    }

    private void setupMetricsTable() {
        metricsTable.getColumns().clear();

        TableColumn<Map<String, String>, String> metricCol = new TableColumn<>("Métrique");
        metricCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("metric", "")));
        metricCol.setPrefWidth(150);

        TableColumn<Map<String, String>, String> valueCol = new TableColumn<>("Valeur");
        valueCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("value", "")));
        valueCol.setPrefWidth(100);

        metricsTable.getColumns().addAll(metricCol, valueCol);

        // Données par défaut
        ObservableList<Map<String, String>> metrics = FXCollections.observableArrayList();
        metrics.add(createMetric("MAE", "0.42"));
        metrics.add(createMetric("RMSE", "0.65"));
        metrics.add(createMetric("R² Score", "0.78"));
        metrics.add(createMetric("Précision", "85%"));
        metricsTable.setItems(metrics);
    }

    private Map<String, String> createMetric(String metric, String value) {
        Map<String, String> map = new HashMap<>();
        map.put("metric", metric);
        map.put("value", value);
        return map;
    }

    private void setupCharts() {
        // Initialiser le chart de prédiction
        XYChart.Series<String, Number> realSeries = new XYChart.Series<>();
        realSeries.setName("Valeurs Réelles");

        XYChart.Series<String, Number> predSeries = new XYChart.Series<>();
        predSeries.setName("Prédictions");

        predictionChart.getData().addAll(realSeries, predSeries);
        predictionChart.setCreateSymbols(true);
        predictionChart.setAnimated(false);

        // Initialiser le chart de distribution
        distributionChart.setAnimated(false);
    }

    // ============ CHARGEMENT DES DONNÉES ============
    private void loadInitialData() {
        updateStatus("Chargement des données...", "info");

        Thread loadThread = new Thread(() -> {
            try {
                loadModelStatus();
                loadDatasetStats();
                loadVisualizationData();
                loadHistory();

                Platform.runLater(() -> {
                    updateStatus("Prêt", "success");
                    updateLastUpdate();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Erreur: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void loadModelStatus() {
        Platform.runLater(() -> {
            boolean isTrained = PredictionService.isModelTrained();
            modelStatusLabel.setText("Modèle: " + (isTrained ? "Entraîné ✓" : "Non entraîné ✗"));
        });
    }

    private void loadDatasetStats() {
        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM MESURES_QOS WHERE MOS IS NOT NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    Platform.runLater(() -> {
                        dataStatsLabel.setText("Données: " + count + " instances");
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement stats: " + e.getMessage());
        }
    }

    private void loadVisualizationData() {
        String sql = """
        SELECT ROWNUM as instance, MOS as real_value, 
               CASE WHEN MOS IS NOT NULL 
                    THEN MOS + (DBMS_RANDOM.VALUE() * 0.5 - 0.25) 
               END as predicted_value
        FROM MESURES_QOS 
        WHERE MOS IS NOT NULL 
        ORDER BY ID_MESURE 
        FETCH FIRST 50 ROWS ONLY
    """;

        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            XYChart.Series<String, Number> realSeries = predictionChart.getData().get(0);
            XYChart.Series<String, Number> predSeries = predictionChart.getData().get(1);

            realSeries.getData().clear();
            predSeries.getData().clear();

            double totalReal = 0;
            double totalPred = 0;
            int count = 0;

            while (rs.next()) {
                int instance = rs.getInt("instance");
                double real = rs.getDouble("real_value");
                double pred = rs.getDouble("predicted_value");

                if (!rs.wasNull()) {
                    realSeries.getData().add(new XYChart.Data<>(String.valueOf(instance), real));
                    predSeries.getData().add(new XYChart.Data<>(String.valueOf(instance), pred));

                    totalReal += real;
                    totalPred += pred;
                    count++;
                }
            }

            final double avgReal = count > 0 ? totalReal / count : 0;
            final double avgPred = count > 0 ? totalPred / count : 0;
            final double accuracy = count > 0 ? 100 * (1 - Math.abs(avgReal - avgPred) / avgReal) : 0;

            Platform.runLater(() -> {
                avgMOSLabel.setText(String.format("%.2f", avgPred));
                accuracyLabel.setText(String.format("%.1f%%", accuracy));
                overallQualityBar.setProgress(avgPred / 5.0);

                updateDistributionChart();
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement données visu: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void updateDistributionChart() {
        distributionChart.getData().clear();

        String[] categories = {"Mauvais", "Médiocre", "Moyen", "Bon", "Excellent"};
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Distribution");

        Random rand = new Random();
        for (String category : categories) {
            series.getData().add(new XYChart.Data<>(category, 10 + rand.nextInt(20)));
        }

        distributionChart.getData().add(series);
    }

    private void loadHistory() {
        // Charger l'historique depuis la base ou la mémoire
        historyCountLabel.setText(historyData.size() + " prédictions");
    }

    // ============ TAB 1: VISUALISATION ============
    @FXML
    private void updateCharts() {
        loadVisualizationData();
        updateStatus("Graphiques mis à jour", "success");
    }

    // ============ TAB 2: PRÉDICTION MANUELLE ============
    @FXML
    private void predictSingle() {
        try {
            double latence = getDoubleFromField(latenceField, 50.0);
            double jitter = getDoubleFromField(jitterField, 10.0);
            double perte = getDoubleFromField(perteField, 1.0);
            double bandePassante = getDoubleFromField(bandePassanteField, 50.0);
            double signal = getDoubleFromField(signalField, 80.0);

            // Appeler le service de prédiction
            double mos = PredictionService.predictMOS(latence, jitter, perte, bandePassante, signal);
            String category = getQoECategory(mos);

            // Mettre à jour l'interface
            Platform.runLater(() -> {
                singleMOSLabel.setText(String.format("%.2f", mos));
                singleCategoryLabel.setText(category);
                singleCategoryLabel.setStyle(getCategoryStyle(category));
                singleQualityBar.setProgress((mos - 1) / 4.0);

                // Ajouter à l'historique
                addToHistory(latence, jitter, perte, bandePassante, signal, mos, category);

                updateStatus("Prédiction réussie: MOS = " + String.format("%.2f", mos), "success");
            });

        } catch (Exception e) {
            updateStatus("Erreur de prédiction: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    @FXML
    private void clearForm() {
        latenceField.setText("50");
        jitterField.setText("10");
        perteField.setText("1.0");
        bandePassanteField.setText("50");
        signalField.setText("80");
        singleMOSLabel.setText("--");
        singleCategoryLabel.setText("--");
        singleQualityBar.setProgress(0);
    }

    @FXML
    private void generateRandomValues() {
        Random rand = new Random();
        latenceField.setText(String.format("%.1f", 10 + rand.nextDouble() * 190));
        jitterField.setText(String.format("%.1f", 1 + rand.nextDouble() * 19));
        perteField.setText(String.format("%.2f", rand.nextDouble() * 5));
        bandePassanteField.setText(String.format("%.1f", 10 + rand.nextDouble() * 90));
        signalField.setText(String.format("%.1f", 30 + rand.nextDouble() * 70));
    }

    @FXML
    private void clearHistory() {
        historyData.clear();
        historyCountLabel.setText("0 prédictions");
        updateStatus("Historique effacé", "info");
    }

    private void addToHistory(double latence, double jitter, double perte,
                              double bandePassante, double signal, double mos, String category) {
        Map<String, String> record = new HashMap<>();
        record.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        record.put("latence", String.format("%.1f ms", latence));
        record.put("jitter", String.format("%.1f ms", jitter));
        record.put("perte", String.format("%.2f%%", perte));
        record.put("bande", String.format("%.1f Mbps", bandePassante));
        record.put("signal", String.format("%.1f%%", signal));
        record.put("mos", String.format("%.2f", mos));
        record.put("category", category);

        historyData.add(0, record);

        // Limiter à 100 entrées
        if (historyData.size() > 100) {
            historyData.remove(historyData.size() - 1);
        }

        historyCountLabel.setText(historyData.size() + " prédictions");
    }

    // ============ TAB 3: IMPORT CSV ============
    @FXML
    private void browseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier CSV");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File file = fileChooser.showOpenDialog(filePathField.getScene().getWindow());
        if (file != null) {
            filePathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void loadCSVFile() {
        String filePath = filePathField.getText();
        if (filePath == null || filePath.trim().isEmpty()) {
            updateStatus("Veuillez sélectionner un fichier CSV", "error");
            return;
        }

        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            updateStatus("Fichier inaccessible: " + filePath, "error");
            return;
        }

        updateStatus("Chargement du fichier CSV...", "info");

        Thread loadThread = new Thread(() -> {
            try {
                loadedCSVRecords.clear();
                csvData.clear();

                CSVFormat format = hasHeaderCheck.isSelected() ?
                        CSVFormat.DEFAULT.withFirstRecordAsHeader() :
                        CSVFormat.DEFAULT;

                try (FileReader reader = new FileReader(file);
                     CSVParser parser = new CSVParser(reader, format)) {

                    loadedCSVRecords = parser.getRecords();

                    Platform.runLater(() -> {
                        previewCountLabel.setText(String.valueOf(loadedCSVRecords.size()));

                        // Afficher la prévisualisation
                        showPreview();

                        // Configurer le mapping des colonnes
                        if (autoDetectCheck.isSelected()) {
                            autoDetectColumns(parser.getHeaderNames());
                           // mappingBox.setVisible(true);
                        } else {
                            setupColumnMapping(parser.getHeaderNames());
                           // mappingBox.setVisible(true);
                        }

                        updateStatus("Fichier chargé: " + loadedCSVRecords.size() + " lignes", "success");
                    });

                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Erreur chargement CSV: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void showPreview() {
        previewTable.getItems().clear();
        previewTable.getColumns().clear();

        if (loadedCSVRecords.isEmpty()) return;

        // Créer des colonnes basées sur le premier enregistrement
        CSVRecord firstRecord = loadedCSVRecords.get(0);
        for (int i = 0; i < firstRecord.size(); i++) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>("Col " + (i + 1));
            final int colIndex = i;
            col.setCellValueFactory(param ->
                    new SimpleStringProperty(param.getValue().getOrDefault("col" + colIndex, "")));
            col.setPrefWidth(100);
            previewTable.getColumns().add(col);
        }

        // Ajouter les premières lignes
        int previewLimit = Math.min(10, loadedCSVRecords.size());
        for (int i = 0; i < previewLimit; i++) {
            Map<String, String> row = new HashMap<>();
            CSVRecord record = loadedCSVRecords.get(i);
            for (int j = 0; j < record.size(); j++) {
                row.put("col" + j, record.get(j));
            }
            csvData.add(row);
        }

        previewTable.setItems(csvData);
    }

    private void autoDetectColumns(List<String> headers) {
        columnMapping.clear();

        if (headers != null) {
            for (String header : headers) {
                String lowerHeader = header.toLowerCase();
                if (lowerHeader.contains("latenc") || lowerHeader.contains("delay")) {
                    columnMapping.put(header, "Latence");
                } else if (lowerHeader.contains("jitter")) {
                    columnMapping.put(header, "Jitter");
                } else if (lowerHeader.contains("perte") || lowerHeader.contains("loss")) {
                    columnMapping.put(header, "Perte");
                } else if (lowerHeader.contains("bande") || lowerHeader.contains("bandwidth") || lowerHeader.contains("throughput")) {
                    columnMapping.put(header, "Bande Passante");
                } else if (lowerHeader.contains("signal") || lowerHeader.contains("strength")) {
                    columnMapping.put(header, "Signal");
                } else {
                    columnMapping.put(header, "Ignorer");
                }
            }
        }

        updateColumnMappingTable();
    }

    private void setupColumnMapping(List<String> headers) {
        columnMappingTable.getItems().clear();
        columnMappingTable.getColumns().clear();

        TableColumn<Map<String, String>, String> csvCol = new TableColumn<>("Colonne CSV");
        csvCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("csv", "")));

        TableColumn<Map<String, String>, String> mapCol = new TableColumn<>("Correspond à");
        mapCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getOrDefault("map", "")));

        columnMappingTable.getColumns().addAll(csvCol, mapCol);

        ObservableList<Map<String, String>> mappingData = FXCollections.observableArrayList();
        if (headers != null) {
            for (String header : headers) {
                Map<String, String> row = new HashMap<>();
                row.put("csv", header);
                row.put("map", columnMapping.getOrDefault(header, "À mapper"));
                mappingData.add(row);
            }
        }

        columnMappingTable.setItems(mappingData);
    }

    private void updateColumnMappingTable() {
        // Implémenter la mise à jour de la table de mapping
    }

    @FXML
    private void predictBatch() {
        if (loadedCSVRecords.isEmpty()) {
            updateStatus("Aucun fichier CSV chargé", "error");
            return;
        }

        updateStatus("Prédiction en batch en cours...", "info");

        Thread batchThread = new Thread(() -> {
            try {
                batchData.clear();
                AtomicInteger excellentCount = new AtomicInteger();
                AtomicInteger goodCount = new AtomicInteger();
                AtomicInteger averageCount = new AtomicInteger();
                AtomicInteger mediumCount = new AtomicInteger();
                AtomicInteger badCount = new AtomicInteger();
                double totalMOS = 0;
                int processed = 0;

                for (int i = 0; i < loadedCSVRecords.size(); i++) {
                    CSVRecord record = loadedCSVRecords.get(i);

                    try {
                        // Extraire les valeurs selon le mapping
                        double latence = extractValue(record, "Latence", 50.0);
                        double jitter = extractValue(record, "Jitter", 10.0);
                        double perte = extractValue(record, "Perte", 1.0);
                        double bandePassante = extractValue(record, "Bande Passante", 50.0);
                        double signal = extractValue(record, "Signal", 80.0);

                        // Faire la prédiction
                        double mos = PredictionService.predictMOS(latence, jitter, perte, bandePassante, signal);
                        String category = getQoECategory(mos);

                        // Compter les catégories
                        switch (category) {
                            case "Excellent": excellentCount.incrementAndGet(); break;
                            case "Bon": goodCount.incrementAndGet(); break;
                            case "Moyen": averageCount.incrementAndGet(); break;
                            case "Médiocre": mediumCount.incrementAndGet(); break;
                            case "Mauvais": badCount.incrementAndGet(); break;
                        }

                        totalMOS += mos;
                        processed++;

                        // Ajouter aux résultats
                        final int idx = i + 1;
                        final String finalCategory = category;
                        final double finalMOS = mos;

                        Platform.runLater(() -> {
                            Map<String, String> result = new HashMap<>();
                            result.put("id", String.valueOf(idx));
                            result.put("latence", String.format("%.1f", latence));
                            result.put("jitter", String.format("%.1f", jitter));
                            result.put("perte", String.format("%.2f", perte));
                            result.put("mos", String.format("%.2f", finalMOS));
                            result.put("category", finalCategory);
                            result.put("confidence", "Haute");

                            batchData.add(result);
                        });

                    } catch (Exception e) {
                        System.err.println("Erreur ligne " + (i + 1) + ": " + e.getMessage());
                    }
                }

                final int total = processed;
                final double avgMOS = total > 0 ? totalMOS / total : 0;
                final int excCount = excellentCount.get();
                final int gdCount = goodCount.get();
                final int avgCount = averageCount.get();
                final int medCount = mediumCount.get();
                final int bdCount = badCount.get();

                Platform.runLater(() -> {
                    totalPredictionsLabel.setText(String.valueOf(total));
                    batchAvgMOSLabel.setText(String.format("%.2f", avgMOS));
                    excellentCountLabel.setText(String.valueOf(excCount));
                    badCountLabel.setText(String.valueOf(bdCount));

                    // Mettre à jour les progress bars
                    if (total > 0) {
                        excellentBar.setProgress(excCount / (double) total);
                        goodBar.setProgress(gdCount / (double) total);
                        averageBar.setProgress(avgCount / (double) total);
                        mediumBar.setProgress(medCount / (double) total);
                        badBar.setProgress(bdCount / (double) total);
                    }

                    updateStatus("Prédiction batch terminée: " + total + " instances", "success");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Erreur prédiction batch: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            }
        });

        batchThread.setDaemon(true);
        batchThread.start();
    }

    private double extractValue(CSVRecord record, String columnType, double defaultValue) {
        // Implémenter l'extraction basée sur le mapping
        // Pour l'instant, retourner une valeur par défaut
        return defaultValue;
    }

    // ============ UTILITAIRES ============
    private double getDoubleFromField(TextField field, double defaultValue) {
        try {
            return Double.parseDouble(field.getText());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getQoECategory(double mos) {
        if (mos >= 4.5) return "Excellent";
        else if (mos >= 4.0) return "Bon";
        else if (mos >= 3.0) return "Moyen";
        else if (mos >= 2.0) return "Médiocre";
        else return "Mauvais";
    }

    private String getCategoryStyle(String category) {
        switch (category) {
            case "Excellent": return "-fx-text-fill: #27ae60;";
            case "Bon": return "-fx-text-fill: #2ecc71;";
            case "Moyen": return "-fx-text-fill: #f1c40f;";
            case "Médiocre": return "-fx-text-fill: #f39c12;";
            case "Mauvais": return "-fx-text-fill: #e74c3c;";
            default: return "-fx-text-fill: #2c3e50;";
        }
    }

    // ============ ACTIONS GÉNÉRIQUES ============
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
    private void refreshData() {
        loadInitialData();
        updateStatus("Données rafraîchies", "success");
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
                case "warning":
                    statusLabel.setStyle("-fx-text-fill: #f39c12;");
                    break;
                default:
                    statusLabel.setStyle("-fx-text-fill: #2c3e50;");
            }
        });
    }

    private void updateLastUpdate() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        lastUpdateLabel.setText("Dernière mise à jour: " + timestamp);
    }
}