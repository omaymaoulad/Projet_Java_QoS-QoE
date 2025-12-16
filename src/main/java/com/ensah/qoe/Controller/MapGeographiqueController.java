package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.DBConnection;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class MapGeographiqueController implements Initializable {

    @FXML
    private WebView mapWebView;

    private WebEngine webEngine;
    private String zoneFiltre = null;
    private boolean mapReady = false;

    // Coordonnées GPS des zones
    private static final Map<String, double[]> COORDONNEES_ZONES = new HashMap<>();

    static {
        // ZONES RÉELLES DE FINLANDE, NORVÈGE ET SUÈDE
        COORDONNEES_ZONES.put("Kolari_FI", new double[]{67.3333, 23.7833});
        COORDONNEES_ZONES.put("Muonio_FI", new double[]{68.0167, 23.7000});
        COORDONNEES_ZONES.put("Kittilae_FI", new double[]{67.6500, 24.9167});
        COORDONNEES_ZONES.put("Lyngseidet_NO", new double[]{69.5833, 20.2167});
        COORDONNEES_ZONES.put("Olderdalen_NO", new double[]{69.3000, 19.6333});
        COORDONNEES_ZONES.put("Enontekioe_FI", new double[]{68.5833, 23.6333});
        COORDONNEES_ZONES.put("Hatteng_NO", new double[]{68.8333, 16.4167});
        COORDONNEES_ZONES.put("Pajala_SE", new double[]{67.2089, 23.3739});

        // Villes marocaines (si vous en avez besoin)
        COORDONNEES_ZONES.put("Casablanca", new double[]{33.5731, -7.5898});
        COORDONNEES_ZONES.put("Rabat", new double[]{34.0209, -6.8416});
        COORDONNEES_ZONES.put("Marrakech", new double[]{31.6295, -7.9811});
        COORDONNEES_ZONES.put("Fès", new double[]{34.0181, -5.0078});
        COORDONNEES_ZONES.put("Tanger", new double[]{35.7595, -5.8340});
        COORDONNEES_ZONES.put("Agadir", new double[]{30.4278, -9.5981});
        COORDONNEES_ZONES.put("Meknès", new double[]{33.8935, -5.5473});
        COORDONNEES_ZONES.put("Oujda", new double[]{34.6814, -1.9086});
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        webEngine = mapWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Configuration du WebView pour meilleure performance
        mapWebView.setContextMenuEnabled(false);
        mapWebView.setZoom(1.0);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                mapReady = true;
                invalidateMapSize();

                // Charger les données après un court délai
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(400);
                        chargerClientsDepuisDB();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        // Écouter les changements de taille
        mapWebView.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (mapReady) invalidateMapSize();
        });

        mapWebView.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (mapReady) invalidateMapSize();
        });

        chargerHTML();
    }

    public void setZoneFiltre(String zone) {
        this.zoneFiltre = zone;
    }

    private void invalidateMapSize() {
        if (!mapReady) return;

        Platform.runLater(() -> {
            try {
                webEngine.executeScript("if (window.invalidateMapSize) { window.invalidateMapSize(); }");
            } catch (Exception e) {
                try {
                    webEngine.executeScript("if (typeof map !== 'undefined') { map.invalidateSize(true); }");
                } catch (Exception ex) {
                    // Ignorer
                }
            }
        });
    }

    private void chargerHTML() {
        String htmlContent = genererHTML();
        webEngine.loadContent(htmlContent);
    }

    private String genererHTML() {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Carte QoE</title>
    
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    
    <style>
        html, body {
            height: 100%;
            width: 100%;
            margin: 0;
            padding: 0;
            overflow: hidden;
        }
        
        #map {
            position: absolute;
            top: 0;
            right: 0;
            bottom: 0;
            left: 0;
        }
        
        .leaflet-popup-content-wrapper {
            background: white;
            color: #333;
            border-radius: 8px;
            padding: 0;
        }
        
        .popup-header {
            background: #f59e0b;
            color: white;
            padding: 12px 15px;
            font-size: 15px;
            font-weight: bold;
        }
        
        .popup-body {
            padding: 12px 15px;
        }
        
        .metric-row {
            display: flex;
            justify-content: space-between;
            padding: 6px 0;
            border-bottom: 1px solid #f0f0f0;
        }
        
        .metric-row:last-child {
            border-bottom: none;
        }
        
        .metric-label {
            font-weight: 600;
            color: #666;
            font-size: 13px;
        }
        
        .metric-value {
            font-weight: bold;
            color: #333;
            font-size: 13px;
        }
        
        .qoe-badge {
            background: #fef3c7;
            color: #92400e;
            padding: 4px 12px;
            border-radius: 12px;
            font-weight: bold;
            font-size: 13px;
        }
        
        .legend {
            background: white;
            padding: 12px 15px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
        }
        
        .legend h4 {
            margin: 0 0 10px 0;
            color: #333;
            font-size: 14px;
            font-weight: bold;
        }
        
        .legend-item {
            display: flex;
            align-items: center;
            gap: 8px;
            margin: 6px 0;
            font-size: 12px;
        }
        
        .legend-circle {
            width: 16px;
            height: 16px;
            border-radius: 50%;
            border: 2px solid white;
            box-shadow: 0 1px 3px rgba(0,0,0,0.2);
        }
    </style>
