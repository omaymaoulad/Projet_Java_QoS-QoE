package com.ensah.qoe.Services;

import com.ensah.qoe.Models.QoE;
import com.ensah.qoe.Models.Qos;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class QoeAnalyzer {

    /**
     * Analyse un fichier CSV contenant les données QoE
     * Format attendu: Satisfaction,VideoQuality,AudioQuality,Interactivity,Reliability,Buffering,LoadingTime,FailureRate,ServiceType,DeviceType,UserId
     */
    public static QoE analyserQoE(String cheminFichier) {
        System.out.println("📊 Début de l'analyse QoE : " + cheminFichier);

        List<Double> satisfactionList = new ArrayList<>();
        List<Double> videoQualityList = new ArrayList<>();
        List<Double> audioQualityList = new ArrayList<>();
        List<Double> interactivityList = new ArrayList<>();
        List<Double> reliabilityList = new ArrayList<>();
        List<Double> bufferingList = new ArrayList<>();
        List<Double> loadingTimeList = new ArrayList<>();
        List<Double> failureRateList = new ArrayList<>();

        String lastServiceType = "Video";
        String lastDeviceType = "Mobile";
        int lastUserId = 1;

        try (BufferedReader br = new BufferedReader(new FileReader(cheminFichier))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    System.out.println("✅ En-tête: " + line);
                    continue;
                }

                String[] values = line.split(",");
                if (values.length < 11) {
                    System.out.println("⚠️ Ligne ignorée (format invalide): " + line);
                    continue;
                }

                try {
                    satisfactionList.add(Double.parseDouble(values[0].trim()));
                    videoQualityList.add(Double.parseDouble(values[1].trim()));
                    audioQualityList.add(Double.parseDouble(values[2].trim()));
                    interactivityList.add(Double.parseDouble(values[3].trim()));
                    reliabilityList.add(Double.parseDouble(values[4].trim()));
                    bufferingList.add(Double.parseDouble(values[5].trim()));
                    loadingTimeList.add(Double.parseDouble(values[6].trim()));
                    failureRateList.add(Double.parseDouble(values[7].trim()));

                    lastServiceType = values[8].trim();
                    lastDeviceType = values[9].trim();
                    lastUserId = Integer.parseInt(values[10].trim());

                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Erreur de parsing pour la ligne: " + line);
                }
            }

            if (satisfactionList.isEmpty()) {
                System.out.println("❌ Aucune donnée valide trouvée");
                return null;
            }

            // Utilisation de la méthode de calcul de QoSAnalyzer
            double avgSatisfaction = QosAnalyzer.moyenne(satisfactionList);
            double avgVideoQuality = QosAnalyzer.moyenne(videoQualityList);
            double avgAudioQuality = QosAnalyzer.moyenne(audioQualityList);
            double avgInteractivity = QosAnalyzer.moyenne(interactivityList);
            double avgReliability = QosAnalyzer.moyenne(reliabilityList);
            double avgBuffering = QosAnalyzer.moyenne(bufferingList);
            double avgLoadingTime = QosAnalyzer.moyenne(loadingTimeList);
            double avgFailureRate = QosAnalyzer.moyenne(failureRateList);

            // Calcul du streaming quality (basé sur buffering et loading time)
            double streamingQuality = calculerStreamingQuality(avgBuffering, avgLoadingTime);

            // Calcul du QoE global (moyenne pondérée)
            double overallQoe = calculerQoeGlobal(
                    avgSatisfaction, avgVideoQuality, avgAudioQuality,
                    avgInteractivity, avgReliability
            );

            // Timestamp actuel
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            QoE qoe = new QoE(
                    avgSatisfaction, avgVideoQuality, avgAudioQuality,
                    avgInteractivity, avgReliability, overallQoe,
                    avgBuffering, avgLoadingTime, avgFailureRate,
                    streamingQuality, lastServiceType, lastDeviceType,
                    lastUserId, timestamp
            );

            System.out.println("✅ Analyse QoE terminée avec succès");
            System.out.println("   - QoE Global: " + String.format("%.2f", overallQoe));
            System.out.println("   - Satisfaction: " + String.format("%.2f", avgSatisfaction));
            System.out.println("   - Streaming Quality: " + String.format("%.2f", streamingQuality));

            return qoe;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'analyse: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * NOUVELLE MÉTHODE: Calcule directement le QoE à partir d'un objet Qos déjà analysé
     * Évite la duplication de code et réutilise les données QoS
     */
    public static QoE calculerQoeDepuisQos(Qos qos) {
        if (qos == null) {
            System.err.println("❌ Objet QoS null - impossible de calculer QoE");
            return null;
        }

        System.out.println("📊 Calcul QoE à partir des métriques QoS...");

        // Conversion des métriques QoS en scores QoE (1-5)
        double videoQuality = mapMosToQuality(qos.getMos());
        double audioQuality = mapMosToQuality(qos.getMos());

        // Interactivité basée sur la latence
        double interactivity = mapLatenceToInteractivity(qos.getLatence());

        // Fiabilité basée sur la perte de paquets
        double reliability = mapPerteToReliability(qos.getPerte());

        // Buffering estimé basé sur bande passante
        double buffering = estimerBuffering(qos.getBandePassante());

        // Loading time estimé
        double loadingTime = estimerLoadingTime(qos.getLatence(), qos.getBandePassante());

        // Failure rate basé sur perte
        double failureRate = qos.getPerte();

        // Satisfaction globale (moyenne simple des 4 métriques principales)
        double satisfaction = (videoQuality + audioQuality + interactivity + reliability) / 4.0;

        // Streaming quality
        double streamingQuality = calculerStreamingQuality(buffering, loadingTime);

        // QoE global avec pondération
        double overallQoe = calculerQoeGlobal(satisfaction, videoQuality, audioQuality,
                interactivity, reliability);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Utiliser les informations contextuelles de QoS si disponibles
        String serviceType = (qos.getType_connexion() != null) ? qos.getType_connexion() : "Network";
        String deviceType = "Network Device";

        QoE qoe = new QoE(
                satisfaction, videoQuality, audioQuality, interactivity, reliability,
                overallQoe, buffering, loadingTime, failureRate, streamingQuality,
                serviceType, deviceType, 0, timestamp
        );

        System.out.println("✅ QoE calculé à partir de QoS");
        System.out.println("   - QoE Global: " + String.format("%.2f", overallQoe));
        System.out.println("   - Basé sur Latence: " + String.format("%.2f ms", qos.getLatence()));
        System.out.println("   - Basé sur MOS: " + String.format("%.2f", qos.getMos()));

        return qoe;
    }

    /**
     * MÉTHODE LEGACY: Calcule le QoE à partir de valeurs individuelles
     * Maintenue pour compatibilité ascendante
     */
    public static QoE calculerQoeDepuisQos(double latence, double jitter, double perte,
                                           double bandePassante, double mos) {
        // Créer un objet QoS temporaire pour réutiliser la nouvelle méthode
        Qos qosTemp = new Qos();
        qosTemp.setLatence(latence);
        qosTemp.setJitter(jitter);
        qosTemp.setPerte(perte);
        qosTemp.setBandePassante(bandePassante);
        qosTemp.setMos(mos);
        qosTemp.setType_connexion("Mixed");

        return calculerQoeDepuisQos(qosTemp);
    }

    /**
     * Calcule la qualité de streaming basée sur le buffering et le temps de chargement
     */
    private static double calculerStreamingQuality(double buffering, double loadingTime) {
        // Score de 1 à 5 (5 = excellent, 1 = mauvais)
        double score = 5.0;

        // Pénalité pour buffering (chaque seconde réduit le score)
        score -= (buffering / 2.0);

        // Pénalité pour loading time (chaque seconde réduit le score)
        score -= (loadingTime / 3.0);

        // Limiter entre 1 et 5
        return Math.max(1.0, Math.min(5.0, score));
    }

    /**
     * Calcule le QoE global avec une moyenne pondérée
     * Pondération: Satisfaction (30%), Video (25%), Audio (20%), Interactivity (15%), Reliability (10%)
     */
    private static double calculerQoeGlobal(double satisfaction, double videoQuality,
                                            double audioQuality, double interactivity,
                                            double reliability) {
        return (satisfaction * 0.30) +
                (videoQuality * 0.25) +
                (audioQuality * 0.20) +
                (interactivity * 0.15) +
                (reliability * 0.10);
    }

    // ========== Fonctions de mapping QoS -> QoE ==========

    /**
     * Convertit le MOS (Mean Opinion Score) en score de qualité QoE
     * MOS: 1-5 → QoE Quality: 1-5 (mapping direct)
     */
    private static double mapMosToQuality(double mos) {
        return Math.max(1.0, Math.min(5.0, mos));
    }

    /**
     * Convertit la latence en score d'interactivité
     * Latence basse = meilleure interactivité
     */
    private static double mapLatenceToInteractivity(double latence) {
        if (latence < 50) return 5.0;   // Excellent
        if (latence < 100) return 4.0;  // Bon
        if (latence < 150) return 3.0;  // Moyen
        if (latence < 200) return 2.0;  // Médiocre
        return 1.0;                      // Mauvais
    }

    /**
     * Convertit le taux de perte de paquets en score de fiabilité
     * Perte faible = meilleure fiabilité
     */
    private static double mapPerteToReliability(double perte) {
        if (perte < 1) return 5.0;      // Excellent
        if (perte < 3) return 4.0;      // Bon
        if (perte < 5) return 3.0;      // Moyen
        if (perte < 10) return 2.0;     // Médiocre
        return 1.0;                      // Mauvais
    }

    /**
     * Estime le temps de buffering basé sur la bande passante
     * Bande passante élevée = moins de buffering
     */
    private static double estimerBuffering(double bandePassante) {
        if (bandePassante > 10) return 0.5;  // Presque pas de buffering
        if (bandePassante > 5) return 1.0;   // Buffering minimal
        if (bandePassante > 2) return 2.0;   // Buffering modéré
        return 5.0;                           // Buffering important
    }

    /**
     * Estime le temps de chargement basé sur latence et bande passante
     * Formule: (latence impact) + (bande passante impact)
     */
    private static double estimerLoadingTime(double latence, double bandePassante) {
        // Composante latence (normalisée)
        double latenceImpact = latence / 100.0;

        // Composante bande passante (inversement proportionnelle)
        double bpImpact = (bandePassante > 0) ? (10.0 / bandePassante) : 10.0;

        return latenceImpact + bpImpact;
    }
}