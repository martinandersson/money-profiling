package com.martinandersson.money.benchmark;

import java.util.Collection;
import java.util.Collections;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.profile.ProfilerResult;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;

/**
 * Will log the memory used after each iteration.<p>
 * 
 * The result (memory consumed) is not something I would take too seriously.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class MemoryUsageProfiler implements InternalProfiler
{
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Memory usage profiling";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeIteration(BenchmarkParams bmParams, IterationParams iParams) {
        // Empty
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Result> afterIteration(
            BenchmarkParams bmParams,
            IterationParams iParams,
            IterationResult result)
    {
        ProfilerResult res = new ProfilerResult(
                "memory used",
                getMemoryUsed() / 1024. / 1024.,
                "MB",
                AggregationPolicy.AVG);
        
        return Collections.singleton(res);
    }
    
    private static long getMemoryUsed() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }
}