package com.ensah.qoe.Services;

import com.ensah.qoe.Models.Qos;
import com.ensah.qoe.Utils.DateUtils;

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

        if (FichierService.fichierExiste(nomFichier)) {
            System.out.println(">>> Fichier déjà importé → retour DB");
            return QosZoneService.getAllFromDatabase();
        }

        Map<String, List<Qos>> regroupement = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {

            System.out.println(">>> Lecture header...");
            String headerLine = br.readLine();
            if (headerLine == null) {
                System.out.println(">>> ERREUR : header vide");
                return new ArrayList<>();
            }

            String[] headers = headerLine.split(",");
            System.out.println(">>> Colonnes trouvées: " + Arrays.toString(headers));

            int idxTimestamp   = find(headers, "timestamp");
            int idxLat         = find(headers, "latitude");
            int idxLon         = find(headers, "longitude");
            int idxDelay       = find(headers, "delay");
            int idxRsrq        = find(headers, "rsrq");
            int idxSinr        = find(headers, "sinr");
            int idxDown        = find(headers, "throughput_downlink");
            int idxUp          = find(headers, "throughput_uplink");
            int idxStatus      = find(headers, "service_status");
            int idxCity        = find(headers, "city");
            int idxCountry     = find(headers, "country");

            System.out.println(">>> Index OK !");

            String line;
            int cpt = 0;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",");

                try {
                    cpt++;
                    if (cpt % 1000 == 0) System.out.println(">>> Ligne: " + cpt);

                    double timestamp = Double.parseDouble(p[idxTimestamp]);
                    double lat = Double.parseDouble(p[idxLat]);
                    double lon = Double.parseDouble(p[idxLon]);

                    String city = safe(p, idxCity);
                    String country = safe(p, idxCountry);

                    LocalDateTime dateReelle = DateUtils.convertirTimestamp(timestamp, "UTC");
                    String tranche12h = DateUtils.convertirEnTranche12h(dateReelle);

                    double delay = Double.parseDouble(p[idxDelay]);
                    double rsrq = Double.parseDouble(p[idxRsrq]);
                    double sinr = Double.parseDouble(p[idxSinr]);
                    double thDown = Double.parseDouble(p[idxDown]);
                    double thUp = Double.parseDouble(p[idxUp]);
                    boolean status = safe(p, idxStatus).equals("1");

                    Qos q = new Qos();
                    q.setLatence(delay);
                    q.setJitter(0);
                    q.setPerte(status ? 0 : 100);
                    q.setSignalScore((rsrq + sinr) / 2);
                    q.setBandePassante((thDown + thUp) / 2);

                    q.setZone(city + ", " + country);
                    q.setDateReelle(dateReelle);
                    q.setTranche12h(tranche12h);
                    q.setNomFichier(nomFichier);

                    String key = q.getZone() + "#" + tranche12h;
                    regroupement.computeIfAbsent(key, k -> new ArrayList<>()).add(q);

                } catch (Exception e) {
                    System.out.println(">>> ERREUR ligne " + cpt + ": " + e.getMessage());
                }
            }

            System.out.println(">>> Analyse lignes terminée : " + cpt + " lignes");

        } catch (Exception e) {
            System.out.println(">>> ERREUR QosAnalyzer principale:");
            e.printStackTrace();
        }

        List<Qos> resultat = new ArrayList<>();
        for (List<Qos> groupe : regroupement.values()) {
            resultat.add(calculerMoyenne(groupe));
        }

        System.out.println(">>> Nb groupes générés: " + resultat.size());

        return resultat;
    }


    // cherche l'index d'une colonne dans le header
    private static int find(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(name.trim())) {
                return i;
            }
        }
        throw new RuntimeException("Colonne manquante : " + name);
    }

    private static String safe(String[] arr, int index) {
        if (index < 0 || index >= arr.length) return "Unknown";
        return arr[index].trim();
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

        double R = 94.2 - (lat / 40.0) - (1.2 * jitter) - (2 * perte);
        double mos = 1 + 0.035 * R + 7e-6 * R * (R - 60) * (100 - R);
        mos = Math.max(1, Math.min(5, mos));

        Qos q = new Qos();
        q.setLatence(lat);
        q.setJitter(jitter);
        q.setPerte(perte);
        q.setBandePassante(bp);
        q.setSignalScore(signal);
        q.setMos(mos);

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
