package com.martinandersson.money.lib.chroniclemap;

import com.martinandersson.money.lib.Numbers;
import java.util.concurrent.TimeUnit;

/**
 * Log time.<p>
 * 
 * Log = store recorded time entries in accumulating instance fields.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class TimeLogger
{
    private int n,
                minCost = Integer.MAX_VALUE,
                maxCost = Integer.MIN_VALUE;
    
    private long totTimeSpent;
    
    
    
    /**
     * Returns the count of entries logged.
     * 
     * @return the count of entries logged
     */
    public int count() {
        return n;
    }
    
    /**
     * Returns the minimum time cost recorded (nanoseconds).
     * 
     * @return the minimum time cost recorded (nanoseconds)
     */
    public int getMinCost() {
        return minCost;
    }
    
    /**
     * Returns the maximum time cost recorded (nanoseconds).
     * 
     * @return the maximum time cost recorded (nanoseconds)
     */
    public int getMaxCost() {
        return maxCost;
    }
    
    /**
     * Returns the total time spent (nanoseconds).
     * 
     * @return the total time spent (nanoseconds)
     */
    public long getTotTimeSpent() {
        return totTimeSpent;
    }
    
    /**
     * Returns the total time spent.
     * 
     * @param unit  which time unit to use
     * 
     * @return the total time spent
     */
    public long getTotTimeSpent(TimeUnit unit) {
        return unit.convert(totTimeSpent, TimeUnit.NANOSECONDS);
    }
    
    
    
    /**
     * Returns the average time spent (nanoseconds).<p>
     * 
     * Uses {@code RoundingMode.HALF_UP}.
     * 
     * @return the average time spent (nanoseconds)
     */
    public int avgTimeCost() {
        return averageOf(totTimeSpent);
    }
    
    
    
    private long then;
    
    public void start() {
        if (then != 0) {
            throw new IllegalStateException("Already recording an operation.");
        }
        
        then = System.nanoTime();
    }
    
    public void complete() {
        final long now = System.nanoTime();
        
        if (then == 0) {
            throw new IllegalStateException(
                    "Please start an operation before completing another one.");
        }
        
        try {
            complete0(now);
        }
        finally {
            then = 0;
        }
    }
    
    private void complete0(long now) {
        final int timeSpent  = Math.toIntExact(now - then);
        
        totTimeSpent = Math.addExact(totTimeSpent, timeSpent);
        ++n;
        
        if (timeSpent < minCost) {
            minCost = timeSpent;
        }

        if (timeSpent > maxCost) {
            maxCost = timeSpent;
        }
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    private int averageOf(double sum) {
        if (n == 0) {
            return 0;
        }
        
        return Numbers.round0(sum / n);
    }
}