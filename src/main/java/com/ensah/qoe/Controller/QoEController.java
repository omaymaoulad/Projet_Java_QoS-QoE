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
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class QoEController implements Initializable {

    // ==== Top Navigation ====
    @FXML private ToggleButton radioClient;
    @FXML private ToggleButton radioGenre;
    @FXML private ToggleButton radioZone;
    @FXML private ToggleGroup filterGroup;

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
    @FXML private Label clientCountLabel;

    // Labels - QoE Global et Quick Stats
    @FXML private Label overallQoeLabel;
    @FXML private Label bufferingLabel;
    @FXML private Label loadingTimeLabel;
    @FXML private Label failureRateLabel;
    @FXML private Label streamingQualityLabel;

    // Labels - Métriques Subjectives
    @FXML private Label satisfactionLabel;
    @FXML private Label videoQualityLabel;
    @FXML private Label audioQualityLabel;
    @FXML private Label interactivityLabel;
    @FXML private Label reliabilityLabel;

    // Couleurs pour chaque genre
    private final Map<String, String> genreColors = new HashMap<>();
    private final List<String> colorPalette = Arrays.asList(
            "#f59e0b", "#ef4444", "#8b5cf6", "#3b82f6", "#10b981",
            "#ec4899", "#14b8a6", "#f97316", "#6366f1", "#84cc16"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup toggle group
        filterGroup = new ToggleGroup();
        radioGenre.setToggleGroup(filterGroup);
        radioClient.setToggleGroup(filterGroup);
        radioZone.setToggleGroup(filterGroup);

        // Sélectionner Genre par défaut
        radioGenre.setSelected(true);

        // Style pour les toggle buttons
        setupToggleButtonStyles();

        hideAllSelectionBoxes();
        hideAllViews();

        loadFilters();
        setupClientTable();

        // Actions des ComboBox
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

        // Afficher le graphique genre par défaut
        afficherGraphiqueGenre();
    }

    private void setupToggleButtonStyles() {
        // Style pour les boutons non sélectionnés
        String inactiveStyle =
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: rgba(255,255,255,0.8); " +
                        "-fx-font-size: 14; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 16 40; " +
                        "-fx-min-width: 180; " +
                        "-fx-background-radius: 0; " +
                        "-fx-border-width: 0 0 3 0; " +
                        "-fx-border-color: transparent; " +
                        "-fx-cursor: hand;";

        // Style pour le bouton sélectionné
        String activeStyle =
                "-fx-background-color: rgba(245, 158, 11, 0.1); " +
                        "-fx-text-fill: #fbbf24; " +
                        "-fx-font-size: 14; " +
                        "-fx-font-weight: 700; " +
                        "-fx-padding: 16 40; " +
                        "-fx-min-width: 180; " +
                        "-fx-background-radius: 0; " +
                        "-fx-border-width: 0 0 3 0; " +
                        "-fx-border-color: #fbbf24; " +
                        "-fx-cursor: hand;";

        // Appliquer les styles
        radioGenre.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            radioGenre.setStyle(isSelected ? activeStyle : inactiveStyle);
        });

        radioClient.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            radioClient.setStyle(isSelected ? activeStyle : inactiveStyle);
        });

        radioZone.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            radioZone.setStyle(isSelected ? activeStyle : inactiveStyle);
        });

        // Initialiser les styles
        radioGenre.setStyle(activeStyle);
        radioClient.setStyle(inactiveStyle);
        radioZone.setStyle(inactiveStyle);
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
        animateLabel(satisfactionLabel);
        animateLabel(videoQualityLabel);
        animateLabel(audioQualityLabel);
        animateLabel(interactivityLabel);
        animateLabel(reliabilityLabel);
        animateLabel(overallQoeLabel);
    }

    private void animateLabel(Label label) {
        ScaleTransition st = new ScaleTransition(Duration.millis(400), label);
        st.setFromX(0.8);
        st.setFromY(0.8);
        st.setToX(1.0);
        st.setToY(1.0);

        FadeTransition ft = new FadeTransition(Duration.millis(400), label);
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

        // Rafraîchir l'affichage actuel
        if (radioGenre.isSelected()) {
            afficherGraphiqueGenre();
        } else if (radioClient.isSelected()) {
            showTableWithAnimation();
        }

        showAlert("Succès", "Fichier CSV importé avec succès!", Alert.AlertType.INFORMATION);
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
        afficherGraphiqueGenre();
        showGraphWithAnimation();
    }

    @FXML
    public void modeZone() {
        hideAllSelectionBoxes();
        hideAllViews();

        zoneSelectionBox.setVisible(true);
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
                int colorIndex = 0;
                while (rs.next()) {
                    String genre = rs.getString(1);
                    if (genre != null && !genre.trim().isEmpty()) {
                        genreCombo.getItems().add(genre);
                        // Assigner une couleur à chaque genre
                        if (!genreColors.containsKey(genre)) {
                            genreColors.put(genre, colorPalette.get(colorIndex % colorPalette.size()));
                            colorIndex++;
                        }
                    }
                }
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

        QoE q = QoeAnalyzer.analyserParGenre(g);

        if (q != null) {
            afficherQoeDansInterface(q);
            afficherGraphiqueGenre();
            showGraphWithAnimation();
        } else {
            overallQoeLabel.setText("Aucune donnée");
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
    // GRAPHIQUE GENRE AVEC COULEURS ET AXES VISIBLES
    // =====================================================================
    private void afficherGraphiqueGenre() {
        // Supprimer le contenu existant (sauf le header)
        if (graphContainer.getChildren().size() > 1) {
            graphContainer.getChildren().remove(1, graphContainer.getChildren().size());
        }

        ObservableList<String> genres = genreCombo.getItems();

        // Vérifier s'il y a des données
        boolean hasData = false;
        for (String genre : genres) {
            if (genre != null && !genre.trim().isEmpty()) {
                QoE qoe = QoeAnalyzer.analyserParGenre(genre);
                if (qoe != null && qoe.getQoeGlobal() > 0) {
                    hasData = true;
                    break;
                }
            }
        }

        if (!hasData) {
            VBox noDataBox = new VBox(16);
            noDataBox.setAlignment(Pos.CENTER);
            noDataBox.setStyle("-fx-padding: 80;");

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

        // ✅ Créer les axes avec configuration explicite
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Genre");
        xAxis.setTickLabelRotation(0);
        xAxis.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-tick-label-fill: #475569;");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Score QoE");
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(5);
        yAxis.setTickUnit(0.5);
        yAxis.setStyle("-fx-font-size: 12px; -fx-tick-label-fill: #64748b;");

        // ✅ Créer le graphique avec configuration complète
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Comparaison QoE par Genre");
        chart.setTitleSide(javafx.geometry.Side.TOP);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(450);
        chart.setMinHeight(450);
        chart.setMaxHeight(450);

        // Style du graphique
        chart.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-padding: 20px;"
        );

        // Créer la série de données
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("QoE Global");

        // Ajouter les données
        for (String genre : genres) {
            if (genre != null && !genre.trim().isEmpty()) {
                QoE qoe = QoeAnalyzer.analyserParGenre(genre);
                if (qoe != null && qoe.getQoeGlobal() > 0) {
                    XYChart.Data<String, Number> data = new XYChart.Data<>(genre, qoe.getQoeGlobal());
                    serie.getData().add(data);
                }
            }
        }

        // Ajouter la série au graphique
        chart.getData().add(serie);

        // ✅ CORRECTION IMPORTANTE: Ajouter le graphique au container AVANT d'appliquer les couleurs
        graphContainer.getChildren().add(chart);

        // Appliquer les styles de base
        chart.applyCss();
        chart.layout();

        // ✅ CORRECTION IMPORTANTE: Appliquer les couleurs avec Platform.runLater
        javafx.application.Platform.runLater(() -> {
            for (int i = 0; i < serie.getData().size(); i++) {
                XYChart.Data<String, Number> data = serie.getData().get(i);
                String genre = data.getXValue();
                String color = genreColors.getOrDefault(genre, "#f59e0b");

                Node node = data.getNode();
                if (node != null) {
                    // Appliquer la couleur personnalisée
                    node.setStyle("-fx-bar-fill: " + color + ";");

                    // Ajouter un tooltip
                    Tooltip tooltip = new Tooltip(
                            genre + "\nQoE: " + String.format("%.2f / 5.00", data.getYValue().doubleValue())
                    );
                    tooltip.setStyle(
                            "-fx-background-color: rgba(0,0,0,0.9); " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 12px; " +
                                    "-fx-padding: 8px; " +
                                    "-fx-background-radius: 6px;"
                    );
                    Tooltip.install(node, tooltip);
                }
            }
        });
    }

    // =====================================================================
    // TABLE CLIENTS
    // =====================================================================
    private void setupClientTable() {
        TableColumn<ClientRow, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> data.getValue().idClientProperty());
        colId.setPrefWidth(60);
        colId.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClientRow, String> colNom = new TableColumn<>("Nom");
        colNom.setCellValueFactory(data -> data.getValue().nomProperty());
        colNom.setPrefWidth(250);

        TableColumn<ClientRow, String> colGenre = new TableColumn<>("Genre");
        colGenre.setCellValueFactory(data -> data.getValue().genreProperty());
        colGenre.setPrefWidth(120);
        colGenre.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClientRow, String> colZone = new TableColumn<>("Zone");
        colZone.setCellValueFactory(data -> data.getValue().zoneProperty());
        colZone.setPrefWidth(180);

        TableColumn<ClientRow, Number> colQoe = new TableColumn<>("QoE Global");
        colQoe.setCellValueFactory(data -> data.getValue().qoeGlobalProperty());
        colQoe.setPrefWidth(120);
        colQoe.setStyle("-fx-alignment: CENTER;");

        // Formater la colonne QoE
        colQoe.setCellFactory(column -> new TableCell<ClientRow, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f / 5", item.doubleValue()));
                    double value = item.doubleValue();
                    String color;
                    if (value >= 4.0) color = "#10b981";
                    else if (value >= 3.0) color = "#f59e0b";
                    else color = "#ef4444";
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 700; -fx-alignment: CENTER;");
                }
            }
        });

        // Colonne Actions
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
                                "-fx-background-radius: 8; " +
                                "-fx-padding: 8 16; " +
                                "-fx-cursor: hand;"
                );

                btnDetails.setOnMouseEntered(e ->
                        btnDetails.setStyle(
                                "-fx-background-color: #d97706; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 11; " +
                                        "-fx-font-weight: 600; " +
                                        "-fx-background-radius: 8; " +
                                        "-fx-padding: 8 16; " +
                                        "-fx-cursor: hand;"
                        )
                );

                btnDetails.setOnMouseExited(e ->
                        btnDetails.setStyle(
                                "-fx-background-color: #f59e0b; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 11; " +
                                        "-fx-font-weight: 600; " +
                                        "-fx-background-radius: 8; " +
                                        "-fx-padding: 8 16; " +
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
        clientTable.setStyle("-fx-background-color: transparent;");

        refreshClientTable();
    }

    private void afficherDetailsClient(ClientRow client) {
        Stage detailStage = new Stage();
        detailStage.setTitle("Détails du Client - " + client.getNom());

        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-padding: 30; -fx-background-color: #f8fafc;");

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

        // Informations
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
                new Separator()
        );

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

    private String format5(double d) {
        return String.format("%.2f / 5", d);
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
        ObservableList<ClientRow> rows = loadClientRows();
        clientTable.setItems(rows);
        if (clientCountLabel != null) {
            clientCountLabel.setText(rows.size() + " client" + (rows.size() > 1 ? "s" : ""));
        }
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

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

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
