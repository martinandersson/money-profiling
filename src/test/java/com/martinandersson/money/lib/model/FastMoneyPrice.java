package com.martinandersson.money.lib.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import static java.util.Objects.hash;
import javax.json.JsonNumber;
import org.javamoney.moneta.FastMoney;
import javax.money.MonetaryAmount;
import static java.util.Objects.requireNonNull;

/**
 * Represents an adjusted close, internally stored as a {@code FastMoney}.<p>
 * 
 * {@code FastMoney} is a {@code MonetaryAmount} implementation from the
 * reference implementation of JSR-354.<p>
 * 
 * The currency used is "USD".<p>
 * 
 * Please note that this implementation implements {@code Serializable}, but not
 * {@code Externalizable}. The serialization protocol has not been customized
 * (method {@code writeObject()} and {@code readObject()} is not implemented).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class FastMoneyPrice implements Price, Serializable
{   
    private static final long serialVersionUID = 1;
    
    /**
     * Max scale of the floating point value this class represents.<p>
     * 
     * Should be compiled to whatever value FastMoney#SCALE has. But that
     * field is private.
     */
    public static final int MAX_SCALE = 5;
    
    public static final FastMoneyPrice EXACT_SIZE = new FastMoneyPrice(
            LocalDate.of(2010, 1, 1),
            new BigDecimal("16.12535"));
    
    /**
     * Construct a new {@code FastMoneyPrice} using specified {@code date} and
     * {@code adjClose}.<p>
     * 
     * If need be, then this method will automatically round the number to
     * {@value #MAX_SCALE} decimal places using {@code RoundingMode.HALF_EVEN}.
     * 
     * @implNote
     * If you study the source code of Moneta, you shall find that whatever
     * non-BigDecimal value we pass to Moneta's factories, a new BigDecimal
     * value will be created. It is regrettable since internally all that
     * FastMoney store is primitives. Hence, {@code
     * JsonNumber.bigDecimalValue()} is used to extract the adjusted close.
     * 
     * @param date      date of the adjusted closing price
     * @param adjClose  adjusted close
     * 
     * @return a new FastMoneyPrice
     */
    public static FastMoneyPrice ofJson(LocalDate date, JsonNumber adjClose) {
        BigDecimal bd = adjClose.bigDecimalValue();
        
        if (bd.scale() > MAX_SCALE) {
            bd = bd.setScale(MAX_SCALE, RoundingMode.HALF_EVEN);
        }
        
        return new FastMoneyPrice(date, bd);
    }
    
    public static FastMoneyPrice ofNumber(LocalDate date, Number number) {
        return new FastMoneyPrice(date, number);
    }
    
    
    
    private final LocalDate date;
    
    private final FastMoney adjClose;
    
    
    
    private FastMoneyPrice(LocalDate date, Number adjClose) {
        this(date, FastMoney.of(adjClose, "USD"));
    }
    
    private FastMoneyPrice(LocalDate date, FastMoney adjClose) {
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
    
    public MonetaryAmount getAdjClose() {
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
        
        if (obj.getClass() != FastMoneyPrice.class) {
            return false;
        }
        
        FastMoneyPrice that = (FastMoneyPrice) obj;
        
        return this.date.equals(that.date) &&
               this.adjClose.equals(that.adjClose);
    }
}