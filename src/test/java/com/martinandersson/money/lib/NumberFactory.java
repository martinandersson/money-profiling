package com.martinandersson.money.lib;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.json.JsonNumber;

/**
 * Factory of Json numbers.<p>
 * 
 * This type will delegate all number construction to the "real" factory methods
 * used by JSONP - yes, we have a hard coded dependency on JSONP.<p>
 * 
 * If you need "real world data", then use {@link AppleData} instead.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public enum NumberFactory
{
    INT    ("Int",        int.class,    Random::nextInt),
    LONG   ("Long",       long.class,   Random::nextLong),
    DOUBLE ("BigDecimal", double.class, Random::nextDouble);
    
    
    /**
     * JSONP's factory for Json numbers.<p>
     * 
     * JSON 1.1 (JSR-374) will provide createValue() methods on JsonProvider.
     * Until then, we're out of luck and must use reflection if we insist using
     * JSONP for creating the numbers.
     */
    private final Method factory;
    
    private final Class<?> implementation;
    
    private final Function<? super Random, ? extends Number> generator;
    
    
    private <N extends Number> NumberFactory(
            String name, Class<N> argType, Function<? super Random, N> generator)
    {
        this.generator = generator;
        
        try {
            Class<?> c = Class.forName("org.glassfish.json.JsonNumberImpl");
            
            factory = c.getDeclaredMethod("getJsonNumber", argType);
            factory.setAccessible(true);
            
            implementation = Class.forName(c.getName() + '$' +
                    MessageFormat.format("Json{0}Number", name));
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    /**
     * Returns a stream of Json numbers.<p>
     * 
     * The numbers will be randomized.
     * 
     * @return a stream of Json numbers
     */
    public Stream<JsonNumber> numbers() {
        return Stream.generate(this::newNumber);
    }
    
    /**
     * Returns a new Json number.<p>
     * 
     * The number will be randomized.
     * 
     * @return a new Json number
     */
    public JsonNumber newNumber() {
        final JsonNumber num;

        try {
            num = (JsonNumber) factory.invoke(null,
                    generator.apply(ThreadLocalRandom.current()));
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        assert num.getClass().equals(implementation);

        return num;
    }
    
    /**
     * Returns JSONP's implementation class that implements the JsonNumber
     * symbolized by this enum.
     * 
     * @return JSONP's implementation class that implements the JsonNumber
     * symbolized by this enum
     */
    public Class<?> getImplementation() {
        return implementation;
    }
    
    
    static {
        assert uniqueInstances();
    }
    
    /**
     * Returns {@code true} if the factory produce different instances given the
     * same argument, otherwise {@code false}.<p>
     * 
     * We also check the underlying {@code BigDecimal}.<p>
     * 
     * We have no reason to doubt new instances are produced, but our benchmark
     * require this guarantee or we risk having caches fucking with our results.
     * 
     * @return {@code true} if the factory produce different instances given the
     * same argument, otherwise {@code false}
     */
    private static boolean uniqueInstances() {
        for (NumberFactory $this : values()) {
            final Number arg;
            
            switch ($this) {
                case INT:
                    arg = Integer.MAX_VALUE;
                    break;
                case LONG:
                    arg = Long.MAX_VALUE;
                    break;
                case DOUBLE:
                    arg = Double.MAX_VALUE;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            try {
                JsonNumber first  = (JsonNumber) $this.factory.invoke(null, arg),
                           second = (JsonNumber) $this.factory.invoke(null, arg);
                
                assert first != second;
                assert first.bigDecimalValue() != second.bigDecimalValue();
            }
            catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        
        return true;
    }
}