package com.ensah.qoe.Models;

public class Qos {
    private int id_mesure;
    private double latence;
    private double jitter;
    private double perte;
    private double bandePassante;
    private double signalScore;
    private double mos;
    private String dateMesure;
    private String zone;
    private String type_connexion;
    private double cellid;


    public Qos(double latence, double jitter, double perte, double bandePassante, double signalScore,double mos) {
        this.latence = latence;
        this.jitter = jitter;
        this.perte = perte;
        this.bandePassante = bandePassante;
        this.signalScore = signalScore;
        this.mos = mos;
    }

    public Qos() {

    }

    public double getLatence() { return latence; }
    public double getJitter() { return jitter; }
    public double getPerte() { return perte; }
    public double getBandePassante() { return bandePassante; }
    public double getSignalScore() { return signalScore; }
    public double getMos() { return mos; }
    public String getDateMesure() { return dateMesure; }
    public double getCellid(){ return cellid; }
    public String getZone() { return zone; }
    public String getType_connexion() { return type_connexion; }
    public int getId_mesure() { return id_mesure; }

    public void setLatence(double latence) { this.latence = latence; }
    public void setJitter(double jitter) { this.jitter = jitter; }
    public void setPerte(double perte) { this.perte = perte; }
    public void setBandePassante(double bandePassante) {this.bandePassante = bandePassante;}
    public void setSignalScore(double signalScore) { this.signalScore = signalScore; }
    public void setMos(double mos) { this.mos = mos; }
    public void setDateMesure(String dateMesure) { this.dateMesure = dateMesure; }
    public void setZone(String zone) { this.zone = zone; }
    public void setType_connexion(String type_connexion) { this.type_connexion = type_connexion; }
    public void setCellid(double cellid) { this.cellid = cellid; }
    public void setId_mesure(int id_mesure) { this.id_mesure = id_mesure; }

    @Override
    public String toString() {
        return "QoS [Latence=" + latence + ", Jitter=" + jitter + ", Perte=" + perte +
                    ", BandePassante=" + bandePassante + ", Signal=" + signalScore +", Mos=" + mos +", dateMesure=" + dateMesure + "]";
        }
}


