package com.ensah.qoe.Services;

import com.ensah.qoe.Models.QoE;
import com.ensah.qoe.Models.DBConnection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class QoeAnalyzer {

    /**
     * Analyse QoE subjectif depuis CSV et retourne un objet QoE pour insertion
     */
    public static QoE analyserQoE(String csvPath, int clientId, String genre) {

        // Listes de moyennes pour les métriques subjectives
        List<Double> satisfactionList = new ArrayList<>();
        List<Double> serviceList      = new ArrayList<>();
        List<Double> prixList         = new ArrayList<>();
        List<Double> contratList      = new ArrayList<>();
        List<Double> lifetimeList     = new ArrayList<>();

        // Métriques QoS simulées basées sur le CSV
        List<Double> latenceList      = new ArrayList<>();
        List<Double> jitterList       = new ArrayList<>();
        List<Double> perteList        = new ArrayList<>();
        List<Double> bandePassanteList= new ArrayList<>();
        List<Double> mosList          = new ArrayList<>();
        List<Double> signalList       = new ArrayList<>();

        // ==================== LECTURE CSV =====================
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {

            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {

                if (first) { first = false; continue; }  // ignorer l'en-tête

                String[] v = line.split(",");

                if (v.length < 20) continue;

                // Colonnes importantes du CSV Telco
                String internet        = v[7];
                String techSupport     = v[11];
                String streamingTV     = v[12];
                String streamingMovies = v[13];
                String contract        = v[15];
                String paymentMethod   = v[16];
                String churn           = v[19];

                double tenure = parseDouble(v[4]);
                double monthlyCharges = parseDouble(v[17]);
                double totalCharges = parseDouble(v[18]);

                // --- MAPPINGS TELCO vers métriques QOE ---
                double satisfaction = mapSatisfaction(tenure, monthlyCharges, churn);
                double service      = mapServiceQuality(internet, streamingTV, streamingMovies);
                double prix         = mapPrixSatisfaction(monthlyCharges, totalCharges);
                double contrat      = mapContratSatisfaction(contract, paymentMethod);
                double lifetime     = mapLifetimeValue(tenure, totalCharges, churn);

                // Métriques QoS simulées basées sur les données client
                double latence      = mapInternetToLatence(internet);
                double jitter       = mapInternetToJitter(internet);
                double perte        = mapChurnToPerte(churn);
                double bandePassante = mapInternetToBandePassante(internet);
                double mos          = mapStreamingToMos(streamingTV, streamingMovies);
                double signal       = mapTechSupportToSignal(techSupport);

                // Ajouter dans les listes
                satisfactionList.add(satisfaction);
                serviceList.add(service);
                prixList.add(prix);
                contratList.add(contrat);
                lifetimeList.add(lifetime);

                latenceList.add(latence);
                jitterList.add(jitter);
                perteList.add(perte);
                bandePassanteList.add(bandePassante);
                mosList.add(mos);
                signalList.add(signal);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // ================== CALCUL DES MOYENNES ===================
        double avgSatisfaction = moyenne(satisfactionList);
        double avgService      = moyenne(serviceList);
        double avgPrix         = moyenne(prixList);
        double avgContrat      = moyenne(contratList);
        double avgLifetime     = moyenne(lifetimeList);

        double avgLatence      = moyenne(latenceList);
        double avgJitter       = moyenne(jitterList);
        double avgPerte        = moyenne(perteList);
        double avgBandePassante= moyenne(bandePassanteList);
        double avgMos          = moyenne(mosList);
        double avgSignal       = moyenne(signalList);

        // === Calcul QoE Global ===
        double qoeGlobal = calculerQoeGlobal(
                avgSatisfaction, avgService, avgPrix, avgContrat, avgLifetime,
                avgLatence, avgJitter, avgPerte, avgBandePassante, avgMos, avgSignal
        );

        System.out.println("🎯 QoE Global calculé = " + qoeGlobal);

        // ============================================================================
        //                      RETOURNER QoE COMPLET POUR INSERTION
        // ============================================================================
        return new QoE(
                // ID_QOE sera généré automatiquement par la base
                clientId, genre,
                // Métriques QoS
                avgLatence, avgJitter, avgPerte, avgBandePassante, avgMos, avgSignal,
                // Métriques QoE subjectives
                avgSatisfaction, avgService, avgPrix, avgContrat, avgLifetime,
                // Feedback score (moyenne des satisfactions)
                avgSatisfaction,
                // Score final
                qoeGlobal,
                LocalDateTime.now()
        );
    }

    /**
     * Calcule le score QoE global avec pondération
     */
    private static double calculerQoeGlobal(
            double satisfaction, double service, double prix, double contrat, double lifetime,
            double latence, double jitter, double perte, double bandePassante, double mos, double signal) {

        // Pondération: 60% QoE subjectif + 40% QoS objectif
        double qoeSubjectif =
                satisfaction * 0.35 +
                        service      * 0.25 +
                        prix         * 0.15 +
                        contrat      * 0.15 +
                        lifetime     * 0.10;

        double qoeObjectif =
                (5 - mapLatenceToScore(latence))    * 0.25 +
                        (5 - mapJitterToScore(jitter))      * 0.20 +
                        (5 - mapPerteToScore(perte))        * 0.20 +
                        mapBandePassanteToScore(bandePassante) * 0.20 +
                        mos                                * 0.15;

        return (qoeSubjectif * 0.6) + (qoeObjectif * 0.4);
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
    //                     MAPPINGS SUBJECTIFS (TELCO CSV → QoE)
    // ============================================================================

    private static double mapSatisfaction(double tenure, double monthlyCharges, String churn) {
        double baseScore = 3.0;

        // Plus le client est ancien, plus il est satisfait
        baseScore += Math.min(2.0, tenure / 20.0);

        // Prix élevé peut réduire la satisfaction
        if (monthlyCharges > 80) baseScore -= 0.5;
        else if (monthlyCharges < 40) baseScore += 0.5;

        // Churn = insatisfaction
        if ("Yes".equals(churn)) baseScore -= 1.5;

        return Math.max(1.0, Math.min(5.0, baseScore));
    }

    private static double mapServiceQuality(String internet, String streamingTV, String streamingMovies) {
        double score = 3.0;

        // Type d'internet
        if (internet.contains("Fiber")) score += 1.5;
        else if (internet.contains("DSL")) score += 0.5;

        // Services de streaming
        if ("Yes".equals(streamingTV)) score += 0.5;
        if ("Yes".equals(streamingMovies)) score += 0.5;

        return Math.max(1.0, Math.min(5.0, score));
    }

    private static double mapPrixSatisfaction(double monthlyCharges, double totalCharges) {
        double score = 3.0;

        // Prix modéré = meilleure satisfaction
        if (monthlyCharges >= 40 && monthlyCharges <= 70) score += 1.0;
        else if (monthlyCharges > 70) score -= 0.5;

        // Client à forte valeur
        if (totalCharges > 2000) score += 0.5;

        return Math.max(1.0, Math.min(5.0, score));
    }

    private static double mapContratSatisfaction(String contract, String paymentMethod) {
        double score = 3.0;

        // Type de contrat
        if ("Two year".equals(contract)) score += 1.0;
        else if ("One year".equals(contract)) score += 0.5;

        // Méthode de paiement
        if ("Electronic check".equals(paymentMethod)) score -= 0.5;
        else if ("Credit card".equals(paymentMethod)) score += 0.5;

        return Math.max(1.0, Math.min(5.0, score));
    }

    private static double mapLifetimeValue(double tenure, double totalCharges, String churn) {
        double score = 3.0;

        // Ancienneté
        score += Math.min(1.5, tenure / 25.0);

        // Valeur totale
        if (totalCharges > 1500) score += 0.5;

        // Risque de départ
        if ("Yes".equals(churn)) score -= 2.0;

        return Math.max(1.0, Math.min(5.0, score));
    }

    // ============================================================================
    //                     MAPPINGS QOS SIMULÉS
    // ============================================================================

    private static double mapInternetToLatence(String internet) {
        if (internet.contains("Fiber")) return 20.0;
        if (internet.contains("DSL")) return 50.0;
        return 100.0; // Dial-up ou autre
    }

    private static double mapInternetToJitter(String internet) {
        if (internet.contains("Fiber")) return 5.0;
        if (internet.contains("DSL")) return 15.0;
        return 30.0;
    }

    private static double mapChurnToPerte(String churn) {
        return "Yes".equals(churn) ? 8.0 : 2.0;
    }

    private static double mapInternetToBandePassante(String internet) {
        if (internet.contains("Fiber")) return 100.0;
        if (internet.contains("DSL")) return 20.0;
        return 5.0;
    }

    private static double mapStreamingToMos(String streamingTV, String streamingMovies) {
        double baseMos = 3.5;
        if ("Yes".equals(streamingTV)) baseMos += 0.5;
        if ("Yes".equals(streamingMovies)) baseMos += 0.5;
        return Math.min(5.0, baseMos);
    }

    private static double mapTechSupportToSignal(String techSupport) {
        return "Yes".equals(techSupport) ? 80.0 : 60.0;
    }

    // ============================================================================
    //                     MAPPINGS QOS → SCORES
    // ============================================================================

    private static double mapLatenceToScore(double latence) {
        if (latence < 30) return 1;
        if (latence < 60) return 2;
        if (latence < 100) return 3;
        if (latence < 150) return 4;
        return 5;
    }

    private static double mapJitterToScore(double jitter) {
        if (jitter < 10) return 1;
        if (jitter < 20) return 2;
        if (jitter < 30) return 3;
        if (jitter < 40) return 4;
        return 5;
    }

    private static double mapPerteToScore(double perte) {
        if (perte < 2) return 1;
        if (perte < 4) return 2;
        if (perte < 6) return 3;
        if (perte < 8) return 4;
        return 5;
    }

    private static double mapBandePassanteToScore(double bandePassante) {
        if (bandePassante > 50) return 5;
        if (bandePassante > 25) return 4;
        if (bandePassante > 10) return 3;
        if (bandePassante > 5) return 2;
        return 1;
    }

    // ============================================================================
    //                  MÉTHODE D'INSERTION DANS LA TABLE QOE
    // ============================================================================

    /**
     * Insère un objet QoE dans la base de données
     */
    public static boolean insererQoe(QoE qoe) {
        String sql = "INSERT INTO QOE (" +
                "ID_CLIENT, GENRE, " +
                "LATENCE_MOY, JITTER_MOY, PERTE_MOY, BANDE_PASSANTE_MOY, MOS_MOY, SIGNAL_MOY, " +
                "SATISFACTION_QOE, SERVICE_QOE, PRIX_QOE, CONTRAT_QOE, LIFETIME_QOE, " +
                "FEEDBACK_SCORE, QOE_GLOBAL, DATE_CALCULE" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Données client
            stmt.setInt(1, qoe.getIdClient());
            stmt.setString(2, qoe.getGenre());

            // Métriques QoS
            stmt.setDouble(3, qoe.getLatenceMoy());
            stmt.setDouble(4, qoe.getJitterMoy());
            stmt.setDouble(5, qoe.getPerteMoy());
            stmt.setDouble(6, qoe.getBandePassanteMoy());
            stmt.setDouble(7, qoe.getMosMoy());
            stmt.setDouble(8, qoe.getSignalMoy());

            // Métriques QoE subjectives
            stmt.setDouble(9, qoe.getSatisfactionQoe());
            stmt.setDouble(10, qoe.getServiceQoe());
            stmt.setDouble(11, qoe.getPrixQoe());
            stmt.setDouble(12, qoe.getContratQoe());
            stmt.setDouble(13, qoe.getLifetimeQoe());

            // Feedback et score global
            stmt.setDouble(14, qoe.getFeedbackScore());
            stmt.setDouble(15, qoe.getQoeGlobal());

            // Date
            stmt.setTimestamp(16, Timestamp.valueOf(qoe.getDateCalcule()));

            int rowsAffected = stmt.executeUpdate();
            boolean success = rowsAffected > 0;

            if (success) {
                System.out.println("✅ Données QoE insérées avec succès pour le client " + qoe.getIdClient());
            } else {
                System.err.println("❌ Échec de l'insertion des données QoE");
            }

            return success;

        } catch (Exception e) {
            System.err.println("❌ Erreur insertion QoE: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Méthode principale pour analyser et insérer les données QoE
     */
    public static boolean analyserEtInsererQoe(String csvPath, int clientId, String genre) {
        System.out.println("🔄 Début de l'analyse QoE...");

        // 1. Analyser le CSV
        QoE qoe = analyserQoE(csvPath, clientId, genre);

        if (qoe == null) {
            System.err.println("❌ Échec de l'analyse QoE");
            return false;
        }

        // 2. Insérer dans la base
        boolean success = insererQoe(qoe);

        if (success) {
            System.out.println("✅ Analyse et insertion QoE terminées avec succès");
            System.out.println("📊 Score QoE Global: " + qoe.getQoeGlobal());
        } else {
            System.err.println("❌ Échec de l'insertion QoE");
        }

        return success;
    }
}