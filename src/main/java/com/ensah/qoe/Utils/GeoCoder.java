package com.ensah.qoe.Utils;

import org.json.JSONObject;

import java.io.*;

public class GeoCoder {

    // -----------------------------------------------------
    // Fonction principale avec CSV venant de FileChooser
    // -----------------------------------------------------
    public static GeoResult getLocation(double lat, double lon, String csvPath,
                                        String cityFromCsv, String countryFromCsv) {

        // 1) SI LE CSV A DÉJÀ LES VALEURS → ON NE GÉOCODE PAS
        if (cityFromCsv != null && !cityFromCsv.isBlank() && !cityFromCsv.equals("Unknown") &&
                countryFromCsv != null && !countryFromCsv.isBlank() && !countryFromCsv.equals("Unknown")) {

            return new GeoResult(cityFromCsv, countryFromCsv, "UTC");
        }

        // 2) SINON → APPEL PYTHON
        GeoResult py = callPython(lat, lon, csvPath);
        if (py != null && !py.city.equals("Unknown"))
            return py;

        // 3) SI PYTHON NE DONNE RIEN → UNKNOWN
        return new GeoResult("Unknown", "Unknown", "UTC");
    }


    // -----------------------------------------------------
    // APPEL DU SCRIPT PYTHON (CSV dynamique)
    // -----------------------------------------------------
    private static GeoResult callPython(double lat, double lon, String csvPath) {
        try {
            String script = "src/main/resources/python/geocode_dynamic.py";

            ProcessBuilder pb = new ProcessBuilder(
                    "python", script,
                    String.valueOf(lat),
                    String.valueOf(lon),
                    csvPath
            );

            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream())
            );

            String json = reader.readLine();
            p.waitFor();

            if (json == null || json.isBlank())
                return null;

            JSONObject obj = new JSONObject(json);

            return new GeoResult(
                    obj.getString("city"),
                    obj.getString("country"),
                    "UTC"
            );

        } catch (Exception e) {
            return null;
        }
    }


    // -----------------------------------------------------
    // STRUCTURE
    // -----------------------------------------------------
    public static class GeoResult {
        public String city;
        public String country;
        public String timezone;

        public GeoResult(String c, String co, String t) {
            this.city = c;
            this.country = co;
            this.timezone = t;
        }
    }
}
