package com.ensah.qoe.ML;

import weka.core.*;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

public class DataPreparationAnomalie {

    // ===========================
    // 1) Charger CSV depuis resources
    // ===========================
    public static Instances loadFromResources(String resourcePath) throws Exception {
        InputStream input = DataPreparationAnomalie.class.getResourceAsStream(resourcePath);

        if (input == null) {
            throw new Exception("❌ Fichier introuvable dans resources : " + resourcePath);
        }

        CSVLoader loader = new CSVLoader();
        loader.setSource(input);
        Instances data = loader.getDataSet();

        // Déterminer l'index de la colonne "anomalie"
        Attribute anomalyAttr = data.attribute("anomalie");

        if (anomalyAttr == null) {
            throw new Exception("⚠ Colonne 'anomalie' introuvable dans le dataset");
        }

        // Si la colonne est numérique, la convertir en nominal
        if (anomalyAttr.isNumeric()) {
            System.out.println("⚠ Conversion du label 'anomalie' -> nominal");

            // Créer un attribut nominal
            ArrayList<String> labels = new ArrayList<>();
            labels.add("0");  // pas d'anomalie
            labels.add("1");  // anomalie

            Attribute nominalAttr = new Attribute("anomalie_nominal", labels);

            // Ajouter le nouvel attribut
            data.insertAttributeAt(nominalAttr, data.numAttributes());

            // Copier et convertir les valeurs
            for (int i = 0; i < data.numInstances(); i++) {
                double value = data.instance(i).value(anomalyAttr.index());
                String labelValue = (value >= 0.5) ? "1" : "0";
                data.instance(i).setValue(data.numAttributes() - 1, labelValue);
            }

            // Supprimer l'ancien attribut numérique
            data.deleteAttributeAt(anomalyAttr.index());

            // Définir la classe (dernier attribut)
            data.setClassIndex(data.numAttributes() - 1);
        } else {
            // Si déjà nominal, définir comme classe
            data.setClassIndex(anomalyAttr.index());
        }

        return data;
    }

    // ===========================
    // 2) Charger CSV depuis fichier
    // ===========================
    public static Instances loadFromFile(String filePath) throws Exception {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new Exception("❌ Fichier introuvable : " + filePath);
        }

        CSVLoader loader = new CSVLoader();
        loader.setSource(file);
        Instances data = loader.getDataSet();

        // Traitement similaire pour la colonne anomalie
        Attribute anomalyAttr = data.attribute("anomalie");

        if (anomalyAttr == null) {
            throw new Exception("⚠ Colonne 'anomalie' introuvable dans le dataset");
        }

        if (anomalyAttr.isNumeric()) {
            System.out.println("⚠ Conversion du label 'anomalie' -> nominal");

            ArrayList<String> labels = new ArrayList<>();
            labels.add("0");
            labels.add("1");

            Attribute nominalAttr = new Attribute("anomalie_nominal", labels);
            data.insertAttributeAt(nominalAttr, data.numAttributes());

            for (int i = 0; i < data.numInstances(); i++) {
                double value = data.instance(i).value(anomalyAttr.index());
                String labelValue = (value >= 0.5) ? "1" : "0";
                data.instance(i).setValue(data.numAttributes() - 1, labelValue);
            }

            data.deleteAttributeAt(anomalyAttr.index());
            data.setClassIndex(data.numAttributes() - 1);
        } else {
            data.setClassIndex(anomalyAttr.index());
        }

