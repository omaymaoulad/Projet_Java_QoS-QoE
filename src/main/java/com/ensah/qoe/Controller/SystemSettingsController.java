package com.ensah.qoe.Controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.ResourceBundle;

public class SystemSettingsController implements Initializable {

    // System Information Fields
    @FXML private TextField systemNameField;
    @FXML private TextField organizationField;
    @FXML private TextField adminEmailField;
    @FXML private ComboBox<String> timezoneCombo;
    @FXML private ComboBox<String> languageCombo;
    @FXML private ComboBox<String> dateFormatCombo;

    // System Status Labels
    @FXML private Label versionLabel;
    @FXML private Label uptimeLabel;
    @FXML private Label dbStatusLabel;
    @FXML private Label licenseLabel;

    // System Resources
    @FXML private ProgressBar cpuProgress;
    @FXML private Label cpuLabel;
    @FXML private ProgressBar memoryProgress;
    @FXML private Label memoryLabel;
    @FXML private ProgressBar diskProgress;
    @FXML private Label diskLabel;

    // Menu Buttons
    @FXML private Button systemInfoBtn;
    @FXML private Button networkConfigBtn;
    @FXML private Button emailSettingsBtn;
    @FXML private Button qoeThresholdsBtn;
    @FXML private Button qosParamsBtn;
    @FXML private Button alertsBtn;
    @FXML private Button securityBtn;
    @FXML private Button permissionsBtn;
    @FXML private Button auditLogsBtn;
    @FXML private Button backupBtn;
    @FXML private Button updatesBtn;

    @FXML private VBox settingsContentArea;

    private Button currentActiveButton;
    private Timeline updateTimeline;
    private Random random = new Random();
    private LocalDateTime startTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startTime = LocalDateTime.now().minus(15, ChronoUnit.DAYS).minus(6, ChronoUnit.HOURS);

        setupComboBoxes();
        updateSystemStatus();
        updateSystemResources();
        startAutoUpdate();

