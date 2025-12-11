package com.ensah.qoe.Services;

import com.ensah.qoe.ML.AnomalyDetectionModels;
import com.ensah.qoe.ML.DataPreparationAnomalie;
import com.ensah.qoe.Models.DBConnection;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.sql.*;

public class PredictionServiceAnomalies {

    private static AnomalyDetectionModels modelHandler;
    private static boolean modelTrained = false;
    public static int[][] confusion = new int[2][2];
    private static Filter normalizeFilter;
    private static Instances trainingHeader;
    private static weka.classifiers.Classifier classifier;
    public static String selectedAlgorithm = "J48";
    // ======================================================================
    // 1) ENTRAÎNER LE MODELE
    // ======================================================================
    public static void trainModel() {
        System.out.println("\n🤖 Entraînement Détection Anomalies — Version Optimisée");
        System.out.println("========================================================");

        // 1. Charger les données depuis DataPreparation
        Instances[] datasets = DataPreparationAnomalie.prepare();
        if (datasets == null) {
            System.err.println("❌ Impossible de charger les données");
            return;
        }

        Instances train = datasets[0];
        Instances test = datasets[1];

        System.out.println("📊 Train = " + train.numInstances());
        System.out.println("📊 Test  = " + test.numInstances());

        try {
            // 2. Initialiser la normalisation
            normalizeFilter = new Normalize();
            normalizeFilter.setInputFormat(train);

            Instances trainNorm = Filter.useFilter(train, normalizeFilter);
            Instances testNorm = Filter.useFilter(test, normalizeFilter);

            // Sauvegarder le header pour la prédiction
            trainingHeader = new Instances(trainNorm, 0);

            // 3. Choix du modèle selon dataset
            modelHandler = new AnomalyDetectionModels();

            if (train.numInstances() <= 50) {
                System.out.println("✔ Dataset petit → Modèle recommandé : " + selectedAlgorithm);

                if (selectedAlgorithm.contains("J48")) {
                    modelHandler.trainJ48(trainNorm);
                }
                else if (selectedAlgorithm.contains("Naive")) {
                    modelHandler.trainNaiveBayes(trainNorm);
                }
                else if (selectedAlgorithm.contains("KNN")) {
                    modelHandler.trainKNN(trainNorm);
                }
            } else {
                System.out.println("✔ Dataset moyen → Naive Bayes");
                modelHandler.trainNaiveBayes(trainNorm);
            }

            classifier = modelHandler.getModel();

            // 4. Évaluation
            System.out.println("\n📈 Évaluation du modèle :");
            modelHandler.evaluate(testNorm);

            // 5. Validation croisée
            System.out.println("\n🔄 Validation croisée 10-fold :");
            modelHandler.crossValidate(trainNorm);

            // ============================================================
            // 🔥 Sauvegarde modèle + filtre + header pour prédiction future
            // ============================================================
            try {
                SerializationHelper.write("models/anomaly.model", classifier);
                SerializationHelper.write("models/anomaly_norm.filter", normalizeFilter);
                SerializationHelper.write("models/anomaly_header.model", trainingHeader);

                System.out.println("💾 Modèle sauvegardé dans /models/");
            } catch (Exception e) {
                System.err.println("❌ Erreur sauvegarde modèle : " + e.getMessage());
            }

            modelTrained = true;
            System.out.println("\n✅ Modèle anomalies entraîné avec succès !");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // ======================================================================
    // 2) CHARGER UN MODELE SAUVEGARDÉ
    // ======================================================================
    public static void loadPreTrainedModel() {
        try {

            System.out.println("📥 Chargement du modèle enregistré...");

            classifier = (weka.classifiers.Classifier)
                    SerializationHelper.read("models/anomaly.model");

            normalizeFilter = (Normalize)
                    SerializationHelper.read("models/anomaly_norm.filter");

            trainingHeader = (Instances)
                    SerializationHelper.read("models/anomaly_header.model");

            modelTrained = true;

            System.out.println("✅ Modèle, filtre et header chargés avec succès !");

        } catch (Exception e) {
            modelTrained = false;
            System.err.println("❌ Erreur lors du chargement du modèle : " + e.getMessage());
        }
    }



    // ======================================================================
    // 3) PRÉDICTION (simple)
    // ======================================================================
    public static String predictAnomaly(double lat, double jit, double perte,
                                        double bp, double signalScore) {

        if (!modelTrained) {
            loadPreTrainedModel();
        }
        if (!modelTrained) {
            System.err.println("❌ Modèle non prêt.");
            return "NORMAL";
        }

        double[] p = predictWithProbability(lat, jit, perte, bp, signalScore);
        return p[1] > 0.5 ? "ANOMALIE" : "NORMAL";
    }


    // ======================================================================
    // 4) PRÉDICTION AVEC PROBABILITÉS
    // ======================================================================
    public static double[] predictWithProbability(
            double lat, double jit, double perte,
            double bp, double signalScore) {

        if (!modelTrained || classifier == null || trainingHeader == null || normalizeFilter == null) {
            System.err.println("❌ Modèle non initialisé. Chargement du modèle...");
            loadPreTrainedModel();
            if (!modelTrained) return new double[]{0.5, 0.5};
        }

        try {
            // 1. Créer instance brute CONFORME AU HEADER
            Instance inst = new DenseInstance(trainingHeader.numAttributes());
            inst.setDataset(trainingHeader);

            inst.setValue(0, lat);
            inst.setValue(1, jit);
            inst.setValue(2, perte);
            inst.setValue(3, bp);
            inst.setValue(4, signalScore);

            // 2. Appliquer la normalisation déjà entraînée (TRÈS IMPORTANT)
            Instances temp = new Instances(trainingHeader, 0);
            temp.add(inst);

            Normalize norm = (Normalize) normalizeFilter;
            Instances normalized = Filter.useFilter(temp, norm);

            Instance normInst = normalized.instance(0);

            // 3. Prédire probabilités
            return classifier.distributionForInstance(normInst);

        } catch (Exception e) {
            System.err.println("❌ Erreur prédiction : " + e.getMessage());
            return new double[]{0.5, 0.5};
        }
    }




    // ======================================================================
    // 5) PRÉDICTIONS EN MASSE → mise à jour DB
    // ======================================================================
    public static void predictMissingAnomalies() {
        System.out.println("\n🔍 Mise à jour des anomalies manquantes…");

        if (!modelTrained) loadPreTrainedModel();
        if (!modelTrained) return;

        String selectSQL = """
            SELECT ID_MESURE, LATENCE, JITTER, PERTE,
                   BANDE_PASSANTE, SIGNAL_SCORE
            FROM MESURES_QOS
            WHERE ANOMALIE IS NULL
        """;

        String updateSQL = "UPDATE MESURES_QOS SET ANOMALIE = ? WHERE ID_MESURE = ?";

        int count = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSQL);
             PreparedStatement upd = conn.prepareStatement(updateSQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String pred = predictAnomaly(
                        rs.getDouble("LATENCE"),
                        rs.getDouble("JITTER"),
                        rs.getDouble("PERTE"),
                        rs.getDouble("BANDE_PASSANTE"),
                        rs.getDouble("SIGNAL_SCORE")
                );

                int anomalyValue = pred.equals("ANOMALIE") ? 1 : 0;

                upd.setInt(1, anomalyValue);
                upd.setInt(2, rs.getInt("ID_MESURE"));
                upd.executeUpdate();

                count++;
            }

            System.out.println("✅ " + count + " anomalies mises à jour !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ======================================================================
    // 6) ÉVALUATION SUR DB
    // ======================================================================
    public static void evaluateOnDatabase() {
        System.out.println("\n📊 Évaluation du modèle sur DB");

        String sql = """
            SELECT LATENCE, JITTER, PERTE, BANDE_PASSANTE, SIGNAL_SCORE, ANOMALIE
            FROM MESURES_QOS
            WHERE ANOMALIE IS NOT NULL
            FETCH FIRST 80 ROWS ONLY
        """;

        int correct = 0;
        int total = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int real = rs.getInt("ANOMALIE");
                String pred = predictAnomaly(
                        rs.getDouble("LATENCE"),
                        rs.getDouble("JITTER"),
                        rs.getDouble("PERTE"),
                        rs.getDouble("BANDE_PASSANTE"),
                        rs.getDouble("SIGNAL_SCORE")
                );
                int predicted = pred.equals("ANOMALIE") ? 1 : 0;

                if (real == predicted) correct++;
                total++;
            }

            System.out.println("Accuracy DB : " +
                    String.format("%.2f%%", (correct * 100.0) / total));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void setSelectedAlgorithm(String algo) {
        selectedAlgorithm = algo;
    }
    /**
     * Charger la matrice de confusion depuis la base Oracle
     * et la mettre dans PredictionServiceAnomalies.confusion
     */
    public static void loadConfusionMatrix() {

        // Reset (cas où pas encore évalué)
        confusion[0][0] = 0; // TN
        confusion[0][1] = 0; // FP
        confusion[1][0] = 0; // FN
        confusion[1][1] = 0; // TP

        String sql = """
        SELECT 
            ANOMALIE AS actual,
            PREDICTION AS predicted,
            COUNT(*) AS total
        FROM (
            SELECT 
                ANOMALIE,
                CASE 
                    WHEN LATENCE > 200 OR JITTER > 50 OR PERTE > 10 OR SIGNAL_SCORE < 30
                         THEN 1 
                    ELSE 0
                END AS PREDICTION
            FROM MESURES_QOS
            WHERE ANOMALIE IS NOT NULL
        )
        GROUP BY ANOMALIE, PREDICTION
        ORDER BY actual, predicted
    """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int actual = rs.getInt("actual");
                int predicted = rs.getInt("predicted");
                int count = rs.getInt("total");

                // actual = 0 (normal) , actual = 1 (anomalie)
                // predicted = 0 / 1
                confusion[actual][predicted] = count;
            }

            System.out.println("\n📊 MATRICE DE CONFUSION CHARGÉE :");
            System.out.println("TN = " + confusion[0][0] + "   FP = " + confusion[0][1]);
            System.out.println("FN = " + confusion[1][0] + "   TP = " + confusion[1][1]);

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du chargement de la matrice de confusion : " + e.getMessage());
        }
    }
    public static boolean isModelTrained() {
        return modelTrained;
    }
    public static void evaluatePredictions() {
        evaluateOnDatabase();
    }
    public static void analyzeAnomalyPrediction(double lat, double jit, double perte,
                                                double bp, double signalScore) {

        System.out.println("\n🔍 ANALYSE DÉTAILLÉE DE PRÉDICTION");
        System.out.println("=====================================");

        String pred = predictAnomaly(lat, jit, perte, bp, signalScore);
        double[] probs = predictWithProbability(lat, jit, perte, bp, signalScore);

        System.out.println("Latence        : " + lat);
        System.out.println("Jitter         : " + jit);
        System.out.println("Perte          : " + perte);
        System.out.println("Bande passante : " + bp);
        System.out.println("Signal Score   : " + signalScore);

        System.out.println("\nPrédiction : " + pred);
        System.out.println("Probabilité anomalie : " + String.format("%.2f%%", probs[1] * 100));
    }
    public static void checkAnomalyMOSCorrelation() {
        System.out.println("\nℹ Vérification MOS→Anomalie (info)");
        System.out.println("Cette version du modèle n’utilise plus MOS.");
    }
    public static double[] predictAnomalyWithProbability(double lat, double jit, double perte,
                                                         double bp, double signalScore) {
        return predictWithProbability(lat, jit, perte, bp, signalScore);
    }

}
