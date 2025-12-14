package com.ensah.qoe.ML;

public class MLConfig {

    // Chemins des fichiers
    public static final String DATA_PATH = "src/main/resources/CSV/prediction_dataset.csv";
    public static final String MODEL_DIR = "models/";
    public static final String RESULTS_DIR = "results/";

    // Paramètres d'entraînement
    public static final int RANDOM_SEED = 42;
    public static final double TRAIN_TEST_RATIO = 0.7;
    public static final int CROSS_VALIDATION_FOLDS = 10;

    // Paramètres des modèles
    public static class RandomForestParams {
        public static final int NUM_TREES = 100;
        public static final int MAX_DEPTH = 10;
    }

    public static class J48Params {
        public static final float CONFIDENCE_FACTOR = 0.25f;
        public static final int MIN_OBJECTS = 2;
    }

    public static class KNNParams {
        public static final int K = 3;
        public static final boolean CROSS_VALIDATE_K = false;
    }

    // Seuils de performance
    public static final double GOOD_ACCURACY = 85.0;
    public static final double GOOD_PRECISION = 0.85;
    public static final double GOOD_RECALL = 0.85;

    // Métriques de performance
    public static void printPerformanceThresholds() {
        System.out.println("\n🎯 SEUILS DE PERFORMANCE:");
        System.out.printf("  Accuracy:  > %.1f%%\n", GOOD_ACCURACY);
        System.out.printf("  Precision: > %.2f\n", GOOD_PRECISION);
        System.out.printf("  Recall:    > %.2f\n", GOOD_RECALL);
    }
}