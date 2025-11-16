package com.ensah.qoe.Services;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.Qos;
import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public class QosInsertService {

    public static void insertListe(List<Qos> liste) {

        String sql = "INSERT INTO MESURES_QOS " +
                "(LATENCE, JITTER, PERTE, BANDE_PASSANTE, SIGNAL_SCORE, MOS,DATE_REELLE, TRANCHE_12H, ZONE, NOM_FICHIER) " +
                "VALUES (?, ?, ?, ?,?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Qos q : liste) {
                ps.setDouble(1, q.getLatence());
                ps.setDouble(2, q.getJitter());
                ps.setDouble(3, q.getPerte());
                ps.setDouble(4, q.getBandePassante());
                ps.setDouble(5, q.getSignalScore());
                ps.setDouble(6, q.getMos());
                ps.setTimestamp(7, Timestamp.valueOf(q.getDateReelle()));
                ps.setString(8, q.getTranche12h());
                ps.setString(9, q.getZone());
                ps.setString(10, q.getNomFichier());

                ps.addBatch();
            }

            ps.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Ajouter le nom du fichier dans TABLE FICHIERS_IMPORTES
        FichierService.enregistrerFichier(liste.get(0).getNomFichier());
    }
}
