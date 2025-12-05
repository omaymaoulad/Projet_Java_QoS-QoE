package com.ensah.qoe.Models;

import java.sql.Timestamp;

public class QoSMeasurement {
    private int idMesure;
    private double latence;
    private double jitter;
    private double perte;
    private double bandePassante;
    private double signalScore;
    private double mos;
    private Timestamp dateReelle;
    private String tranche12h;
    private String zone;
    private String nomFichier;
    private int anomalie;

    // Constructeurs
    public QoSMeasurement() {}

    public QoSMeasurement(int idMesure, double latence, double jitter, double perte,
                          double bandePassante, double signalScore, double mos,
                          Timestamp dateReelle, String tranche12h, String zone,
                          String nomFichier, int anomalie) {
        this.idMesure = idMesure;
        this.latence = latence;
        this.jitter = jitter;
        this.perte = perte;
        this.bandePassante = bandePassante;
        this.signalScore = signalScore;
        this.mos = mos;
        this.dateReelle = dateReelle;
        this.tranche12h = tranche12h;
        this.zone = zone;
        this.nomFichier = nomFichier;
        this.anomalie = anomalie;
    }

    // Getters et Setters
    public int getIdMesure() { return idMesure; }
    public void setIdMesure(int idMesure) { this.idMesure = idMesure; }

    public double getLatence() { return latence; }
    public void setLatence(double latence) { this.latence = latence; }

    public double getJitter() { return jitter; }
    public void setJitter(double jitter) { this.jitter = jitter; }

    public double getPerte() { return perte; }
    public void setPerte(double perte) { this.perte = perte; }

    public double getBandePassante() { return bandePassante; }
    public void setBandePassante(double bandePassante) { this.bandePassante = bandePassante; }

    public double getSignalScore() { return signalScore; }
    public void setSignalScore(double signalScore) { this.signalScore = signalScore; }

    public double getMos() { return mos; }
    public void setMos(double mos) { this.mos = mos; }

    public Timestamp getDateReelle() { return dateReelle; }
    public void setDateReelle(Timestamp dateReelle) { this.dateReelle = dateReelle; }

    public String getTranche12h() { return tranche12h; }
    public void setTranche12h(String tranche12h) { this.tranche12h = tranche12h; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }

    public int getAnomalie() { return anomalie; }
    public void setAnomalie(int anomalie) { this.anomalie = anomalie; }

    // Méthodes utilitaires
    public boolean isAnomaly() {
        return latence > 200 || jitter > 50 || perte > 0.5 || mos < 3.0;
    }

    public String getQualityLevel() {
        if (mos >= 4.0) return "EXCELLENT";
        if (mos >= 3.0) return "BON";
        if (mos >= 2.0) return "FAIBLE";
        return "MAUVAIS";
    }

    public double calculateQualityScore() {
        double score = 0.0;

        // Latence (poids 30%)
        double latenceScore = Math.max(0, 1 - (latence / 500));
        score += latenceScore * 0.3;

        // Jitter (poids 20%)
        double jitterScore = Math.max(0, 1 - (jitter / 100));
        score += jitterScore * 0.2;

        // Perte (poids 20%)
        double perteScore = Math.max(0, 1 - (perte / 5));
        score += perteScore * 0.2;

        // MOS (poids 30%)
        double mosScore = mos / 5;
        score += mosScore * 0.3;

        return score;
    }

    @Override
    public String toString() {
        return String.format("QoS[ID:%d, Lat:%.1f, Jit:%.1f, Per:%.2f%%, MOS:%.2f, Zone:%s]",
                idMesure, latence, jitter, perte, mos, zone);
    }
}