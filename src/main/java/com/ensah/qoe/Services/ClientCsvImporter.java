package com.ensah.qoe.Services;

import com.ensah.qoe.Models.DBConnection;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class ClientCsvImporter {

    public static void importClients() {
        String csvPath = "src/main/resources/clients_finland_international_1000.csv";

        String sql = "INSERT INTO CLIENT (NOM, GENRE, TELEPHONE, LOCALISATION_ZONE, EST_USER_APP) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath));
             Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {

                // Skip header
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(",");

                String nom = parts[0];
                String genre = parts[1];
                String telephone = parts[2];
                String zone = parts[3];
                int estUser = Integer.parseInt(parts[4]);

                ps.setString(1, nom);
                ps.setString(2, genre);
                ps.setString(3, telephone);
                ps.setString(4, zone);
                ps.setInt(5, estUser);

                ps.addBatch();
            }

            ps.executeBatch();
            System.out.println("✔ Import CLIENT terminé avec succès !");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
