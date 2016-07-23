package com.martinandersson.money.lib.model;

import java.io.Serializable;
import static java.lang.Double.doubleToLongBits;
import java.time.LocalDate;
import static java.util.Objects.hash;
import javax.json.JsonNumber;
import static java.util.Objects.requireNonNull;

/**
 * Represents an adjusted close, internally stored as a {@code double}.<p>
 * 
 * Please note that this implementation implements {@code Serializable}, but not
 * {@code Externalizable}. The serialization protocol has not been customized
 * (method {@code writeObject()} and {@code readObject()} is not implemented).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class DoublePrice implements Price, Serializable
{
    private static final long serialVersionUID = 1;
    
    public static final DoublePrice EXACT_SIZE
            = new DoublePrice(LocalDate.of(2010, 1, 1), 1.1);
    
    
    
    private final LocalDate date;
    
    private final double adjClose;
    
    
    
    public static DoublePrice ofDouble(LocalDate date, double adjClose) {
        return new DoublePrice(date, adjClose);
    }
    
    public static DoublePrice ofJson(LocalDate date, JsonNumber adjClose) {
        return new DoublePrice(date, adjClose.doubleValue());
    }
    
    
    
    private DoublePrice(LocalDate date, double adjClose) {
        this.date = requireNonNull(date);
        this.adjClose = adjClose;
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDate getDate() {
        return date;
    }
    
    public double getAdjClose() {
        return adjClose;
    }
    
    
    
    /**
     * {@inheritDoc}
     */    
    @Override
    public int hashCode() {
        return hash(date, adjClose);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null) {
            return false;
        }
        
        if (obj.getClass() != DoublePrice.class) {
            return false;
        }
        
        DoublePrice that = (DoublePrice) obj;
        
        return this.date.equals(that.date) &&
               (doubleToLongBits(this.adjClose) == doubleToLongBits(that.adjClose));
    }
}