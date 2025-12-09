package com.anurag.stock_tracker.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MarketCapFormat {
    private static final BigDecimal MILLION  = new BigDecimal("1000000");
    private static final BigDecimal BILLION  = new BigDecimal("1000000000");
    private static final BigDecimal TRILLION = new BigDecimal("1000000000000");

    public static String format(String rawMarketCap) {
        if (rawMarketCap == null || rawMarketCap.isBlank()) {
            return "N/A";
        }

        BigDecimal value;

        try {
            value = new BigDecimal(rawMarketCap);
        } catch (NumberFormatException e) {
            return "N/A";
        }

        // ≤ 6 digits → print as is
        if (value.compareTo(MILLION) < 0) {
            return value.toPlainString() + " dollars";
        }

        // million (2 decimals)
        if (value.compareTo(BILLION) < 0) {
            return value.divide(MILLION, 2, RoundingMode.HALF_UP)
                    + " million dollars";
        }

        // billion (2 decimals)
        if (value.compareTo(TRILLION) < 0) {
            return value.divide(BILLION, 3, RoundingMode.HALF_UP)
                    + " billion dollars";
        }

        // trillion (4 decimals)
        return value.divide(TRILLION, 3, RoundingMode.HALF_UP)
                + " trillion dollars";
    }
}
