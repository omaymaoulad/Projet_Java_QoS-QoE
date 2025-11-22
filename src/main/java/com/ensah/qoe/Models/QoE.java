package com.ensah.qoe.Models;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class QoE {

    private Integer idQoe;
    private Integer idClient;          // pour lien futur avec CLIENT
    private String genre;             // 'Male' / 'Female' (optionnel)

    // ---- Métriques QoS (moyennes par défaut) ----
    private double latenceMoy;
    private double jitterMoy;
    private double perteMoy;
    private double bandePassanteMoy;
    private double signalScoreMoy;
    private double mosMoy;

    // ---- Métriques QoE subjectives ----
    private double satisfactionQoe;
    private double serviceQoe;
    private double prixQoe;
    private double contratQoe;
    private double lifetimeQoe;
    private Double feedbackScore;     // peut être null

    // ---- Score global ----
    private double qoeGlobal;
    private LocalDateTime dateCalcule;

    // ---- Métadonnées ----
    private String nomFichier;        // NOM_FICHIER dans la table

    // ---- Champs pour l’UI (pas en BD) ----
    private String serviceType;
    private String deviceType;

    public QoE() {}

    // ================== GETTERS / SETTERS BD ==================

    public Integer getIdQoe() { return idQoe; }
    public void setIdQoe(Integer idQoe) { this.idQoe = idQoe; }

    public Integer getIdClient() { return idClient; }
    public void setIdClient(Integer idClient) { this.idClient = idClient; }

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

    public double getSignalScoreMoy() { return signalScoreMoy; }
    public void setSignalScoreMoy(double signalScoreMoy) { this.signalScoreMoy = signalScoreMoy; }

    public double getMosMoy() { return mosMoy; }
    public void setMosMoy(double mosMoy) { this.mosMoy = mosMoy; }

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

    public Double getFeedbackScore() { return feedbackScore; }
    public void setFeedbackScore(Double feedbackScore) { this.feedbackScore = feedbackScore; }

    public double getQoeGlobal() { return qoeGlobal; }
    public void setQoeGlobal(double qoeGlobal) { this.qoeGlobal = qoeGlobal; }

    public LocalDateTime getDateCalcule() { return dateCalcule; }
    public void setDateCalcule(LocalDateTime dateCalcule) { this.dateCalcule = dateCalcule; }

    public void setDateCalculeFromDate(Date d) {
        if (d != null) {
            this.dateCalcule = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
    }

    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }

    // ================== GETTERS POUR L’UI ==================
    // (compatibles avec ton FXML / Controller actuel)

    public double getSatisfactionScore() { return satisfactionQoe; }
    public double getVideoQuality()     { return serviceQoe; }
    public double getAudioQuality()     { return prixQoe; }
    public double getInteractivity()    { return contratQoe; }
    public double getReliability()      { return lifetimeQoe; }
    public double getOverallQoe()       { return qoeGlobal; }

    public double getBuffering()        { return jitterMoy; }
    public double getLoadingTime()      { return latenceMoy; }
    public double getFailureRate()      { return perteMoy; }
    public double getStreamingQuality() { return bandePassanteMoy; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    @Override
    public String toString() {
        return "QoE{" +
                "idQoe=" + idQoe +
                ", idClient=" + idClient +
                ", qoeGlobal=" + qoeGlobal +
                ", satisfactionQoe=" + satisfactionQoe +
                ", nomFichier='" + nomFichier + '\'' +
                '}';
    }
}
