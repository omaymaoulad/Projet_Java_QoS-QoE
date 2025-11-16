package com.ensah.qoe.Utils;

import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GeoCoder {

    // -----------------------------------------------------------
    // 1) OFFLINE : liste des villes déjà trouvées dans le CSV Python
    //    (Pays nordiques + Finlande + Norvège)
    // -----------------------------------------------------------
    private static final List<CityPoint> OFFLINE_POINTS = Arrays.asList(
            new CityPoint(67.9566, 23.6821, "Muonio", "Finland"),
            new CityPoint(68.0050, 24.0700, "Pallas", "Finland"),
            new CityPoint(67.6500, 24.2500, "Äkäslompolo", "Finland"),
            new CityPoint(67.5500, 24.2500, "Ylläsjärvi", "Finland"),
            new CityPoint(67.6500, 24.9000, "Kittilä", "Finland"),
            new CityPoint(67.3300, 23.7700, "Kolari", "Finland"),

            // NORWAY (car reverse_geocoder a renvoyé Olderdalen)
            new CityPoint(69.6028, 20.5300, "Olderdalen", "Norway"),
            new CityPoint(69.7269, 20.9011, "Lyngseidet", "Norway")
    );


    // -----------------------------------------------------------
    // 2) Fonction principale (appelée par QosAnalyzer → NE CHANGE PAS)
    // -----------------------------------------------------------
    public static GeoResult getLocation(double lat, double lon) {

        // ---------- A) D'abord essayer OFFLINE (rapide, fiable si CSV pré-géocodé)
        CityPoint nearest = getNearest(OFFLINE_POINTS, lat, lon);
        if (nearest != null && distance(lat, lon, nearest.lat, nearest.lon) < 50) {
            return new GeoResult(nearest.city, nearest.country, "UTC");
        }

        // ---------- B) Sinon → Geocoder ONLINE Nominatim (ton code original)
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?lat=" + lat +
                    "&lon=" + lon + "&format=json&addressdetails=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "JavaFX-QoS-Analyzer");

            InputStream is = conn.getInputStream();
            String result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(result);

            JSONObject addr = json.getJSONObject("address");

            String city = addr.optString("city",
                    addr.optString("town",
                            addr.optString("village", "Unknown")));

            String country = addr.optString("country", "Unknown");

            return new GeoResult(city, country, "UTC");

        } catch (Exception e) {
            return new GeoResult("Unknown", "Unknown", "UTC");
        }
    }


    // -----------------------------------------------------------
    // 3) Trouver la ville offline la plus proche
    // -----------------------------------------------------------
    private static CityPoint getNearest(List<CityPoint> list, double lat, double lon) {
        CityPoint nearest = null;
        double minDist = Double.MAX_VALUE;

        for (CityPoint p : list) {
            double d = distance(lat, lon, p.lat, p.lon);
            if (d < minDist) {
                minDist = d;
                nearest = p;
            }
        }
        return nearest;
    }


    // Distance Haversine
    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);

        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }


    // Structure des villes offline
    private static class CityPoint {
        double lat, lon;
        String city, country;

        CityPoint(double lat, double lon, String city, String country) {
            this.lat = lat;
            this.lon = lon;
            this.city = city;
            this.country = country;
        }
    }


    // Résultat identique à l’ancienne version → QosAnalyzer continue à fonctionner
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
