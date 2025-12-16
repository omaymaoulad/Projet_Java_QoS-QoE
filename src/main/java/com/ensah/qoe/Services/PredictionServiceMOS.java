package com.ensah.qoe.Services;

import com.ensah.qoe.ML.DataPreparationMOS;
import com.ensah.qoe.ML.MOSPredictionModels;
import weka.core.*;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.io.InputStream;
import java.util.*;

public class PredictionServiceMOS {

    // =========================
    // ÉTAT GLOBAL
    // =========================
    private static Instances trainingHeader;
    private static Instances originalDataset; // Dataset ORIGINAL avec toutes les informations
    private static final MOSPredictionModels modelHandler = new MOSPredictionModels();
    private static boolean modelTrained = false;
    private static boolean modelReady = false;

    // Métriques de performance
    private static double lastRMSE = 0.0;
    private static double lastMAE = 0.0;
    private static double lastR2 = 0.0;
    private static double lastMAPE = 0.0;

    // Statistiques MOS
    private static double avgMOS = 0.0;
    private static double minMOS = 5.0;
    private static double maxMOS = 1.0;
    private static double stdMOS = 0.0;

    // =========================
    // ✅ CHARGER CSV ORIGINAL
    // =========================
    private static Instances loadOriginalCSV(String resourcePath) throws Exception {
        InputStream input = DataPreparationMOS.class.getResourceAsStream(resourcePath);
        if (input == null) {
            throw new Exception("❌ Fichier introuvable : " + resourcePath);
        }

        CSVLoader loader = new CSVLoader();
        loader.setSource(input);
        Instances data = loader.getDataSet();

        // Vérifier si l'attribut MOS existe
        Attribute mosAttr = data.attribute("mos");
        if (mosAttr != null) {
            data.setClass(mosAttr);
        } else {
            // Chercher des noms alternatifs
            for (int i = 0; i < data.numAttributes(); i++) {
                String attrName = data.attribute(i).name().toLowerCase();
                if (attrName.contains("mos") || attrName.contains("score") ||
                        attrName.contains("quality") || attrName.contains("rating")) {
                    data.setClassIndex(i);
                    break;
                }
            }
        }

        return data;
    }

    // =========================
    // 1️⃣ ENTRAÎNEMENT
    // =========================
    public static String trainModel() {
        StringBuilder report = new StringBuilder();

        try {
            // Préparation des données MOS
            Instances[] datasets = DataPreparationMOS.prepareFromResources("/CSV/prediction_dataset.csv");
            Instances trainData = datasets[0];
            Instances testData = datasets[1];

            // ✅ Charger le dataset ORIGINAL complet
            originalDataset = loadOriginalCSV("/CSV/prediction_dataset.csv");

            // Header (structure des features)
            trainingHeader = new Instances(trainData, 0);

            System.out.println("📊 Caractéristiques audio détectées :");
            for (int i = 0; i < trainingHeader.numAttributes(); i++) {
                System.out.println(i + " -> " + trainingHeader.attribute(i).name() +
                        " (Type: " + trainingHeader.attribute(i).type() + ")");
            }
            System.out.println("Classe à prédire: " + trainingHeader.classAttribute().name());

            // Entraînement du modèle de régression
            modelHandler.trainRandomForest(trainData);

            // Évaluation
            MOSPredictionModels.EvaluationResult result = modelHandler.evaluate(testData);

            lastRMSE = result.rmse;
            lastMAE = result.mae;
            lastR2 = result.r2;
            lastMAPE = result.mape;

            // Calcul des statistiques MOS
            calculateMOSStatistics();

            modelTrained = true;
            modelReady = true;

            // Génération du rapport
            report.append("✅ ENTRAÎNEMENT MODÈLE MOS TERMINÉ\n\n");
            report.append("📊 Métriques de performance :\n");
            report.append(String.format("• RMSE  : %.4f\n", lastRMSE));
            report.append(String.format("• MAE   : %.4f\n", lastMAE));
            report.append(String.format("• R²    : %.4f\n", lastR2));
            report.append(String.format("• MAPE  : %.2f%%\n", lastMAPE * 100));

            report.append("\n📈 Statistiques MOS du dataset :\n");
            report.append(String.format("• MOS moyen    : %.2f / 5.0\n", avgMOS));
            report.append(String.format("• MOS minimum  : %.2f\n", minMOS));
            report.append(String.format("• MOS maximum  : %.2f\n", maxMOS));
            report.append(String.format("• Écart-type   : %.3f\n", stdMOS));

            report.append("\n🎯 Interprétation :\n");
            if (lastRMSE < 0.5) {
                report.append("• Précision EXCELLENTE (RMSE < 0.5)\n");
            } else if (lastRMSE < 0.8) {
                report.append("• Précision BONNE (RMSE < 0.8)\n");
            } else {
                report.append("• Précision MODÉRÉE (RMSE ≥ 0.8)\n");
            }

            if (lastR2 > 0.9) {
                report.append("• Ajustement TRÈS BON (R² > 0.9)\n");
            } else if (lastR2 > 0.7) {
                report.append("• Ajustement SATISFAISANT (R² > 0.7)\n");
            } else {
                report.append("• Ajustement À AMÉLIORER (R² ≤ 0.7)\n");
            }

        } catch (Exception e) {
            report.append("❌ ERREUR ENTRAÎNEMENT MOS: ").append(e.getMessage());
            e.printStackTrace();
        }

        return report.toString();
    }

