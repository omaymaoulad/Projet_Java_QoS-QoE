package com.ensah.qoe.Models;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static Connection connection = null;
    private static Properties props = new Properties();

    static {
        // Charger la configuration une seule fois au démarrage
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("❌ Fichier config.properties non trouvé dans les ressources!");
                return;
            }
            props.load(input);
            System.out.println("✅ Configuration chargée: " + props.getProperty("db.url"));
        } catch (IOException e) {
            System.err.println("❌ Erreur lecture config.properties: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        try {
            // Vérifier si la connexion existe et est valide
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("🔄 Création d'une nouvelle connexion...");
                createNewConnection();
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur validation connexion: " + e.getMessage());
            createNewConnection(); // Tenter une reconnexion
        }
        return connection;
    }

    private static void createNewConnection() {
        try {
            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String password = props.getProperty("db.password");

            if (url == null || user == null || password == null) {
                System.err.println("❌ Paramètres de connexion manquants dans config.properties");
                return;
            }

            // Charger le driver
            Class.forName("oracle.jdbc.OracleDriver");

            // Établir la connexion
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connexion réussie à la base Oracle !");

        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver Oracle non trouvé: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Erreur connexion Oracle: " + e.getMessage());
            System.err.println("Code erreur: " + e.getErrorCode());
            connection = null;
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Connexion fermée.");
            }
            connection = null; // Important: réinitialiser à null
        } catch (SQLException e) {
            System.err.println("❌ Erreur fermeture connexion: " + e.getMessage());
        }
    }

    // Méthode pour forcer une reconnexion (utile après erreur)
    public static void reconnect() {
        closeConnection();
        getConnection();
    }

    public static void main(String[] args) {
        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            System.out.println("✅ Connexion Oracle établie avec succès !");
        } else {
            System.out.println("❌ Échec de la connexion à la base Oracle.");
        }
    }
}