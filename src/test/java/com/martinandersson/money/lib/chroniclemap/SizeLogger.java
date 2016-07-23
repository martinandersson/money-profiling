package com.martinandersson.money.lib.chroniclemap;

import com.martinandersson.money.lib.Numbers;
import static java.lang.Math.toIntExact;
import static java.lang.Math.addExact;

/**
 * Log the size of key and value entries.<p>
 * 
 * Log = store recorded size in accumulating instance fields.<p>
 * 
 * Arguable, this guy should have been logging only "one size" and client use
 * two instances for each type (key/value)?<p>
 * 
 * TODO: Bother about it.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class SizeLogger
{
    private int n,
                minKeySize   = Integer.MAX_VALUE,
                maxKeySize   = Integer.MIN_VALUE,
                minValueSize = Integer.MAX_VALUE,
                maxValueSize = Integer.MIN_VALUE;
    
    private long totKeySize,
                 totValueSize;
    
    
    /**
     * Returns the count of entries logged.
     * 
     * @return the count of entries logged
     */
    public int count() {
        return n;
    }
    
    /**
     * Returns the minimum key size recorded (bytes).
     * 
     * @return the minimum key size recorded (bytes)
     */
    public int getMinKeySize() {
        return minKeySize;
    }
    
    /**
     * Returns the minimum value size recorded (bytes).
     * 
     * @return the minimum value size recorded (bytes)
     */
    public int getMinValueSize() {
        return minValueSize;
    }
    
    /**
     * Returns the maximum key size recorded (bytes).
     * 
     * @return the maximum key size recorded (bytes)
     */
    public int getMaxKeySize() {
        return maxKeySize;
    }
    
    /**
     * Returns the maximum value size recorded (bytes).
     * 
     * @return the maximum value size recorded (bytes)
     */
    public int getMaxValueSize() {
        return maxValueSize;
    }
    
    /**
     * Returns the total count of key bytes recorded.
     * 
     * @return the total count of key bytes recorded
     */
    public long getTotKeySize() {
        return totKeySize;
    }
    
    /**
     * Returns the total count of value bytes recorded.
     * 
     * @return the total count of value bytes recorded
     */
    public long getTotValueSize() {
        return totValueSize;
    }
    
    /**
     * Returns the average key size recorded (bytes).<p>
     * 
     * Uses {@code RoundingMode.HALF_UP}.
     * 
     * @return the average key size recorded (bytes)
     */
    public int avgKeySize() {
        return averageOf(totKeySize);
    }
    
    /**
     * Returns the average value size recorded (bytes).<p>
     * 
     * Uses {@code RoundingMode.HALF_UP}.
     * 
     * @return the average value size seen (bytes)
     */
    public int avgValueSize() {
        return averageOf(totValueSize);
    }
    
    public void record(long keySize, long valueSize) {
        final int keySizeInt = toIntExact(keySize),
                  valSizeInt = toIntExact(valueSize);
        
        // Prefer to mutate our state "atomically":
        
        long newTotKeySize = addExact(totKeySize, keySize);
        totValueSize = addExact(totValueSize, valSizeInt);
        totKeySize = newTotKeySize;
        
        ++n;
        
        if (keySizeInt < minKeySize) {
            minKeySize = keySizeInt;
        }
        
        if (keySizeInt > maxKeySize) {
            maxKeySize = keySizeInt;
        }
        
        if (valSizeInt < minValueSize) {
            minValueSize = valSizeInt;
        }
        
        if (valSizeInt > maxValueSize) {
            maxValueSize = valSizeInt;
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