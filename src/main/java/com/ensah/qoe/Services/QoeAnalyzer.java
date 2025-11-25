package com.ensah.qoe.Services;

import com.ensah.qoe.Models.DBConnection;
import com.ensah.qoe.Models.QoE;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class QoeAnalyzer {

    private static final Map<Integer, QoE> subjectifParClient = new HashMap<>();
    private static boolean csvCharge = false;


    // =========================================================================
    // 1) IMPORT CSV + INSERTION AUTOMATIQUE
    // =========================================================================
    public static boolean analyserFichierCsv(String csvPath) {
        String nomFichier = new java.io.File(csvPath).getName().trim();
        System.out.println("=== [QoeAnalyzer] Fichier détecté : " + nomFichier + " ===");
        // -------------------------------------------------------
        //  Vérifier si le fichier a déjà été importé
        // -------------------------------------------------------
        if (FichierService.fichierExiste(nomFichier)) {
            System.out.println("⚠ Le fichier est déjà importé. Chargement depuis la base...");

            csvCharge = false; // empêche l’analyse par CSV
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

        if (!subjectifParClient.containsKey(idClient)) return null;

        try (Connection conn = DBConnection.getConnection()) {
            return analyserParClientSansConnexion(idClient, conn);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
            double mos, double perte) {

        double subjectif = (s1 + s2 + s3 + s4 + s5) / 5.0;

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
}
