package com.ensah.qoe.Services;

import com.ensah.qoe.ML.AnomalyDetectionModels;
import com.ensah.qoe.ML.DataPreparationAnomalie;
import com.ensah.qoe.ML.MLConfig;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class PredictionServiceAnomalies {

    // ============================================================
    // ÉTATS ET CONFIGURATION
    // ============================================================
    private static AnomalyDetectionModels modelHandler;
    private static Filter normalizeFilter;
    private static Instances trainingHeader;

    private static boolean modelTrained = false;
    private static boolean modelLoaded = false;

    // Configuration
    private static String selectedAlgorithm = "RandomForest";
    private static double predictionThreshold = 0.5;

    // Statistiques de performance
    private static double lastAccuracy = 0.0;
    private static double lastPrecision = 0.0;
    private static double lastRecall = 0.0;
    private static String lastConfusionMatrix = "";

    // Historique des prédictions
    private static List<PredictionRecord> predictionHistory = new ArrayList<>();

    // ============================================================
    // CLASSES DÉFINITIONS INTERNES
    // ============================================================

    /**
     * Enregistre une prédiction pour l'historique
     */
    public static class PredictionRecord {
        private Date timestamp;
        private double latency;
        private double jitter;
        private double lossRate;
        private double bandwidth;
        private double signalScore;
        private String prediction;
        private double anomalyProbability;
        private double normalProbability;

        public PredictionRecord(double lat, double jit, double loss, double bw,
                                double signal, String pred, double anomalyProb, double normalProb) {
            this.timestamp = new Date();
            this.latency = lat;
            this.jitter = jit;
            this.lossRate = loss;
            this.bandwidth = bw;
            this.signalScore = signal;
            this.prediction = pred;
            this.anomalyProbability = anomalyProb;
            this.normalProbability = normalProb;
        }

        // Getters
        public Date getTimestamp() { return timestamp; }
        public double getLatency() { return latency; }
        public double getJitter() { return jitter; }
        public double getLossRate() { return lossRate; }
        public double getBandwidth() { return bandwidth; }
        public double getSignalScore() { return signalScore; }
        public String getPrediction() { return prediction; }
        public double getAnomalyProbability() { return anomalyProbability; }
        public double getNormalProbability() { return normalProbability; }

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return String.format("%s | Lat:%.1fms | Jit:%.1fms | Loss:%.3f | BW:%.1f | Sig:%.1f | Pred:%s (Anom:%.1f%%)",
                    sdf.format(timestamp), latency, jitter, lossRate, bandwidth, signalScore,
                    prediction, anomalyProbability * 100);
        }
    }

    // ============================================================
    // 1. INITIALISATION DU SERVICE
    // ============================================================

    static {
        System.out.println("⚡ Initialisation de PredictionServiceAnomalies...");
        modelHandler = new AnomalyDetectionModels();

        // Créer les dossiers nécessaires
        createDirectories();

        // Essayer de charger un modèle existant
        try {
            loadLatestModel();
        } catch (Exception e) {
            System.out.println("ℹ Aucun modèle existant trouvé, un nouvel entraînement sera nécessaire.");
        }
    }

    private static void createDirectories() {
        String[] dirs = {"models", "results", "logs", "predictions"};
        for (String dir : dirs) {
            File directory = new File(dir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("📁 Dossier créé: " + dir);
                }
            }
        }
    }

    // ============================================================
    // 2. ENTRAÎNEMENT DU MODÈLE (COMPLET)
    // ============================================================

    public static String trainModel() {
        return trainModel(false);
    }

    public static String trainModel(boolean compareAll) {
        StringBuilder report = new StringBuilder();

        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🤖 ENTRAÎNEMENT DU MODÈLE DE DÉTECTION D'ANOMALIES");
            System.out.println("=".repeat(60));

            report.append("=== ENTRAÎNEMENT DU MODÈLE ===\n");
            report.append("Algorithme sélectionné: ").append(selectedAlgorithm).append("\n");
            report.append("Heure: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");

            // 1. Préparation des données
            System.out.println("📊 ÉTAPE 1: Préparation des données...");
            report.append("📊 PRÉPARATION DES DONNÉES\n");

            Instances[] datasets = DataPreparationAnomalie.prepareFromResources("/CSV/prediction_dataset.csv");

            if (datasets == null || datasets.length < 2) {
                String error = "❌ Impossible de charger les données d'entraînement";
                System.err.println(error);
                report.append(error).append("\n");
                return report.toString();
            }

            Instances trainData = datasets[0];
            Instances testData = datasets[1];

            report.append("  - Dataset d'entraînement: ").append(trainData.numInstances()).append(" instances\n");
            report.append("  - Dataset de test: ").append(testData.numInstances()).append(" instances\n");
            report.append("  - Nombre d'attributs: ").append(trainData.numAttributes()).append("\n");
            report.append("  - Classe: ").append(trainData.classAttribute().name()).append("\n\n");

            System.out.println("✅ Données chargées: " + trainData.numInstances() + " train, " +
                    testData.numInstances() + " test");

            // 2. Préparation des filtres
            System.out.println("🔧 ÉTAPE 2: Préparation des filtres...");
            prepareNormalizationFilter(trainData);

            // 3. Entraînement du modèle
            System.out.println("🎯 ÉTAPE 3: Entraînement du modèle...");

            if (compareAll) {
                report.append("🔄 COMPARAISON DE TOUS LES ALGORITHMES\n");
                var results = modelHandler.trainAndCompareAll(trainData, testData);

                report.append("Résultats de la comparaison:\n");
                results.forEach((algo, acc) -> {
                    report.append(String.format("  - %-15s: %.2f%%\n", algo, acc));
                });

                report.append("\n🏆 Meilleur algorithme: ").append(AnomalyDetectionModels.GlobalStats.bestAlgorithm);
                report.append(" (Accuracy: ").append(String.format("%.2f%%", AnomalyDetectionModels.GlobalStats.bestAccuracy)).append(")\n\n");

            } else {
                report.append("🎯 ENTRAÎNEMENT AVEC ").append(selectedAlgorithm).append("\n");
                trainSelectedAlgorithm(trainData);
            }

            // 4. Évaluation
            System.out.println("📈 ÉTAPE 4: Évaluation du modèle...");
            AnomalyDetectionModels.EvaluationResult evalResult = modelHandler.evaluate(testData);

            // Sauvegarder les métriques
            lastAccuracy = evalResult.accuracy;
            lastPrecision = evalResult.precision;
            lastRecall = evalResult.recall;
            lastConfusionMatrix = evalResult.confusionMatrix;

            report.append("📈 PERFORMANCE DU MODÈLE\n");
            report.append(String.format("  Accuracy:  %.2f%%\n", evalResult.accuracy));
            report.append(String.format("  Precision: %.4f\n", evalResult.precision));
            report.append(String.format("  Recall:    %.4f\n", evalResult.recall));
            report.append(String.format("  F1-Score:  %.4f\n", evalResult.f1Score));
            report.append(String.format("  AUC:       %.4f\n", evalResult.auc));
            report.append("\n🎯 MATRICE DE CONFUSION:\n").append(evalResult.confusionMatrix).append("\n");

            // 5. Validation croisée
            System.out.println("🔄 ÉTAPE 5: Validation croisée...");
            modelHandler.crossValidate(trainData, MLConfig.CROSS_VALIDATION_FOLDS);

            report.append("\n🔄 VALIDATION CROISÉE (").append(MLConfig.CROSS_VALIDATION_FOLDS).append("-fold)\n");
            report.append("  Effectuée avec succès\n");

            // 6. Sauvegarde
            System.out.println("💾 ÉTAPE 6: Sauvegarde...");
            saveTrainedModel();

            report.append("\n💾 SAUVEGARDE\n");
            report.append("  Modèle sauvegardé dans: ").append(modelHandler.getModelPath()).append("\n");

            // 7. Mise à jour du statut
            modelTrained = true;
            modelLoaded = true;

            System.out.println("✅ Entraînement terminé avec succès !");
            report.append("\n✅ ENTRAÎNEMENT TERMINÉ AVEC SUCCÈS\n");

            // 8. Export du rapport
            exportTrainingReport(report.toString());

        } catch (Exception e) {
            String error = "❌ Erreur pendant l'entraînement: " + e.getMessage();
            System.err.println(error);
            report.append("\n❌ ERREUR: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }

        return report.toString();
    }

    private static void trainSelectedAlgorithm(Instances trainData) throws Exception {
        switch (selectedAlgorithm.toUpperCase()) {
            case "RANDOMFOREST":
                modelHandler.trainRandomForest(trainData);
                break;
            case "J48":
                modelHandler.trainJ48(trainData);
                break;
            case "NAIVEBAYES":
                modelHandler.trainNaiveBayes(trainData);
                break;
            case "KNN":
                modelHandler.trainKNN(trainData);
                break;
            case "SVM":
                modelHandler.trainSVM(trainData);
                break;
            case "MLP":
                modelHandler.trainMLP(trainData);
                break;
            default:
                System.out.println("⚠ Algorithme non reconnu, utilisation de RandomForest par défaut");
                selectedAlgorithm = "RandomForest";
                modelHandler.trainRandomForest(trainData);
        }
    }

    private static void prepareNormalizationFilter(Instances data) throws Exception {
        normalizeFilter = new Normalize();

        // 1. Définir l'index de l'attribut classe dans le dataset
        // (supposons que la classe est le dernier attribut)
        data.setClassIndex(data.numAttributes() - 1);

        // 2. Spécifier la plage d'attributs à normaliser (exclure la classe)
        String range = "first-" + (data.numAttributes() - 2); // tout sauf le dernier
        String[] options = {"-S", "1.0", "-T", "0.0", "-R", range};

        normalizeFilter.setOptions(options);
        normalizeFilter.setInputFormat(data);

        // Appliquer pour créer l'en-tête normalisé
        Filter.useFilter(data, normalizeFilter);
    }

    // ============================================================
    // 3. SAUVEGARDE ET CHARGEMENT
    // ============================================================

    public static boolean saveTrainedModel() {
        try {
            if (modelHandler == null) {
                throw new Exception("Aucun modèle à sauvegarder");
            }

            // Sauvegarder le modèle
            modelHandler.saveModel();

            // Sauvegarder le filtre de normalisation
            String filterPath = "models/normalize_filter_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".filter";
            SerializationHelper.write(filterPath, normalizeFilter);

            // Sauvegarder l'en-tête d'entraînement
            String headerPath = "models/training_header_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".header";
            SerializationHelper.write(headerPath, trainingHeader);

            System.out.println("✅ Modèle complet sauvegardé");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde modèle: " + e.getMessage());
            return false;
        }
    }

    public static boolean loadLatestModel() {
        try {
            modelHandler.loadLatestModel();

            // Charger les composants associés
            File modelDir = new File("models");
            File[] filterFiles = modelDir.listFiles((dir, name) -> name.endsWith(".filter"));
            File[] headerFiles = modelDir.listFiles((dir, name) -> name.endsWith(".header"));

            if (filterFiles != null && filterFiles.length > 0) {
                normalizeFilter = (Filter) SerializationHelper.read(filterFiles[0].getPath());
            }

            if (headerFiles != null && headerFiles.length > 0) {
                trainingHeader = (Instances) SerializationHelper.read(headerFiles[0].getPath());
            }

            modelLoaded = true;
            modelTrained = true;

            System.out.println("✅ Dernier modèle chargé avec succès");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement modèle: " + e.getMessage());
            modelLoaded = false;
            return false;
        }
    }

    public static boolean loadModel(String modelPath) {
        try {
            modelHandler.loadModel(modelPath);
            modelLoaded = true;
            modelTrained = true;
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement modèle spécifique: " + e.getMessage());
            return false;
        }
    }

    // ============================================================
    // 4. PRÉDICTIONS
    // ============================================================

    public static PredictionResult predictAnomaly(double latency, double jitter, double lossRate,
                                                  double bandwidth, double signalScore) {

        return predictAnomaly(latency, jitter, lossRate, bandwidth, signalScore, 3.0, null);
    }

    public static PredictionResult predictAnomaly(double latency, double jitter, double lossRate,
                                                  double bandwidth, double signalScore,
                                                  double mos, String zone) {

        // Vérifier si un modèle est disponible
        if (!modelLoaded && !modelTrained) {
            return new PredictionResult("NORMAL", 0.0, 1.0,
                    "⚠ Aucun modèle disponible, retour à la valeur par défaut");
        }

        try {
            // Créer une nouvelle instance
            DenseInstance instance = new DenseInstance(trainingHeader.numAttributes());
            instance.setDataset(trainingHeader);

            // Remplir les valeurs (respecter l'ordre des attributs)
            instance.setValue(trainingHeader.attribute("latence"), latency);
            instance.setValue(trainingHeader.attribute("jitter"), jitter);
            instance.setValue(trainingHeader.attribute("loss_rate"), lossRate);
            instance.setValue(trainingHeader.attribute("bande_passante"), bandwidth);
            instance.setValue(trainingHeader.attribute("signal_score"), signalScore);

            // Ajouter MOS si présent dans l'en-tête
            Attribute mosAttr = trainingHeader.attribute("mos");
            if (mosAttr != null) {
                instance.setValue(mosAttr, mos);
            }

            // Ajouter zone si présent
            Attribute zoneAttr = trainingHeader.attribute("zone");
            if (zoneAttr != null && zone != null) {
                instance.setValue(zoneAttr, zone);
            }

            // Marquer la classe comme manquante (pour la prédiction)
            instance.setClassMissing();

            // Préparer le dataset pour la normalisation
            Instances tempDataset = new Instances(trainingHeader, 0);
            tempDataset.add(instance);

            // Appliquer la normalisation
            Instances normalized = Filter.useFilter(tempDataset, normalizeFilter);
            Instance normalizedInstance = normalized.firstInstance();

            // Obtenir la distribution de probabilité
            double[] distribution = modelHandler.getModel().distributionForInstance(normalizedInstance);

            double normalProbability = distribution[0];
            double anomalyProbability = distribution[1];

            // Déterminer la prédiction basée sur le seuil
            String prediction;
            String confidenceLevel;

            if (anomalyProbability > predictionThreshold) {
                prediction = "ANOMALIE";
                confidenceLevel = getConfidenceLevel(anomalyProbability);
            } else {
                prediction = "NORMAL";
                confidenceLevel = getConfidenceLevel(normalProbability);
            }

            // Analyser les facteurs contributifs
            String contributingFactors = analyzeContributingFactors(latency, jitter, lossRate,
                    bandwidth, signalScore);

            // Créer le résultat
            PredictionResult result = new PredictionResult(
                    prediction,
                    anomalyProbability,
                    normalProbability,
                    "✅ Prédiction réussie"
            );

            result.setConfidenceLevel(confidenceLevel);
            result.setContributingFactors(contributingFactors);
            result.setRawValues(new double[]{latency, jitter, lossRate, bandwidth, signalScore});

            // Ajouter à l'historique
            PredictionRecord record = new PredictionRecord(
                    latency, jitter, lossRate, bandwidth, signalScore,
                    prediction, anomalyProbability, normalProbability
            );
            predictionHistory.add(record);

            // Limiter la taille de l'historique
            if (predictionHistory.size() > 1000) {
                predictionHistory.remove(0);
            }

            return result;

        } catch (Exception e) {
            System.err.println("❌ Erreur prédiction: " + e.getMessage());
            return new PredictionResult("ERREUR", 0.0, 0.0,
                    "❌ Erreur lors de la prédiction: " + e.getMessage());
        }
    }

    public static class PredictionResult {
        private String prediction;
        private double anomalyProbability;
        private double normalProbability;
        private String status;
        private String confidenceLevel;
        private String contributingFactors;
        private double[] rawValues;
        private Date timestamp;

        public PredictionResult(String prediction, double anomalyProb, double normalProb, String status) {
            this.prediction = prediction;
            this.anomalyProbability = anomalyProb;
            this.normalProbability = normalProb;
            this.status = status;
            this.timestamp = new Date();
        }

        // Getters et Setters
        public String getPrediction() { return prediction; }
        public double getAnomalyProbability() { return anomalyProbability; }
        public double getNormalProbability() { return normalProbability; }
        public String getStatus() { return status; }
        public String getConfidenceLevel() { return confidenceLevel; }
        public String getContributingFactors() { return contributingFactors; }
        public double[] getRawValues() { return rawValues; }
        public Date getTimestamp() { return timestamp; }

        public void setConfidenceLevel(String level) { this.confidenceLevel = level; }
        public void setContributingFactors(String factors) { this.contributingFactors = factors; }
        public void setRawValues(double[] values) { this.rawValues = values; }

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            return String.format("[%s] %s (Anomalie: %.1f%%, Normal: %.1f%%) - %s",
                    sdf.format(timestamp), prediction, anomalyProbability * 100,
                    normalProbability * 100, confidenceLevel);
        }

        public String toDetailedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== RÉSULTAT DE PRÉDICTION ===\n");
            sb.append("Prédiction: ").append(prediction).append("\n");
            sb.append("Probabilité anomalie: ").append(String.format("%.1f%%", anomalyProbability * 100)).append("\n");
            sb.append("Probabilité normal: ").append(String.format("%.1f%%", normalProbability * 100)).append("\n");
            sb.append("Niveau de confiance: ").append(confidenceLevel).append("\n");
            sb.append("Statut: ").append(status).append("\n");

            if (contributingFactors != null) {
                sb.append("\nFacteurs contributifs:\n").append(contributingFactors).append("\n");
            }

            if (rawValues != null) {
                sb.append("\nValeurs d'entrée:\n");
                sb.append(String.format("  Latence: %.1f ms\n", rawValues[0]));
                sb.append(String.format("  Jitter: %.1f ms\n", rawValues[1]));
                sb.append(String.format("  Taux perte: %.3f\n", rawValues[2]));
                sb.append(String.format("  Bande passante: %.1f Mbps\n", rawValues[3]));
                sb.append(String.format("  Score signal: %.1f\n", rawValues[4]));
            }

            return sb.toString();
        }
    }

    private static String getConfidenceLevel(double probability) {
        if (probability >= 0.9) return "TRÈS ÉLEVÉE";
        if (probability >= 0.75) return "ÉLEVÉE";
        if (probability >= 0.6) return "MOYENNE";
        return "FAIBLE";
    }

    private static String analyzeContributingFactors(double lat, double jit, double loss,
                                                     double bw, double signal) {
        List<String> factors = new ArrayList<>();

        // Seuils pour détection d'anomalies (ajustables)
        if (lat > 100) factors.add("Latence élevée (>100ms)");
        if (jit > 30) factors.add("Jitter élevé (>30ms)");
        if (loss > 0.1) factors.add("Taux de perte élevé (>10%)");
        if (bw < 10) factors.add("Bande passante faible (<10Mbps)");
        if (signal < 50) factors.add("Signal faible (<50)");

        if (factors.isEmpty()) {
            return "Tous les paramètres sont dans les limites normales";
        }

        return String.join(", ", factors);
    }

    // ============================================================
    // 5. ÉVALUATION ET ANALYSE
    // ============================================================

    public static String evaluateModel() {
        try {
            if (!modelLoaded) {
                return "❌ Aucun modèle chargé pour évaluation";
            }

            // Charger les données de test
            Instances[] datasets = DataPreparationAnomalie.prepareFromResources("/CSV/prediction_dataset.csv");
            if (datasets == null || datasets.length < 2) {
                return "❌ Impossible de charger les données de test";
            }

            Instances testData = datasets[1];

            // Évaluer
            AnomalyDetectionModels.EvaluationResult result = modelHandler.evaluate(testData);

            // Mettre à jour les statistiques
            lastAccuracy = result.accuracy;
            lastPrecision = result.precision;
            lastRecall = result.recall;
            lastConfusionMatrix = result.confusionMatrix;

            // Générer rapport
            StringBuilder report = new StringBuilder();
            report.append("=== ÉVALUATION DU MODÈLE ===\n\n");
            report.append("Dataset de test: ").append(testData.numInstances()).append(" instances\n");
            report.append("Algorithme: ").append(modelHandler.getAlgorithmName()).append("\n\n");
            report.append("📊 MÉTRIQUES DE PERFORMANCE:\n");
            report.append(String.format("  Accuracy:  %.2f%%\n", result.accuracy));
            report.append(String.format("  Precision: %.4f\n", result.precision));
            report.append(String.format("  Recall:    %.4f\n", result.recall));
            report.append(String.format("  F1-Score:  %.4f\n", result.f1Score));
            report.append(String.format("  AUC:       %.4f\n", result.auc));

            report.append("\n🎯 INTERPRÉTATION:\n");
            if (result.accuracy >= MLConfig.GOOD_ACCURACY) {
                report.append("  ✓ Excellente précision\n");
            } else if (result.accuracy >= 70) {
                report.append("  ↳ Performance acceptable\n");
            } else {
                report.append("  ⚠ Performance à améliorer\n");
            }

            // Sauvegarder le rapport
            exportEvaluationReport(report.toString(), result);

            return report.toString();

        } catch (Exception e) {
            return "❌ Erreur lors de l'évaluation: " + e.getMessage();
        }
    }

    public static void analyzeDataset() {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 ANALYSE DU DATASET");
            System.out.println("=".repeat(60));

            // Charger les données
            Instances[] datasets = DataPreparationAnomalie.prepareFromResources("/CSV/prediction_dataset.csv");
            if (datasets == null) return;

            Instances data = new Instances(datasets[0]);
            for (int i = 0; i < datasets[1].numInstances(); i++) {
                data.add(datasets[1].instance(i));
            }

            // Statistiques de base
            int total = data.numInstances();
            int anomalies = 0;

            for (int i = 0; i < data.numInstances(); i++) {
                if ((int) data.instance(i).classValue() == 1) {
                    anomalies++;
                }
            }

            double anomalyRate = (anomalies * 100.0) / total;

            System.out.println("📈 STATISTIQUES:");
            System.out.println("  Total instances: " + total);
            System.out.println("  Anomalies: " + anomalies);
            System.out.println("  Taux d'anomalies: " + String.format("%.1f%%", anomalyRate));

            // Analyse MOS
            analyzeMOS(data);

            // Analyse des attributs
            analyzeAttributes(data);

        } catch (Exception e) {
            System.err.println("❌ Erreur analyse dataset: " + e.getMessage());
        }
    }

    private static void analyzeMOS(Instances data) {
        try {
            Attribute mosAttr = data.attribute("mos");
            if (mosAttr == null) return;

            int mosIndex = mosAttr.index();
            int classIndex = data.classIndex();

            double mosNormalSum = 0, mosAnomalySum = 0;
            int normalCount = 0, anomalyCount = 0;

            for (int i = 0; i < data.numInstances(); i++) {
                Instance inst = data.instance(i);
                double mos = inst.value(mosIndex);
                int anomaly = (int) inst.classValue();

                if (anomaly == 0) {
                    mosNormalSum += mos;
                    normalCount++;
                } else {
                    mosAnomalySum += mos;
                    anomalyCount++;
                }
            }

            double avgMosNormal = normalCount > 0 ? mosNormalSum / normalCount : 0;
            double avgMosAnomaly = anomalyCount > 0 ? mosAnomalySum / anomalyCount : 0;

            System.out.println("\n📊 CORRÉLATION MOS - ANOMALIES:");
            System.out.println("  MOS moyen (normal): " + String.format("%.2f", avgMosNormal));
            System.out.println("  MOS moyen (anomalie): " + String.format("%.2f", avgMosAnomaly));

            if (avgMosAnomaly < avgMosNormal) {
                System.out.println("  → Les anomalies sont associées à une baisse du MOS");
            } else {
                System.out.println("  → Pas de corrélation forte entre MOS et anomalies");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur analyse MOS: " + e.getMessage());
        }
    }

    private static void analyzeAttributes(Instances data) {
        System.out.println("\n📊 DISTRIBUTION DES ATTRIBUTS:");

        String[] attributes = {"latence", "jitter", "loss_rate", "bande_passante", "signal_score"};

        for (String attrName : attributes) {
            Attribute attr = data.attribute(attrName);
            if (attr != null) {
                int idx = attr.index();
                double min = Double.MAX_VALUE;
                double max = Double.MIN_VALUE;
                double sum = 0;

                for (int i = 0; i < data.numInstances(); i++) {
                    double val = data.instance(i).value(idx);
                    min = Math.min(min, val);
                    max = Math.max(max, val);
                    sum += val;
                }

                double avg = sum / data.numInstances();
                System.out.printf("  %-15s: Min=%.2f, Max=%.2f, Avg=%.2f\n",
                        attrName, min, max, avg);
            }
        }
    }

    // ============================================================
    // 6. UTILITAIRES ET EXPORT
    // ============================================================

    private static void exportTrainingReport(String report) {
        try {
            String filename = "results/training_report_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";

            FileWriter writer = new FileWriter(filename);
            writer.write(report);
            writer.close();

            System.out.println("📄 Rapport d'entraînement exporté: " + filename);
        } catch (Exception e) {
            System.err.println("❌ Erreur export rapport: " + e.getMessage());
        }
    }

    private static void exportEvaluationReport(String report, AnomalyDetectionModels.EvaluationResult result) {
        try {
            String filename = "results/evaluation_report_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";

            FileWriter writer = new FileWriter(filename);
            writer.write(report);
            writer.write("\n=== MATRICE DE CONFUSION ===\n");
            writer.write(result.confusionMatrix);
            writer.close();

            System.out.println("📄 Rapport d'évaluation exporté: " + filename);
        } catch (Exception e) {
            System.err.println("❌ Erreur export évaluation: " + e.getMessage());
        }
    }

    public static void exportPredictionHistory() {
        try {
            String filename = "predictions/history_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";

            FileWriter writer = new FileWriter(filename);
            writer.write("Timestamp,Latency,Jitter,LossRate,Bandwidth,SignalScore,Prediction,AnomalyProb,NormalProb\n");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (PredictionRecord record : predictionHistory) {
                writer.write(String.format("%s,%.2f,%.2f,%.4f,%.2f,%.2f,%s,%.4f,%.4f\n",
                        sdf.format(record.getTimestamp()),
                        record.getLatency(),
                        record.getJitter(),
                        record.getLossRate(),
                        record.getBandwidth(),
                        record.getSignalScore(),
                        record.getPrediction(),
                        record.getAnomalyProbability(),
                        record.getNormalProbability()
                ));
            }

            writer.close();
            System.out.println("📄 Historique des prédictions exporté: " + filename);

        } catch (Exception e) {
            System.err.println("❌ Erreur export historique: " + e.getMessage());
        }
    }

    // ============================================================
    // 7. GETTERS ET SETTERS
    // ============================================================

    public static boolean isModelTrained() {
        return modelTrained;
    }

    public static boolean isModelLoaded() {
        return modelLoaded;
    }

    public static String getSelectedAlgorithm() {
        return selectedAlgorithm;
    }

    public static void setSelectedAlgorithm(String algorithm) {
        selectedAlgorithm = algorithm;
        System.out.println("✅ Algorithme sélectionné: " + algorithm);
    }

    public static double getPredictionThreshold() {
        return predictionThreshold;
    }

    public static void setPredictionThreshold(double threshold) {
        if (threshold >= 0 && threshold <= 1) {
            predictionThreshold = threshold;
            System.out.println("✅ Seuil de prédiction ajusté: " + threshold);
        } else {
            System.err.println("❌ Seuil invalide, doit être entre 0 et 1");
        }
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

    public static List<PredictionRecord> getPredictionHistory() {
        return new ArrayList<>(predictionHistory);
    }

    public static void clearPredictionHistory() {
        predictionHistory.clear();
        System.out.println("✅ Historique des prédictions effacé");
    }

    public static int getPredictionHistorySize() {
        return predictionHistory.size();
    }

    public static AnomalyDetectionModels getModelHandler() {
        return modelHandler;
    }

    public static String getModelInfo() {
        if (modelHandler == null) {
            return "Aucun modèle chargé";
        }

        StringBuilder info = new StringBuilder();
        info.append("=== INFORMATIONS DU MODÈLE ===\n");
        info.append("Algorithme: ").append(modelHandler.getAlgorithmName()).append("\n");
        info.append("Chemin: ").append(modelHandler.getModelPath()).append("\n");
        info.append("Entraîné: ").append(modelTrained ? "Oui" : "Non").append("\n");
        info.append("Chargé: ").append(modelLoaded ? "Oui" : "Non").append("\n");

        if (lastAccuracy > 0) {
            info.append(String.format("Dernière accuracy: %.2f%%\n", lastAccuracy));
        }

        if (predictionHistory.size() > 0) {
            info.append("Prédictions effectuées: ").append(predictionHistory.size()).append("\n");
        }

        return info.toString();
    }

    // ============================================================
    // 8. MÉTHODE DE DÉMONSTRATION
    // ============================================================

    public static void runDemo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🚀 DÉMONSTRATION DU SERVICE DE PRÉDICTION");
        System.out.println("=".repeat(60));

        // 1. Vérifier l'état
        System.out.println("\n1. ÉTAT DU SERVICE:");
        System.out.println("   Modèle entraîné: " + (isModelTrained() ? "✅" : "❌"));
        System.out.println("   Modèle chargé: " + (isModelLoaded() ? "✅" : "❌"));

        // 2. Si pas de modèle, entraîner un rapide
        if (!isModelTrained()) {
            System.out.println("\n2. ENTRAÎNEMENT RAPIDE...");
            trainModel();
        }

        // 3. Analyse du dataset
        System.out.println("\n3. ANALYSE DU DATASET...");
        analyzeDataset();

        // 4. Prédictions de test
        System.out.println("\n4. PRÉDICTIONS DE TEST:");

        // Scénario normal
        System.out.println("\n   📊 Scénario NORMAL:");
        PredictionResult normal = predictAnomaly(50, 10, 0.05, 100, 75);
        System.out.println("   " + normal.toString());

        // Scénario anormal
        System.out.println("\n   📊 Scénario ANORMAL:");
        PredictionResult anomaly = predictAnomaly(200, 50, 0.3, 5, 30);
        System.out.println("   " + anomaly.toString());

        // Scénario limite
        System.out.println("\n   📊 Scénario LIMITE:");
        PredictionResult borderline = predictAnomaly(120, 25, 0.15, 50, 55);
        System.out.println("   " + borderline.toString());

        // 5. Évaluation
        System.out.println("\n5. ÉVALUATION DU MODÈLE...");
        String evalReport = evaluateModel();
        System.out.println("   ✓ Évaluation terminée");

        // 6. Informations finales
        System.out.println("\n6. INFORMATIONS FINALES:");
        System.out.println(getModelInfo());

        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ DÉMONSTRATION TERMINÉE");
        System.out.println("=".repeat(60));
    }

}