        return data;
    }

    // ===========================
    // 3) Balancing (équilibrage)
    // ===========================
    private static Instances balanceDataset(Instances data) {
        // Vérifier que la classe est nominale
        if (!data.classAttribute().isNominal()) {
            throw new IllegalArgumentException("❌ L'attribut classe doit être nominal pour l'équilibrage");
        }

        // Compter les instances par classe
        int[] classCounts = new int[data.numClasses()];
        for (Instance inst : data) {
            classCounts[(int) inst.classValue()]++;
        }

        System.out.println("📊 Distribution avant équilibrage :");
        for (int i = 0; i < data.numClasses(); i++) {
            System.out.println("   Classe " + i + ": " + classCounts[i] + " instances");
        }

        // Séparer par classe
        ArrayList<Instances> byClass = new ArrayList<>();
        for (int i = 0; i < data.numClasses(); i++) {
            byClass.add(new Instances(data, 0));
        }

        for (Instance inst : data) {
            int classVal = (int) inst.classValue();
            byClass.get(classVal).add(inst);
        }

        // Trouver la taille minimale
        int minSize = Integer.MAX_VALUE;
        for (Instances classInstances : byClass) {
            if (classInstances.numInstances() < minSize) {
                minSize = classInstances.numInstances();
            }
        }

        // Créer le dataset équilibré
        Instances balanced = new Instances(data, 0);
        Random rand = new Random(42);

        for (Instances classInstances : byClass) {
            // Si la classe a plus d'instances que minSize, échantillonner aléatoirement
            if (classInstances.numInstances() > minSize) {
                classInstances.randomize(rand);
                for (int i = 0; i < minSize; i++) {
                    balanced.add(classInstances.instance(i));
                }
            } else {
                // Sinon, prendre toutes les instances
                for (int i = 0; i < classInstances.numInstances(); i++) {
                    balanced.add(classInstances.instance(i));
                }
            }
        }

        // Mélanger le dataset équilibré
        balanced.randomize(new Random(42));

        System.out.println("📊 Distribution après équilibrage : " + balanced.numInstances() + " instances");

        return balanced;
    }

    // ===========================
    // 4) Normalisation
    // ===========================
    // ===========================
// 4) Normalisation - CORRIGÉE
// ===========================
    private static Instances normalize(Instances data) throws Exception {
        System.out.println("📈 Début normalisation...");
        System.out.println("   Nombre d'attributs: " + data.numAttributes());

        // Créer le filtre de normalisation
        Normalize normalizeFilter = new Normalize();

        // Vérifier si un attribut classe est défini
        int classIndex = data.classIndex();
        String range;

        if (classIndex >= 0) {
            System.out.println("   Attribut classe détecté à l'index: " + classIndex);
            System.out.println("   Nom de la classe: " + data.classAttribute().name());

            // Exclure l'attribut classe de la normalisation (1-based indexing)
            StringBuilder rangeBuilder = new StringBuilder();
            for (int i = 1; i <= data.numAttributes(); i++) {
                // +1 car Weka utilise 1-based indexing pour l'option -R
                if (i != classIndex + 1) {
                    if (rangeBuilder.length() > 0) {
                        rangeBuilder.append(",");
                    }
                    rangeBuilder.append(i);
                }
            }
            range = rangeBuilder.toString();
            System.out.println("   Plage de normalisation (sans classe): " + range);
        } else {
            // Pas d'attribut classe défini, normaliser tout
            range = "1-" + data.numAttributes();
            System.out.println("   Pas d'attribut classe - normalisation de tous les attributs");
            System.out.println("   Plage de normalisation: " + range);
        }

        // Définir les options
        String[] options = {"-S", "1.0", "-T", "0.0", "-R", range};
        System.out.println("   Options: " + String.join(" ", options));

        try {
            normalizeFilter.setOptions(options);
            normalizeFilter.setInputFormat(data);

            // Appliquer le filtre
            Instances normalizedData = Filter.useFilter(data, normalizeFilter);

            // Conserver l'index de classe si nécessaire
            if (classIndex >= 0) {
                normalizedData.setClassIndex(classIndex);
            }

            System.out.println("✅ Normalisation terminée avec succès");
            System.out.println("   Instances normalisées: " + normalizedData.numInstances());

            return normalizedData;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la normalisation: " + e.getMessage());

            // En cas d'erreur, essayer une approche simplifiée
            System.out.println("🔄 Tentative avec normalisation simplifiée...");
            return normalizeSimple(data);
        }
    }

    // ===========================