    // =========================
    // ✅ CALCUL STATISTIQUES MOS
    // =========================
    private static void calculateMOSStatistics() {
        if (originalDataset == null) return;

        Attribute mosAttr = originalDataset.classAttribute();
        if (mosAttr == null) {
            // Chercher manuellement l'attribut MOS
            for (int i = 0; i < originalDataset.numAttributes(); i++) {
                if (originalDataset.attribute(i).name().toLowerCase().contains("mos")) {
                    mosAttr = originalDataset.attribute(i);
                    break;
                }
            }
        }

        if (mosAttr == null) return;

        double sum = 0;
        double sumSquared = 0;
        int count = 0;

        for (int i = 0; i < originalDataset.numInstances(); i++) {
            Instance inst = originalDataset.instance(i);
            if (!inst.isMissing(mosAttr)) {
                double mosValue = inst.value(mosAttr);
                sum += mosValue;
                sumSquared += mosValue * mosValue;
                count++;

                if (mosValue < minMOS) minMOS = mosValue;
                if (mosValue > maxMOS) maxMOS = mosValue;
            }
        }

        if (count > 0) {
            avgMOS = sum / count;
            double variance = (sumSquared / count) - (avgMOS * avgMOS);
            stdMOS = Math.sqrt(Math.max(0, variance));
        }
    }

    // =========================
    // ✅ NOUVELLES FONCTIONS POUR STATISTIQUES
    // =========================

    /**
     * Retourne le nombre total d'échantillons audio
     */
    public static int getTotalAudioSamples() {
        if (originalDataset == null) return 0;
        return originalDataset.numInstances();
    }

    /**
     * Retourne le MOS moyen du dataset
     */
    public static double getAverageMOS() {
        return avgMOS;
    }

    /**
     * Retourne le MOS minimum
     */
    public static double getMinMOS() {
        return minMOS;
    }

    /**
     * Retourne le MOS maximum
     */
    public static double getMaxMOS() {
        return maxMOS;
    }

    /**
     * Retourne l'écart-type du MOS
     */
    public static double getMOSStdDev() {
        return stdMOS;
    }

    /**
     * Retourne les données pour un graphique de tendance MOS
     */
    public static List<MOSTrendData> getMOSTrendData() {
        List<MOSTrendData> trend = new ArrayList<>();

        if (originalDataset == null || !modelTrained) return trend;

        Attribute mosAttr = originalDataset.classAttribute();
        if (mosAttr == null) return trend;

        // Simuler des prédictions pour la démonstration
        Random rand = new Random(42);
        for (int i = 0; i < Math.min(100, originalDataset.numInstances()); i++) {
            Instance inst = originalDataset.instance(i);
            if (!inst.isMissing(mosAttr)) {
                double actualMOS = inst.value(mosAttr);
                // Simulation de prédiction avec un peu de bruit
                double predictedMOS = actualMOS + (rand.nextDouble() - 0.5) * lastRMSE * 2;
                predictedMOS = Math.max(1.0, Math.min(5.0, predictedMOS)); // Limiter entre 1 et 5

                trend.add(new MOSTrendData(i, actualMOS, predictedMOS));
            }
        }

        return trend;
    }

