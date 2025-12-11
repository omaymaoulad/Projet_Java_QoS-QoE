package com.ensah.qoe.ML;

import com.ensah.qoe.Models.DBConnection;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;

public class DataPreparationAnomalie {

    // ============================
    //        LOAD DATA
    // ============================
    public static Instances loadData() {
        System.out.println("📥 Chargement dataset anomalies…");

        // Collecte des catégories NOMINALES
        HashSet<String> zones = new HashSet<>();
        HashSet<String> tranches = new HashSet<>();

        String collectSQL = """
            SELECT DISTINCT ZONE, TRANCHE_12H
            FROM MESURES_QOS
            WHERE ANOMALIE IS NOT NULL
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(collectSQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (rs.getString("ZONE") != null) zones.add(rs.getString("ZONE"));
                if (rs.getString("TRANCHE_12H") != null) tranches.add(rs.getString("TRANCHE_12H"));
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur collecte catégories : " + e.getMessage());
        }

        // Convertir HashSet → ArrayList
        ArrayList<String> zoneVals = new ArrayList<>(zones);
        ArrayList<String> trancheVals = new ArrayList<>(tranches);

        // ============================
        //        ATTRIBUTS
        // ============================
        ArrayList<Attribute> attributes = new ArrayList<>();

        attributes.add(new Attribute("latence"));
        attributes.add(new Attribute("jitter"));
        attributes.add(new Attribute("perte"));
        attributes.add(new Attribute("bande_passante"));
        attributes.add(new Attribute("signal_score"));
        attributes.add(new Attribute("mos"));
        attributes.add(new Attribute("heure"));

        // Nominal
        attributes.add(new Attribute("tranche_12h", trancheVals));
        attributes.add(new Attribute("zone", zoneVals));

        // Classe nominale
        ArrayList<String> cls = new ArrayList<>();
        cls.add("0");   // normal
        cls.add("1");   // anomalie
        attributes.add(new Attribute("anomalie", cls));

        Instances data = new Instances("QoS_Anomaly", attributes, 0);
        data.setClassIndex(data.numAttributes() - 1);

        // ============================
        //     CHARGEMENT DB
        // ============================
        String sql = """
            SELECT LATENCE, JITTER, PERTE, BANDE_PASSANTE, SIGNAL_SCORE,
                   MOS, DATE_REELLE, TRANCHE_12H, ZONE, ANOMALIE
            FROM MESURES_QOS
            WHERE ANOMALIE IS NOT NULL
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                double[] values = new double[data.numAttributes()];

                values[0] = rs.getDouble("LATENCE");
                values[1] = rs.getDouble("JITTER");
                values[2] = rs.getDouble("PERTE");
                values[3] = rs.getDouble("BANDE_PASSANTE");
                values[4] = rs.getDouble("SIGNAL_SCORE");
                values[5] = rs.getDouble("MOS");

                Timestamp ts = rs.getTimestamp("DATE_REELLE");
                values[6] = ts == null ? 0 : ts.toLocalDateTime().getHour();

                values[7] = trancheVals.indexOf(rs.getString("TRANCHE_12H"));
                values[8] = zoneVals.indexOf(rs.getString("ZONE"));

                values[9] = rs.getInt("ANOMALIE");

                data.add(new DenseInstance(1.0, values));
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement DB : " + e.getMessage());
        }

        System.out.println("✅ Dataset chargé : " + data.numInstances() + " instances");
        return data;
    }


    // ============================
    //   BALANCER LES CLASSES
    // ============================
    private static Instances balanceDataset(Instances data) {
        System.out.println("⚖️ Équilibrage du dataset…");

        Instances normal = new Instances(data, 0);
        Instances anomalies = new Instances(data, 0);

        for (Instance inst : data) {
            if (inst.stringValue(data.classIndex()).equals("1"))
                anomalies.add(inst);
            else
                normal.add(inst);
        }

        // Si équilibré → ne rien faire
        if (normal.numInstances() == anomalies.numInstances()) return data;

        int diff = Math.abs(normal.numInstances() - anomalies.numInstances());
        Instances balanced = new Instances(data);

        if (normal.numInstances() < anomalies.numInstances()) {
            for (int i = 0; i < diff; i++)
                balanced.add((Instance) normal.instance(i % normal.numInstances()).copy());
        } else {
            for (int i = 0; i < diff; i++)
                balanced.add((Instance) anomalies.instance(i % anomalies.numInstances()).copy());
        }

        System.out.println("➡ Normal : " + normal.numInstances());
        System.out.println("➡ Anomalies : " + anomalies.numInstances());
        System.out.println("➡ Total équilibré : " + balanced.numInstances());

        return balanced;
    }


    // ============================
    //     NORMALISATION
    // ============================
    public static Instances normalize(Instances data) {
        try {
            Normalize norm = new Normalize();
            norm.setInputFormat(data);
            return Filter.useFilter(data, norm);
        } catch (Exception e) {
            System.err.println("❌ Normalisation impossible : " + e.getMessage());
            return data;
        }
    }


    // ============================
    //     TRAIN / TEST SPLIT
    // ============================
    public static Instances[] stratifiedSplit(Instances data, double ratio) {
        data.randomize(new java.util.Random(1));

        int trainSize = (int) Math.round(data.numInstances() * ratio);
        int testSize = data.numInstances() - trainSize;

        Instances train = new Instances(data, 0, trainSize);
        Instances test = new Instances(data, trainSize, testSize);

        return new Instances[]{train, test};
    }


    // ============================
    //       PIPELINE FINAL
    // ============================
    public static Instances[] prepare() {
        Instances data = loadData();

        // Équilibrage ✔
        data = balanceDataset(data);

        // Normalisation ✔
        Instances norm = normalize(data);

        // Split ✔
        return stratifiedSplit(norm, 0.80);
    }
}
