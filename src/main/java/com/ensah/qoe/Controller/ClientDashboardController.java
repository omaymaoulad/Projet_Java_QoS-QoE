package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.QoE;
import com.ensah.qoe.Models.User;
import javafx.animation.*;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ResourceBundle;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientDashboardController implements Initializable {

    private User currentUser;
    private int selectedRating = 0;

    // Labels pour les métriques QoE
    @FXML private Label usernameLabel;
    @FXML private Label satisfactionScore;
    @FXML private Label videoQualityLabel;
    @FXML private Label audioQualityLabel;
    @FXML private Label interactivityLabel;
    @FXML private Label reliabilityLabel;
    @FXML private Label loadingTimeLabel;
    @FXML private Label bufferingLabel;
    @FXML private Label failureRateLabel;
    @FXML private Label streamingQualityLabel;
    @FXML private Label overallQoELabel;
    @FXML private Label charCount;
    @FXML private Label selectedRatingLabel;

    // Graphiques
    @FXML private LineChart<String, Number> qoeTrendChart;
    @FXML private PieChart serviceDistributionChart;

    // Formulaire de feedback
    @FXML private ComboBox<String> serviceTypeCombo;
    @FXML private TextArea commentField;
    @FXML private TableView<QoE> feedbackHistoryTable;

    // Boutons pour les étoiles
    @FXML private Button star1;
    @FXML private Button star2;
    @FXML private Button star3;
    @FXML private Button star4;
    @FXML private Button star5;

    // Données
    private ObservableList<QoE> qoeHistory = FXCollections.observableArrayList();

    public ClientDashboardController() {
        // Constructeur par défaut
    }

    public void setUserData(User user) {
        this.currentUser = user;
        if (usernameLabel != null && user != null) {
            usernameLabel.setText(user.getUsername());
            loadUserData();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("🎯 ClientDashboardController initialisé");

        initializeComponents();
        setupFormInteractivity();

        // Test de connexion à la base
        testDatabaseConnection();

        // Charger les données
        loadQoeDataFromDatabase();

        if (qoeHistory.isEmpty()) {
            loadSampleData();
        }

        updateDashboardMetrics();
        updateCharts();
    }

    /**
     * Teste la connexion à la base de données
     */
    private void testDatabaseConnection() {
        try {
            Connection conn = DBConnection.getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Connexion BD réussie");
                conn.close();
            } else {
                System.out.println("❌ Connexion BD échouée");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur connexion BD: " + e.getMessage());
        }
    }

    private void initializeComponents() {
        // Initialiser le ComboBox
        if (serviceTypeCombo != null) {
            serviceTypeCombo.getItems().addAll(
                    "📺 Streaming Vidéo",
                    "🎵 Streaming Audio",
                    "🎮 Gaming en ligne",
                    "💬 Visioconférence",
                    "🌐 Navigation Web",
                    "☁️ Cloud Storage",
                    "📱 Application Mobile"
            );
            System.out.println("✅ ComboBox initialisé avec " + serviceTypeCombo.getItems().size() + " éléments");
        }

        // Configurer le compteur de caractères
        if (commentField != null && charCount != null) {
            commentField.textProperty().addListener((obs, oldText, newText) -> {
                int length = newText.length();
                updateCharacterCount(length);

                if (length > 500) {
                    commentField.setText(oldText);
                }
            });
            System.out.println("✅ Compteur de caractères initialisé");
        }

        setupCharts();
        setupTableColumns();
    }

    /**
     * Charge les données QoE depuis la base de données
     */
    private void loadQoeDataFromDatabase() {
        System.out.println("🔄 Chargement des données QoE depuis la BD...");

        if (currentUser == null) {
            System.err.println("❌ Aucun utilisateur connecté");
            return;
        }

        String sql = "SELECT * FROM QOE WHERE ID_CLIENT = ? ORDER BY DATE_CALCULE DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, currentUser.getId());
            ResultSet rs = stmt.executeQuery();

            qoeHistory.clear();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            int count = 0;

            while (rs.next()) {
                // Récupération des données depuis la table QOE
                double satisfaction = rs.getDouble("SATISFACTION_QOE");
                double serviceQoe = rs.getDouble("SERVICE_QOE");
                double prixQoe = rs.getDouble("PRIX_QOE");
                double contratQoe = rs.getDouble("CONTRAT_QOE");
                double lifetimeQoe = rs.getDouble("LIFETIME_QOE");
                double feedbackScore = rs.getDouble("FEEDBACK_SCORE");
                double qoeGlobal = rs.getDouble("QOE_GLOBAL");

                // Métriques techniques
                double latence = rs.getDouble("LATENCE_MOY");
                double jitter = rs.getDouble("JITTER_MOY");
                double perte = rs.getDouble("PERTE_MOY");
                double bandePassante = rs.getDouble("BANDE_PASSANTE_MOY");

                Timestamp dateCalcul = rs.getTimestamp("DATE_CALCULE");

                System.out.println("📥 Donnée QoE chargée: Client " + currentUser.getId() + " - Score: " + qoeGlobal);

                // Créer un objet QoE avec les données de la table
                QoE qoe = new QoE(
                        satisfaction, serviceQoe, prixQoe, contratQoe, lifetimeQoe, qoeGlobal,
                        latence, jitter, perte, bandePassante,
                        "Service Générique", "Device Client", currentUser.getId(),
                        dateCalcul.toLocalDateTime().format(formatter), rs.getInt("ID_QOE")
                );

                qoeHistory.add(qoe);
                count++;
            }

            if (feedbackHistoryTable != null) {
                feedbackHistoryTable.setItems(qoeHistory);
            }

            System.out.println("✅ " + count + " donnée(s) QoE chargée(s) depuis la BD");

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement données QoE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gère la sélection d'une étoile
     */
    /**
     * Gère la sélection d'une étoile
     */
    @FXML
    private void handleStarSelection(javafx.event.ActionEvent event) {
        Button selectedButton = (Button) event.getSource();
        selectedRating = Integer.parseInt(selectedButton.getUserData().toString());

        System.out.println("⭐ Note sélectionnée: " + selectedRating + " étoiles");

        // Mettre à jour l'affichage
        updateStarButtons();
        updateRatingFeedback();

        // Animation
        animateStarSelection(selectedButton);
    }

    /**
     * Met à jour l'apparence des boutons étoiles
     */
    private void updateStarButtons() {
        // Réinitialiser tous les boutons
        resetStarButtons();

        // Activer les boutons jusqu'à la note sélectionnée
        for (int i = 1; i <= selectedRating; i++) {
            Button starButton = getStarButton(i);
            if (starButton != null) {
                starButton.setStyle("-fx-background-color: #fbbf24; -fx-background-radius: 10; -fx-border-color: #f59e0b;");
            }
        }
    }

    /**
     * Réinitialise l'apparence des boutons étoiles
     */
    private void resetStarButtons() {
        Button[] stars = {star1, star2, star3, star4, star5};
        for (Button star : stars) {
            if (star != null) {
                star.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 10; -fx-border-color: #d1d5db;");
            }
        }
    }

    /**
     * Obtient le bouton étoile correspondant au numéro
     */
    private Button getStarButton(int starNumber) {
        switch (starNumber) {
            case 1: return star1;
            case 2: return star2;
            case 3: return star3;
            case 4: return star4;
            case 5: return star5;
            default: return null;
        }
    }

    /**
     * Met à jour le feedback de la note sélectionnée
     */
    private void updateRatingFeedback() {
        if (selectedRatingLabel != null) {
            String[] messages = {
                    "Aucune note sélectionnée",
                    "😞 Nous sommes désolés de cette expérience",
                    "😕 Nous pouvons faire mieux",
                    "😐 Merci pour votre retour",
                    "😊 Heureux que vous soyez satisfait !",
                    "🤩 Excellent ! Merci beaucoup !"
            };

            if (selectedRating >= 1 && selectedRating <= 5) {
                selectedRatingLabel.setText(messages[selectedRating]);
                selectedRatingLabel.setStyle("-fx-text-fill: #1e293b; -fx-font-weight: bold;");
            } else {
                selectedRatingLabel.setText(messages[0]);
                selectedRatingLabel.setStyle("-fx-text-fill: #64748b;");
            }
        }
    }

    /**
     * Animation lors de la sélection d'une étoile
     */
    private void animateStarSelection(Button selectedButton) {
        if (selectedButton == null) return;

        ScaleTransition st = new ScaleTransition(Duration.millis(200), selectedButton);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.1);
        st.setToY(1.1);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }

    /**
     * Ajoute l'interactivité au formulaire
     */
    private void setupFormInteractivity() {
        System.out.println("✅ Listeners des étoiles initialisés");
    }

    /**
     * Met à jour le compteur de caractères
     */
    private void updateCharacterCount(int count) {
        if (charCount != null) {
            charCount.setText(String.valueOf(count));

            if (count > 500) {
                charCount.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            } else if (count > 450) {
                charCount.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
            } else {
                charCount.setStyle("-fx-text-fill: #64748b;");
            }
        }
    }

    private void setupCharts() {
        if (qoeTrendChart != null) {
            qoeTrendChart.setTitle("");
            qoeTrendChart.setCreateSymbols(true);
            qoeTrendChart.setAnimated(true);
            qoeTrendChart.setLegendVisible(false);
        }

        if (serviceDistributionChart != null) {
            serviceDistributionChart.setLabelsVisible(true);
            serviceDistributionChart.setAnimated(true);
            serviceDistributionChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        }
    }

    private void setupTableColumns() {
        if (feedbackHistoryTable != null && !feedbackHistoryTable.getColumns().isEmpty()) {
            System.out.println("✅ Colonnes du tableau initialisées");
        }
    }

    private void loadUserData() {
        loadQoeDataFromDatabase();
        if (qoeHistory.isEmpty()) {
            loadSampleData();
        }
    }

    private void loadSampleData() {
        qoeHistory.clear();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int userId = currentUser != null ? currentUser.getId() : 1;

        // Données d'exemple basées sur la structure QOE
        qoeHistory.add(new QoE(
                4.5, 4.2, 4.0, 4.3, 4.1, 4.2,  // Métriques QoE
                50.0, 10.0, 2.0, 25.0,         // Métriques QoS
                "📺 Streaming Vidéo", "Smartphone", userId,
                LocalDateTime.now().minusDays(5).format(formatter), 1
        ));

        qoeHistory.add(new QoE(
                4.2, 4.0, 3.8, 4.1, 4.0, 4.0,
                45.0, 8.0, 1.5, 28.0,
                "💬 Visioconférence", "Desktop", userId,
                LocalDateTime.now().minusDays(3).format(formatter), 2
        ));

        qoeHistory.add(new QoE(
                4.8, 4.5, 4.3, 4.6, 4.4, 4.5,
                35.0, 5.0, 0.8, 35.0,
                "🎮 Gaming en ligne", "Console", userId,
                LocalDateTime.now().minusDays(1).format(formatter), 3
        ));

        if (feedbackHistoryTable != null) {
            feedbackHistoryTable.setItems(qoeHistory);
        }

        System.out.println("✅ Données d'exemple chargées");
    }

    private void updateDashboardMetrics() {
        if (!qoeHistory.isEmpty()) {
            QoE latestQoE = qoeHistory.get(0); // Le plus récent

            setLabelValue(satisfactionScore, latestQoE.getSatisfactionScore(), "/5");
            setLabelValue(videoQualityLabel, latestQoE.getVideoQuality(), "/5");
            setLabelValue(audioQualityLabel, latestQoE.getAudioQuality(), "/5");
            setLabelValue(interactivityLabel, latestQoE.getInteractivity(), "/5");
            setLabelValue(reliabilityLabel, latestQoE.getReliability(), "/5");
            setLabelValue(loadingTimeLabel, latestQoE.getLoadingTime(), "ms");
            setLabelValue(bufferingLabel, latestQoE.getBuffering(), "ms");
            setLabelValue(failureRateLabel, latestQoE.getFailureRate(), "%");
            setLabelValue(streamingQualityLabel, latestQoE.getStreamingQuality(), "/5");
            setLabelValue(overallQoELabel, latestQoE.getOverallQoe(), "/5");
        }
    }

    private void setLabelValue(Label label, double value, String suffix) {
        if (label != null) {
            String formattedValue = String.format("%.1f%s", value, suffix);
            label.setText(formattedValue);

            // Animation du label
            animateLabel(label);
        }
    }

    /**
     * Anime le label lors de la mise à jour
     */
    private void animateLabel(Label label) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), label);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.play();
    }

    private void updateCharts() {
        if (qoeTrendChart == null || serviceDistributionChart == null) return;

        qoeTrendChart.getData().clear();
        serviceDistributionChart.getData().clear();

        // Graphique d'évolution QoE
        XYChart.Series<String, Number> qoeSeries = new XYChart.Series<>();
        qoeSeries.setName("Score QoE Global");

        for (int i = 0; i < qoeHistory.size(); i++) {
            QoE qoe = qoeHistory.get(i);
            String dateLabel = "J-" + (qoeHistory.size() - i - 1);
            qoeSeries.getData().add(new XYChart.Data<>(dateLabel, qoe.getOverallQoe()));
        }
        qoeTrendChart.getData().add(qoeSeries);

        // Graphique de répartition par service
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        java.util.Map<String, Long> serviceCount = new java.util.HashMap<>();
        for (QoE qoe : qoeHistory) {
            String service = qoe.getServiceType();
            serviceCount.put(service, serviceCount.getOrDefault(service, 0L) + 1);
        }

        for (java.util.Map.Entry<String, Long> entry : serviceCount.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        serviceDistributionChart.setData(pieData);
    }

    // ==================== MÉTHODES DE FEEDBACK ====================

    @FXML
    private void submitFeedback() {
        System.out.println("🔄 Début de l'envoi du feedback...");

        // Validation des champs obligatoires
        if (serviceTypeCombo.getValue() == null) {
            System.err.println("❌ Service non sélectionné");
            showAlert(Alert.AlertType.WARNING, "Champ manquant",
                    "Veuillez sélectionner un type de service.");
            return;
        }

        if (selectedRating == 0) {
            System.err.println("❌ Note non sélectionnée");
            showAlert(Alert.AlertType.WARNING, "Champ manquant",
                    "Veuillez sélectionner une note de satisfaction.");
            return;
        }

        String serviceType = serviceTypeCombo.getValue();
        String comment = commentField.getText();

        if (currentUser == null) {
            System.err.println("❌ Aucun utilisateur connecté");
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun utilisateur connecté.");
            return;
        }

        System.out.println("📋 Données du formulaire:");
        System.out.println("   - Client ID: " + currentUser.getId());
        System.out.println("   - Service: " + serviceType);
        System.out.println("   - Note: " + selectedRating + " étoiles");
        System.out.println("   - Commentaire: " + (comment.isEmpty() ? "Aucun" : comment.length() + " caractères"));

        // Validation de la longueur du commentaire
        if (comment.length() > 500) {
            showAlert(Alert.AlertType.WARNING, "Commentaire trop long",
                    "Le commentaire ne peut pas dépasser 500 caractères.");
            return;
        }

        // 👉 INSÉRER DANS LA TABLE QOE
        System.out.println("💾 Tentative d'insertion dans la table QOE...");
        boolean success = insertQoeData(currentUser.getId(), selectedRating, serviceType, comment);

        if (success) {
            System.out.println("✅ Données QoE insérées avec succès !");
            showSuccessAlert();
            resetFeedbackForm();

            // 👉 Recharger les données depuis la base
            loadQoeDataFromDatabase();

            // Mettre à jour l'interface
            updateDashboardMetrics();
            updateCharts();

        } else {
            System.err.println("❌ Échec de l'insertion des données QoE");
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'enregistrer les données dans la base de données.");
        }
    }

    /**
     * Insère des données dans la table QOE
     */
    private boolean insertQoeData(int clientId, int feedbackScore, String serviceType, String comment) {
        String sql = "INSERT INTO QOE (" +
                "ID_CLIENT, GENRE, " +
                "LATENCE_MOY, JITTER_MOY, PERTE_MOY, BANDE_PASSANTE_MOY, MOS_MOY, SIGNAL_MOY, " +
                "SATISFACTION_QOE, SERVICE_QOE, PRIX_QOE, CONTRAT_QOE, LIFETIME_QOE, " +
                "FEEDBACK_SCORE, QOE_GLOBAL" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Données client
            stmt.setInt(1, clientId);
            stmt.setString(2, "Client"); // Genre par défaut

            // Métriques QoS (simulées basées sur le feedback)
            stmt.setDouble(3, 50.0 - (feedbackScore * 5)); // Latence
            stmt.setDouble(4, 15.0 - (feedbackScore * 2)); // Jitter
            stmt.setDouble(5, 5.0 - (feedbackScore * 0.8)); // Perte
            stmt.setDouble(6, 20.0 + (feedbackScore * 3)); // Bande passante
            stmt.setDouble(7, 3.0 + (feedbackScore * 0.4)); // MOS
            stmt.setDouble(8, 70.0 + (feedbackScore * 6)); // Signal

            // Métriques QoE (basées sur le feedback)
            double satisfaction = feedbackScore;
            double serviceQoe = feedbackScore;
            double prixQoe = 4.0; // Valeur par défaut
            double contratQoe = 4.0; // Valeur par défaut
            double lifetimeQoe = 4.0; // Valeur par défaut

            stmt.setDouble(9, satisfaction);
            stmt.setDouble(10, serviceQoe);
            stmt.setDouble(11, prixQoe);
            stmt.setDouble(12, contratQoe);
            stmt.setDouble(13, lifetimeQoe);

            // Score de feedback
            stmt.setDouble(14, feedbackScore);

            // Score QoE global (calculé)
            double qoeGlobal = calculateOverallQoe(feedbackScore, serviceType);
            stmt.setDouble(15, qoeGlobal);

            System.out.println("📝 Exécution de la requête SQL...");
            int rowsAffected = stmt.executeUpdate();
            boolean success = rowsAffected > 0;

            if (success) {
                System.out.println("✅ " + rowsAffected + " ligne(s) affectée(s)");
                System.out.println("✅ Données QoE insérées pour le client " + clientId + " - Score global: " + qoeGlobal);
            } else {
                System.err.println("❌ Aucune ligne affectée");
            }

            return success;

        } catch (Exception e) {
            System.err.println("❌ Erreur insertion QoE: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Base de Données",
                    "Erreur lors de l'enregistrement: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calcule le score QoE global
     */
    private double calculateOverallQoe(int rating, String serviceType) {
        double baseScore = rating;

        // Pondération selon le type de service
        if (serviceType.contains("Vidéo") || serviceType.contains("Streaming")) {
            return Math.min(5.0, baseScore * 0.9 + 0.5);
        } else if (serviceType.contains("Gaming")) {
            return Math.min(5.0, baseScore * 0.85 + 0.6);
        } else if (serviceType.contains("Visio") || serviceType.contains("Audio")) {
            return Math.min(5.0, baseScore * 0.88 + 0.4);
        } else {
            return Math.min(5.0, baseScore * 0.95 + 0.3);
        }
    }

    @FXML
    public void resetFeedbackForm() {
        if (serviceTypeCombo != null) {
            serviceTypeCombo.setValue(null);
        }
        if (commentField != null) {
            commentField.clear();
        }

        // Réinitialiser les étoiles
        selectedRating = 0;
        resetStarButtons();
        updateRatingFeedback();

        // Réinitialiser le compteur de caractères
        updateCharacterCount(0);

        System.out.println("🔄 Formulaire réinitialisé");
    }

    @FXML
    private void handleLogout() {
        System.out.println("🚪 Déconnexion: " +
                (currentUser != null ? currentUser.getUsername() : "Unknown"));
        // Implémenter la logique de déconnexion
    }

    /**
     * Affiche une alerte de succès
     */
    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("✅ Succès");
        alert.setHeaderText("Évaluation enregistrée !");
        alert.setContentText("Vos données QoE ont été enregistrées avec succès dans notre base de données.");

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI';");

        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}