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
    @FXML private Label trancheLabel;
    @FXML private Label villeLabel;
    @FXML private Label paysLabel;
    @FXML private Label countLabel;

    @FXML private LineChart<String, Number> qosLineChart;
    @FXML private BarChart<String, Number> latencyChart;

    @FXML private ComboBox<String> zoneCombo;
    @FXML private TextField searchField;

    @FXML private TableView<NetworkDevice> devicesTable;
    @FXML private TableColumn<NetworkDevice, String> deviceNameColumn;
    @FXML private TableColumn<NetworkDevice, String> ipAddressColumn;
    @FXML private TableColumn<NetworkDevice, String> macAddressColumn;
    @FXML private TableColumn<NetworkDevice, String> statusColumn;
    @FXML private TableColumn<NetworkDevice, String> bandwidthColumn;
    @FXML private TableColumn<NetworkDevice, String> latencyColumn;
    @FXML private TableColumn<NetworkDevice, String> uptimeColumn;
    @FXML private TableColumn<NetworkDevice, Void> actionsColumn;

    private ObservableList<NetworkDevice> devicesList;
    private Timeline updateTimeline;
    private final QosZoneService zoneService = new QosZoneService();
    private List<String> zones = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupZoneCombo();
        loadDevicesData();
        setupCharts();
        setupSearch();
        startAutoRefresh();
        updateNetworkStatus();
    }

    private void setupTableColumns() {
        deviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        ipAddressColumn.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        macAddressColumn.setCellValueFactory(new PropertyValueFactory<>("macAddress"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        bandwidthColumn.setCellValueFactory(new PropertyValueFactory<>("bandwidth"));
        latencyColumn.setCellValueFactory(new PropertyValueFactory<>("latency"));
        uptimeColumn.setCellValueFactory(new PropertyValueFactory<>("uptime"));

        statusColumn.setCellFactory(column -> new TableCell<NetworkDevice, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.equals("Active")) {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    } else if (status.equals("Inactive")) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    }
                }
            }
        });
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

                latenceLabel.setText(String.format("%.2f ms", latest.getLatence()));
                jitterLabel.setText(String.format("%.2f ms", latest.getJitter()));
                perteLabel.setText(String.format("%.2f %%", latest.getPerte()));
                bandePassanteLabel.setText(String.format("%.2f Mbps", latest.getBandePassante()));
                signalLabel.setText(String.format("%.2f", latest.getSignalScore()));
                mosLabel.setText(String.format("%.2f / 5", latest.getMos()));
                trancheLabel.setText(latest.getTranche12h());

                String[] zoneParts = latest.getZone().split(",");
                villeLabel.setText(zoneParts.length > 0 ? zoneParts[0].trim() : "");
                paysLabel.setText(zoneParts.length > 1 ? zoneParts[1].trim() : "");
                countLabel.setText(qosList.size() + " measurements");

                updateQoSChart(qosList);
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

    private void loadDevicesData() {
        devicesList = FXCollections.observableArrayList(
                new NetworkDevice("Router-Main", "192.168.1.1", "00:1A:2B:3C:4D:5E", "Active", "125 Mbps", "2ms", "15d 6h"),
                new NetworkDevice("Switch-Floor1", "192.168.1.10", "00:1A:2B:3C:4D:5F", "Active", "89 Mbps", "1ms", "10d 2h"),
                new NetworkDevice("AP-Office", "192.168.1.20", "00:1A:2B:3C:4D:60", "Active", "234 Mbps", "5ms", "8d 14h")
        );

        devicesTable.setItems(devicesList);
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

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterDevices(newValue);
            });
        }
    }

    private void filterDevices(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            devicesTable.setItems(devicesList);
        } else {
            ObservableList<NetworkDevice> filtered = FXCollections.observableArrayList();
            for (NetworkDevice device : devicesList) {
                if (device.getDeviceName().toLowerCase().contains(searchText.toLowerCase()) ||
                        device.getIpAddress().contains(searchText) ||
                        device.getMacAddress().toLowerCase().contains(searchText.toLowerCase())) {
                    filtered.add(device);
                }
            }
            devicesTable.setItems(filtered);
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

    public static class NetworkDevice {
        private String deviceName;
        private String ipAddress;
        private String macAddress;
        private String status;
        private String bandwidth;
        private String latency;
        private String uptime;

        public NetworkDevice(String deviceName, String ipAddress, String macAddress,
                             String status, String bandwidth, String latency, String uptime) {
            this.deviceName = deviceName;
            this.ipAddress = ipAddress;
            this.macAddress = macAddress;
            this.status = status;
            this.bandwidth = bandwidth;
            this.latency = latency;
            this.uptime = uptime;
        }

        public String getDeviceName() { return deviceName; }
        public String getIpAddress() { return ipAddress; }
        public String getMacAddress() { return macAddress; }
        public String getStatus() { return status; }
        public String getBandwidth() { return bandwidth; }
        public String getLatency() { return latency; }
        public String getUptime() { return uptime; }
    }
}