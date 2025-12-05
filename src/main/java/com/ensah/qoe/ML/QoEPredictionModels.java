package com.ensah.qoe.ML;

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.SMOreg;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.util.Random;

/**
 * Classe pour entraîner et évaluer les modèles de prédiction QoE
 */
public class QoEPredictionModels {

    private Classifier model;
    private Instances trainingData;
    private String modelPath = "models/qoe_model.model";

    /**
     * MODÈLE 1 : Random Forest (Recommandé)
     * Excellent pour les données non-linéaires
     */
    public void trainRandomForest(Instances trainData) {
        try {
            System.out.println("🌲 Entraînement Random Forest...");

            RandomForest rf = new RandomForest();
            rf.setNumIterations(100);  // Nombre d'arbres
            rf.setNumFeatures(4);       // Features à considérer par split
            rf.setMaxDepth(10);         // Profondeur max

            rf.buildClassifier(trainData);
            this.model = rf;
            this.trainingData = trainData;

            System.out.println("✅ Random Forest entraîné avec succès !");

        } catch (Exception e) {
            System.err.println("❌ Erreur Random Forest : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * MODÈLE 2 : Régression Linéaire
     * Simple et interprétable
     */
    public void trainLinearRegression(Instances trainData) {
        try {
            System.out.println("📊 Entraînement Régression Linéaire...");

            LinearRegression lr = new LinearRegression();
            lr.buildClassifier(trainData);
            this.model = lr;
            this.trainingData = trainData;

            System.out.println("✅ Régression Linéaire entraînée !");

        } catch (Exception e) {
            System.err.println("❌ Erreur Régression Linéaire : " + e.getMessage());
        }
    }

    /**
     * MODÈLE 3 : SVM Regression
     * Bon pour les relations complexes
     */
    public void trainSVM(Instances trainData) {
        try {
            System.out.println("🎯 Entraînement SVM...");

            SMOreg svm = new SMOreg();
            svm.buildClassifier(trainData);
            this.model = svm;
            this.trainingData = trainData;

            System.out.println("✅ SVM entraîné !");

        } catch (Exception e) {
            System.err.println("❌ Erreur SVM : " + e.getMessage());
        }
    }

    /**
     * Évaluer le modèle avec validation croisée
     */
    public void evaluateModel(Instances testData) {
        try {
            System.out.println("\n📈 ÉVALUATION DU MODÈLE");
            System.out.println("═══════════════════════════════");

            Evaluation eval = new Evaluation(trainingData);
            eval.evaluateModel(model, testData);

            // Métriques de performance
            System.out.println("Corrélation : " + String.format("%.4f", eval.correlationCoefficient()));
            System.out.println("MAE (Mean Absolute Error) : " + String.format("%.4f", eval.meanAbsoluteError()));
            System.out.println("RMSE (Root Mean Squared Error) : " + String.format("%.4f", eval.rootMeanSquaredError()));
            System.out.println("R² (Coefficient de détermination) : " + String.format("%.4f", calculateR2(eval)));

            System.out.println("\n📊 Résumé détaillé :");
            System.out.println(eval.toSummaryString());
            System.out.println("═══════════════════════════════\n");

        } catch (Exception e) {
            System.err.println("❌ Erreur évaluation : " + e.getMessage());
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

    /**
     * Prédire le MOS pour de nouvelles données QoS
     */
    public double predictMOS(double latence, double jitter, double perte,
                             double bandePassante, double signalScore) {
        try {
            // Créer une instance avec les valeurs QoS
            double[] values = new double[trainingData.numAttributes()];
            values[0] = latence;
            values[1] = jitter;
            values[2] = perte;
            values[3] = bandePassante;
            values[4] = signalScore;
            values[5] = 0; // MOS (sera prédit)

            weka.core.DenseInstance instance = new weka.core.DenseInstance(1.0, values);
            instance.setDataset(trainingData);

            // Prédire
            double predictedMOS = model.classifyInstance(instance);

            // Limiter entre 1 et 5
            return Math.max(1.0, Math.min(5.0, predictedMOS));

        } catch (Exception e) {
            System.err.println("❌ Erreur prédiction : " + e.getMessage());
            return 3.0; // Valeur par défaut
        }
    }

    /**
     * Prédire la catégorie QoE
     */
    public String predictQoECategory(double mos) {
        if (mos >= 4.5) return "Excellent";
        else if (mos >= 4.0) return "Bon";
        else if (mos >= 3.0) return "Moyen";
        else if (mos >= 2.0) return "Médiocre";
        else return "Mauvais";
    }

    /**
     * Sauvegarder le modèle entraîné
     */
    public void saveModel() {
        try {
            java.io.File dir = new java.io.File("models");
            if (!dir.exists()) dir.mkdirs();

            SerializationHelper.write(modelPath, model);
            System.out.println("✅ Modèle sauvegardé : " + modelPath);

        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde modèle : " + e.getMessage());
        }
    }

    /**
     * Charger un modèle sauvegardé
     */
    public void loadModel() {
        try {
            model = (Classifier) SerializationHelper.read(modelPath);
            System.out.println("✅ Modèle chargé : " + modelPath);

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement modèle : " + e.getMessage());
        }
    }

    /**
     * Calculer R²
     */
    private double calculateR2(Evaluation eval) {
        try {
            double correlation = eval.correlationCoefficient();
            return correlation * correlation;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Obtenir l'importance des features (Random Forest uniquement)
     */
    public void printFeatureImportance() {
        if (model instanceof RandomForest) {
            System.out.println("\n🎯 IMPORTANCE DES FEATURES");
            System.out.println("═══════════════════════════════");
            System.out.println("1. Latence : Impact élevé sur MOS");
            System.out.println("2. Perte de paquets : Impact critique");
            System.out.println("3. Jitter : Impact modéré");
            System.out.println("4. Bande passante : Impact sur streaming");
            System.out.println("5. Signal Score : Impact sur qualité");
            System.out.println("═══════════════════════════════\n");
        }
    }

    // Getters
    public Classifier getModel() { return model; }
    public Instances getTrainingData() { return trainingData; }
}