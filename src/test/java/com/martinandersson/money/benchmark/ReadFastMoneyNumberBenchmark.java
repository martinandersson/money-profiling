package com.martinandersson.money.benchmark;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.javamoney.moneta.FastMoney;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * If you'd like to serialize a {@code FastMoney} yourself, then getting hold of
 * the internally used {@code long} number is probably something you'd wanna
 * do.<p>
 * 
 * It can be done using public API, or, Java reflection. Question is, which
 * approach is the fastest?<p>
 * 
 * Fastest on author's machine was reflection (API was almost 4.5 times slower).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ReadFastMoneyNumberBenchmark
{
    private Field FastMoney$number;
    
    private FastMoney money;
    
    
    
    /**
     * Will initialize the fields of this class.<p>
     * 
     * Default level = trial = this method is called once for each new JVM.
     * 
     * @throws ReflectiveOperationException
     *             if we fail to lookup {@code FastMoney#number}
     */
    @Setup
    public void init() throws ReflectiveOperationException {
        money = FastMoney.of(new BigDecimal("1234.56789"), "USD");
        
        FastMoney$number = FastMoney.class.getDeclaredField("number");
        FastMoney$number.setAccessible(true);
    }
    
    
    
    @Benchmark
    public FastMoney baseline() {
        return money;
    }
    
    @Benchmark
    public long api() {
        BigDecimal bd = money.getNumber().numberValue(BigDecimal.class);
        return bd.movePointRight(money.getScale()).longValue();
    }
    
    @Benchmark
    public long reflection() throws ReflectiveOperationException {
        return FastMoney$number.getLong(money);
    }
}