package com.ensah.qoe.Models;

public class QoE {
    private double MOS_QoS;      // MOS calculé à partir de QoS
    private double feedbackClient; // Note donnée par le client
    private double QoE_final;     // Score final combiné
    private String niveau;
    private String smiley;
    private double alpha;         // Poids de QoS

    public QoE(double MOS_QoS, double feedbackClient, double alpha, String satisfaction) {
        this.MOS_QoS = MOS_QoS;
        this.feedbackClient = feedbackClient;
        this.alpha = alpha;
        calculateFinalQoE();
    }

    private void calculateFinalQoE() {
        QoE_final = alpha * MOS_QoS + (1 - alpha) * feedbackClient;
        determineLevel();
    }

    private void determineLevel() {
        if (QoE_final >= 4.5) { niveau = "Excellent"; smiley = "😄"; }
        else if (QoE_final >= 4) { niveau = "Bon"; smiley = "🙂"; }
        else if (QoE_final >= 3) { niveau = "Moyen"; smiley = "😐"; }
        else if (QoE_final >= 2) { niveau = "Faible"; smiley = "😟"; }
        else { niveau = "Mauvais"; smiley = "😡"; }
    }

    // Getters
    public double getQoE_final() { return QoE_final; }
    public String getNiveau() { return niveau; }
    public String getSmiley() { return smiley; }

    // Affichage rapide
    public void displayQoE() {
        System.out.println("QoE final: " + String.format("%.2f", QoE_final));
        System.out.println("Niveau: " + niveau);
        System.out.println("Smiley: " + smiley);
    }
}
