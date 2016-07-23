package com.martinandersson.money.benchmark;

import com.martinandersson.money.lib.LocalDates;
import com.martinandersson.money.lib.NumberFactory;
import com.martinandersson.money.lib.model.FastMoneyPrice;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Offers a number of baseline benchmarks for {@link
 * ChronicleMapRealBenchmark}.<p>
 * 
 * The "baseline baseline" is {@link #createPrice() createPrice()} which simply
 * create a {@code FastMoneyPrice} without storing it anywhere.<p>
 * 
 * Then, three benchmarks are provided that apart from creating a price instance
 * also put, get and remove the price using different Map implementations:
 * 
 * <ul>
 *   <li>{@linkplain #hashMap(Blackhole) hashMap()}</li>
 *   <li>{@linkplain #concurrentHashMap(Blackhole) concurrentHashMap()}</li>
 *   <li>{@linkplain #chronicleMap(Blackhole) chronicleMap()}</li>
 * </ul>
 * 
 * This yield a pretty solid baseline for comparing Chronicle Map (in-memory)
 * with HashMap and ConcurrentHashMap, but also for comparing a "vanilla"
 * Chronicle Map with a Chronicle Map configured to use custom marshallers (see
 * {@link ChronicleMapRealBenchmark}).<p>
 * 
 * On author's machine, the winner was not too surprisingly the "create number"
 * benchmark. A close second was ConcurrentHashMap and a close third was
 * HashMap. "Vanilla" Chronicle Map on the other hand was almost 30 times as
 * slow as ConcurrentHashMap.<p>
 * 
 * On that note, Chronicle Map does make the statement that they are 1) a
 * "drop-in replacement" of ConcurrentHashMap and allegedly, 2) in some cases
 * they "perform better":
 * <pre>
 *   https://github.com/OpenHFT/Chronicle-Map#chronicle-map-3-tutorial
 * </pre>
 * 
 * While this class focus on the time cost of Chronicle Map, {@code
 * ChronicleMapTest} output the actual disk space cost.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ChronicleMapBaselineBenchmark
{
    private LocalDate start;
    
    private Map<LocalDate, FastMoneyPrice> hashMap;
    
    private Map<LocalDate, FastMoneyPrice> concurrentHashMap;
    
    private ChronicleMap<LocalDate, FastMoneyPrice> chronicleMap;
    
    
    
    @Setup
    public void createMap() {
        start = LocalDates.MIN;
        
        hashMap = new HashMap<>();
        
        concurrentHashMap = new ConcurrentHashMap<>();
        
        FastMoneyPrice avg = FastMoneyPrice.ofJson(
                LocalDate.now(), NumberFactory.DOUBLE.newNumber());
        
        chronicleMap = ChronicleMapBuilder.of(LocalDate.class, FastMoneyPrice.class)
                .constantKeySizeBySample(LocalDate.now())
                .averageValue(avg)
                .entries(1)
                .putReturnsNull(true)
                .removeReturnsNull(true)
                .create();
    }
    
    @TearDown
    public void closeMap() {
        chronicleMap.close();
    }
    
    
    
    /**
     * This is the baseline of the baseline and will only create a {@code
     * FastMoneyPrice}.
     * 
     * @return a {@code FastMoneyPrice}
     */
    @Benchmark
    public FastMoneyPrice createPrice() {
        return FastMoneyPrice.ofJson(
                next(), NumberFactory.DOUBLE.newNumber());
    }
    
    /**
     * Will put, get and remove a {@code FastMoneyPrice} using a
     * {@code HashMap}.
     * 
     * @param hole  black hole provided by JHM
     */
    @Benchmark
    public void hashMap(Blackhole hole) {
        runAgainst(hashMap, hole);
    }
    
    /**
     * Will put, get and remove a {@code FastMoneyPrice} using a {@code
     * ConcurrentHashMap}.
     * 
     * @param hole  black hole provided by JHM
     */
    @Benchmark
    public void concurrentHashMap(Blackhole hole) {
        runAgainst(concurrentHashMap, hole);
    }
    
    /**
     * Will put, get and remove a {@code FastMoneyPrice} using a {@code
     * CronicleMap}.
     * 
     * @param hole  black hole provided by JHM
     */
    @Benchmark
    public void chronicleMap(Blackhole hole) {
        runAgainst(chronicleMap, hole);
    }
    
    
    /**
     * Returns a date.<p>
     * 
     * The dates returned from this method is iterated through the interval
     * defined by {@code LocalDates.MIN} and {@code LocalDates.MAX}.
     * 
     * @return a date
     */
    private LocalDate next() {
        try {
            return start;
        }
        finally {
            start = start.equals(LocalDates.MAX) ?
                    LocalDates.MIN :
                    start.plusDays(1);
        }
    }
    
    private void runAgainst(Map<LocalDate, FastMoneyPrice> map, Blackhole hole) {
        LocalDate date = next();
        
        FastMoneyPrice store = createPrice();
        
        map.put(date, store);
        FastMoneyPrice read = map.get(date);
        
        // Prefer NPE:
        assert read.equals(store);
        
        hole.consume(read);
        map.remove(date);
    }
}