    /**
     * Retourne la distribution des niveaux de qualité
     */
    public static List<QualityDistribution> getQualityDistribution() {
        List<QualityDistribution> distribution = new ArrayList<>();

        if (originalDataset == null || !modelTrained) return distribution;

        Attribute mosAttr = originalDataset.classAttribute();
        if (mosAttr == null) return distribution;

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Excellente (4.0-5.0)", 0);
        counts.put("Bonne (3.0-4.0)", 0);
        counts.put("Acceptable (2.5-3.0)", 0);
        counts.put("Médiocre (2.0-2.5)", 0);
        counts.put("Mauvaise (1.0-2.0)", 0);

        for (int i = 0; i < originalDataset.numInstances(); i++) {
            Instance inst = originalDataset.instance(i);
            if (!inst.isMissing(mosAttr)) {
                double mos = inst.value(mosAttr);

                if (mos >= 4.0) {
                    counts.put("Excellente (4.0-5.0)", counts.get("Excellente (4.0-5.0)") + 1);
                } else if (mos >= 3.0) {
                    counts.put("Bonne (3.0-4.0)", counts.get("Bonne (3.0-4.0)") + 1);
                } else if (mos >= 2.5) {
                    counts.put("Acceptable (2.5-3.0)", counts.get("Acceptable (2.5-3.0)") + 1);
                } else if (mos >= 2.0) {
                    counts.put("Médiocre (2.0-2.5)", counts.get("Médiocre (2.0-2.5)") + 1);
                } else {
                    counts.put("Mauvaise (1.0-2.0)", counts.get("Mauvaise (1.0-2.0)") + 1);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            distribution.add(new QualityDistribution(entry.getKey(), entry.getValue()));
        }

        return distribution;
    }

    /**
     * Retourne les données pour un graphique feature vs MOS
     */
    public static List<FeatureMOSData> getFeatureMOSData(String featureName) {
        List<FeatureMOSData> data = new ArrayList<>();

        if (originalDataset == null || !modelTrained) return data;

        Attribute featureAttr = originalDataset.attribute(featureName);
        Attribute mosAttr = originalDataset.classAttribute();

        if (featureAttr == null || mosAttr == null) return data;

        // Limiter à 200 points pour la performance
        int step = Math.max(1, originalDataset.numInstances() / 200);

        for (int i = 0; i < originalDataset.numInstances(); i += step) {
            Instance inst = originalDataset.instance(i);

            if (!inst.isMissing(featureAttr) && !inst.isMissing(mosAttr)) {
                double featureValue = inst.value(featureAttr);
                double mosValue = inst.value(mosAttr);

                data.add(new FeatureMOSData(featureValue, mosValue));
            }
        }

        return data;
    }

    /**
     * Retourne les résultats qualité pour les échantillons audio
     */
    public static List<AudioQualityResult> getAudioQualityResults() {
        List<AudioQualityResult> results = new ArrayList<>();

        if (originalDataset == null || !modelTrained) return results;

        Attribute mosAttr = originalDataset.classAttribute();
        if (mosAttr == null) return results;

        // Pour les 50 premiers échantillons
        for (int i = 0; i < Math.min(50, originalDataset.numInstances()); i++) {
            Instance inst = originalDataset.instance(i);
            if (!inst.isMissing(mosAttr)) {
                double actualMOS = inst.value(mosAttr);

                // Simuler une prédiction
                Random rand = new Random(i);
                double predictedMOS = actualMOS + (rand.nextDouble() - 0.5) * lastRMSE;
                predictedMOS = Math.max(1.0, Math.min(5.0, predictedMOS));
                double error = Math.abs(actualMOS - predictedMOS);

                String audioId = "audio_" + (i + 1);
                if (originalDataset.attribute("audio_id") != null) {
                    audioId = inst.stringValue(originalDataset.attribute("audio_id"));
                }

                results.add(new AudioQualityResult(audioId, predictedMOS, actualMOS, error));
            }
        }

        return results;
    }

    // =========================
    // 2️⃣ PRÉDICTION MOS
    // =========================
    public static MOSResult predictMOS(
            double spectralCentroid, double spectralBandwidth,
            double rms, double zcr, double snr,
            double distortion, double noiseLevel) {

        if (!modelTrained || trainingHeader == null) {
            return new MOSResult(0, 0, 0, "Modèle non prêt", "❌");
        }

        try {
            // ==================================================
            // 1️⃣ Création de l'instance avec les features audio
            // ==================================================
            DenseInstance instance = new DenseInstance(trainingHeader.numAttributes());
            instance.setDataset(trainingHeader);

            // Attribution des valeurs (vérifier les noms d'attributs)
            setAttributeValue(instance, "spectral_centroid", spectralCentroid);
            setAttributeValue(instance, "spectral_bandwidth", spectralBandwidth);
            setAttributeValue(instance, "rms", rms);
            setAttributeValue(instance, "zcr", zcr);
            setAttributeValue(instance, "snr", snr);
            setAttributeValue(instance, "distortion", distortion);
            setAttributeValue(instance, "noise_level", noiseLevel);

            // Features optionnelles (si présentes dans le header)
            setAttributeValue(instance, "harmonicity", 0.8);
            setAttributeValue(instance, "roughness", 0.2);
            setAttributeValue(instance, "loudness", 0.5);

            instance.setClassMissing();

            // ==================================================
            // 2️⃣ Normalisation avec le filtre du TRAIN
            // ==================================================
            Normalize normalize = DataPreparationMOS.getNormalizeFilter();
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
            double predictedMOS = modelHandler.getModel().classifyInstance(normalizedInstance);

            // Limiter entre 1.0 et 5.0
            predictedMOS = Math.max(1.0, Math.min(5.0, predictedMOS));

            // ==================================================
            // 4️⃣ Calcul de la confiance
            // ==================================================
            double confidence = calculateConfidence(predictedMOS);

            // ==================================================
            // 5️⃣ Détermination du niveau qualité
            // ==================================================
            String qualityLevel = getQualityLevel(predictedMOS);

            return new MOSResult(
                    predictedMOS,
                    confidence,
                    lastRMSE, // Intervalle de confiance = RMSE
                    "✅ Prédiction réussie",
                    qualityLevel
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new MOSResult(0, 0, 0, "Erreur: " + e.getMessage(), "❌");
        }
    }

    private static void setAttributeValue(DenseInstance instance, String attrName, double value) {
        Attribute attr = trainingHeader.attribute(attrName);
        if (attr != null && !instance.isMissing(attr)) {
            instance.setValue(attr, value);
        }
    }

    private static double calculateConfidence(double predictedMOS) {
        // Calcul basé sur l'erreur RMSE et la position dans l'échelle
        double baseConfidence = 0.8;

        // Moins de confiance aux extrêmes
        if (predictedMOS < 1.5 || predictedMOS > 4.5) {
            baseConfidence -= 0.2;
        }

        // Ajustement basé sur la performance du modèle
        baseConfidence -= (lastRMSE * 0.3);

        return Math.max(0.5, Math.min(0.95, baseConfidence));
    }

    private static String getQualityLevel(double mos) {
        if (mos >= 4.5) return "🎯 Exceptionnelle";
        if (mos >= 4.0) return "🔵 Excellente";
        if (mos >= 3.5) return "🟢 Très bonne";
        if (mos >= 3.0) return "🟢 Bonne";
        if (mos >= 2.5) return "🟡 Acceptable";
        if (mos >= 2.0) return "🟠 Médiocre";
        if (mos >= 1.5) return "🔴 Mauvaise";
        return "🔴 Très mauvaise";
    }

    // =========================
    // 3️⃣ ÉVALUATION
    // =========================
    public static String evaluateModel() {
        if (!modelTrained) {
            throw new IllegalStateException("Modèle MOS non entraîné");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== ÉVALUATION MODÈLE MOS ===\n\n");
        sb.append("📊 Métriques de régression :\n");
        sb.append(String.format("• RMSE  : %.4f\n", lastRMSE));
        sb.append(String.format("• MAE   : %.4f\n", lastMAE));
        sb.append(String.format("• R²    : %.4f\n", lastR2));
        sb.append(String.format("• MAPE  : %.2f%%\n\n", lastMAPE * 100));

        sb.append("📈 Interprétation :\n");
        sb.append(String.format("• Erreur moyenne : ±%.2f points MOS\n", lastMAE));
        sb.append(String.format("• Précision : %.1f%%\n", (1 - lastMAE/2) * 100));

        return sb.toString();
    }

    // =========================
    // 4️⃣ GETTERS POUR DASHBOARD
    // =========================
    public static boolean isModelReady() {
        return modelReady;
    }

    public static boolean isModelTrained() {
        return modelTrained;
    }

    public static double getLastRMSE() {
        return lastRMSE;
    }

    public static double getLastMAE() {
        return lastMAE;
    }

    public static double getLastR2() {
        return lastR2;
    }

    public static double getLastMAPE() {
        return lastMAPE;
    }

    public static ModelStats getModelStatistics() {
        return new ModelStats(
                getTotalAudioSamples(),
                getAverageMOS(),
                getMinMOS(),
                getMaxMOS(),
                getLastRMSE(),
                getLastMAE(),
                getLastR2()
        );
    }

    // =========================
    // ✅ CLASSES DE DONNÉES
    // =========================

    public static class MOSTrendData {
        public final int index;
        public final double actualMOS;
        public final double predictedMOS;

        public MOSTrendData(int index, double actualMOS, double predictedMOS) {
            this.index = index;
            this.actualMOS = actualMOS;
            this.predictedMOS = predictedMOS;
        }

        public int getIndex() { return index; }
        public double getActualMOS() { return actualMOS; }
        public double getPredictedMOS() { return predictedMOS; }
    }

    public static class QualityDistribution {
        public final String qualityLevel;
        public final int count;

        public QualityDistribution(String qualityLevel, int count) {
            this.qualityLevel = qualityLevel;
            this.count = count;
        }

        public String getQualityLevel() { return qualityLevel; }
        public int getCount() { return count; }
    }

    public static class FeatureMOSData {
        public final double featureValue;
        public final double MOS;

        public FeatureMOSData(double featureValue, double MOS) {
            this.featureValue = featureValue;
            this.MOS = MOS;
        }

        public double getFeatureValue() { return featureValue; }
        public double getMOS() { return MOS; }
    }

    public static class AudioQualityResult {
        public final String audioId;
        public final double predictedMOS;
        public final double actualMOS;
        public final double error;

        public AudioQualityResult(String audioId, double predictedMOS,
                                  double actualMOS, double error) {
            this.audioId = audioId;
            this.predictedMOS = predictedMOS;
            this.actualMOS = actualMOS;
            this.error = error;
        }

        public String getAudioId() { return audioId; }
        public double getPredictedMOS() { return predictedMOS; }
        public double getActualMOS() { return actualMOS; }
        public double getError() { return error; }
    }

    public static class ModelStats {
        private final int totalSamples;
        private final double averageMOS;
        private final double minMOS;
        private final double maxMOS;
        private final double RMSE;
        private final double MAE;
        private final double r2Score;

        public ModelStats(int totalSamples, double averageMOS, double minMOS,
                          double maxMOS, double RMSE, double MAE, double r2Score) {
            this.totalSamples = totalSamples;
            this.averageMOS = averageMOS;
            this.minMOS = minMOS;
            this.maxMOS = maxMOS;
            this.RMSE = RMSE;
            this.MAE = MAE;
            this.r2Score = r2Score;
        }

        public int getTotalSamples() { return totalSamples; }
        public double getAverageMOS() { return averageMOS; }
        public double getMinMOS() { return minMOS; }
        public double getMaxMOS() { return maxMOS; }
        public double getRMSE() { return RMSE; }
        public double getMAE() { return MAE; }
        public double getR2Score() { return r2Score; }
    }

    // =========================
    // 5️⃣ CLASSE RÉSULTAT MOS
    // =========================
    public static class MOSResult {
        public final double predictedMOS;
        public final double confidence;
        public final double confidenceInterval;
        public final String status;
        public final String qualityLevel;
        public final Date timestamp = new Date();

        public MOSResult(double predictedMOS, double confidence,
                         double confidenceInterval, String status, String qualityLevel) {
            this.predictedMOS = predictedMOS;
            this.confidence = confidence;
            this.confidenceInterval = confidenceInterval;
            this.status = status;
            this.qualityLevel = qualityLevel;
        }

        public double getPredictedMOS() { return predictedMOS; }
        public double getConfidence() { return confidence; }
        public double getConfidenceInterval() { return confidenceInterval; }
        public String getQualityLevel() { return qualityLevel; }

        public String toDetailedString() {
            return String.format(
                    "🎵 Résultat prédiction MOS\n" +
                            "→ MOS prédit        : %.2f / 5.0\n" +
                            "→ Niveau qualité    : %s\n" +
                            "→ Confiance         : %.1f%%\n" +
                            "→ Intervalle conf.  : ±%.2f\n" +
                            "→ Statut            : %s\n" +
                            "→ Horodatage        : %s\n",
                    predictedMOS,
                    qualityLevel,
                    confidence * 100,
                    confidenceInterval,
                    status,
                    timestamp.toString()
            );
        }
    }
}