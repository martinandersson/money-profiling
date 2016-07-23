package com.martinandersson.money.benchmark;

import com.martinandersson.money.lib.SystemProperties;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.joining;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Application entry point for JMH benchmarks.<p>
 * 
 * Two system properties are read/used by this class:
 * <ol>
 *   <li>{@link SystemProperties#BENCHMARK_REGEX}</li>
 *   <li>{@link SystemProperties#BENCHMARK_FILE}</li>
 * </ol>
 * 
 * Number of JMH forks used is 1.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class StartJmh
{
    public static void main(String... ignored)
            throws RunnerException, ClassNotFoundException
    {
        ChainedOptionsBuilder b = new OptionsBuilder()
                .include(getRegex())
                .forks(1)
                .addProfiler(MemoryUsageProfiler.class)
                .jvmArgsAppend("-ea")
                //TEST
                .warmupIterations(0)
                .measurementIterations(1);
        
        String file = SystemProperties.BENCHMARK_FILE.get();
        
        if (file != null) {
            b.output(file);
            
            System.out.println("Writing results to file: " +
                    Paths.get(file).toAbsolutePath());
        }
        
        new Runner(b.build()).run();
    }
    
    private static String getRegex() {
        String regex = SystemProperties.BENCHMARK_REGEX.get();
        
        if (regex == null) {
            regex = ".+Benchmark";
        }
        else if (Pattern.matches("\\p{Upper}+", regex)) {
            regex = regex.chars()
                    .mapToObj(x -> String.valueOf((char) x))
                    .collect(joining("\\p{Lower}+", "\\.", "\\p{Lower}+\\."));
        }
        
        return regex;
    }
}