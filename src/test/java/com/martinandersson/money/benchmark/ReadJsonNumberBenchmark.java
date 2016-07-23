package com.martinandersson.money.benchmark;

import com.martinandersson.money.lib.NumberFactory;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import javax.json.JsonNumber;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Will profile different ways of acquiring/reading the numeric value from a
 * {@code JsonNumber}<p>
 * 
 * More specifically, this benchmark will profile how to get the numeric value
 * from the reference implementation "JSON Processing" (JSONP). The API (JSR
 * 353) provide no elegant way to do so.<p>
 * 
 * The source code of the reference implementation is located here:
 * <pre>
 *   https://java.net/projects/jsonp/sources/git/content/impl/src/main/java/org/glassfish/json/JsonNumberImpl.java
 * </pre>
 * 
 * If you study this source code file, you shall find that JSONP has only three
 * implementations that internally wrap three different number types: int, long
 * and BigDecimal<sup>1</sup>.<p>
 * 
 * High-performance code will most likely prefer to deal with an int before a
 * long, and a long before a BigDecimal. There's not much point in using a
 * BigDecimal unless we have to.<p>
 * 
 * The API itself has "BigDecimal semantics" written all over it and is not
 * very useful for us. In fact, both API as well as the reference implementation
 * is mind-boggling stupid (my humor). For example, JavaDoc of
 * {@code JsonNumber.isIntegral()} as well as JSONP's implementation do this:
 * <pre>
 *   bd.scale() == 0;
 * </pre>
 * 
 * A less mind-boggling stupid implementation would have been something more
 * like this:
 * <pre>
 *   bd.signum() == 0 || bd.scale() {@literal <}= 0  || bd.stripTrailingZeros().scale() {@literal <}= 0;
 * </pre>
 * 
 * This check risk creating an extra BigDecimal object but at least it will
 * answer the question truthfully and client now have a real option - moving
 * forward - to only use a primitive type versus being forced to use a complex
 * type (BigDecimal)<sup>2</sup>. If the number is integral, then it should
 * "most likely" fit an int or a long.<p>
 * 
 * Our real problem, however, is that JSONP's implementation that store a long
 * under the cover will go the long way through a BigDecimal if you ask for an
 * int value instead of using {@code Math.toIntExact()}<sup>3</sup>.<p>
 * 
 * At the end of the day, there is no way to gracefully ask for an int or a long
 * without also risk having a BigDecimal constructed under the cover.<p>
 * 
 * So what can we do about it? The benchmarks in this class profile a few
 * alternatives I came up with. They are grouped in two categories: "api" and
 * "impl".<p>
 * 
 * "Api" benchmarks try to work their magic using the JSON API only. "Impl"
 * benchmarks are allowed to (and will) query for the implementation one way or
 * the other in order to make a smart method dispatch. Please feel free to GIT
 * push if you have any other ideas.
 * 
 * 
 * 
 * <h3>Results</h3>
 * 
 * Results on author's machine showed that {@link #api_trycatch(Blackhole)
 * api_trycatch()} was the fastest alternative for int numbers. But if the
 * internal number was a long or a BigDecimal, then "try-catch" fell far behind
 * and was many times slower than the next worst alternative.<p>
 * 
 * {@link #impl_refcheck(Blackhole) impl_refcheck()} was almost as fast as
 * "try-catch" for ints, and it was the fastest alternative for longs.<p>
 * 
 * {@link #api_bigdecimal(Blackhole) api_bigdecimal()} was the fastest
 * alternative for doubles, but "ref-check" wasn't far behind.<p>
 * 
 * I have to proclaim the winner to be "ref-check" which will almost be as fast
 * as "bigdecimal" for doubles, but will yield better performance than
 * "bigdecimal" in terms of speed and garbage for ints and longs.
 * 
 * 
 * 
 * <h3>Notes</h3>
 * 
 * 1: As is easy to understand, the underlying source is pure text. If this is a
 * "floating-point" value, The BigDecimal is going to "exactly" represent the
 * floating-point value read. For example, "0.1" and not the double value
 * 0.1d which cannot be represented exactly.<p>
 * 
 * 2: This evaluates to {@code true}:
 * <pre>
 *     new BigDecimal(1).equals(new BigDecimal(1.0D))
 * </pre>
 * 
 * In this case, both instances has precision = 1 and scale = 0. I.e.,
 * JavaDoc and JSONP both claim both instances are integral and indeed,
 * both instances can be used to retrieve the exact int value without exceptions
 * fucking us up.<p>
 * 
 * However, this evaluates to {@code false}:
 * <pre>
 *     new BigDecimal(1).equals(new BigDecimal("1.0"))
 * </pre>
 * 
 * In full compliance with BigDecimal JavaDoc for the String accepting
 * constructor, this cause precision to go from 1 to 2, and scale from 0
 * to 1.<p>
 * 
 * Thusly, for a BigDecimal constructed from the String "1.0", JavaDoc
 * and JSONP would both say this guy is NOT integral. However, {@code
 * intValueExact()} will still be able to produce the exact int value 1 without
 * any fuss.<p>
 * 
 * 3: See https://java.net/jira/browse/JSONP-35
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ReadJsonNumberBenchmark
{
    private static final String UNKNOWN_JSON_IMPL
            = "This benchmark depend on the JSON reference implementation.";
    
    
    
    @Param
    private NumberFactory factory;
    
    private JsonNumber number;
    
    
    
    @Setup(Level.Invocation)
    public void init() {
        number = factory.newNumber();
    }
    
    
    
    /**
     * Baseline return {@code number}.<p>
     * 
     * All other benchmarks in this class will try different ways to figure out
     * what underlying number implementation we are working with.
     * 
     * @return {@code number}
     */
    @Benchmark
    public JsonNumber baseline() {
        return number;
    }
    
    /**
     * This guy features tons of try-catches in order to desperately night hack
     * his way to success.
     * 
     * @param hole  black hole provided by JHM
     */
    @Benchmark
    public void api_trycatch(Blackhole hole) {
        /*
         * Why consume the number?
         * 
         * Without it, other benchmarks will turn out to be faster than baseline
         * itself for certain number types. This can only be attribute to a
         * difference in cost of different consume() overloads.
         * 
         * By consuming the number, we force all benchmarks to share at least to
         * some extent the same overhead and become more comparable.
         */
        hole.consume(number);
        
        final long l;

        try {
            // Try long
            l = number.longValueExact();
        }
        catch (ArithmeticException e) {
            // Can't get long, accept BigDecimal
            hole.consume(number.bigDecimalValue());
            return;
        }

        try {
            // Try int
            hole.consume(Math.toIntExact(l));
        }
        catch (ArithmeticException e) {
            // Can't get int, accept BigDecimal
            hole.consume(l);
        }
    }
    
    /**
     * This guy has lost hope and will always call for the BigDecimal.
     * 
     * @param hole  black hole provided by JHM
     * 
     * @return a BigDecimal
     */
    @Benchmark
    public BigDecimal api_bigdecimal(Blackhole hole) {
        hole.consume(number);
        
        return number.bigDecimalValue();
    }
    
    /**
     * This guy compare actual implementation classes and call methods
     * accordingly.<p>
     * 
     * Please note that the implementation classes are cached as I suspect that
     * is exactly what would happen in the real life too.
     * 
     * @param hole  black hole provided by JHM
     */
    @Benchmark
    public void impl_refcheck(Blackhole hole) {
        hole.consume(number);
        
        final Class<?> c = number.getClass();

        if (c == NumberFactory.INT.getImplementation()) {
            // Why the "exact"? Better safe than sorry.
            hole.consume(number.intValueExact());
        }
        else if (c == NumberFactory.LONG.getImplementation()) {
            hole.consume(number.longValueExact());
        }
        else if (c == NumberFactory.DOUBLE.getImplementation()) {
            hole.consume(number.bigDecimalValue());
        }
        else {
            throw new AssertionError(UNKNOWN_JSON_IMPL);
        }
    }
    
    /**
     * This is a variation of the former where we simply pull a char from the
     * class name and make irresponsible assumptions.
     * 
     * @param hole  black hole provided by JHM
     */
    @Benchmark
    public void impl_charcheck_switch(Blackhole hole) {
        hole.consume(number);
        
        switch (number.getClass().getSimpleName().charAt(4)) {
            case 'I':
                hole.consume(number.intValueExact());
                break;
            case 'L':
                hole.consume(number.longValueExact());
                break;
            case 'B':
                hole.consume(number.bigDecimalValue());
                break;
            default:
                throw new AssertionError(UNKNOWN_JSON_IMPL);
        }
    }
    
    /**
     * This is an alternative of the former where we have replaced the
     * switch-block with tons of if-else instead.
     * 
     * @param hole  black hole provided by JHM
     */
    @Benchmark
    public void impl_charcheck_ifelse(Blackhole hole) {
        hole.consume(number);
        
        char c = number.getClass().getSimpleName().charAt(4);

        if (c == 'I') {
            hole.consume(number.intValueExact());
        }
        else if (c == 'L') {
            hole.consume(number.longValueExact());
        }
        else if (c == 'B') {
            hole.consume(number.bigDecimalValue());
        }
        else {
            throw new AssertionError(UNKNOWN_JSON_IMPL);
        }
    }
}