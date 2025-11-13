package com.ensah.qoe.Services;

import com.ensah.qoe.Models.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class QoeInsertService {

    /**
     * Insère les données QoE dans la base de données
     */
    public void insert(double satisfactionScore, double videoQuality, double audioQuality,
                       double interactivity, double reliability, double overallQoe,
                       double buffering, double loadingTime, double failureRate,
                       double streamingQuality, String serviceType, String deviceType,
                       int userId) {

        String query = "INSERT INTO qoe_metrics " +
                "(satisfaction_score, video_quality, audio_quality, interactivity, " +
                "reliability, overall_qoe, buffering, loading_time, failure_rate, " +
                "streaming_quality, service_type, device_type, user_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,  SYSDATE)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDouble(1, satisfactionScore);
            pstmt.setDouble(2, videoQuality);
            pstmt.setDouble(3, audioQuality);
            pstmt.setDouble(4, interactivity);
            pstmt.setDouble(5, reliability);
            pstmt.setDouble(6, overallQoe);
            pstmt.setDouble(7, buffering);
            pstmt.setDouble(8, loadingTime);
            pstmt.setDouble(9, failureRate);
            pstmt.setDouble(10, streamingQuality);
            pstmt.setString(11, serviceType);
            pstmt.setString(12, deviceType);
            pstmt.setInt(13, userId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Données QoE insérées avec succès dans la base de données");
            } else {
                System.out.println("⚠️ Aucune ligne insérée");
            }

        } catch (SQLException e) {
            System.err.println(" Erreur lors de l'insertion QoE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Insère les données QoE avec lien vers QoS
     */
    public void insertWithQosLink(double satisfactionScore, double videoQuality, double audioQuality,
                                  double interactivity, double reliability, double overallQoe,
                                  double buffering, double loadingTime, double failureRate,
                                  double streamingQuality, String serviceType, String deviceType,
                                  int userId, int qosId) {

        String query = "INSERT INTO qoe_metrics " +
                "(satisfaction_score, video_quality, audio_quality, interactivity, " +
                "reliability, overall_qoe, buffering, loading_time, failure_rate, " +
                "streaming_quality, service_type, device_type, user_id, qos_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,  SYSDATE)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDouble(1, satisfactionScore);
            pstmt.setDouble(2, videoQuality);
            pstmt.setDouble(3, audioQuality);
            pstmt.setDouble(4, interactivity);
            pstmt.setDouble(5, reliability);
            pstmt.setDouble(6, overallQoe);
            pstmt.setDouble(7, buffering);
            pstmt.setDouble(8, loadingTime);
            pstmt.setDouble(9, failureRate);
            pstmt.setDouble(10, streamingQuality);
            pstmt.setString(11, serviceType);
            pstmt.setString(12, deviceType);
            pstmt.setInt(13, userId);
            pstmt.setInt(14, qosId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Données QoE avec lien QoS insérées avec succès");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'insertion QoE avec lien: " + e.getMessage());
            e.printStackTrace();
        }
    }
}