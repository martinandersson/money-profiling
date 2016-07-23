package com.martinandersson.money.benchmark;

import com.martinandersson.money.lib.LocalDates;
import com.martinandersson.money.lib.NumberFactory;
import com.martinandersson.money.lib.chroniclemap.ChronicleMapMarshaller;
import com.martinandersson.money.lib.model.FastMoneyPrice;
import com.martinandersson.money.lib.serializer.SerializationFramework;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks Chronicle Map using different serialization frameworks.<p>
 * 
 * The benchmark provided in this class work much like {@link
 * ChronicleMapBaselineBenchmark} except that this class configure Chronicle Map
 * to use a custom marshaller that delegate the serialization/deserialization
 * to different serialization frameworks provided by {@link
 * SerializationFramework}.<p>
 * 
 * On author's machine, the results of {@link SerializationFramework#JAVA} was
 * almost [as expected] identical to {@link
 * ChronicleMapBaselineBenchmark#chronicleMap(org.openjdk.jmh.infra.Blackhole) ChronicleMapBaselineBenchmark.chronicleMap(Blackhole)}.
 * One interested conclusion has to be that the act of using a custom marshaller
 * has no notable impact.<p>
 * 
 * However, the other serialization frameworks vastly reduced the time cost.
 * {@link SerializationFramework#KRYO_CUSTOM} was the fastest one and "only" 3.8
 * times slower than {@link
 * ChronicleMapBaselineBenchmark#concurrentHashMap(org.openjdk.jmh.infra.Blackhole) ChronicleMapBaselineBenchmark.concurrentHashMap(Blackhole)}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ChronicleMapRealBenchmark
{
    @Param
    private SerializationFramework serializer;
    
    private LocalDate start;
    
    private ChronicleMap<LocalDate, FastMoneyPrice> map;
    
    
    
    @Setup
    public void createMap() {
        start = LocalDates.MIN;
        
        FastMoneyPrice avg = FastMoneyPrice.ofJson(
                LocalDate.now(), NumberFactory.DOUBLE.newNumber());
        
        ChronicleMapMarshaller<LocalDate> keyMarshaller = new ChronicleMapMarshaller<>(serializer);
        
        @SuppressWarnings("unchecked")
        ChronicleMapMarshaller<FastMoneyPrice> valueMarshaller
                = (ChronicleMapMarshaller<FastMoneyPrice>) (ChronicleMapMarshaller) keyMarshaller;
        
        map = ChronicleMapBuilder.of(LocalDate.class, FastMoneyPrice.class)
                .keyMarshaller(keyMarshaller)
                .valueMarshaller(valueMarshaller)
                .constantKeySizeBySample(LocalDate.now())
                .averageValue(avg)
                .entries(1)
                .putReturnsNull(true)
                .removeReturnsNull(true)
                .create();
    }
    
    @TearDown
    public void closeMap() {
        map.close();
    }
    
    
    
    @Benchmark
    public void benchmark(Blackhole hole) {
        LocalDate date = next();
        
        FastMoneyPrice store = FastMoneyPrice.ofJson(
                date, NumberFactory.DOUBLE.newNumber());
        
        map.put(date, store);
        FastMoneyPrice read = map.get(date);
        
        // Prefer NPE:
        assert read.equals(store);
        
        hole.consume(read);
        map.remove(date);
    }
    
    
    
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
}