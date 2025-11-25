package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.QoE;
import com.ensah.qoe.Services.QoeAnalyzer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    @FXML private VBox tableContainer;
    @FXML private VBox graphContainer;
    @FXML private VBox mapContainer;

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


    // =========================================================================
    // INITIALISATION
    // =========================================================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        analyseGroup = new ToggleGroup();
        radioClient.setToggleGroup(analyseGroup);
        radioGenre.setToggleGroup(analyseGroup);
        radioZone.setToggleGroup(analyseGroup);

        hideAllSelectionBoxes();
        hideAllViews();

        chargerFiltresDepuisBD();
        preparerTableClients();

        genreCombo.setOnAction(e -> afficherQoeParGenre(genreCombo.getValue()));
        zoneCombo.setOnAction(e -> afficherQoeParZone(zoneCombo.getValue()));

        clientCombo.setOnAction(e -> {
            String s = clientCombo.getValue();
            if (s != null) {
                int id = Integer.parseInt(s.split(" - ")[0]);
                afficherQoeParClient(id);
            }
        });
    }


    // =========================================================================
    // IMPORT CSV
    // =========================================================================
    @FXML
    private void importerCsv() {


        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un CSV Telco");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv")
        );

        File f = chooser.showOpenDialog(importCsvButton.getScene().getWindow());
        if (f == null) return;

        // Nom du fichier
        String nomFichier = f.getName();

        boolean fichierDejaImporte =
                com.ensah.qoe.Services.FichierService.fichierExiste(nomFichier);

        boolean ok = QoeAnalyzer.analyserFichierCsv(f.getAbsolutePath());

        if (!ok) {
            afficherErreur();
            return;
        }

        // ==============================
        // ✔ Affichage intelligent
        // ==============================
        if (fichierDejaImporte) {
            overallQoeLabel.setText("Données chargées depuis la base !");
        } else {
            overallQoeLabel.setText("CSV chargé !");
        }

        // Mise à jour UI
        chargerFiltresDepuisBD();
        rafraichirTableClients();
    }


    // =========================================================================
    // MODES D'ANALYSE
    // =========================================================================
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
    private void modeZone() {
        hideAllSelectionBoxes();
        hideAllViews();
        zoneSelectionBox.setVisible(true);
        mapContainer.setVisible(true);
    }


    // =========================================================================
    // FILTRES
    // =========================================================================
    private void chargerFiltresDepuisBD() {

        genreCombo.getItems().clear();
        zoneCombo.getItems().clear();
        clientCombo.getItems().clear();

        try (Connection conn = DBConnection.getConnection()) {

            // Genres
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DISTINCT GENRE FROM CLIENT ORDER BY GENRE")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) genreCombo.getItems().add(rs.getString(1));
            }

            // Zones
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DISTINCT ZONE FROM MESURES_QOS ORDER BY ZONE")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) zoneCombo.getItems().add(rs.getString(1));
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

        } catch (Exception e) { e.printStackTrace(); }
    }


    // =========================================================================
    // AFFICHAGE CLIENT / GENRE / ZONE
    // =========================================================================
    private void afficherQoeParClient(int id) {

        QoE q = QoeAnalyzer.analyserParClient(id);

        if (q != null) afficherQoeDansInterface(q);
        else afficherErreur();
    }

    private void afficherQoeParGenre(String g) {

        QoE q = QoeAnalyzer.analyserParGenre(g);

        if (q != null) {
            afficherQoeDansInterface(q);
            afficherGraphiqueGenre();
        } else afficherErreur();
    }

    private void afficherQoeParZone(String z) {

        QoE q = QoeAnalyzer.analyserParZone(z);

        if (q != null) {
            afficherQoeDansInterface(q);
            afficherCartePourZone(z, q);
        } else afficherErreur();
    }


    // =========================================================================
    // TABLE DES CLIENTS
    // =========================================================================
    private void preparerTableClients() {

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
        rafraichirTableClients();
    }

    private void rafraichirTableClients() {
        clientTable.setItems(chargerClientsAvecQoe());
    }

    private ObservableList<ClientRow> chargerClientsAvecQoe() {

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

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }


    // =========================================================================
    // GRAPHIQUE GENRE
    // =========================================================================
    private void afficherGraphiqueGenre() {

        graphContainer.getChildren().clear();

        // ==========================
        // 1) Récupération des QoE pour Male et Female
        // ==========================
        QoE qMale = QoeAnalyzer.analyserParGenre("Male");
        QoE qFemale = QoeAnalyzer.analyserParGenre("Female");

        // ==========================
        // 2) Préparation du graphique
        // ==========================
        CategoryAxis x = new CategoryAxis();
        x.setLabel("Genre");

        NumberAxis y = new NumberAxis();
        y.setLabel("QoE Global (1 à 5)");

        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setTitle("Comparaison QoE Global : Male vs Female");

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("QoE Global");

        // ==========================
        // 3) Ajout des valeurs
        // ==========================
        if (qMale != null) {
            serie.getData().add(new XYChart.Data<>("Male", qMale.getQoeGlobal()));
        }

        if (qFemale != null) {
            serie.getData().add(new XYChart.Data<>("Female", qFemale.getQoeGlobal()));
        }

        // ==========================
        // 4) Affichage
        // ==========================
        chart.getData().add(serie);
        graphContainer.getChildren().add(chart);
    }


    // =========================================================================
    // CARTE ZONE
    // =========================================================================
    private void afficherCartePourZone(String zone, QoE q) {

        mapContainer.getChildren().clear();

        Label title = new Label("📍 Zone : " + zone);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        Label qos = new Label(String.format(
                "Latence %.2f ms • Jitter %.2f ms • Perte %.2f %% • Bande passante %.2f Mbps",
                q.getLatenceMoy(), q.getJitterMoy(),
                q.getPerteMoy(), q.getBandePassanteMoy()
        ));

        Label note = new Label("QoE global : " + format5(q.getQoeGlobal()));

        mapContainer.getChildren().addAll(title, qos, note);
    }


    // =========================================================================
    // AFFICHAGE DES LABELS
    // =========================================================================
    private void afficherQoeDansInterface(QoE q) {

        satisfactionLabel.setText(format5(q.getSatisfactionQoe()));
        videoQualityLabel.setText(format5(q.getServiceQoe()));
        audioQualityLabel.setText(format5(q.getPrixQoe()));
        interactivityLabel.setText(format5(q.getContratQoe()));
        reliabilityLabel.setText(format5(q.getLifetimeQoe()));

        overallQoeLabel.setText(format5(q.getQoeGlobal()));

        loadingTimeLabel.setText(String.format("%.2f ms", q.getLatenceMoy()));
        bufferingLabel.setText(String.format("%.2f ms", q.getJitterMoy()));
        failureRateLabel.setText(String.format("%.2f %%", q.getPerteMoy()));
        streamingQualityLabel.setText(String.format("%.2f Mbps", q.getBandePassanteMoy()));

    }


    // =========================================================================
    // HELPERS
    // =========================================================================
    private void hideAllSelectionBoxes() {
        clientSelectionBox.setVisible(false);
        genreSelectionBox.setVisible(false);
        zoneSelectionBox.setVisible(false);
    }

    private void hideAllViews() {
        tableContainer.setVisible(false);
        graphContainer.setVisible(false);
        mapContainer.setVisible(false);
    }

    private String format5(double d) { return String.format("%.2f / 5", d); }

    private void afficherErreur() {
        overallQoeLabel.setText("Erreur");
    }

    private void afficherMessageCsvOK() {
        overallQoeLabel.setText("CSV chargé !");
    }


    // =========================================================================
    // TABLE CLASS
    // =========================================================================
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
