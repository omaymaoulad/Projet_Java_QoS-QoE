package com.ensah.qoe.Services;

import com.ensah.qoe.Models.QoE;
import com.ensah.qoe.Models.Qos;
import com.ensah.qoe.Models.DBConnection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class QoeAnalyzer {

    //                     1) ANALYSE QoE SUBJECTIF depuis CSV

    public static QoE analyserQoE(String csvPath) {

        // 1) Récupérer le dernier QoS pour lier QoE → QoS
        Qos lastQos = getLastQos();
        Integer lastQosId = (lastQos != null) ? lastQos.getId_mesure() : null;

        // Listes de moyennes
        List<Double> bufferingList   = new ArrayList<>();
        List<Double> loadingList     = new ArrayList<>();
        List<Double> videoList       = new ArrayList<>();
        List<Double> audioList       = new ArrayList<>();
        List<Double> interList       = new ArrayList<>();
        List<Double> relList         = new ArrayList<>();
        List<Double> satisfactionList= new ArrayList<>();
        List<Double> failureList     = new ArrayList<>();

        String lastServiceType = "";
        String lastDeviceType  = "";
        int lastUserId         = 1;   // utilisateur connecté

        // ==================== LECTURE CSV =====================
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {

            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {

                if (first) { first = false; continue; }  // ignorer l'en-tête

                String[] v = line.split(",");

                if (v.length < 20) continue;

                // Colonnes importantes
                String internet        = v[7];
                String techSupport     = v[11];
                String streamingTV     = v[12];
                String streamingMovies = v[13];
                String churn           = v[19];

                double tenure = parseDouble(v[4]);
                double price  = parseDouble(v[17]);

                // --- MAPPINGS TELCO ---
                double buffering = mapMonthlyToBuffering(price);
                double loading   = mapInternetToLoading(internet);
                double video     = mapStreamingToQuality(streamingTV, streamingMovies);
                double audio     = video;
                double inter     = mapTenureToInteractivity(tenure);
                double rel       = mapTechSupportToReliability(techSupport);
                double satisfaction = mapSatisfaction(tenure, video);
                double failure      = churn.equals("Yes") ? 5 : 1;

                // Ajouter dans les listes
                bufferingList.add(buffering);
                loadingList.add(loading);
                videoList.add(video);
                audioList.add(audio);
                interList.add(inter);
                relList.add(rel);
                satisfactionList.add(satisfaction);
                failureList.add(failure);

                lastServiceType = internet;
                lastDeviceType  = v[10];
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // ================== MOYENNES SUBJECTIVES ===================
        double avgSatisfaction = moyenne(satisfactionList);
        double avgVideo        = moyenne(videoList);
        double avgAudio        = moyenne(audioList);
        double avgInter        = moyenne(interList);
        double avgReliability  = moyenne(relList);
        double avgBuffering    = moyenne(bufferingList);
        double avgLoading      = moyenne(loadingList);
        double avgFailure      = moyenne(failureList);

        double streamingQuality = 5 - (avgBuffering + avgLoading) / 2;

        // === Calcul QoE Subjectif ===
        double qoeSubjectif =
                avgSatisfaction * 0.30 +
                        avgVideo        * 0.25 +
                        avgAudio        * 0.20 +
                        avgInter        * 0.15 +
                        avgReliability  * 0.10;

        System.out.println("🎯 QoE Subjectif = " + qoeSubjectif);

        // ============================================================================
        //                     2) CALCUL QoE OBJECTIF depuis BD MESURES_QOS
        // ============================================================================
        if (lastQos == null) {
            System.out.println("⚠️ Aucun QoS trouvé en base !");
            return null;
        }

        QoE qoeObjectif = calculerQoeObjectif(lastQos);
        double qoeObj = qoeObjectif.getOverallQoe();

        System.out.println("🎯 QoE Objectif = " + qoeObj);

        // ============================================================================
        //                     3) FUSION SUBJECTIF + OBJECTIF
        // ============================================================================
        double qoeFinal = (qoeSubjectif + qoeObj) / 2;

        // ============================================================================
        //                     4) RETOURNER QoE COMPLET
        // ============================================================================
        return new QoE(
                avgSatisfaction, avgVideo, avgAudio, avgInter, avgReliability,
                qoeFinal, avgBuffering, avgLoading, avgFailure,
                streamingQuality, lastDeviceType,
                lastUserId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                lastQosId
        );
    }

    // ============================================================================
    //                      UTILITAIRES GÉNÉRAUX
    // ============================================================================
    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return 0; }
    }

    public static double moyenne(List<Double> list) {
        return list.stream().mapToDouble(x -> x).average().orElse(0);
    }

    // ============================================================================
    //                     5) MAPPINGS (TELCO CSV)
    // ============================================================================
    private static double mapMonthlyToBuffering(double price) {
        if (price > 90) return 0.5;
        if (price > 70) return 1;
        if (price > 50) return 2.5;
        return 4;
    }

    private static double mapInternetToLoading(String type) {
        if (type.contains("Fiber")) return 1;
        if (type.contains("DSL")) return 2;
        return 4;
    }

    private static double mapStreamingToQuality(String tv, String movies) {
        int score = 2;
        if (tv.equals("Yes")) score++;
        if (movies.equals("Yes")) score++;
        return Math.min(5, score);
    }

    private static double mapTenureToInteractivity(double t) {
        if (t > 50) return 5;
        if (t > 30) return 4;
        if (t > 10) return 3;
        return 2;
    }

    private static double mapTechSupportToReliability(String t) {
        return t.equals("Yes") ? 5 : 2;
    }

    private static double mapSatisfaction(double tenure, double video) {
        return Math.min(5, (tenure / 20) + (video / 2));
    }

    // ============================================================================
    //                  6) CALCUL QoE OBJECTIF depuis QOS (BD Oracle)
    // ============================================================================
    public static QoE calculerQoeObjectif(Qos qos) {

        double video = mapMos(qos.getMos());
        double audio = video;
        double inter = mapLatence(qos.getLatence());
        double rel   = mapPerte(qos.getPerte());
        double buffer= estimerBuffer(qos.getBandePassante());
        double load  = estimerLoading(qos.getLatence(), qos.getBandePassante());
        double fail  = qos.getPerte();

        double satisfaction = (video + audio + inter + rel) / 4;

        double stream = 5 - (buffer + load) / 2;

        double qoe =
                satisfaction * 0.30 +
                        video        * 0.25 +
                        audio        * 0.20 +
                        inter        * 0.15 +
                        rel          * 0.10;

        return new QoE(satisfaction, video, audio, inter, rel, qoe, buffer, load, fail, stream, "Network Device", 1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), qos.getId_mesure());
    }

    // --- MAPPINGS QoS → QoE Objectif ---
    private static double mapMos(double mos) { return Math.max(1, Math.min(5, mos)); }

    private static double mapLatence(double l) {
        if (l < 50)  return 5;
        if (l < 100) return 4;
        if (l < 150) return 3;
        if (l < 200) return 2;
        return 1;
    }

    private static double mapPerte(double p) {
        if (p < 1)  return 5;
        if (p < 3)  return 4;
        if (p < 5)  return 3;
        if (p < 10) return 2;
        return 1;
    }

    private static double estimerBuffer(double bp) {
        if (bp > 10) return 0.5;
        if (bp > 5)  return 1;
        if (bp > 2)  return 2;
        return 5;
    }

    private static double estimerLoading(double lat, double bp) {
        return (lat / 100.0) + (bp > 0 ? (10.0 / bp) : 10);
    }

    // ============================================================================
    //                 7) RÉCUPÉRER DERNIÈRE LIGNE QOS EN BD ORACLE
    // ============================================================================
    public static Qos getLastQos() {

        String sql = "SELECT * FROM MESURES_QOS ORDER BY ID_MESURE DESC FETCH FIRST 1 ROW ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                Qos q = new Qos();
                q.setId_mesure(rs.getInt("ID_MESURE"));
                q.setLatence(rs.getDouble("LATENCE"));
                q.setJitter(rs.getDouble("JITTER"));
                q.setPerte(rs.getDouble("PERTE"));
                q.setBandePassante(rs.getDouble("BANDE_PASSANTE"));
                q.setMos(rs.getDouble("MOS"));
                q.setSignalScore(rs.getDouble("SIGNAL_SCORE"));
                return q;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
