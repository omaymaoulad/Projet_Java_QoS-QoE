/*package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.QoE;
import com.ensah.qoe.Models.User;
import javafx.animation.*;
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
import java.util.ResourceBundle;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientDashboardController implements Initializable {

   private User currentUser;

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

    // Graphiques
    @FXML private LineChart<String, Number> qoeTrendChart;
    @FXML private PieChart serviceDistributionChart;

    // Formulaire de feedback
    @FXML private ComboBox<String> serviceTypeCombo;
    @FXML private ToggleGroup ratingGroup;
    @FXML private TextArea commentField;
    @FXML private TableView<QoE> feedbackHistoryTable;

    // Données
    private ObservableList<QoE> qoeHistory = FXCollections.observableArrayList();

    public void setUserData(User user) {
        this.currentUser = user;
        if (usernameLabel != null && user != null) {
            usernameLabel.setText(user.getUsername());
            loadUserQoEData();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("✅ ClientDashboardController initialisé");

        initializeComponents();
        setupFormInteractivity();
        loadSampleData();
    }

    private void initializeComponents() {
        // Initialiser le ComboBox avec des icônes
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

            // Ajouter un listener pour le changement de sélection
            serviceTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    animateComboSelection();
                }
            });
        }

        // Configurer les graphiques
        setupCharts();

        // Configurer le tableau d'historique
        setupTableColumns();

        // Configurer le TextArea avec compteur de caractères
        setupCommentField();
    }

    /**
     * Ajoute l'interactivité au formulaire

    private void setupFormInteractivity() {
        // Animer les RadioButtons lors de la sélection
        if (ratingGroup != null) {
            ratingGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                if (newToggle != null) {
                    animateStarSelection((RadioButton) newToggle);
                }
            });
        }
    }

    /**
     * Configure le champ de commentaire avec compteur

    private void setupCommentField() {
        if (commentField != null) {
            commentField.textProperty().addListener((obs, oldText, newText) -> {
                // Limiter à 500 caractères
                if (newText.length() > 500) {
                    commentField.setText(oldText);
                } else {
                    updateCharacterCount(newText.length());
                }
            });

            // Style au focus
            commentField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (isNowFocused) {
                    commentField.setStyle(commentField.getStyle() +
                            "-fx-border-color: #667eea; -fx-border-width: 2;");
                } else {
                    commentField.setStyle(commentField.getStyle() +
                            "-fx-border-color: #e2e8f0; -fx-border-width: 2;");
                }
            });
        }
    }

    /**
     * Met à jour le compteur de caractères (vous pouvez ajouter un Label dans le FXML)

    private void updateCharacterCount(int count) {
        // Si vous ajoutez un Label fx:id="characterCountLabel" dans le FXML
        // characterCountLabel.setText(count + " / 500 caractères");
        System.out.println("Caractères: " + count + "/500");
    }

    /**
     * Animation lors de la sélection du ComboBox

    private void animateComboSelection() {
        if (serviceTypeCombo != null) {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), serviceTypeCombo);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.05);
            st.setToY(1.05);
            st.setCycleCount(2);
            st.setAutoReverse(true);
            st.play();
        }
    }

    /**
     * Animation lors de la sélection d'une étoile

    private void animateStarSelection(RadioButton selectedRadio) {
        if (selectedRadio == null) return;

        // Animation de rebond
        ScaleTransition st = new ScaleTransition(Duration.millis(200), selectedRadio);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.2);
        st.setToY(1.2);
        st.setCycleCount(2);
        st.setAutoReverse(true);

        // Animation de rotation
        RotateTransition rt = new RotateTransition(Duration.millis(400), selectedRadio);
        rt.setByAngle(360);

        ParallelTransition pt = new ParallelTransition(st, rt);
        pt.play();

        // Afficher un message selon le rating
        int rating = Integer.parseInt(selectedRadio.getUserData().toString());
        showRatingFeedback(rating);
    }


    private void showRatingFeedback(int rating) {
        String[] messages = {
                "",
                "😞 Nous sommes désolés de cette expérience",
                "😕 Nous pouvons faire mieux",
                "😐 Merci pour votre retour",
                "😊 Heureux que vous soyez satisfait !",
                "🤩 Excellent ! Merci beaucoup !"
        };

        if (rating >= 1 && rating <= 5) {
            System.out.println("Rating sélectionné: " + rating + " - " + messages[rating]);
            // Vous pouvez afficher ce message dans un Label temporaire
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
            TableColumn<QoE, String> dateColumn = (TableColumn<QoE, String>) feedbackHistoryTable.getColumns().get(0);
            TableColumn<QoE, String> serviceColumn = (TableColumn<QoE, String>) feedbackHistoryTable.getColumns().get(1);
            TableColumn<QoE, Double> scoreColumn = (TableColumn<QoE, Double>) feedbackHistoryTable.getColumns().get(2);
            TableColumn<QoE, Double> videoColumn = (TableColumn<QoE, Double>) feedbackHistoryTable.getColumns().get(3);
            TableColumn<QoE, Double> audioColumn = (TableColumn<QoE, Double>) feedbackHistoryTable.getColumns().get(4);

            dateColumn.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTimestamp()));
            serviceColumn.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().getServiceType()));
            scoreColumn.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getOverallQoe()).asObject());
            videoColumn.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getVideoQuality()).asObject());
            audioColumn.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getAudioQuality()).asObject());

            // Personnaliser l'affichage des cellules avec des couleurs
            scoreColumn.setCellFactory(col -> new TableCell<QoE, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(String.format("%.1f / 5", item));
                        // Couleur selon le score
                        if (item >= 4.5) {
                            setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        } else if (item >= 3.5) {
                            setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                        } else if (item >= 2.5) {
                            setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }
    }

    private void loadUserQoEData() {
        loadSampleData();
    }

    private void loadSampleData() {
        qoeHistory.clear();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int userId = currentUser != null ? currentUser.getId() : 1;

        // Données d'exemple variées
        qoeHistory.add(new QoE(
                4.5, 4.8, 4.7, 4.2, 4.6, 4.5,
                1.2, 2.3, 2.5, 4.6,
                "📺 Streaming Vidéo", "Smartphone", userId,
                LocalDateTime.now().minusDays(5).format(formatter), 1
        ));

        qoeHistory.add(new QoE(
                4.2, 4.5, 4.3, 4.0, 4.4, 4.2,
                2.1, 3.1, 3.8, 4.3,
                "💬 Visioconférence", "Desktop", userId,
                LocalDateTime.now().minusDays(3).format(formatter), 2
        ));

        qoeHistory.add(new QoE(
                4.8, 4.9, 4.8, 4.5, 4.9, 4.8,
                0.8, 1.5, 1.2, 4.9,
                "🎮 Gaming en ligne", "Console", userId,
                LocalDateTime.now().minusDays(1).format(formatter), 3
        ));

        qoeHistory.add(new QoE(
                3.8, 4.0, 3.9, 3.5, 3.7, 3.8,
                3.2, 4.5, 5.1, 3.6,
                "🌐 Navigation Web", "Tablet", userId,
                LocalDateTime.now().format(formatter), 4
        ));

        updateDashboardMetrics();
        updateCharts();

        if (feedbackHistoryTable != null) {
            feedbackHistoryTable.setItems(qoeHistory);
        }
    }

    private void updateDashboardMetrics() {
        if (!qoeHistory.isEmpty()) {
            QoE latestQoE = qoeHistory.get(qoeHistory.size() - 1);

            setLabelValue(satisfactionScore, latestQoE.getSatisfactionScore(), "/5");
            setLabelValue(videoQualityLabel, latestQoE.getVideoQuality(), "/5");
            setLabelValue(audioQualityLabel, latestQoE.getAudioQuality(), "/5");
            setLabelValue(interactivityLabel, latestQoE.getInteractivity(), "/5");
            setLabelValue(reliabilityLabel, latestQoE.getReliability(), "/5");
            setLabelValue(loadingTimeLabel, latestQoE.getLoadingTime(), "s");
            setLabelValue(bufferingLabel, latestQoE.getBuffering(), "s");
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
        if (serviceTypeCombo == null || ratingGroup == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Composants non initialisés");
            return;
        }

        String serviceType = serviceTypeCombo.getValue();
        RadioButton selectedRadio = (RadioButton) ratingGroup.getSelectedToggle();

        if (serviceType == null || selectedRadio == null) {
            showAlert(Alert.AlertType.WARNING, "Attention",
                    "Veuillez remplir tous les champs obligatoires:\n• Type de service\n• Note de satisfaction");
            return;
        }

        int rating = Integer.parseInt(selectedRadio.getUserData().toString());
        String comment = commentField != null ? commentField.getText() : "";

        // Créer un nouvel objet QoE
        QoE newQoE = createQoEFromFeedback(rating, serviceType, comment);
        qoeHistory.add(newQoE);

        // Mettre à jour l'interface avec animation
        updateDashboardMetrics();
        updateCharts();

        // Réinitialiser le formulaire
        resetFeedbackForm();

        // Afficher message de succès
        showSuccessAlert();
    }


    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("✅ Succès");
        alert.setHeaderText("Évaluation enregistrée !");
        alert.setContentText("Merci pour votre feedback. Vos données ont été enregistrées avec succès.");

        // Personnaliser le style
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI';");

        alert.showAndWait();
    }

    private QoE createQoEFromFeedback(int rating, String serviceType, String comment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int userId = currentUser != null ? currentUser.getId() : 1;

        double satisfaction = rating;
        double videoQuality = serviceType.contains("Vidéo") ? rating : 4.0;
        double audioQuality = serviceType.contains("Audio") || serviceType.contains("Visio") ? rating : 4.0;
        double interactivity = rating >= 4 ? 4.5 : 3.5;
        double reliability = rating;
        double overallQoe = calculateOverallQoE(rating, serviceType);

        return new QoE(
                satisfaction, videoQuality, audioQuality, interactivity,
                reliability, overallQoe, 1.5, 2.0, 2.0, 4.0,
                serviceType, "Mobile", userId,
                LocalDateTime.now().format(formatter), qoeHistory.size() + 1
        );
    }

    private double calculateOverallQoE(int rating, String serviceType) {
        double baseScore = rating;

        if (serviceType.contains("Vidéo")) {
            return Math.min(5.0, baseScore * 0.9 + 0.5);
        } else if (serviceType.contains("Gaming")) {
            return Math.min(5.0, baseScore * 0.85 + 0.6);
        } else {
            return Math.min(5.0, baseScore * 0.95 + 0.3);
        }
    }

    private void resetFeedbackForm() {
        if (serviceTypeCombo != null) {
            serviceTypeCombo.setValue(null);
        }
        if (ratingGroup != null) {
            ratingGroup.selectToggle(null);
        }
        if (commentField != null) {
            commentField.clear();
        }
    }

    @FXML
    private void handleLogout() {
        System.out.println("🚪 Déconnexion: " +
                (currentUser != null ? currentUser.getUsername() : "Unknown"));
        // Implémenter la logique de déconnexion
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}*/