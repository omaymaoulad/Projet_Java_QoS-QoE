package com.ensah.qoe.Services;

import com.ensah.qoe.Models.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClientService {

    // 🔥 Récupérer ID_CLIENT = ID_USER
    public static int getClientIdByUserId(int userId) {

        String sql = "SELECT id_client FROM client WHERE id_client = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("id_client");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1; // non trouvé
    }
}
