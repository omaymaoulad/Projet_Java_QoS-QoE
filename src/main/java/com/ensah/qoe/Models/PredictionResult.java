package com.ensah.qoe.Models;

import java.util.Map;
import java.util.List;

public class PredictionResult {
    private double predictedMOS;
    private double anomalyScore;
    private String riskLevel;
    private String qualityLevel;
    private List<String> recommendations;
    private Map<String, Double> featureImportance;
    private boolean isAnomaly;
    private String zone;

    public PredictionResult() {}

    public PredictionResult(double predictedMOS, double anomalyScore,
                            String riskLevel, String qualityLevel,
                            List<String> recommendations,
                            Map<String, Double> featureImportance,
                            boolean isAnomaly, String zone) {
        this.predictedMOS = predictedMOS;
        this.anomalyScore = anomalyScore;
        this.riskLevel = riskLevel;
        this.qualityLevel = qualityLevel;
        this.recommendations = recommendations;
        this.featureImportance = featureImportance;
        this.isAnomaly = isAnomaly;
        this.zone = zone;
    }

    // Getters et Setters
    public double getPredictedMOS() { return predictedMOS; }
    public void setPredictedMOS(double predictedMOS) { this.predictedMOS = predictedMOS; }

    public double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(String qualityLevel) { this.qualityLevel = qualityLevel; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public Map<String, Double> getFeatureImportance() { return featureImportance; }
    public void setFeatureImportance(Map<String, Double> featureImportance) { this.featureImportance = featureImportance; }

    public boolean isAnomaly() { return isAnomaly; }
    public void setAnomaly(boolean anomaly) { isAnomaly = anomaly; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    // Méthodes utilitaires
    public String getRiskColor() {
        switch (riskLevel.toUpperCase()) {
            case "CRITIQUE": return "#e74c3c";
            case "ÉLEVÉ": return "#e67e22";
            case "MODÉRÉ": return "#f1c40f";
            case "FAIBLE": return "#2ecc71";
            default: return "#3498db";
        }
    }

    public String getQualityColor() {
        switch (qualityLevel.toUpperCase()) {
            case "EXCELLENT": return "#27ae60";
            case "BON": return "#2ecc71";
            case "FAIBLE": return "#f39c12";
            case "MAUVAIS": return "#e74c3c";
            default: return "#95a5a6";
        }
    }

    public String getRecommendationsAsString() {
        if (recommendations == null || recommendations.isEmpty()) {
            return "Aucune recommandation nécessaire.";
        }

        StringBuilder sb = new StringBuilder();
        for (String rec : recommendations) {
            sb.append("• ").append(rec).append("\n");
        }
        return sb.toString();
    }
}