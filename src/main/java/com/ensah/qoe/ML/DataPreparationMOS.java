package com.ensah.qoe.ML;

import weka.core.*;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Remove;

import java.io.InputStream;
import java.util.Random;

public class DataPreparationMOS {

    // 🔒 Filtre partagé (TRAIN → TEST → PREDICTION)
    private static Normalize sharedNormalizeFilter;

    // ======================================================
    // 1️⃣ Chargement du CSV - CORRIGÉ
    // ======================================================
    public static Instances loadFromResources(String resourcePath) throws Exception {

        InputStream input = DataPreparationMOS.class.getResourceAsStream(resourcePath);
        if (input == null) {
            throw new Exception("❌ Fichier introuvable : " + resourcePath);
        }

        CSVLoader loader = new CSVLoader();
        loader.setSource(input);
        Instances data = loader.getDataSet();

        System.out.println("✔ CSV MOS chargé : " + data.numInstances() + " lignes");
        System.out.println("📊 Attributs initiaux : " + data.numAttributes());

        // Afficher tous les attributs
        for (int i = 0; i < data.numAttributes(); i++) {
            System.out.println("  " + i + " -> " + data.attribute(i).name() +
                    " (Type: " + data.attribute(i).type() + ")");
        }

        return data;
    }

    // ======================================================
    // 2️⃣ FORCER "mos" comme classe - CORRIGÉ
    // ======================================================
    private static Instances setMOSAsClass(Instances data) throws Exception {
        // Chercher "mos" (exact ou variations)
        Attribute mosAttr = data.attribute("mos");
        if (mosAttr == null) {
            // Chercher variations
            for (int i = 0; i < data.numAttributes(); i++) {
                String attrName = data.attribute(i).name().toLowerCase();
                if (attrName.contains("mos") || attrName.contains("mean_opinion") ||
                        attrName.contains("quality_score")) {
                    mosAttr = data.attribute(i);
                    break;
                }
            }
        }

        if (mosAttr == null) {
            throw new Exception("❌ Colonne 'mos' introuvable dans le dataset");
        }

        // Vérifier que c'est numérique
        if (!mosAttr.isNumeric()) {
            throw new Exception("❌ L'attribut MOS doit être numérique, mais est: " +
                    mosAttr.typeToString(mosAttr.type()));
        }

        // Définir comme classe
        data.setClass(mosAttr);
        System.out.println("🎯 Classe définie : " + mosAttr.name() +
                " (index: " + data.classIndex() + ", Type: Numérique)");

        return data;
    }

    // ======================================================
    // 3️⃣ Suppression des attributs non pertinents - CORRIGÉ
    // ======================================================
    private static Instances removeIrrelevantAttributes(Instances data) throws Exception {
        // Liste des attributs à CONSERVER
        StringBuilder keepIndices = new StringBuilder();

        for (int i = 0; i < data.numAttributes(); i++) {
            Attribute attr = data.attribute(i);
            String attrName = attr.name().toLowerCase();

            // CONSERVER :
            boolean keep =
                    // 1. L'attribut MOS (classe)
                    i == data.classIndex() ||
                            // 2. Les caractéristiques réseau
                            attrName.contains("latence") ||
                            attrName.contains("jitter") ||
                            attrName.contains("loss") ||
                            attrName.contains("bande") ||
                            attrName.contains("signal") ||
                            attrName.contains("score") ||
                            // 3. Caractéristiques audio si présentes
                            attrName.contains("spectral") ||
                            attrName.contains("centroid") ||
                            attrName.contains("rms") ||
                            attrName.contains("zcr") ||
                            attrName.contains("snr") ||
                            attrName.contains("noise") ||
                            attrName.contains("distortion");

            if (keep) {
                if (keepIndices.length() > 0) keepIndices.append(",");
                keepIndices.append(i + 1); // Weka 1-based
            }
        }

        if (keepIndices.length() > 0) {
            Remove remove = new Remove();
            remove.setAttributeIndices(keepIndices.toString());
            remove.setInvertSelection(true); // Garder seulement ces indices
            remove.setInputFormat(data);
            data = Filter.useFilter(data, remove);

            System.out.println("🗑️ Filtrage des attributs...");
            System.out.println("✅ Attributs conservés : " + data.numAttributes());
        }

        return data;
    }

