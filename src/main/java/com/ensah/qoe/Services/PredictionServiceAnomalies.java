package com.ensah.qoe.Services;

import com.ensah.qoe.ML.AnomalyDetectionModels;
import com.ensah.qoe.ML.DataPreparationAnomalie;
import weka.core.*;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.io.InputStream;
import java.util.*;

public class PredictionServiceAnomalies {

    // =========================
    // ÉTAT GLOBAL
    // =========================
    private static final AnomalyDetectionModels modelHandler = new AnomalyDetectionModels();
    private static Instances trainingHeader;
    private static Instances originalDataset; // ✅ NOUVEAU : Dataset ORIGINAL avec zone

    private static boolean modelTrained = false;
    private static boolean modelReady = false;
    // Métriques
    private static double lastAccuracy = 0.0;
    private static double lastPrecision = 0.0;
    private static double lastRecall = 0.0;
    private static String lastConfusionMatrix = "";
    private static double lastF1 = 0.0;

    // =========================
    // ✅ NOUVELLE FONCTION : Charger CSV original
    // =========================
    private static Instances loadOriginalCSV(String resourcePath) throws Exception {
        InputStream input = DataPreparationAnomalie.class.getResourceAsStream(resourcePath);
        if (input == null) {
            throw new Exception("❌ Fichier introuvable : " + resourcePath);
        }

        CSVLoader loader = new CSVLoader();
        loader.setSource(input);
        Instances data = loader.getDataSet();

        // Définir la classe
        Attribute anomalyAttr = data.attribute("anomalie");
        if (anomalyAttr != null) {
            data.setClass(anomalyAttr);
        }

        return data;
    }

    // =========================
    // 1️⃣ ENTRAÎNEMENT (INCHANGÉ)
    // =========================
    public static String trainModel() {

        StringBuilder report = new StringBuilder();

        try {
            Instances[] datasets =
                    DataPreparationAnomalie.prepareFromResources("/CSV/prediction_dataset.csv");

            Instances trainData = datasets[0];
            Instances testData  = datasets[1];

            // ✅ Charger le dataset ORIGINAL avec zones
            originalDataset = loadOriginalCSV("/CSV/prediction_dataset.csv");

            // Header (structure des features)
            trainingHeader = new Instances(trainData, 0);

            // Vérification ordre des classes
            System.out.println("Classes détectées :");
            for (int i = 0; i < trainingHeader.classAttribute().numValues(); i++) {
                System.out.println(i + " -> " + trainingHeader.classAttribute().value(i));
            }

            // Entraînement
            modelHandler.trainRandomForest(trainData);

            // Évaluation
            AnomalyDetectionModels.EvaluationResult result =
                    modelHandler.evaluate(testData);

            lastAccuracy = result.accuracy;
            lastPrecision = result.precision;
            lastRecall = result.recall;
            lastConfusionMatrix = result.confusionMatrix;
            lastF1 = (2 * lastPrecision * lastRecall) / (lastPrecision + lastRecall + 1e-9);
            modelTrained = true;
            modelReady = true;
            report.append("=== ENTRAÎNEMENT TERMINÉ ===\n");
            report.append("Accuracy : ").append(String.format("%.2f%%", lastAccuracy)).append("\n");
            report.append("Precision: ").append(String.format("%.3f", lastPrecision)).append("\n");
            report.append("Recall   : ").append(String.format("%.3f", lastRecall)).append("\n");
            report.append("F1-score : ").append(String.format("%.3f", lastF1)).append("\n");
        } catch (Exception e) {
            report.append("❌ ERREUR ENTRAÎNEMENT: ").append(e.getMessage());
            e.printStackTrace();
        }

        return report.toString();
    }

    // =========================
    // ✅ NOUVELLES FONCTIONS POUR STATISTIQUES
    // =========================

    /**
     * Retourne le nombre total de lignes dans le CSV original
     */
    public static int getTotalInstances() {
        if (originalDataset == null) return 0;
        return originalDataset.numInstances();
    }

