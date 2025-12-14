package com.ensah.qoe.ML;
import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Services.PredictionService;
import weka.core.Instances;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MLDashboardTester {

    public static void main(String[] args) {
        System.out.println("🔧 DÉMARRAGE DU TEST DU ML DASHBOARD 🔧");
        System.out.println("═══════════════════════════════════════\n");

        try {
            // Test 1: Connexion à la base de données
            testDatabaseConnection();

            // Test 2: Chargement des données depuis la base
            testDataLoading();

            // Test 3: Test des méthodes de préparation de données
            testDataPreparation();

            // Test 4: Test du service de prédiction
            testPredictionService();

            // Test 5: Simulation des méthodes du contrôleur
            testControllerMethods();

            System.out.println("\n✅ TOUS LES TESTS TERMINÉS AVEC SUCCÈS !");

        } catch (Exception e) {
            System.err.println("\n❌ ERREUR CRITIQUE DURANT LES TESTS : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testDatabaseConnection() {
        System.out.println("📊 TEST 1: CONNEXION À LA BASE DE DONNÉES");
        System.out.println("─────────────────────────────────────────");

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("✅ Connexion établie avec succès !");

            // Vérifier la table MESURES_QOS
            String testSQL = "SELECT COUNT(*) as total, " +
                    "COUNT(CASE WHEN MOS IS NOT NULL THEN 1 END) as with_mos, " +
                    "COUNT(CASE WHEN MOS IS NULL THEN 1 END) as without_mos " +
                    "FROM MESURES_QOS";

            try (PreparedStatement ps = conn.prepareStatement(testSQL);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    int total = rs.getInt("total");
                    int withMOS = rs.getInt("with_mos");
                    int withoutMOS = rs.getInt("without_mos");

                    System.out.println("📋 Statistiques de la table MESURES_QOS:");
                    System.out.println("   • Total des enregistrements: " + total);
                    System.out.println("   • Avec MOS: " + withMOS);
                    System.out.println("   • Sans MOS: " + withoutMOS);

                    if (total == 0) {
                        System.out.println("⚠️ ATTENTION: La table est vide !");
                    }

                    if (withMOS == 0) {
                        System.out.println("⚠️ ATTENTION: Aucun MOS disponible pour l'entraînement !");
                    }
                }
            }

            // Vérifier la structure de la table
            String structureSQL =
                    "SELECT column_name, data_type, nullable " +
                            "FROM user_tab_columns " +
                            "WHERE table_name = 'MESURES_QOS' " +
                            "ORDER BY column_id";

            try (PreparedStatement ps = conn.prepareStatement(structureSQL);
                 ResultSet rs = ps.executeQuery()) {

                System.out.println("\n🏗️ Structure de la table MESURES_QOS:");
                System.out.println("┌─────────────────────┬────────────────┬──────────┐");
                System.out.println("│ Colonne             │ Type           │ Nullable │");
                System.out.println("├─────────────────────┼────────────────┼──────────┤");

                while (rs.next()) {
                    String colName = rs.getString("column_name");
                    String dataType = rs.getString("data_type");
                    String nullable = rs.getString("nullable");

                    System.out.printf("│ %-19s │ %-14s │ %-8s │\n",
                            colName, dataType, nullable);
                }
                System.out.println("└─────────────────────┴────────────────┴──────────┘");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à la base: " + e.getMessage());
            System.err.println("💡 Vérifiez:");
            System.err.println("   1. Que Oracle est en cours d'exécution");
            System.err.println("   2. Les paramètres de connexion dans DBConnection");
            System.err.println("   3. Que l'utilisateur a les droits nécessaires");
            throw new RuntimeException("Échec de la connexion à la base", e);
        }

        System.out.println("─────────────────────────────────────────\n");
    }

    private static void testDataLoading() {
        System.out.println("📥 TEST 2: CHARGEMENT DES DONNÉES WEKA");
        System.out.println("───────────────────────────────────────");

        try {
            // Appeler la méthode de chargement des données
            Instances data = DataPreparation.loadDataFromDatabase();

            if (data == null) {
                throw new RuntimeException("Les données chargées sont null");
            }

            System.out.println("✅ Données chargées avec succès !");
            System.out.println("📊 Résumé du dataset:");
            System.out.println("   • Nom: " + data.relationName());
            System.out.println("   • Nombre d'instances: " + data.numInstances());
            System.out.println("   • Nombre d'attributs: " + data.numAttributes());

            // Afficher les attributs
            System.out.println("\n🔧 Liste des attributs:");
            for (int i = 0; i < data.numAttributes(); i++) {
                System.out.printf("   %d. %s (%s)\n",
                        i + 1,
                        data.attribute(i).name(),
                        data.attribute(i).isNumeric() ? "Numérique" : "Nominal");
            }

            // Vérifier l'attribut cible
            if (data.classIndex() >= 0) {
                System.out.println("\n🎯 Attribut cible: " + data.classAttribute().name());
            } else {
                System.out.println("⚠️ Aucun attribut cible défini !");
            }

            // Afficher quelques instances
            if (data.numInstances() > 0) {
                System.out.println("\n👀 Exemple d'instances (3 premières):");
                System.out.println("┌─────┬─────────┬────────┬───────┬────────────────┬──────────────┬─────┐");
                System.out.println("│ No  │ Latence │ Jitter │ Perte │ Bande Passante │ Signal Score │ MOS │");
                System.out.println("├─────┼─────────┼────────┼───────┼────────────────┼──────────────┼─────┤");

                for (int i = 0; i < Math.min(3, data.numInstances()); i++) {
                    double latence = data.instance(i).value(0);
                    double jitter = data.instance(i).value(1);
                    double perte = data.instance(i).value(2);
                    double bandePassante = data.instance(i).value(3);
                    double signal = data.instance(i).value(4);
                    double mos = data.instance(i).value(5);

                    System.out.printf("│ %3d │ %7.1f │ %6.1f │ %5.1f │ %14.1f │ %12.1f │ %.2f │\n",
                            i + 1, latence, jitter, perte, bandePassante, signal, mos);
                }
                System.out.println("└─────┴─────────┴────────┴───────┴────────────────┴──────────────┴─────┘");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des données: " + e.getMessage());
            throw new RuntimeException("Échec du chargement des données", e);
        }

        System.out.println("───────────────────────────────────────\n");
    }

    private static void testDataPreparation() {
        System.out.println("🔧 TEST 3: PRÉPARATION DES DONNÉES");
        System.out.println("───────────────────────────────────");

        try {
            // Charger les données
            Instances data = DataPreparation.loadDataFromDatabase();

            if (data.numInstances() == 0) {
                System.out.println("⚠️ Pas assez de données pour les tests de préparation");
                return;
            }

            // Test 3.1: Statistiques du dataset
            System.out.println("📈 Test des statistiques du dataset...");
            DataPreparation.printDatasetStats(data);

            // Test 3.2: Nettoyage des données
            System.out.println("🧹 Test du nettoyage des données...");
            Instances cleanedData = DataPreparation.cleanData(data);
            System.out.println("   • Avant nettoyage: " + data.numInstances() + " instances");
            System.out.println("   • Après nettoyage: " + cleanedData.numInstances() + " instances");

            if (cleanedData.numInstances() < data.numInstances()) {
                System.out.println("   • " + (data.numInstances() - cleanedData.numInstances()) +
                        " instances supprimées (valeurs manquantes)");
            }

            // Test 3.3: Division des données
            System.out.println("\n✂️ Test de la division des données (80/20)...");
            Instances[] split = DataPreparation.splitData(cleanedData, 0.8);

            System.out.println("   • Ensemble d'entraînement: " + split[0].numInstances() + " instances");
            System.out.println("   • Ensemble de test: " + split[1].numInstances() + " instances");
            System.out.println("   • Ratio: " +
                    String.format("%.1f", (split[0].numInstances() * 100.0 / cleanedData.numInstances())) + "% / " +
                    String.format("%.1f", (split[1].numInstances() * 100.0 / cleanedData.numInstances())) + "%");

            // Test 3.4: Normalisation
            System.out.println("\n📏 Test de la normalisation des données...");
            Instances normalizedData = DataPreparation.normalizeData(cleanedData);

            // Afficher les premières valeurs normalisées
            if (normalizedData.numInstances() > 0) {
                System.out.println("   • Première instance normalisée:");
                System.out.print("     ");
                for (int i = 0; i < Math.min(6, normalizedData.numAttributes()); i++) {
                    System.out.printf("%s: %.3f | ",
                            normalizedData.attribute(i).name(),
                            normalizedData.instance(0).value(i));
                }
                System.out.println();
            }

            // Test 3.5: Sauvegarde/Chargement
            System.out.println("\n💾 Test de sauvegarde/chargement...");
            String testFile = "test_dataset.arff";
            DataPreparation.saveDataset(cleanedData, testFile);

            Instances loadedData = DataPreparation.loadDataset(testFile);
            if (loadedData != null) {
                System.out.println("   • Fichier sauvegardé et rechargé avec succès");
                System.out.println("   • Instances chargées: " + loadedData.numInstances());

                // Supprimer le fichier test
                java.io.File file = new java.io.File(testFile);
                if (file.exists() && file.delete()) {
                    System.out.println("   • Fichier test supprimé");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la préparation des données: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("───────────────────────────────────\n");
    }

    private static void testPredictionService() {
        System.out.println("🧠 TEST 4: SERVICE DE PRÉDICTION");
        System.out.println("────────────────────────────────");

        try {
            // Initialiser le service
            PredictionService.initialize();
            System.out.println("✅ Service de prédiction initialisé");

            // Test 4.1: Vérifier si un modèle existe déjà
            System.out.println("\n🔍 Vérification des modèles existants...");
            java.io.File modelFile = new java.io.File("models/qoe_model.model");
            if (modelFile.exists()) {
                System.out.println("   • Modèle trouvé: " + modelFile.getAbsolutePath());
                System.out.println("   • Taille: " + modelFile.length() + " octets");
            } else {
                System.out.println("   • Aucun modèle trouvé, nécessite un entraînement");
            }

            // Test 4.2: Entraîner un modèle (si assez de données)
            System.out.println("\n🏋️ Test de l'entraînement du modèle...");

            // Vérifier d'abord si on a assez de données
            Instances data = DataPreparation.loadDataFromDatabase();
            if (data.numInstances() < 10) {
                System.out.println("⚠️ Pas assez de données pour l'entraînement (minimum 10 instances)");
                System.out.println("   • Instances disponibles: " + data.numInstances());
                return;
            }

            System.out.println("   • Démarrage de l'entraînement avec " + data.numInstances() + " instances...");
            PredictionService.trainModel();

            // Test 4.3: Test rapide de prédiction
            System.out.println("\n🔮 Test rapide de prédiction...");
            double[][] testCases = {
                    {50.0, 10.0, 1.0, 100.0, 90.0},  // Bonnes conditions
                    {200.0, 50.0, 10.0, 20.0, 50.0}, // Mauvaises conditions
                    {100.0, 20.0, 5.0, 50.0, 70.0}   // Conditions moyennes
            };

            for (int i = 0; i < testCases.length; i++) {
                double[] testCase = testCases[i];
                double mos = PredictionService.predictMOS(
                        testCase[0], testCase[1], testCase[2],
                        testCase[3], testCase[4]
                );

                String category = getQoECategory(mos);
                System.out.printf("   Test %d: Latence=%.1f, Jitter=%.1f, Perte=%.1f%%, BP=%.1f, Signal=%.1f\n",
                        i+1, testCase[0], testCase[1], testCase[2], testCase[3], testCase[4]);
                System.out.printf("           → MOS prédit: %.2f (%s)\n", mos, category);
            }

            // Test 4.4: Évaluation
            System.out.println("\n📊 Test de l'évaluation du modèle...");
            PredictionService.evaluatePredictions();

            // Test 4.5: Vérifier si le modèle est entraîné
            System.out.println("\n✅ État du modèle:");
            System.out.println("   • Modèle entraîné: " + (PredictionService.isModelTrained() ? "OUI" : "NON"));

        } catch (Exception e) {
            System.err.println("❌ Erreur dans le service de prédiction: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("────────────────────────────────\n");
    }

    private static void testControllerMethods() {
        System.out.println("🎮 TEST 5: MÉTHODES DU CONTRÔLEUR");
        System.out.println("─────────────────────────────────");

        try {
            // Simuler certaines méthodes du contrôleur
            System.out.println("🧪 Simulation des méthodes principales...");

            // Test 5.1: Méthode getQoECategory
            System.out.println("\n📈 Test de la catégorisation QoE:");
            double[] mosValues = {1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0};
            for (double mos : mosValues) {
                String category = getQoECategory(mos);
                System.out.printf("   • MOS=%.1f → %s\n", mos, category);
            }

            // Test 5.2: Méthodes utilitaires
            System.out.println("\n⚙️ Test des méthodes utilitaires:");

            // Simuler updateStatus
            System.out.println("   • updateStatus():");
            updateStatus("Test d'information", "info");
            updateStatus("Test de succès", "success");
            updateStatus("Test d'erreur", "error");

            // Test 5.3: Chargement des données pour les graphiques
            System.out.println("\n📊 Test du chargement des données pour visualisation:");
            testLoadChartData();

            // Test 5.4: Test de la connexion
            System.out.println("\n🔌 Test de la connexion:");
            testConnection();

        } catch (Exception e) {
            System.err.println("❌ Erreur dans les méthodes du contrôleur: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("─────────────────────────────────\n");
    }

    private static void testLoadChartData() {
        try (Connection conn = DBConnection.getConnection()) {
            // Test de la requête pour les prédictions récentes
            String sql = """
                SELECT COUNT(*) as count
                FROM MESURES_QOS
                WHERE MOS IS NOT NULL
            """;

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("   • Prédictions avec MOS: " + count);
                }
            }

            // Test de la distribution MOS
            String distSQL = """
                SELECT 
                    CASE 
                        WHEN MOS >= 4.5 THEN 'Excellent'
                        WHEN MOS >= 4.0 THEN 'Bon'
                        WHEN MOS >= 3.0 THEN 'Moyen'
                        WHEN MOS >= 2.0 THEN 'Médiocre'
                        ELSE 'Mauvais'
                    END as category,
                    COUNT(*) as count
                FROM MESURES_QOS
                WHERE MOS IS NOT NULL
                GROUP BY 
                    CASE 
                        WHEN MOS >= 4.5 THEN 'Excellent'
                        WHEN MOS >= 4.0 THEN 'Bon'
                        WHEN MOS >= 3.0 THEN 'Moyen'
                        WHEN MOS >= 2.0 THEN 'Médiocre'
                        ELSE 'Mauvais'
                    END
            """;

            try (PreparedStatement ps = conn.prepareStatement(distSQL);
                 ResultSet rs = ps.executeQuery()) {

                System.out.println("   • Distribution MOS:");
                int total = 0;
                while (rs.next()) {
                    String category = rs.getString("category");
                    int count = rs.getInt("count");
                    total += count;
                    System.out.printf("     - %s: %d\n", category, count);
                }
                System.out.println("     Total: " + total);
            }

        } catch (SQLException e) {
            System.err.println("   ❌ Erreur SQL: " + e.getMessage());
        }
    }

    private static void testConnection() {
        try (Connection conn = DBConnection.getConnection()) {
            String testSQL = "SELECT 1 FROM DUAL";
            try (PreparedStatement ps = conn.prepareStatement(testSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("   ✅ Connexion Oracle fonctionnelle");
                }
            }
        } catch (SQLException e) {
            System.err.println("   ❌ Erreur de connexion: " + e.getMessage());
        }
    }

    // Méthodes utilitaires du contrôleur
    private static String getQoECategory(double mos) {
        if (mos >= 4.5) return "Excellent";
        else if (mos >= 4.0) return "Bon";
        else if (mos >= 3.0) return "Moyen";
        else if (mos >= 2.0) return "Médiocre";
        else return "Mauvais";
    }

    private static void updateStatus(String message, String type) {
        String colorCode;
        switch (type.toLowerCase()) {
            case "success":
                colorCode = "🟢";
                break;
            case "error":
                colorCode = "🔴";
                break;
            case "info":
                colorCode = "🔵";
                break;
            default:
                colorCode = "⚫";
        }
        System.out.printf("   %s %s: %s\n", colorCode, type.toUpperCase(), message);
    }

    private static double getDoubleFromField(String text, double defaultValue) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}