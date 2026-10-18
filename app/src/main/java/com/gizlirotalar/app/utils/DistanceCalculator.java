package com.gizlirotalar.app.utils;

public class DistanceCalculator {
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1) return String.format("%.0f m", distanceKm * 1000);
        return String.format("%.1f km", distanceKm);
    }
}
