import sys
import pandas as pd
import requests
from math import radians, sin, cos, atan2


# ------------------------------- Haversine ------------------------------------
def haversine(lat1, lon1, lat2, lon2):
    R = 6371
    dlat = radians(lat2 - lat1)
    dlon = radians(lon2 - lon1)
    a = sin(dlat/2)**2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlon/2)**2
    return 2 * R * atan2(a**0.5, (1 - a)**0.5)


# ----------------------- Extraction des points existants -----------------------
def extract_known(df):
    pts = []
    for _, row in df.iterrows():
        if (
                isinstance(row["city"], str)
                and row["city"] != "Unknown"
                and isinstance(row["country"], str)
                and row["country"] != "Unknown"
        ):
            pts.append((row["latitude"], row["longitude"], row["city"], row["country"]))
    return pts


# --------------------------- Recherche offline --------------------------------
def offline_lookup(lat, lon, pts):
    if not pts:
        return None, None

    best_city, best_country = None, None
    best_dist = 999999

    for la, lo, city, country in pts:
        d = haversine(lat, lon, la, lo)
        if d < best_dist:
            best_dist = d
            best_city, best_country = city, country

    return (best_city, best_country) if best_dist < 50 else (None, None)


# ---------------------------- Nominatim online --------------------------------
def online_lookup(lat, lon):
    try:
        url = f"https://nominatim.openstreetmap.org/reverse?lat={lat}&lon={lon}&format=json&addressdetails=1"
        r = requests.get(url, headers={"User-Agent": "GeoCoder"}, timeout=6)
        if r.status_code == 200:
            addr = r.json().get("address", {})
            city = addr.get("city") or addr.get("town") or addr.get("village")
            country = addr.get("country")
            return city, country
    except:
        return None, None

    return None, None


# ----------------------------------- MAIN -------------------------------------
if __name__ == "__main__":
    csv_path = sys.argv[1]

    df = pd.read_csv(csv_path)

    # Ajout des colonnes manquantes
    if "city" not in df.columns:
        df["city"] = "Unknown"
    if "country" not in df.columns:
        df["country"] = "Unknown"

    # Extraction des points connus
    offline_points = extract_known(df)

    for i, row in df.iterrows():
        lat = float(row["latitude"])
        lon = float(row["longitude"])

        if row["city"] != "Unknown" and row["country"] != "Unknown":
            continue

        # Offline
        city, country = offline_lookup(lat, lon, offline_points)
        if city and country:
            df.at[i, "city"] = city
            df.at[i, "country"] = country
            offline_points.append((lat, lon, city, country))
            continue

        # Online
        city2, country2 = online_lookup(lat, lon)
        if city2 and country2:
            df.at[i, "city"] = city2
            df.at[i, "country"] = country2
            offline_points.append((lat, lon, city2, country2))
            continue

        # Unknown final
        df.at[i, "city"] = "Unknown"
        df.at[i, "country"] = "Unknown"

    # SAUVEGARDE DU CSV AVEC LES NOUVELLES COLONNES
    df.to_csv(csv_path, index=False)

    # ⬇ IMPORTANT : Java lit ce print comme RESULTAT
    print(csv_path)
