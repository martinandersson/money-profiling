package com.martinandersson.money.lib;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for numbers.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class Numbers
{
    private Numbers() {
        // Empty
    }
    
    
    
    public static int round0(double value) {
        return roundX(value, 0).intValueExact();
    }
    
    public static BigDecimal roundX(double value, int scale) {
        return new BigDecimal(value)
                .setScale(scale, RoundingMode.HALF_UP);
    }
}