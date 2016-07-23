package com.martinandersson.money.lib.model;

import com.martinandersson.money.lib.LocalDates;
import com.martinandersson.money.lib.MonetaHack;
import static com.martinandersson.money.lib.model.FastMoneyPrice.MAX_SCALE;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import static java.util.Objects.hash;
import javax.json.JsonNumber;
import javax.money.MonetaryAmount;
import org.javamoney.moneta.FastMoney;
import static java.util.Objects.requireNonNull;

/**
 * Walks like a {@code FastMoneyPrice}, quacks like a {@code FastMoneyPrice},
 * but uses a custom serialization protocol (implements method {@code
 * writeObject()} and {@code readObject()}).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 * 
 * @see FastMoneyPrice
 */
public class CustomFastMoneyPrice1 implements Price, Serializable
{
    private static final long serialVersionUID = 1;
    
    public static final CustomFastMoneyPrice1 EXACT_SIZE
            = new CustomFastMoneyPrice1(
                    LocalDate.of(2010, 1, 1),
                    new BigDecimal("16.12535"));
    
    /**
     * Construct a new {@code CustomFastMoneyPrice1} using specified {@code
     * date} and {@code number}.<p>
     * 
     * @param date      date of the adjusted closing price
     * @param adjClose  adjusted close
     * 
     * @return a new CustomFastMoneyPrice1
     * 
     * @see FastMoneyPrice#ofJson(LocalDate, JsonNumber)
     */
    public static CustomFastMoneyPrice1 ofJson(LocalDate date, JsonNumber adjClose) {
        BigDecimal bd = adjClose.bigDecimalValue();
        
        if (bd.scale() > MAX_SCALE) {
            bd = bd.setScale(MAX_SCALE, RoundingMode.HALF_EVEN);
        }
        
        return new CustomFastMoneyPrice1(date, bd);
    }
    
    
    /** Treat as final. */
    private transient LocalDate date;
    
    /** Treat as final. */
    private transient FastMoney adjClose;
    
    
    
    private CustomFastMoneyPrice1(LocalDate date, BigDecimal adjClose) {
        this(date, FastMoney.of(adjClose, "USD"));
    }
    
    private CustomFastMoneyPrice1(LocalDate date, FastMoney adjClose) {
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
        
        if (obj.getClass() != CustomFastMoneyPrice1.class) {
            return false;
        }
        
        CustomFastMoneyPrice1 that = (CustomFastMoneyPrice1) obj;
        
        return this.date.equals(that.date) &&
               this.adjClose.equals(that.adjClose);
    }
    
    
    
    /*
     *  ----------------------
     * | CUSTOM SERIALIZATION |
     *  ----------------------
     */
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeShort(LocalDates.toShort(date));
        out.writeLong(MonetaHack.getNumber(adjClose));
        out.writeObject(adjClose.getCurrency().getCurrencyCode());
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        date = LocalDates.fromShort(in.readShort());

        long val = in.readLong();
        String currency = (String) in.readObject();

        adjClose = MonetaHack.newFastMoney(val, currency);
    }
}