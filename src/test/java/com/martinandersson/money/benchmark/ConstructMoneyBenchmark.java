package com.martinandersson.money.benchmark;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import org.javamoney.moneta.Money;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * If performance matter, how would you create an instance of {@code Money}?<p>
 * 
 * The natural way is to use a {@code Money} constructor. We could also use
 * reflection.<p>
 * 
 * Winner: Reflection (on my machine). But barely. So I would personally advise
 * to always use the constructor.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ConstructMoneyBenchmark
{
    private Constructor<Money> ctor;
    
    private BigDecimal val;
    
    private CurrencyUnit currency;
    
    
    
    /**
     * Will initialize the fields of this class.<p>
     * 
     * Default level = trial = this method is called once for each new JVM.
     * 
     * @throws ReflectiveOperationException
     *             if we fail to lookup the {@code Money} constructor
     */
    @Setup
    public void init() throws ReflectiveOperationException {
        ctor = Money.class.getDeclaredConstructor(BigDecimal.class, CurrencyUnit.class);
        ctor.setAccessible(true);
        
        val = new BigDecimal(1.1);
        
        currency = Monetary.getCurrency("USD");
    }
    
    
    
    @Benchmark
    public BigDecimal baseline() {
        return val;
    }
    
    @Benchmark
    public Money api() {
        return Money.of(val, currency);
    }
    
    @Benchmark
    public Money reflection() throws ReflectiveOperationException {
        return ctor.newInstance(val, currency);
    }
}