// Méthode de secours simplifiée
// ===========================
    private static Instances normalizeSimple(Instances data) throws Exception {
        System.out.println("🔧 Utilisation de la normalisation simplifiée");

        Normalize normalizeFilter = new Normalize();

        // Option simplifiée - utiliser une plage fixe
        String range;
        if (data.classIndex() >= 0) {
            // Normaliser tous les attributs sauf le dernier (supposé être la classe)
            range = "1-" + (data.numAttributes() - 1);
        } else {
            range = "1-" + data.numAttributes();
        }

        System.out.println("   Plage simplifiée: " + range);
        String[] options = {"-S", "1.0", "-T", "0.0", "-R", range};

        normalizeFilter.setOptions(options);
        normalizeFilter.setInputFormat(data);

        Instances normalizedData = Filter.useFilter(data, normalizeFilter);

        // Conserver l'index de classe
        if (data.classIndex() >= 0) {
            normalizedData.setClassIndex(data.classIndex());
        }

        return normalizedData;
    }

    // ===========================
    // 5) Split train/test
    // ===========================
    private static Instances[] split(Instances data, double ratio) {
        data.randomize(new Random(42));

        // Stratified sampling pour préserver la distribution des classes
        Instances[] split = new Instances[2];
        split[0] = new Instances(data, 0); // Train
        split[1] = new Instances(data, 0); // Test

        for (int i = 0; i < data.numClasses(); i++) {
            Instances classData = new Instances(data, 0);
            for (int j = 0; j < data.numInstances(); j++) {
                if ((int) data.instance(j).classValue() == i) {
                    classData.add(data.instance(j));
                }
            }

            classData.randomize(new Random(42));
            int trainSize = (int) (classData.numInstances() * ratio);

            for (int j = 0; j < classData.numInstances(); j++) {
                if (j < trainSize) {
                    split[0].add(classData.instance(j));
                } else {
                    split[1].add(classData.instance(j));
                }
            }
        }

        // Mélanger les ensembles
        split[0].randomize(new Random(42));
        split[1].randomize(new Random(42));

        return split;
    }

    // ===========================
    // 6) PIPELINE COMPLET - version resources
    // ===========================
    public static Instances[] prepareFromResources(String resourcePath) {
        try {
            System.out.println("\n===== Préparation Dataset Anomalies (Resources) =====");

            // 1. Lecture CSV depuis resources
            Instances data = loadFromResources(resourcePath);
            System.out.println("✔ Loaded : " + data.numInstances() + " instances");
            System.out.println("✔ Attributes : " + data.numAttributes());
            System.out.println("✔ Class : " + data.classAttribute().name());

            // 2. Balancing
            System.out.println("⚖ Équilibrage...");
            Instances balanced = balanceDataset(data);

            // 3. Normalisation
            System.out.println("📈 Normalisation...");
            Instances normalized = normalize(balanced);

            // 4. Split final (70% train, 30% test)
            System.out.println("✂️ Split train/test...");
            Instances[] split = split(normalized, 0.7);

            System.out.println("✅ Préparation terminée :");
            System.out.println("   → Train = " + split[0].numInstances());
            System.out.println("   → Test  = " + split[1].numInstances());

            return split;

        } catch (Exception e) {
            System.err.println("❌ PREPARE ERROR : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ===========================
    // 7) PIPELINE COMPLET - version fichier
    // ===========================
    public static Instances[] prepareFromFile(String filePath) {
        try {
            System.out.println("\n===== Préparation Dataset Anomalies (Fichier) =====");

            // 1. Lecture CSV depuis fichier
            Instances data = loadFromFile(filePath);
            System.out.println("✔ Loaded : " + data.numInstances() + " instances");

            // 2. Balancing
            System.out.println("⚖ Équilibrage...");
            Instances balanced = balanceDataset(data);

            // 3. Normalisation
            System.out.println("📈 Normalisation...");
            Instances normalized = normalize(balanced);

            // 4. Split final
            System.out.println("✂️ Split train/test...");
            Instances[] split = split(normalized, 0.7);

            System.out.println("✅ Préparation terminée :");
            System.out.println("   → Train = " + split[0].numInstances());
            System.out.println("   → Test  = " + split[1].numInstances());

            return split;

        } catch (Exception e) {
            System.err.println("❌ PREPARE ERROR : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ===========================
    // 8) Méthode principale pour tester
    // ===========================
    public static void main(String[] args) {
        try {
            // Test avec le fichier resources
            String resourcePath = "/CSV/prediction_dataset.csv";

            Instances[] result = prepareFromResources(resourcePath);

            if (result != null) {
                System.out.println("\n✅ Pipeline exécuté avec succès !");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}