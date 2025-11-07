package com.ensah.qoe.Services;
import com.ensah.qoe.Models.Qos;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;

import static org.apache.spark.sql.functions.*;


/**
 * Classe QosAnalyzer
 * -------------------
 * Rôle : lire un fichier CSV contenant des mesures réseau (QoS),
 * calculer les indicateurs QoS (latence, jitter, perte, bande passante),
 * estimer le MOS (Mean Opinion Score),
 * et retourner un objet Qos prêt à être inséré dans la base Oracle.
 */
public class QosAnalyzer {

    /**
     * Fonction principale : analyser un fichier CSV pour extraire les métriques QoS.
     * @param csvPath chemin du fichier CSV contenant les données réseau.
     * @return un objet Qos avec les valeurs calculées.
     */
    public static Qos analyserQoS(String csvPath) {

        // -------------------------------
        // 1️⃣ Initialisation de Spark
        // -------------------------------
        SparkSession spark = SparkSession.builder()
                .appName("QoS Analyzer")
                .master("local[*]") // local[*] = exécution sur tous les cœurs disponibles
                .getOrCreate();

        // Lecture du fichier CSV
        Dataset<Row> df = spark.read()
                .option("header", "true")        // Le fichier contient une ligne d'en-tête
                .option("inferSchema", "true")   // Spark devine automatiquement le type (double, int, etc.)
                .csv(csvPath);

        // ---------------------------------------------
        // 2️⃣ Calcul des métriques de base du QoS
        // ---------------------------------------------

        // 💡 LATENCE (ms)
        // C’est le délai moyen aller-retour entre l’envoi et la réception.
        // Formule : moyenne(delay_network_ping)
        Dataset<Row> latenceDF = df.agg(avg("delay_network_ping").alias("latence_moyenne"));
        double latence = latenceDF.first().getDouble(0);

        // 💡 BANDE PASSANTE (Mbps)
        // Moyenne des débits descendant et montant :
        // Formule : (moyenne(DL_throughput_ifstat) + moyenne(UL_throughput_ifstat)) / 2
        Dataset<Row> bpDF = df.agg(
                avg("DL_throughput_ifstat").alias("dl"),
                avg("UL_throughput_ifstat").alias("ul")
        );
        Row bpRow = bpDF.first();
        double bandePassante = (bpRow.getDouble(0) + bpRow.getDouble(1)) / 2;

        // 💡 PERTE DE PAQUETS (%)
        // Si "service_status" = false → paquet perdu.
        // Formule : (nb_paquets_perdus / nb_total_paquets) × 100
        long total = df.count();
        long lost = df.filter(col("service_status").equalTo(false)).count();
        double perte = (double) lost / total * 100;

        // 💡 SIGNAL SCORE (moyenne des indicateurs radio)
        // Utilise RSRQ et SINR comme indicateurs de la qualité du signal :
        // Formule : (RSRQ + SINR) / 2
        Dataset<Row> signalDF = df.agg(
                avg("RSRQ").alias("rsrq"),
                avg("SINR").alias("sinr")
        );
        Row sRow = signalDF.first();
        double signalScore = (sRow.getDouble(0) + sRow.getDouble(1)) / 2;

        // 💡 JITTER (ms)
        // Variation du délai entre deux paquets consécutifs.
        // Formule : moyenne(|delay_i+1 - delay_i|)
        WindowSpec w = Window.orderBy("timestamp");
        Dataset<Row> jitterDF = df
                .withColumn("prev_delay", lag("delay_network_ping", 1).over(w))
                .withColumn("jitter", abs(col("delay_network_ping").minus(col("prev_delay"))));
        Row jRow = jitterDF.agg(avg("jitter").alias("jitter_moyen")).first();
        double jitter = jRow.getDouble(0);

        // ---------------------------------------------
        // 3️⃣ Calcul du MOS (Mean Opinion Score)
        // ---------------------------------------------
        // Le MOS traduit la qualité perçue par l'utilisateur (QoE)
        // à partir des mesures techniques QoS.

        // 🧮 Formule simplifiée adaptée à ton projet :
        // MOS = 5 - 0.1 × (latence / 100) - 0.2 × jitter - 2 × (perte / 100)
        // puis bornage entre 1 et 5.
        double mos = 5 - 0.1 * (latence / 100) - 0.2 * jitter - 2 * (perte / 100);
        if (mos > 5) mos = 5;
        if (mos < 1) mos = 1;

        // ---------------------------------------------
        // 4️⃣ Retour de l’objet Qos
        // ---------------------------------------------
        Qos qos = new Qos(latence, jitter, perte, bandePassante, signalScore, mos);

        // Affichage console pour vérification
        System.out.println("Résultats QoS calculés : " + qos);

        // Fermeture de Spark
        spark.close();

        return qos;
    }
}

