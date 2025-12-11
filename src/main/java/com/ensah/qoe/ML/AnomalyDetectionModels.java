package com.ensah.qoe.ML;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.util.Random;

public class AnomalyDetectionModels {

    private Classifier model;
    private Instances trainingHeader;
    private String modelPath = "models/anomaly_model.model";
    private String normalizeFilterPath = "models/anomaly_norm.filter";
    private weka.filters.Filter normalizeFilter;
    public static String lastAccuracy = "--";
    public static String lastPrecision = "--";
    public static String lastRecall = "--";
    public static String lastF1 = "--";

    // ================================
    // 1) ENTRAÎNEMENT DES MODÈLES
    // ================================
    public void trainJ48(Instances train) throws Exception {
        System.out.println("🌳 J48 entraîné !");
        J48 tree = new J48();
        tree.setUnpruned(false);
        tree.buildClassifier(train);

        this.model = tree;
        this.trainingHeader = new Instances(train, 0);
    }

    public void trainNaiveBayes(Instances train) throws Exception {
        System.out.println("📘 NaiveBayes entraîné !");
        NaiveBayes nb = new NaiveBayes();
        nb.buildClassifier(train);

        this.model = nb;
        this.trainingHeader = new Instances(train, 0);
    }

    public void trainKNN(Instances train) throws Exception {
        System.out.println("👣 KNN(k=3) entraîné !");
        IBk knn = new IBk(3);
        knn.buildClassifier(train);

        this.model = knn;
        this.trainingHeader = new Instances(train, 0);
    }



    // ================================
    // 2) ÉVALUATION SUR TEST
    // ================================
    public void evaluate(Instances test) throws Exception {

        if (model == null) {
            System.err.println("❌ Modèle non initialisé !");
            return;
        }

        Evaluation eval = new Evaluation(test);
        eval.evaluateModel(model, test);

        System.out.println("\n📈 ÉVALUATION");
        System.out.println(eval.toSummaryString());
        System.out.println(eval.toClassDetailsString());
        System.out.println(eval.toMatrixString());
        lastAccuracy = String.format("%.2f%%", eval.pctCorrect());
        lastPrecision = String.format("%.2f", eval.weightedPrecision());
        lastRecall = String.format("%.2f", eval.weightedRecall());
        lastF1 = String.format("%.2f", eval.weightedFMeasure());
    }

    // ================================
    // 3) VALIDATION CROISÉE
    // ================================
    public void crossValidate(Instances train) throws Exception {
        if (model == null) return;

        System.out.println("\n🔄 VALIDATION CROISÉE 10-FOLD");

        Evaluation eval = new Evaluation(train);
        eval.crossValidateModel(model, train, 10, new Random(1));

        System.out.println(eval.toSummaryString());
    }

    // ================================
    // 4) SAUVEGARDE DU MODÈLE
    // ================================
    public void saveModel() {
        try {
            SerializationHelper.write(modelPath, model);
            if (normalizeFilter != null) {
                SerializationHelper.write(normalizeFilterPath, normalizeFilter);
            }
            System.out.println("💾 Modèle sauvegardé !");
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde modèle : " + e.getMessage());
        }
    }

    // ================================
    // 5) CHARGEMENT DU MODÈLE
    // ================================
    public void loadModel() {
        try {
            model = (Classifier) SerializationHelper.read(modelPath);
            normalizeFilter = (weka.filters.Filter) SerializationHelper.read(normalizeFilterPath);
            System.out.println("📥 Modèle chargé !");
        } catch (Exception e) {
            System.err.println("❌ Impossible de charger le modèle : " + e.getMessage());
        }
    }

    // ================================
    // GETTERS
    // ================================
    public Classifier getModel() {
        return model;
    }

    public Instances getTrainingHeader() {
        return trainingHeader;
    }

    public weka.filters.Filter getNormalizeFilter() {
        return normalizeFilter;
    }

    public void setNormalizeFilter(weka.filters.Filter filter) {
        this.normalizeFilter = filter;
    }
}