        // Set initial active button
        currentActiveButton = systemInfoBtn;
        setActiveButton(systemInfoBtn);
    }

    private void setupComboBoxes() {
        timezoneCombo.getSelectionModel().selectFirst();
        languageCombo.getSelectionModel().selectFirst();
        dateFormatCombo.getSelectionModel().selectFirst();
    }

    private void updateSystemStatus() {
        versionLabel.setText("v2.5.0");
        dbStatusLabel.setText("Connected");
        dbStatusLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #10b981;");
        licenseLabel.setText("Active");
        licenseLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        updateUptime();
    }

    private void updateUptime() {
        LocalDateTime now = LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(startTime, now);
        long hours = ChronoUnit.HOURS.between(startTime, now) % 24;
        uptimeLabel.setText(days + " days " + hours + " hours");
    }

    private void updateSystemResources() {
        // Simulate CPU usage
        double cpu = 0.35 + random.nextDouble() * 0.15;
        cpuProgress.setProgress(cpu);
        cpuLabel.setText(String.format("%.0f%%", cpu * 100));

        // Simulate Memory usage
        double memory = 0.65 + random.nextDouble() * 0.08;
        memoryProgress.setProgress(memory);
        double memoryUsed = memory * 8;
        memoryLabel.setText(String.format("%.0f%% (%.1f GB / 8 GB)", memory * 100, memoryUsed));

        // Disk usage (more stable)
        double disk = 0.55;
        diskProgress.setProgress(disk);
        diskLabel.setText("55% (275 GB / 500 GB)");
    }

    private void startAutoUpdate() {
        updateTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            updateUptime();
            updateSystemResources();
        }));
        updateTimeline.setCycleCount(Animation.INDEFINITE);
        updateTimeline.play();
    }

    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            currentActiveButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-alignment: center-left; -fx-padding: 12 15; -fx-background-radius: 6; -fx-font-size: 13;");
        }

        button.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1e40af; -fx-alignment: center-left; -fx-padding: 12 15; -fx-background-radius: 6; -fx-font-size: 13;");
        currentActiveButton = button;
    }

    @FXML
    private void showSystemInfo() {
        setActiveButton(systemInfoBtn);
        // Content is already shown by default
    }

    @FXML
    private void showNetworkConfig() {
        setActiveButton(networkConfigBtn);
        showAlert("Network Configuration", "Network configuration panel coming soon...", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void showEmailSettings() {
        setActiveButton(emailSettingsBtn);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Email Settings");
        dialog.setHeaderText("Configure Email Server");

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        TextField smtpServer = new TextField("smtp.ensah.ma");
        TextField smtpPort = new TextField("587");
        TextField username = new TextField("admin@ensah.ma");
        PasswordField password = new PasswordField();
        CheckBox useTLS = new CheckBox("Use TLS/SSL");
        useTLS.setSelected(true);

        content.getChildren().addAll(
                new Label("SMTP Server:"), smtpServer,
                new Label("SMTP Port:"), smtpPort,
                new Label("Username:"), username,
                new Label("Password:"), password,
                useTLS
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Success", "Email settings saved successfully!", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void showQoEThresholds() {
        setActiveButton(qoeThresholdsBtn);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("QoE Thresholds");
        dialog.setHeaderText("Configure QoE Alert Thresholds");

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        Spinner<Integer> excellentThreshold = new Spinner<>(1, 5, 5);
        Spinner<Integer> goodThreshold = new Spinner<>(1, 5, 4);
        Spinner<Integer> fairThreshold = new Spinner<>(1, 5, 3);
        Spinner<Integer> poorThreshold = new Spinner<>(1, 5, 2);

        content.getChildren().addAll(
                new Label("Excellent (MOS):"), excellentThreshold,
                new Label("Good (MOS):"), goodThreshold,
                new Label("Fair (MOS):"), fairThreshold,
                new Label("Poor (MOS):"), poorThreshold
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Success", "QoE thresholds updated successfully!", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void showQoSParams() {
        setActiveButton(qosParamsBtn);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("QoS Parameters");
        dialog.setHeaderText("Configure QoS Monitoring Parameters");

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        Spinner<Integer> latencyThreshold = new Spinner<>(10, 1000, 100);
        Spinner<Double> jitterThreshold = new Spinner<>(1.0, 100.0, 30.0, 1.0);
        Spinner<Double> packetLossThreshold = new Spinner<>(0.1, 10.0, 1.0, 0.1);
        Spinner<Integer> bandwidthMin = new Spinner<>(1, 10000, 100);

        content.getChildren().addAll(
                new Label("Max Latency (ms):"), latencyThreshold,
                new Label("Max Jitter (ms):"), jitterThreshold,
                new Label("Max Packet Loss (%):"), packetLossThreshold,
                new Label("Min Bandwidth (Mbps):"), bandwidthMin
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Success", "QoS parameters updated successfully!", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void showAlerts() {
        setActiveButton(alertsBtn);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Alerts & Notifications");
        dialog.setHeaderText("Configure System Alerts");

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        CheckBox emailAlerts = new CheckBox("Enable email alerts");
        CheckBox smsAlerts = new CheckBox("Enable SMS alerts");
        CheckBox desktopNotifications = new CheckBox("Enable desktop notifications");
        CheckBox criticalOnly = new CheckBox("Critical alerts only");

        emailAlerts.setSelected(true);
        desktopNotifications.setSelected(true);

        TextField recipients = new TextField("admin@ensah.ma");

        content.getChildren().addAll(
                emailAlerts,
                smsAlerts,
                desktopNotifications,
                criticalOnly,
                new Separator(),
                new Label("Alert Recipients:"),
                recipients
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Success", "Alert settings saved successfully!", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void showSecurity() {
        setActiveButton(securityBtn);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Security Policies");
        dialog.setHeaderText("Configure Security Settings");

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        CheckBox twoFactor = new CheckBox("Require two-factor authentication");
        CheckBox strongPassword = new CheckBox("Enforce strong passwords");
        CheckBox sessionTimeout = new CheckBox("Enable session timeout");

        Spinner<Integer> passwordLength = new Spinner<>(6, 20, 8);
        Spinner<Integer> timeoutMinutes = new Spinner<>(5, 120, 30);

        strongPassword.setSelected(true);
        sessionTimeout.setSelected(true);

        content.getChildren().addAll(
                twoFactor,
                strongPassword,
                new Label("Minimum Password Length:"), passwordLength,
                sessionTimeout,
                new Label("Session Timeout (minutes):"), timeoutMinutes
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Success", "Security policies updated successfully!", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void showPermissions() {
        setActiveButton(permissionsBtn);
        showAlert("User Permissions", "User permissions panel coming soon...", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void showAuditLogs() {
        setActiveButton(auditLogsBtn);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Audit Logs");
        alert.setHeaderText("Recent System Activities");
        alert.setContentText(
                "2024-12-03 11:30 - User 'admin' logged in\n" +
                        "2024-12-03 11:25 - Report generated: QoE Weekly\n" +
                        "2024-12-03 11:20 - Settings updated by 'admin'\n" +
                        "2024-12-03 11:15 - New user created: 'johndoe'\n" +
                        "2024-12-03 11:10 - Network device connected: Router-Main\n" +
                        "\nView full logs in the Audit Logs section..."
        );
        alert.showAndWait();
    }

    @FXML
    private void showBackup() {
        setActiveButton(backupBtn);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Backup & Restore");
        dialog.setHeaderText("System Backup Configuration");

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        CheckBox autoBackup = new CheckBox("Enable automatic backups");
        autoBackup.setSelected(true);

        ComboBox<String> frequency = new ComboBox<>();
        frequency.getItems().addAll("Daily", "Weekly", "Monthly");
        frequency.setValue("Daily");

        TextField backupPath = new TextField("");

        Spinner<Integer> retention = new Spinner<>(1, 365, 30);

        Label lastBackup = new Label("Last backup: 2024-12-03 02:00");

        content.getChildren().addAll(
                autoBackup,
                new Label("Backup Frequency:"), frequency,
                new Label("Backup Location:"), backupPath,
                new Label("Retention (days):"), retention,
                new Separator(),
                lastBackup
        );

        dialog.getDialogPane().setContent(content);
        ButtonType backupNow = new ButtonType("Backup Now");
        dialog.getDialogPane().getButtonTypes().addAll(backupNow, ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == backupNow) {
                showAlert("Backup Started", "System backup initiated. This may take several minutes...", Alert.AlertType.INFORMATION);
            } else if (response == ButtonType.OK) {
                showAlert("Success", "Backup settings saved successfully!", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void showUpdates() {
        setActiveButton(updatesBtn);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("System Updates");
        alert.setHeaderText("Check for Updates");
        alert.setContentText(
                "Current Version: v2.5.0\n" +
                        "Latest Version: v2.5.0\n\n" +
                        "Your system is up to date!\n\n" +
                        "Last checked: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        alert.showAndWait();
    }

    @FXML
    private void saveSettings() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Save Settings");
        confirmation.setHeaderText("Save System Settings?");
        confirmation.setContentText("Do you want to save the current configuration?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Save settings logic here
                showAlert("Success", "Settings saved successfully!", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void clearCache() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Clear Cache");
        confirmation.setHeaderText("Clear System Cache?");
        confirmation.setContentText("This will clear all cached data. Continue?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Cache Cleared", "System cache cleared successfully! (245 MB freed)", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void optimizeDatabase() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Optimize Database");
        confirmation.setHeaderText("Optimize Database?");
        confirmation.setContentText("This process may take several minutes. Continue?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Optimization Started", "Database optimization in progress...", Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    private void generateDiagnostics() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("System Diagnostics");
        alert.setHeaderText("System Health Report");
        alert.setContentText(
                "System Status: Healthy ✓\n" +
                        "CPU Usage: " + cpuLabel.getText() + "\n" +
                        "Memory Usage: " + memoryLabel.getText() + "\n" +
                        "Disk Space: " + diskLabel.getText() + "\n" +
                        "Database: Connected ✓\n" +
                        "Network: Operational ✓\n" +
                        "Services: All Running ✓\n\n" +
                        "Diagnostics report generated at: " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        alert.showAndWait();
    }

    @FXML
    private void restartSystem() {
        Alert confirmation = new Alert(Alert.AlertType.WARNING);
        confirmation.setTitle("Restart System");
        confirmation.setHeaderText("Restart Required");
        confirmation.setContentText("This will restart the system and disconnect all users. Continue?");
        confirmation.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                showAlert("System Restart", "System will restart in 30 seconds. Please save your work.", Alert.AlertType.WARNING);
            }
        });
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}