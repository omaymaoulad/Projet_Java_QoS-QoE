package com.ensah.qoe.Services;

import com.ensah.qoe.Models.Qos;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;

import static org.apache.spark.sql.functions.*;

public class QosAnalyzer {

    public static Qos analyserQoS(String csvPath) {
        // -------------------------------
        // 1️⃣ SparkSession local
        // -------------------------------
        SparkSession spark = SparkSession.builder()
                .appName("QoSAnalyzer")
                .master("local[*]")   // mode local, tous les coeurs disponibles
                .getOrCreate();

        // Lecture du CSV
        Dataset<Row> df = spark.read()
                .option("header", "true")
                .option("inferSchema", "true")
                .csv(csvPath);

        System.out.println("📋 Colonnes : " + String.join(", ", df.columns()));

        // -------------------------------
        // 2️⃣ Calcul des métriques QoS
        // -------------------------------
        double latence = df.agg(avg(col("delay").cast("double")).alias("latence_moyenne"))
                .first().getDouble(0);

        Row bpRow = df.agg(
                avg(col("throughput_downlink").cast("double")).alias("dl"),
                avg(col("throughput_uplink").cast("double")).alias("ul")
        ).first();
        double bandePassante = (bpRow.getDouble(0) + bpRow.getDouble(1)) / 2;

        long total = df.count();
        long lost = df.filter(col("service_status").equalTo(0)).count();
        double perte = total > 0 ? ((double) lost / total * 100) : 0;

        Row sRow = df.agg(
                avg(col("rsrq").cast("double")).alias("rsrq"),
                avg(col("sinr").cast("double")).alias("sinr")
        ).first();
        double signalScore = (sRow.getDouble(0) + sRow.getDouble(1)) / 2;

        WindowSpec w = Window.orderBy("timestamp");
        Row jRow = df.withColumn("prev_delay", lag(col("delay"), 1).over(w))
                .withColumn("jitter", abs(col("delay").minus(col("prev_delay"))))
                .na().fill(0, new String[]{"jitter"})
                .agg(avg("jitter").alias("jitter_moyen"))
                .first();
        double jitter = jRow.getDouble(0);

        // -------------------------------
        // 3️⃣ Calcul du MOS
        // -------------------------------
        double mos = 5 - 0.1 * (latence / 100) - 0.2 * jitter - 2 * (perte / 100);
        mos = Math.max(1, Math.min(5, mos));

        // -------------------------------
        // 4️⃣ Création de l'objet Qos
        // -------------------------------
        Qos qos = new Qos(latence, jitter, perte, bandePassante, signalScore, mos);
        System.out.println("Résultats QoS : " + qos);

        return qos;
    }
}
