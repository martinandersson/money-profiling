package com.martinandersson.money.unittest;

import com.martinandersson.money.lib.model.BigDecimalPrice;
import com.martinandersson.money.lib.model.CustomFastMoneyPrice1;
import com.martinandersson.money.lib.model.CustomFastMoneyPrice2;
import com.martinandersson.money.lib.model.DoublePrice;
import com.martinandersson.money.lib.model.FastMoneyPrice;
import com.martinandersson.money.lib.model.MoneyPrice;
import com.martinandersson.money.lib.model.Price;
import com.martinandersson.money.lib.serializer.SerializationFramework;
import com.martinandersson.money.lib.serializer.Serializer;
import java.io.Serializable;
import static java.lang.System.out;
import java.util.Arrays;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Will unit test serialization frameworks for different versions of {@code
 * Price}, ensuring serialization and deserialization work properly as well as
 * print their serialized byte size and serialization time cost.<p>
 * 
 * Don't take the time cost results of this class too seriously. Firstly, the
 * time cost will vary quit a lot from execution to execution. Also as
 * demonstrated in other tests and benchmarks, the performance change
 * significantly once we start serializing/deserializing thousands of prices.<p>
 * 
 * What can be said about the results of this class is that Kryo is the unbeaten
 * winner for every {@code Price} implementation when it comes to saving
 * bytes.<p>
 * 
 * The JavaDoc of all methods herein list the results produced by author's
 * machine.<p>
 * 
 * The winner in terms of disk space is {@code FastMoneyPrice} combined with
 * {@link SerializationFramework#KRYO_CUSTOM}.<p>
 * 
 * However, a real application may go even further on its quest to save bytes.
 * The {@code FastMoneyPrice#date} field is already present in the map as the
 * entry key. Hence, this field could be marked transient.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class PriceSerializationTest
{
    // Could be more elegantly written using @Factory, but that brake my IDE support :'(
    @DataProvider
    private static Serializer[][] serializer() {
        return Arrays.stream(SerializationFramework.values())
                .map(s -> new Serializer[]{s})
                .toArray(Serializer[][]::new);
    }
    
    
    
    /**
     * Java:
     * <pre>
     *   Time spent serializing: 0.778 ms
     *   Number of bytes: 158
     *   Time spent deserializing: 0.251 ms
     * </pre>
     * 
     * Kryo Vanilla:
     * <pre>
     *   Time spent serializing: 0.158 ms
     *   Number of bytes: 17
     *   Time spent deserializing: 1.279 ms
     * </pre>
     * 
     * Kryo Custom:
     * <pre>
     *   Time spent serializing: 2.151 ms
     *   Number of bytes: 13
     *   Time spent deserializing: 0.086 ms
     * </pre>
     * 
     * FST Vanilla:
     * <pre>
     *   Time spent serializing: 3.533 ms
     *   Number of bytes: 33
     *   Time spent deserializing: 2.841 ms
     * </pre>
     * 
     * FST Custom:
     * <pre>
     *   Time spent serializing: 0.133 ms
     *   Number of bytes: 15
     *   Time spent deserializing: 0.118 ms
     * </pre>
     * 
     * @param s  provided by TestNG
     */
    @Test(dataProvider = "serializer")
    public void test_double(Serializer s) {
        byte[] bytes = serialize(s, DoublePrice.EXACT_SIZE);
        assertEquals(DoublePrice.EXACT_SIZE, deserialize(s, bytes));
    }
    
    /**
     * Java:
     * <pre>
     *   Time spent serializing: 1.221 ms
     *   Number of bytes: 468
     *   Time spent deserializing: 0.561 ms
     * </pre>
     * 
     * Kryo Vanilla:
     * <pre>
     *   Time spent serializing: 0.194 ms
     *   Number of bytes: 16
     *   Time spent deserializing: 1.346 ms
     * </pre>
     * 
     * Kryo Custom:
     * <pre>
     *   Time spent serializing: 2.285 ms
     *   Number of bytes: 12
     *   Time spent deserializing: 0.087 ms
     * </pre>
     * 
     * FST Vanilla:
     * <pre>
     *   Time spent serializing: 4.426 ms
     *   Number of bytes: 39
     *   Time spent deserializing: 3.382 ms
     * </pre>
     * 
     * FST Custom:
     * <pre>
     *   Time spent serializing: 0.286 ms
     *   Number of bytes: 18
     *   Time spent deserializing: 0.088 ms
     * </pre>
     * 
     * @param s  provided by TestNG
     */
    @Test(dataProvider = "serializer")
    public void test_bigDecimal(Serializer s) {
        BigDecimalPrice p = BigDecimalPrice.getAverageSize();
        byte[] bytes = serialize(s, p);
        assertEquals(p, deserialize(s, bytes));
    }
    
    /**
     * Java:
     * <pre>
     *   Time spent serializing: 3.524 ms
     *   Number of bytes: 1361
     *   Time spent deserializing: 1.21 ms
     * </pre>
     * 
     * Kryo Vanilla:
     * <pre>
     *   Time spent serializing: 0.375 ms
     *   Number of bytes: 132
     *   Time spent deserializing: 1.382 ms
     * </pre>
     * 
     * Kryo Custom:
     * <pre>
     *   Time spent serializing: 2.611 ms
     *   Number of bytes: 14
     *   Time spent deserializing: 0.154 ms
     * </pre>
     * 
     * FST Vanilla:
     * <pre>
     *   Time spent serializing: 4.067 ms
     *   Number of bytes: 196
     *   Time spent deserializing: 3.343 ms
     * </pre>
     * 
     * FST Custom:
     * <pre>
     *   Time spent serializing: 0.399 ms
     *   Number of bytes: 21
     *   Time spent deserializing: 0.166 ms
     * </pre>
     * 
     * @param s  provided by TestNG
     */
    @Test(dataProvider = "serializer")
    public void test_money(Serializer s) {
        MoneyPrice p = MoneyPrice.getAverageSize();
        byte[] bytes = serialize(s, p);
        assertEquals(p, deserialize(s, bytes));
    }
    
    /**
     * Java:
     * <pre>
     *   Time spent serializing: 2.551 ms
     *   Number of bytes: 735
     *   Time spent deserializing: 0.626 ms
     * </pre>
     * 
     * Kryo Vanilla:
     * <pre>
     *   Time spent serializing: 0.268 ms
     *   Number of bytes: 53
     *   Time spent deserializing: 1.391 ms
     * </pre>
     * 
     * Kryo Custom:
     * <pre>
     *   Time spent serializing: 2.927 ms
     *   Number of bytes: 12
     *   Time spent deserializing: 0.187 ms
     * </pre>
     * 
     * FST Vanilla:
     * <pre>
     *   Time spent serializing: 4.336 ms
     *   Number of bytes: 79
     *   Time spent deserializing: 3.166 ms
     * </pre>
     * 
     * FST Custom:
     * <pre>
     *   Time spent serializing: 0.325 ms
     *   Number of bytes: 17
     *   Time spent deserializing: 0.153 ms
     * </pre>
     * 
     * @param s  provided by TestNG
     */
    @Test(dataProvider = "serializer")
    public void test_fastMoney(Serializer s) {
        byte[] bytes = serialize(s, FastMoneyPrice.EXACT_SIZE);
        assertEquals(FastMoneyPrice.EXACT_SIZE, deserialize(s, bytes));
    }
    
    /**
     * Java:
     * <pre>
     *   Time spent serializing: 4.022 ms
     *   Number of bytes: 97
     *   Time spent deserializing: 0.295 ms
     * </pre>
     * 
     * Please note that only JDK serialization is used in this test. That's
     * kind of the whole point of the class {@code CustomFastMoneyPrice1}.
     */
    @Test
    public void test_customFastMoney1() {
        Serializer s = SerializationFramework.JAVA;
        byte[] bytes = serialize(s, CustomFastMoneyPrice1.EXACT_SIZE);
        assertEquals(CustomFastMoneyPrice1.EXACT_SIZE, deserialize(s, bytes));
    }
    
    /**
     * Java:
     * <pre>
     *   Time spent serializing: 3.679 ms
     *   Number of bytes: 97
     *   Time spent deserializing: 0.248 ms
     * </pre>
     * 
     * Please note that only JDK serialization is used in this test. That's
     * kind of the whole point of the class {@code CustomFastMoneyPrice2}.
     */
    @Test
    public void test_customFastMoney2() {
        Serializer s = SerializationFramework.JAVA;
        byte[] bytes = serialize(s, CustomFastMoneyPrice2.EXACT_SIZE);
        assertEquals(CustomFastMoneyPrice2.EXACT_SIZE, deserialize(s, bytes));
    }
    
    
    
    private static <T extends Price & Serializable> byte[] serialize(
            Serializer serializer, T price)
    {
        out.println("--- " + serializer + " ---");
        
        final byte[] bytes = serializer.serialize(price, time ->
            out.println("Time spent serializing: " + time));
        
        out.println("Number of bytes: " + bytes.length);
        return bytes;
    }
    
    private static <T extends Price & Serializable> T deserialize(
            Serializer serializer, byte[] bytes)
    {
        return serializer.deserialize(bytes, time ->
            out.println("Time spent deserializing: " + time + System.lineSeparator()));
    }
}