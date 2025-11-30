package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.QoE;
import com.ensah.qoe.Services.QoeAnalyzer;
import javafx.animation.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Duration;

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

    // Conteneurs dynamiques
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

        // Actions des ComboBox avec animations
        genreCombo.setOnAction(e -> {
            if (genreCombo.getValue() != null) {
                animateTransition(() -> afficherQoeParGenre(genreCombo.getValue()));
            }
        });

        clientCombo.setOnAction(e -> {
            String s = clientCombo.getValue();
            if (s != null) {
                int id = Integer.parseInt(s.split(" - ")[0]);
                animateTransition(() -> afficherQoeParClient(id));
            }
        });

        zoneCombo.setOnAction(e -> {
            String zone = zoneCombo.getValue();
            if (zone != null) {
                animateTransition(() -> afficherQoeParZone(zone));
            }
        });
    }

    // =====================================================================
    // ANIMATIONS
    // =====================================================================
    private void animateTransition(Runnable action) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200));
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            action.run();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300));
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }

    private void animateMetrics() {
        // Animation pour les labels de métriques
        animateLabel(satisfactionLabel);
        animateLabel(videoQualityLabel);
        animateLabel(audioQualityLabel);
        animateLabel(interactivityLabel);
        animateLabel(reliabilityLabel);
        animateLabel(overallQoeLabel);
    }

    private void animateLabel(Label label) {
        ScaleTransition st = new ScaleTransition(Duration.millis(300), label);
        st.setFromX(0.8);
        st.setFromY(0.8);
        st.setToX(1.0);
        st.setToY(1.0);

        FadeTransition ft = new FadeTransition(Duration.millis(300), label);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.play();
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
            showAlert("Erreur", "Impossible de charger le fichier CSV", Alert.AlertType.ERROR);
            return;
        }

        overallQoeLabel.setText("CSV chargé !");
        loadFilters();
        refreshClientTable();

        showAlert("Succès", "Fichier CSV importé avec succès!", Alert.AlertType.INFORMATION);
    }

    // =====================================================================
    // MÉTHODES QoE GLOBAL ET RAPPORT (AJOUTÉES)
    // =====================================================================

    @FXML
    public void afficherQoEGlobal() {
        System.out.println("Affichage du QoE Global");

        // Calculer et afficher le QoE global pour toutes les données
        QoE qoeGlobal = QoeAnalyzer.analyserQoEGlobal();
        if (qoeGlobal != null) {
            afficherQoeDansInterface(qoeGlobal);
            showAlert("QoE Global", "Score QoE Global calculé: " + String.format("%.2f", qoeGlobal.getQoeGlobal()), Alert.AlertType.INFORMATION);
        } else {
            showAlert("Erreur", "Impossible de calculer le QoE global", Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void exporterRapport() {
        System.out.println("Export du rapport");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le rapport QoE");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx"),
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv")
        );

        File file = fileChooser.showSaveDialog(importCsvButton.getScene().getWindow());
        if (file != null) {
            // Implémentez votre logique d'export ici
            boolean success = QoeAnalyzer.exporterRapport(file.getAbsolutePath());
            if (success) {
                showAlert("Export Réussi", "Rapport exporté: " + file.getName(), Alert.AlertType.INFORMATION);
            } else {
                showAlert("Erreur", "Échec de l'export du rapport", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void ouvrirParametres() {
        System.out.println("Ouverture des paramètres");

        try {
            // Créer une fenêtre de paramètres simple
            Stage paramStage = new Stage();
            paramStage.setTitle("Paramètres QoE");
            paramStage.initModality(Modality.APPLICATION_MODAL);

            VBox root = new VBox(20);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-padding: 30; -fx-background-color: #f8fafc;");

            Label titleLabel = new Label("⚙️ Paramètres");
            titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

            VBox settingsBox = new VBox(15);
            settingsBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12;");

            // Exemple de paramètres
            CheckBox autoRefresh = new CheckBox("Actualisation automatique");
            CheckBox notifications = new CheckBox("Notifications");
            Slider refreshRate = new Slider(1, 60, 5);
            refreshRate.setShowTickLabels(true);
            refreshRate.setShowTickMarks(true);

            HBox refreshBox = new HBox(10);
            refreshBox.setAlignment(Pos.CENTER_LEFT);
            refreshBox.getChildren().addAll(new Label("Intervalle d'actualisation (min):"), refreshRate);

            settingsBox.getChildren().addAll(autoRefresh, notifications, refreshBox);

            Button saveBtn = new Button("Sauvegarder");
            saveBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold;");
            saveBtn.setOnAction(e -> {
                showAlert("Paramètres", "Paramètres sauvegardés avec succès", Alert.AlertType.INFORMATION);
                paramStage.close();
            });

            root.getChildren().addAll(titleLabel, settingsBox, saveBtn);

            Scene scene = new Scene(root, 400, 300);
            paramStage.setScene(scene);
            paramStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir les paramètres", Alert.AlertType.ERROR);
        }
    }

    // =====================================================================
    // MODES (CLIENT / GENRE / ZONE)
    // =====================================================================
    @FXML
    private void modeClient() {
        hideAllSelectionBoxes();
        hideAllViews();

        clientSelectionBox.setVisible(true);
        refreshClientTable();
        showTableWithAnimation();
    }

    @FXML
    private void modeGenre() {
        hideAllSelectionBoxes();
        hideAllViews();

        genreSelectionBox.setVisible(true);
        graphContainer.setVisible(true);
        afficherGraphiqueGenre(); // Afficher le graphique directement
        showGraphWithAnimation();
    }

    @FXML
    public void modeZone() {
        hideAllSelectionBoxes();
        hideAllViews();

        zoneSelectionBox.setVisible(true);
        // Ouvrir directement la carte pour toutes les zones
        ouvrirCarteComplete();
    }

    // =====================================================================
    // ANIMATIONS D'AFFICHAGE
    // =====================================================================
    private void showTableWithAnimation() {
        tableContainer.setVisible(true);
        tableContainer.setManaged(true);
        tableContainer.setOpacity(0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(400), tableContainer);
        tt.setFromY(30);
        tt.setToY(0);

        FadeTransition ft = new FadeTransition(Duration.millis(400), tableContainer);
        ft.setFromValue(0);
        ft.setToValue(1);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.play();
    }

    private void showGraphWithAnimation() {
        graphContainer.setVisible(true);
        graphContainer.setManaged(true);
        graphContainer.setOpacity(0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(400), graphContainer);
        tt.setFromY(30);
        tt.setToY(0);

        FadeTransition ft = new FadeTransition(Duration.millis(400), graphContainer);
        ft.setFromValue(0);
        ft.setToValue(1);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.play();
    }

    // =====================================================================
    // OUVERTURE DE LA CARTE
    // =====================================================================
    private void ouvrirCarteComplete() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/map_geographique.fxml"));
            Parent root = loader.load();

            MapGeographiqueController mapController = loader.getController();

            Stage mapStage = new Stage();
            mapStage.setTitle("Carte QoE - Tous les clients");
            mapStage.setScene(new Scene(root, 1200, 800));
            mapStage.setMaximized(true);

            mapStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la carte géographique", Alert.AlertType.ERROR);
        }
    }

    private void ouvrirCarteZone(String zone) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/map_geographique.fxml"));
            Parent root = loader.load();

            MapGeographiqueController mapController = loader.getController();
            mapController.setZoneFiltre(zone);

            Stage mapStage = new Stage();
            mapStage.setTitle("Carte QoE - " + zone);
            mapStage.setScene(new Scene(root, 1200, 800));
            mapStage.setMaximized(true);

            mapStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la carte pour la zone: " + zone, Alert.AlertType.ERROR);
        }
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
                    "SELECT DISTINCT GENRE FROM CLIENT WHERE GENRE IS NOT NULL ORDER BY GENRE")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String genre = rs.getString(1);
                    if (genre != null && !genre.trim().isEmpty()) {
                        genreCombo.getItems().add(genre);
                    }
                }
                System.out.println("Genres chargés: " + genreCombo.getItems());
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
            showAlert("Erreur", "Impossible de charger les filtres: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // =====================================================================
    // AFFICHAGE CLIENT / GENRE / ZONE
    // =====================================================================
    private void afficherQoeParClient(int id) {
        QoE q = QoeAnalyzer.analyserParClient(id);
        if (q != null) {
            afficherQoeDansInterface(q);
            showTableWithAnimation();
        } else {
            overallQoeLabel.setText("Aucune donnée");
            showAlert("Information", "Aucune donnée QoE trouvée pour ce client", Alert.AlertType.INFORMATION);
        }
    }

    private void afficherQoeParGenre(String g) {
        if (g == null || g.trim().isEmpty()) {
            overallQoeLabel.setText("Sélectionner un genre");
            return;
        }

        System.out.println("Analyse du genre: '" + g + "'");

        QoE q = QoeAnalyzer.analyserParGenre(g);

        if (q != null) {
            System.out.println("QoE trouvé pour " + g + ": " + q.getQoeGlobal());
            afficherQoeDansInterface(q);
            afficherGraphiqueGenre();
            showGraphWithAnimation();
        } else {
            System.out.println("Aucune donnée QoE pour le genre: " + g);
            overallQoeLabel.setText("Aucune donnée");

            // Afficher quand même le graphique pour voir les données disponibles
            hideAllViews();
            afficherGraphiqueGenre();
            showGraphWithAnimation();
        }
    }

    private void afficherQoeParZone(String z) {
        if (z == null || z.trim().isEmpty()) {
            overallQoeLabel.setText("Sélectionner une zone");
            return;
        }

        QoE q = QoeAnalyzer.analyserParZone(z);
        if (q != null) {
            afficherQoeDansInterface(q);
            ouvrirCarteZone(z);
        } else {
            overallQoeLabel.setText("Aucune donnée");
            showAlert("Information", "Aucune donnée QoE trouvée pour la zone: " + z, Alert.AlertType.INFORMATION);
        }
    }

    // =====================================================================
    // TABLE CLIENTS AVEC BOUTON DÉTAILS
    // =====================================================================
    private void setupClientTable() {
        // Colonnes
        TableColumn<ClientRow, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> data.getValue().idClientProperty());
        colId.setPrefWidth(60);

        TableColumn<ClientRow, String> colNom = new TableColumn<>("Nom");
        colNom.setCellValueFactory(data -> data.getValue().nomProperty());
        colNom.setPrefWidth(200);

        TableColumn<ClientRow, String> colGenre = new TableColumn<>("Genre");
        colGenre.setCellValueFactory(data -> data.getValue().genreProperty());
        colGenre.setPrefWidth(100);

        TableColumn<ClientRow, String> colZone = new TableColumn<>("Zone");
        colZone.setCellValueFactory(data -> data.getValue().zoneProperty());
        colZone.setPrefWidth(150);

        TableColumn<ClientRow, Number> colQoe = new TableColumn<>("QoE");
        colQoe.setCellValueFactory(data -> data.getValue().qoeGlobalProperty());
        colQoe.setPrefWidth(100);

        // Colonne avec bouton "Détails"
        TableColumn<ClientRow, Void> colAction = new TableColumn<>("Actions");
        colAction.setPrefWidth(120);

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button("📋 Détails");

            {
                btnDetails.setStyle(
                        "-fx-background-color: #f59e0b; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 11; " +
                                "-fx-font-weight: 600; " +
                                "-fx-background-radius: 6; " +
                                "-fx-padding: 6 12; " +
                                "-fx-cursor: hand;"
                );

                btnDetails.setOnMouseEntered(e ->
                        btnDetails.setStyle(
                                "-fx-background-color: #d97706; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 11; " +
                                        "-fx-font-weight: 600; " +
                                        "-fx-background-radius: 6; " +
                                        "-fx-padding: 6 12; " +
                                        "-fx-cursor: hand;"
                        )
                );

                btnDetails.setOnMouseExited(e ->
                        btnDetails.setStyle(
                                "-fx-background-color: #f59e0b; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 11; " +
                                        "-fx-font-weight: 600; " +
                                        "-fx-background-radius: 6; " +
                                        "-fx-padding: 6 12; " +
                                        "-fx-cursor: hand;"
                        )
                );

                btnDetails.setOnAction(event -> {
                    ClientRow client = getTableView().getItems().get(getIndex());
                    afficherDetailsClient(client);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDetails);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        clientTable.getColumns().setAll(colId, colNom, colGenre, colZone, colQoe, colAction);

        // Style de la table
        clientTable.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background-radius: 8;"
        );

        refreshClientTable();
    }

    // =====================================================================
    // FENÊTRE DÉTAILS CLIENT
    // =====================================================================
    private void afficherDetailsClient(ClientRow client) {
        Stage detailStage = new Stage();
        detailStage.initModality(Modality.APPLICATION_MODAL);
        detailStage.setTitle("Détails du Client - " + client.getNom());

        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle(
                "-fx-padding: 30; " +
                        "-fx-background-color: #f8fafc;"
        );

        // En-tête
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setStyle(
                "-fx-background-color: linear-gradient(135deg, #f59e0b 0%, #ea580c 100%); " +
                        "-fx-background-radius: 12; " +
                        "-fx-padding: 24;"
        );

        Label titleLabel = new Label("👤 " + client.getNom());
        titleLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 24; " +
                        "-fx-font-weight: 700;"
        );

        Label subtitleLabel = new Label("ID: " + client.getIdClient() + " • " + client.getGenre());
        subtitleLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.9); " +
                        "-fx-font-size: 14;"
        );

        header.getChildren().addAll(titleLabel, subtitleLabel);

        // Informations principales
        VBox infoBox = new VBox(16);
        infoBox.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-padding: 24; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"
        );

        infoBox.getChildren().addAll(
                createInfoRow("📍 Zone", client.getZone()),
                createInfoRow("⭐ QoE Global", String.format("%.2f / 5.00", client.getQoeGlobal())),
                createSeparator()
        );

        // Récupérer les détails QoE du client
        QoE clientQoe = QoeAnalyzer.analyserParClient(client.getIdClient());

        if (clientQoe != null) {
            VBox metricsBox = new VBox(12);
            metricsBox.getChildren().addAll(
                    createMetricCard("😊 Satisfaction", clientQoe.getSatisfactionQoe(), "#fbbf24"),
                    createMetricCard("📹 Qualité Vidéo", clientQoe.getServiceQoe(), "#ef4444"),
                    createMetricCard("🎵 Qualité Audio", clientQoe.getPrixQoe(), "#8b5cf6"),
                    createMetricCard("⚡ Interactivité", clientQoe.getContratQoe(), "#3b82f6"),
                    createMetricCard("🛡️ Fiabilité", clientQoe.getLifetimeQoe(), "#10b981")
            );

            infoBox.getChildren().add(metricsBox);
        }

        // Bouton fermer
        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle(
                "-fx-background-color: #64748b; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14; " +
                        "-fx-font-weight: 600; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 12 32; " +
                        "-fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> detailStage.close());

        root.getChildren().addAll(header, infoBox, closeBtn);

        Scene scene = new Scene(root, 500, 650);
        detailStage.setScene(scene);
        detailStage.show();
    }

    private HBox createInfoRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblLabel = new Label(label);
        lblLabel.setStyle(
                "-fx-font-size: 14; " +
                        "-fx-font-weight: 600; " +
                        "-fx-text-fill: #475569; " +
                        "-fx-min-width: 150;"
        );

        Label lblValue = new Label(value != null ? value : "N/A");
        lblValue.setStyle(
                "-fx-font-size: 14; " +
                        "-fx-text-fill: #0f172a;"
        );

        row.getChildren().addAll(lblLabel, lblValue);
        return row;
    }

    private HBox createMetricCard(String label, double value, String color) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: #f1f5f9; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 12 16; " +
                        "-fx-border-color: " + color + "; " +
                        "-fx-border-width: 0 0 0 3; " +
                        "-fx-border-radius: 8;"
        );

        Label lblLabel = new Label(label);
        lblLabel.setStyle(
                "-fx-font-size: 13; " +
                        "-fx-font-weight: 600; " +
                        "-fx-text-fill: #475569; " +
                        "-fx-min-width: 150;"
        );

        Label lblValue = new Label(String.format("%.2f / 5.00", value));
        lblValue.setStyle(
                "-fx-font-size: 16; " +
                        "-fx-font-weight: 700; " +
                        "-fx-text-fill: " + color + ";"
        );

        card.getChildren().addAll(lblLabel, lblValue);
        return card;
    }

    private Separator createSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #e2e8f0;");
        return sep;
    }

    // =====================================================================
    // UTILITAIRES
    // =====================================================================
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Genre");

        NumberAxis yAxis = new NumberAxis(0, 5, 0.5);
        yAxis.setLabel("Score QoE");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Comparaison QoE par Genre");
        chart.setLegendVisible(false);
        chart.setPrefHeight(400);

        chart.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background-radius: 8;"
        );

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("QoE Global");

        // Récupérer tous les genres disponibles dans le ComboBox
        ObservableList<String> genres = genreCombo.getItems();
        System.out.println("Genres disponibles pour graphique: " + genres);

        boolean hasData = false;

        for (String genre : genres) {
            if (genre != null && !genre.trim().isEmpty()) {
                System.out.println("Chargement QoE pour genre: '" + genre + "'");
                QoE qoe = QoeAnalyzer.analyserParGenre(genre);

                if (qoe != null && qoe.getQoeGlobal() > 0) {
                    XYChart.Data<String, Number> data = new XYChart.Data<>(genre, qoe.getQoeGlobal());
                    serie.getData().add(data);
                    hasData = true;
                    System.out.println("QoE " + genre + ": " + qoe.getQoeGlobal());
                } else {
                    System.out.println("Pas de QoE pour: " + genre);
                }
            }
        }

        if (!hasData) {
            VBox noDataBox = new VBox(16);
            noDataBox.setAlignment(Pos.CENTER);
            noDataBox.setStyle("-fx-padding: 60;");

            Label iconLabel = new Label("📊");
            iconLabel.setStyle("-fx-font-size: 48;");

            Label noData = new Label("Aucune donnée QoE disponible");
            noData.setStyle("-fx-font-size: 18; -fx-text-fill: #64748b; -fx-font-weight: 600;");

            Label hint = new Label("Importez un fichier CSV pour générer les analyses");
            hint.setStyle("-fx-font-size: 14; -fx-text-fill: #94a3b8;");

            noDataBox.getChildren().addAll(iconLabel, noData, hint);
            graphContainer.getChildren().add(noDataBox);
            return;
        }

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

        animateMetrics();
    }

    private String format5(double d) { return String.format("%.2f / 5", d); }

    private void hideAllViews() {
        tableContainer.setVisible(false);
        tableContainer.setManaged(false);
        graphContainer.setVisible(false);
        graphContainer.setManaged(false);
    }

    private void hideAllSelectionBoxes() {
        clientSelectionBox.setVisible(false);
        genreSelectionBox.setVisible(false);
        zoneSelectionBox.setVisible(false);
    }

    // =====================================================================
    // CLASSE INTERNE - ClientRow
    // =====================================================================
    public static class ClientRow {
        private final IntegerProperty idClient = new SimpleIntegerProperty();
        private final StringProperty nom = new SimpleStringProperty();
        private final StringProperty genre = new SimpleStringProperty();
        private final StringProperty zone = new SimpleStringProperty();
        private final DoubleProperty qoeGlobal = new SimpleDoubleProperty();

        public ClientRow(int id, String nom, String genre, String zone, double qoe) {
            this.idClient.set(id);
            this.nom.set(nom);
            this.genre.set(genre);
            this.zone.set(zone);
            this.qoeGlobal.set(qoe);
        }

        public IntegerProperty idClientProperty() { return idClient; }
        public StringProperty nomProperty() { return nom; }
        public StringProperty genreProperty() { return genre; }
        public StringProperty zoneProperty() { return zone; }
        public DoubleProperty qoeGlobalProperty() { return qoeGlobal; }

        public int getIdClient() { return idClient.get(); }
        public String getNom() { return nom.get(); }
        public String getGenre() { return genre.get(); }
        public String getZone() { return zone.get(); }
        public double getQoeGlobal() { return qoeGlobal.get(); }
    }
}