package com.ensah.qoe.ML;

import weka.core.Instances;

public class MLTest {
    public static void main(String[] args) {
        System.out.println("🧪 Test du système ML QoE/QoS");
        System.out.println("═══════════════════════════════");

        // Test de chargement des données
        Instances data = DataPreparation.loadDataFromDatabase();
        DataPreparation.printDatasetStats(data);

        // Test de prédiction rapide
        QoEPredictionModels model = new QoEPredictionModels();
        model.trainRandomForest(DataPreparation.splitData(data, 0.8)[0]);

        double mos = model.predictMOS(50, 10, 1.0, 50, 80);
        System.out.println("MOS prédit: " + mos);
        System.out.println("Catégorie: " + model.predictQoECategory(mos));
    }
}