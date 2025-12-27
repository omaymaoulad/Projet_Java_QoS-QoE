package com.ensah.qoe.Services;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.QoE;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class QoeAnalyzer {

    private static final Map<Integer, QoE> subjectifParClient = new HashMap<>();
    private static boolean csvCharge = false;

    private static Double feedbackTemp = null;

    // =========================================================================
    // 1) IMPORT CSV + INSERTION AUTOMATIQUE
    // =========================================================================
    public static boolean analyserFichierCsv(String csvPath) {
        if (csvCharge) {
            System.out.println("✔ CSV déjà chargé en mémoire — aucune réimportation");
            return true;
        }
        String nomFichier = new java.io.File(csvPath).getName().trim();
        System.out.println("=== [QoeAnalyzer] Fichier détecté : " + nomFichier + " ===");
        // -------------------------------------------------------
        //  Vérifier si le fichier a déjà été importé
        // -------------------------------------------------------
        if (FichierService.fichierExiste(nomFichier)) {
            System.out.println("⚠ Le fichier est déjà importé. Chargement depuis la base...");

            csvCharge = true; // empêche l'analyse par CSV
            chargerDepuisBase(nomFichier);
            return true;
        }
        System.out.println("=== [QoeAnalyzer] Import CSV + Subjectif ===");

        subjectifParClient.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {

            String header = br.readLine();
            if (header == null) return false;

            String line;
            int id = 1;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] c = line.split(",", -1);
                if (c.length < 20) continue;

                QoE q = new QoE();
                q.setNomFichier(nomFichier);
                q.setSatisfactionQoe(computeSatisfaction(c[19], parseDoubleSafe(c[17]), c[11], c[14]));
                q.setServiceQoe(computeVideoQuality(c[7], c[12], c[13]));
                q.setPrixQoe(computeAudioQuality(c[11], c[10], c[8]));
                q.setContratQoe(computeInteractivity(c[5], c[6], c[2], parseIntSafe(c[4])));
                q.setLifetimeQoe(computeReliability(c[9], c[10], c[14], parseIntSafe(c[1])));

                subjectifParClient.put(id, q);
                id++;
            }

            csvCharge = true;

            System.out.println("[CSV] Nombre de clients subjectifs chargés = " + subjectifParClient.size());

            // INSERTION AUTOMATIQUE
            insererToutesLesQoe();
            FichierService.enregistrerFichier(nomFichier);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static void chargerDepuisBase(String nomFichier) {

        subjectifParClient.clear();

        String sql =
                "SELECT ID_CLIENT, GENRE, LATENCE_MOY, JITTER_MOY, PERTE_MOY, " +
                        "BANDE_PASSANTE_MOY, SIGNAL_SCORE_MOY, MOS_MOY, " +
                        "SATISFACTION_QOE, SERVICE_QOE, PRIX_QOE, CONTRAT_QOE, " +
                        "LIFETIME_QOE, FEEDBACK_SCORE, QOE_GLOBAL " +
                        "FROM QOE WHERE NOM_FICHIER = ? ORDER BY ID_CLIENT";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nomFichier);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                QoE q = new QoE();

                int id = rs.getInt("ID_CLIENT");

                // === SUBJECTIF ===
                q.setSatisfactionQoe(rs.getDouble("SATISFACTION_QOE"));
                q.setServiceQoe(rs.getDouble("SERVICE_QOE"));
                q.setPrixQoe(rs.getDouble("PRIX_QOE"));
                q.setContratQoe(rs.getDouble("CONTRAT_QOE"));
                q.setLifetimeQoe(rs.getDouble("LIFETIME_QOE"));
                q.setFeedbackScore(rs.getDouble("FEEDBACK_SCORE"));

                // === OBJECTIF ===
                q.setLatenceMoy(rs.getDouble("LATENCE_MOY"));
                q.setJitterMoy(rs.getDouble("JITTER_MOY"));
                q.setPerteMoy(rs.getDouble("PERTE_MOY"));
                q.setBandePassanteMoy(rs.getDouble("BANDE_PASSANTE_MOY"));
                q.setSignalScoreMoy(rs.getDouble("SIGNAL_SCORE_MOY"));
                q.setMosMoy(rs.getDouble("MOS_MOY"));

                // === Infos générales ===
                q.setGenre(rs.getString("GENRE"));
                q.setQoeGlobal(rs.getDouble("QOE_GLOBAL"));
                q.setNomFichier(nomFichier);

                subjectifParClient.put(id, q);
            }

            System.out.println("✔ QOE chargées intégralement depuis la base : "
                    + subjectifParClient.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // 2) INSERTION AUTOMATIQUE AVEC UNE SEULE CONNEXION
    // =========================================================================
    private static void insererToutesLesQoe() throws SQLException {

        System.out.println("=== [AUTO INSERT] Calcul + insertion ===");

        Connection conn = DBConnection.getConnection();

        String sql =
                "INSERT INTO QOE (" +
                        "ID_CLIENT, GENRE, LATENCE_MOY, JITTER_MOY, PERTE_MOY," +
                        "BANDE_PASSANTE_MOY, SIGNAL_SCORE_MOY, MOS_MOY," +
                        "SATISFACTION_QOE, SERVICE_QOE, PRIX_QOE, CONTRAT_QOE," +
                        "LIFETIME_QOE, FEEDBACK_SCORE, QOE_GLOBAL, NOM_FICHIER" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(sql);

        for (int id = 1; id <= subjectifParClient.size(); id++) {

            QoE q = analyserParClientSansConnexion(id, conn);

            if (q == null) continue;

            ps.setInt(1, id);
            ps.setString(2, q.getGenre());
            ps.setDouble(3, q.getLatenceMoy());
            ps.setDouble(4, q.getJitterMoy());
            ps.setDouble(5, q.getPerteMoy());
            ps.setDouble(6, q.getBandePassanteMoy());
            ps.setDouble(7, q.getSignalScoreMoy()); // IMPORTANT
            ps.setDouble(8, q.getMosMoy());
            ps.setDouble(9, q.getSatisfactionQoe());
            ps.setDouble(10, q.getServiceQoe());
            ps.setDouble(11, q.getPrixQoe());
            ps.setDouble(12, q.getContratQoe());
            ps.setDouble(13, q.getLifetimeQoe());
            if (q.getFeedbackScore() == null) {
                ps.setNull(14, java.sql.Types.DOUBLE);
            } else {
                ps.setDouble(14, q.getFeedbackScore());
            }
            ps.setDouble(15, q.getQoeGlobal());
            ps.setString(16, q.getNomFichier());

            ps.executeUpdate();
        }

        ps.close();
        conn.close();

        System.out.println("=== ✔ FIN : Insertion automatique de tous les QoE ===");
    }

    // =========================================================================
    // 3) ANALYSE CLIENT SANS FERMER LA CONNEXION
    // =========================================================================
    private static QoE analyserParClientSansConnexion(int id, Connection conn) {

        QoE q = copierSubjectif(subjectifParClient.get(id));
        if (q == null) return null;

        String zone = null, genre = null;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT LOCALISATION_ZONE, GENRE FROM CLIENT WHERE ID_CLIENT = ?"
        )) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                zone = rs.getString(1);
                genre = rs.getString(2);
            }

        } catch (Exception ignore) {}

        q.setGenre(genre);

        if (zone != null)
            remplirQosPourZoneSansConnexion(q, zone, conn);
        if (q.getFeedbackScore() != null) {
            System.out.println("✔ Feedback détecté : " + q.getFeedbackScore());
        }
        // Calcul final
        q.setQoeGlobal(computeGlobalQoe(
                q.getSatisfactionQoe(), q.getServiceQoe(),
                q.getPrixQoe(), q.getContratQoe(), q.getLifetimeQoe(),
                q.getMosMoy(), q.getPerteMoy()
        ));

        return q;
    }

    // =========================================================================
    // 4) ANALYSE UTILISÉE PAR LE CONTROLLER (séparée)
    // =========================================================================
    public static QoE analyserParClient(int idClient) {

        QoE q = null;

        try (Connection conn = DBConnection.getConnection()) {

            // Récupérer feedback APRES calcul subjectif
            feedbackTemp = getFeedbackScore(idClient, conn);

            // 2) Vérifier si subjectif existe
            if (!subjectifParClient.containsKey(idClient)) {
                System.out.println("⚠ Subjectif introuvable pour id=" + idClient +
                        " → chargement depuis la BDD");

                return chargerQoeDepuisBase(idClient, conn);
            }
            q = analyserParClientSansConnexion(idClient, conn);

            feedbackTemp = null; // reset

        } catch (Exception e) {
            e.printStackTrace();
        }

        return q;
    }

    public static QoE analyserParGenre(String genre) {
        QoE q = new QoE();
        q.setGenre(genre);

        // ==========================
        // 1) SUBJECTIF filtré par genre
        // ==========================
        double s1=0, s2=0, s3=0, s4=0, s5=0;
        int count = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT ID_CLIENT FROM CLIENT WHERE GENRE = ? ORDER BY ID_CLIENT"
             )) {

            ps.setString(1, genre);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt(1);

                if (!subjectifParClient.containsKey(id)) continue;

                QoE c = subjectifParClient.get(id);

                s1 += c.getSatisfactionQoe();
                s2 += c.getServiceQoe();
                s3 += c.getPrixQoe();
                s4 += c.getContratQoe();
                s5 += c.getLifetimeQoe();
                count++;
            }

        } catch (Exception e) { e.printStackTrace(); }

        if (count == 0) return null;

        // === Moyenne subjectif genre ===
        q.setSatisfactionQoe(s1 / count);
        q.setServiceQoe(s2 / count);
        q.setPrixQoe(s3 / count);
        q.setContratQoe(s4 / count);
        q.setLifetimeQoe(s5 / count);

        // ==========================
        // 2) OBJECTIF filtré par genre
        // ==========================
        String sql =
                "SELECT AVG(m.LATENCE), AVG(m.JITTER), AVG(m.PERTE), " +
                        "AVG(m.BANDE_PASSANTE), AVG(m.MOS) " +
                        "FROM CLIENT c " +
                        "JOIN MESURES_QOS m ON m.ZONE = c.LOCALISATION_ZONE " +
                        "WHERE c.GENRE = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, genre);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                q.setLatenceMoy(rs.getDouble(1));
                q.setJitterMoy(rs.getDouble(2));
                q.setPerteMoy(rs.getDouble(3));
                q.setBandePassanteMoy(rs.getDouble(4));
                q.setMosMoy(rs.getDouble(5));
            }

        } catch (Exception e) { e.printStackTrace(); }

        // ==========================
        // 3) CALCUL QoE GLOBAL du genre
        // ==========================
        q.setQoeGlobal(computeGlobalQoe(
                q.getSatisfactionQoe(), q.getServiceQoe(),
                q.getPrixQoe(), q.getContratQoe(), q.getLifetimeQoe(),
                q.getMosMoy(), q.getPerteMoy()
        ));

        return q;
    }

    public static QoE analyserParZone(String zone) {

        QoE q = new QoE();

        // SUBJECTIF = MOYENNE du CSV (global)
        double s1=0,s2=0,s3=0,s4=0,s5=0;
        int count = subjectifParClient.size();

        for (QoE c : subjectifParClient.values()) {
            s1 += c.getSatisfactionQoe();
            s2 += c.getServiceQoe();
            s3 += c.getPrixQoe();
            s4 += c.getContratQoe();
            s5 += c.getLifetimeQoe();
        }

        q.setSatisfactionQoe(s1/count);
        q.setServiceQoe(s2/count);
        q.setPrixQoe(s3/count);
        q.setContratQoe(s4/count);
        q.setLifetimeQoe(s5/count);

        // REMPLIR QOS pour la zone
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT AVG(LATENCE), AVG(JITTER), AVG(PERTE), AVG(BANDE_PASSANTE), AVG(MOS) " +
                             "FROM MESURES_QOS WHERE ZONE = ?"
             )) {

            ps.setString(1, zone);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                q.setLatenceMoy(rs.getDouble(1));
                q.setJitterMoy(rs.getDouble(2));
                q.setPerteMoy(rs.getDouble(3));
                q.setBandePassanteMoy(rs.getDouble(4));
                q.setMosMoy(rs.getDouble(5));
            }

        } catch (Exception e) { e.printStackTrace(); }

        q.setQoeGlobal(computeGlobalQoe(
                q.getSatisfactionQoe(), q.getServiceQoe(),
                q.getPrixQoe(), q.getContratQoe(), q.getLifetimeQoe(),
                q.getMosMoy(), q.getPerteMoy()
        ));

        return q;
    }

    // =========================================================================
    // NOUVELLES MÉTHODES POUR LE MENU QoE GLOBAL
    // =========================================================================

    /**
     * Calcule le QoE global pour toutes les données
     */
    public static QoE analyserQoEGlobal() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = """
                SELECT 
                    AVG(q.SATISFACTION_QOE) as satisfaction,
                    AVG(q.SERVICE_QOE) as service,
                    AVG(q.PRIX_QOE) as prix,
                    AVG(q.CONTRAT_QOE) as contrat,
                    AVG(q.LIFETIME_QOE) as lifetime,
                    AVG(q.QOE_GLOBAL) as qoe_global,
                    AVG(q.LATENCE_MOY) as latence,
                    AVG(q.JITTER_MOY) as jitter,
                    AVG(q.PERTE_MOY) as perte,
                    AVG(q.BANDE_PASSANTE_MOY) as bande_passante
                FROM QOE q
                WHERE q.QOE_GLOBAL IS NOT NULL
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    QoE qoe = new QoE();
                    qoe.setSatisfactionQoe(rs.getDouble("satisfaction"));
                    qoe.setServiceQoe(rs.getDouble("service"));
                    qoe.setPrixQoe(rs.getDouble("prix"));
                    qoe.setContratQoe(rs.getDouble("contrat"));
                    qoe.setLifetimeQoe(rs.getDouble("lifetime"));
                    qoe.setQoeGlobal(rs.getDouble("qoe_global"));
                    qoe.setLatenceMoy(rs.getDouble("latence"));
                    qoe.setJitterMoy(rs.getDouble("jitter"));
                    qoe.setPerteMoy(rs.getDouble("perte"));
                    qoe.setBandePassanteMoy(rs.getDouble("bande_passante"));

                    return qoe;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Exporte un rapport dans le format spécifié
     */
    public static boolean exporterRapport(String cheminFichier) {
        try {
            // Déterminer le format basé sur l'extension
            if (cheminFichier.toLowerCase().endsWith(".csv")) {
                return exporterRapportCSV(cheminFichier);
            } else if (cheminFichier.toLowerCase().endsWith(".pdf")) {
                return exporterRapportPDF(cheminFichier);
            } else if (cheminFichier.toLowerCase().endsWith(".xlsx")) {
                return exporterRapportExcel(cheminFichier);
            } else {
                System.err.println("Format non supporté: " + cheminFichier);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Exporte un rapport CSV simple
     */
    private static boolean exporterRapportCSV(String cheminFichier) {
        try (Connection conn = DBConnection.getConnection();
             PrintWriter writer = new PrintWriter(new FileWriter(cheminFichier))) {

            // En-tête CSV
            writer.println("ID_Client,Nom,Genre,Zone,QoE_Global,Satisfaction,Service,Prix,Contrat,Lifetime,Latence,Jitter,Perte,BandePassante");

            String sql = """
                SELECT 
                    c.ID_CLIENT, c.NOM, c.GENRE, c.LOCALISATION_ZONE,
                    q.QOE_GLOBAL, q.SATISFACTION_QOE, q.SERVICE_QOE, q.PRIX_QOE, 
                    q.CONTRAT_QOE, q.LIFETIME_QOE, q.LATENCE_MOY, q.JITTER_MOY, 
                    q.PERTE_MOY, q.BANDE_PASSANTE_MOY
                FROM CLIENT c
                LEFT JOIN QOE q ON c.ID_CLIENT = q.ID_CLIENT
                WHERE q.QOE_GLOBAL IS NOT NULL
                ORDER BY c.NOM
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    writer.printf("%d,%s,%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                            rs.getInt("ID_CLIENT"),
                            escapeCsv(rs.getString("NOM")),
                            escapeCsv(rs.getString("GENRE")),
                            escapeCsv(rs.getString("LOCALISATION_ZONE")),
                            rs.getDouble("QOE_GLOBAL"),
                            rs.getDouble("SATISFACTION_QOE"),
                            rs.getDouble("SERVICE_QOE"),
                            rs.getDouble("PRIX_QOE"),
                            rs.getDouble("CONTRAT_QOE"),
                            rs.getDouble("LIFETIME_QOE"),
                            rs.getDouble("LATENCE_MOY"),
                            rs.getDouble("JITTER_MOY"),
                            rs.getDouble("PERTE_MOY"),
                            rs.getDouble("BANDE_PASSANTE_MOY")
                    );
                }
            }

            System.out.println("Rapport CSV exporté: " + cheminFichier);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Échappe les caractères spéciaux pour CSV
     */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Exporte un rapport PDF (version simplifiée)
     */
    private static boolean exporterRapportPDF(String cheminFichier) {
        try {
            // Pour une implémentation PDF complète, vous pourriez utiliser une bibliothèque comme iText
            // Cette version crée un simple fichier texte pour l'exemple
            try (PrintWriter writer = new PrintWriter(new FileWriter(cheminFichier.replace(".pdf", "_report.txt")))) {

                writer.println("=== RAPPORT QoE ===");
                writer.println("Généré le: " + new java.util.Date());
                writer.println();

                try (Connection conn = DBConnection.getConnection()) {
                    // Statistiques globales
                    String statsSql = """
                        SELECT 
                            COUNT(*) as total_clients,
                            AVG(QOE_GLOBAL) as qoe_moyen,
                            MIN(QOE_GLOBAL) as qoe_min,
                            MAX(QOE_GLOBAL) as qoe_max
                        FROM QOE 
                        WHERE QOE_GLOBAL IS NOT NULL
                        """;

                    try (PreparedStatement ps = conn.prepareStatement(statsSql);
                         ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            writer.printf("Total clients: %d%n", rs.getInt("total_clients"));
                            writer.printf("QoE moyen: %.2f/5%n", rs.getDouble("qoe_moyen"));
                            writer.printf("QoE min: %.2f/5%n", rs.getDouble("qoe_min"));
                            writer.printf("QoE max: %.2f/5%n", rs.getDouble("qoe_max"));
                            writer.println();
                        }
                    }

                    // Détails par genre
                    writer.println("=== QoE PAR GENRE ===");
                    String genreSql = """
                        SELECT c.GENRE, AVG(q.QOE_GLOBAL) as qoe_moyen, COUNT(*) as count
                        FROM CLIENT c
                        JOIN QOE q ON c.ID_CLIENT = q.ID_CLIENT
                        WHERE c.GENRE IS NOT NULL AND q.QOE_GLOBAL IS NOT NULL
                        GROUP BY c.GENRE
                        ORDER BY qoe_moyen DESC
                        """;

                    try (PreparedStatement ps = conn.prepareStatement(genreSql);
                         ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {
                            writer.printf("%s: %.2f/5 (%d clients)%n",
                                    rs.getString("GENRE"),
                                    rs.getDouble("qoe_moyen"),
                                    rs.getInt("count"));
                        }
                    }
                }

                System.out.println("Rapport texte exporté: " + cheminFichier.replace(".pdf", "_report.txt"));
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Exporte un rapport Excel (version simplifiée - CSV avec extension xlsx)
     */
    private static boolean exporterRapportExcel(String cheminFichier) {
        // Pour l'instant, on utilise le même format que CSV
        return exporterRapportCSV(cheminFichier.replace(".xlsx", ".csv"));
    }

    // =========================================================================
    // 5) REMPLISSAGE QOS
    // =========================================================================
    private static void remplirQosPourZoneSansConnexion(QoE q, String zone, Connection conn) {

        String sql =
                "SELECT AVG(LATENCE), AVG(JITTER), AVG(PERTE), " +
                        "AVG(BANDE_PASSANTE), AVG(MOS) " +
                        "FROM MESURES_QOS WHERE ZONE = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, zone);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                q.setLatenceMoy(rs.getDouble(1));
                q.setJitterMoy(rs.getDouble(2));
                q.setPerteMoy(rs.getDouble(3));
                q.setBandePassanteMoy(rs.getDouble(4));
                q.setMosMoy(rs.getDouble(5));
            }

        } catch (Exception ignore) {}
    }

    // =========================================================================
    // 6) FORMULE GLOBALE
    // =========================================================================
    private static double computeGlobalQoe(
            double s1, double s2, double s3, double s4, double s5,
            double mos, double perte ) {
        double feedback;
        double subjectif = (s1 + s2 + s3 + s4 + s5) / 5.0;
        // Petit bonus = 10% si feedback existe
        if (feedbackTemp != null) {
            subjectif = (subjectif * 0.9) + (feedbackTemp * 0.1);
        }
        double mosNorm = mos / 5.0;
        double perteNorm = (100 - perte) / 100.0;

        double qos = (0.7 * mosNorm + 0.3 * perteNorm) * 5.0;

        return Math.max(1, Math.min(5, 0.6 * subjectif + 0.4 * qos));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private static QoE copierSubjectif(QoE src) {
        QoE q = new QoE();
        q.setSatisfactionQoe(src.getSatisfactionQoe());
        q.setServiceQoe(src.getServiceQoe());
        q.setPrixQoe(src.getPrixQoe());
        q.setContratQoe(src.getContratQoe());
        q.setLifetimeQoe(src.getLifetimeQoe());
        q.setNomFichier(src.getNomFichier());
        return q;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return 0; }
    }

    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return 0.0; }
    }

    // =========================================================================
    // FORMULES SUBJECTIVES
    // =========================================================================
    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double computeSatisfaction(String churn, double monthlyCharges,
                                              String techSupport, String contract) {

        double score = 5.0;

        if ("Yes".equalsIgnoreCase(churn)) score -= 3;
        if (monthlyCharges > 80) score -= 1;
        if ("No".equalsIgnoreCase(techSupport)) score -= 1;
        if ("Month-to-month".equalsIgnoreCase(contract)) score -= 0.5;

        return clamp(score, 1, 5);
    }

    private static double computeVideoQuality(String internetService,
                                              String streamingTV,
                                              String streamingMovies) {

        double score = 1;

        if ("Fiber optic".equalsIgnoreCase(internetService)) score += 3;
        if ("Yes".equalsIgnoreCase(streamingTV)) score += 0.5;
        if ("Yes".equalsIgnoreCase(streamingMovies)) score += 0.5;

        return clamp(score, 1, 5);
    }

    private static double computeAudioQuality(String techSupport,
                                              String deviceProtection,
                                              String onlineSecurity) {

        double score = 1;

        if ("Yes".equalsIgnoreCase(techSupport)) score += 2;
        if ("Yes".equalsIgnoreCase(deviceProtection)) score += 1;
        if ("Yes".equalsIgnoreCase(onlineSecurity)) score += 1;

        return clamp(score, 1, 5);
    }

    private static double computeInteractivity(String phoneService,
                                               String multipleLines,
                                               String partner,
                                               int tenure) {

        double score = 1;

        if ("Yes".equalsIgnoreCase(phoneService)) score += 1;
        if ("Yes".equalsIgnoreCase(multipleLines)) score += 1;
        if ("Yes".equalsIgnoreCase(partner)) score += 0.5;
        if (tenure > 12) score += 1;

        return clamp(score, 1, 5);
    }

    private static double computeReliability(String onlineBackup,
                                             String deviceProtection,
                                             String contract,
                                             int senior) {

        double score = 1;

        if ("Yes".equalsIgnoreCase(onlineBackup)) score += 1;
        if ("Yes".equalsIgnoreCase(deviceProtection)) score += 1;
        if ("One year".equalsIgnoreCase(contract) || "Two year".equalsIgnoreCase(contract))
            score += 1;
        if (senior == 0) score += 1;

        return clamp(score, 1, 5);
    }

    private static Double getFeedbackScore(int idClient, Connection conn) {

        String sql = "SELECT score FROM FEEDBACKS WHERE CLIENT_NAME = " +
                "(SELECT NOM FROM CLIENT WHERE ID_CLIENT = ?) " +
                "ORDER BY FEEDBACK_DATE DESC FETCH FIRST 1 ROW ONLY";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception ignored) {}

        return null;
    }

    private static QoE chargerQoeDepuisBase(int id, Connection conn) {

        String sql = "SELECT * FROM QOE WHERE ID_CLIENT = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                QoE q = new QoE();
                q.setIdClient(id);
                q.setSatisfactionQoe(rs.getDouble("SATISFACTION_QOE"));
                q.setServiceQoe(rs.getDouble("SERVICE_QOE"));
                q.setPrixQoe(rs.getDouble("PRIX_QOE"));
                q.setContratQoe(rs.getDouble("CONTRAT_QOE"));
                q.setLifetimeQoe(rs.getDouble("LIFETIME_QOE"));
                q.setFeedbackScore(rs.getDouble("FEEDBACK_SCORE"));

                q.setLatenceMoy(rs.getDouble("LATENCE_MOY"));
                q.setJitterMoy(rs.getDouble("JITTER_MOY"));
                q.setPerteMoy(rs.getDouble("PERTE_MOY"));
                q.setBandePassanteMoy(rs.getDouble("BANDE_PASSANTE_MOY"));
                q.setMosMoy(rs.getDouble("MOS_MOY"));

                q.setQoeGlobal(rs.getDouble("QOE_GLOBAL"));

                return q;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void reset() {
        csvCharge = false;
        subjectifParClient.clear();
        feedbackTemp = null;

        System.out.println("🔄 QoeAnalyzer réinitialisé — prêt pour un nouveau CSV");
    }
    public static boolean isCsvCharge() {
        return csvCharge;
    }

}