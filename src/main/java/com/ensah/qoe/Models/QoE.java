package com.ensah.qoe.Models;

public class QoE {
    // Métriques subjectives
    private double satisfactionScore;      // Score de satisfaction utilisateur (1-5)
    private double videoQuality;           // Qualité vidéo perçue (1-5)
    private double audioQuality;           // Qualité audio perçue (1-5)
    private double interactivity;          // Réactivité de l'application (1-5)
    private double reliability;            // Fiabilité du service (1-5)
    private double overallQoe;             // QoE globale calculée (1-5)

    // Métriques objectives basées sur QoS
    private double buffering;              // Temps de buffering (secondes)
    private double loadingTime;            // Temps de chargement (secondes)
    private double failureRate;            // Taux d'échec (%)
    private double streamingQuality;       // Qualité de streaming calculée

    // Informations contextuelles
    private String serviceType;            // Type de service (Video, VoIP, Gaming, Web)
    private String deviceType;             // Type d'appareil
    private int userId;
    private Integer qosId;
    private String timestamp;              // Horodatage

    // Constructeur vide
    public QoE() {
    }

    // Constructeur complet
    public QoE(double satisfactionScore, double videoQuality, double audioQuality,
               double interactivity, double reliability, double overallQoe,
               double buffering, double loadingTime, double failureRate,
               double streamingQuality, String deviceType,
               int userId, String timestamp,Integer qosId) {
        this.satisfactionScore = satisfactionScore;
        this.videoQuality = videoQuality;
        this.audioQuality = audioQuality;
        this.interactivity = interactivity;
        this.reliability = reliability;
        this.overallQoe = overallQoe;
        this.buffering = buffering;
        this.loadingTime = loadingTime;
        this.failureRate = failureRate;
        this.streamingQuality = streamingQuality;
        this.deviceType = deviceType;
        this.userId = userId;
        this.timestamp = timestamp;
        this.qosId = qosId;
    }

    // Getters et Setters
    public double getSatisfactionScore() {
        return satisfactionScore;
    }

    public void setSatisfactionScore(double satisfactionScore) {
        this.satisfactionScore = satisfactionScore;
    }

    public double getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(double videoQuality) {
        this.videoQuality = videoQuality;
    }

    public double getAudioQuality() {
        return audioQuality;
    }

    public void setAudioQuality(double audioQuality) {
        this.audioQuality = audioQuality;
    }

    public double getInteractivity() {
        return interactivity;
    }

    public void setInteractivity(double interactivity) {
        this.interactivity = interactivity;
    }

    public double getReliability() {
        return reliability;
    }

    public void setReliability(double reliability) {
        this.reliability = reliability;
    }

    public double getOverallQoe() {
        return overallQoe;
    }

    public void setOverallQoe(double overallQoe) {
        this.overallQoe = overallQoe;
    }

    public double getBuffering() {
        return buffering;
    }

    public void setBuffering(double buffering) {
        this.buffering = buffering;
    }

    public double getLoadingTime() {
        return loadingTime;
    }

    public void setLoadingTime(double loadingTime) {
        this.loadingTime = loadingTime;
    }

    public double getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(double failureRate) {
        this.failureRate = failureRate;
    }

    public double getStreamingQuality() {
        return streamingQuality;
    }

    public void setStreamingQuality(double streamingQuality) {
        this.streamingQuality = streamingQuality;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public Integer getQosId() { return qosId; }
    public void setQosId(Integer qosId) { this.qosId = qosId; }

    @Override
    public String toString() {
        return "Qoe{" +
                "satisfactionScore=" + satisfactionScore +
                ", videoQuality=" + videoQuality +
                ", audioQuality=" + audioQuality +
                ", interactivity=" + interactivity +
                ", reliability=" + reliability +
                ", overallQoe=" + overallQoe +
                ", buffering=" + buffering +
                ", loadingTime=" + loadingTime +
                ", failureRate=" + failureRate +
                ", streamingQuality=" + streamingQuality +
                ", serviceType='" + serviceType + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", userId=" + userId +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}