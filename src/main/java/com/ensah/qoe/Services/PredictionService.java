package com.ensah.qoe.Services;

import com.ensah.qoe.ML.DataPreparation;
import com.ensah.qoe.ML.QoEPredictionModels;
import com.ensah.qoe.Models.DBConnection;
import weka.core.Instances;

import java.sql.*;

public class PredictionService {

    private static QoEPredictionModels predictionModel;
    private static boolean modelTrained = false;

    /**
     * Entraîner le modèle avec toutes les données disponibles
     */
    public static void trainModel() {
        System.out.println("\n🤖 DÉMARRAGE DE L'ENTRAÎNEMENT ML");
        System.out.println("═══════════════════════════════════════");

        // 1. Charger les données
        Instances data = DataPreparation.loadDataFromDatabase();

        if (data.numInstances() < 10) {
            System.err.println("❌ Pas assez de données pour entraîner (minimum 10 instances)");
            System.err.println("Trouvé: " + data.numInstances() + " instances");
            System.err.println("Vérifiez que la table MESURES_QOS contient des données valides");
            return;
        }

        // 2. Afficher les stats
        DataPreparation.printDatasetStats(data);
        DataPreparation.printDescriptiveStats(data);

        // 3. Nettoyer les données
        data = DataPreparation.cleanData(data);
        System.out.println("✅ Données nettoyées : " + data.numInstances() + " instances");

        // 4. Normaliser (recommandé pour Random Forest)
        Instances normalizedData = DataPreparation.normalizeData(data);

        // 5. Diviser en train/test (80/20)
        Instances[] split = DataPreparation.splitData(normalizedData, 0.8);
        Instances trainData = split[0];
        Instances testData = split[1];

        System.out.println("📊 Train set : " + trainData.numInstances() + " instances");
        System.out.println("📊 Test set : " + testData.numInstances() + " instances");

        // 6. Créer et entraîner le modèle
        predictionModel = new QoEPredictionModels();

        // Choisir le modèle selon les données disponibles
        if (data.numInstances() < 50) {
            System.out.println("⚠️ Peu de données -> Utilisation de la Régression Linéaire");
            predictionModel.trainLinearRegression(trainData);
        } else {
            System.out.println("✅ Nombre de données suffisant -> Utilisation de Random Forest");
            predictionModel.trainRandomForest(trainData);
        }

        // 7. Évaluer le modèle
        predictionModel.evaluateModel(testData);

        // 8. Validation croisée
        predictionModel.crossValidation(data);

        // 9. Afficher importance des features
        predictionModel.printFeatureImportance();

        // 10. Sauvegarder le modèle
        predictionModel.saveModel();

        modelTrained = true;

        System.out.println("✅ ENTRAÎNEMENT TERMINÉ !");
        System.out.println("═══════════════════════════════════════\n");
    }

    /**
     * Charger un modèle pré-entraîné
     */
    public static void loadPreTrainedModel() {
        if (predictionModel == null) {
            predictionModel = new QoEPredictionModels();
        }
        try {
            predictionModel.loadModel();
            modelTrained = true;
            System.out.println("✅ Modèle pré-entraîné chargé avec succès!");
        } catch (Exception e) {
            System.err.println("❌ Impossible de charger le modèle : " + e.getMessage());
            System.err.println("Le modèle sera entraîné à la prochaine prédiction.");
        }
    }

    /**
     * Prédire le MOS pour une nouvelle mesure QoS
     */
    public static double predictMOS(double latence, double jitter, double perte,
                                    double bandePassante, double signalScore) {

        if (!modelTrained) {
            System.out.println("⚠️ Modèle non entraîné, tentative de chargement...");
            loadPreTrainedModel();
        }

        if (!modelTrained || predictionModel == null) {
            System.err.println("❌ Aucun modèle disponible - utilisation de la formule");
            return calculateMOSFormula(latence, jitter, perte, bandePassante);
        }

        try {
            return predictionModel.predictMOS(latence, jitter, perte, bandePassante, signalScore);
        } catch (Exception e) {
            System.err.println("❌ Erreur de prédiction avec ML: " + e.getMessage());
            System.err.println("Utilisation de la formule de secours");
            return calculateMOSFormula(latence, jitter, perte, bandePassante);
        }
    }

