package com.ensah.qoe.ML;

import weka.clusterers.SimpleKMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AnomalyDetector implements Serializable {

    private SimpleKMeans kmeansModel;
    private Standardize normalizer;
    private double anomalyThreshold = 2.5; // Seuil de distance
    private Instances trainingData;

    /**
     * Entraîner le détecteur d'anomalies avec K-Means
     */
    public void train(Instances data) throws Exception {
        System.out.println("🔧 Entraînement du détecteur d'anomalies...");

        // 1. Normaliser les données
        normalizer = new Standardize();
        normalizer.setInputFormat(data);
        Instances normalizedData = Filter.useFilter(data, normalizer);

        // 2. Configurer K-Means
        kmeansModel = new SimpleKMeans();
        kmeansModel.setNumClusters(3); // 3 clusters: Normal, Dégradé, Critique
        kmeansModel.setSeed(42);
        kmeansModel.setPreserveInstancesOrder(true);

        // 3. Entraîner
        kmeansModel.buildClusterer(normalizedData);
        trainingData = normalizedData;

        System.out.println("✅ Modèle d'anomalies entraîné avec " + data.numInstances() + " instances");
        printClusterInfo();
    }

    /**
     * Détecter si une nouvelle mesure est anormale
     */
    public AnomalyResult detectAnomaly(Instance instance) throws Exception {
        // Normaliser l'instance
        normalizer.input(instance);
        normalizer.batchFinished();
        Instance normalized = normalizer.output();

        // Trouver le cluster le plus proche
        int cluster = kmeansModel.clusterInstance(normalized);
        double[] distances = kmeansModel.getClusterCentroids().instance(cluster).toDoubleArray();

        // Calculer la distance au centroïde
        double distance = calculateDistance(normalized, kmeansModel.getClusterCentroids().instance(cluster));

        // Déterminer si c'est une anomalie
        boolean isAnomaly = distance > anomalyThreshold;
        String severity = determineSeverity(distance);
        String type = determineAnomalyType(instance);

        return new AnomalyResult(isAnomaly, severity, distance, cluster, type, instance);
    }

    /**
     * Calculer la distance euclidienne
     */
    private double calculateDistance(Instance a, Instance b) {
        double sum = 0;
        for (int i = 0; i < a.numAttributes() - 1; i++) { // Exclure la classe
            double diff = a.value(i) - b.value(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Déterminer la sévérité de l'anomalie
     */
    private String determineSeverity(double distance) {
        if (distance < 1.5) return "NORMAL";
        if (distance < 2.5) return "WARNING";
        if (distance < 4.0) return "CRITICAL";
        return "SEVERE";
    }

    /**
     * Identifier le type d'anomalie
     */
    private String determineAnomalyType(Instance instance) {
        double latence = instance.value(0);
        double jitter = instance.value(1);
        double perte = instance.value(2);
        double bandePassante = instance.value(3);
        double signalScore = instance.value(4);

        List<String> problems = new ArrayList<>();

        if (latence > 200) problems.add("LATENCE_ELEVEE");
        if (jitter > 50) problems.add("JITTER_ELEVE");
        if (perte > 5) problems.add("PERTE_PAQUETS");
        if (bandePassante < 1000) problems.add("BANDE_PASSANTE_FAIBLE");
        if (signalScore < 2) problems.add("SIGNAL_FAIBLE");

        if (problems.isEmpty()) return "NORMAL";
        return String.join(", ", problems);
    }

    /**
     * Afficher les informations sur les clusters
     */
    private void printClusterInfo() throws Exception {
        System.out.println("\n📊 INFORMATION SUR LES CLUSTERS");
        System.out.println("═══════════════════════════════════");

        Instances centroids = kmeansModel.getClusterCentroids();
        int[] clusterSizes = kmeansModel.getClusterSizes();

        for (int i = 0; i < centroids.numInstances(); i++) {
            System.out.println("Cluster " + i + " (" + clusterSizes[i] + " instances):");
            Instance centroid = centroids.instance(i);

            System.out.printf("  Latence: %.2f ms\n", centroid.value(0));
            System.out.printf("  Jitter: %.2f ms\n", centroid.value(1));
            System.out.printf("  Perte: %.2f%%\n", centroid.value(2));
            System.out.printf("  Bande passante: %.2f Mbps\n", centroid.value(3));
            System.out.printf("  Signal: %.2f\n", centroid.value(4));
            System.out.println();
        }
        System.out.println("═══════════════════════════════════");
    }

    /**
     * Sauvegarder le modèle
     */
    public void saveModel(String filename) throws Exception {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(this);
            System.out.println("✅ Modèle d'anomalies sauvegardé : " + filename);
        }
    }

    /**
     * Charger le modèle
     */
    public static AnomalyDetector loadModel(String filename) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            System.out.println("✅ Modèle d'anomalies chargé : " + filename);
            return (AnomalyDetector) ois.readObject();
        }
    }

    /**
     * Classe pour stocker les résultats de détection
     */
    public static class AnomalyResult {
        public boolean isAnomaly;
        public String severity;
        public double anomalyScore;
        public int cluster;
        public String anomalyType;
        public Instance instance;

        public AnomalyResult(boolean isAnomaly, String severity, double anomalyScore,
                             int cluster, String anomalyType, Instance instance) {
            this.isAnomaly = isAnomaly;
            this.severity = severity;
            this.anomalyScore = anomalyScore;
            this.cluster = cluster;
            this.anomalyType = anomalyType;
            this.instance = instance;
        }

        @Override
        public String toString() {
            return String.format("Anomalie: %s | Sévérité: %s | Score: %.2f | Type: %s",
                    isAnomaly, severity, anomalyScore, anomalyType);
        }
    }
}