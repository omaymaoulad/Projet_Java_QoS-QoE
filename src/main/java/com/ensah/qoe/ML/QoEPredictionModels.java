package com.ensah.qoe.ML;

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.SMOreg;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.SerializationHelper;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.io.File;
import java.util.Random;

/**
 * Version corrigée avec gestion de la normalisation
 */
public class QoEPredictionModels {

    private Classifier model;
    private Instances trainingData;
    private Instances originalTrainingData; // IMPORTANT : données originales
    private String modelPath = "models/qoe_model.model";
    private Normalize normalizeFilter; // Filtre de normalisation
    private boolean useNormalization = false; // Flag pour savoir si on normalise

    /**
     * MODÈLE 1 : Random Forest (Recommandé)
     */
    public void trainRandomForest(Instances trainData) {
        try {
            System.out.println("[RF] Entrainement Random Forest...");

            // GARDER LES DONNÉES ORIGINALES
            this.originalTrainingData = new Instances(trainData);
            this.trainingData = new Instances(trainData);
            this.useNormalization = false; // Random Forest n'a pas besoin de normalisation

            RandomForest rf = new RandomForest();
            rf.setNumIterations(50);  // Réduit de 100 à 50 pour plus de rapidité
            rf.setMaxDepth(8);         // Réduit de 10 à 8
            rf.setNumFeatures(0);      // 0 = sqrt(numFeatures)
            rf.setSeed(1);

            System.out.println("[RF] Configuration: 50 arbres, profondeur max 8");

            rf.buildClassifier(trainData);
            this.model = rf;

            System.out.println("[OK] Random Forest entraine avec succes !\n");

        } catch (Exception e) {
            System.err.println("[ERREUR] Random Forest : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * MODÈLE 2 : Régression Linéaire
     */
    public void trainLinearRegression(Instances trainData) {
        try {
            System.out.println("[LR] Entrainement Regression Lineaire...");

            this.originalTrainingData = new Instances(trainData);
            this.trainingData = new Instances(trainData);
            this.useNormalization = false;

            LinearRegression lr = new LinearRegression();
            lr.setRidge(1.0e-8);
            lr.buildClassifier(trainData);

            this.model = lr;

            System.out.println("[OK] Regression Lineaire entrainee !\n");

        } catch (Exception e) {
            System.err.println("[ERREUR] Regression Lineaire : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * MODÈLE 3 : SVM Regression
     */
    public void trainSVM(Instances trainData) {
        try {
            System.out.println("🎯 Entraînement SVM...");

            this.originalTrainingData = new Instances(trainData);
            this.trainingData = new Instances(trainData);
            this.useNormalization = false;

            SMOreg svm = new SMOreg();
            svm.buildClassifier(trainData);

            this.model = svm;

            System.out.println("✅ SVM entraîné !");

        } catch (Exception e) {
            System.err.println("❌ Erreur SVM : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prédire le MOS pour de nouvelles données QoS
     * VERSION CORRIGÉE - Sans normalisation problématique
     */
    public double predictMOS(double latence, double jitter, double perte,
                             double bandePassante, double signalScore) {
        try {
            // VÉRIFICATION CRITIQUE
            if (model == null || trainingData == null) {
                System.err.println("⚠️ Modèle non initialisé - utilisation du calcul empirique");
                return calculateEmpiricalMOS(latence, jitter, perte, bandePassante, signalScore);
            }

            // Créer une nouvelle instance AVEC LES DONNÉES ORIGINALES (non normalisées)
            Instance newInstance = new DenseInstance(trainingData.numAttributes());
            newInstance.setDataset(trainingData);

            // Définir les valeurs (ORDRE IMPORTANT)
            newInstance.setValue(0, latence);
            newInstance.setValue(1, jitter);
            newInstance.setValue(2, perte);
            newInstance.setValue(3, bandePassante);
            newInstance.setValue(4, signalScore);
            // L'attribut 5 (MOS) sera prédit

            // Faire la prédiction
            double prediction = model.classifyInstance(newInstance);

            // DEBUG - Afficher les détails
            System.out.println("\n========== PREDICTION ML ==========");
            System.out.printf("Entrees: L=%.1f, J=%.1f, P=%.2f, BP=%.1f, S=%.1f\n",
                    latence, jitter, perte, bandePassante, signalScore);
            System.out.printf("MOS predit (ML): %.2f\n", prediction);

            // Limiter entre 1 et 5
            if (prediction < 1.0) {
                System.out.println("[WARN] MOS < 1.0, ajuste a 1.0");
                prediction = 1.0;
            }
            if (prediction > 5.0) {
                System.out.println("[WARN] MOS > 5.0, ajuste a 5.0");
                prediction = 5.0;
            }

            System.out.printf("MOS final: %.2f (%s)\n", prediction, predictQoECategory(prediction));
            System.out.println("===================================\n");

            return prediction;

        } catch (Exception e) {
            System.err.println("❌ Erreur prédiction ML: " + e.getMessage());
            e.printStackTrace();

            // Fallback
            System.out.println("⚠️ Utilisation du calcul empirique (fallback)");
            return calculateEmpiricalMOS(latence, jitter, perte, bandePassante, signalScore);
        }
    }

    /**
     * CALCUL EMPIRIQUE DU MOS (Fallback si ML échoue)
     */
    private double calculateEmpiricalMOS(double latence, double jitter, double perte,
                                         double bandePassante, double signal) {

        System.out.println("\n━━━━━━ CALCUL EMPIRIQUE MOS ━━━━━━");
        System.out.printf("Entrées: L=%.1f, J=%.1f, P=%.2f, BP=%.1f, S=%.1f\n",
                latence, jitter, perte, bandePassante, signal);

        // Calculer les scores individuels
        double scoreLatence = calculateLatenceScore(latence);
        double scoreJitter = calculateJitterScore(jitter);
        double scorePerte = calculatePerteScore(perte);
        double scoreBande = calculateBandePassanteScore(bandePassante);
        double scoreSignal = calculateSignalScore(signal);

        System.out.printf("Scores individuels:\n");
        System.out.printf("  - Latence: %.2f\n", scoreLatence);
        System.out.printf("  - Jitter: %.2f\n", scoreJitter);
        System.out.printf("  - Perte: %.2f\n", scorePerte);
        System.out.printf("  - Bande: %.2f\n", scoreBande);
        System.out.printf("  - Signal: %.2f\n", scoreSignal);

        // Moyenne pondérée
        double mos = (scoreLatence * 0.30) +
                (scorePerte * 0.25) +
                (scoreJitter * 0.20) +
                (scoreBande * 0.15) +
                (scoreSignal * 0.10);

        mos = Math.max(1.0, Math.min(5.0, mos));

        System.out.printf("MOS calculé: %.2f (%s)\n", mos, predictQoECategory(mos));
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        return mos;
    }

    private double calculateLatenceScore(double latence) {
        if (latence <= 10) return 5.0;
        if (latence <= 30) return 4.5;
        if (latence <= 60) return 4.0;
        if (latence <= 100) return 3.0;
        if (latence <= 150) return 2.5;
        if (latence <= 200) return 2.0;
        if (latence <= 300) return 1.5;
        return 1.0;
    }

    private double calculateJitterScore(double jitter) {
        if (jitter <= 2) return 5.0;
        if (jitter <= 5) return 4.5;
        if (jitter <= 10) return 4.0;
        if (jitter <= 20) return 3.0;
        if (jitter <= 30) return 2.5;
        if (jitter <= 40) return 2.0;
        if (jitter <= 60) return 1.5;
        return 1.0;
    }

    private double calculatePerteScore(double perte) {
        if (perte <= 0.1) return 5.0;
        if (perte <= 0.5) return 4.5;
        if (perte <= 1.0) return 4.0;
        if (perte <= 3.0) return 3.0;
        if (perte <= 5.0) return 2.5;
        if (perte <= 8.0) return 2.0;
        if (perte <= 12.0) return 1.5;
        return 1.0;
    }

    private double calculateBandePassanteScore(double bande) {
        if (bande >= 90) return 5.0;
        if (bande >= 60) return 4.5;
        if (bande >= 40) return 4.0;
        if (bande >= 25) return 3.0;
        if (bande >= 15) return 2.5;
        if (bande >= 10) return 2.0;
        if (bande >= 5) return 1.5;
        return 1.0;
    }

    private double calculateSignalScore(double signal) {
        if (signal >= 90) return 5.0;
        if (signal >= 80) return 4.5;
        if (signal >= 65) return 4.0;
        if (signal >= 50) return 3.0;
        if (signal >= 40) return 2.5;
        if (signal >= 30) return 2.0;
        if (signal >= 20) return 1.5;
        return 1.0;
    }

    /**
     * Évaluer le modèle
     */
    public void evaluateModel(Instances testData) {
        try {
            System.out.println("\n========== EVALUATION MODELE ==========");

            Evaluation eval = new Evaluation(trainingData);
            eval.evaluateModel(model, testData);

            System.out.println("Correlation : " + String.format("%.4f", eval.correlationCoefficient()));
            System.out.println("MAE : " + String.format("%.4f", eval.meanAbsoluteError()));
            System.out.println("RMSE : " + String.format("%.4f", eval.rootMeanSquaredError()));
            System.out.println("R2 : " + String.format("%.4f", calculateR2(eval)));
            System.out.println("=======================================\n");

        } catch (Exception e) {
            System.err.println("[ERREUR] Evaluation : " + e.getMessage());
        }
    }

    /**
     * Validation croisée 10-fold
     */
    public void crossValidation(Instances data) {
        try {
            System.out.println("\n🔄 VALIDATION CROISÉE (10-Fold)");
            System.out.println("═══════════════════════════════");

            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(model, data, 10, new Random(1));

            System.out.println("Corrélation : " + String.format("%.4f", eval.correlationCoefficient()));
            System.out.println("MAE : " + String.format("%.4f", eval.meanAbsoluteError()));
            System.out.println("RMSE : " + String.format("%.4f", eval.rootMeanSquaredError()));
            System.out.println("═══════════════════════════════\n");

        } catch (Exception e) {
            System.err.println("❌ Erreur validation croisée : " + e.getMessage());
        }
    }

    public String predictQoECategory(double mos) {
        if (mos >= 4.5) return "Excellent";
        else if (mos >= 4.0) return "Bon";
        else if (mos >= 3.0) return "Moyen";
        else if (mos >= 2.0) return "Médiocre";
        else return "Mauvais";
    }

    public void saveModel() {
        try {
            File dir = new File("models");
            if (!dir.exists()) dir.mkdirs();

            SerializationHelper.write(modelPath, model);

            String dataPath = "models/training_data.arff";
            DataPreparation.saveDataset(originalTrainingData != null ? originalTrainingData : trainingData, dataPath);

            System.out.println("✅ Modèle et données sauvegardés");

        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde : " + e.getMessage());
        }
    }

    public void loadModel() {
        try {
            model = (Classifier) SerializationHelper.read(modelPath);
            System.out.println("✅ Modèle chargé : " + modelPath);

            if (trainingData == null) {
                trainingData = DataPreparation.loadDataset("models/training_data.arff");
                originalTrainingData = new Instances(trainingData);
                if (trainingData != null) {
                    System.out.println("✅ Données d'entraînement chargées");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement modèle : " + e.getMessage());
        }
    }

    public void printFeatureImportance() {
        System.out.println("\n🎯 IMPORTANCE DES FEATURES");
        System.out.println("═══════════════════════════════");
        System.out.println("1. Latence (30%)      : ⭐⭐⭐⭐⭐");
        System.out.println("2. Perte paquets (25%): ⭐⭐⭐⭐⭐");
        System.out.println("3. Jitter (20%)       : ⭐⭐⭐⭐");
        System.out.println("4. Bande passante (15%): ⭐⭐⭐");
        System.out.println("5. Signal (10%)       : ⭐⭐");
        System.out.println("═══════════════════════════════\n");
    }

    private double calculateR2(Evaluation eval) {
        try {
            double correlation = eval.correlationCoefficient();
            return correlation * correlation;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public Classifier getModel() { return model; }
    public Instances getTrainingData() { return trainingData; }
}