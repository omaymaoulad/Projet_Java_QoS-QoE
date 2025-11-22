package com.ensah.qoe.Models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class QoE {
    private int idQoe;
    private int idClient;
    private String genre;

    // Métriques QoS agrégées
    private double latenceMoy;
    private double jitterMoy;
    private double perteMoy;
    private double bandePassanteMoy;
    private double mosMoy;
    private double signalMoy;

    // Métriques QoE subjectives
    private double satisfactionQoe;
    private double serviceQoe;
    private double prixQoe;
    private double contratQoe;
    private double lifetimeQoe;

    // Feedback utilisateur
    private double feedbackScore;

    // Score final
    private double qoeGlobal;
    private LocalDateTime dateCalcule;

    // Champs supplémentaires pour l'interface
    private String serviceType;
    private String deviceType;
    private String timestamp;

    // Constructeurs
    public QoE() {}

    // Constructeur pour l'interface (compatible avec le code existant)
    public QoE(double satisfactionScore, double videoQuality, double audioQuality,
               double interactivity, double reliability, double overallQoe,
               double loadingTime, double buffering, double failureRate, double streamingQuality,
               String serviceType, String deviceType, int idClient, String timestamp, int idQoe) {
        this.satisfactionQoe = satisfactionScore;
        this.serviceQoe = videoQuality;
        this.prixQoe = audioQuality;
        this.contratQoe = interactivity;
        this.lifetimeQoe = reliability;
        this.qoeGlobal = overallQoe;
        this.latenceMoy = loadingTime;
        this.jitterMoy = buffering;
        this.perteMoy = failureRate;
        this.bandePassanteMoy = streamingQuality;
        this.serviceType = serviceType;
        this.deviceType = deviceType;
        this.idClient = idClient;
        this.timestamp = timestamp;
        this.idQoe = idQoe;
    }
    // Ajoutez ce constructeur dans votre classe QoE
    public QoE(int idClient, String genre,
               double latenceMoy, double jitterMoy, double perteMoy, double bandePassanteMoy,
               double mosMoy, double signalMoy, double satisfactionQoe, double serviceQoe,
               double prixQoe, double contratQoe, double lifetimeQoe, double feedbackScore,
               double qoeGlobal, LocalDateTime dateCalcule) {

        this.idClient = idClient;
        this.genre = genre;
        this.latenceMoy = latenceMoy;
        this.jitterMoy = jitterMoy;
        this.perteMoy = perteMoy;
        this.bandePassanteMoy = bandePassanteMoy;
        this.mosMoy = mosMoy;
        this.signalMoy = signalMoy;
        this.satisfactionQoe = satisfactionQoe;
        this.serviceQoe = serviceQoe;
        this.prixQoe = prixQoe;
        this.contratQoe = contratQoe;
        this.lifetimeQoe = lifetimeQoe;
        this.feedbackScore = feedbackScore;
        this.qoeGlobal = qoeGlobal;
        this.dateCalcule = dateCalcule;

        // Valeurs par défaut pour l'interface
        this.serviceType = "Service Générique";
        this.deviceType = "Device Client";
        this.timestamp = dateCalcule.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
    // Constructeur pour la base de données
    public QoE(int idQoe, int idClient, String genre,
               double latenceMoy, double jitterMoy, double perteMoy, double bandePassanteMoy,
               double mosMoy, double signalMoy, double satisfactionQoe, double serviceQoe,
               double prixQoe, double contratQoe, double lifetimeQoe, double feedbackScore,
               double qoeGlobal, LocalDateTime dateCalcule) {
        this.idQoe = idQoe;
        this.idClient = idClient;
        this.genre = genre;
        this.latenceMoy = latenceMoy;
        this.jitterMoy = jitterMoy;
        this.perteMoy = perteMoy;
        this.bandePassanteMoy = bandePassanteMoy;
        this.mosMoy = mosMoy;
        this.signalMoy = signalMoy;
        this.satisfactionQoe = satisfactionQoe;
        this.serviceQoe = serviceQoe;
        this.prixQoe = prixQoe;
        this.contratQoe = contratQoe;
        this.lifetimeQoe = lifetimeQoe;
        this.feedbackScore = feedbackScore;
        this.qoeGlobal = qoeGlobal;
        this.dateCalcule = dateCalcule;

        // Valeurs par défaut pour l'interface
        this.serviceType = "Service Générique";
        this.deviceType = "Device Client";
        this.timestamp = dateCalcule != null ?
                dateCalcule.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) :
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    // Getters et Setters
    public int getIdQoe() { return idQoe; }
    public void setIdQoe(int idQoe) { this.idQoe = idQoe; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public double getLatenceMoy() { return latenceMoy; }
    public void setLatenceMoy(double latenceMoy) { this.latenceMoy = latenceMoy; }

    public double getJitterMoy() { return jitterMoy; }
    public void setJitterMoy(double jitterMoy) { this.jitterMoy = jitterMoy; }

    public double getPerteMoy() { return perteMoy; }
    public void setPerteMoy(double perteMoy) { this.perteMoy = perteMoy; }

    public double getBandePassanteMoy() { return bandePassanteMoy; }
    public void setBandePassanteMoy(double bandePassanteMoy) { this.bandePassanteMoy = bandePassanteMoy; }

    public double getMosMoy() { return mosMoy; }
    public void setMosMoy(double mosMoy) { this.mosMoy = mosMoy; }

    public double getSignalMoy() { return signalMoy; }
    public void setSignalMoy(double signalMoy) { this.signalMoy = signalMoy; }

    public double getSatisfactionQoe() { return satisfactionQoe; }
    public void setSatisfactionQoe(double satisfactionQoe) { this.satisfactionQoe = satisfactionQoe; }

    public double getServiceQoe() { return serviceQoe; }
    public void setServiceQoe(double serviceQoe) { this.serviceQoe = serviceQoe; }

    public double getPrixQoe() { return prixQoe; }
    public void setPrixQoe(double prixQoe) { this.prixQoe = prixQoe; }

    public double getContratQoe() { return contratQoe; }
    public void setContratQoe(double contratQoe) { this.contratQoe = contratQoe; }

    public double getLifetimeQoe() { return lifetimeQoe; }
    public void setLifetimeQoe(double lifetimeQoe) { this.lifetimeQoe = lifetimeQoe; }

    public double getFeedbackScore() { return feedbackScore; }
    public void setFeedbackScore(double feedbackScore) { this.feedbackScore = feedbackScore; }

    public double getQoeGlobal() { return qoeGlobal; }
    public void setQoeGlobal(double qoeGlobal) { this.qoeGlobal = qoeGlobal; }

    public LocalDateTime getDateCalcule() { return dateCalcule; }
    public void setDateCalcule(LocalDateTime dateCalcule) { this.dateCalcule = dateCalcule; }

    // Getters pour l'interface (compatibilité avec le code existant)
    public double getSatisfactionScore() { return satisfactionQoe; }
    public double getVideoQuality() { return serviceQoe; }
    public double getAudioQuality() { return prixQoe; }
    public double getInteractivity() { return contratQoe; }
    public double getReliability() { return lifetimeQoe; }
    public double getOverallQoe() { return qoeGlobal; }
    public double getLoadingTime() { return latenceMoy; }
    public double getBuffering() { return jitterMoy; }
    public double getFailureRate() { return perteMoy; }
    public double getStreamingQuality() { return bandePassanteMoy; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "QoE{" +
                "idQoe=" + idQoe +
                ", idClient=" + idClient +
                ", qoeGlobal=" + qoeGlobal +
                ", satisfaction=" + satisfactionQoe +
                ", serviceType='" + serviceType + '\'' +
                ", dateCalcule=" + timestamp +
                '}';
    }
}