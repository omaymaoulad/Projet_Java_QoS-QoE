package com.ensah.qoe.ML;

import com.ensah.qoe.Models.DBConnection;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.core.converters.ArffSaver;

import java.sql.*;
import java.util.ArrayList;
import java.io.File;

public class DataPreparation {

    /**
     * Extraire les données depuis MESURES_QOS
     */
    public static Instances loadDataFromDatabase() {
        // Définir les attributs
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("latence"));
        attributes.add(new Attribute("jitter"));
        attributes.add(new Attribute("perte"));
        attributes.add(new Attribute("bande_passante"));
        attributes.add(new Attribute("signal_score"));
        attributes.add(new Attribute("mos")); // Target

        // Créer le dataset
        Instances dataset = new Instances("QoS_QoE_Data", attributes, 0);
        dataset.setClassIndex(dataset.numAttributes() - 1);

        // REQUÊTE CORRECTE - noms complets des colonnes
        String sql = """
            SELECT
                ID_MESURE,
                LATENCE,
                JITTER,
                PERTE,
                BANDE_PASSANTE,
                SIGNAL_SCORE,
                MOS
            FROM MESURES_QOS
            WHERE MOS IS NOT NULL
              AND LATENCE IS NOT NULL
              AND JITTER IS NOT NULL
              AND PERTE IS NOT NULL
              AND BANDE_PASSANTE IS NOT NULL
              AND SIGNAL_SCORE IS NOT NULL
            ORDER BY ID_MESURE
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int count = 0;
            int skipped = 0;

            while (rs.next()) {
                try {
                    // Extraire et valider les valeurs
                    double latence = rs.getDouble("LATENCE");
                    double jitter = rs.getDouble("JITTER");
                    double perte = rs.getDouble("PERTE");
                    double bandePassante = rs.getDouble("BANDE_PASSANTE");
                    double signalScore = rs.getDouble("SIGNAL_SCORE");
                    double mos = rs.getDouble("MOS");

                    // Validation et nettoyage
                    if (perte > 100) {
                        System.out.println("⚠️ Perte > 100% à la ligne " + rs.getInt("ID_MESURE") + ": " + perte);
                        perte = 100.0; // Limiter à 100%
                    }

                    if (mos < 1 || mos > 5) {
                        System.out.println("⚠️ MOS hors limites [" + mos + "] à la ligne " + rs.getInt("ID_MESURE"));
                        mos = Math.max(1.0, Math.min(5.0, mos)); // Normaliser
                    }

                    double[] values = new double[6];
                    values[0] = latence;
                    values[1] = jitter;
                    values[2] = perte;
                    values[3] = bandePassante;
                    values[4] = signalScore;
                    values[5] = mos;

                    dataset.add(new DenseInstance(1.0, values));
                    count++;

                } catch (SQLException e) {
                    skipped++;
                    System.err.println("Erreur ligne " + rs.getInt("ID_MESURE") + ": " + e.getMessage());
                }
            }

            System.out.println("✅ Données chargées : " + count + " instances valides");
            if (skipped > 0) {
                System.out.println("⚠️ " + skipped + " instances ignorées (données invalides)");
            }

            // Vérifier si nous avons assez de données
            if (count == 0) {
                System.out.println("❌ Aucune donnée valide trouvée dans la table MESURES_QOS");
                System.out.println("Vérifiez que:");
                System.out.println("1. La table existe bien");
                System.out.println("2. Les colonnes ont les bonnes noms");
                System.out.println("3. Il y a des données avec MOS non NULL");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL lors du chargement des données: " + e.getMessage());
            System.err.println("SQL: " + sql);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Erreur générale: " + e.getMessage());
            e.printStackTrace();
        }

        return dataset;
    }

    /**
     * Alternative : Charger avec filtrage MOS
     */
    public static Instances loadDataWithFilter(double minMOS, double maxMOS) {
        Instances data = loadDataFromDatabase();
        Instances filtered = new Instances(data, 0);

        for (int i = 0; i < data.numInstances(); i++) {
            double mos = data.instance(i).value(data.classIndex());
            if (mos >= minMOS && mos <= maxMOS) {
                filtered.add(data.instance(i));
            }
        }

        System.out.println("✅ Données filtrées (MOS " + minMOS + "-" + maxMOS + "): "
                + filtered.numInstances() + " instances");
        return filtered;
    }

    /**
     * Normaliser les données
     */
    public static Instances normalizeData(Instances data) {
        try {
            Normalize normalize = new Normalize();
            normalize.setInputFormat(data);
            return Filter.useFilter(data, normalize);
        } catch (Exception e) {
            System.err.println("❌ Erreur normalisation : " + e.getMessage());
            return data;
        }
    }

    /**
     * Standardiser les données (z-score)
     */
    public static Instances standardizeData(Instances data) {
        try {
            weka.filters.unsupervised.attribute.Standardize standardize =
                    new weka.filters.unsupervised.attribute.Standardize();
            standardize.setInputFormat(data);
            return Filter.useFilter(data, standardize);
        } catch (Exception e) {
            System.err.println("❌ Erreur standardisation : " + e.getMessage());
            return data;
        }
    }

    /**
     * Diviser les données en training (80%) et test (20%)
     */
    public static Instances[] splitData(Instances data, double trainPercentage) {
        if (trainPercentage <= 0 || trainPercentage >= 1) {
            throw new IllegalArgumentException("trainPercentage doit être entre 0 et 1");
        }

        data.randomize(new java.util.Random());

        int trainSize = (int) Math.round(data.numInstances() * trainPercentage);
        int testSize = data.numInstances() - trainSize;

        Instances train = new Instances(data, 0, trainSize);
        Instances test = new Instances(data, trainSize, testSize);

        System.out.println("✅ Division : " + trainSize + " train, " + testSize + " test");
        return new Instances[]{train, test};
    }

    /**
     * Division stratifiée (maintient la distribution des classes)
     */
    public static Instances[] stratifiedSplit(Instances data, double trainPercentage) {
        data.stratify(10); // Crée 10 folds stratifiés

        Instances train = new Instances(data, 0);
        Instances test = new Instances(data, 0);

        for (int i = 0; i < data.numInstances(); i++) {
            if (i % 10 < trainPercentage * 10) {
                train.add(data.instance(i));
            } else {
                test.add(data.instance(i));
            }
        }

        System.out.println("✅ Division stratifiée : " + train.numInstances()
                + " train, " + test.numInstances() + " test");
        return new Instances[]{train, test};
    }

    /**
     * Sauvegarder le dataset au format ARFF (format Weka)
     */
    public static void saveDataset(Instances data, String filename) {
        try {
            ArffSaver saver = new ArffSaver();
            saver.setInstances(data);
            saver.setFile(new File(filename));
            saver.writeBatch();
            System.out.println("✅ Dataset sauvegardé : " + filename);
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde : " + e.getMessage());
        }
    }

    /**
     * Charger dataset depuis fichier ARFF
     */
    public static Instances loadDataset(String filename) {
        try {
            weka.core.converters.ConverterUtils.DataSource source =
                    new weka.core.converters.ConverterUtils.DataSource(filename);
            Instances data = source.getDataSet();
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }
            System.out.println("✅ Dataset chargé : " + filename);
            return data;
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement : " + e.getMessage());
            return null;
        }
    }

    /**
     * Exporter en CSV
     */
    public static void exportToCSV(Instances data, String filename) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new File(filename))) {
            // Écrire l'en-tête
            for (int i = 0; i < data.numAttributes(); i++) {
                writer.print(data.attribute(i).name());
                if (i < data.numAttributes() - 1) writer.print(",");
            }
            writer.println();

            // Écrire les données
            for (int i = 0; i < data.numInstances(); i++) {
                for (int j = 0; j < data.numAttributes(); j++) {
                    writer.print(data.instance(i).value(j));
                    if (j < data.numAttributes() - 1) writer.print(",");
                }
                writer.println();
            }

            System.out.println("✅ Données exportées en CSV : " + filename);
        } catch (Exception e) {
            System.err.println("❌ Erreur export CSV : " + e.getMessage());
        }
    }

    /**
     * Statistiques du dataset
     */
    public static void printDatasetStats(Instances data) {
        System.out.println("\n📊 STATISTIQUES DU DATASET");
        System.out.println("═══════════════════════════════");
        System.out.println("Nom du dataset : " + data.relationName());
        System.out.println("Nombre d'instances : " + data.numInstances());
        System.out.println("Nombre d'attributs : " + data.numAttributes());
        System.out.println("Attribut cible : " + data.classAttribute().name());

        System.out.println("\n📈 Distribution des classes :");
        double[] classCounts = new double[data.numClasses()];
        for (int i = 0; i < data.numInstances(); i++) {
            classCounts[(int)data.instance(i).classValue()]++;
        }

        for (int i = 0; i < data.numClasses(); i++) {
            double percentage = (classCounts[i] / data.numInstances()) * 100;
            System.out.printf("  %s: %d instances (%.1f%%)\n",
                    data.classAttribute().value(i), (int)classCounts[i], percentage);
        }

        System.out.println("\n🔧 Attributs :");
        for (int i = 0; i < data.numAttributes(); i++) {
            System.out.printf("  %d. %s (%s)\n",
                    i + 1,
                    data.attribute(i).name(),
                    data.attribute(i).isNumeric() ? "Numérique" : "Nominal");
        }
        System.out.println("═══════════════════════════════\n");
    }

    /**
     * Statistiques descriptives détaillées
     */
    public static void printDescriptiveStats(Instances data) {
        System.out.println("\n📊 STATISTIQUES DESCRIPTIVES");
        System.out.println("══════════════════════════════════════");

        for (int i = 0; i < data.numAttributes(); i++) {
            if (data.attribute(i).isNumeric()) {
                double[] values = new double[data.numInstances()];
                for (int j = 0; j < data.numInstances(); j++) {
                    values[j] = data.instance(j).value(i);
                }

                double min = values[0];
                double max = values[0];
                double sum = 0;

                for (double val : values) {
                    if (val < min) min = val;
                    if (val > max) max = val;
                    sum += val;
                }

                double mean = sum / values.length;

                // Calcul de l'écart-type
                double variance = 0;
                for (double val : values) {
                    variance += Math.pow(val - mean, 2);
                }
                variance /= values.length;
                double stdDev = Math.sqrt(variance);

                System.out.printf("%s:\n", data.attribute(i).name());
                System.out.printf("  Min: %.4f, Max: %.4f, Moyenne: %.4f, Écart-type: %.4f\n",
                        min, max, mean, stdDev);
                System.out.printf("  Plage: [%.4f, %.4f]\n\n", min, max);
            }
        }
        System.out.println("══════════════════════════════════════\n");
    }

    /**
     * Nettoyer les données (supprimer valeurs manquantes)
     */
    public static Instances cleanData(Instances data) {
        Instances cleaned = new Instances(data, 0);
        int removed = 0;

        for (int i = 0; i < data.numInstances(); i++) {
            boolean hasMissing = false;
            for (int j = 0; j < data.numAttributes(); j++) {
                if (data.instance(i).isMissing(j)) {
                    hasMissing = true;
                    break;
                }
            }

            if (!hasMissing) {
                cleaned.add(data.instance(i));
            } else {
                removed++;
            }
        }

        if (removed > 0) {
            System.out.println("⚠️ " + removed + " instances avec valeurs manquantes supprimées");
        }

        return cleaned;
    }

    /**
     * Imputer les valeurs manquantes avec la moyenne
     */
    public static Instances imputeMissingValues(Instances data) {
        try {
            weka.filters.unsupervised.attribute.ReplaceMissingValues imputer =
                    new weka.filters.unsupervised.attribute.ReplaceMissingValues();
            imputer.setInputFormat(data);
            return Filter.useFilter(data, imputer);
        } catch (Exception e) {
            System.err.println("❌ Erreur imputation : " + e.getMessage());
            return data;
        }
    }

    /**
     * Méthode principale pour tester
     */
    public static void main(String[] args) {
        try {
            System.out.println("🔧 Début préparation données...");

            // 1. Charger les données
            Instances data = loadDataFromDatabase();

            if (data.numInstances() == 0) {
                System.out.println("❌ Aucune donnée chargée. Vérifiez la base de données.");
                return;
            }

            // 2. Afficher les statistiques
            printDatasetStats(data);
            printDescriptiveStats(data);

            // 3. Nettoyer les données
            data = cleanData(data);
            System.out.println("✅ Données nettoyées : " + data.numInstances() + " instances");

            // 4. Normaliser
            Instances normalized = normalizeData(data);

            // 5. Diviser les données
            Instances[] split = splitData(normalized, 0.8);
            Instances train = split[0];
            Instances test = split[1];

            // 6. Sauvegarder
            saveDataset(train, "qos_train.arff");
            saveDataset(test, "qos_test.arff");

            // 7. Exporter en CSV
            exportToCSV(data, "qos_data.csv");

            System.out.println("✅ Préparation terminée avec succès!");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la préparation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}