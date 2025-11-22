package com.ensah.qoe.Services;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.QoE;

import java.sql.*;

public class QoeInsertService {

    public static void insererQoe(QoE q) {

        String sql =
                "INSERT INTO QOE(" +
                        "ID_CLIENT, GENRE, LATENCE_MOY, JITTER_MOY, PERTE_MOY," +
                        "BANDE_PASSANTE_MOY, SIGNAL_SCORE_MOY, MOS_MOY," +
                        "SATISFACTION_QOE, SERVICE_QOE, PRIX_QOE, CONTRAT_QOE," +
                        "LIFETIME_QOE, FEEDBACK_SCORE, QOE_GLOBAL, NOM_FICHIER" +
                        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, q.getIdClient());
            ps.setString(2, q.getGenre());
            ps.setDouble(3, q.getLatenceMoy());
            ps.setDouble(4, q.getJitterMoy());
            ps.setDouble(5, q.getPerteMoy());
            ps.setDouble(6, q.getBandePassanteMoy());
            ps.setDouble(7, q.getSignalScoreMoy());
            ps.setDouble(8, q.getMosMoy());
            ps.setDouble(9, q.getSatisfactionQoe());
            ps.setDouble(10, q.getServiceQoe());
            ps.setDouble(11, q.getPrixQoe());
            ps.setDouble(12, q.getContratQoe());
            ps.setDouble(13, q.getLifetimeQoe());

            if (q.getFeedbackScore() == null)
                ps.setNull(14, Types.NUMERIC);
            else
                ps.setDouble(14, q.getFeedbackScore());

            ps.setDouble(15, q.getQoeGlobal());
            ps.setString(16, q.getNomFichier());

            ps.executeUpdate();

            System.out.println("✔ QOE inséré : " + q.getNomFichier());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static QoE chargerParNomFichier(String nom) {

        String sql = "SELECT * FROM QOE WHERE NOM_FICHIER=? ORDER BY ID_QOE DESC FETCH FIRST 1 ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nom);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            QoE q = new QoE();

            q.setIdQoe(rs.getInt("ID_QOE"));
            q.setIdClient(rs.getInt("ID_CLIENT"));
            q.setGenre(rs.getString("GENRE"));

            q.setLatenceMoy(rs.getDouble("LATENCE_MOY"));
            q.setJitterMoy(rs.getDouble("JITTER_MOY"));
            q.setPerteMoy(rs.getDouble("PERTE_MOY"));
            q.setBandePassanteMoy(rs.getDouble("BANDE_PASSANTE_MOY"));
            q.setSignalScoreMoy(rs.getDouble("SIGNAL_SCORE_MOY"));
            q.setMosMoy(rs.getDouble("MOS_MOY"));

            q.setSatisfactionQoe(rs.getDouble("SATISFACTION_QOE"));
            q.setServiceQoe(rs.getDouble("SERVICE_QOE"));
            q.setPrixQoe(rs.getDouble("PRIX_QOE"));
            q.setContratQoe(rs.getDouble("CONTRAT_QOE"));
            q.setLifetimeQoe(rs.getDouble("LIFETIME_QOE"));

            Object fb = rs.getObject("FEEDBACK_SCORE");
            q.setFeedbackScore(fb != null ? rs.getDouble("FEEDBACK_SCORE") : null);

            q.setQoeGlobal(rs.getDouble("QOE_GLOBAL"));
            q.setNomFichier(rs.getString("NOM_FICHIER"));

            q.setServiceType("Streaming");
            q.setDeviceType("Mobile");

            return q;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
