package com.martinandersson.money.lib.serializer;

import com.martinandersson.money.lib.LocalDates;
import com.martinandersson.money.lib.MonetaHack;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import org.javamoney.moneta.FastMoney;
import org.javamoney.moneta.Money;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.nustaq.serialization.serializers.FSTJSonSerializers;

/**
 * Serializers for FST.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class FSTSerializers
{
    private FSTSerializers() {
        // Empty
    }
    
    
    /**
     * Will hookup the specified {@code conf} thing with all serializers in
     * this class.
     * 
     * @param conf  FST configuration
     * 
     * @return same conf thing
     */
    public static FSTConfiguration hookup(FSTConfiguration conf) {
        // If this class is not registered explicitly, then FST will crash
        // with different exceptions depending on what we're deserializing. Hmm.
        conf.registerClass(LocalDate.class);
        
        conf.registerSerializer(LocalDate.class,  LOCAL_DATE,  false);
        conf.registerSerializer(BigDecimal.class, BIG_DECIMAL, false);
        conf.registerSerializer(Money.class,      MONEY,       false);
        conf.registerSerializer(FastMoney.class,  FAST_MONEY,  false);
        
        return conf;
    }
    
    
    /**
     * FST serializer for {@code LocalDate}.<p>
     * 
     * I have found that FST, when using a custom serializer, doesn't write/read
     * all possible short values consistently. If we write a short like so:
     * <pre>
     *   out.writeShort(myShort);
     * </pre>
     * 
     * ..and read like so:
     * <pre>
     *   in.readShort();
     * </pre>
     * 
     * Then for some short values, what is read will be different from what was
     * written, causing {@code ChronicleMapRealBenchmark} using {@code
     * SerializationFramework.FST_CUSTOM} to fail (if assertions are enabled)
     * and <i>only if</i> the benchmark that use {@code
     * SerializationFramework.FST_VANILLA} executed first. This implies that FST
     * has undesirable side-effects somewhere.<p>
     * 
     * {@code writeShort()} actually accept an {@code int} which could be the
     * first sign something is fucked up? But even if we replace the write
     * method to this:
     * <pre>
     *   out.getCodec().writeFShort(myShort);
     * </pre>
     * 
     * ..the problem still doesn't go away. And yes, I've tried many flavors of
     * night hacks.<p>
     * 
     * So this implementation currently write/read 2 bytes instead of a short.
     * But, according to my testing, this doesn't affect the byte size of the
     * serialized container. I have not yet investigated how our hack affect the
     * time cost, but I suspect there is no significant impact.
     */
    public static final FSTBasicObjectSerializer LOCAL_DATE = newSerializer(
            (obj, out) -> {
                short myShort = LocalDates.toShort((LocalDate) obj);
                byte[] bytes = ByteBuffer.allocate(2).putShort(myShort).array();
                out.getCodec().writeRawBytes(bytes, 0, 2);
            },
            in -> {
                byte[] bytes = {
                    in.getCodec().readFByte(),
                    in.getCodec().readFByte()};
                
                short date = ByteBuffer.wrap(bytes).getShort();
                return LocalDates.fromShort(date);
            });
    
    public static final FSTBasicObjectSerializer BIG_DECIMAL
            = new FSTJSonSerializers.BigDecSerializer();
    
    public static final FSTBasicObjectSerializer MONEY = new FSTBasicObjectSerializer() {
        @Override
        public void writeObject(
                FSTObjectOutput out,
                Object toWrite,
                FSTClazzInfo clzInfo,
                FSTClazzInfo.FSTFieldInfo referencedBy,
                int streamPosition) throws IOException
        {
            Money money = (Money) toWrite;
            
            BIG_DECIMAL.writeObject(
                    out,
                    MonetaHack.getNumber(money),
                    clzInfo,
                    referencedBy,
                    streamPosition);
            
            out.writeStringUTF(money.getCurrency().getCurrencyCode());
        }

        @Override
        public Object instantiate(
                Class objectClass,
                FSTObjectInput in,
                FSTClazzInfo serializationInfo,
                FSTClazzInfo.FSTFieldInfo reference,
                int streamPosition) throws Exception
        {
            BigDecimal number = (BigDecimal) BIG_DECIMAL.instantiate(
                    objectClass, in, serializationInfo, reference, streamPosition);
            
            String currency = in.readStringUTF();
            
            return Money.of(number, currency);
        }
    };
    
    public static final FSTBasicObjectSerializer FAST_MONEY = newSerializer(
            (obj, out) -> {
                FastMoney money = (FastMoney) obj;
                out.writeLong(MonetaHack.getNumber(money));
                out.writeStringUTF(money.getCurrency().getCurrencyCode());
            },
            in -> MonetaHack.newFastMoney(in.readLong(), in.readStringUTF()));
    
    
    
    private static FSTBasicObjectSerializer newSerializer(
            IOBiConsumer<Object, FSTObjectOutput> write,
            IOFunction<FSTObjectInput, Object> instantiate)
    {
        return new FSTBasicObjectSerializer() {
            @Override
            public void writeObject(
                    FSTObjectOutput out,
                    Object toWrite,
                    FSTClazzInfo clzInfo,
                    FSTClazzInfo.FSTFieldInfo referencedBy,
                    int streamPosition) throws IOException
            {
                write.accept(toWrite, out);
            }

            @Override
            public Object instantiate(
                    Class objectClass,
                    FSTObjectInput in,
                    FSTClazzInfo serializationInfo,
                    FSTClazzInfo.FSTFieldInfo reference,
                    int streamPosition) throws Exception
            {
                return instantiate.apply(in);
            }
        };
    }
    
    private interface IOBiConsumer<T, U> {
        void accept(T t, U u) throws IOException;
    }
    
    private interface IOFunction<T, R> {
        R apply(T t) throws IOException;
    }
}