</head>
<body>
    <div id="map"></div>
    
    <script>
        var map;
        var markersLayer;
        
        // PATCH CRITIQUE pour JavaFX WebView
        function patchLeafletForWebView() {
            if (window.L && L.Browser) {
                L.Browser.webkit3d = false;
                L.Browser.any3d = false;
                L.Browser.retina = false;
                
                // Override setTransform pour éviter translate3d
                var originalSetTransform = L.DomUtil.setTransform;
                L.DomUtil.setTransform = function(el, offset, scale) {
                    if (!el) return;
                    var pos = offset || new L.Point(0, 0);
                    var transform = 'translate(' + pos.x + 'px,' + pos.y + 'px)';
                    if (scale) {
                        transform += ' scale(' + scale + ')';
                    }
                    el.style.transform = transform;
                    el.style.webkitTransform = transform;
                };
                
                // Désactiver animations
                L.Map.mergeOptions({ 
                    zoomAnimation: false, 
                    fadeAnimation: false,
                    markerZoomAnimation: false,
                    inertia: false
                });
            }
        }
        
        function createMap() {
            if (map) return map;
            
            patchLeafletForWebView();
            
            map = L.map('map', {
                zoomControl: true,
                zoomAnimation: false,
                fadeAnimation: false,
                markerZoomAnimation: false,
                scrollWheelZoom: true,
                inertia: false,
                preferCanvas: true
            }).setView([68.0, 23.0], 5);  // Centré sur la Finlande/Norvège
            
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© OpenStreetMap',
                maxZoom: 18,
                updateWhenIdle: true,
                keepBuffer: 4
            }).addTo(map);
            
            markersLayer = L.layerGroup().addTo(map);
            
            // Légende
            var legend = L.control({position: 'bottomright'});
            legend.onAdd = function(map) {
                var div = L.DomUtil.create('div', 'legend');
                div.innerHTML = `
                    <h4>Légende QoE</h4>
                    <div class="legend-item">
                        <div class="legend-circle" style="background: #10b981;"></div>
                        <span>Excellent (4.5-5.0)</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-circle" style="background: #22c55e;"></div>
                        <span>Bon (4.0-4.4)</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-circle" style="background: #eab308;"></div>
                        <span>Moyen (3.0-3.9)</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-circle" style="background: #f97316;"></div>
                        <span>Faible (2.0-2.9)</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-circle" style="background: #ef4444;"></div>
                        <span>Mauvais (&lt; 2.0)</span>
                    </div>
                `;
                return div;
            };
            legend.addTo(map);
            
            return map;
        }
        
        // Fonctions API
        function getQoEColor(qoe) {
            if (qoe >= 4.5) return '#10b981';
            if (qoe >= 4.0) return '#22c55e';
            if (qoe >= 3.0) return '#eab308';
            if (qoe >= 2.0) return '#f97316';
            return '#ef4444';
        }
        
        function getQoELabel(qoe) {
            if (qoe >= 4.5) return 'Excellent';
            if (qoe >= 4.0) return 'Bon';
            if (qoe >= 3.0) return 'Moyen';
            if (qoe >= 2.0) return 'Faible';
            return 'Mauvais';
        }
        
        function addMarker(lat, lng, nomClient, genre, telephone, zone, qoe) {
            var mapInstance = createMap();
            var color = getQoEColor(qoe);
            var label = getQoELabel(qoe);
            
            var offsetLat = (Math.random() - 0.5) * 0.02;
            var offsetLng = (Math.random() - 0.5) * 0.02;
            
            var marker = L.circleMarker([lat + offsetLat, lng + offsetLng], {
                radius: 7,
                fillColor: color,
                color: '#ffffff',
                weight: 2,
                opacity: 1,
                fillOpacity: 0.85
            }).addTo(markersLayer);
            
            var genreIcon = genre === 'Male' ? '👨' : '👩';
            
            var popup = `
                <div class="popup-header">
                    ${genreIcon} ${nomClient}
                </div>
                <div class="popup-body">
                    <div class="metric-row">
                        <span class="metric-label">QoE</span>
                        <span class="qoe-badge">${qoe.toFixed(2)}/5 (${label})</span>
                    </div>
                    <div class="metric-row">
                        <span class="metric-label"> Zone</span>
                        <span class="metric-value">${zone}</span>
                    </div>
                    <div class="metric-row">
                        <span class="metric-label">⚧ Genre</span>
                        <span class="metric-value">${genre}</span>
                    </div>
                    <div class="metric-row">
                        <span class="metric-label"> Téléphone</span>
                        <span class="metric-value">${telephone}</span>
                    </div>
                </div>
            `;
            
            marker.bindPopup(popup, { maxWidth: 280 });
            return marker;
        }
        
        function clearMarkers() {
            if (markersLayer) {
                markersLayer.clearLayers();
            }
        }
        
        function centerOnZone(lat, lng) {
            var mapInstance = createMap();
            mapInstance.setView([lat, lng], 10);
        }
        
        window.invalidateMapSize = function() {
            var mapInstance = createMap();
            if (mapInstance) {
                try {
                    mapInstance.invalidateSize(true);
                } catch(e) {
                    console.error('invalidateMapSize error:', e);
                }
            }
        };
        
        window.mapAPI = {
            addMarker: addMarker,
            clearMarkers: clearMarkers,
            centerOnZone: centerOnZone
        };
        
        // Initialiser la carte
        createMap();
        
        setTimeout(function() {
            window.invalidateMapSize();
        }, 100);
    </script>
