package com.martinandersson.money.benchmark;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import org.javamoney.moneta.FastMoney;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * If we already have the long number we know {@code FastMoney} store
 * internally, then is it faster to use the public API or reflection to create
 * an instance of FastMoney?<p>
 * 
 * Answer: Reflection is even faster than baseline and about twice as fast as
 * API.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ConstructFastMoneyBenchmark
{
    private Constructor<FastMoney> ctor;
    
    private long val;
    
    private BigDecimal valBd;
    
    private CurrencyUnit currency;
    
    
    
    @Setup
    public void init() throws ReflectiveOperationException {
        ctor = FastMoney.class.getDeclaredConstructor(long.class, CurrencyUnit.class);
        ctor.setAccessible(true);
        
        val = 1612535;
        valBd = new BigDecimal("16.12535");
        
        currency = Monetary.getCurrency("USD");
    }
    
    
    
    /**
     * The baseline is not your average baseline =) It's assuming we have a
     * {@code BigDecimal} and use the appropriate {@code BigDecimal}
     * constructor.
     * 
     * @return a {@code FastMoney}
     */
    @Benchmark
    public FastMoney baseline() {
        return FastMoney.of(valBd, currency);
    }
    
    @Benchmark
    public FastMoney api() {
        BigDecimal bd = BigDecimal.valueOf(val).movePointLeft(5);
        return FastMoney.of(bd, currency);
    }
    
    @Benchmark
    public FastMoney reflection() throws ReflectiveOperationException {
        return ctor.newInstance(val, currency);
    }
}