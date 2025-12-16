package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.Qos;
import com.ensah.qoe.Services.QosZoneService;
import com.ensah.qoe.Services.QosAnalyzer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportsController implements Initializable {

    @FXML private Label totalReportsLabel;
    @FXML private Label dataProcessedLabel;
    @FXML private Label avgMOSLabel;
    @FXML private Label avgLatencyLabel;

    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private ComboBox<String> zoneCombo;
    @FXML private ComboBox<String> formatCombo;

    @FXML private LineChart<String, Number> mosTrendChart;
    @FXML private BarChart<String, Number> qualityDistributionChart;
    @FXML private BarChart<String, Number> zoneComparisonChart;

    @FXML private TableView<QosData> qosDataTable;
    @FXML private TableColumn<QosData, String> zoneColumn;
    @FXML private TableColumn<QosData, Double> latencyColumn;
    @FXML private TableColumn<QosData, Double> jitterColumn;
    @FXML private TableColumn<QosData, Double> packetLossColumn;
    @FXML private TableColumn<QosData, Double> bandwidthColumn;
    @FXML private TableColumn<QosData, Double> mosColumn;
    @FXML private TableColumn<QosData, String> timeColumn;

    private ObservableList<QosData> qosDataList;
    private final QosZoneService zoneService = new QosZoneService();
    private List<Qos> allQosData = new ArrayList<>();
    private Map<String, List<Qos>> zoneQosMap = new HashMap<>();
    private List<GeneratedReport> generatedReports = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadQoSData();
        setupComboBoxes();
        setupTable();
        setupCharts();
        updateStatistics();
    }

    private void loadQoSData() {
        try {
            List<String> zones = zoneService.getZones();
            allQosData.clear();
            zoneQosMap.clear();

            for (String zone : zones) {
                List<Qos> zoneData = QosZoneService.getQosByZone(zone);
                zoneQosMap.put(zone, zoneData);
                allQosData.addAll(zoneData);
            }

            System.out.println("✅ Chargé " + allQosData.size() + " enregistrements QoS pour reporting");

            // Convertir pour le tableau
            convertToTableData();

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement données QoS: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les données QoS: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void convertToTableData() {
        qosDataList = FXCollections.observableArrayList();

        for (Qos qos : allQosData) {
            QosData data = new QosData(
                    qos.getZone(),
                    qos.getLatence(),
                    qos.getJitter(),
                    qos.getPerte(),
                    qos.getBandePassante(),
                    qos.getMos(),
                    qos.getTranche12h()
            );
            qosDataList.add(data);
        }

        if (qosDataTable != null) {
            qosDataTable.setItems(qosDataList);
        }
    }

    private void setupComboBoxes() {
        // Types de rapports
        reportTypeCombo.getItems().addAll(
                "Rapport QoS Complet",
                "Analyse de Performance",
                "Comparaison des Zones",
                "Tendances Temporelles",
                "Rapport de Qualité",
                "Statistiques Détaillées"
        );

        // Zones disponibles
        List<String> zones = zoneService.getZones();
        zoneCombo.getItems().addAll(zones);
        zoneCombo.getItems().add(0, "Toutes les zones");

        // Formats d'export
        formatCombo.getItems().addAll( "CSV", "HTML", "TXT");

        reportTypeCombo.getSelectionModel().selectFirst();
        zoneCombo.getSelectionModel().selectFirst();
        formatCombo.getSelectionModel().select("CSV");
    }

    private void setupTable() {
        zoneColumn.setCellValueFactory(new PropertyValueFactory<>("zone"));
        latencyColumn.setCellValueFactory(new PropertyValueFactory<>("latency"));
        jitterColumn.setCellValueFactory(new PropertyValueFactory<>("jitter"));
        packetLossColumn.setCellValueFactory(new PropertyValueFactory<>("packetLoss"));
        bandwidthColumn.setCellValueFactory(new PropertyValueFactory<>("bandwidth"));
        mosColumn.setCellValueFactory(new PropertyValueFactory<>("mos"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));

        // Colorer les cellules MOS
        mosColumn.setCellFactory(column -> new TableCell<QosData, Double>() {
            @Override
            protected void updateItem(Double mos, boolean empty) {
                super.updateItem(mos, empty);
                if (empty || mos == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f", mos));
                    if (mos >= 4.0) {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-background-color: rgba(16, 185, 129, 0.1);");
                    } else if (mos >= 3.0) {
                        setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-background-color: rgba(59, 130, 246, 0.1);");
                    } else if (mos >= 2.0) {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-background-color: rgba(245, 158, 11, 0.1);");
                    } else {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-background-color: rgba(239, 68, 68, 0.1);");
                    }
                }
            }
        });
    }

    private void setupCharts() {
        setupMOSTrendChart();
        setupQualityDistributionChart();
        setupZoneComparisonChart();
    }

    private void setupMOSTrendChart() {
        mosTrendChart.getData().clear();

        if (allQosData.isEmpty()) return;

        // Grouper par intervalle de temps
        Map<String, List<Double>> timeGroups = new TreeMap<>();
        for (Qos qos : allQosData) {
            String time = qos.getTranche12h();
            timeGroups.computeIfAbsent(time, k -> new ArrayList<>()).add(qos.getMos());
        }

        XYChart.Series<String, Number> mosSeries = new XYChart.Series<>();
        mosSeries.setName("MOS Moyen");

        for (Map.Entry<String, List<Double>> entry : timeGroups.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            mosSeries.getData().add(new XYChart.Data<>(entry.getKey(), avg));
        }

        mosTrendChart.getData().add(mosSeries);
    }

    private void setupQualityDistributionChart() {
        qualityDistributionChart.getData().clear();

        if (allQosData.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Distribution");

        int excellent = 0, good = 0, fair = 0, poor = 0;
        for (Qos qos : allQosData) {
            double mos = qos.getMos();
            if (mos >= 4.0) excellent++;
            else if (mos >= 3.0) good++;
            else if (mos >= 2.0) fair++;
            else poor++;
        }

        series.getData().add(new XYChart.Data<>("Excellent\n(MOS ≥ 4)", excellent));
        series.getData().add(new XYChart.Data<>("Bon\n(MOS 3-4)", good));
        series.getData().add(new XYChart.Data<>("Moyen\n(MOS 2-3)", fair));
        series.getData().add(new XYChart.Data<>("Mauvais\n(MOS < 2)", poor));

        qualityDistributionChart.getData().add(series);
    }

    private void setupZoneComparisonChart() {
        zoneComparisonChart.getData().clear();

        if (zoneQosMap.isEmpty()) return;

        XYChart.Series<String, Number> latencySeries = new XYChart.Series<>();
        latencySeries.setName("Latence Moyenne (ms)");

        XYChart.Series<String, Number> mosSeries = new XYChart.Series<>();
        mosSeries.setName("MOS Moyen");

        for (Map.Entry<String, List<Qos>> entry : zoneQosMap.entrySet()) {
            String zone = entry.getKey();
            List<Qos> zoneData = entry.getValue();

            if (!zoneData.isEmpty()) {
                double avgLatency = zoneData.stream().mapToDouble(Qos::getLatence).average().orElse(0);
                double avgMOS = zoneData.stream().mapToDouble(Qos::getMos).average().orElse(0);

                // Tronquer le nom de zone si trop long
                String displayZone = zone.length() > 15 ? zone.substring(0, 15) + "..." : zone;

                latencySeries.getData().add(new XYChart.Data<>(displayZone, avgLatency));
                mosSeries.getData().add(new XYChart.Data<>(displayZone, avgMOS));
            }
        }

        zoneComparisonChart.getData().addAll(latencySeries, mosSeries);
    }

    private void updateStatistics() {
        if (allQosData.isEmpty()) {
            totalReportsLabel.setText("0");
            dataProcessedLabel.setText("0 données");
            avgMOSLabel.setText("0.00");
            avgLatencyLabel.setText("0 ms");
            return;
        }

        // Nombre de rapports générés
        totalReportsLabel.setText(String.valueOf(generatedReports.size()));

        // Données traitées
        dataProcessedLabel.setText(allQosData.size() + " mesures");

        // Calcul des moyennes
        double avgMOS = allQosData.stream().mapToDouble(Qos::getMos).average().orElse(0);
        double avgLatency = allQosData.stream().mapToDouble(Qos::getLatence).average().orElse(0);

        avgMOSLabel.setText(String.format("%.2f", avgMOS));
        avgLatencyLabel.setText(String.format("%.1f ms", avgLatency));
    }

    @FXML
    private void generateReport() {
        String reportType = reportTypeCombo.getValue();
        String zone = zoneCombo.getValue();
        String format = formatCombo.getValue();

        if (allQosData.isEmpty()) {
            showAlert("Données manquantes", "Aucune donnée QoS disponible pour générer un rapport.", Alert.AlertType.WARNING);
            return;
        }

        ProgressDialog progress = new ProgressDialog();
        progress.show();

        Task<Void> reportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0.1, 1.0);

                try {
                    // Filtrer les données par zone si nécessaire
                    List<Qos> reportData = allQosData;
                    if (!"Toutes les zones".equals(zone) && zone != null) {
                        reportData = zoneQosMap.getOrDefault(zone, new ArrayList<>());
                    }

                    updateProgress(0.3, 1.0);

                    // Générer le rapport selon le format
                    String fileName = generateReportFile(reportType, zone, format, reportData);

                    updateProgress(0.9, 1.0);

                    // Ajouter au historique
                    GeneratedReport report = new GeneratedReport(
                            reportType + " - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                            reportType,
                            zone,
                            format,
                            fileName
                    );
                    generatedReports.add(report);

                    updateProgress(1.0, 1.0);

                    javafx.application.Platform.runLater(() -> {
                        progress.close();
                        updateStatistics();
                        showAlert("Rapport généré",
                                "Rapport '" + reportType + "' généré avec succès!\n" +
                                        "Fichier: " + fileName,
                                Alert.AlertType.INFORMATION);
                    });

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        progress.close();
                        showAlert("Erreur", "Échec génération rapport: " + e.getMessage(), Alert.AlertType.ERROR);
                    });
                    throw e;
                }

                return null;
            }
        };

        new Thread(reportTask).start();
    }

    private String generateReportFile(String reportType, String zone, String format, List<Qos> data) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "Rapport_QoS_" + reportType.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp;
        String downloadsDir = System.getProperty("user.home") + "/Downloads/";

        switch (format.toUpperCase()) {
            case "CSV":
                return generateCSVReport(downloadsDir + fileName + ".csv", data);
            case "HTML":
                return generateHTMLReport(downloadsDir + fileName + ".html", reportType, zone, data);
            case "TXT":
                return generateTextReport(downloadsDir + fileName + ".txt", reportType, zone, data);
            default:
                return generateTextReport(downloadsDir + fileName + ".txt", reportType, zone, data);
        }
    }

    private String generateTextReport(String filePath, String reportType, String zone, List<Qos> data) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("=".repeat(80));
            writer.println("RAPPORT QOS/QOE - " + reportType.toUpperCase());
            writer.println("=".repeat(80));
            writer.println();
            writer.println("Date de génération: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            writer.println("Zone: " + (zone == null ? "Toutes" : zone));
            writer.println("Nombre d'enregistrements: " + data.size());
            writer.println();

            writer.println("-".repeat(80));
            writer.println("STATISTIQUES GLOBALES");
            writer.println("-".repeat(80));

            if (!data.isEmpty()) {
                double avgMOS = data.stream().mapToDouble(Qos::getMos).average().orElse(0);
                double avgLatency = data.stream().mapToDouble(Qos::getLatence).average().orElse(0);
                double avgJitter = data.stream().mapToDouble(Qos::getJitter).average().orElse(0);
                double avgPacketLoss = data.stream().mapToDouble(Qos::getPerte).average().orElse(0);
                double avgBandwidth = data.stream().mapToDouble(Qos::getBandePassante).average().orElse(0);

                writer.printf("MOS moyen: %.2f / 5.0\n", avgMOS);
                writer.printf("Latence moyenne: %.2f ms\n", avgLatency);
                writer.printf("Jitter moyen: %.2f ms\n", avgJitter);
                writer.printf("Perte moyenne: %.2f %%\n", avgPacketLoss);
                writer.printf("Bande passante moyenne: %.2f Mbps\n", avgBandwidth);
                writer.println();

                // Distribution qualité
                int excellent = 0, good = 0, fair = 0, poor = 0;
                for (Qos qos : data) {
                    double mos = qos.getMos();
                    if (mos >= 4.0) excellent++;
                    else if (mos >= 3.0) good++;
                    else if (mos >= 2.0) fair++;
                    else poor++;
                }

                writer.println("DISTRIBUTION DE QUALITÉ:");
                writer.printf("  Excellent (MOS ≥ 4.0): %d (%.1f%%)\n", excellent, (excellent * 100.0 / data.size()));
                writer.printf("  Bon (3.0 ≤ MOS < 4.0): %d (%.1f%%)\n", good, (good * 100.0 / data.size()));
                writer.printf("  Moyen (2.0 ≤ MOS < 3.0): %d (%.1f%%)\n", fair, (fair * 100.0 / data.size()));
                writer.printf("  Mauvais (MOS < 2.0): %d (%.1f%%)\n", poor, (poor * 100.0 / data.size()));
            }

            writer.println();
            writer.println("-".repeat(80));
            writer.println("DONNÉES DÉTAILLÉES (10 premiers enregistrements)");
            writer.println("-".repeat(80));

            int limit = Math.min(data.size(), 10);
            for (int i = 0; i < limit; i++) {
                Qos qos = data.get(i);
                writer.printf("Zone: %s | MOS: %.2f | Latence: %.2f ms | Bande passante: %.2f Mbps\n",
                        qos.getZone(), qos.getMos(), qos.getLatence(), qos.getBandePassante());
            }

            if (data.size() > 10) {
                writer.println("... (" + (data.size() - 10) + " enregistrements supplémentaires)");
            }
        }

        return filePath;
    }

    private String generateCSVReport(String filePath, List<Qos> data) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Zone,Latence (ms),Jitter (ms),Perte (%),BandePassante (Mbps),MOS,Score Signal,DateHeure");

            for (Qos qos : data) {
                writer.printf("%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                        qos.getZone(),
                        qos.getLatence(),
                        qos.getJitter(),
                        qos.getPerte(),
                        qos.getBandePassante(),
                        qos.getMos(),
                        qos.getSignalScore(),
                        qos.getTranche12h()
                );
            }
        }

        return filePath;
    }

    private String generateHTMLReport(String filePath, String reportType, String zone, List<Qos> data) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang='fr'>");
            writer.println("<head>");
            writer.println("    <meta charset='UTF-8'>");
            writer.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            writer.println("    <title>Rapport QoS/QoE</title>");
            writer.println("    <style>");
            writer.println("        body { font-family: Arial, sans-serif; margin: 40px; background-color: #f8fafc; }");
            writer.println("        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 30px; }");
            writer.println("        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }");
            writer.println("        .stat-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
            writer.println("        .stat-value { font-size: 2em; font-weight: bold; margin: 10px 0; }");
            writer.println("        .stat-label { color: #64748b; font-size: 0.9em; }");
            writer.println("        table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
            writer.println("        th { background-color: #3b82f6; color: white; padding: 12px; text-align: left; }");
            writer.println("        td { padding: 10px; border-bottom: 1px solid #e2e8f0; }");
            writer.println("        tr:nth-child(even) { background-color: #f8fafc; }");
            writer.println("        .excellent { background-color: #d1fae5 !important; }");
            writer.println("        .good { background-color: #dbeafe !important; }");
            writer.println("        .fair { background-color: #fef3c7 !important; }");
            writer.println("        .poor { background-color: #fee2e2 !important; }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");

            writer.println("    <div class='header'>");
            writer.println("        <h1>📊 Rapport QoS/QoE</h1>");
            writer.println("        <h2>" + reportType + "</h2>");
            writer.println("        <p>Zone: " + (zone == null ? "Toutes" : zone) + " | Généré le: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "</p>");
            writer.println("    </div>");

            if (!data.isEmpty()) {
                double avgMOS = data.stream().mapToDouble(Qos::getMos).average().orElse(0);
                double avgLatency = data.stream().mapToDouble(Qos::getLatence).average().orElse(0);
                double avgJitter = data.stream().mapToDouble(Qos::getJitter).average().orElse(0);
                double avgBandwidth = data.stream().mapToDouble(Qos::getBandePassante).average().orElse(0);

                writer.println("    <div class='stats-grid'>");
                writer.println("        <div class='stat-card'>");
                writer.println("            <div class='stat-label'>MOS Moyen</div>");
                writer.println("            <div class='stat-value'>" + String.format("%.2f", avgMOS) + " / 5</div>");
                writer.println("        </div>");
                writer.println("        <div class='stat-card'>");
                writer.println("            <div class='stat-label'>Latence Moyenne</div>");
                writer.println("            <div class='stat-value'>" + String.format("%.2f", avgLatency) + " ms</div>");
                writer.println("        </div>");
                writer.println("        <div class='stat-card'>");
                writer.println("            <div class='stat-label'>Jitter Moyen</div>");
                writer.println("            <div class='stat-value'>" + String.format("%.2f", avgJitter) + " ms</div>");
                writer.println("        </div>");
                writer.println("        <div class='stat-card'>");
                writer.println("            <div class='stat-label'>Bande Passante</div>");
                writer.println("            <div class='stat-value'>" + String.format("%.2f", avgBandwidth) + " Mbps</div>");
                writer.println("        </div>");
                writer.println("    </div>");

                writer.println("    <h3>Données Détaillées</h3>");
                writer.println("    <table>");
                writer.println("        <thead>");
                writer.println("            <tr>");
                writer.println("                <th>Zone</th>");
                writer.println("                <th>Latence (ms)</th>");
                writer.println("                <th>Jitter (ms)</th>");
                writer.println("                <th>Perte (%)</th>");
                writer.println("                <th>Bande Passante (Mbps)</th>");
                writer.println("                <th>MOS</th>");
                writer.println("                <th>Score Signal</th>");
                writer.println("                <th>Date/Heure</th>");
                writer.println("            </tr>");
                writer.println("        </thead>");
                writer.println("        <tbody>");

                for (Qos qos : data) {
                    String qualityClass = "";
                    if (qos.getMos() >= 4.0) qualityClass = "excellent";
                    else if (qos.getMos() >= 3.0) qualityClass = "good";
                    else if (qos.getMos() >= 2.0) qualityClass = "fair";
                    else qualityClass = "poor";

                    writer.println("            <tr class='" + qualityClass + "'>");
                    writer.printf("                <td>%s</td>\n", qos.getZone());
                    writer.printf("                <td>%.2f</td>\n", qos.getLatence());
                    writer.printf("                <td>%.2f</td>\n", qos.getJitter());
                    writer.printf("                <td>%.2f</td>\n", qos.getPerte());
                    writer.printf("                <td>%.2f</td>\n", qos.getBandePassante());
                    writer.printf("                <td>%.2f</td>\n", qos.getMos());
                    writer.printf("                <td>%.2f</td>\n", qos.getSignalScore());
                    writer.printf("                <td>%s</td>\n", qos.getTranche12h());
                    writer.println("            </tr>");
                }

                writer.println("        </tbody>");
                writer.println("    </table>");
            } else {
                writer.println("    <p style='color: #ef4444; padding: 20px; background: #fee2e2; border-radius: 8px;'>");
                writer.println("        Aucune donnée disponible pour générer le rapport.");
                writer.println("    </p>");
            }

            writer.println("</body>");
            writer.println("</html>");
        }

        return filePath;
    }


    @FXML
    private void refreshData() {
        loadQoSData();
        setupCharts();
        updateStatistics();
        showAlert("Rafraîchissement", "Données QoS rafraîchies avec succès!", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void exportData() {
        if (allQosData.isEmpty()) {
            showAlert("Données manquantes", "Aucune donnée à exporter.", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les données QoS");
        fileChooser.setInitialFileName("qos_data_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(qosDataTable.getScene().getWindow());
        if (file != null) {
            try {
                generateCSVReport(file.getAbsolutePath(), allQosData);
                showAlert("Export réussi", "Données exportées vers: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Erreur export", "Échec export: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Classes internes
    public static class QosData {
        private final String zone;
        private final double latency;
        private final double jitter;
        private final double packetLoss;
        private final double bandwidth;
        private final double mos;
        private final String time;

        public QosData(String zone, double latency, double jitter, double packetLoss,
                       double bandwidth, double mos, String time) {
            this.zone = zone;
            this.latency = latency;
            this.jitter = jitter;
            this.packetLoss = packetLoss;
            this.bandwidth = bandwidth;
            this.mos = mos;
            this.time = time;
        }

        public String getZone() { return zone; }
        public double getLatency() { return latency; }
        public double getJitter() { return jitter; }
        public double getPacketLoss() { return packetLoss; }
        public double getBandwidth() { return bandwidth; }
        public double getMos() { return mos; }
        public String getTime() { return time; }
    }

    public static class GeneratedReport {
        private final String name;
        private final String type;
        private final String zone;
        private final String format;
        private final String filePath;

        public GeneratedReport(String name, String type, String zone, String format, String filePath) {
            this.name = name;
            this.type = type;
            this.zone = zone;
            this.format = format;
            this.filePath = filePath;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getZone() { return zone; }
        public String getFormat() { return format; }
        public String getFilePath() { return filePath; }
    }

    // Dialog de progression
    private static class ProgressDialog extends Dialog<Void> {
        public ProgressDialog() {
            setTitle("Génération du rapport");
            setHeaderText("Veuillez patienter...");

            ProgressBar progressBar = new ProgressBar();
            progressBar.setPrefWidth(300);

            VBox content = new VBox(20, progressBar);
            content.setPadding(new javafx.geometry.Insets(20));
            getDialogPane().setContent(content);
            getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        }
    }
}