    /**
     * Retourne le nombre d'instances normales
     */
    public static int getNormalCount() {
        if (originalDataset == null || !modelTrained) return 0;

        int count = 0;
        Attribute classAttr = originalDataset.attribute("anomalie");
        if (classAttr == null) return 0;

        for (int i = 0; i < originalDataset.numInstances(); i++) {
            Instance inst = originalDataset.instance(i);
            if (!inst.isMissing(classAttr)) {
                double value = inst.value(classAttr);
                if (value == 0.0) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Retourne le nombre d'anomalies
     */
    public static int getAnomalyCount() {
        if (originalDataset == null || !modelTrained) return 0;

        int count = 0;
        Attribute classAttr = originalDataset.attribute("anomalie");
        if (classAttr == null) return 0;

        for (int i = 0; i < originalDataset.numInstances(); i++) {
            Instance inst = originalDataset.instance(i);
            if (!inst.isMissing(classAttr)) {
                double value = inst.value(classAttr);
                if (value == 1.0) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Retourne les données pour un graphique scatter de 2 features
     */
    public static List<DataPoint> getScatterData(String feature1, String feature2) {
        List<DataPoint> points = new ArrayList<>();

        if (originalDataset == null || !modelTrained) return points;

        Attribute attr1 = originalDataset.attribute(feature1);
        Attribute attr2 = originalDataset.attribute(feature2);
        Attribute classAttr = originalDataset.attribute("anomalie");

        if (attr1 == null || attr2 == null || classAttr == null) return points;

        // Limiter à 500 points pour la performance
        int step = Math.max(1, originalDataset.numInstances() / 500);

        for (int i = 0; i < originalDataset.numInstances(); i += step) {
            Instance inst = originalDataset.instance(i);

            if (inst.isMissing(attr1) || inst.isMissing(attr2) || inst.isMissing(classAttr)) {
                continue;
            }

            double x = inst.value(attr1);
            double y = inst.value(attr2);
            boolean isAnomaly = inst.value(classAttr) == 1.0;

            points.add(new DataPoint(x, y, isAnomaly));
        }

        return points;
    }

    /**
     * Retourne les statistiques par zone (depuis le CSV original)
     */
    public static List<ZoneStats> getZoneStatistics() {
        List<ZoneStats> zones = new ArrayList<>();

        if (originalDataset == null || !modelTrained) return zones;

        Attribute zoneAttr = originalDataset.attribute("zone");
        Attribute classAttr = originalDataset.attribute("anomalie");

        if (zoneAttr == null || classAttr == null) return zones;

        // Map pour compter par zone
        Map<String, int[]> zoneMap = new HashMap<>();

        for (int i = 0; i < originalDataset.numInstances(); i++) {
            Instance inst = originalDataset.instance(i);

            if (inst.isMissing(zoneAttr) || inst.isMissing(classAttr)) {
                continue;
            }

            String zoneName = inst.stringValue(zoneAttr);
            boolean isAnomaly = inst.value(classAttr) == 1.0;

            zoneMap.putIfAbsent(zoneName, new int[]{0, 0}); // [normal, anomaly]

            if (isAnomaly) {
                zoneMap.get(zoneName)[1]++;
            } else {
                zoneMap.get(zoneName)[0]++;
            }
        }

        // Convertir en liste
        for (Map.Entry<String, int[]> entry : zoneMap.entrySet()) {
            String zoneName = entry.getKey();
            int normalCount = entry.getValue()[0];
            int anomalyCount = entry.getValue()[1];

            zones.add(new ZoneStats(zoneName, normalCount, anomalyCount));
        }

        // Trier par nombre total décroissant
        zones.sort((a, b) -> Integer.compare(b.getTotal(), a.getTotal()));

        return zones;
    }

    // =========================
    // 2️⃣ PRÉDICTION (INCHANGÉE - utilise déjà le bon filtre)
    // =========================
    public static PredictionResult predict(
            double latency, double jitter, double loss,
            double bandwidth, double signal) {

        if (!modelTrained || trainingHeader == null) {
            return new PredictionResult("ERREUR", 0, 0, "Modèle non prêt");
        }

        try {
            // ==================================================
            // 1️⃣ Création de l'instance brute (SANS mos)
            // ==================================================
            DenseInstance instance = new DenseInstance(trainingHeader.numAttributes());
            instance.setDataset(trainingHeader);

            instance.setValue(trainingHeader.attribute("latence"), latency);
            instance.setValue(trainingHeader.attribute("jitter"), jitter);
            instance.setValue(trainingHeader.attribute("loss_rate"), loss);
            instance.setValue(trainingHeader.attribute("bande_passante"), bandwidth);
            instance.setValue(trainingHeader.attribute("signal_score"), signal);

            instance.setClassMissing();

            // ==================================================
            // 2️⃣ Normalisation avec le filtre du TRAIN
            // ==================================================
            Normalize normalize = DataPreparationAnomalie.getNormalizeFilter();
            if (normalize == null) {
                throw new IllegalStateException("Filtre de normalisation non initialisé");
            }

            Instances temp = new Instances(trainingHeader, 0);
            temp.add(instance);
            temp.setClassIndex(trainingHeader.classIndex());

            Instances normalizedTemp = Filter.useFilter(temp, normalize);
            normalizedTemp.setClassIndex(trainingHeader.classIndex());

            Instance normalizedInstance = normalizedTemp.firstInstance();

            // ==================================================
            // 3️⃣ Prédiction
            // ==================================================
            double predictionValue =
                    modelHandler.getModel().classifyInstance(normalizedInstance);

            // Cas 1️⃣ : classe NUMÉRIQUE (0 / 1)
            if (trainingHeader.classAttribute().isNumeric()) {

                boolean isAnomaly = predictionValue >= 0.5;

                String prediction = isAnomaly ? "ANOMALIE" : "NORMAL";

                double confidence = isAnomaly
                        ? predictionValue
                        : (1.0 - predictionValue);

                return new PredictionResult(
                        prediction,
                        confidence,
                        1.0 - confidence,
                        "OK"
                );
            }

            // Cas 2️⃣ : classe NOMINALE
            else {

                double[] dist =
                        modelHandler.getModel().distributionForInstance(normalizedInstance);

                int anomalyIndex =
                        trainingHeader.classAttribute().indexOfValue("1");

                double anomalyProb = dist[anomalyIndex];
                double normalProb  = 1.0 - anomalyProb;

                String prediction =
                        anomalyProb >= 0.5 ? "ANOMALIE" : "NORMAL";

                return new PredictionResult(
                        prediction,
                        anomalyProb,
                        normalProb,
                        "OK"
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new PredictionResult("ERREUR", 0, 0, e.getMessage());
        }
    }

    // Alias pour le Dashboard
    public static PredictionResult predictAnomaly(
            double lat, double jit, double loss,
            double bw, double sig
    ) {
        return predict(lat, jit, loss, bw, sig);
    }

    // =========================
    // 3️⃣ ÉVALUATION (INCHANGÉE)
    // =========================
    public static String evaluateModel() {

        if (!modelTrained) {
            throw new IllegalStateException("Modèle non entraîné");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== ÉVALUATION DU MODÈLE ===\n");
        sb.append("Accuracy : ").append(String.format("%.2f%%", lastAccuracy)).append("\n");
        sb.append("Precision: ").append(String.format("%.3f", lastPrecision)).append("\n");
        sb.append("Recall   : ").append(String.format("%.3f", lastRecall)).append("\n");
        sb.append("Confusion Matrix:\n").append(lastConfusionMatrix).append("\n");
        sb.append("F1-score : ").append(String.format("%.3f", lastF1)).append("\n");
        return sb.toString();
    }

    // =========================
    // 4️⃣ GETTERS POUR DASHBOARD (INCHANGÉS)
    // =========================
    public static boolean isModelReady() {
        return modelTrained;
    }

    public static boolean isModelTrained() {
        return modelTrained;
    }

    public static double getLastAccuracy() {
        return lastAccuracy;
    }

    public static double getLastPrecision() {
        return lastPrecision;
    }

    public static double getLastRecall() {
        return lastRecall;
    }

    public static String getLastConfusionMatrix() {
        return lastConfusionMatrix;
    }

    public static double getLastF1() {
        return lastF1;
    }

    // =========================
    // 5️⃣ ANALYSE DATASET (INCHANGÉE)
    // =========================
    public static void analyzeDataset() {
        System.out.println("=== ANALYSE DATASET CSV ===");
        System.out.println("Instances : " + trainingHeader.numInstances());
        System.out.println("Attributs : " + trainingHeader.numAttributes());
        System.out.println("Classes   : " + trainingHeader.classAttribute().numValues());
    }

    // =========================
    // ✅ NOUVELLES CLASSES
    // =========================

    public static class DataPoint {
        public final double x;
        public final double y;
        public final boolean isAnomaly;

        public DataPoint(double x, double y, boolean isAnomaly) {
            this.x = x;
            this.y = y;
            this.isAnomaly = isAnomaly;
        }
    }

    public static class ZoneStats {
        public final String zoneName;
        public final int normalCount;
        public final int anomalyCount;

        public ZoneStats(String zoneName, int normalCount, int anomalyCount) {
            this.zoneName = zoneName;
            this.normalCount = normalCount;
            this.anomalyCount = anomalyCount;
        }

        public int getTotal() {
            return normalCount + anomalyCount;
        }

        public double getAnomalyPercentage() {
            if (getTotal() == 0) return 0;
            return (anomalyCount * 100.0) / getTotal();
        }
    }

    // =========================
    // 6️⃣ CLASSE RÉSULTAT (INCHANGÉE)
    // =========================
    public static class PredictionResult {
        public final String prediction;
        public final double anomalyProbability;
        public final double normalProbability;
        public final String status;
        public final Date timestamp = new Date();

        public PredictionResult(String p, double a, double n, String s) {
            prediction = p;
            anomalyProbability = a;
            normalProbability = n;
            status = s;
        }

        public String getPrediction() { return prediction; }
        public double getAnomalyProbability() { return anomalyProbability; }
        public String toDetailedString() {
            return String.format(
                    "📌 Résultat de la prédiction\n" +
                            "→ Statut           : %s\n" +
                            "→ Probabilité anomalie : %.2f %%\n" +
                            "→ Probabilité normale  : %.2f %%\n" +
                            "→ Horodatage       : %s\n",
                    prediction,
                    anomalyProbability * 100,
                    normalProbability * 100,
                    timestamp.toString()
            );
        }
    }
}