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

    @FXML private HBox genreSubMenu;
    @FXML private HBox zoneSubMenu;
    @FXML private ComboBox<String> clientCombo;
    @FXML private ComboBox<String> genreCombo;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private ComboBox<String> zoneCombo;
    @FXML private Button allZonesButton;

    @FXML private Button importCsvButton;

    // ==== Layout containers ====
    @FXML private HBox mainContentBox;
    @FXML private VBox metricsColumn;
    @FXML private VBox dynamicContentColumn;
    @FXML private HBox qoeGlobalSection;
    @FXML private GridPane quickStatsGrid;
    @FXML private VBox subjectiveMetricsBox;

    // ==== Dynamic content containers ====
    @FXML private VBox tableContainer;
    @FXML private VBox graphContainer;
    @FXML private VBox mapContainer;
    @FXML private VBox mapPlaceholder;
    @FXML private Button openMapButton;
    @FXML private Label mapTitleLabel;

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

    private String currentZoneFilter = null;

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

        loadFilters();
        setupClientTable();

        // Ajouter les options Male/Female au ComboBox sexe
        sexeCombo.getItems().addAll("Male", "Female");

        // Actions des ComboBox
        genreCombo.setOnAction(e -> {
            if (genreCombo.getValue() != null) {
                animateTransition(() -> afficherQoeParGenre(genreCombo.getValue()));
            }
        });

        sexeCombo.setOnAction(e -> {
            if (sexeCombo.getValue() != null) {
                animateTransition(() -> {
                    afficherQoeParSexe(sexeCombo.getValue());
                    afficherGraphiqueGenre();
                });
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
                currentZoneFilter = zone;
                animateTransition(() -> afficherQoeParZone(zone));
            }
        });

        // Action pour le bouton de la carte
        openMapButton.setOnAction(e -> {
            if (currentZoneFilter != null && !currentZoneFilter.isEmpty()) {
                ouvrirCarteZone(currentZoneFilter);
            } else {
                ouvrirCarteComplete();
            }
        });

        // Afficher le mode par défaut (métriques pleine page)
        showMetricsFullScreen();
        if (QoeAnalyzer.isCsvCharge()) {
            afficherQoeGlobal();
        }
    }

    private void setupToggleButtonStyles() {
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

        radioGenre.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            radioGenre.setStyle(isSelected ? activeStyle : inactiveStyle);
        });

        radioClient.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            radioClient.setStyle(isSelected ? activeStyle : inactiveStyle);
        });

        radioZone.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            radioZone.setStyle(isSelected ? activeStyle : inactiveStyle);
        });

        radioGenre.setStyle(activeStyle);
        radioClient.setStyle(inactiveStyle);
        radioZone.setStyle(inactiveStyle);
    }

    // =====================================================================
    // LAYOUT MANAGEMENT - Gestion de l'affichage
    // =====================================================================

    /**
     * Mode par défaut: Métriques en pleine largeur, pas de contenu dynamique
     */
    private void showMetricsFullScreen() {
        // Métriques prennent toute la largeur
        metricsColumn.prefWidthProperty().unbind();
        HBox.setHgrow(metricsColumn, Priority.ALWAYS);

        // Cacher la colonne dynamique
        dynamicContentColumn.setVisible(false);
        dynamicContentColumn.setManaged(false);

        // Cacher tous les conteneurs dynamiques
        hideAllDynamicContainers();

        // Cacher tous les sous-menus
        genreSubMenu.setVisible(false);
        zoneSubMenu.setVisible(false);

        // Restaurer la taille normale des métriques
        expandSubjectiveMetrics();
    }

    /**
     * Mode split: Métriques à gauche (compactes), contenu dynamique à droite
     */
    private void showSplitLayout() {
        // Métriques prennent une largeur fixe
        metricsColumn.prefWidthProperty().unbind();
        metricsColumn.setPrefWidth(500);
        metricsColumn.setMaxWidth(500);
        HBox.setHgrow(metricsColumn, Priority.NEVER);

        // Afficher la colonne dynamique
        dynamicContentColumn.setVisible(true);
        dynamicContentColumn.setManaged(true);
        HBox.setHgrow(dynamicContentColumn, Priority.ALWAYS);

        // Compacter les métriques subjectives
        compactSubjectiveMetrics();
    }

    /**
     * Compacter les métriques subjectives pour le mode split
     */
    private void compactSubjectiveMetrics() {
        // En mode compact, les métriques ont des tailles réduites
        subjectiveMetricsBox.setSpacing(16);

        // Réduire la taille des valeurs dans les labels
        satisfactionLabel.setStyle(satisfactionLabel.getStyle().replace("-fx-font-size: 32;", "-fx-font-size: 24;"));
        videoQualityLabel.setStyle(videoQualityLabel.getStyle().replace("-fx-font-size: 32;", "-fx-font-size: 24;"));
        audioQualityLabel.setStyle(audioQualityLabel.getStyle().replace("-fx-font-size: 32;", "-fx-font-size: 24;"));
        interactivityLabel.setStyle(interactivityLabel.getStyle().replace("-fx-font-size: 32;", "-fx-font-size: 24;"));
        reliabilityLabel.setStyle(reliabilityLabel.getStyle().replace("-fx-font-size: 32;", "-fx-font-size: 24;"));
    }

    /**
     * Restaurer la taille normale des métriques
     */
    private void expandSubjectiveMetrics() {
        subjectiveMetricsBox.setSpacing(20);

        satisfactionLabel.setStyle(satisfactionLabel.getStyle().replace("-fx-font-size: 24;", "-fx-font-size: 32;"));
        videoQualityLabel.setStyle(videoQualityLabel.getStyle().replace("-fx-font-size: 24;", "-fx-font-size: 32;"));
        audioQualityLabel.setStyle(audioQualityLabel.getStyle().replace("-fx-font-size: 24;", "-fx-font-size: 32;"));
        interactivityLabel.setStyle(interactivityLabel.getStyle().replace("-fx-font-size: 24;", "-fx-font-size: 32;"));
        reliabilityLabel.setStyle(reliabilityLabel.getStyle().replace("-fx-font-size: 24;", "-fx-font-size: 32;"));
    }

    private void hideAllDynamicContainers() {
        tableContainer.setVisible(false);
        tableContainer.setManaged(false);

        graphContainer.setVisible(false);
        graphContainer.setManaged(false);

        mapContainer.setVisible(false);
        mapContainer.setManaged(false);
    }

    // =====================================================================
    // ANIMATIONS
    // =====================================================================
    private void animateTransition(Runnable action) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), mainContentBox);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            action.run();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), mainContentBox);
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
        QoeAnalyzer.reset();

        boolean ok = QoeAnalyzer.analyserFichierCsv(f.getAbsolutePath());

        if (!ok) {
            overallQoeLabel.setText("Erreur");
            showAlert("Erreur", "Impossible de charger le fichier CSV", Alert.AlertType.ERROR);
            return;
        }


        loadFilters();
        refreshClientTable();

        // Rafraîchir l'affichage selon le mode actif
       /* if (radioGenre.isSelected()) {
            modeGenre();
        } else if (radioClient.isSelected()) {
            modeClient();
        } else if (radioZone.isSelected()) {
            modeZone();
        }*/
        // Revenir à la vue QoE globale après import CSV
        filterGroup.selectToggle(null);   // aucun mode sélectionné
        showMetricsFullScreen();          // métriques plein écran
        afficherQoeGlobal();              // QoE global
        animateMetrics();

        showAlert("Succès", "Fichier CSV importé avec succès!", Alert.AlertType.INFORMATION);
    }

    // =====================================================================
    // MODES (CLIENT / GENRE / ZONE)
    // =====================================================================
    @FXML
    private void modeClient() {
        animateTransition(() -> {
            // Passer en mode split
            showSplitLayout();

            // Cacher les sous-menus
            genreSubMenu.setVisible(false);
            zoneSubMenu.setVisible(false);

            // Afficher uniquement le conteneur table
            hideAllDynamicContainers();
            tableContainer.setVisible(true);
            tableContainer.setManaged(true);

            // Charger les données
            refreshClientTable();

            // Si un client est sélectionné, afficher ses données
            if (clientCombo.getValue() != null) {
                String s = clientCombo.getValue();
                int id = Integer.parseInt(s.split(" - ")[0]);
                afficherQoeParClient(id);
            } else {
                // Afficher des métriques par défaut
                resetMetrics();
            }
        });
    }

    @FXML
    private void modeGenre() {
        animateTransition(() -> {
            // Passer en mode split
            showSplitLayout();

            // Afficher le sous-menu genre
            genreSubMenu.setVisible(true);

            // Cacher le sous-menu zone
            zoneSubMenu.setVisible(false);

            // Afficher uniquement le conteneur graphique
            hideAllDynamicContainers();
            graphContainer.setVisible(true);
            graphContainer.setManaged(true);

            // Afficher le graphique
            afficherGraphiqueGenre();

            // Afficher les métriques selon la sélection
            if (sexeCombo.getValue() != null) {
                // Si un sexe est sélectionné, afficher ses données
                afficherQoeParSexe(sexeCombo.getValue());
            } else {
                // Sinon, afficher les données globales
                resetMetrics();
            }
        });
    }

    @FXML
    public void modeZone() {
        animateTransition(() -> {
            // Passer en mode split
            showSplitLayout();

            // Cacher le sous-menu genre
            genreSubMenu.setVisible(false);

            // Afficher le sous-menu zone
            zoneSubMenu.setVisible(true);

            // Afficher uniquement le conteneur carte
            hideAllDynamicContainers();
            mapContainer.setVisible(true);
            mapContainer.setManaged(true);

            // Réinitialiser le filtre zone
            currentZoneFilter = null;
            mapTitleLabel.setText("CARTE GÉOGRAPHIQUE - TOUTES LES ZONES");

            // Si une zone est sélectionnée, afficher ses données
            if (zoneCombo.getValue() != null) {
                afficherQoeParZone(zoneCombo.getValue());
            } else {
                resetMetrics();
            }
        });
    }

    @FXML
    private void afficherToutesLesZones() {
        currentZoneFilter = null;
        zoneCombo.getSelectionModel().clearSelection();
        mapTitleLabel.setText("CARTE GÉOGRAPHIQUE - TOUTES LES ZONES");
        resetMetrics();
        ouvrirCarteComplete();
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
        } else {
            resetMetrics();
            showAlert("Information", "Aucune donnée QoE trouvée pour ce client", Alert.AlertType.INFORMATION);
        }
    }

    private void afficherQoeParGenre(String g) {
        if (g == null || g.trim().isEmpty()) {
            resetMetrics();
            return;
        }

        QoE q = QoeAnalyzer.analyserParGenre(g);

        if (q != null) {
            afficherQoeDansInterface(q);
            afficherGraphiqueGenre();
        } else {
            resetMetrics();
            afficherGraphiqueGenre();
        }
    }

    private void afficherQoeParSexe(String sexe) {
        if (sexe == null || sexe.trim().isEmpty()) {
            resetMetrics();
            return;
        }

        // Analyser directement par le sexe (Male/Female)
        QoE q = QoeAnalyzer.analyserParGenre(sexe);

        if (q != null) {
            afficherQoeDansInterface(q);
        } else {
            resetMetrics();
            showAlert("Information", "Aucune donnée QoE trouvée pour " + sexe, Alert.AlertType.INFORMATION);
        }
    }

    private void afficherQoeParGenreEtSexe(String genre, String sexe) {
        if (genre == null || genre.trim().isEmpty() || sexe == null || sexe.trim().isEmpty()) {
            resetMetrics();
            return;
        }

        // Analyser les données par genre et sexe
        QoE q = analyserParGenreEtSexe(genre, sexe);

        if (q != null) {
            afficherQoeDansInterface(q);
            afficherGraphiqueGenre();
        } else {
            resetMetrics();
            afficherGraphiqueGenre();
            showAlert("Information", "Aucune donnée QoE trouvée pour " + genre + " - " + sexe, Alert.AlertType.INFORMATION);
        }
    }

    /**
     * Analyser les données QoE par genre et sexe
     * Note: Dans la base de données, le sexe est stocké dans la colonne GENRE
     */
    private QoE analyserParGenreEtSexe(String genre, String sexe) {
        String sql = """
            SELECT 
                AVG(q.SATISFACTION_QOE) as sat,
                AVG(q.SERVICE_QOE) as srv,
                AVG(q.PRIX_QOE) as prix,
                AVG(q.CONTRAT_QOE) as ctr,
                AVG(q.LIFETIME_QOE) as life,
                AVG(q.QOE_GLOBAL) as global,
                AVG(q.LATENCE_MOY) as lat,
                AVG(q.JITTER_MOY) as jit,
                AVG(q.PERTE_MOY) as perte,
                AVG(q.BANDE_PASSANTE_MOY) as bp
            FROM QOE q
            JOIN CLIENT c ON q.ID_CLIENT = c.ID_CLIENT
            WHERE c.GENRE = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sexe);  // Utiliser sexe directement car GENRE contient Male/Female
            ResultSet rs = ps.executeQuery();

            if (rs.next() && rs.getDouble("global") > 0) {
                QoE qoe = new QoE();
                qoe.setSatisfactionQoe(rs.getDouble("sat"));
                qoe.setServiceQoe(rs.getDouble("srv"));
                qoe.setPrixQoe(rs.getDouble("prix"));
                qoe.setContratQoe(rs.getDouble("ctr"));
                qoe.setLifetimeQoe(rs.getDouble("life"));
                qoe.setQoeGlobal(rs.getDouble("global"));
                qoe.setLatenceMoy((int) rs.getDouble("lat"));
                qoe.setJitterMoy((int) rs.getDouble("jit"));
                qoe.setPerteMoy(rs.getDouble("perte"));
                qoe.setBandePassanteMoy((int) rs.getDouble("bp"));
                return qoe;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void afficherQoeParZone(String z) {
        if (z == null || z.trim().isEmpty()) {
            resetMetrics();
            return;
        }

        currentZoneFilter = z;
        mapTitleLabel.setText("CARTE GÉOGRAPHIQUE - " + z.toUpperCase());

        QoE q = QoeAnalyzer.analyserParZone(z);
        if (q != null) {
            afficherQoeDansInterface(q);
        } else {
            resetMetrics();
            showAlert("Information", "Aucune donnée QoE trouvée pour la zone: " + z, Alert.AlertType.INFORMATION);
        }
    }

    // =====================================================================
    // GRAPHIQUE GENRE
    // =====================================================================
    private void afficherGraphiqueGenre() {
        if (graphContainer.getChildren().size() > 1) {
            graphContainer.getChildren().remove(1, graphContainer.getChildren().size());
        }

        ObservableList<String> genres = genreCombo.getItems();

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

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Comparaison QoE par Genre");
        chart.setTitleSide(javafx.geometry.Side.TOP);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(400);
        chart.setMinHeight(400);

        chart.setStyle("-fx-background-color: transparent; -fx-padding: 20px;");

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("QoE Global");

        for (String genre : genres) {
            if (genre != null && !genre.trim().isEmpty()) {
                QoE qoe = QoeAnalyzer.analyserParGenre(genre);
                if (qoe != null && qoe.getQoeGlobal() > 0) {
                    XYChart.Data<String, Number> data = new XYChart.Data<>(genre, qoe.getQoeGlobal());
                    serie.getData().add(data);
                }
            }
        }

        chart.getData().add(serie);
        graphContainer.getChildren().add(chart);

        chart.applyCss();
        chart.layout();

        javafx.application.Platform.runLater(() -> {
            for (int i = 0; i < serie.getData().size(); i++) {
                XYChart.Data<String, Number> data = serie.getData().get(i);
                String genre = data.getXValue();
                String color = genreColors.getOrDefault(genre, "#f59e0b");

                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-bar-fill: " + color + ";");

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
        colNom.setPrefWidth(200);

        TableColumn<ClientRow, String> colGenre = new TableColumn<>("Genre");
        colGenre.setCellValueFactory(data -> data.getValue().genreProperty());
        colGenre.setPrefWidth(100);
        colGenre.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClientRow, String> colZone = new TableColumn<>("Zone");
        colZone.setCellValueFactory(data -> data.getValue().zoneProperty());
        colZone.setPrefWidth(150);

        TableColumn<ClientRow, Number> colQoe = new TableColumn<>("QoE Global");
        colQoe.setCellValueFactory(data -> data.getValue().qoeGlobalProperty());
        colQoe.setPrefWidth(100);
        colQoe.setStyle("-fx-alignment: CENTER;");

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

        TableColumn<ClientRow, Void> colAction = new TableColumn<>("Actions");
        colAction.setPrefWidth(100);
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

    private void resetMetrics() {
        satisfactionLabel.setText("—");
        videoQualityLabel.setText("—");
        audioQualityLabel.setText("—");
        interactivityLabel.setText("—");
        reliabilityLabel.setText("—");
        overallQoeLabel.setText("—");
        loadingTimeLabel.setText("—");
        bufferingLabel.setText("—");
        failureRateLabel.setText("—");
        streamingQualityLabel.setText("—");
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
    private void afficherQoeGlobal() {
        QoE q = QoeAnalyzer.analyserQoEGlobal();
        if (q != null) {
            afficherQoeDansInterface(q);
        } else {
            resetMetrics();
            showAlert(
                    "Information",
                    "Aucune donnée QoE globale disponible. Veuillez importer un fichier CSV.",
                    Alert.AlertType.INFORMATION
            );
        }
    }

}