package com.martinandersson.money.unittest;

import com.martinandersson.money.lib.AppleData;
import com.martinandersson.money.lib.model.BigDecimalPrice;
import com.martinandersson.money.lib.chroniclemap.ChronicleMapMarshaller;
import com.martinandersson.money.lib.model.CustomFastMoneyPrice1;
import com.martinandersson.money.lib.model.CustomFastMoneyPrice2;
import com.martinandersson.money.lib.model.DoublePrice;
import com.martinandersson.money.lib.model.FastMoneyPrice;
import com.martinandersson.money.lib.model.MoneyPrice;
import com.martinandersson.money.lib.model.Price;
import com.martinandersson.money.lib.chroniclemap.ChronicleMapLogger;
import com.martinandersson.money.lib.serializer.SerializationFramework;
import com.martinandersson.money.lib.SystemProperties;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.invoke.SerializedLambda;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.json.JsonNumber;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Will unit test the full life-cycle of a Chronicle Map backed by a file on
 * disk.<p>
 * 
 * Most test methods will build a map, read all {@link AppleData} and put these
 * entities in a Chronicle Map. Then, read all entities back from the map.<p>
 * 
 * Each test print statistics in terms of key/value byte sizes, time cost for
 * put() and get(), but most importantly (for me at least), the actual file size
 * that results from serializing all 8961 prices contained in {@link
 * AppleData}.<p>
 * 
 * Parameterized test methods that accept a {@code SerializationFramework} are
 * split based on {@code Price} implementation.<p>
 * 
 * Chronicle Map can be configured to use a custom marshaller for keys and
 * values. Parameterized tests are executed for each serialization framework
 * used in this project (see {@link #serializer() serializer()}).<p>
 * 
 * JavaDoc that refer to "worst" versus "best" serialization framework judge
 * file size only (as noted on my machine). For all price implementations, not
 * using a serialization framework (Chronicle Map default) used most disk space.
 * The winner was Kryo (with custom Kryo serializers).<p>
 * 
 * Time cost is irrelevant for the purpose of comparison, but speaking of file
 * sizes, it might be interesting to know that I run Windows 10, NTFS.<p>
 * 
 * Please note that time performance of Chronicle Map is more accurately
 * benchmarked in {@code ChronicleMapBaselineBenchmark} and {@code
 * ChronicleMapRealBenchmark}.<p>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ChronicleMapTest
{
    private static final String PROVIDER = "provider";
    
    /**
     * TestNG data provider that provide all serializers being used.<p>
     * 
     * Apart from the enum literals declared in {@code SerializationFramework},
     * we also use {@code null} which will instruct the test to let Chronicle
     * Map do his own serialization (no marshaller provided to Chronicle Map).
     * For all others, the serialization is delegated to a specific
     * serialization framework.
     * 
     * @return all serializers
     */
    @DataProvider(name = PROVIDER)
    private static SerializationFramework[][] serializer() {
        Stream<SerializationFramework> firstNull = Arrays.stream(new SerializationFramework[]{null}),
                           restValid = Arrays.stream(SerializationFramework.values());
        
        return Stream.concat(firstNull, restValid)
                .map(s -> new SerializationFramework[]{s})
                .toArray(SerializationFramework[][]::new);
    }
    
    
    
    /**
     * Serializer used.<p>
     * 
     * May be {@code null} in which case no custom marshaller is provided to
     * Chronicle Map.
     */
    private SerializationFramework serializer;
    
    /**
     * Real [temporary] file used by Chronicle Map for persistence.
     */
    private Path file;
    
    /**
     * Will log byte sizes of key/value and cost of Map operations.
     */
    private ChronicleMapLogger<LocalDate, ? extends Price, ?> mapLogger;
    
    
    
    @BeforeMethod
    public void before_initialize(Object[] testArgs) {
        if (testArgs.length == 1) {
            serializer = (SerializationFramework) testArgs[0];
        }
        else if (testArgs.length > 1) {
            throw new AssertionError(
                    "Unexpected. Either no arg, or just 1 serializer.");
        }
        
        mapLogger = new ChronicleMapLogger<>();
    }
    
    @AfterMethod
    private void after_deleteTempFile() throws IOException {
        if (file == null) {
            return;
        }
        
        Files.deleteIfExists(file);
    }
    
    
    
    /**
     * Packages all parameters that goes into the build of a Chronicle Map.<p>
     * 
     * ..and run the test in {@code run()}.<p>
     * 
     * The only purpose of this class is to reduce code repetition. The actual
     * build methods are defined in the enclosing class and has names that begin
     * with "build".
     * 
     * This test class could be completed removed if we accept some boiler plate
     * to be copy-pasted in the test methods instead. For example, {@code
     * test_double()} could be written something like this:
     * <pre>{@code
     * 
     *    try (ChronicleMap<LocalDate, DoublePrice> map = buildMapConstant(DoublePrice.EXACT_SIZE)) {
     *        AppleData.rowsAs(DoublePrice::ofJson)
     *                .forEach(p -> map.put(p.getDate(), p));
     *    }
     *    
     *    try (ChronicleMap<LocalDate, DoublePrice> map = buildMapConstant(DoublePrice.EXACT_SIZE)) {
     *        assertMap(map, DoublePrice::ofJson);
     *    }
     *  
     *    printFileSize(DoublePrice.class);
     *    printMapStatistics();
     * }</pre>
     */
    class TestSpecification<P extends Price>
    {
        private final Class<P> priceType;
        
        private BiFunction<LocalDate, JsonNumber, P> converter;
        
        private P averageValue;
        
        private P constantValue;
        
        
        public TestSpecification(Class<P> priceType) {
            this.priceType = priceType;
        }
        
        public TestSpecification<P> converter(BiFunction<LocalDate, JsonNumber, P> converter) {
            this.converter = converter;
            return this;
        }
        
        public TestSpecification<P> averageSize(P sample) {
            averageValue = sample;
            return this;
        }
        
        public TestSpecification<P> constantSize(P sample) {
            constantValue = sample;
            return this;
        }
        
        
        public void run() {
            final Supplier<ChronicleMap<LocalDate, P>> map;
            
            if (averageValue != null) {
                map = () -> buildMapAverage(averageValue);
            }
            else if (constantValue != null) {
                map = () -> buildMapConstant(constantValue);
            }
            else {
                throw new IllegalStateException("Set a size sample.");
            }
            
            try (ChronicleMap<LocalDate, P> m = map.get()) {
                AppleData.rowsAs(converter)
                        .forEach(p -> m.put(p.getDate(), p));
            }
            
            try (ChronicleMap<LocalDate, P> m = map.get()) {
                assertMap(m, converter);
            }
            
            printFileSize(priceType);
            printMapStatistics();
        }
    }
    
    
    
    /*
     *  -------
     * | TESTS |
     *  -------
     */
    
    /**
     * Author's output:
     * <pre>
     *   Wrote 0 Dummy's. File size: 1417216 bytes = 1.3515625 Mb.
     * </pre>
     * 
     * I guess this is what could be called a vanilla overhead.
     */
    @Test
    public void test_writeNothing_dummy() {
        Price dummy = new Dummy();
        
        try (ChronicleMap<LocalDate, Price> map = buildMapConstant(dummy)) {
            // Empty
        }
        
        printFileSize(0, Dummy.class);
    }
    
    /**
     * Author's output:
     * <pre>
     *   Wrote 0 SerializedLambda's. File size: 6942720 bytes = 6.62109375 Mb.
     * </pre>
     * 
     * Please note that this is a significantly larger size than what any other
     * test in this class has managed to produce!<p>
     * 
     * BTW, this method serve no other point than being play code.
     */
    @Test
    public void test_writeNothing_lambda() {
        Price dummy = (Price & Serializable) LocalDate::now;
        
        try (ChronicleMap<LocalDate, Price> map = buildMapConstant(dummy)) {
            // Empty
        }
        
        printFileSize(0, SerializedLambda.class);
    }
    
    /**
     * Worst:
     * <pre>
     *   --- Chronicle Map's default ---
     *   Wrote 8961 DoublePrice's. File size: 2265088 bytes = 2.16015625 Mb.
     *   Map statistics for put():
     *       minKeySize=44
     *       maxKeySize=44
     *       avgKeySize=44
     *       totKeySize=394284 (0.376 Mb)
     *       minValueSize=158
     *       maxValueSize=158
     *       avgValueSize=158
     *       totValueSize=1415838 (1.350 Mb)
     *       minCost=4737 ns
     *       maxCost=2474347 ns
     *       avgTimeCost=13733 ns
     *       totTimeSpent=123 ms
     *   Map statistics for get():
     *       minCost=19343 ns
     *       maxCost=1657591 ns
     *       avgTimeCost=33807 ns
     *       totTimeSpent=302 ms
     * </pre>
     * 
     * Best:
     * <pre>
     *   --- Kryo Custom ---
     *   Wrote 8961 DoublePrice's. File size: 299008 bytes = 0.28515625 Mb.
     *   Map statistics for put():
     *       minKeySize=4
     *       maxKeySize=4
     *       avgKeySize=4
     *       totKeySize=35844 (0.034 Mb)
     *       minValueSize=13
     *       maxValueSize=13
     *       avgValueSize=13
     *       totValueSize=116493 (0.111 Mb)
     *       minCost=1579 ns
     *       maxCost=773727 ns
     *       avgTimeCost=2663 ns
     *       totTimeSpent=23 ms
     *   Map statistics for get():
     *       minCost=1579 ns
     *       maxCost=929262 ns
     *       avgTimeCost=3091 ns
     *       totTimeSpent=27 ms
     * </pre>
     * 
     * @param alreadySet  provided by TestNG (set in {@code @BeforeMethod})
     */
    @Test(dataProvider = PROVIDER)
    public void test_double(SerializationFramework alreadySet) {
        new TestSpecification<>(DoublePrice.class)
                .constantSize(DoublePrice.EXACT_SIZE)
                .converter(DoublePrice::ofJson)
                .run();
    }
    
    /**
     * Worst:
     * <pre>
     *   --- Chronicle Map's default ---
     *   Wrote 8961 BigDecimalPrice's. File size: 6967296 bytes = 6.64453125 Mb.
     *   Map statistics for put():
     *       minKeySize=44
     *       maxKeySize=44
     *       avgKeySize=44
     *       totKeySize=394284 (0.376 Mb)
     *       minValueSize=467
     *       maxValueSize=471
     *       avgValueSize=471
     *       totValueSize=4220377 (4.025 Mb)
     *       minCost=5921 ns
     *       maxCost=2576194 ns
     *       avgTimeCost=16909 ns
     *       totTimeSpent=151 ms
     *   Map statistics for get():
     *       minCost=34344 ns
     *       maxCost=3153726 ns
     *       avgTimeCost=82729 ns
     *       totTimeSpent=741 ms
     * </pre>
     * 
     * Best:
     * <pre>
     *   --- Kryo Custom ---
     *   Wrote 8961 BigDecimalPrice's. File size: 385024 bytes = 0.3671875 Mb.
     *   Map statistics for put():
     *       minKeySize=4
     *       maxKeySize=4
     *       avgKeySize=4
     *       totKeySize=35844 (0.034 Mb)
     *       minValueSize=11
     *       maxValueSize=15
     *       avgValueSize=15
     *       totValueSize=134217 (0.128 Mb)
     *       minCost=1184 ns
     *       maxCost=677406 ns
     *       avgTimeCost=2314 ns
     *       totTimeSpent=20 ms
     *   Map statistics for get():
     *       minCost=1184 ns
     *       maxCost=1290071 ns
     *       avgTimeCost=2498 ns
     *       totTimeSpent=22 ms
     * </pre>
     * 
     * @param alreadySet  provided by TestNG
     */
    @Test(dataProvider = PROVIDER)
    public void test_bigDecimal(SerializationFramework alreadySet) {
        new TestSpecification<>(BigDecimalPrice.class)
                .averageSize(BigDecimalPrice.getAverageSize(AppleData.getAverage()))
                .converter(BigDecimalPrice::ofJson)
                .run();
    }
    
    /**
     * Worst:
     * <pre>
     *   --- Chronicle Map's default ---
     *   Wrote 8961 MoneyPrice's. File size: 19062784 bytes = 18.1796875 Mb.
     *   Map statistics for put():
     *       minKeySize=44
     *       maxKeySize=44
     *       avgKeySize=44
     *       totKeySize=394284 (0.376 Mb)
     *       minValueSize=1360
     *       maxValueSize=1364
     *       avgValueSize=1364
     *       totValueSize=12222550 (11.656 Mb)
     *       minCost=5921 ns
     *       maxCost=2518954 ns
     *       avgTimeCost=20360 ns
     *       totTimeSpent=182 ms
     *   Map statistics for get():
     *       minCost=69872 ns
     *       maxCost=7968596 ns
     *       avgTimeCost=145671 ns
     *       totTimeSpent=1305 ms
     * </pre>
     * 
     * Best:
     * <pre>
     *   --- Kryo Custom ---
     *   Wrote 8961 MoneyPrice's. File size: 405504 bytes = 0.38671875 Mb.
     *   Map statistics for put():
     *       minKeySize=4
     *       maxKeySize=4
     *       avgKeySize=4
     *       totKeySize=35844 (0.034 Mb)
     *       minValueSize=13
     *       maxValueSize=17
     *       avgValueSize=17
     *       totValueSize=152139 (0.145 Mb)
     *       minCost=1973 ns
     *       maxCost=126718 ns
     *       avgTimeCost=2932 ns
     *       totTimeSpent=26 ms
     *   Map statistics for get():
     *       minCost=3158 ns
     *       maxCost=883075 ns
     *       avgTimeCost=6044 ns
     *       totTimeSpent=54 ms
     * </pre>
     * 
     * @param alreadySet  provided by TestNG
     */
    @Test(dataProvider = PROVIDER)
    public void test_money(SerializationFramework alreadySet) {
        new TestSpecification<>(MoneyPrice.class)
                .averageSize(MoneyPrice.getAverageSize(AppleData.getAverage()))
                .converter(MoneyPrice::ofJson)
                .run();
    }
    
    /**
     * Worst:
     * <pre>
     *   --- Chronicle Map's default ---
     *   Wrote 8961 FastMoneyPrice's. File size: 9773056 bytes = 9.3203125 Mb.
     *   Map statistics for put():
     *       minKeySize=44
     *       maxKeySize=44
     *       avgKeySize=44
     *       totKeySize=394284 (0.376 Mb)
     *       minValueSize=735
     *       maxValueSize=735
     *       avgValueSize=735
     *       totValueSize=6586335 (6.281 Mb)
     *       minCost=7105 ns
     *       maxCost=2728571 ns
     *       avgTimeCost=18275 ns
     *       totTimeSpent=163 ms
     *   Map statistics for get():
     *       minCost=46186 ns
     *       maxCost=2831603 ns
     *       avgTimeCost=88244 ns
     *       totTimeSpent=790 ms
     * </pre>
     * 
     * Best:
     * <pre>
     *   --- Kryo Custom ---
     *   Wrote 8961 FastMoneyPrice's. File size: 339968 bytes = 0.32421875 Mb.
     *   Map statistics for put():
     *       minKeySize=4
     *       maxKeySize=4
     *       avgKeySize=4
     *       totKeySize=35844 (0.034 Mb)
     *       minValueSize=12
     *       maxValueSize=13
     *       avgValueSize=12
     *       totValueSize=109439 (0.104 Mb)
     *       minCost=394 ns
     *       maxCost=175668 ns
     *       avgTimeCost=1065 ns
     *       totTimeSpent=9 ms
     *   Map statistics for get():
     *       minCost=1973 ns
     *       maxCost=887812 ns
     *       avgTimeCost=4897 ns
     *       totTimeSpent=43 ms
     * </pre>
     * 
     * @param alreadySet  provided by TestNG
     */
    @Test(dataProvider = PROVIDER)
    public void test_fastMoney(SerializationFramework alreadySet) {
        /*
         * Average size? But we use a reference called "EXACT_SIZE"? Kryo and
         * FST both write variable-length primites. So it doesn't matter if we'd
         * like to think of a long as having an "exact size". If we use
         * constantSize() instead of averageSize(), then you shall find that
         * this method crash at a point where Chronicle Map would like to use
         * 2 chunks of data blocks where just 1 was expected.
         */
        
        new TestSpecification<>(FastMoneyPrice.class)
                .averageSize(FastMoneyPrice.EXACT_SIZE)
                .converter(FastMoneyPrice::ofJson)
                .run();
    }
    
    /**
     * Author's output:
     * <pre>
     *   --- Chronicle Map's default ---
     *   Wrote 8961 CustomFastMoneyPrice1's. File size: 1617920 bytes = 1.54296875 Mb.
     *   Map statistics for put():
     *       minKeySize=44
     *       maxKeySize=44
     *       avgKeySize=44
     *       totKeySize=394284 (0.376 Mb)
     *       minValueSize=97
     *       maxValueSize=97
     *       avgValueSize=97
     *       totValueSize=869217 (0.829 Mb)
     *       minCost=5131 ns
     *       maxCost=2417501 ns
     *       avgTimeCost=15341 ns
     *       totTimeSpent=137 ms
     *   Map statistics for get():
     *       minCost=15000 ns
     *       maxCost=4182072 ns
     *       avgTimeCost=30699 ns
     *       totTimeSpent=275 ms
     * </pre>
     */
    @Test
    public void test_writeCustomFastMoney1() {
        new TestSpecification<>(CustomFastMoneyPrice1.class)
                .constantSize(CustomFastMoneyPrice1.EXACT_SIZE)
                .converter(CustomFastMoneyPrice1::ofJson)
                .run();
    }
    
    /**
     * Author's output:
     * <pre>
     *   --- Chronicle Map's default ---
     *   Wrote 8961 CustomFastMoneyPrice2's. File size: 823296 bytes = 0.78515625 Mb.
     *   Map statistics for put():
     *       minKeySize=44
     *       maxKeySize=44
     *       avgKeySize=44
     *       totKeySize=394284 (0.376 Mb)
     *       minValueSize=22
     *       maxValueSize=22
     *       avgValueSize=22
     *       totValueSize=197142 (0.188 Mb)
     *       minCost=4737 ns
     *       maxCost=2449082 ns
     *       avgTimeCost=13424 ns
     *       totTimeSpent=120 ms
     *   Map statistics for get():
     *       minCost=8289 ns
     *       maxCost=1100982 ns
     *       avgTimeCost=15731 ns
     *       totTimeSpent=140 ms
     * </pre>
     * 
     * It's interesting to note that {@code
     * PriceSerialization.test_customFastMoney2()} report an exact byte size of
     * 97. This test see an exact byte size of 22.<p>
     * 
     * If the explanation was that the currency code String object is "reused",
     * then we would have seen the same effect for the previous test case using
     * {@code CustomFastMoneyPrice1} - but, we don't.<p>
     * 
     * It's also odd that the value - which contains both a date (key) and money
     * information - is actually smaller then just the key (one date).
     */
    @Test
    public void test_writeCustomFastMoney2() {
        new TestSpecification<>(CustomFastMoneyPrice2.class)
                .constantSize(CustomFastMoneyPrice2.EXACT_SIZE)
                .converter(CustomFastMoneyPrice2::ofJson)
                .run();
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    private <V extends Price> ChronicleMap<LocalDate, V> buildMapAverage(V valueExample) {
        return buildMap(valueExample, ChronicleMapBuilder::averageValue);
    }
    
    private <V extends Price> ChronicleMap<LocalDate, V> buildMapConstant(V valueExample) {
        return buildMap(valueExample, ChronicleMapBuilder::constantValueSizeBySample);
    }
    
    private <V extends Price> ChronicleMap<LocalDate, V> buildMap(
            V valueExample, BuilderBiFunction<V> setValueSize)
    {
        @SuppressWarnings("unchecked")
        final Class<V> type = (Class<V>) valueExample.getClass();
        
        @SuppressWarnings("unchecked")
        ChronicleMapLogger<LocalDate, V, ?> logger
                = (ChronicleMapLogger<LocalDate, V, ?>) mapLogger;
        
        ChronicleMapBuilder<LocalDate, V> b
                = ChronicleMapBuilder.of(LocalDate.class, type)
                        .constantKeySizeBySample(valueExample.getDate())
                        .entries(AppleData.count())
                        .putReturnsNull(true)
                        .removeReturnsNull(true)
                        .mapMethods(logger);
        
        if (serializer != null) {
            ChronicleMapMarshaller<LocalDate> key = new ChronicleMapMarshaller<>(serializer);
            
            @SuppressWarnings("unchecked")
            ChronicleMapMarshaller<V> value = (ChronicleMapMarshaller<V>) (ChronicleMapMarshaller) key;
            
            b.keyMarshaller(key)
             .valueMarshaller(value);
        }
        
        final ChronicleMap<LocalDate, V> map;
        
        try {
            map = setValueSize.apply(b, valueExample)
                    .createPersistedTo(getFile());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        return map;
    }
    
    private File getFile() {
        if (file != null) {
            return file.toFile();
        }
        
        final Path tempDir = Paths.get(SystemProperties.GRADLE_TEST_TEMP_DIR.require());
        
        final Path temp;
        
        try {
            temp = Files.createTempFile(tempDir, null, null);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        return (file = temp).toFile();
    }
    
    private <V extends Price> void assertMap(
            ChronicleMap<LocalDate, V> map, BiFunction<LocalDate, JsonNumber, V> converter)
    {
        assertNotNull(map);
        assertEquals(map.size(), AppleData.count());
        AppleData.rowsAs(converter).forEach(p ->
                assertEquals(map.get(p.getDate()), p));
    }
    
    private void printFileSize(Class<?> valueType) {
        printFileSize(AppleData.count(), valueType);
    }
    
    private void printFileSize(long count, Class<?> valueType) {
        final long bytes;
        
        try {
            bytes = Files.size(file);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        final String s = serializer == null ?
                "Chronicle Map's default" :
                serializer.toString();
        
        System.out.println("--- " + s + " ---");
        System.out.printf("Wrote %s %s's. File size: %s bytes = %s Mb." + System.lineSeparator(),
                count, valueType.getSimpleName(), bytes, bytes / 1024. / 1024);
    }
    
    private void printMapStatistics() {
        System.out.println(mapLogger);
        System.out.println();
    }
    
    @FunctionalInterface
    private interface BuilderBiFunction<T> extends BiFunction<
            ChronicleMapBuilder<LocalDate, T>, T, ChronicleMapBuilder<LocalDate, T>> {
        // Only used to capture tons of type parameters.
    }
    
    private static class Dummy implements Price, Serializable {
        private static final long serialVersionUID = 1;
        
        @Override public LocalDate getDate() {
                return LocalDate.now();
        }
    }
}