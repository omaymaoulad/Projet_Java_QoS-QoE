package com.ensah.qoe.Services;

import com.ensah.qoe.Models.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class QosInsertService {

    public void insert(double lat, double jitter, double perte, double bp,
                       double mos, double signal, double cellId, String zone, String typeConnexion) {

        String sql = "INSERT INTO MESURES_QOS (LATENCE, JITTER, PERTE, BANDE_PASSANTE, MOS, SIGNAL_SCORE, CELL_ID, ZONE, TYPE_CONNEXION) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, lat);
            ps.setDouble(2, jitter);
            ps.setDouble(3, perte);
            ps.setDouble(4, bp);
            ps.setDouble(5, mos);
            ps.setDouble(6, signal);
            ps.setDouble(7, cellId);
            ps.setString(8, zone);
            ps.setString(9, typeConnexion);

            ps.executeUpdate();
            System.out.println(" Mesure QoS enregistrée avec succès dans la base Oracle.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println(" Erreur lors de l’insertion dans MESURES_QOS : " + e.getMessage());
        }
    }
}
