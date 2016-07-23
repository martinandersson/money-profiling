package com.martinandersson.money.lib.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.martinandersson.money.lib.LocalDates;
import com.martinandersson.money.lib.MonetaHack;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.javamoney.moneta.FastMoney;
import org.javamoney.moneta.Money;

/**
 * Serializers for Kryo.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class KryoSerializers
{
    private KryoSerializers() {
        // Empty
    }
    
    
    
    /**
     * Will hookup the specified {@code kryo} thing with all serializers in
     * this class.
     * 
     * @param kryo  kryo instance
     * 
     * @return same kryo instance
     */
    public static Kryo hookup(Kryo kryo) {
        kryo.register(LocalDate.class,  LOCAL_DATE);
        kryo.register(BigDecimal.class, BIG_DECIMAL);
        kryo.register(Money.class,      MONEY);
        kryo.register(FastMoney.class,  FAST_MONEY);
        
        return kryo;
    }
    
    
    
    public static final Serializer<LocalDate> LOCAL_DATE = new Serializer<LocalDate>(false, true) {
        @Override
        public void write(Kryo kryo, Output out, LocalDate date) {
            out.writeShort(LocalDates.toShort(date));
        }
        
        @Override
        public LocalDate read(Kryo kryo, Input in, Class<LocalDate> type) {
            return LocalDates.fromShort(in.readShort());
        }
    };
    
    public static final Serializer<BigDecimal> BIG_DECIMAL
            = new DefaultSerializers.BigDecimalSerializer();
    
    public static final Serializer<Money> MONEY = new Serializer<Money>(false, true) {
        @Override
        public void write(Kryo kryo, Output out, Money money) {
            BIG_DECIMAL.write(kryo, out, MonetaHack.getNumber(money));
            out.writeString(money.getCurrency().getCurrencyCode());
        }
        
        @Override
        public Money read(Kryo kryo, Input in, Class<Money> type) {
            BigDecimal number = BIG_DECIMAL.read(kryo, in, BigDecimal.class);
            String currency = in.readString();
            return Money.of(number, currency);
        }
    };
    
    public static final Serializer<FastMoney> FAST_MONEY = new Serializer<FastMoney>(false, true) {
        @Override
        public void write(Kryo kryo, Output out, FastMoney money) {
            out.writeLong(MonetaHack.getNumber(money), true);
            out.writeString(money.getCurrency().getCurrencyCode());
        }
        
        @Override
        public FastMoney read(Kryo kryo, Input in, Class<FastMoney> type) {
            long number = in.readLong(true);
            String currency = in.readString();
            return MonetaHack.newFastMoney(number, currency);
        }
    };
}
