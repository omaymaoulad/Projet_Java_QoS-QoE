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
     * VERSION SIMPLIFIEE - Entraînement rapide
     */
    public static void trainModel() {
        System.out.println("\n========== ENTRAINEMENT ML ==========");

        try {
            // 1. Charger les données
            System.out.println("[1/5] Chargement donnees...");
            Instances data = DataPreparation.loadDataFromDatabase();

            if (data.numInstances() < 10) {
                System.err.println("[ERREUR] Pas assez de donnees (min 10)");
                return;
            }
            System.out.println("[OK] " + data.numInstances() + " instances chargees");

            // 2. Diviser train/test
            System.out.println("[2/5] Division train/test...");
            Instances[] split = DataPreparation.splitData(data, 0.8);
            Instances trainData = split[0];
            Instances testData = split[1];

            // 3. Créer et entraîner
            System.out.println("[3/5] Entrainement modele...");
            predictionModel = new QoEPredictionModels();

            // Toujours utiliser Régression Linéaire (rapide)
            predictionModel.trainLinearRegression(trainData);

            // 4. Évaluer
            System.out.println("[4/5] Evaluation...");
            predictionModel.evaluateModel(testData);

            // 5. Sauvegarder
            System.out.println("[5/5] Sauvegarde...");
            predictionModel.saveModel();

            modelTrained = true;
            System.out.println("\n[OK] ENTRAINEMENT TERMINE !\n");
            System.out.println("=====================================\n");

        } catch (Exception e) {
            System.err.println("[ERREUR] Entrainement : " + e.getMessage());
            e.printStackTrace();
        }
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
            System.out.println("[OK] Modele pre-entraine charge");
        } catch (Exception e) {
            System.err.println("[WARN] Impossible de charger le modele");
        }
    }

    /**
     * Prédire le MOS
     */
    public static double predictMOS(double latence, double jitter, double perte,
                                    double bandePassante, double signalScore) {

        if (!modelTrained) {
            loadPreTrainedModel();
        }

        if (!modelTrained || predictionModel == null) {
            System.err.println("[WARN] Aucun modele - utilisation formule");
            return calculateMOSFormula(latence, jitter, perte, bandePassante);
        }

        try {
            return predictionModel.predictMOS(latence, jitter, perte, bandePassante, signalScore);
        } catch (Exception e) {
            System.err.println("[ERREUR] Prediction ML: " + e.getMessage());
            return calculateMOSFormula(latence, jitter, perte, bandePassante);
        }
    }

    /**
     * Prédire pour toutes les données sans MOS
     */
    public static void predictMissingMOS() {
        System.out.println("\n========== PREDICTION MOS MANQUANTS ==========");

        if (!modelTrained) {
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
            ORDER BY ID_MESURE
        """;

        String updateSQL = "UPDATE MESURES_QOS SET MOS = ? WHERE ID_MESURE = ?";

        int count = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement selectPs = conn.prepareStatement(selectSQL);
             PreparedStatement updatePs = conn.prepareStatement(updateSQL);
             ResultSet rs = selectPs.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("ID_MESURE");
                double latence = rs.getDouble("LATENCE");
                double jitter = rs.getDouble("JITTER");
                double perte = rs.getDouble("PERTE");
                double bp = rs.getDouble("BANDE_PASSANTE");
                double signal = rs.getDouble("SIGNAL_SCORE");

                double predictedMOS = predictMOS(latence, jitter, perte, bp, signal);

                updatePs.setDouble(1, predictedMOS);
                updatePs.setInt(2, id);
                updatePs.executeUpdate();

                count++;
            }

            System.out.println("[OK] " + count + " MOS predits avec succes");
            System.out.println("==============================================\n");

        } catch (SQLException e) {
            System.err.println("[ERREUR] SQL : " + e.getMessage());
        }
    }

    /**
     * Évaluer les prédictions
     */
    public static void evaluatePredictions() {
        System.out.println("\n========== EVALUATION PREDICTIONS ==========");

        String sql = """
            SELECT ID_MESURE, LATENCE, JITTER, PERTE, 
                   BANDE_PASSANTE, SIGNAL_SCORE, MOS
            FROM MESURES_QOS
            WHERE MOS IS NOT NULL AND MOS > 0
            ORDER BY DBMS_RANDOM.VALUE
            FETCH FIRST 20 ROWS ONLY
        """;

        double totalError = 0;
        int count = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\nID    | MOS Reel | MOS Predit | Erreur");
            System.out.println("------------------------------------------");

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
                count++;

                System.out.printf("%5d |   %.2f   |    %.2f    | %.3f\n",
                        id, actualMOS, predictedMOS, error);
            }

            if (count > 0) {
                double mae = totalError / count;
                System.out.println("------------------------------------------");
                System.out.printf("MAE (Erreur Moyenne) : %.4f\n", mae);
            }
            System.out.println("============================================\n");

        } catch (SQLException e) {
            System.err.println("[ERREUR] Evaluation : " + e.getMessage());
        }
    }

    /**
     * Formule de calcul MOS (fallback)
     */
    private static double calculateMOSFormula(double latence, double jitter,
                                              double perte, double bandePassante) {
        double R = 93.2;

        if (latence > 150) R -= (latence - 150) / 10;
        R -= jitter / 3;
        R -= perte * 2.5;
        if (bandePassante < 10) R -= (10 - bandePassante) * 1.5;

        double mos;
        if (R < 0) mos = 1.0;
        else if (R > 100) mos = 4.5;
        else mos = 1 + 0.035 * R + R * (R - 60) * (100 - R) * 7e-6;

        return Math.max(1.0, Math.min(5.0, mos));
    }

    private static String getQoECategory(double mos) {
        if (mos >= 4.5) return "Excellent";
        else if (mos >= 4.0) return "Bon";
        else if (mos >= 3.0) return "Moyen";
        else if (mos >= 2.0) return "Mediocre";
        else return "Mauvais";
    }

    /**
     * Test rapide
     */
    public static void quickTest() {
        System.out.println("\n========== TEST RAPIDE ==========");

        double mos1 = predictMOS(20, 5, 0.1, 100, 90);
        System.out.printf("Scenario EXCELLENT: MOS = %.2f (%s)\n", mos1, getQoECategory(mos1));

        double mos2 = predictMOS(50, 15, 1.0, 50, 75);
        System.out.printf("Scenario BON: MOS = %.2f (%s)\n", mos2, getQoECategory(mos2));

        double mos3 = predictMOS(100, 30, 3.0, 20, 60);
        System.out.printf("Scenario MOYEN: MOS = %.2f (%s)\n", mos3, getQoECategory(mos3));

        double mos4 = predictMOS(200, 50, 10.0, 5, 40);
        System.out.printf("Scenario MAUVAIS: MOS = %.2f (%s)\n", mos4, getQoECategory(mos4));

        System.out.println("=================================\n");
    }

    public static boolean isModelTrained() {
        return modelTrained && predictionModel != null;
    }

    public static void printDatasetInfo() {
        System.out.println("\n========== INFO DATASET ==========");

        try (Connection conn = DBConnection.getConnection()) {
            String countSQL = "SELECT COUNT(*) FROM MESURES_QOS";
            try (PreparedStatement ps = conn.prepareStatement(countSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Total mesures : " + rs.getInt(1));
                }
            }

            String mosSQL = "SELECT COUNT(*) FROM MESURES_QOS WHERE MOS IS NOT NULL";
            try (PreparedStatement ps = conn.prepareStatement(mosSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Mesures avec MOS : " + rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERREUR] Info dataset : " + e.getMessage());
        }

        System.out.println("==================================\n");
    }

    public static void initialize() {
        // Initialisation si nécessaire
    }
}