package com.martinandersson.money.lib.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import static java.util.Objects.hash;
import javax.json.JsonNumber;
import javax.money.MonetaryAmount;
import org.javamoney.moneta.Money;
import static java.util.Objects.requireNonNull;

/**
 * Represents an adjusted close, internally stored as a {@code Money}.<p>
 * 
 * {@code Money} is a {@code MonetaryAmount} implementation from the reference
 * implementation of JSR-354.<p>
 * 
 * The currency used is "USD".<p>
 * 
 * Please note that this implementation implements {@code Serializable}, but not
 * {@code Externalizable}. The serialization protocol has not been customized
 * (method {@code writeObject()} and {@code readObject()} is not implemented).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class MoneyPrice implements Price, Serializable
{
    private static final long serialVersionUID = 1;
    
    /**
     * Returns a MoneyPrice that internally store a BigDecimal of 16.12535.
     * 
     * @return a MoneyPrice that internally store a BigDecimal of 16.12535
     */
    public static MoneyPrice getAverageSize() {
        return getAverageSize(new BigDecimal("16.12535"));
    }
    
    /**
     * Returns a MoneyPrice that internally store the specified BigDecimal.
     * 
     * @param assuming  BigDecimal to use
     * 
     * @return a MoneyPrice that internally store the specified BigDecimal.
     */
    public static MoneyPrice getAverageSize(BigDecimal assuming) {
        return new MoneyPrice(LocalDate.of(2010, 1, 1), assuming);
    }
    
    /**
     * Construct a new {@code MoneyPrice} using specified {@code date} and
     * {@code adjClose}.<p>
     * 
     * @implNote
     * If you study the source code of Moneta, you shall find that whatever
     * non-BigDecimal value we pass to Moneta's factories, a new BigDecimal
     * value will be created. It is understandable since a BigDecimal is exactly
     * what Money uses internally. Hence, {@code JsonNumber.bigDecimalValue()}
     * is used to extract the adjusted close.
     * 
     * @param date      date of the adjusted closing price
     * @param adjClose  adjusted close
     * 
     * @return a new MoneyPrice
     */
    public static MoneyPrice ofJson(LocalDate date, JsonNumber adjClose) {
        return new MoneyPrice(date, adjClose.bigDecimalValue());
    }
    
    
    
    private final LocalDate date;
    
    private final Money adjClose;
    
    
    
    private MoneyPrice(LocalDate date, BigDecimal adjClose) {
        this(date, Money.of(adjClose, "USD"));
    }
    
    private MoneyPrice(LocalDate date, Money adjClose) {
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
    
    MonetaryAmount getAdjClose() {
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
        
        if (obj.getClass() != MoneyPrice.class) {
            return false;
        }
        
        MoneyPrice that = (MoneyPrice) obj;
        
        return this.date.equals(that.date) &&
               this.adjClose.equals(that.adjClose);
    }
}