</body>
</html>
                """;
    }

    private void chargerClientsDepuisDB() {
        try {
            List<ClientGeo> clients = recupererClientsAvecQoE();

            if (clients.isEmpty()) {
                System.out.println("Aucun client trouvé");
                return;
            }

            System.out.println("Chargement de " + clients.size() + " clients");

            effacerMarqueurs();

            double[] coordsZoneCentree = null;

            for (ClientGeo client : clients) {
                if (zoneFiltre != null && !zoneFiltre.equals(client.zone)) {
                    continue;
                }

                double[] coords = obtenirCoordonnees(client);
                if (coords != null) {
                    ajouterMarqueurClient(
                            coords[0], coords[1],
                            client.nom, client.genre,
                            client.telephone, client.zone,
                            client.qoeGlobal
                    );

                    if (zoneFiltre != null && coordsZoneCentree == null) {
                        coordsZoneCentree = coords;
                    }
                }
            }

            if (coordsZoneCentree != null) {
                centrerSurZone(coordsZoneCentree[0], coordsZoneCentree[1]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<ClientGeo> recupererClientsAvecQoE() {
        List<ClientGeo> clients = new ArrayList<>();

        String sql = """
            SELECT 
                c.ID_CLIENT,
                c.NOM,
                c.GENRE,
                c.TELEPHONE,
                c.LOCALISATION_ZONE,
                c.LATITUDE,
                c.LONGITUDE,
                NVL(q.QOE_GLOBAL, 0) as QOE_GLOBAL
            FROM CLIENT c
            LEFT JOIN QOE q ON c.ID_CLIENT = q.ID_CLIENT
            WHERE c.LOCALISATION_ZONE IS NOT NULL
              AND c.LATITUDE IS NOT NULL
              AND c.LONGITUDE IS NOT NULL
            ORDER BY c.NOM
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ClientGeo client = new ClientGeo();
                client.idClient = rs.getInt("ID_CLIENT");
                client.nom = rs.getString("NOM");
                client.genre = rs.getString("GENRE");
                client.telephone = rs.getString("TELEPHONE");
                client.zone = rs.getString("LOCALISATION_ZONE");
                client.latitude = rs.getDouble("LATITUDE");
                client.longitude = rs.getDouble("LONGITUDE");
                client.qoeGlobal = rs.getDouble("QOE_GLOBAL");
                clients.add(client);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return clients;
    }

    private double[] obtenirCoordonnees(ClientGeo client) {
        // Si le client a des coordonnées précises, les utiliser
        if (client.latitude != 0 && client.longitude != 0) {
            return new double[]{client.latitude, client.longitude};
        }

        // Sinon, utiliser les coordonnées par défaut de la zone
        if (client.zone != null && COORDONNEES_ZONES.containsKey(client.zone)) {
            return COORDONNEES_ZONES.get(client.zone);
        }

        return null;
    }

    private void ajouterMarqueurClient(double lat, double lng, String nom,
                                       String genre, String tel, String zone, double qoe) {
        try {
            String script = String.format(Locale.US,
                    "window.mapAPI.addMarker(%f, %f, '%s', '%s', '%s', '%s', %f);",
                    lat, lng,
                    echapperJS(nom),
                    echapperJS(genre),
                    echapperJS(tel),
                    echapperJS(zone),
                    qoe
            );

            webEngine.executeScript(script);

        } catch (Exception e) {
            System.err.println("Erreur ajout marqueur: " + e.getMessage());
        }
    }

    private void effacerMarqueurs() {
        try {
            webEngine.executeScript("window.mapAPI.clearMarkers();");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void centrerSurZone(double lat, double lng) {
        try {
            String script = String.format(Locale.US,
                    "window.mapAPI.centerOnZone(%f, %f);", lat, lng
            );
            webEngine.executeScript(script);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String echapperJS(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static class ClientGeo {
        int idClient;
        String nom;
        String genre;
        String telephone;
        String zone;
        double latitude;
        double longitude;
        double qoeGlobal;
    }
}