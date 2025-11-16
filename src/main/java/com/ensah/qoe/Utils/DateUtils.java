package com.ensah.qoe.Utils;

import java.time.*;

public class DateUtils {

    /**
     * Convertir un timestamp UNIX (avec décimales)
     * vers l'heure locale réelle selon le fuseau horaire dynamique
     */
    public static LocalDateTime convertirTimestamp(double timestampUnix, String timezone) {

        long millis = (long) (timestampUnix * 1000);

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.of(timezone))
                .toLocalDateTime();
    }

    /**
     * Convertir une date réelle en tranche 12h :
     * Exemple : 2024-03-13_00-12 ou 2024-03-13_12-24
     */
    public static String convertirEnTranche12h(LocalDateTime date) {

        int hour = date.getHour();
        int start = (hour < 12) ? 0 : 12;
        int end = start + 12;

        String d = date.toLocalDate().toString();
        return d + "_" + String.format("%02d", start) + "-" + String.format("%02d", end);
    }
}
