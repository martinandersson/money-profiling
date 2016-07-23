package com.martinandersson.money.benchmark;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.javamoney.moneta.Money;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * If you'd like to serialize a {@code Money} yourself, then getting hold of the
 * internally used {@code BigDecimal} number is probably something you'd wanna
 * do.<p>
 * 
 * It can be done using public API, or, Java reflection. Question is, which
 * approach is the fastest?<p>
 * 
 * Fastest on author's machine was reflection (API was almost 4 times slower).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ReadMoneyNumberBenchmark
{
    private Field Money$number;
    
    private Money money;
    
    
    
    @Setup
    public void init() throws ReflectiveOperationException {
        // Why double constructor? See: http://stackoverflow.com/a/38023232
        money = Money.of(new BigDecimal(1.1), "USD");
        
        Money$number = Money.class.getDeclaredField("number");
        Money$number.setAccessible(true);
    }
    
    
    
    @Benchmark
    public Money baseline() {
        return money;
    }
    
    @Benchmark
    public BigDecimal api() {
        return money.getNumber().numberValue(BigDecimal.class);
    }
    
    @Benchmark
    public BigDecimal reflection() throws ReflectiveOperationException {
        return (BigDecimal) Money$number.get(money);
    }
}