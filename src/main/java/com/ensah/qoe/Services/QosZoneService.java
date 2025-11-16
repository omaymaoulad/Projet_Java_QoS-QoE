package com.ensah.qoe.Services;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.Qos;

import java.sql.*;
import java.util.*;

public class QosZoneService {

    public static List<String> getZones() {
        List<String> zones = new ArrayList<>();
        String sql = "SELECT DISTINCT ZONE FROM MESURES_QOS ORDER BY ZONE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) zones.add(rs.getString(1));

        } catch (Exception ignored) {}

        return zones;
    }

    public static List<Qos> getQosByZone(String zone) {
        List<Qos> liste = new ArrayList<>();

        String sql = "SELECT LATENCE, JITTER, PERTE, BANDE_PASSANTE, " +
                "SIGNAL_SCORE, MOS, TRANCHE_12H " +
                "FROM MESURES_QOS WHERE ZONE = ? " +
                "ORDER BY TRANCHE_12H";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, zone);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Qos q = new Qos();
                q.setLatence(rs.getDouble(1));
                q.setJitter(rs.getDouble(2));
                q.setPerte(rs.getDouble(3));
                q.setBandePassante(rs.getDouble(4));
                q.setSignalScore(rs.getDouble(5));
                q.setMos(rs.getDouble(6));
                q.setTranche12h(rs.getString(7));
                q.setZone(zone);
                liste.add(q);
            }

        } catch (Exception ignored) {}

        return liste;
    }

    public static List<Qos> getAllFromDatabase() {
        List<Qos> liste = new ArrayList<>();
        String sql = "SELECT * FROM MESURES_QOS";

        try (Connection conn = DBConnection.getConnection();
             ResultSet rs = conn.prepareStatement(sql).executeQuery()) {

            while (rs.next()) {
                Qos q = new Qos();
                q.setLatence(rs.getDouble("LATENCE"));
                q.setJitter(rs.getDouble("JITTER"));
                q.setPerte(rs.getDouble("PERTE"));
                q.setBandePassante(rs.getDouble("BANDE_PASSANTE"));
                q.setSignalScore(rs.getDouble("SIGNAL_SCORE"));
                q.setMos(rs.getDouble("MOS"));
                q.setTranche12h(rs.getString("TRANCHE_12H"));
                q.setZone(rs.getString("ZONE"));
                q.setNomFichier(rs.getString("NOM_FICHIER"));
                liste.add(q);
            }

        } catch (Exception ignored) {}

        return liste;
    }
}
