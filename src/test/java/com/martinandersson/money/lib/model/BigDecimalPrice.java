package com.martinandersson.money.lib.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import static java.util.Objects.hash;
import javax.json.JsonNumber;
import static java.util.Objects.requireNonNull;

/**
 * Represents an adjusted close, internally stored as a {@code BigDecimal}.<p>
 * 
 * Please note that this implementation implements {@code Serializable}, but not
 * {@code Externalizable}. The serialization protocol has not been customized
 * (method {@code writeObject()} and {@code readObject()} is not implemented).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class BigDecimalPrice implements Price, Serializable
{
    private static final long serialVersionUID = 1;
    
    /**
     * Returns a BigDecimalPrice that internally store a BigDecimal of
     * 16.12535.
     * 
     * @return a BigDecimalPrice that internally store a BigDecimal of 16.12535
     */
    public static BigDecimalPrice getAverageSize() {
        return getAverageSize(new BigDecimal("16.12535"));
    }
    
    /**
     * Returns a BigDecimalPrice that internally store the specified BigDecimal.
     * 
     * @param assuming  BigDecimal to use
     * 
     * @return a BigDecimalPrice that internally store the specified BigDecimal.
     */
    public static BigDecimalPrice getAverageSize(BigDecimal assuming) {
        return new BigDecimalPrice(LocalDate.of(2010, 1, 1), assuming);
    }
    
    public static BigDecimalPrice ofJson(LocalDate date, JsonNumber adjClose) {
        return new BigDecimalPrice(date, adjClose.bigDecimalValue());
    }
    
    
    
    private final LocalDate date;
    
    private final BigDecimal adjClose;
    
    
    
    private BigDecimalPrice(LocalDate date, BigDecimal adjClose) {
        this.date = requireNonNull(date);
        this.adjClose = requireNonNull(adjClose);
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDate getDate() {
        return date;
    }
    
    public BigDecimal getAdjClose() {
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
        
        if (obj.getClass() != BigDecimalPrice.class) {
            return false;
        }
        
        BigDecimalPrice that = (BigDecimalPrice) obj;
        
        return this.date.equals(that.date) &&
               this.adjClose.equals(that.adjClose);
    }
}