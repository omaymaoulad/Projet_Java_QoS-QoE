package com.ensah.qoe.Services;

import com.ensah.qoe.Models.Qos;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QosAnalyzer {

    public static Qos analyserQoS(String csvPath) {
        List<Double> delays = new ArrayList<>();
        List<Double> rsrqs = new ArrayList<>();
        List<Double> sinrs = new ArrayList<>();
        List<Double> thDowns = new ArrayList<>();
        List<Double> thUps = new ArrayList<>();
        List<Boolean> statusList = new ArrayList<>();

        double cellId = 0;
        String band = "";
        String ran = "";

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String header = br.readLine(); // ignore l'entête
            if (header == null) {
                System.err.println("⚠️ Fichier CSV vide : " + csvPath);
                return null;
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                try {
                    double delay = Double.parseDouble(parts[16].trim());
                    double rsrq = Double.parseDouble(parts[6].trim());
                    double sinr = Double.parseDouble(parts[9].trim());
                    double thDown = Double.parseDouble(parts[21].trim());
                    double thUp = Double.parseDouble(parts[22].trim());
                    String service = parts[19].trim();
                    boolean status = service.equals("1") || service.equalsIgnoreCase("true");

                    cellId = Double.parseDouble(parts[12].trim());
                    band = parts[10].trim(); // TYPE_CONNEXION
                    ran = parts[11].trim();  // ZONE

                    delays.add(delay);
                    rsrqs.add(rsrq);
                    sinrs.add(sinr);
                    thDowns.add(thDown);
                    thUps.add(thUp);
                    statusList.add(status);

                } catch (NumberFormatException e) {
                    // ignore les lignes mal formatées
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur lecture CSV : " + e.getMessage());
            return null;
        }

        if (delays.isEmpty()) {
            System.err.println("⚠️ Aucune donnée valide trouvée.");
            return null;
        }

        // --- Calculs QoS ---
        double latence = moyenne(delays);
        double jitter = calculerJitter(delays);
        double perte = calculerPerte(statusList);
        double bandePassante = (moyenne(thDowns) + moyenne(thUps)) / 2.0;
        double signal = (moyenne(rsrqs) + moyenne(sinrs)) / 2.0;

        // MOS réaliste (basé sur ITU-T)
        double rFactor = 94.2 - (latence / 40.0) - (1.2 * jitter) - (perte * 2);
        double mos = 1 + (0.035) * rFactor + (rFactor * (rFactor - 60) * (100 - rFactor) * 7e-6);
        mos = Math.max(1, Math.min(5, mos));

        Qos qos = new Qos(latence, jitter, perte, bandePassante, signal, mos);
        qos.setCellid(cellId);
        qos.setZone(ran);
        qos.setType_connexion(band);

        System.out.println("✅ Résultats QoS : " + qos);
        return qos;
    }

    public static double moyenne(List<Double> valeurs) {
        return valeurs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public static double calculerJitter(List<Double> delays) {
        double total = 0;
        for (int i = 1; i < delays.size(); i++) {
            total += Math.abs(delays.get(i) - delays.get(i - 1));
        }
        return delays.size() > 1 ? total / (delays.size() - 1) : 0;
    }

    public static double calculerPerte(List<Boolean> status) {
        long total = status.size();
        long perdus = status.stream().filter(s -> !s).count();
        return total > 0 ? (double) perdus / total * 100.0 : 0;
    }
}