    // ======================================================
    // 4️⃣ Nettoyage des données MOS - CORRIGÉ
    // ======================================================
    private static Instances cleanMOSData(Instances data) {
        System.out.println("🧹 Nettoyage des données MOS...");

        // 1. Suppression instances avec MOS manquant
        int initialCount = data.numInstances();
        for (int i = data.numInstances() - 1; i >= 0; i--) {
            if (data.instance(i).isMissing(data.classIndex())) {
                data.delete(i);
            }
        }
        System.out.println("  • Instances supprimées (MOS manquant) : " +
                (initialCount - data.numInstances()));

        // 2. Vérification plage MOS
        double minMOS = Double.MAX_VALUE;
        double maxMOS = Double.MIN_VALUE;

        for (int i = 0; i < data.numInstances(); i++) {
            double mos = data.instance(i).classValue();
            if (mos < minMOS) minMOS = mos;
            if (mos > maxMOS) maxMOS = mos;
        }

        System.out.println("  • Plage MOS détectée : " +
                String.format("%.2f", minMOS) + " - " +
                String.format("%.2f", maxMOS));

        // 3. Normalisation entre 1-5 si nécessaire
        if (minMOS < 1.0 || maxMOS > 5.0 || maxMOS - minMOS > 4.0) {
            System.out.println("  ⚠️ Normalisation de l'échelle MOS (1-5)...");
            for (int i = 0; i < data.numInstances(); i++) {
                double mos = data.instance(i).classValue();
                double normalizedMOS;

                if (maxMOS - minMOS > 0) {
                    normalizedMOS = 1.0 + ((mos - minMOS) * 4.0 / (maxMOS - minMOS));
                } else {
                    normalizedMOS = 3.0; // Valeur moyenne si pas de variance
                }

                // Limiter entre 1.0 et 5.0
                normalizedMOS = Math.max(1.0, Math.min(5.0, normalizedMOS));
                data.instance(i).setClassValue(normalizedMOS);
            }

            // Recalculer après normalisation
            minMOS = 5.0; maxMOS = 1.0;
            for (int i = 0; i < Math.min(10, data.numInstances()); i++) {
                double mos = data.instance(i).classValue();
                if (mos < minMOS) minMOS = mos;
                if (mos > maxMOS) maxMOS = mos;
            }
            System.out.println("  • Plage MOS après normalisation : " +
                    String.format("%.2f", minMOS) + " - " +
                    String.format("%.2f", maxMOS));
        }

        return data;
    }

    // ======================================================
    // 5️⃣ Split TRAIN / TEST (AVANT normalisation)
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
    // 6️⃣ Normalisation (SANS la classe) - CORRIGÉ
    // ======================================================
    private static Instances normalizeTrain(Instances train) throws Exception {

        // 🔑 IMPORTANT : classIndex doit être défini
        if (train.classIndex() < 0) {
            train.setClassIndex(train.numAttributes() - 1);
        }

        sharedNormalizeFilter = new Normalize();
        sharedNormalizeFilter.setIgnoreClass(true); // NE PAS normaliser la classe
        sharedNormalizeFilter.setInputFormat(train);

        Instances normTrain = Filter.useFilter(train, sharedNormalizeFilter);
        normTrain.setClassIndex(train.classIndex());

        return normTrain;
    }

    private static Instances normalizeTest(Instances test) throws Exception {

        if (test.classIndex() < 0) {
            test.setClassIndex(test.numAttributes() - 1);
        }

        Instances normTest = Filter.useFilter(test, sharedNormalizeFilter);
        normTest.setClassIndex(test.classIndex());

        return normTest;
    }

    // ======================================================
    // 7️⃣ PIPELINE COMPLET - CORRIGÉ
    // ======================================================
    public static Instances[] prepareFromResources(String path) {

        try {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("   PREPARATION DATASET MOS   ");
            System.out.println("=".repeat(50));

            // 1. Chargement brut
            Instances data = loadFromResources(path);

            // 2. FORCER "mos" comme classe
            data = setMOSAsClass(data);

            // 3. Suppression attributs non pertinents
            data = removeIrrelevantAttributes(data);

            // Vérification finale
            System.out.println("\n📋 Structure finale avant split:");
            System.out.println("• Total instances : " + data.numInstances());
            System.out.println("• Total attributs : " + data.numAttributes());
            System.out.println("• Classe          : " + data.classAttribute().name() +
                    " (index: " + data.classIndex() + ")");
            System.out.println("• Type classe     : " +
                    (data.classAttribute().isNumeric() ? "Numérique ✅" : "Nominal ❌"));

            // Afficher les attributs
            System.out.println("• Attributs (" + (data.numAttributes() - 1) + " features):");
            for (int i = 0; i < data.numAttributes(); i++) {
                if (i == data.classIndex()) {
                    System.out.println("  [" + i + "] " + data.attribute(i).name() + " ⭐ (CLASSE)");
                } else {
                    System.out.println("  " + i + " -> " + data.attribute(i).name());
                }
            }

            // 4. Nettoyage
            data = cleanMOSData(data);

            // 5. Split
            Instances[] split = split(data, 0.8);

            // 6. Normalisation
            Instances train = normalizeTrain(split[0]);
            Instances test  = normalizeTest(split[1]);

            System.out.println("\n" + "-".repeat(50));
            System.out.println("✅ PRÉPARATION TERMINÉE");
            System.out.println("-".repeat(50));
            System.out.println("✔ Train = " + train.numInstances() + " instances");
            System.out.println("✔ Test  = " + test.numInstances() + " instances");
            System.out.println("✔ Features = " + (train.numAttributes() - 1));
            System.out.println("✔ Classe = " + train.classAttribute().name() +
                    " (Numérique: " + train.classAttribute().isNumeric() + ")");

            // Afficher échantillon
            System.out.println("\n📋 Échantillon MOS (5 premières):");
            for (int i = 0; i < Math.min(5, train.numInstances()); i++) {
                System.out.println("  Instance " + (i+1) + ": " +
                        train.instance(i).toString(train.classIndex()));
            }

            return new Instances[]{train, test};

        } catch (Exception e) {
            System.err.println("❌ ERREUR dans DataPreparationMOS:");
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   Cause: " + e.getCause());
            e.printStackTrace();
            return null;
        }
    }

    // ======================================================
    // 8️⃣ Accès filtre (PRÉDICTION)
    // ======================================================
    public static Normalize getNormalizeFilter() {
        return sharedNormalizeFilter;
    }
}