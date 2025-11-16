package com.ensah.qoe.Services;

import com.ensah.qoe.Models.Qos;
import com.ensah.qoe.Utils.DateUtils;
import com.ensah.qoe.Utils.GeoCoder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.*;

public class QosAnalyzer {

    /**
     * Analyse un fichier CSV QoS et retourne la liste des mesures
     * regroupées par : (ZONE + TRANCHE 12H)
     */
    public static List<Qos> analyserQoSFichier(String csvPath, String nomFichier) {
        System.out.println(">>> QosAnalyzer lancé pour : " + nomFichier);
        // 1) Si le fichier existe déjà → on lit la DB (évite double import)
        if (FichierService.fichierExiste(nomFichier)) {
            System.out.println("⚠ Fichier déjà importé → chargement depuis la base.");
            return QosZoneService.getAllFromDatabase();
        }

        // Groupe :  "ZONE#TRANCHE12H" → liste de mesures brutes
        Map<String, List<Qos>> regroupement = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {

            String header = br.readLine(); // ignorer première ligne
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",");

                try {
                    // ---------- Extraction des colonnes importantes ----------
                    double timestamp = Double.parseDouble(p[0]);
                    double lat = Double.parseDouble(p[1]);
                    double lon = Double.parseDouble(p[2]);

                    // ---------- Ville & pays DIRECTEMENT DU CSV ----------
                    String city = p[23].trim();
                    String country = p[24].trim();

                    // ---------- Conversion date ----------
                    LocalDateTime dateReelle = DateUtils.convertirTimestamp(timestamp, "UTC");

                    String tranche12h = DateUtils.convertirEnTranche12h(dateReelle);

                    // ---------- Indicateurs QoS ----------
                    double delay = Double.parseDouble(p[16]);
                    double rsrq = Double.parseDouble(p[6]);
                    double sinr = Double.parseDouble(p[9]);
                    double thDown = Double.parseDouble(p[21]);
                    double thUp = Double.parseDouble(p[22]);
                    boolean status = p[19].trim().equals("1");

                    // ---------- Création objet QoS ----------
                    Qos q = new Qos();
                    q.setLatence(delay);
                    q.setJitter(0);
                    q.setPerte(status ? 0 : 100);
                    q.setSignalScore((rsrq + sinr) / 2);
                    q.setBandePassante((thDown + thUp) / 2);

                    q.setDateReelle(dateReelle);
                    q.setTranche12h(tranche12h);
                    q.setNomFichier(nomFichier);

                    // IMPORTANT : utiliser city + country du CSV
                    q.setZone(city + ", " + country);

                    // ---------- Regroupement ----------
                    String key = q.getZone() + "#" + tranche12h;

                    regroupement.computeIfAbsent(key, k -> new ArrayList<>()).add(q);

                } catch (Exception e) {
                    System.out.println("Erreur ligne: " + e.getMessage());
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // ---------- Calcul des moyennes par groupe ----------
        List<Qos> resultat = new ArrayList<>();

        for (List<Qos> groupe : regroupement.values()) {
            resultat.add(calculerMoyenne(groupe));
        }
        QosInsertService.insertListe(resultat);
        return resultat;
    }


    /**
     * Calcule les moyennes d’un groupe regroupé par zone + tranche12h
     */
    private static Qos calculerMoyenne(List<Qos> liste) {

        double lat = liste.stream().mapToDouble(Qos::getLatence).average().orElse(0);
        double jitter = calculerJitter(liste);
        double perte = liste.stream().mapToDouble(Qos::getPerte).average().orElse(0);
        double signal = liste.stream().mapToDouble(Qos::getSignalScore).average().orElse(0);
        double bp = liste.stream().mapToDouble(Qos::getBandePassante).average().orElse(0);

        // ---------- MOS (ITU-T) ----------
        double R = 94.2 - (lat / 40.0) - (1.2 * jitter) - (2 * perte);
        double mos = 1 + 0.035 * R + 7e-6 * R * (R - 60) * (100 - R);
        mos = Math.max(1, Math.min(5, mos));
        // ---------- QoS final ----------
        Qos q = new Qos();
        q.setLatence(lat);
        q.setJitter(jitter);
        q.setPerte(perte);
        q.setBandePassante(bp);
        q.setSignalScore(signal);
        q.setMos(mos);

        // On copie zone + tranche + fichier
        q.setZone(liste.get(0).getZone());
        q.setDateReelle(liste.get(0).getDateReelle());
        q.setTranche12h(liste.get(0).getTranche12h());
        q.setNomFichier(liste.get(0).getNomFichier());



        return q;
    }


    /**
     * Calcule le jitter à partir de la variation de latence
     */
    private static double calculerJitter(List<Qos> liste) {

        if (liste.size() <= 1) return 0;

        double total = 0;
        for (int i = 1; i < liste.size(); i++) {
            total += Math.abs(liste.get(i).getLatence() - liste.get(i - 1).getLatence());
        }

        return total / (liste.size() - 1);
    }
}
