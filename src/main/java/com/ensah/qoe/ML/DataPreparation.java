package com.ensah.qoe.ML;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.RemoveUseless;

import java.io.File;
import java.sql.*;
import java.util.Random;

public class DataPreparation {

    /**
     * Charger les données depuis Oracle
     */
    public static Instances loadDataFromDatabase() {
        System.out.println("[DB] Creation nouvelle connexion...");

        String sql = """
            SELECT 
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

        try (Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("[OK] Connexion reussie a la base Oracle !");

            // Créer la structure ARFF
            weka.core.FastVector attributes = new weka.core.FastVector();
            attributes.addElement(new weka.core.Attribute("latence"));
            attributes.addElement(new weka.core.Attribute("jitter"));
            attributes.addElement(new weka.core.Attribute("perte"));
            attributes.addElement(new weka.core.Attribute("bande_passante"));
            attributes.addElement(new weka.core.Attribute("signal_score"));
            attributes.addElement(new weka.core.Attribute("mos"));

            Instances data = new Instances("QoS_QoE_Data", attributes, 0);
            data.setClassIndex(data.numAttributes() - 1); // MOS = cible

            // Charger les données
            int count = 0;
            while (rs.next()) {
                double[] values = new double[6];
                values[0] = rs.getDouble("LATENCE");
                values[1] = rs.getDouble("JITTER");
                values[2] = rs.getDouble("PERTE");
                values[3] = rs.getDouble("BANDE_PASSANTE");
                values[4] = rs.getDouble("SIGNAL_SCORE");
                values[5] = rs.getDouble("MOS");

                data.add(new weka.core.DenseInstance(1.0, values));
                count++;
            }

            System.out.println("[OK] Donnees chargees : " + count + " instances valides");
            return data;

        } catch (SQLException e) {
            System.err.println("[ERREUR] Chargement donnees : " + e.getMessage());
            e.printStackTrace();
            return new Instances("Empty", new weka.core.FastVector(), 0);
        }
    }

    /**
     * Afficher les statistiques du dataset - VERSION SIMPLE
     */
    public static void printDatasetStats(Instances data) {
        System.out.println("Instances: " + data.numInstances() + ", Attributs: " + data.numAttributes());
    }

    /**
     * Statistiques descriptives - VERSION SIMPLE
     */
    public static void printDescriptiveStats(Instances data) {
        // Simplifié - pas d'affichage détaillé
    }

    /**
     * Nettoyer les données
     */
    public static Instances cleanData(Instances data) {
        try {
            // Supprimer les attributs inutiles (variance nulle)
            RemoveUseless remove = new RemoveUseless();
            remove.setInputFormat(data);
            Instances cleaned = Filter.useFilter(data, remove);

            System.out.println("[OK] Nettoyage termine");
            return cleaned;

        } catch (Exception e) {
            System.err.println("[ERREUR] Nettoyage donnees: " + e.getMessage());
            return data;
        }
    }

    /**
     * Diviser en train/test
     */
    public static Instances[] splitData(Instances data, double trainRatio) {
        int trainSize = (int) Math.round(data.numInstances() * trainRatio);
        int testSize = data.numInstances() - trainSize;

        data.randomize(new Random(1));

        Instances train = new Instances(data, 0, trainSize);
        Instances test = new Instances(data, trainSize, testSize);

        System.out.println("[OK] Division : " + trainSize + " train, " + testSize + " test");

        return new Instances[]{train, test};
    }

    /**
     * Normaliser les données (0-1) - OPTIONNEL
     */
    public static Instances normalizeData(Instances data) {
        try {
            Normalize normalize = new Normalize();
            normalize.setInputFormat(data);
            Instances normalized = Filter.useFilter(data, normalize);

            System.out.println("[OK] Normalisation terminee");
            return normalized;

        } catch (Exception e) {
            System.err.println("[ERREUR] Normalisation: " + e.getMessage());
            e.printStackTrace();
            return data;
        }
    }

    /**
     * Sauvegarder le dataset
     */
    public static void saveDataset(Instances data, String filename) {
        try {
            ArffSaver saver = new ArffSaver();
            saver.setInstances(data);
            saver.setFile(new File(filename));
            saver.writeBatch();

            System.out.println("[OK] Dataset sauvegarde: " + filename);

        } catch (Exception e) {
            System.err.println("[ERREUR] Sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Charger un dataset depuis un fichier ARFF
     */
    public static Instances loadDataset(String filename) {
        try {
            DataSource source = new DataSource(filename);
            Instances data = source.getDataSet();

            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            System.out.println("[OK] Dataset charge : " + filename);
            return data;

        } catch (Exception e) {
            System.err.println("[ERREUR] Chargement fichier: " + e.getMessage());
            return null;
        }
    }
}