    /**
     * Prédire pour toutes les nouvelles données QoS sans MOS
     */
    public static void predictMissingMOS() {
        System.out.println("\n🔮 PRÉDICTION DES MOS MANQUANTS DANS MESURES_QOS");
        System.out.println("══════════════════════════════════════════════");

        if (!modelTrained) {
            System.out.println("⚠️ Modèle non entraîné, tentative de chargement...");
            loadPreTrainedModel();
        }

        String selectSQL = """
            SELECT ID_MESURE, LATENCE, JITTER, PERTE, 
                   BANDE_PASSANTE, SIGNAL_SCORE
            FROM MESURES_QOS
            WHERE (MOS IS NULL OR MOS = 0)
              AND LATENCE IS NOT NULL
              AND JITTER IS NOT NULL
              AND PERTE IS NOT NULL
              AND BANDE_PASSANTE IS NOT NULL
              AND SIGNAL_SCORE IS NOT NULL
            ORDER BY ID_MESURE
        """;

        String updateSQL = "UPDATE MESURES_QOS SET MOS = ? WHERE ID_MESURE = ?";

        int count = 0;
        int errors = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement selectPs = conn.prepareStatement(selectSQL);
             PreparedStatement updatePs = conn.prepareStatement(updateSQL);
             ResultSet rs = selectPs.executeQuery()) {

            System.out.println("Recherche des MOS manquants...");

            while (rs.next()) {
                try {
                    int id = rs.getInt("ID_MESURE");
                    double latence = rs.getDouble("LATENCE");
                    double jitter = rs.getDouble("JITTER");
                    double perte = rs.getDouble("PERTE");
                    double bp = rs.getDouble("BANDE_PASSANTE");
                    double signal = rs.getDouble("SIGNAL_SCORE");

                    // Prédire le MOS
                    double predictedMOS = predictMOS(latence, jitter, perte, bp, signal);

                    // Mettre à jour dans la DB
                    updatePs.setDouble(1, predictedMOS);
                    updatePs.setInt(2, id);
                    updatePs.executeUpdate();

                    count++;

                    if (count % 10 == 0) {
                        System.out.println("✅ " + count + " prédictions effectuées...");
                    }

                } catch (SQLException e) {
                    errors++;
                    System.err.println("❌ Erreur sur ID_MESURE " + rs.getInt("ID_MESURE") + ": " + e.getMessage());
                }
            }

            System.out.println("\n══════════════════════════════════════════════");
            System.out.println("✅ Total : " + count + " MOS prédits avec succès");
            if (errors > 0) {
                System.out.println("⚠️ " + errors + " erreurs rencontrées");
            }
            System.out.println("══════════════════════════════════════════════\n");

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL lors de la prédiction : " + e.getMessage());
            System.err.println("SQL: " + selectSQL);
            e.printStackTrace();
        }
    }

    /**
     * Évaluer la qualité des prédictions
     */
    public static void evaluatePredictions() {
        System.out.println("\n📊 ÉVALUATION DES PRÉDICTIONS SUR MESURES_QOS");
        System.out.println("════════════════════════════════════════════");

        String sql = """
            SELECT ID_MESURE, LATENCE, JITTER, PERTE, 
                   BANDE_PASSANTE, SIGNAL_SCORE,
                   MOS
            FROM MESURES_QOS
            WHERE MOS IS NOT NULL 
              AND MOS > 0
              AND LATENCE IS NOT NULL
              AND JITTER IS NOT NULL
              AND PERTE IS NOT NULL
              AND BANDE_PASSANTE IS NOT NULL
              AND SIGNAL_SCORE IS NOT NULL
            ORDER BY DBMS_RANDOM.VALUE
            FETCH FIRST 50 ROWS ONLY
        """;

        double totalError = 0;
        double totalSquaredError = 0;
        int count = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\nID    | MOS Réel | MOS Prédit | Erreur | Catégorie");
            System.out.println("─────────────────────────────────────────────────────");

            while (rs.next()) {
                int id = rs.getInt("ID_MESURE");
                double actualMOS = rs.getDouble("MOS");
                double predictedMOS = predictMOS(
                        rs.getDouble("LATENCE"),
                        rs.getDouble("JITTER"),
                        rs.getDouble("PERTE"),
                        rs.getDouble("BANDE_PASSANTE"),
                        rs.getDouble("SIGNAL_SCORE")
                );

                double error = Math.abs(actualMOS - predictedMOS);
                totalError += error;
                totalSquaredError += error * error;
                count++;

                String actualCategory = getQoECategory(actualMOS);
                String predictedCategory = getQoECategory(predictedMOS);

                System.out.printf("%5d |   %.2f   |    %.2f    |  %.3f | %s → %s\n",
                        id, actualMOS, predictedMOS, error, actualCategory, predictedCategory);
            }

            if (count > 0) {
                double mae = totalError / count;
                double rmse = Math.sqrt(totalSquaredError / count);
                double accuracy = (count - totalError) / count * 100;

                System.out.println("─────────────────────────────────────────────────────");
                System.out.printf("MAE (Mean Absolute Error) : %.4f\n", mae);
                System.out.printf("RMSE (Root Mean Squared Error) : %.4f\n", rmse);
                System.out.printf("Accuracy approximative : %.1f%%\n", accuracy);
                System.out.println("════════════════════════════════════════════════\n");
            } else {
                System.out.println("❌ Aucune donnée trouvée pour l'évaluation");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur évaluation : " + e.getMessage());
        }
    }

    /**
     * Formule de calcul MOS (fallback si pas de ML)
     */
    private static double calculateMOSFormula(double latence, double jitter,
                                              double perte, double bandePassante) {
        // R-Factor calculation simplifié (basé sur E-Model)
        double R = 93.2; // Valeur de base

        // Impact de la latence (ms)
        if (latence > 150) {
            R -= (latence - 150) / 10;
        }

        // Impact du jitter (ms)
        R -= jitter / 3;

        // Impact de la perte de paquets (%)
        R -= perte * 2.5;

        // Impact de la bande passante (Mbps)
        if (bandePassante < 10) {
            R -= (10 - bandePassante) * 1.5;
        }

        // Convertir R en MOS (1-5)
        double mos;
        if (R < 0) {
            mos = 1.0;
        } else if (R > 100) {
            mos = 4.5;
        } else {
            mos = 1 + 0.035 * R + R * (R - 60) * (100 - R) * 7e-6;
        }

        // Assurer que MOS est entre 1 et 5
        return Math.max(1.0, Math.min(5.0, mos));
    }

    /**
     * Catégorie QoE basée sur le MOS
     */
    private static String getQoECategory(double mos) {
        if (mos >= 4.5) return "Excellent";
        else if (mos >= 4.0) return "Bon";
        else if (mos >= 3.0) return "Moyen";
        else if (mos >= 2.0) return "Médiocre";
        else return "Mauvais";
    }

    /**
     * Test rapide du système de prédiction
     */
    public static void quickTest() {
        System.out.println("\n🧪 TEST RAPIDE DE PRÉDICTION QoE");
        System.out.println("════════════════════════════════════");

        System.out.println("\n📊 Scénarios de test avec valeurs typiques:");
        System.out.println("────────────────────────────────────");

        // Scénario 1 : Excellente qualité
        double mos1 = predictMOS(20, 5, 0.1, 100, 90);
        System.out.printf("Scénario EXCELLENT (Latence: 20ms, Perte: 0.1%%)\n");
        System.out.printf("  → MOS prédit: %.2f (%s)\n", mos1, getQoECategory(mos1));

        // Scénario 2 : Bonne qualité
        double mos2 = predictMOS(50, 15, 1.0, 50, 75);
        System.out.printf("\nScénario BON (Latence: 50ms, Perte: 1.0%%)\n");
        System.out.printf("  → MOS prédit: %.2f (%s)\n", mos2, getQoECategory(mos2));

        // Scénario 3 : Qualité moyenne
        double mos3 = predictMOS(100, 30, 3.0, 20, 60);
        System.out.printf("\nScénario MOYEN (Latence: 100ms, Perte: 3.0%%)\n");
        System.out.printf("  → MOS prédit: %.2f (%s)\n", mos3, getQoECategory(mos3));

        // Scénario 4 : Mauvaise qualité
        double mos4 = predictMOS(200, 50, 10.0, 5, 40);
        System.out.printf("\nScénario MAUVAIS (Latence: 200ms, Perte: 10.0%%)\n");
        System.out.printf("  → MOS prédit: %.2f (%s)\n", mos4, getQoECategory(mos4));

        System.out.println("\n════════════════════════════════════\n");
    }

    /**
     * Vérifier l'état du modèle
     */
    public static boolean isModelTrained() {
        return modelTrained && predictionModel != null;
    }

    /**
     * Obtenir des statistiques sur le dataset
     */
    public static void printDatasetInfo() {
        System.out.println("\n📋 INFORMATIONS SUR LE DATASET MESURES_QOS");
        System.out.println("══════════════════════════════════════");

        try (Connection conn = DBConnection.getConnection()) {
            // Nombre total de mesures
            String countSQL = "SELECT COUNT(*) FROM MESURES_QOS";
            try (PreparedStatement ps = conn.prepareStatement(countSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Total des mesures : " + rs.getInt(1));
                }
            }

            // Nombre de mesures avec MOS
            String mosSQL = "SELECT COUNT(*) FROM MESURES_QOS WHERE MOS IS NOT NULL";
            try (PreparedStatement ps = conn.prepareStatement(mosSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Mesures avec MOS : " + rs.getInt(1));
                }
            }

            // Statistiques MOS
            String statsSQL = """
                SELECT 
                    MIN(MOS) as MIN_MOS,
                    MAX(MOS) as MAX_MOS,
                    AVG(MOS) as AVG_MOS,
                    STDDEV(MOS) as STD_MOS
                FROM MESURES_QOS 
                WHERE MOS IS NOT NULL
            """;
            try (PreparedStatement ps = conn.prepareStatement(statsSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.printf("MOS - Min: %.2f, Max: %.2f, Moyenne: %.2f, Écart-type: %.2f\n",
                            rs.getDouble("MIN_MOS"),
                            rs.getDouble("MAX_MOS"),
                            rs.getDouble("AVG_MOS"),
                            rs.getDouble("STD_MOS")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des infos : " + e.getMessage());
        }

        System.out.println("══════════════════════════════════════\n");
    }
}