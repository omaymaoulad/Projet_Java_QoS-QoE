package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.QoE;
import java.sql.*;
import java.util.*;
import com.ensah.qoe.Models.QoE;
import java.sql.*;
import java.util.*;

public class QoEController {

    // Récupère les feedbacks existants depuis la DB
    public Map<Integer, Integer> getFeedbacks() {
        Map<Integer, Integer> feedbackMap = new HashMap<>();
        try {
            Connection conn = DBConnection.getConnection();
            Statement stmt = conn.createStatement();
            String query = "SELECT user_id, feedback_score FROM feedbacks";
            ResultSet rs = stmt.executeQuery(query);

            while(rs.next()) {
                feedbackMap.put(rs.getInt("user_id"), rs.getInt("feedback_score"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return feedbackMap;
    }
    public double calculateMOS(double latency, double jitter, double packetLoss) {
        double R = 100 - (latency / 10 + jitter * 2 + packetLoss * 100); // simplifié
        double MOS = 1 + 0.035 * R + R * (R - 60) * (100 - R) * 7 * Math.pow(10, -6);
        return MOS;
    }

    // Déterminer niveau de satisfaction
    public String getSatisfaction(double finalQoE) {
        if(finalQoE >= 4.5) return "Excellent";
        else if(finalQoE >= 4) return "Bon";
        else if(finalQoE >= 3) return "Moyen";
        else if(finalQoE >= 2) return "Faible";
        else return "Mauvais";
    }
    public QoE computeQoEForUser(int userId, double latency, double jitter, double packetLoss) {
        double mos = calculateMOS(latency, jitter, packetLoss);

        Map<Integer, Integer> feedbacks = getFeedbacks();
        double feedbackScore = feedbacks.getOrDefault(userId, 3); // 3 = neutre si pas de feedback

        double finalQoE = (mos + feedbackScore) / 2; // tu peux ajuster pondération
        String satisfaction = getSatisfaction(finalQoE);

        return new QoE(userId, mos, finalQoE, satisfaction);
    }
}