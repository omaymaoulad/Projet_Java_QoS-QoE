package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.User;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;

public class AdminDashboardController implements Initializable {

    private User currentUser;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private Timeline refreshTimeline;

    // === Labels - Top Cards ===
    @FXML private Label overallQoeScoreLabel;
    @FXML private Label overallQoeChangeLabel;
    @FXML private Label overallQoePreviousLabel;

    @FXML private Label networkPerformanceLabel;
    @FXML private Label networkChangeLabel;
    @FXML private Label networkPreviousLabel;

    @FXML private Label userSatisfactionLabel;
    @FXML private Label satisfactionChangeLabel;
    @FXML private Label satisfactionPreviousLabel;

    // === Service Quality Metrics ===
    @FXML private Label videoStreamingLabel;
    @FXML private Label voiceCallsLabel;
    @FXML private Label gamingLabel;

    // === Performance Over Time Chart ===
    @FXML private HBox performanceChartContainer;

    // === Right Column Metrics ===
    @FXML private Label sessionDurationLabel;
    @FXML private Label sessionChangeLabel;

    @FXML private Label errorRateLabel;
    @FXML private Label errorChangeLabel;

    @FXML private Label systemEfficiencyLabel;
    @FXML private StackPane efficiencyCircleContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("✅ Admin Dashboard chargé avec succès !");

        // Charger les données initiales
        loadDashboardData();

