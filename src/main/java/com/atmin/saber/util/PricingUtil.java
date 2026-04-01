package com.atmin.saber.util;

import java.math.BigDecimal;

/**
 * Utility class for pricing calculations.
 */
public class PricingUtil {

    private PricingUtil() {
        // Utility class
    }

    /**
     * Get hourly rate for a given zone/room.
     */
    public static BigDecimal getHourlyRateForZone(String roomName) {
        if (roomName == null) return BigDecimal.valueOf(10000);
        return switch (roomName.toLowerCase()) {
            case "atmin2", "atmin5" -> BigDecimal.valueOf(20000);
            case "atmin3", "atmin6" -> BigDecimal.valueOf(30000);
            default -> BigDecimal.valueOf(10000);
        };
    }
}

