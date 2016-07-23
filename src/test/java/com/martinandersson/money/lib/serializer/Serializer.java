package com.martinandersson.money.lib.serializer;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * A serializer know how to serialize and deserialize any given object to/from
 * a {@code byte[]} as well as to/from {@code OutputStream}/{@code
 * InputStream}.<p>
 * 
 * You may call this interface an adapter, that bridge our code base to
 * different serialization frameworks.<p>
 * 
 * The serializer implementation is thread-safe.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 * 
 * @see SerializationFramework
 */
public interface Serializer
{
    /**
     * Serialize specified {@code object}.
     * 
     * @implSpec
     * Default implementation uses {@link #serialize(Object, Consumer)
     * serialize(Object, Consumer)}, passing {@code null} as {@code duration}.
     * 
     * @param object  object to serialize
     * 
     * @return object serialized
     */
    default byte[] serialize(Object object) {
        return serialize(object, (Consumer<String>) null);
    }
    
    /**
     * Serialize specified {@code object}.<p>
     * 
     * The duration consumer will receive the time it took to serialize the
     * object in a nicely formatted String ("X.XXX ms").
     * 
     * @param object    object to serialize
     * @param duration  duration consumer (may be {@code null})
     * 
     * @return object serialized
     */
    byte[] serialize(Object object, Consumer<String> duration);
    
    /**
     * Serialize specified {@code object}.<p>
     * 
     * Please note that this method do not close the specified output stream.
     * 
     * @param object   object to serialize
     * @param out      where to put the serialized bytes
     */
    void serialize(Object object, OutputStream out);
    
    /**
     * Deserialize the specified {@code bytes}.
     * 
     * @implSpec
     * The default implementation uses {@link #deserialize(byte[], Consumer)
     * deserialize(byte[], Consumer)}, passing {@code null} as {@code duration}.
     * 
     * @param <T>    deserialized type
     * @param bytes  bytes to deserialize
     * 
     * @return a object
     */
    default <T> T deserialize(byte[] bytes) {
        return deserialize(bytes, (Consumer<String>) null);
    }
    
    /**
     * Deserialize the specified {@code bytes}.<p>
     * 
     * The duration consumer will receive the time it took to deserialize the
     * bytes in a nicely formatted String ("X.XXX ms").
     * 
     * @param <T>       deserialized type
     * @param bytes     bytes to deserialize
     * @param duration  duration consumer (may be {@code null})
     * 
     * @return an object
     */
    <T> T deserialize(byte[] bytes, Consumer<String> duration);
    
    /**
     * Deserialize an {@code object}.<p>
     * 
     * Please note that this method do not close the specified input stream.
     * 
     * @param <T>  deserialized type
     * @param in   input stream
     * 
     * @return an object
     */
    <T> T deserialize(InputStream in);
}