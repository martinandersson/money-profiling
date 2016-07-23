package com.martinandersson.money.lib.model;

import com.martinandersson.money.lib.LocalDates;
import com.martinandersson.money.lib.MonetaHack;
import static com.martinandersson.money.lib.model.FastMoneyPrice.MAX_SCALE;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
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
 * but uses its own serialization protocol (implements {@code Externalizable}).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 * 
 * @see FastMoneyPrice
 */
public class CustomFastMoneyPrice2 implements Price, Externalizable
{
    private static final long serialVersionUID = 1;
    
    public static final CustomFastMoneyPrice2 EXACT_SIZE
            = new CustomFastMoneyPrice2(
                    LocalDate.of(2010, 1, 1),
                    new BigDecimal("16.12535"));
    
    /**
     * Construct a new {@code CustomFastMoneyPrice2} using specified {@code
     * date} and {@code number}.<p>
     * 
     * @param date      date of the adjusted closing price
     * @param adjClose  adjusted close
     * 
     * @return a new CustomFastMoneyPrice2
     * 
     * @see FastMoneyPrice#ofJson(LocalDate, JsonNumber)
     */
    public static CustomFastMoneyPrice2 ofJson(LocalDate date, JsonNumber adjClose) {
        BigDecimal bd = adjClose.bigDecimalValue();
        
        if (bd.scale() > MAX_SCALE) {
            bd = bd.setScale(MAX_SCALE, RoundingMode.HALF_EVEN);
        }
        
        return new CustomFastMoneyPrice2(date, bd);
    }
    
    
    /** Treat as final. */
    private LocalDate date;
    
    /** Treat as final. */
    private FastMoney adjClose;
    
    
    
    private CustomFastMoneyPrice2(LocalDate date, BigDecimal adjClose) {
        this(date, FastMoney.of(adjClose, "USD"));
    }
    
    private CustomFastMoneyPrice2(LocalDate date, FastMoney adjClose) {
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
        
        if (obj.getClass() != CustomFastMoneyPrice2.class) {
            return false;
        }
        
        CustomFastMoneyPrice2 that = (CustomFastMoneyPrice2) obj;
        
        return this.date.equals(that.date) &&
               this.adjClose.equals(that.adjClose);
    }
    
    
    
    /*
     *  ----------------------
     * | EXTERNALIZABLE IMPL. |
     *  ----------------------
     */
    
    /** Stay away from me. */
    public CustomFastMoneyPrice2() { }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(LocalDates.toShort(date));
        out.writeLong(MonetaHack.getNumber(adjClose));
        out.writeObject(adjClose.getCurrency().getCurrencyCode());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        date = LocalDates.fromShort(in.readShort());

        long val = in.readLong();
        String currency = (String) in.readObject();

        adjClose = MonetaHack.newFastMoney(val, currency);
    }
}