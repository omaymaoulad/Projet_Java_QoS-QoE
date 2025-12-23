package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.QoE;
import com.ensah.qoe.Models.User;
import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.Feedback;
import com.ensah.qoe.Services.*;
import javafx.animation.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.time.LocalDateTime;

public class ClientDashboardController implements Initializable {

    private User currentUser;

    // Labels QoE
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

    // Feedback
    @FXML private ComboBox<String> serviceTypeCombo;
    @FXML private ToggleGroup ratingGroup;
    @FXML private TextArea commentField;
    @FXML private Label charCountLabel;
    // Table historique
    @FXML private TableView<Feedback> feedbackHistoryTable;
    @FXML private TableColumn<Feedback, String> colDate;
    @FXML private TableColumn<Feedback, String> colService;
    @FXML private TableColumn<Feedback, String> colScore;
    @FXML private TableColumn<Feedback, String> colComment;

    private ObservableList<QoE> qoeHistory = FXCollections.observableArrayList();

    // -------------------- INITIALISATION ---------------------
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        setupServiceComboBox();
        setupCharts();
        setupHistoryTable();
        setupCommentField();
    }

    public void setUserData(User user) {
        this.currentUser = user;
        usernameLabel.setText(user.getUsername());
        loadUserQoEData();
    }

    // -------------------- UI CONFIG -------------------------
    private void setupServiceComboBox() {
        serviceTypeCombo.getItems().addAll(
                "📺 Streaming Vidéo",
                "🎵 Streaming Audio",
                "🎮 Gaming en ligne",
                "💬 Visioconférence",
                "🌐 Navigation Web",
                "☁️ Cloud Storage",
                "📱 Application Mobile"
        );
    }

    private void setupCharts() {
        qoeTrendChart.setAnimated(true);
        qoeTrendChart.setLegendVisible(false);
        serviceDistributionChart.setLegendVisible(true);
    }

    private void setupCommentField() {
        commentField.textProperty().addListener((obs, old, neu) -> {
            if (neu.length() > 500) {
                commentField.setText(old);
            } else {
                charCountLabel.setText(neu.length() + "");
            }
        });
    }

    // -------------------- HISTORIQUE -------------------------
    private void setupHistoryTable() {
        // ✅ SOLUTION : Désactiver la colonne vide
        feedbackHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colDate.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getDate() != null ?
                                cell.getValue().getDate().toString() : ""
                )
        );

        colService.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getService())
        );

        colScore.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getScore() + " / 5")
        );

        colComment.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCommentaire())
        );
    }

    private ObservableList<Feedback> loadFeedbackHistory(int clientId) {

        ObservableList<Feedback> list = FXCollections.observableArrayList();

        String sql = """
            SELECT SERVICE, SCORE, COMMENTAIRE, FEEDBACK_DATE
            FROM FEEDBACKS
            WHERE CLIENT_NAME = (SELECT NOM FROM CLIENT WHERE ID_CLIENT = ?)
            ORDER BY FEEDBACK_DATE DESC
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Feedback f = new Feedback();
                f.setService(rs.getString("SERVICE"));
                f.setScore(rs.getDouble("SCORE"));
                f.setCommentaire(rs.getString("COMMENTAIRE"));

                Timestamp ts = rs.getTimestamp("FEEDBACK_DATE");
                if (ts != null) f.setDate(ts.toLocalDateTime());

                list.add(f);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // -------------------- QOE GLOBAL -------------------------
    private void loadUserQoEData() {

        int clientId = ClientService.getClientIdByUserId(currentUser.getId());
        if (clientId == -1) return;

        QoE qoe = QoeAnalyzer.analyserParClient(clientId);

        if (qoe != null) {
            qoeHistory.clear();
            qoeHistory.add(qoe);
            updateDashboardMetrics();
            updateCharts();
        }

        // Charger historique
        feedbackHistoryTable.setItems(loadFeedbackHistory(clientId));
    }

    private void updateDashboardMetrics() {

        QoE q = qoeHistory.get(0);

        satisfactionScore.setText(q.getSatisfactionQoe() + "/5");
        videoQualityLabel.setText(q.getServiceQoe() + "/5");
        audioQualityLabel.setText(q.getPrixQoe() + "/5");
        interactivityLabel.setText(q.getContratQoe() + "/5");
        reliabilityLabel.setText(q.getLifetimeQoe() + "/5");

        loadingTimeLabel.setText(q.getLatenceMoy() + " ms");
        bufferingLabel.setText(q.getJitterMoy() + " ms");
        failureRateLabel.setText(q.getPerteMoy() + "%");
        streamingQualityLabel.setText(q.getBandePassanteMoy() + " Mbps");

        overallQoELabel.setText(q.getQoeGlobal() + "/5");
    }

    private void updateCharts() {
        QoE q = qoeHistory.get(0);

        qoeTrendChart.getData().clear();

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.getData().add(new XYChart.Data<>("Score", q.getQoeGlobal()));

        qoeTrendChart.getData().add(s);

        serviceDistributionChart.getData().clear();
        serviceDistributionChart.getData().add(new PieChart.Data("Satisfaction", q.getSatisfactionQoe()));
        serviceDistributionChart.getData().add(new PieChart.Data("Service", q.getServiceQoe()));
        serviceDistributionChart.getData().add(new PieChart.Data("Prix", q.getPrixQoe()));
        serviceDistributionChart.getData().add(new PieChart.Data("Contrat", q.getContratQoe()));
        serviceDistributionChart.getData().add(new PieChart.Data("Lifetime", q.getLifetimeQoe()));
    }

    // -------------------- ENVOI FEEDBACK -------------------------
    @FXML
    private void submitFeedback() {

        RadioButton selected = (RadioButton) ratingGroup.getSelectedToggle();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Choisissez une note !");
            return;
        }

        int score = Integer.parseInt(selected.getUserData().toString());
        String service = serviceTypeCombo.getValue();
        String commentaire = commentField.getText();
        int clientId = ClientService.getClientIdByUserId(currentUser.getId());

        try (Connection conn = DBConnection.getConnection()) {

            PreparedStatement ps1 = conn.prepareStatement(
                    "UPDATE QOE SET FEEDBACK_SCORE = ? WHERE ID_CLIENT = ?"
            );
            ps1.setInt(1, score);
            ps1.setInt(2, clientId);
            ps1.executeUpdate();

            String sql = """
                INSERT INTO FEEDBACKS (ID, CLIENT_NAME, SERVICE, SCORE, COMMENTAIRE, FEEDBACK_DATE)
                SELECT FEEDBACKS_SEQ.NEXTVAL, NOM, ?, ?, ?, SYSDATE
                FROM CLIENT WHERE ID_CLIENT = ?
            """;

            PreparedStatement ps2 = conn.prepareStatement(sql);
            ps2.setString(1, service);
            ps2.setInt(2, score);
            ps2.setString(3, commentaire);
            ps2.setInt(4, clientId);
            ps2.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Enregistrement impossible.");
            return;
        }

        loadUserQoEData();
        resetFeedbackForm();
        showSuccessAlert();
    }

    private void resetFeedbackForm() {
        serviceTypeCombo.setValue(null);
        ratingGroup.selectToggle(null);
        commentField.clear();
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Évaluation enregistrée !");
        alert.setContentText("Merci pour votre feedback.");
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    @FXML
    private void handleLogout() {
        System.out.println("🚪 Déconnexion de client: " +
                (currentUser != null ? currentUser.getUsername() : "Unknown"));

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de déconnexion");
        confirmation.setHeaderText("Déconnexion");
        confirmation.setContentText("Êtes-vous sûr de vouloir vous déconnecter ?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                Stage stage = (Stage) usernameLabel.getScene().getWindow();
                stage.close();
                openLoginWindow();
            }
        });
    }
    private void openLoginWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Connexion - QoE System");
            loginStage.setScene(new Scene(root,1366,700));
            loginStage.show();

        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}
