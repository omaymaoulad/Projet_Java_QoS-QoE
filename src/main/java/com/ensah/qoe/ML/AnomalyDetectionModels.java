package com.ensah.qoe.ML;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AnomalyDetectionModels {

    private Classifier model;
    private Instances trainingHeader;
    private String algorithmName;

    // Chemins pour les modèles
    private static final String MODEL_DIR = "models/";
    private String modelPath = MODEL_DIR + "anomaly_model.model";

    // Métriques de performance
    private double accuracy;
    private double precision;
    private double recall;
    private double f1Score;
    private double auc;
    private String confusionMatrix;

    // Statistiques globales
    public static class GlobalStats {
        public static String lastAccuracy = "--";
        public static String lastPrecision = "--";
        public static String lastRecall = "--";
        public static String lastF1 = "--";
        public static String bestAlgorithm = "--";
        public static double bestAccuracy = 0.0;
    }

    // ================================
    // CONSTRUCTOR & INITIALIZATION
    // ================================

    public AnomalyDetectionModels() {
        // Créer le dossier models s'il n'existe pas
        File modelDir = new File(MODEL_DIR);
        if (!modelDir.exists()) {
            modelDir.mkdirs();
        }
    }

    // ================================
    // 1) MODEL TRAINING METHODS
    // ================================

    /**
     * Entraîne un RandomForest avec paramètres optimisés
     */
    public void trainRandomForest(Instances train) throws Exception {
        System.out.println("🌲 Entraînement RandomForest...");
        algorithmName = "RandomForest";

        RandomForest rf = new RandomForest();
        // Paramètres optimisés
        rf.setNumIterations(100);
        rf.setMaxDepth(10);
        rf.setNumFeatures(0); // Auto-détection
        rf.setSeed(42); // Pour la reproductibilité

        rf.buildClassifier(train);
        this.model = rf;
        this.trainingHeader = new Instances(train, 0);

        System.out.println("✅ RandomForest entraîné avec succès!");
    }

    /**
     * Entraîne un arbre de décision J48
     */
    public void trainJ48(Instances train) throws Exception {
        System.out.println("🌳 Entraînement J48 (C4.5)...");
        algorithmName = "J48";

        J48 tree = new J48();
        // Paramètres optimisés
        tree.setConfidenceFactor(0.25f);
        tree.setMinNumObj(2);
        tree.setUnpruned(false);
        tree.setSeed(42);

        tree.buildClassifier(train);
        this.model = tree;
        this.trainingHeader = new Instances(train, 0);

        System.out.println("✅ J48 entraîné avec succès!");
    }

    /**
     * Entraîne un classifieur NaiveBayes
     */
    public void trainNaiveBayes(Instances train) throws Exception {
        System.out.println("📘 Entraînement NaiveBayes...");
        algorithmName = "NaiveBayes";

        NaiveBayes nb = new NaiveBayes();
        // Utiliser la distribution normale pour les attributs numériques
        nb.setUseKernelEstimator(false);
        nb.setUseSupervisedDiscretization(false);

        nb.buildClassifier(train);
        this.model = nb;
        this.trainingHeader = new Instances(train, 0);

        System.out.println("✅ NaiveBayes entraîné avec succès!");
    }

    /**
     * Entraîne un KNN (IBk)
     */
    public void trainKNN(Instances train) throws Exception {
        System.out.println("⚡ Entraînement KNN...");
        algorithmName = "KNN";

        IBk knn = new IBk();
        knn.setKNN(3);

        // Correction pour setDistanceWeighting
        knn.setDistanceWeighting(new weka.core.SelectedTag(
                IBk.WEIGHT_INVERSE,
                IBk.TAGS_WEIGHTING
        ));

        knn.setCrossValidate(false);
        knn.buildClassifier(train);
        this.model = knn;
        this.trainingHeader = new Instances(train, 0);

        System.out.println("✅ KNN entraîné avec succès!");
    }

    /**
     * Entraîne un SVM (SMO)
     */
    public void trainSVM(Instances train) throws Exception {
        System.out.println("⚡ Entraînement SVM...");
        algorithmName = "SVM";

        weka.classifiers.functions.SMO svm = new weka.classifiers.functions.SMO();
        svm.setC(1.0);

        // Utiliser un kernel spécifique au lieu de setBuildLogisticModels
        svm.setKernel(new weka.classifiers.functions.supportVector.RBFKernel());

        svm.buildClassifier(train);
        this.model = svm;
        this.trainingHeader = new Instances(train, 0);

        System.out.println("✅ SVM entraîné avec succès!");
    }

    /**
     * Entraîne un MLP (Multi-layer Perceptron)
     */
    public void trainMLP(Instances train) throws Exception {
        System.out.println("🧠 Entraînement MLP (Neural Network)...");
        algorithmName = "MLP";

        weka.classifiers.functions.MultilayerPerceptron mlp = new weka.classifiers.functions.MultilayerPerceptron();
        mlp.setLearningRate(0.3);
        mlp.setMomentum(0.2);
        mlp.setTrainingTime(500);
        mlp.setHiddenLayers("a"); // Auto-détection

        mlp.buildClassifier(train);
        this.model = mlp;
        this.trainingHeader = new Instances(train, 0);

        System.out.println("✅ MLP entraîné avec succès!");
    }

    /**
     * Méthode générique pour entraîner tous les modèles et sélectionner le meilleur
     */
    public Map<String, Double> trainAndCompareAll(Instances train, Instances test) throws Exception {
        System.out.println("\n🔄 Comparaison de tous les algorithmes...");

        Map<String, Classifier> models = new HashMap<>();
        Map<String, Double> accuracies = new HashMap<>();

        // Liste des algorithmes à tester
        String[] algorithms = {"RandomForest", "J48", "NaiveBayes", "KNN", "SVM", "MLP"};

        for (String algo : algorithms) {
            try {
                switch (algo) {
                    case "RandomForest":
                        trainRandomForest(train);
                        break;
                    case "J48":
                        trainJ48(train);
                        break;
                    case "NaiveBayes":
                        trainNaiveBayes(train);
                        break;
                    case "KNN":
                        trainKNN(train);
                        break;
                    case "SVM":
                        trainSVM(train);
                        break;
                    case "MLP":
                        trainMLP(train);
                        break;
                }

                // Évaluation sur le test set
                Evaluation eval = new Evaluation(test);
                eval.evaluateModel(model, test);
                double accuracy = eval.pctCorrect();

                models.put(algo, model);
                accuracies.put(algo, accuracy);

                System.out.printf("  %-15s → Accuracy: %.2f%%\n", algo, accuracy);

                // Mettre à jour le meilleur modèle global
                if (accuracy > GlobalStats.bestAccuracy) {
                    GlobalStats.bestAccuracy = accuracy;
                    GlobalStats.bestAlgorithm = algo;
                    // Sauvegarder le meilleur modèle
                    this.model = model;
                    this.algorithmName = algo;
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur avec " + algo + ": " + e.getMessage());
            }
        }

        // Afficher le meilleur modèle
        System.out.println("\n🏆 Meilleur modèle: " + GlobalStats.bestAlgorithm +
                " (Accuracy: " + String.format("%.2f%%", GlobalStats.bestAccuracy) + ")");

        return accuracies;
    }

    // ================================
    // 2) EVALUATION METHODS
    // ================================

    /**
     * Évaluation complète sur le jeu de test
     */
    // ================================
// 2) EVALUATION METHODS (SANS CONFUSION MATRIX)
// ================================
    // ================================
// 2) EVALUATION METHODS (SAFE - NO WEKA METRICS)
// ================================
    public EvaluationResult evaluate(Instances test) throws Exception {

        if (model == null) {
            throw new Exception("❌ Aucun modèle entraîné !");
        }

        System.out.println("\n📈 ÉVALUATION DU MODÈLE: " + algorithmName);

        int TP = 0, TN = 0, FP = 0, FN = 0;

        for (int i = 0; i < test.numInstances(); i++) {

            Instance inst = test.instance(i);
            double actual = inst.classValue();
            double predicted = model.classifyInstance(inst);

            if (actual == 1.0 && predicted == 1.0) TP++;
            else if (actual == 0.0 && predicted == 0.0) TN++;
            else if (actual == 0.0 && predicted == 1.0) FP++;
            else if (actual == 1.0 && predicted == 0.0) FN++;
        }

        double total = TP + TN + FP + FN;

        this.accuracy = (TP + TN) / total;

        this.precision = (TP + FP) == 0 ? 0 : (double) TP / (TP + FP);
        this.recall    = (TP + FN) == 0 ? 0 : (double) TP / (TP + FN);
        this.f1Score   = (precision + recall) == 0
                ? 0
                : 2 * precision * recall / (precision + recall);

        this.auc = -1; // optionnel (ou calculé plus tard)

        // Logs propres
        System.out.println("Accuracy  : " + accuracy);
        System.out.println("Precision : " + precision);
        System.out.println("Recall    : " + recall);
        System.out.println("F1-score  : " + f1Score);

        return new EvaluationResult(
                accuracy,
                precision,
                recall,
                f1Score,
                auc,
                null
        );
    }





    /**
     * Validation croisée
     */
    public void crossValidate(Instances data, int folds) throws Exception {
        if (model == null) {
            throw new Exception("❌ Aucun modèle entraîné !");
        }

        System.out.println("\n🔄 VALIDATION CROISÉE (" + folds + "-fold)");

        Evaluation eval = new Evaluation(data);
        eval.crossValidateModel(model, data, folds, new Random(42));

        System.out.println("=".repeat(50));
        System.out.println("📊 RÉSULTATS VALIDATION CROISÉE");
        System.out.println("=".repeat(50));
        System.out.println(eval.toSummaryString());
        System.out.println("\n📈 MÉTRIQUES:");
        System.out.printf("  Accuracy (CV):  %.2f%%\n", eval.pctCorrect());
        System.out.printf("  Precision (CV): %.4f\n", eval.weightedPrecision());
        System.out.printf("  Recall (CV):    %.4f\n", eval.weightedRecall());
        System.out.printf("  F1-Score (CV):  %.4f\n", eval.weightedFMeasure());
    }

    /**
     * Prédiction sur de nouvelles instances
     */
    public double[] predict(Instances newData) throws Exception {
        if (model == null) {
            throw new Exception("❌ Aucun modèle chargé ou entraîné !");
        }

        if (trainingHeader == null) {
            throw new Exception("❌ En-tête d'entraînement manquant !");
        }

        // Préparation des données pour la prédiction
        Instances dataToPredict = new Instances(trainingHeader);
        for (int i = 0; i < newData.numInstances(); i++) {
            dataToPredict.add(newData.instance(i));
        }

        // Supprimer l'attribut classe s'il existe (pour la prédiction)
        Remove remove = new Remove();
        remove.setAttributeIndices("" + (dataToPredict.classIndex() + 1));
        remove.setInputFormat(dataToPredict);
        Instances dataWithoutClass = Filter.useFilter(dataToPredict, remove);

        double[] predictions = new double[newData.numInstances()];
        for (int i = 0; i < newData.numInstances(); i++) {
            predictions[i] = model.classifyInstance(dataToPredict.instance(i));
        }

        return predictions;
    }

    /**
     * Prédiction d'une seule instance
     */
    public double predictSingle(Instance instance) throws Exception {
        if (model == null) {
            throw new Exception("❌ Aucun modèle chargé ou entraîné !");
        }

        Instance inst = (Instance) instance.copy();
        inst.setDataset(trainingHeader);

        return model.classifyInstance(inst);
    }

    // ================================
    // 3) MODEL PERSISTENCE
    // ================================

    /**
     * Sauvegarde du modèle
     */
    public void saveModel() throws Exception {
        if (model == null) {
            throw new Exception("❌ Aucun modèle à sauvegarder !");
        }

        // Créer un nom de fichier avec timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        String filename = MODEL_DIR + "anomaly_" + algorithmName + "_" + timestamp + ".model";

        SerializationHelper.write(filename, new Object[]{model, trainingHeader, algorithmName});
        this.modelPath = filename;

        System.out.println("💾 Modèle sauvegardé: " + filename);
    }

    /**
     * Chargement du modèle
     */
    public void loadModel(String filePath) throws Exception {
        Object[] loaded = (Object[]) SerializationHelper.read(filePath);
        this.model = (Classifier) loaded[0];
        this.trainingHeader = (Instances) loaded[1];
        this.algorithmName = (String) loaded[2];
        this.modelPath = filePath;

        System.out.println("📥 Modèle chargé: " + algorithmName + " (" + filePath + ")");
    }

    /**
     * Chargement du dernier modèle
     */
    public void loadLatestModel() throws Exception {
        File modelDir = new File(MODEL_DIR);
        File[] modelFiles = modelDir.listFiles((dir, name) -> name.endsWith(".model"));

        if (modelFiles == null || modelFiles.length == 0) {
            throw new Exception("❌ Aucun modèle trouvé dans " + MODEL_DIR);
        }

        // Trouver le fichier le plus récent
        File latestFile = modelFiles[0];
        for (File file : modelFiles) {
            if (file.lastModified() > latestFile.lastModified()) {
                latestFile = file;
            }
        }

        loadModel(latestFile.getPath());
    }

    // ================================
    // 4) UTILITY METHODS
    // ================================

    /**
     * Exporte les résultats d'évaluation vers un fichier CSV
     */
    public void exportResultsToCSV(String filePath, EvaluationResult result) throws Exception {
        FileWriter writer = new FileWriter(filePath, true);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());

        writer.write(String.format("%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f\n",
                timestamp,
                algorithmName,
                result.accuracy,
                result.precision,
                result.recall,
                result.f1Score,
                result.auc));

        writer.close();
        System.out.println("📄 Résultats exportés vers: " + filePath);
    }

    /**
     * Affiche un résumé du modèle
     */
    public void printModelSummary() {
        if (model == null) {
            System.out.println("❌ Aucun modèle disponible");
            return;
        }

        System.out.println("\n📋 RÉSUMÉ DU MODÈLE");
        System.out.println("=".repeat(40));
        System.out.println("Algorithme: " + algorithmName);
        System.out.println("Chemin: " + modelPath);
        System.out.println("Nombre attributs: " + (trainingHeader != null ? trainingHeader.numAttributes() : "N/A"));
        System.out.println("Classe: " + (trainingHeader != null ? trainingHeader.classAttribute().name() : "N/A"));

        if (accuracy > 0) {
            System.out.println("\n📈 DERNIÈRES PERFORMANCES:");
            System.out.printf("  Accuracy:  %.2f%%\n", accuracy);
            System.out.printf("  Precision: %.4f\n", precision);
            System.out.printf("  Recall:    %.4f\n", recall);
            System.out.printf("  F1-Score:  %.4f\n", f1Score);
        }
    }

    /**
     * Classe pour stocker les résultats d'évaluation
     */
    public static class EvaluationResult {
        public final double accuracy;
        public final double precision;
        public final double recall;
        public final double f1Score;
        public final double auc;
        public final String confusionMatrix;

        public EvaluationResult(double accuracy, double precision, double recall,
                                double f1Score, double auc, String confusionMatrix) {
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.f1Score = f1Score;
            this.auc = auc;
            this.confusionMatrix = confusionMatrix;
        }
    }

    // ================================
    // GETTERS & SETTERS
    // ================================

    public Classifier getModel() {
        return model;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getF1Score() {
        return f1Score;
    }

    public double getAuc() {
        return auc;
    }

    public String getConfusionMatrix() {
        return confusionMatrix;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }
}