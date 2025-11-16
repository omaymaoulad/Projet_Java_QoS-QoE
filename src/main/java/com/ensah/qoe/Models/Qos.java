package com.ensah.qoe.Models;
import java.time.LocalDateTime;
public class Qos {

    private int id_mesure;

    private double latence;
    private double jitter;
    private double perte;
    private double bandePassante;
    private double signalScore;
    private double mos;

    // Nouvelle architecture
    private String tranche12h;   // ex : "2024-11-12_00-12"
    private String zone;         // ex : "Muonio, Finland"
    private String nomFichier;
    private LocalDateTime dateReelle;

    public Qos() {}

    public Qos(double latence, double jitter, double perte,
               double bandePassante, double signalScore, double mos) {
        this.latence = latence;
        this.jitter = jitter;
        this.perte = perte;
        this.bandePassante = bandePassante;
        this.signalScore = signalScore;
        this.mos = mos;
    }

    // --- GETTERS ---
    public int getId_mesure() { return id_mesure; }
    public double getLatence() { return latence; }
    public double getJitter() { return jitter; }
    public double getPerte() { return perte; }
    public double getBandePassante() { return bandePassante; }
    public double getSignalScore() { return signalScore; }
    public double getMos() { return mos; }

    public String getTranche12h() { return tranche12h; }
    public String getZone() { return zone; }
    public String getNomFichier() { return nomFichier; }
    public LocalDateTime getDateReelle() { return dateReelle; }
    public void setDateReelle(LocalDateTime d) { this.dateReelle = d; }
    // --- SETTERS ---
    public void setId_mesure(int id_mesure) { this.id_mesure = id_mesure; }
    public void setLatence(double latence) { this.latence = latence; }
    public void setJitter(double jitter) { this.jitter = jitter; }
    public void setPerte(double perte) { this.perte = perte; }
    public void setBandePassante(double bandePassante) { this.bandePassante = bandePassante; }
    public void setSignalScore(double signalScore) { this.signalScore = signalScore; }
    public void setMos(double mos) { this.mos = mos; }

    public void setTranche12h(String tranche12h) { this.tranche12h = tranche12h; }
    public void setZone(String zone) { this.zone = zone; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }

    @Override
    public String toString() {
        return "QoS [" +
                "Latence=" + latence +
                ", Jitter=" + jitter +
                ", Perte=" + perte +
                ", BandePassante=" + bandePassante +
                ", Signal=" + signalScore +
                ", Mos=" + mos +
                ", Tranche12h=" + tranche12h +
                ", Zone=" + zone +
                "]";
    }
}
