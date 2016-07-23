package com.martinandersson.money.lib.chroniclemap;

import com.martinandersson.money.lib.Numbers;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;
import net.openhft.chronicle.hash.Data;
import net.openhft.chronicle.map.MapMethods;
import net.openhft.chronicle.map.MapQueryContext;
import net.openhft.chronicle.map.ReturnValue;

/**
 * Is an SPI implementation for Chronicle Map that log details about the Map
 * get() and put() operation.<p>
 * 
 * Use {@code toString()} method to figure out what exactly got logged.
 * 
 * @param <K>  Map key type
 * @param <V>  Map value type
 * @param <R>  Chronicle Map say "the return type of MapEntryOperations
 *             specialized for the queried map"
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ChronicleMapLogger<K, V, R> implements MapMethods<K, V, R>
{
    private final SizeLogger size;
    
    private final TimeLogger put,
                             get;
    
    
    public ChronicleMapLogger() {
        size = new SizeLogger();
        put = new TimeLogger();
        get = new TimeLogger();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void put(MapQueryContext<K, V, R> q, Data<V> value, ReturnValue<V> returnValue) {
        put.start();
        MapMethods.super.put(q, value, returnValue);
        put.complete();
        size.record(q.queriedKey().size(), value.size());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void get(MapQueryContext<K, V, R> q, ReturnValue<V> returnValue) {
        get.start();
        MapMethods.super.get(q, returnValue);
        get.complete();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final String P = System.lineSeparator() + "    ";
        
        LongFunction<String> toMb = bytes ->
                " (" + Numbers.roundX(bytes / 1024. / 1024, 3) + " Mb)";
        
        StringBuilder b = new StringBuilder("Map statistics for put():")
                .append(P).append("minKeySize=").append(size.getMinKeySize())
                .append(P).append("maxKeySize=").append(size.getMaxKeySize())
                .append(P).append("avgKeySize=").append(size.avgKeySize())
                .append(P).append("totKeySize=").append(size.getTotKeySize())
                    .append(toMb.apply(size.getTotKeySize()))
                .append(P).append("minValueSize=").append(size.getMinValueSize())
                .append(P).append("maxValueSize=").append(size.getMaxValueSize())
                .append(P).append("avgValueSize=").append(size.avgValueSize())
                .append(P).append("totValueSize=").append(size.getTotValueSize())
                    .append(toMb.apply(size.getTotValueSize()));
        
        return appendTimeMetrics(b, P, put) +
               System.lineSeparator() +
               appendTimeMetrics(new StringBuilder("Map statistics for get():"), P, get);
    }
    
    private static StringBuilder appendTimeMetrics(
            StringBuilder builder, String prefix, TimeLogger time)
    {
        return builder
                .append(prefix).append("minCost=")
                    .append(time.getMinCost()).append(" ns")
                .append(prefix).append("maxCost=")
                    .append(time.getMaxCost()).append(" ns")
                .append(prefix).append("avgTimeCost=")
                    .append(time.avgTimeCost()).append(" ns")
                .append(prefix).append("totTimeSpent=")
                    .append(time.getTotTimeSpent(TimeUnit.MILLISECONDS)).append(" ms");
    }
}