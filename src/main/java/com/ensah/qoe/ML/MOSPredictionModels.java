package com.ensah.qoe.ML;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SelectedTag;

import java.util.Random;

public class MOSPredictionModels {

    private Classifier currentModel;
    private String modelType;

    // ======================================================
    // 1️⃣ CONSTRUCTEUR
    // ======================================================
    public MOSPredictionModels() {
        // Modèle par défaut
        this.currentModel = new RandomForest();
        this.modelType = "RandomForest";
    }

    // ======================================================
    // 2️⃣ ENTRAÎNEMENT RANDOM FOREST
    // ======================================================
    public void trainRandomForest(Instances trainData) throws Exception {
        System.out.println("🌲 Entraînement RandomForest pour MOS...");

        RandomForest rf = new RandomForest();

        // Configuration
        rf.setNumIterations(100);
        rf.setMaxDepth(20);
        rf.setSeed(42);

        currentModel = rf;
        modelType = "RandomForest";
        currentModel.buildClassifier(trainData);

        System.out.println("✅ RandomForest MOS entraîné");
    }

    // ======================================================
    // 3️⃣ ENTRAÎNEMENT RÉSEAU DE NEURONES
    // ======================================================
    public void trainNeuralNetwork(Instances trainData) throws Exception {
        System.out.println("🧠 Entraînement réseau de neurones pour MOS...");

        MultilayerPerceptron mlp = new MultilayerPerceptron();

        // Configuration
        mlp.setHiddenLayers("64,32,16");
        mlp.setLearningRate(0.01);
        mlp.setMomentum(0.2);
        mlp.setTrainingTime(500);
        mlp.setSeed(42);

        currentModel = mlp;
        modelType = "NeuralNetwork";
        currentModel.buildClassifier(trainData);

        System.out.println("✅ Réseau de neurones MOS entraîné");
    }

    // ======================================================
// 4️⃣ ENTRAÎNEMENT RÉGRESSION LINÉAIRE
// ======================================================
    public void trainLinearRegression(Instances trainData) throws Exception {
        System.out.println("📈 Entraînement régression linéaire pour MOS...");

        LinearRegression lr = new LinearRegression();

        // Configuration CORRECTE
        // Utiliser les méthodes disponibles dans LinearRegression
        lr.setRidge(1.0E-8);                    // Paramètre Ridge pour la régularisation
        lr.setMinimal(false);                   // Ne pas utiliser le modèle minimal
        lr.setEliminateColinearAttributes(true); // Éliminer attributs colinéaires
        lr.setAttributeSelectionMethod(new SelectedTag(
                LinearRegression.SELECTION_NONE,
                LinearRegression.TAGS_SELECTION
        ));

        currentModel = lr;
        modelType = "LinearRegression";
        currentModel.buildClassifier(trainData);

        System.out.println("✅ Régression linéaire MOS entraînée");

        // Afficher l'équation de régression
        printRegressionEquation(lr);
    }

    // ======================================================
// MÉTHODE POUR AFFICHER L'ÉQUATION
// ======================================================
    private void printRegressionEquation(LinearRegression lr) {
        try {
            System.out.println("\n📈 Équation de régression :");
            System.out.println(lr);

            // Extraire les coefficients
            double[] coefficients = lr.coefficients();
            if (coefficients != null && coefficients.length > 0) {
                System.out.println("\nCoefficients significatifs :");
                for (int i = 0; i < Math.min(coefficients.length, 10); i++) {
                    if (Math.abs(coefficients[i]) > 0.001) {
                        System.out.println(String.format("  Coef[%d] = %.4f", i, coefficients[i]));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Impossible d'afficher l'équation de régression");
        }
    }

    // ======================================================
    // 5️⃣ ÉVALUATION
    // ======================================================
    public EvaluationResult evaluate(Instances testData) throws Exception {
        if (currentModel == null) {
            throw new IllegalStateException("❌ Aucun modèle entraîné");
        }

        System.out.println("📊 Évaluation du modèle MOS...");

        Evaluation eval = new Evaluation(testData);
        eval.evaluateModel(currentModel, testData);

        // Métriques de régression
        double rmse = eval.rootMeanSquaredError();
        double mae = eval.meanAbsoluteError();
        double r2 = eval.correlationCoefficient();
        double mape = calculateMAPE(eval, testData);

        // Affichage
        System.out.println("\n=== RÉSULTATS ÉVALUATION MOS ===");
        System.out.println("RMSE  : " + String.format("%.4f", rmse));
        System.out.println("MAE   : " + String.format("%.4f", mae));
        System.out.println("R²    : " + String.format("%.4f", r2));
        System.out.println("MAPE  : " + String.format("%.2f%%", mape * 100));
        System.out.println("=============================\n");

        return new EvaluationResult(rmse, mae, r2, mape);
    }

    // ======================================================
    // 6️⃣ PRÉDICTION POUR UNE INSTANCE
    // ======================================================
    public double predict(Instances data, int instanceIndex) throws Exception {
        if (currentModel == null) {
            throw new IllegalStateException("❌ Aucun modèle entraîné");
        }

        return currentModel.classifyInstance(data.instance(instanceIndex));
    }

    // ======================================================
    // 7️⃣ CALCUL MAPE
    // ======================================================
    private double calculateMAPE(Evaluation eval, Instances data) throws Exception {
        double totalError = 0;
        int count = 0;

        for (int i = 0; i < data.numInstances(); i++) {
            double actual = data.instance(i).classValue();
            if (actual > 0) { // Éviter division par zéro
                double predicted = currentModel.classifyInstance(data.instance(i));
                totalError += Math.abs((actual - predicted) / actual);
                count++;
            }
        }

        return (count > 0) ? totalError / count : 0;
    }

    // ======================================================
    // 8️⃣ GETTERS
    // ======================================================
    public Classifier getModel() {
        return currentModel;
    }

    public String getModelType() {
        return modelType;
    }

    public boolean isModelTrained() {
        return currentModel != null;
    }

    // ======================================================
    // 9️⃣ CLASSE RÉSULTATS
    // ======================================================
    public static class EvaluationResult {
        public final double rmse;
        public final double mae;
        public final double r2;
        public final double mape;

        public EvaluationResult(double rmse, double mae, double r2, double mape) {
            this.rmse = rmse;
            this.mae = mae;
            this.r2 = r2;
            this.mape = mape;
        }
    }
}