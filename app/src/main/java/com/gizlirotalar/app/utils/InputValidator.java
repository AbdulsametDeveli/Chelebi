package com.gizlirotalar.app.utils;

public class InputValidator {
    public static boolean validateSpotName(String name) {
        return name != null && name.trim().length() >= 3 && name.trim().length() <= 100;
    }
    public static boolean validateDescription(String desc) {
        return desc != null && desc.trim().length() >= 10 && desc.trim().length() <= 1000;
    }
    public static boolean validateCoordinates(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }
    public static boolean validateRating(int rating) {
        return rating >= 1 && rating <= 5;
    }
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
