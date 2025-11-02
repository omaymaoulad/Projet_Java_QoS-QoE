package Models;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static Connection connection = null;
    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Charger les propriétés
                Properties props = new Properties();
                FileInputStream fis = new FileInputStream("src/main/resources/config.properties");
                props.load(fis);

                // Lire les propriétés
                String url = props.getProperty("db.url");
                String user = props.getProperty("db.user");
                String password = props.getProperty("db.password");
                Class.forName("oracle.jdbc.OracleDriver");
                // Établir la connexion
                connection = DriverManager.getConnection(url, user, password);
                System.out.println(" Connexion réussie à la base Oracle !");
            } catch (IOException e) {
                System.out.println("Erreur de lecture du fichier config.properties : " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.out.println("class not found exception : " + e.getMessage());
            }
            catch (SQLException e) {
                System.out.println("Erreur de connexion à Oracle : " + e.getMessage());
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Connexion fermée.");
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        // Tente d'obtenir la connexion à la base Oracle
        java.sql.Connection conn = DBConnection.getConnection();

        if (conn != null) {
            System.out.println("✅ Connexion Oracle établie avec succès !");
        } else {
            System.out.println("❌ Échec de la connexion à la base Oracle.");
        }

        // Fermer la connexion proprement
        DBConnection.closeConnection();
    }
}
