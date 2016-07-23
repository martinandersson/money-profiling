package com.martinandersson.money.lib;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import org.javamoney.moneta.FastMoney;
import org.javamoney.moneta.Money;

/**
 * Provide access to internal details of Moneta's {@code Money} and {@code
 * FastMoney}.<p>
 * 
 * This API is the result of the following benchmarks:
 * 
 * <ul>
 *   <li>{@link com.martinandersson.money.benchmark.ReadMoneyNumberBenchmark}</li>
 *   <li>{@link com.martinandersson.money.benchmark.ReadFastMoneyNumberBenchmark}</li>
 *   <li>{@link com.martinandersson.money.benchmark.ConstructMoneyBenchmark}</li>
 *   <li>{@link com.martinandersson.money.benchmark.ConstructFastMoneyBenchmark}</li>
 * </ul>
 * 
 * If we want to access the internally stored number in Moneta's two
 * implementations of {@code MonetaryAmount}, then we could either use the
 * public API or Java reflection to get it.<p>
 * 
 * It was "proven" that for both implementations, we gain a "substantial"
 * performance gain by using Java reflection. Hence, this class export two
 * methods that access the number field using Java reflection.<p>
 * 
 * If we want to construct an instance of any of the two implementations, it was
 * "proven" that we saw no substantial performance gain using Java reflection
 * versus the constructor for {@code Money}, but we did gain a lot using
 * reflection when constructing {@code FastMoney}. Hence, this class export just
 * one method to construct a new {@code FastMoney} but does not offer a method
 * for constructing {@code Money}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class MonetaHack
{
    private MonetaHack() {
        // Empty
    }
    
    
    
    private static final Field NUMBER_M,
                               NUMBER_FM;
    
    private static final Constructor<FastMoney> CTOR;
    
    static {
        try {
            NUMBER_M = Money.class.getDeclaredField("number");
            NUMBER_M.setAccessible(true);
            
            NUMBER_FM = FastMoney.class.getDeclaredField("number");
            NUMBER_FM.setAccessible(true);
            
            CTOR = FastMoney.class.getDeclaredConstructor(long.class, CurrencyUnit.class);
            CTOR.setAccessible(true);
        }
        catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    /**
     * Returns the internally stored {@code BigDecimal} of the specified {@code
     * money}.<p>
     * 
     * @implNote
     * This implementation uses Java reflection.
     * 
     * @param money  which {@code Money} instance to read
     * 
     * @return the internally stored {@code BigDecimal} of the specified {@code
     * money}
     * 
     * @see MonetaHack
     */
    public static BigDecimal getNumber(Money money) {
        try {
            return (BigDecimal) NUMBER_M.get(money);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns the internally stored {@code long} of the specified {@code
     * money}.
     * 
     * @implNote
     * This implementation uses Java reflection.
     * 
     * @param money  which {@code FastMoney} instance to read
     * 
     * @return the internally stored {@code long} of the specified {@code
     * money}
     * 
     * @see MonetaHack
     */
    public static long getNumber(FastMoney money) {
        try {
            return NUMBER_FM.getLong(money);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns a new instance of {@code FastMoney}.
     * 
     * @implNote
     * This implementation uses Java reflection.
     * 
     * @param val           internally stored number
     * @param currencyCode  currency code
     * 
     * @return a new instance of {@code FastMoney}
     */
    public static FastMoney newFastMoney(long val, String currencyCode) {
        try {
            return CTOR.newInstance(val, Monetary.getCurrency(currencyCode));
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}