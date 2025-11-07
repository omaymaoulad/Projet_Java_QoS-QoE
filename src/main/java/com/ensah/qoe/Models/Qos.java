package com.ensah.qoe.Models;

public class Qos {
    private double latence;
    private double jitter;
    private double perte;
    private double bandePassante;
    private double signalScore;
    private double mos;
    private String dateMesure;

    public Qos(double latence, double jitter, double perte, double bandePassante, double signalScore,double mos) {
        this.latence = latence;
        this.jitter = jitter;
        this.perte = perte;
        this.bandePassante = bandePassante;
        this.signalScore = signalScore;
        this.mos = mos;
    }
    public double getLatence() { return latence; }
    public double getJitter() { return jitter; }
    public double getPerte() { return perte; }
    public double getBandePassante() { return bandePassante; }
    public double getSignalScore() { return signalScore; }
    public double getMos() { return mos; }
    public String getDateMesure() { return dateMesure; }

    public void setLatence(double latence) { this.latence = latence; }
    public void setJitter(double jitter) { this.jitter = jitter; }
    public void setPerte(double perte) { this.perte = perte; }
    public void setBandePassante(double bandePassante) {this.bandePassante = bandePassante;}
    public void setSignalScore(double signalScore) { this.signalScore = signalScore; }
    public void setMos(double mos) { this.mos = mos; }
    public void setDateMesure(String dateMesure) { this.dateMesure = dateMesure; }

    @Override
    public String toString() {
        return "QoS [Latence=" + latence + ", Jitter=" + jitter + ", Perte=" + perte +
                    ", BandePassante=" + bandePassante + ", Signal=" + signalScore +", Mos=" + mos +", dateMesure=" + dateMesure + "]";
        }
}


