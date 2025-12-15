package com.ensah.qoe.ML;

import weka.core.*;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.io.InputStream;
import java.util.Random;

public class DataPreparationAnomalie {

    // 🔒 Filtre partagé (TRAIN → TEST → PREDICTION)
    private static Normalize sharedNormalizeFilter;

    // ======================================================
    // 1️⃣ Chargement du CSV
    // ======================================================
    public static Instances loadFromResources(String resourcePath) throws Exception {

        InputStream input = DataPreparationAnomalie.class.getResourceAsStream(resourcePath);
        if (input == null) {
            throw new Exception("❌ Fichier introuvable : " + resourcePath);
        }

        CSVLoader loader = new CSVLoader();
        loader.setSource(input);
        Instances data = loader.getDataSet();

        System.out.println("✔ CSV chargé : " + data.numInstances() + " lignes");

        // Classe = anomalie
        Attribute anomalyAttr = data.attribute("anomalie");
        if (anomalyAttr == null) {
            throw new Exception("❌ Colonne 'anomalie' introuvable");
        }
        data.setClass(anomalyAttr);

        // Colonnes inutiles
        if (data.attribute("zone") != null) {
            data.deleteAttributeAt(data.attribute("zone").index());
        }
        if (data.attribute("mos") != null) {
            data.deleteAttributeAt(data.attribute("mos").index());
        }

        data.setClassIndex(data.numAttributes() - 1);

        return data;
    }

    // ======================================================
    // 2️⃣ Équilibrage (undersampling)
    // ======================================================
    private static Instances balance(Instances data) {

        Instances c0 = new Instances(data, 0);
        Instances c1 = new Instances(data, 0);

        for (Instance inst : data) {
            if ((int) inst.classValue() == 0) c0.add(inst);
            else c1.add(inst);
        }

        int min = Math.min(c0.numInstances(), c1.numInstances());
        Random r = new Random(42);

        c0.randomize(r);
        c1.randomize(r);

        Instances balanced = new Instances(data, 0);
        for (int i = 0; i < min; i++) {
            balanced.add(c0.instance(i));
            balanced.add(c1.instance(i));
        }

        balanced.randomize(r);
        balanced.setClassIndex(data.classIndex());

        return balanced;
    }

    // ======================================================
    // 3️⃣ Split TRAIN / TEST (AVANT normalisation)
    // ======================================================
    private static Instances[] split(Instances data, double ratio) {

        data.randomize(new Random(42));

        int trainSize = (int) (data.numInstances() * ratio);

        Instances train = new Instances(data, 0, trainSize);
        Instances test  = new Instances(data, trainSize, data.numInstances() - trainSize);

        train.setClassIndex(data.classIndex());
        test.setClassIndex(data.classIndex());

        return new Instances[]{train, test};
    }

    // ======================================================
    // 4️⃣ Normalisation (SANS la classe)
    // ======================================================
    private static Instances normalizeTrain(Instances train) throws Exception {

        // 🔑 IMPORTANT : classIndex AVANT normalize
        train.setClassIndex(train.numAttributes() - 1);

        sharedNormalizeFilter = new Normalize();
        sharedNormalizeFilter.setInputFormat(train);

        Instances normTrain = Filter.useFilter(train, sharedNormalizeFilter);
        normTrain.setClassIndex(train.classIndex());

        return normTrain;
    }

    private static Instances normalizeTest(Instances test) throws Exception {

        test.setClassIndex(test.numAttributes() - 1);

        Instances normTest = Filter.useFilter(test, sharedNormalizeFilter);
        normTest.setClassIndex(test.classIndex());

        return normTest;
    }

    // ======================================================
    // 5️⃣ PIPELINE COMPLET
    // ======================================================
    public static Instances[] prepareFromResources(String path) {

        try {
            System.out.println("\n===== PREPARATION DATASET ANOMALIES =====");

            Instances data = loadFromResources(path);
            data = balance(data);

            Instances[] split = split(data, 0.7);

            Instances train = normalizeTrain(split[0]);
            Instances test  = normalizeTest(split[1]);

            System.out.println("✔ Train = " + train.numInstances());
            System.out.println("✔ Test  = " + test.numInstances());

            return new Instances[]{train, test};

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ======================================================
    // 6️⃣ Accès filtre (PRÉDICTION)
    // ======================================================
    public static Normalize getNormalizeFilter() {
        return sharedNormalizeFilter;
    }
}
