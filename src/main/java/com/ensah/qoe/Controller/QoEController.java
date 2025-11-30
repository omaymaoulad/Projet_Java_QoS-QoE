package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.QoE;
import com.ensah.qoe.Services.QoeAnalyzer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class QoEController implements Initializable {

    // ==== Filtres ====
    @FXML private RadioButton radioClient;
    @FXML private RadioButton radioGenre;
    @FXML private RadioButton radioZone;

    private ToggleGroup analyseGroup;

    @FXML private HBox clientSelectionBox;
    @FXML private HBox genreSelectionBox;
    @FXML private HBox zoneSelectionBox;

    @FXML private ComboBox<String> clientCombo;
    @FXML private ComboBox<String> genreCombo;
    @FXML private ComboBox<String> zoneCombo;

    @FXML private Button importCsvButton;

    // Conteneur dynamique
    @FXML private VBox tableContainer;
    @FXML private VBox graphContainer;

    @FXML private TableView<ClientRow> clientTable;

    // Labels
    @FXML private Label satisfactionLabel;
    @FXML private Label videoQualityLabel;
    @FXML private Label audioQualityLabel;
    @FXML private Label interactivityLabel;
    @FXML private Label reliabilityLabel;
    @FXML private Label overallQoeLabel;

    @FXML private Label bufferingLabel;
    @FXML private Label loadingTimeLabel;
    @FXML private Label failureRateLabel;
    @FXML private Label streamingQualityLabel;

    // =====================================================================
    // INITIALISATION
    // =====================================================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        analyseGroup = new ToggleGroup();
        radioClient.setToggleGroup(analyseGroup);
        radioGenre.setToggleGroup(analyseGroup);
        radioZone.setToggleGroup(analyseGroup);

        hideAllSelectionBoxes();
        hideAllViews();

        loadFilters();
        setupClientTable();

        // Actions des ComboBox
        genreCombo.setOnAction(e -> afficherQoeParGenre(genreCombo.getValue()));

        clientCombo.setOnAction(e -> {
            String s = clientCombo.getValue();
            if (s != null) {
                int id = Integer.parseInt(s.split(" - ")[0]);
                afficherQoeParClient(id);
            }
        });

        // Action pour le ComboBox de zone
        zoneCombo.setOnAction(e -> {
            String zone = zoneCombo.getValue();
            if (zone != null) {
                afficherQoeParZone(zone);
            }
        });
    }

    // =====================================================================
    // IMPORT CSV
    // =====================================================================
    @FXML
    private void importerCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un CSV Telco");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv")
        );

        File f = chooser.showOpenDialog(importCsvButton.getScene().getWindow());
        if (f == null) return;

        boolean ok = QoeAnalyzer.analyserFichierCsv(f.getAbsolutePath());

        if (!ok) {
            overallQoeLabel.setText("Erreur");
            return;
        }

        overallQoeLabel.setText("CSV chargé !");
        loadFilters();
        refreshClientTable();
    }

    // =====================================================================
    // MODES (CLIENT / GENRE / ZONE)
    // =====================================================================
    @FXML
    private void modeClient() {
        hideAllSelectionBoxes();
        hideAllViews();

        clientSelectionBox.setVisible(true);
        tableContainer.setVisible(true);
    }

    @FXML
    private void modeGenre() {
        hideAllSelectionBoxes();
        hideAllViews();

        genreSelectionBox.setVisible(true);
        graphContainer.setVisible(true);
    }

    @FXML
    public void modeZone() {
        hideAllSelectionBoxes();
        hideAllViews();

        zoneSelectionBox.setVisible(true);

        // Ouvrir directement la carte avec toutes les zones
        ouvrirCarteComplete();
    }

    // =====================================================================
    // OUVERTURE DE LA CARTE
    // =====================================================================
    private void ouvrirCarteComplete() {
        try {
            // Charger le FXML de la carte
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/map_geographique.fxml"));
            Parent root = loader.load();

            // Obtenir le contrôleur de la carte
            MapGeographiqueController mapController = loader.getController();

            // Ne pas définir de filtre de zone (afficher tous les clients)
            // mapController.setZoneFiltre(null);

            // Créer la nouvelle fenêtre
            Stage mapStage = new Stage();
            mapStage.setTitle("Carte QoE - Tous les clients");
            mapStage.setScene(new Scene(root, 1200, 800));
            mapStage.setMaximized(true);

            mapStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerteErreur("Erreur", "Impossible d'ouvrir la carte géographique");
        }
    }

    private void ouvrirCarteZone(String zone) {
        try {
            // Charger le FXML de la carte
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/map_geographique.fxml"));
            Parent root = loader.load();

            // Obtenir le contrôleur de la carte
            MapGeographiqueController mapController = loader.getController();

            // Passer la zone sélectionnée au contrôleur de la carte
            mapController.setZoneFiltre(zone);

            // Créer la nouvelle fenêtre
            Stage mapStage = new Stage();
            mapStage.setTitle("Carte QoE - " + zone);
            mapStage.setScene(new Scene(root, 1200, 800));
            mapStage.setMaximized(true);

            mapStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerteErreur("Erreur", "Impossible d'ouvrir la carte pour la zone: " + zone);
        }
    }

    private void afficherAlerteErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =====================================================================
    // FILTRES
    // =====================================================================
    private void loadFilters() {

        genreCombo.getItems().clear();
        zoneCombo.getItems().clear();
        clientCombo.getItems().clear();

        try (Connection conn = DBConnection.getConnection()) {

            // Genre
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DISTINCT GENRE FROM CLIENT ORDER BY GENRE")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) genreCombo.getItems().add(rs.getString(1));
            }

            // Zones
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DISTINCT LOCALISATION_ZONE FROM CLIENT WHERE LOCALISATION_ZONE IS NOT NULL ORDER BY LOCALISATION_ZONE")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String zone = rs.getString(1);
                    if (zone != null && !zone.trim().isEmpty()) {
                        zoneCombo.getItems().add(zone);
                    }
                }
            }

            // Clients
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ID_CLIENT, NOM FROM CLIENT ORDER BY NOM")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    clientCombo.getItems().add(
                            rs.getInt(1) + " - " + rs.getString(2)
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =====================================================================
    // AFFICHAGE CLIENT / GENRE / ZONE
    // =====================================================================
    private void afficherQoeParClient(int id) {
        QoE q = QoeAnalyzer.analyserParClient(id);
        if (q != null) afficherQoeDansInterface(q);
        else overallQoeLabel.setText("Erreur");
    }

    private void afficherQoeParGenre(String g) {
        QoE q = QoeAnalyzer.analyserParGenre(g);
        if (q != null) {
            afficherQoeDansInterface(q);
            afficherGraphiqueGenre();
        } else overallQoeLabel.setText("Erreur");
    }

    private void afficherQoeParZone(String z) {
        QoE q = QoeAnalyzer.analyserParZone(z);
        if (q != null) {
            afficherQoeDansInterface(q);
            // Ouvrir la carte filtrée pour cette zone
            ouvrirCarteZone(z);
        } else overallQoeLabel.setText("Erreur");
    }

    // =====================================================================
    // TABLE CLIENTS
    // =====================================================================
    private void setupClientTable() {
        TableColumn<ClientRow, Number> c1 = new TableColumn<>("ID");
        c1.setCellValueFactory(x -> x.getValue().idClientProperty());

        TableColumn<ClientRow, String> c2 = new TableColumn<>("Nom");
        c2.setCellValueFactory(x -> x.getValue().nomProperty());

        TableColumn<ClientRow, String> c3 = new TableColumn<>("Genre");
        c3.setCellValueFactory(x -> x.getValue().genreProperty());

        TableColumn<ClientRow, String> c4 = new TableColumn<>("Zone");
        c4.setCellValueFactory(x -> x.getValue().zoneProperty());

        TableColumn<ClientRow, Number> c5 = new TableColumn<>("QoE");
        c5.setCellValueFactory(x -> x.getValue().qoeGlobalProperty());

        clientTable.getColumns().setAll(c1, c2, c3, c4, c5);
        refreshClientTable();
    }

    private void refreshClientTable() {
        clientTable.setItems(loadClientRows());
    }

    private ObservableList<ClientRow> loadClientRows() {
        ObservableList<ClientRow> list = FXCollections.observableArrayList();

        String sql =
                """
                SELECT c.ID_CLIENT, c.NOM, c.GENRE, c.LOCALISATION_ZONE,
                       NVL(MAX(q.QOE_GLOBAL), 0)
                FROM CLIENT c
                LEFT JOIN QOE q ON c.ID_CLIENT = q.ID_CLIENT
                GROUP BY c.ID_CLIENT, c.NOM, c.GENRE, c.LOCALISATION_ZONE
                ORDER BY c.NOM
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new ClientRow(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getDouble(5)
                ));
            }

        } catch (Exception e) { e.printStackTrace(); }

        return list;
    }

    // =====================================================================
    // GRAPHIQUE GENRE
    // =====================================================================
    private void afficherGraphiqueGenre() {
        graphContainer.getChildren().clear();

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();

        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setTitle("QoE par Genre");

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("QoE Global");

        QoE male = QoeAnalyzer.analyserParGenre("Male");
        QoE female = QoeAnalyzer.analyserParGenre("Female");

        if (male != null)
            serie.getData().add(new XYChart.Data<>("Male", male.getQoeGlobal()));

        if (female != null)
            serie.getData().add(new XYChart.Data<>("Female", female.getQoeGlobal()));

        chart.getData().add(serie);
        graphContainer.getChildren().add(chart);
    }

    // =====================================================================
    // AFFICHAGE DES METRIQUES
    // =====================================================================
    private void afficherQoeDansInterface(QoE q) {
        satisfactionLabel.setText(format5(q.getSatisfactionQoe()));
        videoQualityLabel.setText(format5(q.getServiceQoe()));
        audioQualityLabel.setText(format5(q.getPrixQoe()));
        interactivityLabel.setText(format5(q.getContratQoe()));
        reliabilityLabel.setText(format5(q.getLifetimeQoe()));

        overallQoeLabel.setText(format5(q.getQoeGlobal()));

        loadingTimeLabel.setText(q.getLatenceMoy() + " ms");
        bufferingLabel.setText(q.getJitterMoy() + " ms");
        failureRateLabel.setText(q.getPerteMoy() + " %");
        streamingQualityLabel.setText(q.getBandePassanteMoy() + " Mbps");
    }

    private String format5(double d) { return String.format("%.2f / 5", d); }

    // Hide all containers
    private void hideAllViews() {
        tableContainer.setVisible(false);
        graphContainer.setVisible(false);
    }

    private void hideAllSelectionBoxes() {
        clientSelectionBox.setVisible(false);
        genreSelectionBox.setVisible(false);
        zoneSelectionBox.setVisible(false);
    }

    // =====================================================================
    // CLASSES INTERNES
    // =====================================================================
    public static class ClientRow {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty nom = new SimpleStringProperty();
        private final StringProperty genre = new SimpleStringProperty();
        private final StringProperty zone = new SimpleStringProperty();
        private final DoubleProperty qoe = new SimpleDoubleProperty();

        public ClientRow(int i, String n, String g, String z, double q) {
            id.set(i);
            nom.set(n);
            genre.set(g);
            zone.set(z);
            qoe.set(q);
        }

        public IntegerProperty idClientProperty() { return id; }
        public StringProperty nomProperty() { return nom; }
        public StringProperty genreProperty() { return genre; }
        public StringProperty zoneProperty() { return zone; }
        public DoubleProperty qoeGlobalProperty() { return qoe; }
    }
}