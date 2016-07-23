package com.martinandersson.money.lib;

import static com.martinandersson.money.lib.model.FastMoneyPrice.MAX_SCALE;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import static java.util.Collections.unmodifiableNavigableMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import static java.util.stream.Collectors.toMap;

/**
 * This class has a static API for access to data downloaded from Quandl.com (as
 * read on 2016-06-26):
 * <pre>{@code
 * 
 *   https://www.quandl.com/api/v3/datasets/WIKI/AAPL.json?order=acs&column_index=11
 * }</pre>
 * 
 * This class doesn't actually make a request. The data is saved in the resource
 * folder in a file called "aapl-data.json".<p>
 * 
 * If you look into the file, you'll notice that we asked for ascending order
 * but got descending =)<p>
 * 
 * If you don't need "real world data", then consider using {@link
 * NumberFactory} instead.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class AppleData
{
    private AppleData() {
        // Empty
    }
    
    
    private static BigDecimal AVG;
    
    
    /**
     * Returns the average adjusted closing price of WIKI/AAPL (in terms of
     * scale and precision).
     * 
     * @return the average adjusted closing price of WIKI/AAPL (in terms of
     * scale and precision)
     * 
     * @param maxScale  max scale of the returned average
     */
    public static BigDecimal getAverage(int maxScale) {
        BigDecimal avg = getAverage();
        
        if (avg.scale() > maxScale) {
            avg = avg.setScale(MAX_SCALE, RoundingMode.HALF_UP);
        }
        
        return avg;
    }
    
    /**
     * Returns the average adjusted closing price of WIKI/AAPL (in terms of
     * scale and precision).
     * 
     * @return the average adjusted closing price of WIKI/AAPL (in terms of
     * scale and precision)
     */
    public static BigDecimal getAverage() {
        BigDecimal avg = AVG;
        
        if (avg == null) {
            BigDecimal sum = numbers()
                    .map(JsonNumber::bigDecimalValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            double avgScale = numbers()
                    .map(JsonNumber::bigDecimalValue)
                    .mapToInt(BigDecimal::scale)
                    .summaryStatistics()
                    .getAverage();
            
            int scale = BigDecimal.valueOf(avgScale)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();
            
            avg = sum.divide(
                    BigDecimal.valueOf(AppleData.count()),
                    scale,
                    RoundingMode.HALF_UP);
        }
        
        return AVG = avg;
    }
    
    private static final NavigableMap<LocalDate, JsonNumber> PRICES
            = unmodifiableNavigableMap(init());
    
    private static NavigableMap<LocalDate, JsonNumber> init() {
        String resourceDir = SystemProperties.GRADLE_RESOURCE_DIR.require();
        
        return readRoot(Paths.get(resourceDir, "aapl-data.json"))
                .getJsonObject("dataset")
                .getJsonArray("data")
                .stream()
                .map(x -> (JsonArray) x)
                .collect(toMap(
                        // First column is String, parsed as LocalDate (Map key):
                        row -> LocalDate.parse(row.getString(0)),
                        // Second column is JsonNumber (Map value):
                        row -> row.getJsonNumber(1),
                        // JavaDoc lie. NPE if we don't provide something (merger):
                        (k, v) -> {
                            throw new AssertionError("I was wrong.");},
                        // NavigableMap supplier:
                        () -> new TreeMap<>()));
    }
    
    
    
    public static int count() {
        return PRICES.size();
    }
    
    public static Stream<LocalDate> dates() {
        return PRICES.keySet().stream();
    }
    
    public static Stream<JsonNumber> numbers() {
        return PRICES.values().stream();
    }
    
    public static <T> Stream<T> numbersAs(Function<JsonNumber, T> converter) {
        return numbers().map(converter::apply);
    }
    
    public static Stream<Map.Entry<LocalDate, JsonNumber>> rows() {
        return PRICES.entrySet().stream();
    }
    
    public static <T> Stream<T> rowsAs(BiFunction<LocalDate, JsonNumber, T> converter) {
        return rows().map(e ->
                converter.apply(e.getKey(), e.getValue()));
    }
    
    
    
    private static JsonObject readRoot(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            try (JsonReader json = Json.createReader(reader)) {
                return json.readObject();
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}