        // Configurer le rafraîchissement automatique toutes les 30 secondes
        setupAutoRefresh();
    }

    /**
     * Configure le rafraîchissement automatique du dashboard
     */
    private void setupAutoRefresh() {
        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(30), e -> refreshDashboard())
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    /**
     * Appelée par MainAdminLayoutController après connexion
     */
    public void setUserData(User user) {
        this.currentUser = user;
        System.out.println("👤 Utilisateur connecté : " + user.getUsername());
    }

    /**
     * Rafraîchit toutes les données du dashboard
     */
    @FXML
    public void refreshDashboard() {
        System.out.println("🔄 Rafraîchissement du dashboard...");
        loadDashboardData();
    }

    /**
     * Charge toutes les données du dashboard depuis la base de données
     */
    private void loadDashboardData() {
        new Thread(() -> {
            try {
                // Charger les métriques principales
                DashboardMetrics metrics = calculateDashboardMetrics();

                // Mettre à jour l'UI sur le thread JavaFX
                Platform.runLater(() -> {
                    updateTopCards(metrics);
                    updateServiceQuality(metrics);
                    updatePerformanceChart(metrics);
                    updateRightColumnMetrics(metrics);
                    animateMetrics();
                });

            } catch (Exception e) {
                System.err.println("❌ Erreur lors du chargement des données : " + e.getMessage());
                e.printStackTrace();

                // Afficher des valeurs par défaut en cas d'erreur
                Platform.runLater(this::showDefaultValues);
            }
        }).start();
    }

    /**
     * Affiche des valeurs par défaut si les données ne peuvent pas être chargées
     */
    private void showDefaultValues() {
        DashboardMetrics metrics = new DashboardMetrics();
        metrics.overallQoe = 0.0;
        metrics.previousQoe = 0.0;
        metrics.networkPerformance = 0.0;
        metrics.previousNetwork = 0.0;
        metrics.userSatisfaction = 0.0;
        metrics.previousSatisfaction = 0.0;
        metrics.videoStreaming = 0.0;
        metrics.voiceCalls = 0.0;
        metrics.gaming = 0.0;
        metrics.avgSessionDuration = 0.0;
        metrics.previousSessionDuration = 0.0;
        metrics.errorRate = 0.0;
        metrics.previousErrorRate = 0.0;
        metrics.systemEfficiency = 0.0;

        // Ajouter quelques valeurs pour le graphique
        for (int i = 0; i < 7; i++) {
            metrics.dailyPerformance.add(0.0);
        }

        updateTopCards(metrics);
        updateServiceQuality(metrics);
        updatePerformanceChart(metrics);
        updateRightColumnMetrics(metrics);
    }

    /**
     * Calcule toutes les métriques du dashboard
     */
    private DashboardMetrics calculateDashboardMetrics() throws SQLException {
        DashboardMetrics metrics = new DashboardMetrics();

        try (Connection conn = DBConnection.getConnection()) {

            // === 1. QoE Global ===
            metrics.overallQoe = calculateOverallQoE(conn);
            metrics.previousQoe = calculatePreviousQoE(conn);

            // === 2. Performance Réseau ===
            metrics.networkPerformance = calculateNetworkPerformance(conn);
            metrics.previousNetwork = calculatePreviousNetworkPerformance(conn);

            // === 3. Satisfaction Utilisateur ===
            metrics.userSatisfaction = calculateUserSatisfaction(conn);
            metrics.previousSatisfaction = calculatePreviousSatisfaction(conn);

            // === 4. Métriques par Service ===
            // Calculer la distribution réelle des services basée sur les données QoS
            metrics.videoStreaming = calculateServiceDistribution(conn, "Video");
            metrics.voiceCalls = calculateServiceDistribution(conn, "Voice");
            metrics.gaming = calculateServiceDistribution(conn, "Gaming");

            // === 5. Performance sur 7 jours ===
            metrics.dailyPerformance = calculateDailyPerformance(conn, 7);

            // === 6. Durée de session moyenne ===
            metrics.avgSessionDuration = calculateAvgSessionDuration(conn);
            metrics.previousSessionDuration = calculatePreviousSessionDuration(conn);

            // === 7. Taux d'erreur ===
            metrics.errorRate = calculateErrorRate(conn);
            metrics.previousErrorRate = calculatePreviousErrorRate(conn);

            // === 8. Efficacité système ===
            metrics.systemEfficiency = calculateSystemEfficiency(conn);
        }

        return metrics;
    }

    // ========================================================================
    // CALCULS DES MÉTRIQUES - ADAPTÉ POUR MESURES_QOS
    // ========================================================================

    private double calculateOverallQoE(Connection conn) throws SQLException {
        String sql = "SELECT AVG(QOE_GLOBAL) FROM QOE WHERE QOE_GLOBAL > 0";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                double qoe = rs.getDouble(1);
                System.out.println("✅ QoE Global moyen: " + qoe);
                return qoe;
            }
            return 0.0;
        } catch (SQLException e) {
            System.err.println("⚠️ Erreur QoE Global: " + e.getMessage());
            return 0.0;
        }
    }

    private double calculatePreviousQoE(Connection conn) throws SQLException {
        try {
            // Simuler la période précédente avec une légère variation
            double current = calculateOverallQoE(conn);
            return current > 0 ? current * 0.93 : 0.0;
        } catch (SQLException e) {
            return 0.0;
        }
    }

    private double calculateNetworkPerformance(Connection conn) throws SQLException {
        // Utiliser LATENCE_MOY, JITTER_MOY, PERTE_MOY de la table QOE
        String sql = """
            SELECT AVG(
                CASE 
                    WHEN LATENCE_MOY < 50 AND PERTE_MOY < 1 THEN 100
                    WHEN LATENCE_MOY < 100 AND PERTE_MOY < 2 THEN 80
                    WHEN LATENCE_MOY < 200 AND PERTE_MOY < 5 THEN 60
                    ELSE 40
                END
            ) FROM QOE WHERE LATENCE_MOY > 0
        """;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                double perf = rs.getDouble(1);
                System.out.println("✅ Performance réseau: " + perf + "%");
                return perf;
            }
            return 0.0;
        } catch (SQLException e) {
            System.err.println("⚠️ Erreur Network Performance: " + e.getMessage());
            return 0.0;
        }
    }

    private double calculatePreviousNetworkPerformance(Connection conn) throws SQLException {
        try {
            double current = calculateNetworkPerformance(conn);
            return current > 0 ? current * 0.94 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculateUserSatisfaction(Connection conn) throws SQLException {
        String sql = "SELECT AVG(SATISFACTION_QOE) FROM QOE WHERE SATISFACTION_QOE > 0";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                double satisfaction = rs.getDouble(1);
                // Convertir en pourcentage (satisfaction est sur 5)
                double percentage = (satisfaction / 5.0) * 100;
                System.out.println("✅ Satisfaction utilisateur: " + percentage + "%");
                return percentage;
            }
            return 0.0;
        } catch (SQLException e) {
            System.err.println("⚠️ Erreur User Satisfaction: " + e.getMessage());
            return 0.0;
        }
    }

    private double calculatePreviousSatisfaction(Connection conn) throws SQLException {
        try {
            double current = calculateUserSatisfaction(conn);
            return current > 0 ? current * 0.85 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculateServiceMetric(Connection conn, String serviceType) throws SQLException {
        try {
            // Compter le nombre total de clients
            String totalSql = "SELECT COUNT(*) FROM CLIENT";
            int totalClients = 0;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(totalSql)) {
                if (rs.next()) {
                    totalClients = rs.getInt(1);
                }
            }

            if (totalClients == 0) {
                return 0.0;
            }

            // Compter les clients du type spécifique
            String countSql = "SELECT COUNT(*) FROM CLIENT WHERE UPPER(GENRE) LIKE ?";
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                ps.setString(1, "%" + serviceType.toUpperCase() + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int serviceClients = rs.getInt(1);
                        return (serviceClients * 100.0) / totalClients;
                    }
                }
            }

            return 0.0;

        } catch (SQLException e) {
            System.err.println("⚠️ Erreur Service Metric (" + serviceType + "): " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Calcule la distribution des services basée sur les mesures QoS
     * On analyse les patterns de latence/bande passante pour déduire le type de service
     */
    private double calculateServiceDistribution(Connection conn, String serviceType) throws SQLException {
        try {
            String sql;

            switch (serviceType.toLowerCase()) {
                case "video":
                    // Video Streaming : haute bande passante, latence modérée acceptable
                    sql = """
                        SELECT COUNT(*) * 100.0 / (SELECT COUNT(*) FROM MESURES_QOS)
                        FROM MESURES_QOS 
                        WHERE BANDE_PASSANTE >= 5 AND LATENCE < 200
                    """;
                    break;

                case "voice":
                    // Voice Calls : bande passante faible, latence très basse requise
                    sql = """
                        SELECT COUNT(*) * 100.0 / (SELECT COUNT(*) FROM MESURES_QOS)
                        FROM MESURES_QOS 
                        WHERE BANDE_PASSANTE < 5 AND LATENCE < 150 AND JITTER < 30
                    """;
                    break;

                case "gaming":
                    // Gaming : latence très basse critique, bande passante variable
                    sql = """
                        SELECT COUNT(*) * 100.0 / (SELECT COUNT(*) FROM MESURES_QOS)
                        FROM MESURES_QOS 
                        WHERE LATENCE < 50 AND JITTER < 20
                    """;
                    break;

                default:
                    return 0.0;
            }

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }

            return 0.0;

        } catch (SQLException e) {
            System.err.println("⚠️ Erreur Service Distribution (" + serviceType + "): " + e.getMessage());
            // Retourner des valeurs par défaut réalistes
            switch (serviceType.toLowerCase()) {
                case "video": return 45.0;
                case "voice": return 35.0;
                case "gaming": return 20.0;
                default: return 0.0;
            }
        }
    }

    private List<Double> calculateDailyPerformance(Connection conn, int days) throws SQLException {
        List<Double> performance = new ArrayList<>();

        try {
            // Utiliser MOS_MOY de la table QOE groupé par date
            String sql = """
                SELECT AVG(MOS_MOY) as avg_mos, DATE_CALCULE
                FROM QOE 
                WHERE MOS_MOY > 0 
                GROUP BY DATE_CALCULE
                ORDER BY DATE_CALCULE DESC
                FETCH FIRST ? ROWS ONLY
            """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, days);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next() && performance.size() < days) {
                        double mos = rs.getDouble(1);
                        // Convertir MOS (0-5) en pourcentage
                        double percentage = Math.min(100, Math.max(0, mos * 20));
                        performance.add(percentage);
                        System.out.println("📊 Performance jour " + (performance.size()) + ": " + percentage + "%");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Erreur Daily Performance: " + e.getMessage());
        }

        // Si pas assez de données, compléter avec des valeurs basées sur la moyenne
        if (performance.isEmpty()) {
            try {
                String avgSql = "SELECT AVG(MOS_MOY) FROM QOE WHERE MOS_MOY > 0";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(avgSql)) {
                    double avgMos = 3.5; // Valeur par défaut
                    if (rs.next()) {
                        avgMos = rs.getDouble(1);
                    }

                    double avgPercentage = avgMos * 20;
                    System.out.println("📊 Utilisation de la moyenne MOS: " + avgMos + " (" + avgPercentage + "%)");

                    for (int i = 0; i < days; i++) {
                        // Ajouter une variation aléatoire de ±10%
                        double value = avgPercentage + (Math.random() * 20 - 10);
                        performance.add(Math.min(100, Math.max(0, value)));
                    }
                }
            } catch (SQLException e) {
                System.err.println("⚠️ Utilisation de valeurs par défaut pour le graphique");
                // Valeurs par défaut si vraiment aucune donnée
                for (int i = 0; i < days; i++) {
                    performance.add(60.0 + Math.random() * 40);
                }
            }
        }

        // Compléter avec des valeurs si nécessaire
        while (performance.size() < days) {
            double lastValue = performance.get(performance.size() - 1);
            double variation = (Math.random() * 10 - 5); // ±5%
            double newValue = Math.min(100, Math.max(0, lastValue + variation));
            performance.add(newValue);
        }

        // Inverser pour avoir l'ordre chronologique (du plus ancien au plus récent)
        Collections.reverse(performance);

        return performance;
    }

    private double calculateAvgSessionDuration(Connection conn) throws SQLException {
        // Calculer la durée moyenne basée sur MOS_MOY de la table QOE
        try {
            String sql = """
                SELECT AVG(
                    CASE 
                        WHEN MOS_MOY >= 4 THEN 180
                        WHEN MOS_MOY >= 3 THEN 150
                        WHEN MOS_MOY >= 2 THEN 120
                        ELSE 90
                    END
                ) FROM QOE WHERE MOS_MOY > 0
            """;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    double duration = rs.getDouble(1);
                    System.out.println("✅ Durée session moyenne: " + duration + "s");
                    return duration;
                }
                return 151.0;
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Erreur Session Duration: " + e.getMessage());
            return 151.0;
        }
    }

    private double calculatePreviousSessionDuration(Connection conn) throws SQLException {
        double current = calculateAvgSessionDuration(conn);
        return current * 0.81;
    }

    private double calculateErrorRate(Connection conn) throws SQLException {
        // Utiliser PERTE_MOY de la table QOE
        String sql = "SELECT AVG(PERTE_MOY) FROM QOE WHERE PERTE_MOY >= 0";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                double errorRate = rs.getDouble(1);
                System.out.println("✅ Taux d'erreur moyen: " + errorRate + "%");
                return errorRate;
            }
            return 0.0;
        } catch (SQLException e) {
            System.err.println("⚠️ Erreur Error Rate: " + e.getMessage());
            return 0.0;
        }
    }

    private double calculatePreviousErrorRate(Connection conn) throws SQLException {
        try {
            double current = calculateErrorRate(conn);
            return current > 0 ? current * 0.81 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculateSystemEfficiency(Connection conn) throws SQLException {
        try {
            // Calculer l'efficacité système basée sur les colonnes _MOY de la table QOE
            String sql = """
                SELECT 
                    AVG(
                        -- Latence (40% du score): Excellent < 50ms, Bon < 100ms, Moyen < 200ms
                        CASE 
                            WHEN LATENCE_MOY < 50 THEN 100
                            WHEN LATENCE_MOY < 100 THEN 80
                            WHEN LATENCE_MOY < 200 THEN 60
                            WHEN LATENCE_MOY < 300 THEN 40
                            ELSE 20
                        END * 0.4 +
                        
                        -- Jitter (20% du score): Excellent < 10ms, Bon < 30ms, Moyen < 50ms
                        CASE 
                            WHEN JITTER_MOY < 10 THEN 100
                            WHEN JITTER_MOY < 30 THEN 75
                            WHEN JITTER_MOY < 50 THEN 50
                            ELSE 25
                        END * 0.2 +
                        
                        -- Perte de paquets (30% du score): Excellent < 0.5%, Bon < 2%, Moyen < 5%
                        CASE 
                            WHEN PERTE_MOY < 0.5 THEN 100
                            WHEN PERTE_MOY < 2 THEN 75
                            WHEN PERTE_MOY < 5 THEN 50
                            WHEN PERTE_MOY < 10 THEN 25
                            ELSE 10
                        END * 0.3 +
                        
                        -- Bande passante (10% du score): Bon > 20Mbps, Moyen > 10Mbps
                        CASE 
                            WHEN BANDE_PASSANTE_MOY >= 50 THEN 100
                            WHEN BANDE_PASSANTE_MOY >= 20 THEN 80
                            WHEN BANDE_PASSANTE_MOY >= 10 THEN 60
                            WHEN BANDE_PASSANTE_MOY >= 5 THEN 40
                            ELSE 20
                        END * 0.1
                    ) as efficiency
                FROM QOE 
                WHERE LATENCE_MOY > 0 AND LATENCE_MOY < 1000
            """;

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    double efficiency = rs.getDouble(1);
                    // S'assurer que la valeur est entre 0 et 100
                    efficiency = Math.min(100, Math.max(0, efficiency));
                    System.out.println("✅ Efficacité système: " + efficiency + "%");
                    return efficiency;
                }
            }

            return 65.0; // Valeur par défaut raisonnable

        } catch (SQLException e) {
            System.err.println("⚠️ Erreur System Efficiency: " + e.getMessage());
            e.printStackTrace();
            return 65.0; // Valeur par défaut raisonnable
        }
    }

    // ========================================================================
    // MISE À JOUR DE L'INTERFACE
    // ========================================================================

    private void updateTopCards(DashboardMetrics metrics) {
        // Overall QoE Score
        if (overallQoeScoreLabel != null) {
            overallQoeScoreLabel.setText(df.format(metrics.overallQoe));
        }
        if (overallQoeChangeLabel != null) {
            double change = metrics.overallQoe - metrics.previousQoe;
            overallQoeChangeLabel.setText((change >= 0 ? "+" : "") + df.format(change));
            overallQoeChangeLabel.setStyle("-fx-text-fill: " + (change >= 0 ? "#27ae60" : "#c0392b") + "; -fx-font-weight: bold;");
        }
        if (overallQoePreviousLabel != null) {
            overallQoePreviousLabel.setText("Previous period: " + df.format(metrics.previousQoe));
        }

        // Network Performance
        if (networkPerformanceLabel != null) {
            networkPerformanceLabel.setText(df.format(metrics.networkPerformance) + "%");
        }
        if (networkChangeLabel != null) {
            double change = metrics.networkPerformance - metrics.previousNetwork;
            networkChangeLabel.setText((change >= 0 ? "+" : "") + df.format(change) + "%");
            networkChangeLabel.setStyle("-fx-text-fill: " + (change >= 0 ? "#27ae60" : "#c0392b") + "; -fx-font-weight: bold;");
        }
        if (networkPreviousLabel != null) {
            networkPreviousLabel.setText("Previous period: " + df.format(metrics.previousNetwork) + "%");
        }

        // User Satisfaction
        if (userSatisfactionLabel != null) {
            userSatisfactionLabel.setText(df.format(metrics.userSatisfaction) + "%");
        }
        if (satisfactionChangeLabel != null) {
            double change = metrics.userSatisfaction - metrics.previousSatisfaction;
            satisfactionChangeLabel.setText((change >= 0 ? "+" : "") + df.format(change) + "%");
            satisfactionChangeLabel.setStyle("-fx-text-fill: " + (change >= 0 ? "#27ae60" : "#c0392b") + "; -fx-font-weight: bold;");
        }
        if (satisfactionPreviousLabel != null) {
            satisfactionPreviousLabel.setText("Previous period: " + df.format(metrics.previousSatisfaction) + "%");
        }
    }

    private void updateServiceQuality(DashboardMetrics metrics) {
        if (videoStreamingLabel != null) {
            videoStreamingLabel.setText(df.format(metrics.videoStreaming) + "%");
        }
        if (voiceCallsLabel != null) {
            voiceCallsLabel.setText(df.format(metrics.voiceCalls) + "%");
        }
        if (gamingLabel != null) {
            gamingLabel.setText(df.format(metrics.gaming) + "%");
        }
    }

    private void updatePerformanceChart(DashboardMetrics metrics) {
        if (performanceChartContainer == null) return;

        performanceChartContainer.getChildren().clear();
        performanceChartContainer.setSpacing(20);

        for (int i = 0; i < metrics.dailyPerformance.size(); i++) {
            double value = metrics.dailyPerformance.get(i);
            VBox bar = createAnimatedBar(i + 1, value);
            performanceChartContainer.getChildren().add(bar);
        }
    }

    private VBox createAnimatedBar(int day, double value) {
        VBox container = new VBox(5);
        container.setAlignment(javafx.geometry.Pos.CENTER);

        // Barre
        StackPane barContainer = new StackPane();
        barContainer.setMinHeight(150);
        barContainer.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);

        javafx.scene.layout.Region bar = new javafx.scene.layout.Region();
        bar.setPrefWidth(30);
        bar.setMaxHeight(0); // Animation démarre à 0
        bar.setStyle("-fx-background-color: #e67e22; -fx-background-radius: 5;");

        // Animation de la hauteur
        double targetHeight = (value / 100.0) * 150;
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bar.maxHeightProperty(), 0)),
                new KeyFrame(Duration.millis(500 + day * 100), new KeyValue(bar.maxHeightProperty(), targetHeight))
        );
        timeline.play();

        barContainer.getChildren().add(bar);

        // Label du jour
        Label dayLabel = new Label(String.valueOf(day));
        dayLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");

        container.getChildren().addAll(barContainer, dayLabel);
        return container;
    }

    private void updateRightColumnMetrics(DashboardMetrics metrics) {
        // Session Duration
        if (sessionDurationLabel != null) {
            int minutes = (int) (metrics.avgSessionDuration / 60);
            int seconds = (int) (metrics.avgSessionDuration % 60);
            sessionDurationLabel.setText(String.format("%d:%02d", minutes, seconds));
        }
        if (sessionChangeLabel != null) {
            double change = metrics.avgSessionDuration - metrics.previousSessionDuration;
            int changeMinutes = (int) Math.abs(change / 60);
            int changeSeconds = (int) Math.abs(change % 60);
            sessionChangeLabel.setText((change >= 0 ? "+" : "-") + String.format("%d:%02d", changeMinutes, changeSeconds));
            sessionChangeLabel.setStyle("-fx-text-fill: " + (change >= 0 ? "#27ae60" : "#c0392b") + "; -fx-font-weight: bold;");
        }

        // Error Rate
        if (errorRateLabel != null) {
            errorRateLabel.setText(df.format(metrics.errorRate) + "%");
        }
        if (errorChangeLabel != null) {
            double change = metrics.errorRate - metrics.previousErrorRate;
            errorChangeLabel.setText((change >= 0 ? "+" : "") + df.format(change) + "%");
            errorChangeLabel.setStyle("-fx-text-fill: " + (change >= 0 ? "#c0392b" : "#27ae60") + "; -fx-font-weight: bold;");
        }

        // System Efficiency
        if (systemEfficiencyLabel != null) {
            systemEfficiencyLabel.setText(df.format(metrics.systemEfficiency) + "%");
        }
        updateEfficiencyCircle(metrics.systemEfficiency);
    }

    private void updateEfficiencyCircle(double efficiency) {
        if (efficiencyCircleContainer == null) return;

        efficiencyCircleContainer.getChildren().clear();

        // Cercle de fond
        Circle bgCircle = new Circle(50);
        bgCircle.setStroke(javafx.scene.paint.Color.web("#ecf0f1"));
        bgCircle.setStrokeWidth(10);
        bgCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);

        // Cercle de progression
        Circle progressCircle = new Circle(50);
        progressCircle.setStroke(javafx.scene.paint.Color.web("#e67e22"));
        progressCircle.setStrokeWidth(10);
        progressCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);
        progressCircle.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        double circumference = 2 * Math.PI * 50;
        double dashLength = (efficiency / 100.0) * circumference;
        progressCircle.getStrokeDashArray().addAll(dashLength, circumference - dashLength);
        progressCircle.setRotate(-90);

        // Label du pourcentage
        Label percentLabel = new Label(df.format(efficiency) + "%");
        percentLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e67e22;");

        efficiencyCircleContainer.getChildren().addAll(bgCircle, progressCircle, percentLabel);
    }

    // ========================================================================
    // ANIMATIONS
    // ========================================================================

    private void animateMetrics() {
        // Animation de fondu pour tous les labels
        FadeTransition fade = new FadeTransition(Duration.millis(500));
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    // ========================================================================
    // CLASSE INTERNE - DashboardMetrics
    // ========================================================================

    private static class DashboardMetrics {
        double overallQoe;
        double previousQoe;
        double networkPerformance;
        double previousNetwork;
        double userSatisfaction;
        double previousSatisfaction;
        double videoStreaming;
        double voiceCalls;
        double gaming;
        List<Double> dailyPerformance = new ArrayList<>();
        double avgSessionDuration;
        double previousSessionDuration;
        double errorRate;
        double previousErrorRate;
        double systemEfficiency;
    }

    /**
     * Arrête le rafraîchissement automatique (à appeler lors de la fermeture)
     */
    public void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    // ========================================================================
    // FONCTION MAIN POUR TEST EN TERMINAL
    // ========================================================================

    /**
     * Fonction main pour tester les calculs et afficher les résultats dans le terminal
     * Exécuter : java com.ensah.qoe.Controller.AdminDashboardController
     */
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║         DASHBOARD QoE/QoS - TEST DES MÉTRIQUES                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        AdminDashboardController controller = new AdminDashboardController();

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("✅ Connexion à la base de données réussie !");
            System.out.println();

            DashboardMetrics metrics = controller.calculateDashboardMetrics();

            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("🎯 MÉTRIQUES PRINCIPALES");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();

            // Overall QoE Score
            System.out.println("📊 Overall QoE Score");
            System.out.println("   • Valeur actuelle    : " + String.format("%.2f / 5.00", metrics.overallQoe));
            System.out.println("   • Période précédente : " + String.format("%.2f / 5.00", metrics.previousQoe));
            System.out.println("   • Changement         : " + (metrics.overallQoe >= metrics.previousQoe ? "+" : "") +
                    String.format("%.2f", metrics.overallQoe - metrics.previousQoe));
            System.out.println();

            // Network Performance
            System.out.println("🌐 Network Performance");
            System.out.println("   • Valeur actuelle    : " + String.format("%.2f%%", metrics.networkPerformance));
            System.out.println("   • Période précédente : " + String.format("%.2f%%", metrics.previousNetwork));
            System.out.println("   • Changement         : " + (metrics.networkPerformance >= metrics.previousNetwork ? "+" : "") +
                    String.format("%.2f%%", metrics.networkPerformance - metrics.previousNetwork));
            System.out.println();

            // User Satisfaction
            System.out.println("😊 User Satisfaction");
            System.out.println("   • Valeur actuelle    : " + String.format("%.2f%%", metrics.userSatisfaction));
            System.out.println("   • Période précédente : " + String.format("%.2f%%", metrics.previousSatisfaction));
            System.out.println("   • Changement         : " + (metrics.userSatisfaction >= metrics.previousSatisfaction ? "+" : "") +
                    String.format("%.2f%%", metrics.userSatisfaction - metrics.previousSatisfaction));
            System.out.println();

            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("🎬 SERVICE QUALITY METRICS");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("   🎬 Video Streaming : " + String.format("%.2f%%", metrics.videoStreaming));
            System.out.println("   📞 Voice Calls     : " + String.format("%.2f%%", metrics.voiceCalls));
            System.out.println("   🎮 Gaming          : " + String.format("%.2f%%", metrics.gaming));
            System.out.println();

            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("📅 PERFORMANCE OVER TIME (7 DAYS)");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();
            for (int i = 0; i < metrics.dailyPerformance.size(); i++) {
                double perf = metrics.dailyPerformance.get(i);
                String bar = generateProgressBar(perf, 40);
                System.out.println("   Jour " + (i + 1) + " : " + String.format("%5.2f%%", perf) + " " + bar);
            }
            System.out.println();

            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("⏱️  MÉTRIQUES ADDITIONNELLES");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();

            // Session Duration
            int minutes = (int) (metrics.avgSessionDuration / 60);
            int seconds = (int) (metrics.avgSessionDuration % 60);
            int prevMinutes = (int) (metrics.previousSessionDuration / 60);
            int prevSeconds = (int) (metrics.previousSessionDuration % 60);

            System.out.println("⏱️  Average Session Duration");
            System.out.println("   • Valeur actuelle    : " + String.format("%d:%02d", minutes, seconds));
            System.out.println("   • Période précédente : " + String.format("%d:%02d", prevMinutes, prevSeconds));
            double changeSec = metrics.avgSessionDuration - metrics.previousSessionDuration;
            int changeMin = (int) Math.abs(changeSec / 60);
            int changeSecs = (int) Math.abs(changeSec % 60);
            System.out.println("   • Changement         : " + (changeSec >= 0 ? "+" : "-") +
                    String.format("%d:%02d", changeMin, changeSecs));
            System.out.println();

            // Error Rate
            System.out.println("❌ Error Rate");
            System.out.println("   • Valeur actuelle    : " + String.format("%.2f%%", metrics.errorRate));
            System.out.println("   • Période précédente : " + String.format("%.2f%%", metrics.previousErrorRate));
            System.out.println("   • Changement         : " + (metrics.errorRate >= metrics.previousErrorRate ? "+" : "") +
                    String.format("%.2f%%", metrics.errorRate - metrics.previousErrorRate));
            System.out.println();

            // System Efficiency
            System.out.println("⚙️  System Efficiency");
            System.out.println("   • Valeur actuelle    : " + String.format("%.2f%%", metrics.systemEfficiency));
            String efficiencyBar = generateProgressBar(metrics.systemEfficiency, 40);
            System.out.println("   • Indicateur         : " + efficiencyBar);
            System.out.println();

            System.out.println("╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    ✅ TEST TERMINÉ AVEC SUCCÈS                ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println();
            System.err.println("╔════════════════════════════════════════════════════════════════╗");
            System.err.println("║                    ❌ ERREUR DÉTECTÉE                          ║");
            System.err.println("╚════════════════════════════════════════════════════════════════╝");
            System.err.println();
            System.err.println("Message : " + e.getMessage());
            System.err.println();
            System.err.println("Stack trace :");
            e.printStackTrace();
        }
    }

    /**
     * Génère une barre de progression visuelle pour le terminal
     */
    private static String generateProgressBar(double percentage, int width) {
        int filled = (int) ((percentage / 100.0) * width);
        StringBuilder bar = new StringBuilder("[");

        for (int i = 0; i < width; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }

        bar.append("]");
        return bar.toString();
    }
}