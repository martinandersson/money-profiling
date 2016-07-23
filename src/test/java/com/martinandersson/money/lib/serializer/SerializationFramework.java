package com.martinandersson.money.lib.serializer;

import com.martinandersson.money.lib.model.DoublePrice;
import com.martinandersson.money.lib.model.BigDecimalPrice;
import com.martinandersson.money.lib.model.MoneyPrice;
import com.martinandersson.money.lib.model.FastMoneyPrice;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.money.CurrencyContext;
import javax.money.MonetaryContext;
import org.javamoney.moneta.FastMoney;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.internal.JDKCurrencyAdapter;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;
import org.objenesis.strategy.SerializingInstantiatorStrategy;

/**
 * Provides the {@code Serializer} implementation of all known serialization
 * frameworks used in this project:
 * 
 * <ul>
 *   <li>{@linkplain #JAVA Java}</li>
 *   <li>{@linkplain #KRYO_VANILLA Kryo Vanilla}</li>
 *   <li>{@linkplain #KRYO_CUSTOM Kryo Custom}</li>
 *   <li>{@linkplain #FST_VANILLA FST Vanilla}</li>
 *   <li>{@linkplain #FST_CUSTOM FST Custom}</li>
 * </ul>
 * 
 * "Vanilla" versions of a serialization framework will serialize/deserialize
 * using the framework "out of the box".<p>
 * 
 * "Custom" variants will configure the framework to use custom serializers for
 * an even smarter serialization/deserialization protocol.<p>
 * 
 * Both Kryo and FST can be configured beyond all sanity when it comes to
 * serialization/deserialization. I try to stick to as much vanilla flavor as
 * possible. For example, both Kryo and FST support internal use of {@code
 * sun.misc.Unsafe} and both support the ability to not reuse objects but always
 * create new instances. Even though the latter configuration actually make
 * sense for our domain model (since all our prices and the fields within them
 * represents truly unique objects), none of these configuration night hacks has
 * been applied.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public enum SerializationFramework implements Serializer
{
    /**
     * This serializer uses the JDK provided {@code ObjectOutputStream} and
     * {@code ObjectInputStream} to serialize/deserialize.<p>
     * 
     * The objects themselves may customize this protocol ({@code Serializable})
     * or fully replace it {@code (Externalizable}).
     */
    JAVA ("Java") {
        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] serialize(Object object, Consumer<String> duration) {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream(BUFFER_SIZE);
            
            final long nanos;

            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                final long then = System.nanoTime();
                out.writeObject(object);
                nanos = System.nanoTime() - then;
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            
            forward(duration, nanos);
            return bytes.toByteArray();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(Object object, OutputStream out) {
            try (ObjectOutputStream os = new ObjectOutputStream(out)) {
                os.writeObject(object);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T deserialize(byte[] bytes, Consumer<String> duration) {
            final ByteArrayInputStream obj = new ByteArrayInputStream(bytes);
            
            final T t;
            
            final long nanos;

            try (ObjectInputStream in = new ObjectInputStream(obj)) {
                final long then = System.nanoTime();
                
                @SuppressWarnings("unchecked")
                T t0 = (T) in.readObject();
                
                nanos = System.nanoTime() - then;
                t = t0;
                
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            
            forward(duration, nanos);
            return t;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T deserialize(InputStream in) {
            try (ObjectInputStream ios = new ObjectInputStream(in)) {
                @SuppressWarnings("unchecked")
                T t = (T) ios.readObject();
                
                return t;
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        
    },
    
    /**
     * This serializer uses Kryo for serialization/deserialization.
     */
    KRYO_VANILLA ("Kryo Vanilla", new KryoImpl(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(true);
        classes().forEach(kryo::register);
        
        // Use Java's standard serialization mechanism for instantiation:
        kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());
        
        // Alternatively, we could have first try no-arg constructor and fallback to Objenesis:
//        kryo.setInstantiatorStrategy(
//                new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        // ..or only used Objenesis:
//        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        return kryo;
    })),
    
    /**
     * This serializer uses a customized version of Kryo for
     * serialization/deserialization.<p>
     * 
     * Kryo has been customized with serializers that is a bit smarter than
     * vanilla Kryo when it comes to marshalling objects. See {@link
     * KryoSerializers}.
     */
    KRYO_CUSTOM ("Kryo Custom", new KryoImpl(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(true);
        
        KryoSerializers.hookup(kryo);
        kryo.register(DoublePrice.class);
        kryo.register(BigDecimalPrice.class);
        kryo.register(MoneyPrice.class);
        kryo.register(FastMoneyPrice.class);
        
        kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());
        return kryo;
    })),
    
    /**
     * This serializer uses FST for serialization/deserialization.
     */
    FST_VANILLA ("FST Vanilla", new FSTImpl(conf ->
            conf.registerClass(classes().toArray(Class[]::new)))),
    
    /**
     * This serializer uses a customized version of FST for
     * serialization/deserialization.<p>
     * 
     * FST has been customized with serializers that is a bit smarter than
     * vanilla FST when it comes to marshalling objects. See {@link
     * FSTSerializers}.
     */
    FST_CUSTOM ("FST Custom", new FSTImpl(conf -> {
        FSTSerializers.hookup(conf);
        conf.registerClass(
                DoublePrice.class,
                BigDecimalPrice.class,
                MoneyPrice.class,
                FastMoneyPrice.class);
    }));
    
    
    /**
     * 1361 is the largest byte size I've seen used for a serialized object.
     */
    private static final int BUFFER_SIZE = 1361;
    
    private static Stream<Class<?>> classes() {
        return Stream.of(LocalDate.class,
                         BigDecimal.class,
                         
                         Money.class,
                         JDKCurrencyAdapter.class,
                         Currency.class,
                         CurrencyContext.class,
                         HashMap.class,
                         MonetaryContext.class,
                         Class.class,
                         RoundingMode.class,
                         
                         FastMoney.class,
                         
                         DoublePrice.class,
                         BigDecimalPrice.class,
                         MoneyPrice.class,
                         FastMoneyPrice.class);
    }
    
    private static final ThreadLocal<NumberFormat> DECIMAL_FORMATTER
            = ThreadLocal.withInitial(() -> {
                NumberFormat f = new DecimalFormat("#.###");
                f.setRoundingMode(RoundingMode.HALF_UP);
                return f;
            });
    
    private static void forward(Consumer<String> duration, long nanos) {
        if (duration == null) {
            return;
        }
        
        double ms = nanos / 1_000_000.;
        duration.accept(DECIMAL_FORMATTER.get().format(ms) + " ms");
    }
    
    
    private final String name;
    
    private final Serializer delegate;
    
    private SerializationFramework(String name) {
        this(name, null);
    }
    
    private SerializationFramework(String name, Serializer delegate) {
        this.name = name;
        this.delegate = delegate;
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] serialize(Object object, Consumer<String> duration) {
        return delegate.serialize(object, duration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(Object object, OutputStream out) {
        delegate.serialize(object, out); }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T deserialize(byte[] bytes, Consumer<String> duration) {
        return delegate.deserialize(bytes, duration); }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T deserialize(InputStream in) {
        return delegate.deserialize(in);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        return name;
    }
    
    
    
    private static class KryoImpl implements Serializer
    {
        /**
         * Is it sad that Kryo insist on using a buffer even though we provide
         * Kryo our own Output-/InputStream?<p>
         * 
         * I tested and found no performance impact using the current value 8
         * versus {@code BUFFER_SIZE}.
         */
        private static final int MIN_BUFFER = 8;
        
        private final KryoPool pool;
        
        public KryoImpl(KryoFactory factory) {
            this.pool = new KryoPool.Builder(factory)
                    .softReferences()
                    .build();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] serialize(Object object, Consumer<String> duration) {
            Kryo kryo = pool.borrow();
            
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
                
                Output o = new Output(out, MIN_BUFFER);
                
                final long nanos;
                final long then = System.nanoTime();
                
                kryo.writeClassAndObject(o, object);
                nanos = System.nanoTime() - then;
                
                o.flush();
                forward(duration, nanos);
                return out.toByteArray();
            }
            finally {
                pool.release(kryo);
            }
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(Object object, OutputStream out) {
            Kryo kryo = pool.borrow();
            
            try (Output o = new Output(out, MIN_BUFFER)) {
                kryo.writeClassAndObject(o, object);
            }
            finally {
                pool.release(kryo);
            }
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T deserialize(byte[] bytes, Consumer<String> duration) {
            Kryo kryo = pool.borrow();
            
            try (Input i = new Input(bytes)) {
                final long then = System.nanoTime();
                
                @SuppressWarnings("unchecked")
                T t = (T) kryo.readClassAndObject(i);
                
                forward(duration, System.nanoTime() - then);
                return t;
            }
            finally {
                pool.release(kryo);
            }
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T deserialize(InputStream in) {
            Kryo kryo = pool.borrow();
            
            try {
                Input i = new Input(in, MIN_BUFFER);
                
                @SuppressWarnings("unchecked")
                T t = (T) kryo.readClassAndObject(i);
                
                return t;
            }
            finally {
                pool.release(kryo);
            }
        }
    }
    
    private static class FSTImpl implements Serializer
    {
        private final FSTConfiguration conf;
        
        public FSTImpl(Consumer<FSTConfiguration> configure) {
            FSTConfiguration conf0 = FSTConfiguration.createDefaultConfiguration();
            configure.accept(conf0);
            conf = conf0;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] serialize(Object object, Consumer<String> duration) {
            final long then = System.nanoTime();
            byte[] bytes = conf.asByteArray(object);
            forward(duration, System.nanoTime() - then);
            return bytes;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(Object object, OutputStream out) {
            try {
                FSTObjectOutput fout = conf.getObjectOutput(out);
                fout.writeObject(object);
                fout.flush();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T deserialize(byte[] bytes, Consumer<String> duration) {
            final long then = System.nanoTime();
            
            // If you look in the source code of FSTConfiguration.asObject(byte[]),
            // specifically the exception handling, then I don't really know what
            // this method is up to. There's also an issue related to this method:
            // https://github.com/RuedigerMoeller/fast-serialization/issues/141
            
            // So we do what the method do but with our own exception "handling":
            
            T t;
            
            try {
                @SuppressWarnings("unchecked")
                T t0 = (T) conf.getObjectInput(bytes).readObject();
                
                t = t0;
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            
            forward(duration, System.nanoTime() - then);
            return t;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T deserialize(InputStream in) {
            try {
                @SuppressWarnings("unchecked")
                T t = (T) conf.getObjectInput(in).readObject();
                
                return t;
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}