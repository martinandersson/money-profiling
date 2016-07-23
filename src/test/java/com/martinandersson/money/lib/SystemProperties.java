package com.martinandersson.money.lib;

import java.text.MessageFormat;
import static java.util.Objects.requireNonNull;

/**
 * API of system properties, turning loose Strings into strongly-typed members
 * of this enum class.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public enum SystemProperties
{
    /**
     * {@code StartJmh} that launches JMH benchmarks use this property to parse
     * an optional regex that specify which benchmarks to run.<p>
     * 
     * The property key is "r" and the property is optional. The value is a
     * regex passed to JMH as-is (except for one case, see next paragraph). If a
     * regex is not provided, all benchmarks will run.<p>
     * 
     * I am not sure what exactly the regex is being matched
     * against<sup>1</sup>. JMH has very limited documentation.<p>
     * 
     * If the "regex" is made of all uppercase alphabetic characters, then the
     * String will be rebuilt to a regex that will match all benchmarks in class
     * names that has each letter in it.<p>
     * 
     * This yield a practical shorthand for how to refer to a specific benchmark
     * class. For example, "CMRB" will run
     * <strong>C</strong>hronicle<strong>M</strong>ap<strong>R</strong>eal<strong>B</strong>enchmark
     * and nothing else.<p>
     * 
     * To be perfectly clear, "CMRB" will be transformed to
     * "\\.C\\p{Lower}+M\\p{Lower}+R\\p{Lower}+B\\p{Lower}+\\.".
     * 
     * <h3>Notes</h3>
     * It's probably class qualified name + "." + method. See implementation of
     * method {@code getUsername()} in this file:
     * <pre>
     *   http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-core/src/main/java/org/openjdk/jmh/runner/BenchmarkListEntry.java
     * </pre>
     */
    BENCHMARK_REGEX ("r", "benchmark regex"),
    
    /**
     * {@code StartJmh} that launches JMH benchmarks use this property to
     * redirect the output of JMH to a file.<p>
     * 
     * The property key is "f" and the property is optional.<p>
     * 
     * Setting this property will also make JMH's output to the console go away.
     */
    BENCHMARK_FILE ("f", "benchmark file"),
    
    /**
     * Represents a path to the common resource directory where resources are
     * put.<p>
     * 
     * The value of this property is set by the Gradle build script.
     */
    GRADLE_RESOURCE_DIR ("gradle.resource.dir", "resource directory"),
    
    /**
     * Represents a path to a temporary directory where test classes can put
     * stuff.<p>
     * 
     * The value of this property is set by the Gradle build script.
     */
    GRADLE_TEST_TEMP_DIR ("gradle.test.temp.dir", "temp directory for tests");
    
    
    
    private final String prop,
                         description;
    
    
    
    private SystemProperties(String prop, String description) {
        this.prop = prop;
        this.description = description;
    }
    
    
    /**
     * Returns the value of this system property, or {@code null} if it has not
     * been set.
     * 
     * @return the value of this system property, or {@code null} if it has not
     * been set
     */
    public String get() {
        return System.getProperty(prop);
    }
    
    /**
     * Returns the value of this system property.
     * 
     * @return the value of this system property
     * 
     * @throws NullPointerException  if the system property has not been set
     */
    public String require() {
        return requireNonNull(get(), () -> MessageFormat.format(
                "Failed to lookup {0}.", description));
    }
}