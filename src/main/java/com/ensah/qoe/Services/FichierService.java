package com.ensah.qoe.Services;

import com.ensah.qoe.Models.DBConnection;

import java.sql.*;

public class FichierService {

    public static boolean fichierExiste(String nomFichier) {
        String sql = "SELECT 1 FROM FICHIERS_IMPORTES WHERE NOM_FICHIER = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nomFichier);
            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) { return false; }
    }

    public static void enregistrerFichier(String nomFichier) {
        String sql = "INSERT INTO FICHIERS_IMPORTES (NOM_FICHIER) VALUES (?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nomFichier);
            ps.executeUpdate();

        } catch (Exception ignored) {}
    }
}
